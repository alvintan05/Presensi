package com.pnj.presensi.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pnj.presensi.databinding.ItemRiwayatBinding
import com.pnj.presensi.entity.presensi.Presensi
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class RiwayatAdapter : RecyclerView.Adapter<RiwayatAdapter.ViewHolder>() {

    private var listPresensi: List<Presensi> = ArrayList()

    fun setList(list: List<Presensi>) {
        this.listPresensi = list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRiwayatBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            with(listPresensi[position]) {
                binding.tvDate.text = convertDate(this.tanggal)
                binding.tvJamDatang.text = this.jamDatang
                binding.tvJamPulang.text = this.jamPulang
            }
        }
    }

    override fun getItemCount(): Int = listPresensi.size

    inner class ViewHolder(val binding: ItemRiwayatBinding) : RecyclerView.ViewHolder(binding.root)

    private fun convertDate(date: String): String {
        val patternFromApi = "yyy-MM-dd"
        val pattern = "EEEE, dd MMMM yyyy"
        val locale = Locale("id", "ID")
        val formatter = SimpleDateFormat(pattern, locale)

        val dateConvert = SimpleDateFormat(patternFromApi, locale).parse(date)

        return formatter.format(dateConvert)
    }
}