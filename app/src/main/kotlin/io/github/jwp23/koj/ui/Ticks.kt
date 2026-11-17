package io.github.jwp23.koj.ui

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val MINUTE_MILLIS = 60_000L

/**
 * Emits once at once and then once a minute, so anything that depends on the time is recomputed.
 */
fun minuteTicks(): Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        delay(MINUTE_MILLIS)
    }
}
