package com.screenshotvault.domain.model

enum class TimePeriod {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    OLDER,
    ;

    fun displayName(): String = when (this) {
        TODAY -> "Today"
        THIS_WEEK -> "This Week"
        THIS_MONTH -> "This Month"
        OLDER -> "Older"
    }

    companion object {
        fun fromString(value: String): TimePeriod = when (value) {
            "TODAY" -> TODAY
            "THIS_WEEK" -> THIS_WEEK
            "THIS_MONTH" -> THIS_MONTH
            else -> OLDER
        }
    }
}
