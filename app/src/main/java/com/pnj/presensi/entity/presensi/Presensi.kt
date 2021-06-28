package com.pnj.presensi.entity.presensi

import android.os.Parcel
import android.os.Parcelable

data class Presensi(
    val idPresensi: Int,
    val idPegawai: Int,
    val tanggal: String?,
    val jamDatang: String?,
    val jamPulang: String?,
    val lokasiKerja: String?,
    val aktivitasPekerjaan: String?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(idPresensi)
        parcel.writeInt(idPegawai)
        parcel.writeString(tanggal)
        parcel.writeString(jamDatang)
        parcel.writeString(jamPulang)
        parcel.writeString(lokasiKerja)
        parcel.writeString(aktivitasPekerjaan)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Presensi> {
        override fun createFromParcel(parcel: Parcel): Presensi {
            return Presensi(parcel)
        }

        override fun newArray(size: Int): Array<Presensi?> {
            return arrayOfNulls(size)
        }
    }
}