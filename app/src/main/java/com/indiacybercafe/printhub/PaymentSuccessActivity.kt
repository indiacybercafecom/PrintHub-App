package com.indiacybercafe.printhub

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.indiacybercafe.printhub.databinding.ActivityPaymentSuccessBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PaymentSuccessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentSuccessBinding
    private var pulseAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            enableEdgeToEdge()
            super.onCreate(savedInstanceState)
            Log.e("SUCCESS_DEBUG", "onCreate")
            
            binding = ActivityPaymentSuccessBinding.inflate(layoutInflater)
            setContentView(binding.root)

            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }

            // Initialize Success Icon
            Log.e("SUCCESS_DEBUG", "setImage")
            binding.ivSuccess.apply {
                setImageResource(R.drawable.icon_payment_success)
                visibility = View.VISIBLE
                alpha = 0f
                scaleX = 0f
                scaleY = 0f
            }
            
            Log.e("SUCCESS_DEBUG", "playAnimation")
            binding.ivSuccess.post {
                playSuccessAnimation()
            }

            val orderId = intent.getStringExtra("orderId") ?: "N/A"
            val amount = intent.getDoubleExtra("amount", 0.0)
            val method = intent.getStringExtra("method") ?: "Online"

            binding.tvOrderId.text = if (orderId.startsWith("#")) orderId else "#$orderId"
            binding.tvAmount.text = "₹${String.format(Locale.getDefault(), "%.2f", amount)}"
            binding.tvMethod.text = method

            val sdfDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val sdfTime = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val now = Date()
            
            binding.tvDate.text = sdfDate.format(now)
            binding.tvTime.text = sdfTime.format(now)

            binding.btnTrackOrder.setOnClickListener {
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("navigate_to", "orders")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }

            binding.btnGoHome.setOnClickListener {
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
        } catch (e: Exception) {
            Log.e("SUCCESS_PAGE", "Error in onCreate: ${e.message}", e)
            // Fallback: Go home if screen fails to load
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        }
    }

    private fun playSuccessAnimation() {
        Log.e("SUCCESS_DEBUG", "playSuccessAnimation entered")
        val view = binding.ivSuccess
        
        // STEP 1 & 2: Entry Animation with rotation and pop
        Log.e("SUCCESS_DEBUG", "rotationStart")
        
        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .rotationBy(720f)
            .setDuration(800)
            .setInterpolator(OvershootInterpolator(4f))
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    startPulseAnimation()
                }
            })
            .start()
    }

    private fun startPulseAnimation() {
        Log.e("SUCCESS_DEBUG", "pulseStart")
        val view = binding.ivSuccess
        
        // STEP 3: Continuous pulse using PropertyValuesHolder
        val pvhX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.08f, 1f)
        val pvhY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.08f, 1f)
        
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(view, pvhX, pvhY).apply {
            duration = 900
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    override fun onDestroy() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        super.onDestroy()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}
