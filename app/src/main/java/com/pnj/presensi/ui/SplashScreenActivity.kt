package com.pnj.presensi.ui

import android.app.ProgressDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pnj.presensi.databinding.ActivitySplashScreenBinding
import com.pnj.presensi.network.AzureRequest
import com.pnj.presensi.network.RetrofitServer
import com.pnj.presensi.ui.face_recognition.RecordFaceActivity
import com.pnj.presensi.utils.Common
import com.pnj.presensi.utils.PresensiDataStore
import kotlinx.coroutines.*
import retrofit2.HttpException

class SplashScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashScreenBinding
    private lateinit var progressDialog: ProgressDialog
    private lateinit var serviceAzure: AzureRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        progressDialog = Common.createProgressDialog(this)
        serviceAzure = RetrofitServer.azureRequest

        setFullScreen()

        // coroutine to hold screen several second
        lifecycleScope.launch {
            delay(3000)
            if (PresensiDataStore(this@SplashScreenActivity).isUserLoggedIn()) {
                if (PresensiDataStore(this@SplashScreenActivity).getPersonId() == "") {
                    // belum punya person group person
                    val intent = Intent(this@SplashScreenActivity, RecordFaceActivity::class.java)
                    intent.putExtra("person", false)
                    intent.putExtra("face", false)
                    startActivity(intent)
                    finish()
                } else {
                    if (PresensiDataStore(this@SplashScreenActivity).isRecordImageExists()) {
                        // sudah punya person group person dan record face
                        startActivity(
                            Intent(
                                this@SplashScreenActivity,
                                HomeActivity::class.java
                            )
                        )
                        finish()
                    } else {
                        getFaceList()
                    }
                }
            } else {
                startActivity(Intent(this@SplashScreenActivity, LoginActivity::class.java))
                finish()
            }
        }
    }

    private fun getFaceList() {
        progressDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            val response = serviceAzure.getPersonFaceList(
                "pegawai",
                PresensiDataStore(this@SplashScreenActivity).getPersonId()
            )
            withContext(Dispatchers.Main) {
                try {
                    if (response.isSuccessful) {
                        progressDialog.dismiss()
                        val responseData = response.body()
                        if (responseData != null) {
                            if (responseData.persistedFaceIds.isEmpty()) {
                                //apabila tidak ada face di person
                                val intent = Intent(
                                    this@SplashScreenActivity,
                                    RecordFaceActivity::class.java
                                )
                                intent.putExtra("person", true)
                                intent.putExtra("face", false)
                                intent.putExtra("record", 0)
                                startActivity(intent)
                                finish()
                            } else {
                                val total = responseData.persistedFaceIds.size
                                // sudah punya person group person dan sudah record face
                                val intent = Intent(
                                    this@SplashScreenActivity,
                                    RecordFaceActivity::class.java
                                )
                                intent.putExtra("person", true)
                                intent.putExtra("face", true)
                                intent.putExtra("fromSplash", true)
                                intent.putExtra("record", total)
                                startActivity(intent)
                                finish()
                            }
                        }
                    } else {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@SplashScreenActivity,
                            "Error: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                } catch (e: HttpException) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@SplashScreenActivity,
                        "Exception ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } catch (e: Throwable) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@SplashScreenActivity,
                        "Something else went wrong",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }
    }

    private fun setFullScreen() {
        // Set full screen splash screen
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
    }
}