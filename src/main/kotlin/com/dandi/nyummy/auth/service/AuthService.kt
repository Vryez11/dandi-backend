package com.dandi.nyummy.auth.service

import com.dandi.nyummy.auth.config.AuthProperties
import com.dandi.nyummy.auth.dto.ConfirmAuthCodeRequest
import com.dandi.nyummy.auth.dto.ConfirmAuthCodeResponse
import com.dandi.nyummy.auth.dto.LoginRequest
import com.dandi.nyummy.auth.dto.LoginResponse
import com.dandi.nyummy.auth.dto.PasswordResetRequest
import com.dandi.nyummy.auth.dto.RefreshRequest
import com.dandi.nyummy.auth.dto.RefreshResponse
import com.dandi.nyummy.auth.dto.SendAuthCodeRequest
import com.dandi.nyummy.auth.dto.SendAuthCodeResponse
import com.dandi.nyummy.auth.dto.SignUpRequest
import com.dandi.nyummy.auth.dto.SignUpResponse
import com.dandi.nyummy.auth.entity.RefreshToken
import com.dandi.nyummy.auth.enum.AuthPurpose
import com.dandi.nyummy.auth.repository.RefreshTokenRepository
import com.dandi.nyummy.exception.BusinessException
import com.dandi.nyummy.exception.errorcode.AuthErrorCode
import com.dandi.nyummy.infra.aws.ses.SesService
import com.dandi.nyummy.profile.entity.Profile
import com.dandi.nyummy.profile.repository.ProfileRepository
import com.dandi.nyummy.security.jwt.TokenService
import com.dandi.nyummy.security.jwt.TokenType
import com.dandi.nyummy.user.entity.User
import com.dandi.nyummy.user.repository.UserRepository
import com.dandi.nyummy.user.service.PasswordService
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val profileRepository: ProfileRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val tokenService: TokenService,
    private val codeService: CodeService,
    private val sesService: SesService,
    private val passwordService: PasswordService,
    private val authProperties: AuthProperties,
) {

    /**
     * 이메일과 비밀번호로 사용자를 인증하고 AccessToken·RefreshToken을 발급한다.
     * 기존에 발급된 RefreshToken이 있으면 새 토큰으로 교체(rotate)하고, 없으면 새로 저장한다.
     *
     * @param request 로그인 요청 정보를 담은 [LoginRequest] (이메일, 비밀번호)
     * @return 리다이렉트 URL과 AccessToken·RefreshToken을 담은 [LoginResponse]
     * @throws BusinessException [AuthErrorCode.INVALID_CREDENTIALS] 이메일에 해당하는 사용자가 없거나 비밀번호가 일치하지 않는 경우
     */
    @Transactional
    fun login(request: LoginRequest): LoginResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw BusinessException(AuthErrorCode.INVALID_CREDENTIALS)

        if (!passwordService.matchesPassword(request.password, user.password)) {
            throw BusinessException(AuthErrorCode.INVALID_CREDENTIALS)
        }

        val userId = user.id

        val (newAccessToken, newRefreshToken) = tokenService.createTokenPair(userId)
        val newExpiresAt = tokenService.getExpiration(newRefreshToken, TokenType.REFRESH).toInstant()

        val existingToken = refreshTokenRepository.findByUserId(userId)

        if (existingToken != null) {
            existingToken.rotate(newRefreshToken, newExpiresAt)
        } else {
            refreshTokenRepository.save(
                RefreshToken(
                    refreshToken = newRefreshToken,
                    userId = userId,
                    expiresAt = newExpiresAt,
                ),
            )
        }

        val redirectUrl = authProperties.loginRedirectUrl

        return LoginResponse(redirectUrl, newAccessToken, newRefreshToken)
    }

    @Transactional
    fun signup(request: SignUpRequest): SignUpResponse {
        val emailVerifiedToken = request.emailVerifiedToken

        val (email, purpose) = try {
            Pair(
                tokenService.getEmail(emailVerifiedToken, TokenType.EMAIL_VERIFIED),
                tokenService.getPurpose(emailVerifiedToken, TokenType.EMAIL_VERIFIED),
            )
        } catch (e: ExpiredJwtException) {
            throw BusinessException(AuthErrorCode.EMAIL_VERIFICATION_EXPIRED)
        } catch (e: JwtException) {
            throw BusinessException(AuthErrorCode.UNAUTHORIZED)
        }

        if (purpose != AuthPurpose.SIGNUP) {
            throw BusinessException(AuthErrorCode.UNAUTHORIZED)
        }

        if (userRepository.existsByEmail(email)) {
            throw BusinessException(AuthErrorCode.EMAIL_ALREADY_EXISTS)
        }

        val encodedPassword = passwordService.encodePassword(request.password)

        val savedUser = try {
            userRepository.save(
                User(
                    email = email,
                    password = encodedPassword,
                ),
            )
        } catch (e: DataIntegrityViolationException) {
            throw BusinessException(AuthErrorCode.EMAIL_ALREADY_EXISTS)
        }

        val userId = savedUser.id

        profileRepository.save(
            Profile(
                nickname = request.nickname,
                birth = request.birth,
                gender = request.gender,
                height = request.height,
                weight = request.weight,
                userId = userId,
            ),
        )

        val (accessToken, refreshToken) = tokenService.createTokenPair(userId)
        val newExpiresAt = tokenService.getExpiration(refreshToken, TokenType.REFRESH).toInstant()

        refreshTokenRepository.save(
            RefreshToken(
                refreshToken = refreshToken,
                userId = userId,
                expiresAt = newExpiresAt,
            ),
        )

        return SignUpResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
        )
    }

    /**
     * 리프레시 토큰을 검증하고 새 AccessToken·RefreshToken을 발급한다(rotate).
     *
     * @param request 리프레시 요청 정보를 담은 [RefreshRequest] (리프레시 토큰)
     * @return 새로 발급된 AccessToken·RefreshToken을 담은 [RefreshResponse]
     * @throws BusinessException [AuthErrorCode.INVALID_REFRESH_TOKEN] 토큰이 유효하지 않거나(서명·만료·타입 불일치),
     * 저장된 리프레시 토큰이 없거나, 이미 교체(rotate)된 토큰인 경우
     */
    @Transactional
    fun refresh(request: RefreshRequest): RefreshResponse {
        val userId = try {
            tokenService.getUserId(request.refreshToken, TokenType.REFRESH)
        } catch (e: Exception) {
            throw BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN)
        }

        val existingToken = refreshTokenRepository.findByUserId(userId)
            ?: throw BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN)

        if (existingToken.refreshToken != request.refreshToken) {
            throw BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN)
        }

        val (newAccessToken, newRefreshToken) = tokenService.createTokenPair(userId)
        val newExpiresAt = tokenService.getExpiration(newRefreshToken, TokenType.REFRESH).toInstant()

        existingToken.rotate(newRefreshToken, newExpiresAt)

        return RefreshResponse(newAccessToken, newRefreshToken)
    }

    /**
     * 사용자의 RefreshToken을 삭제해 로그아웃 처리한다.
     *
     * 저장된 RefreshToken이 없어도 이미 로그아웃된 상태로 보고 정상 처리한다(멱등).
     *
     * @param userId 로그아웃할 사용자 ID
     * @param accessToken 블랙리스트 등록에 사용할 AccessToken (Redis 도입 전까지 미사용)
     */
    @Transactional
    fun logout(userId: Long, accessToken: String) {
        // TODO: accessToken 레디스 블랙리스트에 저장

        val refreshToken = refreshTokenRepository.findByUserId(userId)
            ?: return

        refreshTokenRepository.delete(refreshToken)
    }

    /**
     * 이메일로 6자리 인증 코드를 발급·발송하고, 인증 세션 식별용 emailChallengeToken을 발급한다.
     *
     * 용도별 전제조건을 먼저 검사한다: 회원가입은 미가입 이메일이어야 하고, 비밀번호 찾기는 가입된 이메일이어야 한다.
     * 용도는 emailChallengeToken의 클레임에 실려 confirm까지 전달된다.
     *
     * @param request 인증 코드 발송 요청 정보를 담은 [SendAuthCodeRequest] (이메일, 용도)
     * @return 발급된 emailChallengeToken을 담은 [SendAuthCodeResponse]
     * @throws BusinessException [AuthErrorCode.EMAIL_ALREADY_EXISTS] 회원가입 용도인데 이미 가입된 이메일인 경우
     * @throws BusinessException [AuthErrorCode.EMAIL_NOT_FOUND] 비밀번호 찾기 용도인데 가입되지 않은 이메일인 경우
     * @throws BusinessException [AuthErrorCode.EMAIL_SEND_RATE_LIMITED] TTL 윈도우 내 발송 횟수가 5회를 초과한 경우
     * @throws BusinessException [SesErrorCode.EMAIL_SEND_FAILED] SES 이메일 발송이 실패한 경우
     */
    fun sendAuthCode(request: SendAuthCodeRequest): SendAuthCodeResponse {
        val email = request.email
        val purpose = request.purpose

        when (purpose) {
            AuthPurpose.SIGNUP -> if (userRepository.existsByEmail(email)) {
                throw BusinessException(AuthErrorCode.EMAIL_ALREADY_EXISTS)
            }

            AuthPurpose.RESET_PASSWORD -> if (!userRepository.existsByEmail(email)) {
                throw BusinessException(AuthErrorCode.EMAIL_NOT_FOUND)
            }
        }

        val authCode = codeService.createCodeByEmail(email)

        sesService.sendAuthCode(email, authCode)

        val emailChallengeToken = tokenService.createEmailChallengeToken(email, purpose)

        return SendAuthCodeResponse(emailChallengeToken)
    }

    /**
     * emailChallengeToken과 인증 코드를 검증하고, 토큰의 용도를 승계한 emailVerifiedToken을 발급한다.
     *
     * 인증 코드의 유효 시간은 emailChallengeToken의 만료(exp)가 유일한 기준이며, DB에서 시간 계산은 하지 않는다.
     *
     * @param request 인증 코드 확인 요청 정보를 담은 [ConfirmAuthCodeRequest] (인증 코드, emailChallengeToken)
     * @return 발급된 emailVerifiedToken을 담은 [ConfirmAuthCodeResponse]
     * @throws BusinessException [AuthErrorCode.EMAIL_CODE_EXPIRED] emailChallengeToken이 만료된 경우 (코드 재발송 필요)
     * @throws BusinessException [AuthErrorCode.UNAUTHORIZED] 토큰의 서명·형식·타입·용도가 유효하지 않은 경우
     * @throws BusinessException [AuthErrorCode.INCORRECT_EMAIL] 해당 이메일로 발급된 인증 코드가 없는 경우
     * @throws BusinessException [AuthErrorCode.EMAIL_CODE_ATTEMPT_EXCEEDED] 오답이 5회 누적된 경우
     * @throws BusinessException [AuthErrorCode.EMAIL_CODE_MISMATCH] 인증 코드가 일치하지 않는 경우
     */
    fun confirmAuthCode(request: ConfirmAuthCodeRequest): ConfirmAuthCodeResponse {
        val challengeToken = request.emailChallengeToken
        val challengeCode = request.authCode

        val (email, purpose) = try {
            Pair(
                tokenService.getEmail(challengeToken, TokenType.EMAIL_CHALLENGE),
                tokenService.getPurpose(challengeToken, TokenType.EMAIL_CHALLENGE),
            )
        } catch (e: ExpiredJwtException) {
            throw BusinessException(AuthErrorCode.EMAIL_CODE_EXPIRED)
        } catch (e: JwtException) {
            throw BusinessException(AuthErrorCode.UNAUTHORIZED)
        }

        codeService.confirmAuthCodeByEmail(challengeCode, email)

        return ConfirmAuthCodeResponse(tokenService.createEmailVerifiedToken(email, purpose))
    }

    /**
     * 비밀번호 찾기 용도의 emailVerifiedToken을 검증하고, 임시 비밀번호로 교체한 뒤 이메일로 발송한다.
     *
     * 토큰 파싱(트랜잭션 없음) → 비밀번호 교체([PasswordService] 트랜잭션) → 이메일 발송(트랜잭션 밖) 순으로
     * 순차 실행된다 — 발송이 실패하면 사용자는 코드 발송부터 플로우를 재시작해야 한다.
     *
     * @param request 비밀번호 재설정 요청 정보를 담은 [PasswordResetRequest] (emailVerifiedToken)
     * @throws BusinessException [AuthErrorCode.EMAIL_VERIFICATION_EXPIRED] emailVerifiedToken이 만료된 경우
     * @throws BusinessException [AuthErrorCode.UNAUTHORIZED] 토큰의 서명·형식·타입이 유효하지 않거나 비밀번호 찾기 용도가 아닌 경우
     * @throws BusinessException [AuthErrorCode.EMAIL_NOT_FOUND] 토큰의 이메일에 해당하는 사용자가 없는 경우
     * @throws BusinessException [SesErrorCode.EMAIL_SEND_FAILED] 임시 비밀번호 이메일 발송이 실패한 경우
     */
    fun resetPassword(request: PasswordResetRequest) {
        // TODO: 레디스 도입 시 사용한 emailVerifiedToken을 블랙리스트로 등록해 일회성 보장

        val emailVerifiedToken = request.emailVerifiedToken

        val (email, purpose) = try {
            Pair(
                tokenService.getEmail(emailVerifiedToken, TokenType.EMAIL_VERIFIED),
                tokenService.getPurpose(emailVerifiedToken, TokenType.EMAIL_VERIFIED),
            )
        } catch (e: ExpiredJwtException) {
            throw BusinessException(AuthErrorCode.EMAIL_VERIFICATION_EXPIRED)
        } catch (e: JwtException) {
            throw BusinessException(AuthErrorCode.UNAUTHORIZED)
        }

        if (purpose != AuthPurpose.RESET_PASSWORD) {
            throw BusinessException(AuthErrorCode.UNAUTHORIZED)
        }

        val tempPassword = passwordService.createTempPasswordByEmail(email)

        sesService.sendTempPassword(email, tempPassword)
    }
}
