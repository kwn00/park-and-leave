package io.github.kwn00.parkandleave.domain

/**
 * Link to the vehicle service. A missing link is not a normal parked car.
 */
enum class SdkLink {
    NOT_CONNECTED,
    CONNECTED,
    DISCONNECTED,
}

enum class SignalSource {
    VEHICLE_SERVICE,
    WINDOWS,
    DOOR_POSITION,
    DOOR_LOCK,
    CABIN_LIGHT,
    READING_LIGHT,
    LOW_BEAM,
    TAIL_LAMP,
    SEATS,
    HVAC,
    TIRES,
    WASHER,
    TELLTALES,
    SOC,
    FUEL,
    TRIP,
}

enum class UnreadCause {
    /** SecurityException or a permission this app cannot hold. */
    PERMISSION,

    /** The service said this property is not available on the vehicle. */
    NOT_AVAILABLE,

    /** The call returned null. That is not a confirmed off, closed, or empty value. */
    NULL_VALUE,

    /** Timeout or any other failed read. */
    FAILED,

    /** The vehicle service is not connected. */
    DISCONNECTED,
}

data class UnreadSignal(
    val source: SignalSource,
    val cause: UnreadCause,
)

/**
 * Telltales are documented as permanently out of reach for a third-party app.
 *
 * Vehicle SDK 2.3.2 `Safety` KDoc (sources shipped with the artifact) says
 * `getTelltalesInfo`, `checkTelltalesCapabilities`, and `registerTelltales`
 * require `pleos.car.permission.PRIVATE_CAR_SAFETY` at protection level
 * `signature|privileged`. `SafetyImpl` enforces that with
 * `@RequiresCustomPermission(PleosPermission.PRIVATE_CAR_SAFETY)`, and
 * `PermissionUtil` throws `SecurityException` when `checkSelfPermission` fails.
 * The interface method `getTelltalesInfo` is also annotated
 * `@RequiresPermission(CAR_SAFETY)`. `CAR_SAFETY` is a normal permission, but
 * the KDoc and the implementation check both name `PRIVATE_CAR_SAFETY`, and
 * the implementation check is the one that runs.
 *
 * Android grants `signature|privileged` only to an app signed with the
 * declaring certificate, or to a privileged system app on the allowlist:
 * https://developer.android.com/guide/topics/manifest/permission-element#plevel
 * A third-party app cannot hold it, so a telltale permission failure must not
 * keep the headline on "확인된 이상 없음".
 */
fun UnreadSignal.blocksClear(): Boolean = when (cause) {
    UnreadCause.PERMISSION -> source != SignalSource.TELLTALES
    UnreadCause.NOT_AVAILABLE -> false
    UnreadCause.NULL_VALUE,
    UnreadCause.FAILED,
    UnreadCause.DISCONNECTED,
    -> true
}

fun UnreadSignal.line(): String = "${source.label()}: ${cause.phrase()}"

/**
 * One readable info line. Reasons stay on [UnreadSignal.line]; the summary lists names.
 * Example: "확인할 수 없는 항목: 경고등, 워셔액, 주행 거리".
 */
fun uncheckedNote(lines: List<String>): String? {
    if (lines.isEmpty()) return null
    val names = lines.map { it.substringBefore(':').trim() }.filter { it.isNotEmpty() }
    if (names.isEmpty()) return null
    return "확인할 수 없는 항목: ${names.joinToString(", ")}"
}

fun SignalSource.label(): String = when (this) {
    SignalSource.VEHICLE_SERVICE -> "차량 서비스"
    SignalSource.WINDOWS -> "창문"
    SignalSource.DOOR_POSITION -> "도어"
    SignalSource.DOOR_LOCK -> "도어 잠금"
    SignalSource.CABIN_LIGHT -> "실내등"
    SignalSource.READING_LIGHT -> "독서등"
    SignalSource.LOW_BEAM -> "하향등"
    SignalSource.TAIL_LAMP -> "미등"
    SignalSource.SEATS -> "뒷좌석"
    SignalSource.HVAC -> "공조"
    SignalSource.TIRES -> "타이어"
    SignalSource.WASHER -> "워셔액"
    SignalSource.TELLTALES -> "경고등"
    SignalSource.SOC -> "배터리"
    SignalSource.FUEL -> "연료"
    SignalSource.TRIP -> "주행 거리"
}

fun UnreadCause.phrase(): String = when (this) {
    UnreadCause.PERMISSION -> "이 차량에서 앱이 읽을 수 없음"
    UnreadCause.NOT_AVAILABLE -> "이 차량에서 제공하지 않음"
    UnreadCause.NULL_VALUE -> "확인할 수 없음"
    UnreadCause.FAILED -> "확인할 수 없음"
    UnreadCause.DISCONNECTED -> "연결되지 않음"
}

/**
 * Maps an SDK failure onto a cause. [securityException] is true for
 * `SecurityException`, including the SDK text "need Permission".
 */
fun causeOfReadFailure(securityException: Boolean, message: String?): UnreadCause {
    if (securityException) return UnreadCause.PERMISSION
    val text = message.orEmpty()
    if (
        text.contains("need Permission", ignoreCase = true) ||
        text.contains("SecurityException", ignoreCase = true)
    ) {
        return UnreadCause.PERMISSION
    }
    if (
        text.contains("Service connection failed", ignoreCase = true) ||
        text.contains("not initialize", ignoreCase = true) ||
        text.contains("CAS has not started", ignoreCase = true)
    ) {
        return UnreadCause.DISCONNECTED
    }
    if (
        text.contains("not available", ignoreCase = true) ||
        text.contains("unavailable", ignoreCase = true)
    ) {
        return UnreadCause.NOT_AVAILABLE
    }
    return UnreadCause.FAILED
}

/**
 * When every source failed because the service is down, one calm line is enough.
 * A single disconnected source does not erase a different source's result.
 */
fun collapseDisconnected(unread: List<UnreadSignal>): List<UnreadSignal> {
    if (unread.isEmpty() || unread.any { it.cause != UnreadCause.DISCONNECTED }) return unread
    return listOf(UnreadSignal(SignalSource.VEHICLE_SERVICE, UnreadCause.DISCONNECTED))
}
