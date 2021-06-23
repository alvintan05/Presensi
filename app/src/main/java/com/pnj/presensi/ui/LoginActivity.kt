package com.pnj.presensi.ui

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.*
import com.pnj.presensi.databinding.ActivityLoginBinding
import com.pnj.presensi.entity.azure.person_group_person.PersonGroupPerson
import com.pnj.presensi.network.ApiRequest
import com.pnj.presensi.network.AzureRequest
import com.pnj.presensi.network.RetrofitServer
import com.pnj.presensi.utils.Common
import com.pnj.presensi.utils.PresensiDataStore
import com.pnj.presensi.utils.Status
import com.wajahatkarim3.easyvalidation.core.view_ktx.validator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var service: ApiRequest
    private lateinit var azureService: AzureRequest
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        service = RetrofitServer.apiRequest
        azureService = RetrofitServer.azureRequest
        progressDialog = Common.createProgressDialog(this)

        binding.btnLogin.setOnClickListener {
            if (validateInput()) {
                val nip: String = binding.edtNip.text.toString().trim()
                val password: String = binding.edtPassword.text.toString().trim()

                loginUser(nip, password)
            }
        }
    }

    private fun validateInput(): Boolean {
        binding.tilNip.error = ""
        binding.tilPassword.error = ""

        var nipValid = false
        var passValid = false

        binding.edtNip.validator()
            .nonEmpty()
            .onlyNumbers()
            .addErrorCallback {
                binding.tilNip.error = it
            }
            .addSuccessCallback {
                nipValid = true
            }
            .check()

        binding.edtPassword.validator()
            .nonEmpty()
            .addErrorCallback {
                binding.tilPassword.error = it
            }
            .addSuccessCallback {
                passValid = true
            }
            .check()
        return nipValid && passValid
    }

    private fun loginUser(nip: String, password: String) {
        progressDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            val response = service.loginUser(nip, password)
            withContext(Dispatchers.Main) {
                try {
                    if (response.isSuccessful) {
                        val pegawaiResponse = response.body()
                        if (pegawaiResponse?.status == Status.SUCCESS.toString()) {
                            val pegawaiData = pegawaiResponse.data
                            PresensiDataStore(this@LoginActivity).saveSessionAndData(pegawaiData)
                            checkPersonGroupPersonExists("alvina tandiardi_12345")
                        } else if (pegawaiResponse?.status == Status.FAILURE.toString()) {
                            Toast.makeText(
                                this@LoginActivity,
                                "Akun tidak ditemukan",
                                Toast.LENGTH_SHORT
                            ).show()
                            progressDialog.dismiss()
                        }
                    } else {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@LoginActivity,
                            "Error: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: HttpException) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@LoginActivity,
                        "Exception ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Throwable) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@LoginActivity,
                        "Something else went wrong",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun checkPersonGroupPersonExists(personName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = azureService.getListPersonGroupPerson("test")
            withContext(Dispatchers.Main) {
                try {
                    if (response.isSuccessful) {
                        progressDialog.dismiss()
                        val responseData = response.body()
                        val person: PersonGroupPerson? =
                            responseData?.find { it.name == personName }
                        if (person != null) {
                            if (person.persistedFaceIds.isEmpty()) {
                                //apabila tidak ada face di person
                                PresensiDataStore(this@LoginActivity).savePersonId(person.personId)
                            } else {
                                //apabila data lengkap
                                PresensiDataStore(this@LoginActivity).savePersonIdAndFaceSession(
                                    person.personId,
                                    true
                                )
                                startActivity(
                                    Intent(
                                        this@LoginActivity,
                                        HomeActivity::class.java
                                    )
                                )
                                finish()
                            }
                        } else {
                            Toast.makeText(
                                this@LoginActivity,
                                "Person Belum Ada",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@LoginActivity,
                            "Error: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: HttpException) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@LoginActivity,
                        "Exception ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Throwable) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@LoginActivity,
                        "Something else went wrong",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}