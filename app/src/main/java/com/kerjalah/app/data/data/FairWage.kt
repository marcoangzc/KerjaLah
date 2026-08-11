package com.kerjalah.app.data.data

// [A] Fair-Wage Check (core selling point of KerjaLah, SDG 8).
// Malaysia Minimum Wages Order 2024: RM1,700 per month = RM8.72 per hour.
// Every posting must pass this check before it goes live.
object FairWage {
    const val MIN_HOURLY_RM = 8.72

    // Why: keep the wage rule in ONE place,
    // so UI and ViewModel never hardcode numbers.
    fun isFair(payPerHour: Double): Boolean = payPerHour >= MIN_HOURLY_RM
}
