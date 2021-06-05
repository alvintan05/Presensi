package com.pnj.presensi.network

import com.pnj.presensi.entity.pegawai.PegawaiResponse
import com.pnj.presensi.entity.presensi.Presensi
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ApiRequest {

    @FormUrlEncoded
    @POST("account")
    suspend fun loginUser(
        @Field(value = "username") username: String,
        @Field(value = "password") password: String
    ): Response<PegawaiResponse>

    @FormUrlEncoded
    @POST("presensi/datang")
    suspend fun recordPresensiDatang(
        @Field(value="id_pegawai") idPegawai: String,
        @Field(value = "jam_datang") jamDatang: String,
        @Field(value="lokasi_kerja") lokasiKerja: String
    ): Response<Presensi>

}