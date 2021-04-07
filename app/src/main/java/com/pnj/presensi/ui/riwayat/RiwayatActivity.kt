package com.pnj.presensi.ui.riwayat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.pnj.presensi.databinding.ActivityRiwayatBinding

class RiwayatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRiwayatBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRiwayatBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
    }
}