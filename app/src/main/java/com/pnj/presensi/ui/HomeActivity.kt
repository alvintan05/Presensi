package com.pnj.presensi.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pnj.presensi.databinding.ActivityHomeBinding
import com.pnj.presensi.network.ApiRequest
import com.pnj.presensi.network.RetrofitServer
import com.pnj.presensi.ui.face_recognition.FaceRecognitionActivity
import com.pnj.presensi.ui.riwayat.RiwayatActivity
import com.pnj.presensi.utils.Common
import com.pnj.presensi.utils.PresensiDataStore
import com.pnj.presensi.utils.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var service: ApiRequest
    private var datangStatus = false
    private var pulangStatus = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        service = RetrofitServer.apiRequest

        lifecycleScope.launch {
            binding.tvName.text = PresensiDataStore(this@HomeActivity).getName()
            binding.tvUnit.text = PresensiDataStore(this@HomeActivity).getBagian()
        }

        binding.tvDate.text = getTodayDate()

        binding.cvDatang.setOnClickListener {
            showDialog("datang")
        }

        binding.cvPulang.setOnClickListener {
            showDialog("pulang")
        }

        binding.cvRiwayat.setOnClickListener {
            startActivity(Intent(this, RiwayatActivity::class.java))
        }

        binding.cvProfil.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        binding.cvLogout.setOnClickListener {
            lifecycleScope.launch {
                PresensiDataStore(this@HomeActivity).deleteLoginSession()
                startActivity(Intent(this@HomeActivity, LoginActivity::class.java))
                finish()
            }
        }

    }

    override fun onResume() {
        super.onResume()
        checkTodayPresensi()
        enableDisableButton()
    }

    private fun showDialog(jenis: String) {
        val items = arrayOf("WFO", "WFH")
        val builder = AlertDialog.Builder(this)

        with(builder) {
            setTitle("Pilih Lokasi Kerja")
            setItems(items) { dialog, which ->
                val bundle = Bundle()
                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                bundle.putString("lokasi_kerja", items[which])
                bundle.putString("jam", sdf.format(Calendar.getInstance().time))
                bundle.putString("jenis", jenis)

                if (items[which] == "WFO") {
                    val intent = Intent(this@HomeActivity, MapsActivity::class.java)
                    intent.putExtras(bundle)
                    startActivity(intent)
                } else if (items[which] == "WFH") {
                    val intent = Intent(this@HomeActivity, FaceRecognitionActivity::class.java)
                    intent.putExtras(bundle)
                    startActivity(intent)
                }
            }
            show()
        }
    }

    private fun getTodayDate(): String {
        return SimpleDateFormat(
            "EEEE, dd MMMM yyyy",
            Locale("in", "ID")
        ).format(Calendar.getInstance().time)
    }

    private fun checkTodayPresensi() {
        val progressDialog = Common.createProgressDialog(this)
        progressDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            val response =
                service.getTodayPresensi(PresensiDataStore(this@HomeActivity).getIdPegawai())
            withContext(Dispatchers.Main) {
                try {
                    if (response.isSuccessful) {
                        progressDialog.dismiss()
                        val presensiResponse = response.body()
                        if (presensiResponse?.status == Status.SUCCESS.toString()) {
                            if (presensiResponse.data.jamPulang == null) {
                                binding.ivPulangNo.visibility = View.VISIBLE
                                Toast.makeText(
                                    this@HomeActivity,
                                    "Anda Belum Melakukan Presensi Pulang",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                pulangStatus = true
                                binding.tvPulang.text =
                                    "Pulang: ${presensiResponse.data.jamPulang.substring(0, 5)}"
                            }
                            datangStatus = true
                            binding.tvDatang.text =
                                "Datang: ${presensiResponse.data.jamDatang?.substring(0, 5)}"
                        } else if (presensiResponse?.status == Status.FAILURE.toString()) {
                            binding.ivDatangNo.visibility = View.VISIBLE
                            binding.ivPulangNo.visibility = View.VISIBLE

                            Toast.makeText(
                                this@HomeActivity,
                                "Anda Belum Melakukan Presensi Hari Ini",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@HomeActivity,
                            "Error: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: HttpException) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@HomeActivity,
                        "Exception ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Throwable) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@HomeActivity,
                        "Something else went wrong",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun enableDisableButton() {
        if (datangStatus) {
            binding.cvDatang.isEnabled = false
            binding.cvDatang.setOnClickListener {
                Toast.makeText(this, "Anda sudah melakukan presensi datang", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            binding.cvDatang.isEnabled = true
        }

        if (pulangStatus) {
            binding.cvPulang.isEnabled = false
            binding.cvPulang.setOnClickListener {
                Toast.makeText(this, "Anda sudah melakukan presensi pulang", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            binding.cvPulang.isEnabled = true
        }
    }
}