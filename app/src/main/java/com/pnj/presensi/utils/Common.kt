package com.pnj.presensi.utils

import android.app.ProgressDialog
import android.content.Context
import android.graphics.Bitmap
import com.pnj.presensi.entity.azure.FaceRectangle
import java.io.ByteArrayOutputStream

object Common {
    fun createProgressDialog(context: Context): ProgressDialog {
        val progressDialog = ProgressDialog(context)
        progressDialog.setMessage("Harap tunggu sebentar...")
        progressDialog.setCancelable(false)
        return progressDialog
    }

    fun cropImage(bitmap: Bitmap, data: FaceRectangle): ByteArray {
        val crop =
            ImageHelper.generateFaceThumbnail(bitmap, data)

        val stream = ByteArrayOutputStream()
        crop.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val byte = stream.toByteArray()

        return byte
    }

    fun createNameForAzure(name: String, nip: String): String {
        return "${name}_${nip}"
    }
}