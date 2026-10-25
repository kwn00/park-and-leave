package io.github.kwn00.parkandleave.domain

fun formatDebugRows(
    originLabel: String,
    gear: GearReading,
    snapshot: VehicleSnapshot,
): List<DebugRow> {
    val rows = mutableListOf<DebugRow>()
    rows += DebugRow("데이터", originLabel)
    rows += DebugRow("GEAR_SELECTION", gear.propertyId.toString())
    rows += DebugRow("gear.raw", gear.rawValue?.toString() ?: "null")
    rows += DebugRow("gear", gear.gear.name)
    rows += DebugRow("gear.error", gear.error ?: "-")
    rows += DebugRow("snapshot.ready", snapshot.ready.toString())
    rows += DebugRow("sdk", snapshot.link.name)
    snapshot.unread.forEach { unread ->
        rows += DebugRow("unread.${unread.source.name}", unread.line())
    }
    snapshot.windows.forEach { window ->
        rows += DebugRow(
            "window.${window.area.name}",
            "pos=${window.position} max=${window.maxPosition} supported=${window.supported}",
        )
    }
    snapshot.doors.forEach { door ->
        rows += DebugRow(
            "door.${door.area.name}",
            "position=${door.position ?: "null"} locked=${door.locked ?: "null"}",
        )
    }
    rows += DebugRow("cabinLight", snapshot.cabinLightOn?.toString() ?: "null")
    snapshot.readingLights.forEach { light ->
        rows += DebugRow("reading.${light.area.name}", light.on?.toString() ?: "null")
    }
    rows += DebugRow("lowBeam", snapshot.lowBeamOn?.toString() ?: "null")
    rows += DebugRow("tailLamp", snapshot.tailLampOn?.toString() ?: "null")
    snapshot.seats.forEach { seat ->
        rows += DebugRow(
            "seat.${seat.area.name}",
            "occupancy=${seat.occupancy.name} belt=${seat.beltBuckled?.toString() ?: "null"}",
        )
    }
    rows += DebugRow("hvacOn", snapshot.hvacOn?.toString() ?: "null")
    snapshot.hvacZones.forEach { zone ->
        rows += DebugRow("hvac.${zone.area.name}", zone.on?.toString() ?: "null")
    }
    snapshot.tires.forEach { tire ->
        rows += DebugRow("tire.${tire.area.name}", tire.lowPressure?.toString() ?: "null")
    }
    rows += DebugRow("washerLow", snapshot.washerLow?.toString() ?: "null")
    snapshot.telltales.forEach { telltale ->
        rows += DebugRow("telltale.${telltale.rawName}", "active=${telltale.active} kind=${telltale.kind.name}")
    }
    rows += DebugRow("soc", snapshot.socPercent?.toString() ?: "null")
    rows += DebugRow("fuel", snapshot.fuelPercent?.toString() ?: "null")
    rows += DebugRow("trip", snapshot.tripDistance?.toString() ?: "null")
    snapshot.notes.forEach { note -> rows += DebugRow("note", note) }
    snapshot.readErrors.forEach { error -> rows += DebugRow("error", error) }
    return rows
}
