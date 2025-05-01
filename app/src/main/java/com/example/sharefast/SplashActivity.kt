package com.example.sharefast

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val splashIcon = findViewById<ImageView>(R.id.splashIcon)
        val rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate)
        
        splashIcon.startAnimation(rotateAnimation)

        // Navigate to MainActivity after animation
        rotateAnimation.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}

            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                finish()
            }

            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })
    }
}