package com.pnj.presensi.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.pnj.presensi.databinding.ActivityPasswordBinding

class PasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPasswordBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
    }
}