package com.pnj.presensi.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pnj.presensi.databinding.ActivityLoginBinding
import com.pnj.presensi.network.RetrofitServer
import com.pnj.presensi.ui.home.HomeActivity
import com.pnj.presensi.utils.Status
import com.wajahatkarim3.easyvalidation.core.view_ktx.validator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val service = RetrofitServer.apiRequest

        binding.btnLogin.setOnClickListener {
            if (validateInput()) {
                val nip: String = binding.edtNip.text.toString().trim()
                val password: String = binding.edtPassword.text.toString().trim()

                CoroutineScope(Dispatchers.IO).launch {
                    val response = service.loginUser(nip, password)
                    withContext(Dispatchers.Main) {
                        try {
                            if (response.isSuccessful) {
                                val pegawaiResponse = response.body()
                                if (pegawaiResponse?.status == Status.SUCCESS.toString()) {
                                    startActivity(
                                        Intent(
                                            this@LoginActivity,
                                            HomeActivity::class.java
                                        )
                                    )
                                    finish()
                                } else if (pegawaiResponse?.status == Status.FAILURE.toString()) {
                                    Toast.makeText(
                                        this@LoginActivity,
                                        "Akun tidak ditemukan",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Error: ${response.code()}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: HttpException) {
                            Toast.makeText(
                                this@LoginActivity,
                                "Exception ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: Throwable) {
                            Toast.makeText(
                                this@LoginActivity,
                                "Ooops: Something else went wrong",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
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
}