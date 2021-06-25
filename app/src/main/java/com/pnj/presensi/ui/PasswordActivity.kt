package com.pnj.presensi.ui

import android.app.ProgressDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pnj.presensi.databinding.ActivityPasswordBinding
import com.pnj.presensi.network.ApiRequest
import com.pnj.presensi.network.RetrofitServer
import com.pnj.presensi.utils.Common
import com.pnj.presensi.utils.PresensiDataStore
import com.wajahatkarim3.easyvalidation.core.view_ktx.validator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class PasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPasswordBinding
    private lateinit var service: ApiRequest
    private lateinit var progressDialog: ProgressDialog
    private lateinit var jam: String
    private lateinit var lokasiKerja: String
    private lateinit var jenis: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPasswordBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        service = RetrofitServer.apiRequest
        progressDialog = Common.createProgressDialog(this)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Verifikasi Password"

        // Get intent data
        val bundle = intent.extras
        jam = bundle?.getString("jam") ?: ""
        lokasiKerja = bundle?.getString("lokasi_kerja") ?: ""
        jenis = bundle?.getString("jenis") ?: ""


        binding.btnSend.setOnClickListener {
            if (validateInput()) {
                val password = binding.edtPassword.text.toString().trim()
                checkPasswordAccount(password)
            }
        }
    }

    private fun validateInput(): Boolean {
        binding.tilPassword.error = ""

        var passValid = false

        binding.edtPassword.validator()
            .nonEmpty()
            .addErrorCallback {
                binding.tilPassword.error = it
            }
            .addSuccessCallback {
                passValid = true
            }
            .check()
        return passValid
    }

    private fun checkPasswordAccount(password: String) {
        progressDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            val response = service.checkPasswordAccount(
                PresensiDataStore(this@PasswordActivity).getNip(),
                password
            )
            withContext(Dispatchers.Main) {
                try {
                    if (response.isSuccessful) {
                        progressDialog.dismiss()
                        val status = response.body()
                        if (status == true) {
                            addPresensiDatang()
                        } else {
                            Toast.makeText(
                                this@PasswordActivity,
                                "Password Anda Salah",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@PasswordActivity,
                            "Error: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: HttpException) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@PasswordActivity,
                        "Exception ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Throwable) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@PasswordActivity,
                        "Something else went wrong",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun addPresensiDatang() {
        val progressDialog = Common.createProgressDialog(this)
        progressDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            val idPegawai = PresensiDataStore(this@PasswordActivity).getIdPegawai()
            val response = service.recordPresensiDatang(idPegawai, jam, lokasiKerja)
            withContext(Dispatchers.Main) {
                try {
                    if (response.isSuccessful) {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@PasswordActivity,
                            "Anda Berhasil Melakukan Presensi Datang",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    } else {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@PasswordActivity,
                            "Error: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: HttpException) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@PasswordActivity,
                        "Exception ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Throwable) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@PasswordActivity,
                        "Something else went wrong",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}