package com.pnj.presensi.entity.azure

data class VerifyBodyRequest(
    val faceId: String,
    val personId: String,
    val personGroupId: String
)
