package com.indiacybercafe.printhub

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.indiacybercafe.printhub.databinding.ActivityPaymentFailedBinding

class PaymentFailedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentFailedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentFailedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnRetry.setOnClickListener {
            finish()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}
