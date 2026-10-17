package com.dandi.nyummy.config

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.TransactionExecution
import org.springframework.transaction.TransactionExecutionListener

/**
 * 트랜잭션 하나를 span 하나로 남긴다. trace에서 커넥션·쿼리 span이 어느 트랜잭션(서비스 메서드)에 속하는지 보이게 한다.
 * Boot가 TransactionExecutionListener 빈을 트랜잭션 매니저에 자동으로 등록한다.
 */
@Component
class ObservationTransactionExecutionListener(private val observationRegistry: ObservationRegistry) :
    TransactionExecutionListener {

    // REQUIRES_NEW처럼 한 스레드에서 트랜잭션이 겹칠 수 있어 스택으로 쌓는다.
    private val openedObservations = ThreadLocal.withInitial { ArrayDeque<OpenedObservation>() }

    // 커넥션은 트랜잭션 시작 도중(beforeBegin ~ afterBegin)에 빌려지므로, 시작 전에 scope를 열어야 그 아래로 들어온다.
    override fun beforeBegin(transaction: TransactionExecution) {
        val observation = Observation.createNotStarted(OBSERVATION_NAME, observationRegistry)
            .contextualName(convertSpanName(transaction.transactionName))
            .lowCardinalityKeyValue("transaction.read-only", transaction.isReadOnly.toString())
            .highCardinalityKeyValue("transaction.name", transaction.transactionName)
            .start()
        openedObservations.get().addLast(OpenedObservation(transaction, observation, observation.openScope()))
    }

    override fun afterBegin(transaction: TransactionExecution, beginFailure: Throwable?) {
        if (beginFailure != null) {
            stopObservation(transaction, "begin-failed", beginFailure)
        }
    }

    override fun afterCommit(transaction: TransactionExecution, commitFailure: Throwable?) {
        stopObservation(transaction, if (commitFailure == null) "commit" else "commit-failed", commitFailure)
    }

    // 비즈니스 거절로 인한 롤백은 정상 흐름이라 에러로 표시하지 않는다. 롤백 자체가 실패했을 때만 에러다.
    override fun afterRollback(transaction: TransactionExecution, rollbackFailure: Throwable?) {
        stopObservation(transaction, if (rollbackFailure == null) "rollback" else "rollback-failed", rollbackFailure)
    }

    private fun stopObservation(transaction: TransactionExecution, outcome: String, failure: Throwable?) {
        val stack = openedObservations.get()
        // 바깥 트랜잭션에 참여만 한 경우에도 afterRollback이 불린다. 여기서 시작하지 않은 트랜잭션이면 무시한다.
        val opened = stack.lastOrNull { it.transaction === transaction } ?: return
        if (stack.last() !== opened) {
            logger.warn("트랜잭션 span 종료 순서 어긋남: name={}", transaction.transactionName)
        }
        stack.remove(opened)
        opened.scope.close()
        opened.observation.lowCardinalityKeyValue("transaction.outcome", outcome)
        failure?.let { opened.observation.error(it) }
        opened.observation.stop()
        if (stack.isEmpty()) {
            openedObservations.remove()
        }
    }

    // "com.dandi.nyummy.meal.service.MealService.createMeal" → "tx MealService.createMeal"
    private fun convertSpanName(transactionName: String): String {
        val shortName = transactionName.split('.').takeLast(2).joinToString(".")
        return if (shortName.isBlank()) "tx" else "tx $shortName"
    }

    // equals를 재정의하지 않아 스택에서 같은 객체(identity)로만 찾고 지운다.
    private class OpenedObservation(
        val transaction: TransactionExecution,
        val observation: Observation,
        val scope: Observation.Scope,
    )

    companion object {
        private const val OBSERVATION_NAME = "nyummy.transaction"
        private val logger = LoggerFactory.getLogger(ObservationTransactionExecutionListener::class.java)
    }
}
