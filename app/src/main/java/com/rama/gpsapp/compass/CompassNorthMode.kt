package com.rama.gpsapp.compass

enum class CompassNorthMode {
    MAGNETIC,
    TRUE_NORTH;

    fun toPersistedString(): String = name

    companion object {
        fun fromPersistedString(value: String): CompassNorthMode =
            entries.find { it.name == value } ?: MAGNETIC
    }
}
