package com.pnj.presensi.entity.presensi

data class Presensi(
    val idPresensi: Int,
    val idPegawai: Int,
    val tanggal: String,
    val jamDatang: String,
    val jamPulang: String,
    val lokasiKerja: String,
    val aktivitasPekerjaan: String,
    val jenisTerlambat: String
)
