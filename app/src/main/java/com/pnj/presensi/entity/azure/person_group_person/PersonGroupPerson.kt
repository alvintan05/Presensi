package com.pnj.presensi.entity.azure.person_group_person

data class PersonGroupPerson(
	val name: String,
	val personId: String,
	val persistedFaceIds: List<String>
)

