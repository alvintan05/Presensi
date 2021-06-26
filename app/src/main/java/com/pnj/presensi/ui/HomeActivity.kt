package com.pnj.presensi.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pnj.presensi.databinding.ActivityHomeBinding
import com.pnj.presensi.entity.presensi.Presensi
import com.pnj.presensi.network.ApiRequest
import com.pnj.presensi.network.RetrofitServer
import com.pnj.presensi.ui.face_recognition.FaceRecognitionActivity
import com.pnj.presensi.ui.face_recognition.RecordFaceActivity
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
            if (datangStatus) {
                Toast.makeText(this, "Anda sudah melakukan presensi datang", Toast.LENGTH_SHORT)
                    .show()
            } else {
                showDialog("datang")
            }
        }

        binding.cvPulang.setOnClickListener {
            if (pulangStatus) {
                Toast.makeText(this, "Anda sudah melakukan presensi pulang", Toast.LENGTH_SHORT)
                    .show()
            } else {
                showDialog("pulang")
            }
        }

        binding.cvRiwayat.setOnClickListener {
            startActivity(Intent(this, RiwayatActivity::class.java))
        }

        binding.cvProfil.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        binding.cvKelola.setOnClickListener {
            val intent = Intent(this, RecordFaceActivity::class.java)
            intent.putExtra("manage", true)
            intent.putExtra("person", true)
            startActivity(intent)
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
                            updateViewFromData(true, presensiResponse.data)
                        } else if (presensiResponse?.status == Status.FAILURE.toString()) {
                            updateViewFromData(false, presensiResponse.data)
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

    private fun updateViewFromData(status: Boolean, data: Presensi) {
        if (status) {
            if (data.jamDatang != null && data.jamPulang != null) {
                binding.ivDatangNo.visibility = View.GONE
                binding.ivPulangNo.visibility = View.GONE

                binding.tvDatang.text = "Datang: ${data.jamDatang}"
                binding.tvPulang.text = "Pulang: ${data.jamPulang}"

                datangStatus = true
                pulangStatus = true

            } else if (data.jamDatang != null) {
                binding.tvDatang.text = "Datang: ${data.jamDatang}"
                binding.ivDatangNo.visibility = View.GONE
                binding.ivPulangNo.visibility = View.VISIBLE

                datangStatus = true
                pulangStatus = false
            } else {
                binding.tvPulang.text = "Pulang: ${data.jamPulang}"
                binding.ivDatangNo.visibility = View.VISIBLE
                binding.ivPulangNo.visibility = View.GONE

                datangStatus = false
                pulangStatus = true
            }
        } else {
            //kondisi belum presensi datang dan pulang
            binding.ivDatangNo.visibility = View.VISIBLE
            binding.ivPulangNo.visibility = View.VISIBLE

            datangStatus = false
            pulangStatus = false
        }
    }
}