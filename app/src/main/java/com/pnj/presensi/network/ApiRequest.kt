package com.pnj.presensi.network

import com.pnj.presensi.entity.pegawai.PegawaiResponse
import com.pnj.presensi.entity.presensi.Presensi
import com.pnj.presensi.entity.presensi.PresensiResponse
import retrofit2.Response
import retrofit2.http.*

interface  ApiRequest {

    @FormUrlEncoded
    @POST("account")
    suspend fun loginUser(
        @Field(value = "username") username: String,
        @Field(value = "password") password: String
    ): Response<PegawaiResponse>

    @FormUrlEncoded
    @POST("presensi/datang")
    suspend fun recordPresensiDatang(
        @Field(value="id_pegawai") idPegawai: Int,
        @Field(value = "jam_datang") jamDatang: String,
        @Field(value="lokasi_kerja") lokasiKerja: String
    ): Response<Presensi>

    @FormUrlEncoded
    @POST("presensi/pulang")
    suspend fun recordPresensiPulang(
        @Field("id_pegawai") idPegawai: Int,
        @Field("jam_pulang") jamPulang: String,
        @Field("lokasi_kerja") lokasiKerja: String,
        @Field("aktivitas") aktivitas: String
    ): Response<Presensi>

    @GET("presensi/today")
    suspend fun getTodayPresensi(@Query("id_pegawai") idPegawai: Int): Response<PresensiResponse>

}