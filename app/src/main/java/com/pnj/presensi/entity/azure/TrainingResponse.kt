package com.pnj.presensi.entity.azure

import java.util.*

data class TrainingResponse(
    val status: String,
    val createdDateTime: Date,
    val lastActionDateTime: Date,
    val message: String?
)