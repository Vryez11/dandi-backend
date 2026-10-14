package com.dandi.nyummy.meal.service

import com.dandi.nyummy.cat.repository.CatRepository
import com.dandi.nyummy.cat.service.CatService
import com.dandi.nyummy.config.AppLinkProperties
import com.dandi.nyummy.exception.BusinessException
import com.dandi.nyummy.exception.errorcode.AuthErrorCode
import com.dandi.nyummy.exception.errorcode.MealErrorCode
import com.dandi.nyummy.infra.ai.nutrition.AnalysisFailureCause
import com.dandi.nyummy.infra.ai.nutrition.NutritionAnalysisClient
import com.dandi.nyummy.infra.ai.nutrition.NutritionAnalysisException
import com.dandi.nyummy.infra.push.dto.PushMessage
import com.dandi.nyummy.infra.push.dto.PushType
import com.dandi.nyummy.meal.dto.MealStatusResponse
import com.dandi.nyummy.meal.entity.Meal
import com.dandi.nyummy.meal.enum.MealStatus
import com.dandi.nyummy.meal.mapper.toMealStatusResponse
import com.dandi.nyummy.meal.repository.MealRepository
import com.dandi.nyummy.notification.NotificationId
import com.dandi.nyummy.notification.service.NotificationService
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class AnalysisService(
    private val updateMealService: UpdateMealService,
    private val mealRepository: MealRepository,
    private val nutritionAnalysisClient: NutritionAnalysisClient,
    private val catRepository: CatRepository,
    private val catService: CatService,
    private val notificationService: NotificationService,
    private val appLinkProperties: AppLinkProperties,
    private val clock: Clock,
    private val observationRegistry: ObservationRegistry,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(AnalysisService::class.java)

        // 분석 소요 시간·결과 메트릭(timer)과 span 이름. 태그: outcome, cause, attempt
        private const val ANALYSIS_OBSERVATION_NAME = "nyummy.meal.analysis"
        private const val ATTEMPT_FIRST = "first"
        private const val ATTEMPT_RETRY = "retry"
    }

    /**
     * 식사의 영양 분석 상태를 조회한다.
     *
     * @param userId 조회를 요청한 사용자 ID (소유권 검증에 사용)
     * @param mealId 조회할 [Meal]의 ID
     * @return 분석 상태를 담은 [MealStatusResponse]
     * @throws BusinessException [MealErrorCode.MEAL_NOT_FOUND] mealId에 해당하는 식사가 없거나, 삭제된 경우
     * @throws BusinessException [AuthErrorCode.FORBIDDEN] userId가 소유자가 아닌 경우
     */
    @Transactional(readOnly = true)
    fun getMealStatus(userId: Long, mealId: Long): MealStatusResponse {
        val meal = mealRepository.findMealByIdAndDeletedAtIsNull(mealId)
            ?: throw BusinessException(MealErrorCode.MEAL_NOT_FOUND, "Meal Not Found")

        meal.validateOwnership(userId)

        return meal.toMealStatusResponse()
    }

    /**
     * 식사 이미지를 분석해 이름·아이콘·영양을 저장하고,
     * 분석 상태 전이(WAITING/FAILED → ANALYZING → COMPLETED/FAILED)를 전담한다.
     *
     * 상태 변경은 [UpdateMealService]를 통해 각각 독립 트랜잭션으로 즉시 커밋되므로,
     * 분석이 진행되는 동안에도 [getMealStatus] 폴링이 ANALYZING 상태를 조회할 수 있다.
     * 분석 실패는 예외로 전파하지 않고 FAILED 상태로 기록한다.
     * 소유권 검증은 호출자가 보장한다.
     *
     * COMPLETED로 확정되면 고양이의 마지막 식사 시각을 갱신한다. 리텐션 배치가 이 값으로 대상을 고른다.
     *
     * 상태가 확정된 뒤 결과를 푸시로 알린다([sendAnalysisPush]).
     *
     * 분석 구간(이미지 다운로드·AI 호출·결과 저장)은 `nyummy.meal.analysis` 관측으로 남긴다.
     * 소요 시간과 함께 결과(outcome), 실패 원인(cause), 첫 분석·재시도 여부(attempt)를 태그로 단다.
     *
     * @param meal 분석할 [Meal]
     *  (상태가 [MealStatus.ANALYZABLE_STATUSES]에 속하지 않으면 아무 작업도 하지 않는다)
     */
    fun analyzeNutrition(meal: Meal) {
        if (meal.status !in MealStatus.ANALYZABLE_STATUSES) {
            return
        }

        // 이 시도를 식별하는 값. 재시도가 또 실패해도 앞선 실패와 다른 알림이 되게 한다.
        val analyzedAt = Instant.now(clock)

        // 상태를 ANALYZING으로 바꾸기 전에 읽어야 첫 분석(WAITING)과 재시도(FAILED)가 구분된다.
        val attempt = if (meal.status == MealStatus.FAILED) ATTEMPT_RETRY else ATTEMPT_FIRST

        updateMealService.updateStatus(meal, MealStatus.ANALYZING)

        var status = MealStatus.COMPLETED
        var failureCause: AnalysisFailureCause? = null

        // 이미지 다운로드·AI 호출·결과 저장 구간. 결과 푸시는 넣지 않는다.
        // scope를 열어 두면 이 안의 Gemini 호출 span이 분석 span의 자식으로 붙는다.
        val observation = Observation.start(ANALYSIS_OBSERVATION_NAME, observationRegistry)
        try {
            observation.openScope().use {
                val analysisResult = nutritionAnalysisClient.analyzeNutrition(meal.imageKey)
                updateMealService.updateAnalysisResult(meal, analysisResult)
            }
        } catch (e: RuntimeException) {
            status = MealStatus.FAILED
            observation.error(e)

            if (e is NutritionAnalysisException) {
                failureCause = e.failureCause
                // 원인 상세(HTTP 상태·응답 본문·스택 트레이스)는 경계(Gemini·S3)에서 이미 남겼다. 여기서는 상태 전이만 남긴다.
                logger.info(
                    "영양 분석 실패: mealId={}, userId={}, attempt={}, cause={}, message={}",
                    meal.id,
                    meal.userId,
                    attempt,
                    failureCause,
                    e.message,
                )
            } else {
                failureCause = AnalysisFailureCause.UNKNOWN
                logger.error(
                    "영양 분석 실패(예상하지 못한 예외): mealId={}, userId={}, imageKey={}, attempt={}",
                    meal.id,
                    meal.userId,
                    meal.imageKey,
                    attempt,
                    e,
                )
            }
        } finally {
            observation
                .lowCardinalityKeyValue("outcome", status.name.lowercase())
                .lowCardinalityKeyValue("cause", failureCause?.name?.lowercase() ?: "none")
                .lowCardinalityKeyValue("attempt", attempt)
                .stop()
        }

        updateMealService.updateStatus(meal, status)

        if (status == MealStatus.COMPLETED) {
            // 실패는 위 catch에서 원인과 함께 남겼다.
            logger.info("영양 분석 완료: mealId={}, userId={}, attempt={}", meal.id, meal.userId, attempt)
            catService.updateLastMealAt(meal.userId, meal.mealAt)
        }

        sendAnalysisPush(meal, status, analyzedAt)
    }

    /**
     * 분석 결과를 푸시로 알린다. 상태가 이미 커밋된 뒤에 보내므로 발송이 실패해도 결과는 남는다.
     * 수신 거부·토큰 없음은 [NotificationService.sendServicePush]가 조용히 넘긴다.
     */
    private fun sendAnalysisPush(meal: Meal, status: MealStatus, analyzedAt: Instant) {
        try {
            val catName = catRepository.findByUserId(meal.userId)?.name
                ?: run {
                    logger.error("가입 시 생성되어야 할 고양이가 없습니다: userId={}, mealId={}", meal.userId, meal.id)
                    return
                }

            val result = notificationService.sendServicePush(
                meal.userId,
                convertPushMessage(meal, status, catName, analyzedAt),
            )

            notificationService.deleteInvalidDeviceTokens(result.invalidTokens)
        } catch (e: RuntimeException) {
            logger.error("분석 결과 알림 발송 실패: mealId={}, userId={}, status={}", meal.id, meal.userId, status, e)
        }
    }

    /**
     * 성공·실패에 따라 알림 내용을 고른다.
     *
     * 링크는 둘 다 식사 상세다. 성공은 칼로리를 확인하러, 실패는 재시도하러 같은 화면으로 간다.
     */
    private fun convertPushMessage(meal: Meal, status: MealStatus, catName: String, analyzedAt: Instant): PushMessage =
        if (status == MealStatus.COMPLETED) {
            PushMessage(
                type = PushType.ANALYSIS_COMPLETED,
                notificationId = NotificationId.createAnalysisId(meal.id, status, analyzedAt),
                title = "$catName 맛있게 먹었다냥 🐾",
                body = "${meal.name} 분석이 끝났어요. 칼로리 확인해줘!",
                appLink = appLinkProperties.createMealDetailLink(meal.id),
            )
        } else {
            PushMessage(
                type = PushType.ANALYSIS_FAILED,
                notificationId = NotificationId.createAnalysisId(meal.id, status, analyzedAt),
                title = "사진을 못 알아봤다냥 🐾",
                body = "다시 시도해줄래?",
                appLink = appLinkProperties.createMealDetailLink(meal.id),
            )
        }

    /**
     * FAILED 상태의 식사에 대해 영양 분석을 재시도한다.
     *
     * @param userId 재시도를 요청한 사용자 ID (소유권 검증에 사용)
     * @param mealId 재시도할 [Meal]의 ID
     * @return 재시도 결과의 분석 상태를 담은 [MealStatusResponse]
     * @throws BusinessException [MealErrorCode.MEAL_NOT_FOUND] mealId에 해당하는 식사가 없거나, 삭제된 경우
     * @throws BusinessException [AuthErrorCode.FORBIDDEN] userId가 소유자가 아닌 경우
     * @throws BusinessException [MealErrorCode.ANALYSIS_NOT_RETRYABLE] 식사가 FAILED 상태가 아닌 경우
     */
    fun retryNutritionAnalysis(userId: Long, mealId: Long): MealStatusResponse {
        val meal = mealRepository.findMealByIdAndDeletedAtIsNull(mealId)
            ?: throw BusinessException(MealErrorCode.MEAL_NOT_FOUND)

        meal.validateOwnership(userId)

        if (meal.status != MealStatus.FAILED) {
            throw BusinessException(MealErrorCode.ANALYSIS_NOT_RETRYABLE)
        }

        analyzeNutrition(meal)

        return meal.toMealStatusResponse()
    }
}
