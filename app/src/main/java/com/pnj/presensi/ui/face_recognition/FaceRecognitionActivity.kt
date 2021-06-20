package com.pnj.presensi.ui.face_recognition

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pnj.presensi.databinding.ActivityFaceRecognitionBinding
import com.pnj.presensi.network.ApiRequest
import com.pnj.presensi.network.RetrofitServer
import com.pnj.presensi.ui.AktivitasActivity
import com.pnj.presensi.utils.Common
import com.pnj.presensi.utils.PresensiDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class FaceRecognitionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFaceRecognitionBinding
    private lateinit var service: ApiRequest
    private lateinit var lokasiKerja: String //WFO atau WFH
    private lateinit var jam: String
    private lateinit var jenis: String //Datang atau Pulang

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaceRecognitionBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        service = RetrofitServer.apiRequest

        // Get intent data
        val bundle = intent.extras
        jam = bundle?.getString("jam") ?: ""
        lokasiKerja = bundle?.getString("lokasi_kerja") ?: ""
        jenis = bundle?.getString("jenis") ?: ""

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Pengenalan Wajah"

        binding.btnSend.setOnClickListener {
            if (jenis == "datang") {
                addPresensiDatang()
            } else if (jenis == "pulang") {
                intentToAktivitas()
            }
        }
    }

    private fun intentToAktivitas() {
        val bundle = Bundle()
        bundle.putString("lokasi_kerja", lokasiKerja)
        bundle.putString("jam", jam)
        bundle.putString("jenis", jenis)
        val intent = Intent(this, AktivitasActivity::class.java)
        intent.putExtras(bundle)
        startActivity(intent)
        finish()
    }

    private fun addPresensiDatang() {
        val progressDialog = Common.createProgressDialog(this)
        progressDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            val idPegawai = PresensiDataStore(this@FaceRecognitionActivity).getIdPegawai()
            val response = service.recordPresensiDatang(idPegawai, jam, lokasiKerja)
            withContext(Dispatchers.Main) {
                try {
                    if (response.isSuccessful) {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@FaceRecognitionActivity,
                            "Anda Berhasil Melakukan Presensi Datang",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@FaceRecognitionActivity,
                            "Error: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: HttpException) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@FaceRecognitionActivity,
                        "Exception ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Throwable) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@FaceRecognitionActivity,
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