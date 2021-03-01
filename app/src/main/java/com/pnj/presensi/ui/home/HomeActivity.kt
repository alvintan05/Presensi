package com.pnj.presensi.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pnj.presensi.MapsActivity
import com.pnj.presensi.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.cvGeofence.setOnClickListener {
            startActivity(Intent(this@HomeActivity, MapsActivity::class.java))
        }
    }
}