package com.pnj.presensi.network

import com.pnj.presensi.entity.pegawai.PegawaiResponse
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

}