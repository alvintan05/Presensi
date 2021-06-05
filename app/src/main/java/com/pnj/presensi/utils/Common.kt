package com.pnj.presensi.utils

import android.app.ProgressDialog
import android.content.Context

object Common {
    fun createProgressDialog(context: Context): ProgressDialog {
        val progressDialog = ProgressDialog(context)
        progressDialog.setMessage("Harap tunggu sebentar...")
        progressDialog.setCancelable(false)
        return progressDialog
    }
}