package com.pnj.presensi.entity.azure

data class PersonGroupPerson(
	val name: String,
	val personId: String,
	val persistedFaceIds: List<String>
)

