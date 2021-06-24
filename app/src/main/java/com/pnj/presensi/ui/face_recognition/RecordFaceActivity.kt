package com.pnj.presensi.ui.face_recognition

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.JsonObject
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.PictureResult
import com.pnj.presensi.databinding.LayoutCameraBinding
import com.pnj.presensi.entity.azure.FaceRectangle
import com.pnj.presensi.network.AzureRequest
import com.pnj.presensi.network.RetrofitServer
import com.pnj.presensi.ui.HomeActivity
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

class RecordFaceActivity : AppCompatActivity() {

    private lateinit var binding: LayoutCameraBinding
    private lateinit var serviceAzure: AzureRequest
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutCameraBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.camera.setLifecycleOwner(this)
        serviceAzure = RetrofitServer.azureRequest
        progressDialog = Common.createProgressDialog(this)

        supportActionBar?.title = "Perekam Wajah"

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

        var isPersonCreated = intent.getBooleanExtra("person", false)

        if (!isPersonCreated) {
            createPersonGroupPerson()
        }
    }

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
            val response = serviceAzure.createPersonGroupPerson("test", body)
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
                        "Something else went wrong\n${e.message}",
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
                "test",
                PresensiDataStore(this@RecordFaceActivity).getPersonId(),
                requestBody
            )
            withContext(Dispatchers.Main) {
                try {
                    if (response.isSuccessful) {
                        progressDialog.dismiss()
                        val responseData = response.body()
                        Toast.makeText(
                            this@RecordFaceActivity,
                            "Wajah berhasil ditambahkan",
                            Toast.LENGTH_SHORT
                        ).show()
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

    private fun showDialog() {
        val builder = AlertDialog.Builder(this)
        //builder.setTitle("Androidly Alert")
        builder.setMessage("Apakah anda ingin menambahkan wajah lagi?")

        builder.setPositiveButton("Iya") { dialog, which ->
            dialog.dismiss()
        }

        builder.setNegativeButton("Tidak") { dialog, which ->
            CoroutineScope(Dispatchers.IO).launch {
                PresensiDataStore(this@RecordFaceActivity).saveFaceSession(true)
                withContext(Dispatchers.Main){
                    startActivity(Intent(this@RecordFaceActivity, HomeActivity::class.java))
                    finish()
                }
            }
        }

        builder.show()
    }

}