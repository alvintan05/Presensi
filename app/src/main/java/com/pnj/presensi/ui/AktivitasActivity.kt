package com.pnj.presensi.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pnj.presensi.databinding.ActivityAktivitasBinding
import com.pnj.presensi.network.ApiRequest
import com.pnj.presensi.network.RetrofitServer
import com.pnj.presensi.utils.Common
import com.pnj.presensi.utils.PresensiDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class AktivitasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAktivitasBinding
    private lateinit var service: ApiRequest
    private lateinit var lokasiKerja: String //WFO atau WFH
    private lateinit var jam: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAktivitasBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        service = RetrofitServer.apiRequest

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Aktivitas Pekerjaan"

        // Get intent data
        val bundle = intent.extras
        jam = bundle?.getString("jam") ?: ""
        lokasiKerja = bundle?.getString("lokasi_kerja") ?: ""

        binding.btnSend.setOnClickListener {
            addPresensiPulang()
        }
    }

    private fun addPresensiPulang() {
        val progressDialog = Common.createProgressDialog(this)
        progressDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            val idPegawai = PresensiDataStore(this@AktivitasActivity).getIdPegawai()
            val aktivitas = binding.edtAktivitas.text.toString().trim()
            val response = service.recordPresensiPulang(idPegawai, jam, lokasiKerja, aktivitas)
            withContext(Dispatchers.Main) {
                try {
                    if (response.isSuccessful) {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@AktivitasActivity,
                            "Anda Berhasil Melakukan Presensi Pulang",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@AktivitasActivity,
                            "Error: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: HttpException) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@AktivitasActivity,
                        "Exception ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Throwable) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@AktivitasActivity,
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