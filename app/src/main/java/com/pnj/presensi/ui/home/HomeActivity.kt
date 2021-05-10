package com.pnj.presensi.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.pnj.presensi.ui.location.MapsActivity
import com.pnj.presensi.databinding.ActivityHomeBinding
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.tvDate.text = getTodayDate()

        binding.cvDatang.setOnClickListener {
            showDialog()
        }

        binding.cvPulang.setOnClickListener {
            showDialog()
        }
    }

    private fun showDialog() {
        val items = arrayOf("WFO", "WFH")
        val builder = AlertDialog.Builder(this)

        with(builder) {
            setTitle("Pilih Lokasi Kerja")
            setItems(items) { dialog, which ->
//                Toast.makeText(applicationContext, items[which] + " is clicked", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@HomeActivity, MapsActivity::class.java))
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
}