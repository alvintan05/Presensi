package com.pnj.presensi.network

import com.pnj.presensi.entity.azure.person_group_person.PersonGroupPerson
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface AzureRequest {

    @GET("persongroups/{personGroupId}/persons")
    suspend fun getListPersonGroupPerson(
        @Path(value = "personGroupId") personGroupId: String
    ): Response<List<PersonGroupPerson>>

}