package com.pnj.presensi.ui.face_recognition

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pnj.presensi.databinding.ActivityManageFaceBinding

class ManageFaceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageFaceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageFaceBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        supportActionBar?.title = "Kelola Data Wajah"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnAdd.setOnClickListener {
            val intent = Intent(this, RecordFaceActivity::class.java)
            intent.putExtra("manage", true)
            intent.putExtra("person", true)
            startActivity(intent)
        }
        binding.btnDelete.setOnClickListener { showDialog() }
    }

    private fun showDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Konfirmasi")
        builder.setMessage("Apakah Anda yakin ingin menghapus semua data wajah Anda?")

        builder.setPositiveButton("Iya") { dialog, which ->

        }

        builder.setNegativeButton("Tidak") { dialog, which ->
            dialog.dismiss()
        }

        builder.show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}