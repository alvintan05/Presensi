package com.pnj.presensi.entity.presensi

data class Presensi(
    private val idPresensi: Int,
    private val idPegawai: Int,
    private val tanggal: String,
    private val jamPulang: String,
    private val lokasiKerja: String,
    private val aktivitasPekerjaan: String,
    private val jenisTerlambat: String
)
