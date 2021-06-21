package com.pnj.presensi.ui.riwayat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pnj.presensi.databinding.ActivityDetailRiwayatBinding
import com.pnj.presensi.entity.presensi.Presensi
import java.text.SimpleDateFormat
import java.util.*

class DetailRiwayatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailRiwayatBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailRiwayatBinding.inflate(layoutInflater)
        val view = binding.root

        setContentView(view)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Detail Presensi"

        val presensi = intent.getParcelableExtra<Presensi>("presensi") as Presensi
        setUpViewWithData(presensi)
    }

    private fun setUpViewWithData(presensi: Presensi) {
        binding.tvTanggal.text = presensi.tanggal?.let { convertDate(it) }
        binding.tvLokasi.text = presensi.lokasiKerja
        binding.tvDatang.text = presensi.jamDatang
        binding.tvPulang.text = presensi.jamPulang
        binding.tvAktivitas.text = presensi.aktivitasPekerjaan
    }

    private fun convertDate(date: String): String {
        val patternFromApi = "yyy-MM-dd"
        val pattern = "EEEE, dd MMMM yyyy"
        val locale = Locale("id", "ID")
        val formatter = SimpleDateFormat(pattern, locale)

        val dateConvert = SimpleDateFormat(patternFromApi, locale).parse(date)

        return formatter.format(dateConvert)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}