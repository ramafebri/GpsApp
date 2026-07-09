package com.rama.gpsapp.level

enum class LevelDisplayMode {
    BUBBLE,
    CLINOMETER,
    COMBINED;

    fun toPersistedString(): String = name

    companion object {
        fun fromPersistedString(value: String): LevelDisplayMode =
            entries.find { it.name == value } ?: BUBBLE
    }
}
