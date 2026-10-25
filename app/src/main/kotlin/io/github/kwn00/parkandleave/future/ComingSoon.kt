package io.github.kwn00.parkandleave.future

/**
 * Vehicle control, on-device speech, and place profiles are behind these
 * interfaces. The MVP does not depend on the coming-soon SDK artifacts.
 */
interface VehicleControlGateway {
    val available: Boolean
    fun closeWindow(areaName: String): ControlResult
    fun turnOffCabinLight(): ControlResult
    fun lockDoors(): ControlResult
}

interface OnDeviceSpeech {
    val available: Boolean
    fun speak(text: String): Boolean
    fun stop()
}

enum class PlaceProfile {
    DEFAULT,
}

interface PlaceProfileSource {
    val available: Boolean
    fun activeProfile(): PlaceProfile
}

sealed class ControlResult {
    data object Unavailable : ControlResult()
    data class Rejected(val reason: String) : ControlResult()
}

class UnavailableVehicleControl : VehicleControlGateway {
    override val available: Boolean = false
    override fun closeWindow(areaName: String): ControlResult = ControlResult.Unavailable
    override fun turnOffCabinLight(): ControlResult = ControlResult.Unavailable
    override fun lockDoors(): ControlResult = ControlResult.Unavailable
}

class UnavailableOnDeviceSpeech : OnDeviceSpeech {
    override val available: Boolean = false
    override fun speak(text: String): Boolean = false
    override fun stop() = Unit
}

class DefaultPlaceProfileSource : PlaceProfileSource {
    override val available: Boolean = false
    override fun activeProfile(): PlaceProfile = PlaceProfile.DEFAULT
}
