package com.pnj.presensi.ui.home

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
import com.pnj.presensi.ui.location.MapsActivity
import com.pnj.presensi.ui.login.LoginActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        service = RetrofitServer.apiRequest

        lifecycleScope.launch {
            binding.tvName.text = PresensiDataStore(this@HomeActivity).getName()
            binding.tvUnit.text = PresensiDataStore(this@HomeActivity).getBagian()
            checkTodayPresensi()
        }

        binding.tvDate.text = getTodayDate()

        binding.cvDatang.setOnClickListener {
            showDialog()
        }

        binding.cvPulang.setOnClickListener {
            showDialog()
        }

        binding.cvLogout.setOnClickListener {
            lifecycleScope.launch {
                PresensiDataStore(this@HomeActivity).deleteLoginSession()
                startActivity(Intent(this@HomeActivity, LoginActivity::class.java))
                finish()
            }
        }

    }

    private fun showDialog() {
        val items = arrayOf("WFO", "WFH")
        val builder = AlertDialog.Builder(this)

        with(builder) {
            setTitle("Pilih Lokasi Kerja")
            setItems(items) { dialog, which ->
//                Toast.makeText(applicationContext, items[which] + " is clicked", Toast.LENGTH_SHORT).show()
                val bundle = Bundle()
                val sdf: SimpleDateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                bundle.putString("lokasi_kerja", items[which])
                bundle.putString("jam", sdf.format(Calendar.getInstance().time))
                val intent = Intent(this@HomeActivity, MapsActivity::class.java)
                intent.putExtras(bundle)
                startActivity(intent)
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
                                binding.tvPulang.text =
                                    "Pulang: ${presensiResponse.data.jamPulang.substring(0, 5)}"
                            }

                            binding.tvDatang.text =
                                "Datang: ${presensiResponse.data.jamDatang.substring(0, 5)}"
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
}