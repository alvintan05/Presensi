package com.pnj.presensi.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pnj.presensi.databinding.ActivityProfileBinding
import com.pnj.presensi.entity.pegawai.Pegawai
import com.pnj.presensi.network.ApiRequest
import com.pnj.presensi.network.RetrofitServer
import com.pnj.presensi.utils.Common
import com.pnj.presensi.utils.PresensiDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var service: ApiRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Profil Pegawai"

        service = RetrofitServer.apiRequest

        getProfileData()
    }

    private fun getProfileData() {
        val progressDialog = Common.createProgressDialog(this)
        progressDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            val response =
                service.getPegawaiData(PresensiDataStore(this@ProfileActivity).getIdPegawai())
            withContext(Dispatchers.Main) {
                try {
                    if (response.isSuccessful) {
                        progressDialog.dismiss()
                        val dataPegawai = response.body()
                        if (dataPegawai != null) {
                            bindDataToView(dataPegawai)
                        } else {
                            Toast.makeText(
                                this@ProfileActivity,
                                "Data tidak ditemukan",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@ProfileActivity,
                            "Error: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: HttpException) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@ProfileActivity,
                        "Exception ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Throwable) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@ProfileActivity,
                        "Something else went wrong",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun bindDataToView(pegawai: Pegawai) {
        binding.tvNama.text = pegawai.nama
        binding.tvNip.text = pegawai.nip
        binding.tvBagian.text = pegawai.namaBagian
        binding.tvStatus.text = pegawai.status
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}