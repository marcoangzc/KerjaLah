package com.kerjalah.app.data

// [A] Module 2 - Job data model (Data Layer).
// This is the source-of-truth shape of one job posting.
// Later Supabase maps its "jobs" table rows into this same class,
// so ViewModel and UI never change (UDF layering).
data class Job(
    val id: String,
    val employerId: String,   // who posted this job (fake id now, Supabase Auth uid later)
    val title: String,
    val companyName: String,
    val location: String,     // e.g. "Near Universiti Malaya, KL"
    val payPerHour: Double,   // in RM
    val hoursPerWeek: Int,
    val description: String,
)
