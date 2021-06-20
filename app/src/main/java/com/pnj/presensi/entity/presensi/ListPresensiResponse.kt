package com.pnj.presensi.entity.presensi

data class ListPresensiResponse(
    val status: String,
    val data: List<Presensi>
)