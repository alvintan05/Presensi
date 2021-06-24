package com.pnj.presensi.entity.azure

data class VerifyPersonResponse(
    val isIdentical: Boolean,
    val confidence: Float
)