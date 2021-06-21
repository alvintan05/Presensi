package com.pnj.presensi.ui.riwayat

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.pnj.presensi.databinding.ActivityRiwayatBinding
import com.pnj.presensi.entity.presensi.Presensi
import com.pnj.presensi.network.ApiRequest
import com.pnj.presensi.network.RetrofitServer
import com.pnj.presensi.ui.adapter.RiwayatAdapter
import com.pnj.presensi.utils.Common
import com.pnj.presensi.utils.PresensiDataStore
import com.pnj.presensi.utils.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.text.DateFormatSymbols
import java.util.*

class RiwayatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRiwayatBinding
    private lateinit var adapter: RiwayatAdapter
    private lateinit var service: ApiRequest

    private val calendar = Calendar.getInstance(Locale("id", "ID"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRiwayatBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Riwayat Presensi"

        val listYear = listYear()
        service = RetrofitServer.apiRequest

        setUpRecyclerView()

        ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listYear
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            binding.spinnerYear.adapter = adapter
            binding.spinnerYear.prompt = "Pilih Tahun"
            binding.spinnerYear.setSelection(0)
        }

        binding.spinnerYear.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (parent != null && position != 0) {
                    setUpSpinnerMonth(parent.getItemAtPosition(position) as String)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

        }

        binding.buttonSearch.setOnClickListener {

            if (binding.spinnerYear.selectedItemPosition > 0) {
                val year = binding.spinnerYear.selectedItem.toString()
                val month = binding.spinnerMonth.selectedItemPosition + 1
                val monthName = binding.spinnerMonth.selectedItem.toString()

                getPresensiData(month, year, monthName)
            } else {
                Toast.makeText(this, "Pilih Tahun", Toast.LENGTH_SHORT).show()
            }

        }

        adapter.setOnClick(object : RiwayatAdapter.OnItemClickListener {
            override fun onItemClick(v: View, presensi: Presensi) {
                val intent = Intent(this@RiwayatActivity, DetailRiwayatActivity::class.java)
                intent.putExtra("presensi", presensi)
                startActivity(intent)
            }

        })

    }

    private fun setUpRecyclerView() {
        adapter = RiwayatAdapter()
        binding.rvPresensi.layoutManager = LinearLayoutManager(this)
        binding.rvPresensi.setHasFixedSize(true)
        binding.rvPresensi.adapter = adapter
    }

    private fun listYear(): Array<String> {
        var years = arrayOf<String>()
        val yearsTitle = arrayOf("Tahun")
        val thisYear = calendar.get(Calendar.YEAR)
        for (i in 2000..thisYear) {
            val year = i.toString()
            years += year
        }
        return yearsTitle + years
    }

    private fun setUpSpinnerMonth(selectedYear: String) {
        val converterMonthName = DateFormatSymbols(Locale("id", "ID")).months
        var months = arrayOf<String>()
        if (calendar.get(Calendar.YEAR) == selectedYear.toInt()) {
            for (i in 0..calendar.get(Calendar.MONTH)) {
                months += converterMonthName[i]
            }
        } else {
            for (i in 1..12) {
                months += converterMonthName[i - 1]
            }
        }

        ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            months
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            binding.spinnerMonth.adapter = adapter
        }

        binding.spinnerMonth.prompt = "Pilih Bulan"
        binding.spinnerMonth.setSelection(0)
    }

    private fun getPresensiData(month: Int, year: String, monthName: String) {
        val progressDialog = Common.createProgressDialog(this)
        progressDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            val idPegawai = PresensiDataStore(this@RiwayatActivity).getIdPegawai()
            val response = service.getPresensiByMonth(month, year, idPegawai)
            withContext(Dispatchers.Main) {
                try {
                    if (response.isSuccessful) {
                        progressDialog.dismiss()
                        val presensiResponse = response.body()
                        if (presensiResponse?.status == Status.SUCCESS.toString()) {
                            adapter.setList(presensiResponse.data)
                            adapter.notifyDataSetChanged()
                            binding.rvPresensi.visibility = View.VISIBLE
                            binding.tvInfo.visibility = View.GONE
                        } else if (presensiResponse?.status == Status.FAILURE.toString()) {
                            binding.rvPresensi.visibility = View.GONE
                            binding.tvInfo.visibility = View.VISIBLE
                            binding.tvInfo.text =
                                "Data Presensi Bulan $monthName Tahun $year Tidak Ditemukan"
                        }
                    } else {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@RiwayatActivity,
                            "Error: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: HttpException) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@RiwayatActivity,
                        "Exception ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Throwable) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@RiwayatActivity,
                        "Something else went wrong",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

}