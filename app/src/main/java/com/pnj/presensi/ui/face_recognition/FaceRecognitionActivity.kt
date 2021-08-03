package com.pnj.presensi.ui.face_recognition

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.PictureResult
import com.pnj.presensi.R
import com.pnj.presensi.databinding.CustomAlertDialogBinding
import com.pnj.presensi.databinding.CustomAlertInstructionBinding
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
import pub.devrel.easypermissions.EasyPermissions
import retrofit2.HttpException
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class FaceRecognitionActivity : AppCompatActivity() {

    private lateinit var binding: LayoutCameraBinding
    private lateinit var service: ApiRequest
    private lateinit var lokasiKerja: String //WFO atau WFH
    private lateinit var jam: String
    private lateinit var jenis: String //Datang atau Pulang
    private lateinit var serviceAzure: AzureRequest
    private lateinit var progressDialog: ProgressDialog
    private lateinit var outputStream: FileOutputStream

    private var counterError = 0
//    private val RC_STORAGE_PERM = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutCameraBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.camera.setLifecycleOwner(this)
        service = RetrofitServer.apiRequest
        serviceAzure = RetrofitServer.azureRequest
        progressDialog = Common.createProgressDialog(this)

//        EasyPermissions.requestPermissions(
//            this,
//            getString(R.string.rationale_location),
//            RC_STORAGE_PERM,
//            Manifest.permission.WRITE_EXTERNAL_STORAGE
//        )

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
                        detectFace(byte, bitmap)
                    }
                }
            }
        })

        binding.fabPicture.setOnClickListener {
            binding.camera.takePicture()
        }

        showDialogInstruction()

    }

    // permission storage hanya untuk keperluan testing
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
//    }

    private fun detectFace(datas: ByteArray, bitmap: Bitmap) {
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
                            val croppedByteArray = Common.cropImage(bitmap, data[0].faceRectangle)
//                            saveImageLocal(datas)
//                            saveImageLocal(croppedByteArray)

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
                        counterError++
                        if (counterError == 3) {
                            buildAlertMessage(4)
                        } else {
                            buildAlertMessage(2)
                        }
                    }
                } catch (e: HttpException) {
                    progressDialog.dismiss()
                    counterError++
                    if (counterError == 3) {
                        buildAlertMessage(4)
                    } else {
                        buildAlertMessage(2)
                    }
                } catch (e: Throwable) {
                    progressDialog.dismiss()
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
                "pegawai"
            )
            val response = serviceAzure.verifyFaceToPerson(body)
            withContext(Dispatchers.Main) {
                try {
                    if (response.isSuccessful) {
                        progressDialog.dismiss()
                        val data = response.body()
                        if (data != null) {
                            if (data.isIdentical) {
                                buildAlertMessage(1)
                            } else {
                                counterError++
                                if (counterError == 3) {
                                    buildAlertMessage(4)
                                } else {
                                    buildAlertMessage(3)
                                }
                            }
                        }
                    } else {
                        progressDialog.dismiss()
                        if (counterError == 3) {
                            buildAlertMessage(4)
                        } else {
                            buildAlertMessage(3)
                        }
                    }
                } catch (e: HttpException) {
                    progressDialog.dismiss()
                    if (counterError == 3) {
                        buildAlertMessage(4)
                    } else {
                        buildAlertMessage(3)
                    }
                } catch (e: Throwable) {
                    progressDialog.dismiss()
                    if (counterError == 3) {
                        buildAlertMessage(4)
                    } else {
                        buildAlertMessage(3)
                    }
                }
            }
        }
    }

    private fun showDialogInstruction() {
        val binding = CustomAlertInstructionBinding.inflate(LayoutInflater.from(this))
        val builder = androidx.appcompat.app.AlertDialog.Builder(this).apply {
            setCancelable(false)
            setView(binding.root)
        }

        val dialog: androidx.appcompat.app.AlertDialog = builder.create()
        dialog.show()

        binding.btnDialog.setOnClickListener {
            dialog.dismiss()
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

    private fun addPresensiDatangSatpam() {
        val progressDialog = Common.createProgressDialog(this)
        progressDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            val idPegawai = PresensiDataStore(this@FaceRecognitionActivity).getIdPegawai()
            val response = service.recordPresensiDatangSatpam(idPegawai, jam, lokasiKerja)
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
                        var unsurId = 0;
                        lifecycleScope.launch {
                            unsurId = PresensiDataStore(this@FaceRecognitionActivity).getUnsur()
                        }

                        if (unsurId == 3) {
                            addPresensiDatangSatpam()
                        } else {
                            addPresensiDatang()
                        }
                    } else if (jenis == "pulang") {
                        intentWithData(true)
                    }
                }
                2, 3 -> {
                    dialog.dismiss()
                }
                4 -> {
                    intentWithData(false)
                }
            }
        }
    }

    private fun saveImageLocal(byteArray: ByteArray) {
        val filepath = Environment.getExternalStorageDirectory()
        val format = SimpleDateFormat(
            "yyyy-MM-dd-HH-mm-ss-SSS",
            Locale.US
        ).format(System.currentTimeMillis())

        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size);
        val imageFile = File("${filepath.absolutePath}/presensi/")
        imageFile.mkdir()

        val file = File(imageFile, "$format.png")
        try {
            outputStream = FileOutputStream(file)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        Toast.makeText(this, "Image saved", Toast.LENGTH_SHORT).show()

        try {
            outputStream.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }


        try {
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}