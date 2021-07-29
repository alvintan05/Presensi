package com.pnj.presensi.ui.riwayat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pnj.presensi.databinding.ActivityDetailRiwayatBinding
import com.pnj.presensi.entity.presensi.Presensi
import com.pnj.presensi.utils.PresensiDataStore
import kotlinx.coroutines.launch
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
        var idUnsur = 0;
        lifecycleScope.launch {
            idUnsur = PresensiDataStore(this@DetailRiwayatActivity).getUnsur()
        }

        if (idUnsur != 0 && idUnsur == 3) {
            if (presensi.idPresensiPulang != presensi.idPresensi) {
                val tanggalList = presensi.tanggal?.split(" / ")?.toTypedArray()
                val tanggalConvert = "${tanggalList?.get(0)?.let { convertDate(it) }} - " +
                        "${tanggalList?.get(1)?.let { convertDate(it) }}"
                binding.tvTanggal.text = tanggalConvert
            } else {
                binding.tvTanggal.text = presensi.tanggal?.let { convertDate(it) }
            }
        } else {
            binding.tvTanggal.text = presensi.tanggal?.let { convertDate(it) }
        }
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