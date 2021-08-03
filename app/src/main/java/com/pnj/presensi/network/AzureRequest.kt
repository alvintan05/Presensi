package com.pnj.presensi.network

import android.app.Person
import com.google.gson.JsonObject
import com.pnj.presensi.entity.azure.*
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface AzureRequest {

    @GET("persongroups/{personGroupId}/persons")
    suspend fun getListPersonGroupPerson(
        @Path(value = "personGroupId") personGroupId: String
    ): Response<List<PersonGroupPerson>>

    @GET("persongroups/{personGroupId}/training")
    suspend fun getTrainingPersonGroupStatus(
        @Path(value = "personGroupId") personGroupId: String
    ): Response<TrainingResponse>

    @GET("persongroups/{personGroupId}/persons/{personId}")
    suspend fun getPersonFaceList(
        @Path(value = "personGroupId") personGroupId: String,
        @Path(value = "personId") personId: String,
    ): Response<PersonGroupPerson>

//    @Headers("Content-type: application/octet-stream")
//    @POST("detect")
//    suspend fun detectFace(@Body body: RequestBody): Response<List<FaceDetectResponse>>

    @Headers("Content-type: application/octet-stream")
    @POST("detect")
    suspend fun detectFace(
        @Body body: RequestBody,
        @Query("detectionModel") model: String = "detection_01",
        @Query("recognitionModel") recogModel: String = "recognition_04"
    ): Response<List<FaceDetectResponse>>

//    @Headers("Content-type: application/octet-stream")
//    @POST("persongroups/{personGroupId}/persons/{personId}/persistedFaces")
//    suspend fun addFaceToPerson(
//        @Path(value = "personGroupId") personGroupId: String,
//        @Path(value = "personId") personId: String,
//        @Body body: RequestBody
//    ): Response<AddFaceResponse>

    @Headers("Content-type: application/octet-stream")
    @POST("persongroups/{personGroupId}/persons/{personId}/persistedFaces")
    suspend fun addFaceToPerson(
        @Body body: RequestBody,
        @Path(value = "personGroupId") personGroupId: String,
        @Path(value = "personId") personId: String,
        @Query("detectionModel") model: String = "detection_01"
    ): Response<AddFaceResponse>

    @Headers("Content-type: application/json")
    @POST("verify")
    suspend fun verifyFaceToPerson(@Body json: VerifyBodyRequest): Response<VerifyPersonResponse>

    @POST("persongroups/{personGroupId}/persons")
    suspend fun createPersonGroupPerson(
        @Path(value = "personGroupId") personGroupId: String,
        @Body body: JsonObject
    ): Response<PersonGroupPersonCreate>

    @POST("persongroups/{personGroupId}/train")
    suspend fun trainPersonGroup(
        @Path(value = "personGroupId") personGroupId: String,
    ): Response<Void>

}