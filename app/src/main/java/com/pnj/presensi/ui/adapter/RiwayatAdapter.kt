package com.pnj.presensi.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pnj.presensi.databinding.ItemRiwayatBinding
import com.pnj.presensi.entity.presensi.Presensi
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class RiwayatAdapter : RecyclerView.Adapter<RiwayatAdapter.ViewHolder>() {

    private var idUnsur = 0;
    private var listPresensi: List<Presensi> = ArrayList()
    private var mOnItemClickListener: OnItemClickListener? = null

    fun setList(list: List<Presensi>) {
        this.listPresensi = list
    }

    fun setOnClick(onItemClickListener: OnItemClickListener) {
        mOnItemClickListener = onItemClickListener
    }

    fun setIdUnsur(idUnsur: Int) {
        this.idUnsur = idUnsur
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
                if (idUnsur != 0 && idUnsur == 3) {
                    if (this.idPresensiPulang != this.idPresensi) {
                        val tanggalList = this.tanggal?.split(" / ")?.toTypedArray()
                        val tanggalConvert = "${tanggalList?.get(0)?.let { convertDate(it) }} - " +
                                "${tanggalList?.get(1)?.let { convertDate(it) }}"
                        binding.tvDate.text = tanggalConvert
                    } else {
                        binding.tvDate.text = this.tanggal?.let { convertDate(it) }
                    }
                } else {
                    binding.tvDate.text = this.tanggal?.let { convertDate(it) }
                }

                binding.tvJamDatang.text = this.jamDatang
                binding.tvJamPulang.text = this.jamPulang
                binding.tvLokasi.text = this.lokasiKerja

                binding.root.setOnClickListener {
                    mOnItemClickListener?.onItemClick(it, this)
                }
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

    interface OnItemClickListener {
        fun onItemClick(v: View, presensi: Presensi)
    }
}