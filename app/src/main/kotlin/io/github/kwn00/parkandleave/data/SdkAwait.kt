package io.github.kwn00.parkandleave.data

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume

internal suspend fun <T> awaitSdk(
    timeoutMs: Long = 2_500,
    call: (onComplete: (T) -> Unit, onFailed: (Exception) -> Unit) -> Unit,
): Result<T> {
    return try {
        withTimeout(timeoutMs) {
            suspendCancellableCoroutine { continuation ->
                try {
                    call(
                        { value ->
                            if (continuation.isActive) continuation.resume(Result.success(value))
                        },
                        { error ->
                            if (continuation.isActive) continuation.resume(Result.failure(error))
                        },
                    )
                } catch (error: Exception) {
                    if (continuation.isActive) continuation.resume(Result.failure(error))
                }
            }
        }
    } catch (timeout: TimeoutCancellationException) {
        Result.failure(timeout)
    }
}

internal fun Throwable.shortMessage(): String = message ?: javaClass.simpleName
