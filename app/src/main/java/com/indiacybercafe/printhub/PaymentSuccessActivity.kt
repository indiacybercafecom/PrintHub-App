package com.indiacybercafe.printhub

import android.content.Intent
import android.os.Bundle
import android.util.Log
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

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            enableEdgeToEdge()
            super.onCreate(savedInstanceState)
            binding = ActivityPaymentSuccessBinding.inflate(layoutInflater)
            setContentView(binding.root)

            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
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

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}
