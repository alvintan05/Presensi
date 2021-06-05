package com.pnj.presensi.ui.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.pnj.presensi.databinding.ActivityHomeBinding
import com.pnj.presensi.ui.location.MapsActivity
import com.pnj.presensi.ui.login.LoginActivity
import com.pnj.presensi.utils.PresensiDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        lifecycleScope.launch {
            binding.tvName.text = PresensiDataStore(this@HomeActivity).getName()
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