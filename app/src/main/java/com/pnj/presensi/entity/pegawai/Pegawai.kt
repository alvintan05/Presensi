package com.pnj.presensi.entity.pegawai

data class Pegawai(
    val idPegawai: Int,
    val idBagian: Int,
    val idGroup: Int,
    val nip: String,
    val nama: String,
    val status: String,
    val namaBagian: String,
    val namaGroup: String
)