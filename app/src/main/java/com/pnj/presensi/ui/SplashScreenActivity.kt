package com.pnj.presensi.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pnj.presensi.databinding.ActivitySplashScreenBinding
import com.pnj.presensi.ui.face_recognition.RecordFaceActivity
import com.pnj.presensi.utils.PresensiDataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setFullScreen()

        // coroutine to hold screen several second
        lifecycleScope.launch {
            delay(3000)
            if (PresensiDataStore(this@SplashScreenActivity).isUserLoggedIn()) {
                if (PresensiDataStore(this@SplashScreenActivity).getPersonId() == "") {
                    // belum punya person group person
                    val intent = Intent(this@SplashScreenActivity, RecordFaceActivity::class.java)
                    intent.putExtra("person", false)
                    startActivity(intent)
                } else {
                    if (PresensiDataStore(this@SplashScreenActivity).isRecordImageExists()) {
                        // sudah punya person group person dan record face
                        startActivity(Intent(this@SplashScreenActivity, HomeActivity::class.java))
                    } else {
                        // sudah punya person group person dan belum record face
                        val intent = Intent(this@SplashScreenActivity, RecordFaceActivity::class.java)
                        intent.putExtra("person", true)
                        startActivity(intent)
                    }
                }
            } else {
                startActivity(Intent(this@SplashScreenActivity, LoginActivity::class.java))
            }
            finish()
        }
    }

    private fun setFullScreen() {
        // Set full screen splash screen
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
    }
}