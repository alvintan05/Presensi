package com.pnj.presensi.ui.face_recognition

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.PictureResult
import com.pnj.presensi.R
import com.pnj.presensi.databinding.CustomAlertDialogBinding
import com.pnj.presensi.databinding.LayoutCameraBinding
import com.pnj.presensi.entity.azure.VerifyBodyRequest
import com.pnj.presensi.network.ApiRequest
import com.pnj.presensi.network.AzureRequest
import com.pnj.presensi.network.RetrofitServer
import com.pnj.presensi.ui.AktivitasActivity
import com.pnj.presensi.ui.PasswordActivity
import com.pnj.presensi.utils.Common
import com.pnj.presensi.utils.PresensiDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import retrofit2.HttpException
import java.io.ByteArrayOutputStream
import java.io.IOException

class FaceRecognitionActivity : AppCompatActivity() {

    private lateinit var binding: LayoutCameraBinding
    private lateinit var service: ApiRequest
    private lateinit var lokasiKerja: String //WFO atau WFH
    private lateinit var jam: String
    private lateinit var jenis: String //Datang atau Pulang
    private lateinit var serviceAzure: AzureRequest
    private lateinit var progressDialog: ProgressDialog

    private var counterError = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutCameraBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.camera.setLifecycleOwner(this)
        service = RetrofitServer.apiRequest
        serviceAzure = RetrofitServer.azureRequest
        progressDialog = Common.createProgressDialog(this)

        // Get intent data
        val bundle = intent.extras
        jam = bundle?.getString("jam") ?: ""
        lokasiKerja = bundle?.getString("lokasi_kerja") ?: ""
        jenis = bundle?.getString("jenis") ?: ""

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Pengenalan Wajah"

        binding.camera.addCameraListener(object : CameraListener() {
            override fun onPictureTaken(result: PictureResult) {
                result.toBitmap { bitmap ->
                    if (bitmap != null) {
                        val stream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                        val byte = stream.toByteArray()
                        detectFace(byte)
                    }
                }
            }
        })

        binding.fabPicture.setOnClickListener {
            binding.camera.takePicture()
        }

    }

    private fun detectFace(datas: ByteArray) {
        progressDialog.show()
        val requestBody: RequestBody = object : RequestBody() {
            override fun contentType(): MediaType? {
                return MediaType.parse("application/octet-stream")
            }

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                sink.write(datas)
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            val response = serviceAzure.detectFace(requestBody)
            withContext(Dispatchers.Main) {
                try {
                    if (response.isSuccessful) {
                        progressDialog.dismiss()
                        val data = response.body()
                        if (!data.isNullOrEmpty()) {
                            val faceId = data[0].faceId
                            verifyFacePerson(faceId)
                        } else {
                            counterError++
                            if (counterError == 3) {
                                buildAlertMessage(4)
                            } else {
                                buildAlertMessage(2)
                            }
                        }
                    } else {
                        progressDialog.dismiss()
//                        Toast.makeText(
//                            this@FaceRecognitionActivity,
//                            "Wajah tidak terdeteksi, harap coba lagi",
//                            Toast.LENGTH_SHORT
//                        ).show()

                        counterError++
                        if (counterError == 3) {
                            buildAlertMessage(4)
                        } else {
                            buildAlertMessage(2)
                        }
                    }
                } catch (e: HttpException) {
                    progressDialog.dismiss()
//                    Toast.makeText(
//                        this@FaceRecognitionActivity,
//                        "Exception ${e.message}",
//                        Toast.LENGTH_SHORT
//                    ).show()
                    counterError++
                    if (counterError == 3) {
                        buildAlertMessage(4)
                    } else {
                        buildAlertMessage(2)
                    }
                } catch (e: Throwable) {
                    progressDialog.dismiss()
//                    Toast.makeText(
//                        this@FaceRecognitionActivity,
//                        "Wajah tidak terdeteksi, harap coba lagi",
//                        Toast.LENGTH_SHORT
//                    ).show()
                    counterError++
                    if (counterError == 3) {
                        buildAlertMessage(4)
                    } else {
                        buildAlertMessage(2)
                    }
                }
            }
        }

    }

    private fun verifyFacePerson(faceId: String) {
        progressDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            val body = VerifyBodyRequest(
                faceId,
                PresensiDataStore(this@FaceRecognitionActivity).getPersonId(),
                "test"
            )
            val response = serviceAzure.verifyFaceToPerson(body)
            withContext(Dispatchers.Main) {
                try {
                    if (response.isSuccessful) {
                        progressDialog.dismiss()
                        val data = response.body()
                        if (data != null) {
                            if (data.isIdentical && data.confidence > 0.6) {
//                                Toast.makeText(
//                                    this@FaceRecognitionActivity,
//                                    "Wajah Dikenali",
//                                    Toast.LENGTH_SHORT
//                                ).show()
                                buildAlertMessage(1)
                            } else {
//                                Toast.makeText(
//                                    this@FaceRecognitionActivity,
//                                    "Anda tidak dikenali",
//                                    Toast.LENGTH_SHORT
//                                ).show()
                                counterError++
                                if (counterError == 3) {
                                    buildAlertMessage(4)
                                } else {
                                    buildAlertMessage(3)
                                }
                            }
                        }
                    } else {
//                        Toast.makeText(
//                            this@FaceRecognitionActivity,
//                            "Gagal, Silahkan coba lagi",
//                            Toast.LENGTH_SHORT
//                        ).show()
                        progressDialog.dismiss()
                        if (counterError == 3) {
                            buildAlertMessage(4)
                        } else {
                            buildAlertMessage(3)
                        }
                    }
                } catch (e: HttpException) {
                    progressDialog.dismiss()
//                    Toast.makeText(
//                        this@FaceRecognitionActivity,
//                        "Exception ${e.message}",
//                        Toast.LENGTH_SHORT
//                    ).show()
                    if (counterError == 3) {
                        buildAlertMessage(4)
                    } else {
                        buildAlertMessage(3)
                    }
                } catch (e: Throwable) {
                    progressDialog.dismiss()
//                    Toast.makeText(
//                        this@FaceRecognitionActivity,
//                        "Something else went wrong\n${e.message}",
//                        Toast.LENGTH_SHORT
//                    ).show()
                    if (counterError == 3) {
                        buildAlertMessage(4)
                    } else {
                        buildAlertMessage(3)
                    }
                }
            }
        }
    }

    private fun intentWithData(isSuccess: Boolean) {
        val bundle = Bundle()
        bundle.putString("lokasi_kerja", lokasiKerja)
        bundle.putString("jam", jam)
        bundle.putString("jenis", jenis)

        if (isSuccess) {
            val intent = Intent(this, AktivitasActivity::class.java)
            intent.putExtras(bundle)
            startActivity(intent)
        } else {
            val intent = Intent(this, PasswordActivity::class.java)
            intent.putExtras(bundle)
            startActivity(intent)
        }
        finish()
    }

    private fun addPresensiDatang() {
        val progressDialog = Common.createProgressDialog(this)
        progressDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            val idPegawai = PresensiDataStore(this@FaceRecognitionActivity).getIdPegawai()
            val response = service.recordPresensiDatang(idPegawai, jam, lokasiKerja)
            withContext(Dispatchers.Main) {
                try {
                    if (response.isSuccessful) {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@FaceRecognitionActivity,
                            "Anda Berhasil Melakukan Presensi Datang",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    } else {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@FaceRecognitionActivity,
                            "Error: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: HttpException) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@FaceRecognitionActivity,
                        "Exception ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Throwable) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@FaceRecognitionActivity,
                        "Something else went wrong",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    /*
    * 1 = success recognize face
    * 2 = fail detect face
    * 3 = fail recognize face before 3 times
    * 4 = fail recognize face 3 times
    * */
    private fun buildAlertMessage(statusCode: Int) {
        val binding = CustomAlertDialogBinding.inflate(LayoutInflater.from(this))
        val builder = AlertDialog.Builder(this).apply {
            setCancelable(false)
            setView(binding.root)
        }

        when (statusCode) {
            1 -> {
                binding.tvTitle.text = getString(R.string.dialog_face_recog_success)
                if (jenis == "datang") binding.btnDialog.text = getString(R.string.button_send)
            }
            2 -> {
                binding.ivError.visibility = View.VISIBLE
                binding.ivSuccess.visibility = View.GONE
                binding.tvTitle.text = getString(R.string.dialog_face_detect_fail)
                binding.btnDialog.text = getString(R.string.button_try)

            }
            3 -> {
                binding.ivError.visibility = View.VISIBLE
                binding.ivSuccess.visibility = View.GONE
                binding.tvTitle.text = getString(R.string.dialog_face_recog_fail)
                binding.btnDialog.text = getString(R.string.button_try)

            }
            4 -> {
                binding.ivError.visibility = View.VISIBLE
                binding.ivSuccess.visibility = View.GONE
                binding.tvTitle.text = getString(R.string.dialog_face_recog_fail_max)
            }
        }

        val dialog: AlertDialog = builder.create()
        dialog.show()

        binding.btnDialog.setOnClickListener {
            when (statusCode) {
                1 -> {
                    if (jenis == "datang") {
                        addPresensiDatang()
                    } else if (jenis == "pulang") {
                        intentWithData(true)
                    }
                }
                2, 3 ->{
                    dialog.dismiss()
                }
                4 ->{
                    intentWithData(false)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}