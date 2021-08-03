package com.pnj.presensi.ui.face_recognition

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.JsonObject
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.PictureResult
import com.pnj.presensi.R
import com.pnj.presensi.databinding.CustomAlertInstructionBinding
import com.pnj.presensi.databinding.CustomAlertRecordFaceBinding
import com.pnj.presensi.databinding.LayoutCameraBinding
import com.pnj.presensi.entity.azure.FaceRectangle
import com.pnj.presensi.network.AzureRequest
import com.pnj.presensi.network.RetrofitServer
import com.pnj.presensi.ui.HomeActivity
import com.pnj.presensi.utils.Common
import com.pnj.presensi.utils.PresensiDataStore
import kotlinx.coroutines.*
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import retrofit2.HttpException
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class RecordFaceActivity : AppCompatActivity() {

    private lateinit var binding: LayoutCameraBinding
    private lateinit var serviceAzure: AzureRequest
    private lateinit var progressDialog: ProgressDialog
    private var isManage = false
    private var isFaceRecorded = false
    private var totalFaces = 0
    private var recordFaces = 0

    private lateinit var outputStream: FileOutputStream
    //    private val RC_STORAGE_PERM = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutCameraBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.camera.setLifecycleOwner(this)
        serviceAzure = RetrofitServer.azureRequest
        progressDialog = Common.createProgressDialog(this)

        //        EasyPermissions.requestPermissions(
//            this,
//            getString(R.string.rationale_location),
//            RC_STORAGE_PERM,
//            Manifest.permission.WRITE_EXTERNAL_STORAGE
//        )

        supportActionBar?.title = "Tambah Data Wajah"

        binding.tvDataWajah.visibility = View.VISIBLE

        binding.camera.addCameraListener(object : CameraListener() {
            override fun onPictureTaken(result: PictureResult) {
                result.toBitmap { bitmap ->
                    if (bitmap != null) {
                        val stream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                        val byte = stream.toByteArray()
                        //saveImageLocal(byte)
                        detectFace(byte, bitmap)
                    }
                }
            }
        })

        binding.fabPicture.setOnClickListener {
            binding.camera.takePicture()
        }

        val isFromSplash = intent.getBooleanExtra("fromSplash", false)
        val isPersonCreated = intent.getBooleanExtra("person", false)
        isFaceRecorded = intent.getBooleanExtra("face", false)
        recordFaces = intent.getIntExtra("record", 0)
        isManage = intent.getBooleanExtra("manage", false)

        if (!isPersonCreated) {
            createPersonGroupPerson()
        } else {
            getFaceList()
            if (isManage) {
                showDialogInstruction()
            } else {
                if (isFaceRecorded) {
                    // sudah ada record
                    if (recordFaces < 3) {
                        showDialogRecord(getString(R.string.dialog_face_record_not_complete))
                    } else {
                        if (recordFaces >= 3 && isFromSplash) {
                            trainPersonGroup()
                        } else {
                            // record lebih atau sama dengan 3
                            showDialogInstruction()
                        }
                    }
                } else {
                    // masih kosong face nya
                    showDialogRecord(getString(R.string.dialog_no_face_record))
                }
            }
        }
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

    private fun createPersonGroupPerson() {
        progressDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            val body = JsonObject()
            body.addProperty(
                "name",
                Common.createNameForAzure(
                    PresensiDataStore(this@RecordFaceActivity).getName(),
                    PresensiDataStore(this@RecordFaceActivity).getNip()
                )
            )
            val response = serviceAzure.createPersonGroupPerson("pegawai", body)
            withContext(Dispatchers.Main) {
                try {
                    if (response.isSuccessful) {
                        progressDialog.dismiss()
                        val responseData = response.body()
                        if (responseData != null) {
                            PresensiDataStore(this@RecordFaceActivity).savePersonId(responseData.personId)
                            Toast.makeText(
                                this@RecordFaceActivity,
                                "Success create person",
                                Toast.LENGTH_SHORT
                            ).show()

                            showDialogRecord(getString(R.string.dialog_no_face_record))
                        }
                    } else {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@RecordFaceActivity,
                            "Error: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: HttpException) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@RecordFaceActivity,
                        "Exception ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Throwable) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@RecordFaceActivity,
                        "Something else went wrong",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun detectFace(byteArray: ByteArray, bitmap: Bitmap) {
        val requestBody: RequestBody = object : RequestBody() {
            override fun contentType(): MediaType? {
                return MediaType.parse("application/octet-stream")
            }

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                sink.write(byteArray)
            }
        }

        progressDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            val response = serviceAzure.detectFace(requestBody)
            withContext(Dispatchers.Main) {
                try {
                    if (response.isSuccessful) {
                        val data = response.body()
                        progressDialog.dismiss()
                        val faceRectangle: FaceRectangle? = data?.get(0)?.faceRectangle
                        if (faceRectangle != null) {
                            val croppedByteArray = Common.cropImage(bitmap, faceRectangle)
                            //saveImageLocal(byteArray)
                            //saveImageLocal(croppedByteArray)
                            addFaceToPerson(croppedByteArray)
                        }
                    } else {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@RecordFaceActivity,
                            "Wajah tidak terdeteksi, harap coba lagi",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: HttpException) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@RecordFaceActivity,
                        "Exception ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Throwable) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@RecordFaceActivity,
                        "Wajah tidak terdeteksi, harap coba lagi",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun addFaceToPerson(byteArray: ByteArray) {
        progressDialog.show()
        val requestBody: RequestBody = object : RequestBody() {
            override fun contentType(): MediaType? {
                return MediaType.parse("application/octet-stream")
            }

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                sink.write(byteArray)
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            val response = serviceAzure.addFaceToPerson(
                requestBody,
                "pegawai",
                PresensiDataStore(this@RecordFaceActivity).getPersonId()
            )
            withContext(Dispatchers.Main) {
                try {
                    if (response.isSuccessful) {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@RecordFaceActivity,
                            "Wajah berhasil ditambahkan",
                            Toast.LENGTH_SHORT
                        ).show()
                        getFaceList()
                        recordFaces++
                        showDialog()
                    } else {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@RecordFaceActivity,
                            "Error: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: HttpException) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@RecordFaceActivity,
                        "Exception ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Throwable) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@RecordFaceActivity,
                        "Something else went wrong",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun getFaceList() {
        progressDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            val response = serviceAzure.getPersonFaceList(
                "pegawai",
                PresensiDataStore(this@RecordFaceActivity).getPersonId()
            )
            withContext(Dispatchers.Main) {
                try {
                    if (response.isSuccessful) {
                        progressDialog.dismiss()
                        val responseData = response.body()
                        if (responseData != null) {
                            totalFaces = responseData.persistedFaceIds.size
                            binding.tvDataWajah.text = "Data Wajah Saat Ini: $totalFaces"
                        }
                    } else {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@RecordFaceActivity,
                            "Error: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: HttpException) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@RecordFaceActivity,
                        "Exception ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Throwable) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@RecordFaceActivity,
                        "Something else went wrong",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showDialog() {
        val builder = AlertDialog.Builder(this@RecordFaceActivity)
        builder.setCancelable(false)

        if (isManage) {
            builder.setMessage("Apakah anda ingin menambahkan wajah lagi?")

            builder.setPositiveButton("Iya") { dialog, which ->
                dialog.dismiss()
            }

            builder.setNegativeButton("Tidak") { dialog, which ->
                trainPersonGroup()
            }
        } else {
            if (recordFaces < 3) {
                builder.setMessage("Data wajah berhasil disimpan. Lakukan penambahan minimal 3 kali")

                builder.setPositiveButton("Lanjutkan") { dialog, which ->
                    dialog.dismiss()
                }
            } else {
                builder.setMessage("Apakah anda ingin menambahkan wajah lagi?")

                builder.setPositiveButton("Iya") { dialog, which ->
                    dialog.dismiss()
                }

                builder.setNegativeButton("Tidak") { dialog, which ->
                    trainPersonGroup()
                }
            }
        }

        builder.show()
    }

    private fun showDialogRecord(message: String) {
        val binding = CustomAlertRecordFaceBinding.inflate(LayoutInflater.from(this))
        val builder = androidx.appcompat.app.AlertDialog.Builder(this).apply {
            setCancelable(false)
            setView(binding.root)
        }

        binding.tvTitle.text = message

        val dialog: androidx.appcompat.app.AlertDialog = builder.create()
        dialog.show()

        binding.btnDialog.setOnClickListener {
            dialog.dismiss()
            showDialogInstruction()
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

    private fun trainPersonGroup() {
        progressDialog.setMessage("Train Data Wajah")
        progressDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            val response = serviceAzure.trainPersonGroup("pegawai")
            withContext(Dispatchers.Main) {
                try {
                    if (response.isSuccessful) {
                        progressDialog.dismiss()
                        PresensiDataStore(this@RecordFaceActivity).saveFaceSession(true)
                        withContext(Dispatchers.Main) {
                            if (!isManage) {
                                startActivity(
                                    Intent(
                                        this@RecordFaceActivity,
                                        HomeActivity::class.java
                                    )
                                )
                                finish()
                            } else {
                                finish()
                            }
                        }
                    } else {
                        progressDialog.dismiss()
                        trainPersonGroup()
                    }
                } catch (e: HttpException) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@RecordFaceActivity,
                        "Exception ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Throwable) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@RecordFaceActivity,
                        "Something else went wrong",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

//    private fun getTrainingStatus() {
//        progressDialog.setMessage("Train Data Wajah")
//        progressDialog.show()
//        CoroutineScope(Dispatchers.IO).launch {
//            val response = serviceAzure.getTrainingPersonGroupStatus("pegawai")
//            withContext(Dispatchers.Main) {
//                try {
//                    if (response.isSuccessful) {
//                        progressDialog.dismiss()
//                        val responseData = response.body()
//                        if (responseData != null) {
//                            if (responseData.status == "succeeded") {
//                                CoroutineScope(Dispatchers.IO).launch {
//                                    PresensiDataStore(this@RecordFaceActivity).saveFaceSession(true)
//                                    withContext(Dispatchers.Main) {
//                                        if (!isManage) {
//                                            startActivity(
//                                                Intent(
//                                                    this@RecordFaceActivity,
//                                                    HomeActivity::class.java
//                                                )
//                                            )
//                                            finish()
//                                        } else {
//                                            finish()
//                                        }
//                                    }
//                                }
//                            } else {
//                                getTrainingStatus()
//                            }
//                        }
//                    } else {
//                        progressDialog.dismiss()
//                        trainPersonGroup()
//                    }
//                } catch (e: HttpException) {
//                    progressDialog.dismiss()
//                    Toast.makeText(
//                        this@RecordFaceActivity,
//                        "Exception ${e.message}",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                } catch (e: Throwable) {
//                    progressDialog.dismiss()
//                    Toast.makeText(
//                        this@RecordFaceActivity,
//                        "Something else went wrong",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            }
//        }
//    }

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

}