package io.github.kwn00.parkandleave.domain

object ParkingThresholds {
    const val SOC_ITEM_PERCENT: Int = 20
    const val SOC_TOMORROW_PERCENT: Int = 30
    const val FUEL_ITEM_PERCENT: Int = 15
    const val TIRE_FLAG_MAX: Float = 1f
}

class ParkingCheckEvaluator {
    fun evaluate(snapshot: VehicleSnapshot): ParkingReport {
        val items = buildList {
            addRearOccupancy(snapshot)
            addTrunk(snapshot)
            addHood(snapshot)
            addOpenDoors(snapshot)
            addTire(snapshot)
            addWindows(snapshot)
            addUnlocked(snapshot)
            addCabin(snapshot)
            addReadingLights(snapshot)
            addExteriorLights(snapshot)
            addHvac(snapshot)
            addWasher(snapshot)
            addSoc(snapshot)
            addFuel(snapshot)
            addMappedTelltales(snapshot)
        }.sortedWith(compareBy<CheckItem> { it.severity.ordinal }.thenBy { it.id.ordinal })

        val unread = visibleUnread(snapshot)
        val blocked = unread.any { it.blocksClear() } ||
            (snapshot.unread.isEmpty() && snapshot.link == SdkLink.CONNECTED && snapshot.readErrors.isNotEmpty())
        val allClear = items.isEmpty() && !blocked
        val headline = when {
            items.isNotEmpty() -> "확인 ${items.size}건"
            !allClear -> "확인된 이상 없음"
            else -> "모두 정상"
        }
        return ParkingReport(
            items = items,
            headline = headline,
            allClear = allClear,
            tomorrowLine = tomorrowLine(snapshot, items),
            clearanceLine = if (allClear) "내려도 됩니다" else null,
            speechLine = speechLine(items),
            uncheckedLines = unread.map { it.line() },
        )
    }

    private fun MutableList<CheckItem>.addRearOccupancy(snapshot: VehicleSnapshot) {
        val occupied = snapshot.seats.filter {
            it.area.isRearSeat() && it.occupancy == SeatOccupancy.OCCUPIED
        }
        if (occupied.isEmpty()) return
        add(
            item(
                SignalId.REAR_OCCUPANCY,
                Severity.WARNING,
                "뒷좌석 확인",
                occupied.joinToString(" · ") { "${it.area.label()} 점유" },
                "뒷좌석을 확인하세요",
            ),
        )
    }

    /**
     * Hood and trunk are closures, not cabin door locks.
     *
     * Vehicle SDK 2.3.2 `Door.setDoorPosition` says the minimum is typically 0
     * for fully closed, and a higher value is open. `Door.setDoorLock` says
     * true locks and false unlocks. Cabin doors keep that split.
     *
     * An open frunk on the contest vehicle arrives as HOOD `DOOR_LOCK` = false
     * while `DOOR_POS` stays 0, and the system UI asks to close the frunk.
     * A released hood or trunk latch is therefore its own open card and is
     * never mixed into "도어 잠금 해제" or "도어 열림".
     */
    private fun MutableList<CheckItem>.addTrunk(snapshot: VehicleSnapshot) {
        if (snapshot.doors.none { it.area == VehicleArea.TRUNK && it.isOpenClosure() }) return
        add(item(SignalId.TRUNK, Severity.WARNING, "트렁크 열림", "트렁크가 열려 있습니다", "트렁크를 닫아 주세요"))
    }

    private fun MutableList<CheckItem>.addHood(snapshot: VehicleSnapshot) {
        if (snapshot.doors.none { it.area == VehicleArea.HOOD && it.isOpenClosure() }) return
        add(item(SignalId.HOOD, Severity.WARNING, "보닛 열림", "보닛이 열려 있습니다", "보닛을 닫아 주세요"))
    }

    private fun MutableList<CheckItem>.addOpenDoors(snapshot: VehicleSnapshot) {
        val open = snapshot.doors.filter { !it.area.isClosure() && (it.position ?: 0) > 0 }
        if (open.isEmpty()) return
        add(
            item(
                SignalId.DOOR_OPEN,
                Severity.WARNING,
                "도어 열림",
                open.joinToString(" · ") { "${it.area.label()} 열림" },
                "도어를 닫아 주세요",
            ),
        )
    }

    private fun MutableList<CheckItem>.addTire(snapshot: VehicleSnapshot) {
        val low = snapshot.tires.filter { isLowTire(it.lowPressure) }
        if (low.isEmpty()) return
        add(
            item(
                SignalId.TIRE,
                Severity.WARNING,
                "타이어 저압",
                low.joinToString(" · ") { it.area.label() },
                "타이어 공기압을 점검하세요",
            ),
        )
    }

    private fun MutableList<CheckItem>.addWindows(snapshot: VehicleSnapshot) {
        val open = snapshot.windows.filter { it.supported && it.position > 0 }
        if (open.isEmpty()) return
        add(
            item(
                SignalId.WINDOW,
                Severity.CAUTION,
                "창문 열림",
                open.joinToString(" · ") { windowDetail(it) },
                "해당 창문을 닫아 주세요",
            ),
        )
    }

    private fun MutableList<CheckItem>.addUnlocked(snapshot: VehicleSnapshot) {
        val unlocked = snapshot.doors.filter { !it.area.isClosure() && it.locked == false }
        if (unlocked.isEmpty()) return
        add(
            item(
                SignalId.DOOR_UNLOCKED,
                Severity.CAUTION,
                "도어 잠금 해제",
                unlocked.joinToString(" · ") { "${it.area.label()} 잠금 해제" },
                "도어 잠금 버튼을 눌러 주세요",
            ),
        )
    }

    private fun MutableList<CheckItem>.addCabin(snapshot: VehicleSnapshot) {
        if (snapshot.cabinLightOn != true) return
        add(item(SignalId.CABIN_LIGHT, Severity.CAUTION, "실내등 켜짐", "실내등이 켜져 있습니다", "실내등 스위치를 꺼 주세요"))
    }

    private fun MutableList<CheckItem>.addReadingLights(snapshot: VehicleSnapshot) {
        val on = snapshot.readingLights.filter { it.on == true }
        if (on.isEmpty()) return
        add(
            item(
                SignalId.READING_LIGHT,
                Severity.CAUTION,
                "독서등 켜짐",
                on.joinToString(" · ") { "${it.area.label()} 켜짐" },
                "독서등을 꺼 주세요",
            ),
        )
    }

    private fun MutableList<CheckItem>.addExteriorLights(snapshot: VehicleSnapshot) {
        val parts = buildList {
            if (snapshot.lowBeamOn == true) add("하향등 켜짐")
            if (snapshot.tailLampOn == true) add("미등 켜짐")
        }
        if (parts.isEmpty()) return
        add(item(SignalId.EXTERIOR_LIGHT, Severity.CAUTION, "외부등 켜짐", parts.joinToString(" · "), "전조등·미등을 꺼 주세요"))
    }

    private fun MutableList<CheckItem>.addHvac(snapshot: VehicleSnapshot) {
        if (snapshot.hvacOn != true) return
        add(item(SignalId.HVAC, Severity.CAUTION, "공조 켜짐", "공조가 켜져 있습니다", "공조를 꺼 주세요"))
    }

    private fun MutableList<CheckItem>.addWasher(snapshot: VehicleSnapshot) {
        if (snapshot.washerLow != true) return
        add(item(SignalId.WASHER, Severity.CAUTION, "워셔액 부족", "워셔액이 부족합니다", "워셔액을 보충하세요"))
    }

    private fun MutableList<CheckItem>.addSoc(snapshot: VehicleSnapshot) {
        val soc = snapshot.socPercent ?: return
        if (soc >= ParkingThresholds.SOC_ITEM_PERCENT) return
        add(item(SignalId.LOW_SOC, Severity.CAUTION, "배터리 잔량 낮음", "SOC $soc%", "충전을 권장합니다"))
    }

    private fun MutableList<CheckItem>.addFuel(snapshot: VehicleSnapshot) {
        val fuel = snapshot.fuelPercent ?: return
        if (fuel >= ParkingThresholds.FUEL_ITEM_PERCENT) return
        add(item(SignalId.LOW_FUEL, Severity.CAUTION, "연료 잔량 낮음", "연료 $fuel%", "연료를 보충하세요"))
    }

    private fun MutableList<CheckItem>.addMappedTelltales(snapshot: VehicleSnapshot) {
        val active = snapshot.telltales.filter { it.active }
        if (active.any { it.kind == TelltaleKind.TRUNK_OPEN } && none { it.id == SignalId.TRUNK }) {
            add(item(SignalId.TRUNK, Severity.WARNING, "트렁크 열림", "경고등 TRUNK_OPEN", "트렁크를 닫아 주세요"))
        }
        if (active.any { it.kind == TelltaleKind.DOOR_OPEN } && none { it.id == SignalId.DOOR_OPEN }) {
            add(item(SignalId.DOOR_OPEN, Severity.WARNING, "도어 열림", "경고등 DOOR_OPEN", "도어를 닫아 주세요"))
        }
        if (active.any { it.kind == TelltaleKind.TPMS } && none { it.id == SignalId.TIRE }) {
            add(item(SignalId.TIRE, Severity.WARNING, "타이어 저압", "경고등 TPMS", "타이어 공기압을 점검하세요"))
        }
        if (active.any { it.kind == TelltaleKind.LOW_FUEL } && none { it.id == SignalId.LOW_FUEL }) {
            add(item(SignalId.LOW_FUEL, Severity.CAUTION, "연료 잔량 낮음", "경고등 LOW_FUEL", "연료를 보충하세요"))
        }
        if (active.any { it.kind == TelltaleKind.LOW_EV_BATTERY } && none { it.id == SignalId.LOW_SOC }) {
            add(item(SignalId.LOW_SOC, Severity.CAUTION, "배터리 잔량 낮음", "경고등 LOW_EV_BATTERY", "충전을 권장합니다"))
        }
        val faults = active.filter { it.kind.isGroupedFault() }
        if (faults.isNotEmpty()) {
            add(
                item(
                    SignalId.TELLTALE,
                    Severity.WARNING,
                    "경고등",
                    faults.joinToString(" · ") { it.rawName },
                    "경고등을 확인하세요",
                ),
            )
        }
    }
}

/**
 * A disconnected service is one unchecked line. Structured [VehicleSnapshot.unread]
 * entries are the source of truth. Legacy [VehicleSnapshot.readErrors] strings
 * still block "모두 정상" only when no structured entry explains them.
 */
private fun visibleUnread(snapshot: VehicleSnapshot): List<UnreadSignal> {
    val recorded = snapshot.unread.distinctBy { it.source }.toMutableList()
    if (snapshot.socPercent != null) {
        recorded.removeAll { it.source == SignalSource.FUEL }
    }
    if (snapshot.link != SdkLink.CONNECTED && recorded.none { it.source == SignalSource.VEHICLE_SERVICE }) {
        recorded += UnreadSignal(SignalSource.VEHICLE_SERVICE, UnreadCause.DISCONNECTED)
    }
    return recorded.sortedBy { it.source.ordinal }
}

private fun DoorReading.isOpenClosure(): Boolean = (position ?: 0) > 0 || locked == false

private fun VehicleArea.isClosure(): Boolean = when (this) {
    VehicleArea.HOOD,
    VehicleArea.TRUNK,
    -> true
    VehicleArea.ROW_1_LEFT,
    VehicleArea.ROW_1_CENTER,
    VehicleArea.ROW_1_RIGHT,
    VehicleArea.ROW_2_LEFT,
    VehicleArea.ROW_2_CENTER,
    VehicleArea.ROW_2_RIGHT,
    VehicleArea.ROW_3_LEFT,
    VehicleArea.ROW_3_CENTER,
    VehicleArea.ROW_3_RIGHT,
    VehicleArea.FRONT_WINDSHIELD,
    VehicleArea.REAR_WINDSHIELD,
    VehicleArea.ROOF,
    VehicleArea.WHEEL_LEFT_FRONT,
    VehicleArea.WHEEL_RIGHT_FRONT,
    VehicleArea.WHEEL_LEFT_REAR,
    VehicleArea.WHEEL_RIGHT_REAR,
    -> false
}

private fun item(
    id: SignalId,
    severity: Severity,
    title: String,
    detail: String,
    guidance: String,
): CheckItem = CheckItem(id, severity, title, detail, guidance)

private fun windowDetail(reading: WindowReading): String {
    val percent = if (reading.maxPosition > 0) " ${reading.position * 100 / reading.maxPosition}%" else ""
    return "${reading.area.label()}$percent"
}

private fun isLowTire(value: Float?): Boolean {
    if (value == null) return false
    return value > 0f && value <= ParkingThresholds.TIRE_FLAG_MAX
}

private fun tomorrowLine(snapshot: VehicleSnapshot, items: List<CheckItem>): String? {
    val soc = snapshot.socPercent
    val trip = snapshot.tripDistance
    val fuel = snapshot.fuelPercent
    return when {
        soc != null && soc < ParkingThresholds.SOC_TOMORROW_PERCENT && trip != null && trip > 0f ->
            "내일 같은 거리라면 충전 권장"
        soc != null && soc < ParkingThresholds.SOC_ITEM_PERCENT ->
            "잔여 에너지가 낮습니다"
        fuel != null && fuel < ParkingThresholds.FUEL_ITEM_PERCENT ->
            "내일 출발 전 연료를 확인하세요"
        items.any { it.id == SignalId.TIRE } ->
            "내일 출발 전 타이어 공기압 점검"
        else -> null
    }
}

private fun speechLine(items: List<CheckItem>): String? {
    if (items.isEmpty()) return null
    val names = items.take(2).joinToString(" · ") { it.title }
    return "확인 ${items.size}건. $names"
}

private fun TelltaleKind.isGroupedFault(): Boolean = when (this) {
    TelltaleKind.ENGINE,
    TelltaleKind.MASTER_WARNING,
    TelltaleKind.ABS,
    TelltaleKind.AIRBAG,
    TelltaleKind.LOW_VOLT_BATTERY,
    TelltaleKind.ENGINE_OIL,
    TelltaleKind.ENGINE_TEMP,
    TelltaleKind.POWER_DOWN,
    TelltaleKind.EV_SYSTEM,
    -> true
    TelltaleKind.TRUNK_OPEN,
    TelltaleKind.DOOR_OPEN,
    TelltaleKind.TPMS,
    TelltaleKind.LOW_FUEL,
    TelltaleKind.LOW_EV_BATTERY,
    TelltaleKind.OTHER,
    -> false
}
