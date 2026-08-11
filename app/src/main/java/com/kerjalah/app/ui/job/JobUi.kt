package com.kerjalah.app.ui.job

import com.kerjalah.app.data.data.FairWage
import com.kerjalah.app.data.data.Job

// [A] UI model for one job. Screen only sees ready-to-show text.
data class JobUi(
    val id: String,
    val title: String,
    val companyName: String,
    val location: String,
    val payText: String,      // e.g. "RM 10.00 / hour"
    val hoursText: String,    // e.g. "15 hrs / week"
    val isFairWage: Boolean,  // drives the green "Fair Wage" badge
    val description: String,
)

// [A] Transform: Data model -> UI model (the UDF "toUi" step).
// Why: all formatting lives here, so screens stay dumb and simple.
fun Job.toUi() = JobUi(
    id = id,
    title = title,
    companyName = companyName,
    location = location,
    payText = "RM %.2f / hour".format(payPerHour),
    hoursText = "$hoursPerWeek hrs / week",
    isFairWage = FairWage.isFair(payPerHour),
    description = description,
)
