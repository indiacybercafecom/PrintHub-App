package com.indiacybercafe.printhub

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.indiacybercafe.printhub.adapters.LegalAdapter
import com.indiacybercafe.printhub.databinding.ActivityLegalBinding
import com.indiacybercafe.printhub.models.LegalItem

class LegalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLegalBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLegalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val items = listOf(
            LegalItem(
                R.drawable.ic_shield_lock,
                "Privacy Policy",
                "Learn how we collect, use and protect your data.",
                "https://printhub.indiacybercafe.com/legal-and-information/privacy-policy.html"
            ),
            LegalItem(
                R.drawable.ic_legal,
                "Terms of Service",
                "Rules and conditions for using ICC PrintHub.",
                "https://printhub.indiacybercafe.com/legal-and-information/terms-of-service.html"
            ),
            LegalItem(
                R.drawable.ic_voucher,
                "Returns & Refunds",
                "Cancellation, refund and return policies.",
                "https://printhub.indiacybercafe.com/legal-and-information/returns-and-refunds.html"
            ),
            LegalItem(
                R.drawable.ic_account,
                "Delete Account",
                "Request account and personal data deletion.",
                "https://printhub.indiacybercafe.com/legal-and-information/delete-account.html"
            ),
            LegalItem(
                R.drawable.ic_shield_lock,
                "File Privacy & Retention",
                "• Uploaded files are used only for printing.\n• Files are automatically deleted after successful delivery.\n• Certain transaction records may be retained for legal and accounting purposes.",
                null,
                false
            )
        )

        binding.rvLegal.layoutManager = LinearLayoutManager(this)
        binding.rvLegal.adapter = LegalAdapter(items) { item ->
            item.url?.let { openLegalPage(item.title, it) }
        }
    }

    private fun openLegalPage(title: String, url: String) {
        val intent = Intent(this, WebViewActivity::class.java).apply {
            putExtra("title", title)
            putExtra("url", url)
        }
        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
