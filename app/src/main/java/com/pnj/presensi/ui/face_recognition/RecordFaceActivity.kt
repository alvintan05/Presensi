package com.pnj.presensi.ui.face_recognition

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pnj.presensi.databinding.ActivityRecordFaceBinding

class RecordFaceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecordFaceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordFaceBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
    }
}