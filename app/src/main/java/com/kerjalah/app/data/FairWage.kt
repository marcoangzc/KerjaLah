package com.kerjalah.app.data

// [A] Fair-Wage Check (core selling point of KerjaLah, SDG 8).
// Malaysia Minimum Wages Order 2024: RM1,700 per month = RM8.72 per hour.
//
// !! MINIMUM WAGE LIVES IN TWO PLACES !!
// This constant is the friendly UI hint - it tells an employer why the form
// won't submit, before a round trip. The REAL gate is the CHECK constraint
// `jobs_pay_per_hour_min_wage` in supabase_migration_01.sql, because a client
// check is only a suggestion to anyone holding the anon key.
// When the Minimum Wages Order changes, update BOTH.
object FairWage {
    const val MIN_HOURLY_RM = 8.72

    // Why: keep the wage rule in ONE place,
    // so UI and ViewModel never hardcode numbers.
    fun isFair(payPerHour: Double): Boolean = payPerHour >= MIN_HOURLY_RM
}
