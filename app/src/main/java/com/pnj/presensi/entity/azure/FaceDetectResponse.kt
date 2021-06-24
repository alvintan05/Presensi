package com.pnj.presensi.entity.azure

data class FaceDetectResponse(
    val faceId: String,
    val faceRectangle: FaceRectangle
)
