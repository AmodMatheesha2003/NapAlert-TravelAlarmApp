package com.nibm.napalert

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.logo)
        val myEventsText = findViewById<TextView>(R.id.myEventsText)
        val description = findViewById<TextView>(R.id.welcomeText)

        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_zoom)
        myEventsText.startAnimation(fadeInAnimation)

        val descriptionAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_zoom)
        description.startAnimation(descriptionAnimation)

        val logoAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_zoom)
        logo.startAnimation(logoAnimation)

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 3000)
    }
}
