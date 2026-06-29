package com.indiacybercafe.printhub

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.indiacybercafe.printhub.databinding.ActivityPaymentBinding
import com.indiacybercafe.printhub.models.OrderDraft
import com.indiacybercafe.printhub.models.OrderModel
import com.indiacybercafe.printhub.utils.GlobalUploadObserver
import com.indiacybercafe.printhub.utils.UploadManager
import com.indiacybercafe.printhub.utils.NotificationHelper
import com.indiacybercafe.printhub.utils.RazorpayConfig
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import org.json.JSONObject
import java.util.Locale

class PaymentActivity : AppCompatActivity(), PaymentResultListener {

    companion object {
        private const val TAG = "RAZORPAY"
    }

    private lateinit var binding: ActivityPaymentBinding
    private lateinit var uploadObserver: GlobalUploadObserver
    private lateinit var database: DatabaseReference
    private var orderDraft: OrderDraft? = null
    private var orderId: String? = null
    private var selectedPaymentMethod: String? = null
    
    private var isRazorpayEnabled = true
    private var isCodEnabled = true
    
    // Flags to prevent duplicate execution and crashes
    private var isProcessing = false
    private var isOrderCreated = false
    private var paymentHandled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logEvent("PAYMENT_PAGE_OPEN")
        
        try {
            enableEdgeToEdge()
            binding = ActivityPaymentBinding.inflate(layoutInflater)
            setContentView(binding.root)

            uploadObserver = GlobalUploadObserver(this)
            uploadObserver.startObserving()

            ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(0, systemBars.top, 0, 0)
                insets
            }

            ViewCompat.setOnApplyWindowInsetsListener(binding.bottomCard) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(0, 0, 0, systemBars.bottom)
                insets
            }

            orderDraft = intent.getSerializableExtra("orderDraft") as? OrderDraft
            orderId = intent.getStringExtra("orderId")

            if (orderDraft == null || orderId == null) {
                logError("Missing order details: orderDraft=$orderDraft, orderId=$orderId")
                showErrorDialog("Order details missing. Please try again.") {
                    finish()
                }
                return
            }

            database = FirebaseDatabase.getInstance().reference

            setupToolbar()
            displayOrderSummary()
            fetchPaymentSettings()
            setupClickListeners()
            
            Checkout.preload(applicationContext)
            logEvent("RAZORPAY_PRELOADED")
            
        } catch (e: Exception) {
            logError("APP_CRASH_REASON (onCreate): ${e.stackTraceToString()}")
            showErrorDialog("Failed to initialize payment page: ${e.message}")
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { 
            if (!isProcessing) {
                onBackPressedDispatcher.onBackPressed() 
            } else {
                safeToast("Please wait while we process your request")
            }
        }
    }

    private fun displayOrderSummary() {
        try {
            val draft = orderDraft ?: return
            binding.tvSummaryOrderId.text = "#$orderId"
            
            var fileCount = 0
            draft.printSets?.forEach { set ->
                fileCount += set.files?.size ?: 0
            }
            binding.tvSummaryFiles.text = "$fileCount Files"
            
            binding.tvSummaryPrintCharges.text = "₹${String.format(Locale.getDefault(), "%.2f", draft.printingCharges)}"
            binding.tvSummaryDeliveryCharges.text = "₹${String.format(Locale.getDefault(), "%.2f", draft.deliveryCharge)}"
            binding.tvSummaryDiscount.text = "-₹0.00"
            binding.tvSummaryTotal.text = "₹${String.format(Locale.getDefault(), "%.2f", draft.totalAmount)}"
        } catch (e: Exception) {
            logError("Error displaying summary: ${e.stackTraceToString()}")
        }
    }

    private fun fetchPaymentSettings() {
        database.child("paymentSettings").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    logEvent("PAYMENT_SETTINGS_MISSING")
                    return
                }

                try {
                    isRazorpayEnabled = snapshot.child("razorpayEnabled").getValue(Boolean::class.java) ?: true
                    isCodEnabled = snapshot.child("codEnabled").getValue(Boolean::class.java) ?: true
                    
                    val razorpayMessage = snapshot.child("razorpayMessage").getValue(String::class.java)
                    val codMessage = snapshot.child("codMessage").getValue(String::class.java)

                    updatePaymentMethodUI(razorpayMessage, codMessage)
                } catch (e: Exception) {
                    logError("Error parsing payment settings: ${e.stackTraceToString()}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                logError("Firebase Error: ${error.message}")
            }
        })
    }

    private fun updatePaymentMethodUI(razorpayMsg: String?, codMsg: String?) {
        try {
            if (isFinishing || isDestroyed) return

            // Razorpay UI
            if (isRazorpayEnabled) {
                binding.viewRazorpayOverlay.visibility = View.GONE
                binding.tvRazorpayUnavailable.visibility = View.GONE
                binding.tvRazorpayMessage.text = razorpayMsg ?: getString(R.string.razorpay_secure)
                binding.tvRazorpayMessage.setTextColor(getColor(R.color.success_green))
                binding.cardRazorpay.isEnabled = true
            } else {
                binding.viewRazorpayOverlay.visibility = View.VISIBLE
                binding.tvRazorpayUnavailable.visibility = View.VISIBLE
                binding.tvRazorpayMessage.text = razorpayMsg ?: "Online Payment Currently Unavailable"
                binding.tvRazorpayMessage.setTextColor(getColor(R.color.error_red))
                binding.cardRazorpay.isEnabled = false
                if (selectedPaymentMethod == "razorpay") {
                    selectedPaymentMethod = null
                    binding.ivRazorpayCheck.visibility = View.GONE
                    binding.cardRazorpay.strokeWidth = 0
                    updateProceedButton()
                }
            }

            // COD UI
            if (isCodEnabled) {
                binding.viewCodOverlay.visibility = View.GONE
                binding.tvCodUnavailable.visibility = View.GONE
                binding.tvCodMessage.text = codMsg ?: getString(R.string.cod_sub)
                binding.cardCod.isEnabled = true
            } else {
                binding.viewCodOverlay.visibility = View.VISIBLE
                binding.tvCodUnavailable.visibility = View.VISIBLE
                binding.tvCodMessage.text = codMsg ?: "Cash On Delivery Currently Unavailable"
                binding.cardCod.isEnabled = false
                if (selectedPaymentMethod == "cod") {
                    selectedPaymentMethod = null
                    binding.ivCodCheck.visibility = View.GONE
                    binding.cardCod.strokeWidth = 0
                    updateProceedButton()
                }
            }

            if (!isRazorpayEnabled && !isCodEnabled) {
                binding.tvNoPaymentAvailable.visibility = View.VISIBLE
                binding.btnProceed.isEnabled = false
            } else {
                binding.tvNoPaymentAvailable.visibility = View.GONE
            }
        } catch (e: Exception) {
            logError("Error updating UI: ${e.stackTraceToString()}")
        }
    }

    private fun setupClickListeners() {
        binding.cardRazorpay.setOnClickListener {
            if (isRazorpayEnabled && !isProcessing) {
                logEvent("RAZORPAY_SELECTED")
                selectPaymentMethod("razorpay")
            }
        }

        binding.cardCod.setOnClickListener {
            if (isCodEnabled && !isProcessing) {
                logEvent("COD_SELECTED")
                selectPaymentMethod("cod")
            }
        }

        binding.btnProceed.setOnClickListener {
            if (isProcessing || isOrderCreated) return@setOnClickListener

            when (selectedPaymentMethod) {
                "razorpay" -> {
                    logEvent("PROCEED_RAZORPAY")
                    startRazorpayPayment()
                }
                "cod" -> {
                    logEvent("COD_CLICKED")
                    finalizeCodOrder()
                }
                else -> {
                    safeToast("Please select a payment method")
                }
            }
        }
    }

    private fun selectPaymentMethod(method: String) {
        selectedPaymentMethod = method
        logEvent("PAYMENT_METHOD_SELECTED: $method")
        
        binding.ivRazorpayCheck.visibility = if (method == "razorpay") View.VISIBLE else View.GONE
        binding.cardRazorpay.strokeWidth = if (method == "razorpay") 4 else 0
        
        binding.ivCodCheck.visibility = if (method == "cod") View.VISIBLE else View.GONE
        binding.cardCod.strokeWidth = if (method == "cod") 4 else 0
        
        updateProceedButton()
    }

    private fun updateProceedButton() {
        binding.btnProceed.isEnabled = selectedPaymentMethod != null
    }

    private fun startRazorpayPayment() {
        val draft = orderDraft ?: return
        val checkout = Checkout()
        checkout.setKeyID(RazorpayConfig.KEY_ID)
        
        try {
            val options = JSONObject()
            options.put("name", RazorpayConfig.MERCHANT_NAME)
            options.put("description", "Payment for Order #$orderId")
            options.put("image", "https://s3.amazonaws.com/rzp-mobile/images/rzp.png")
            options.put("currency", "INR")
            options.put("amount", (draft.totalAmount * 100).toInt())
            
            val theme = JSONObject()
            theme.put("color", RazorpayConfig.THEME_COLOR)
            options.put("theme", theme)

            val retryObj = JSONObject()
            retryObj.put("enabled", true)
            retryObj.put("max_count", 4)
            options.put("retry", retryObj)

            checkout.open(this, options)
        } catch (e: Exception) {
            logError("Razorpay SDK Error: ${e.stackTraceToString()}")
            showErrorDialog("Razorpay could not be opened: ${e.message}")
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        runOnUiThread {
            try {
                if (paymentHandled) return@runOnUiThread
                paymentHandled = true
                
                logEvent("PAYMENT_SUCCESS: ${razorpayPaymentId ?: "NULL"}")
                saveOrderToFirebase("razorpay", "paid", null, razorpayPaymentId)
            } catch (e: Exception) {
                logError("RAZORPAY_CRASH (onPaymentSuccess): ${e.stackTraceToString()}")
            }
        }
    }

    override fun onPaymentError(code: Int, response: String?) {
        runOnUiThread {
            try {
                if (paymentHandled) return@runOnUiThread
                
                logError("PAYMENT_FAILED: code=$code, response=$response")
                val draft = orderDraft
                if (draft != null) {
                    try {
                        NotificationHelper.sendPaymentFailedNotification(this, draft.uid, orderId ?: "")
                    } catch (e: Exception) {
                        Log.e(TAG, "Notification error: ${e.message}")
                    }
                }
                
                if (code == Checkout.PAYMENT_CANCELED) {
                    safeToast("Payment cancelled by user")
                } else {
                    val intent = Intent(this, PaymentFailedActivity::class.java)
                    startActivity(intent)
                }
            } catch (e: Exception) {
                logError("RAZORPAY_CRASH (onPaymentError): ${e.stackTraceToString()}")
            }
        }
    }

    private fun finalizeCodOrder() {
        AlertDialog.Builder(this)
            .setTitle("Confirm COD Order")
            .setMessage("Are you sure you want to place this order using Cash On Delivery?")
            .setPositiveButton("Confirm") { _, _ ->
                saveOrderToFirebase("cod", "pending", null, null)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveOrderToFirebase(method: String, paymentStatus: String, rzpOrderId: String?, rzpPaymentId: String?) {
        if (isOrderCreated) {
            logEvent("ORDER_ALREADY_CREATED_SKIPPING")
            return
        }
        
        isProcessing = true
        logEvent("ORDER_CREATING")
        
        val draft = orderDraft ?: run {
            isProcessing = false
            return
        }
        val currentOrderId = orderId ?: run {
            isProcessing = false
            return
        }

        showLoader(if (method == "cod") "Creating Order..." else "Verifying Payment...")
        
        try {
            val timestamp = System.currentTimeMillis()
            val order = OrderModel().apply {
                this.orderId = currentOrderId
                uid = draft.uid
                address = draft.address
                this.paymentStatus = paymentStatus
                this.paymentMethod = method
                this.razorpayOrderId = rzpOrderId
                this.razorpayPaymentId = rzpPaymentId
                setCodOrder(method == "cod")
                status = "Pending"
                totalAmount = draft.totalAmount
                deliveryCharge = draft.deliveryCharge
                printingCharges = draft.printingCharges
                createdAt = timestamp
                updatedAt = timestamp
                gstType = draft.gstType
                printSets = draft.printSets
            }

            database.child("orders").child(currentOrderId).setValue(order)
                .addOnSuccessListener {
                    if (isFinishing || isDestroyed) {
                        logError("Activity finished before success callback")
                        return@addOnSuccessListener
                    }

                    isOrderCreated = true
                    logEvent("ORDER_CREATED_FIREBASE: $currentOrderId")
                    
                    try {
                        database.child("users").child(draft.uid).child("orders").child(currentOrderId).setValue(true)
                        logEvent("ORDER_SAVED_USER_NODE")
                    } catch (e: Exception) {
                        logError("Error saving to user node: ${e.message}")
                    }
                    
                    // Send Notifications
                    try {
                        if (method == "cod") {
                            NotificationHelper.sendOrderCreatedNotification(this, draft.uid, currentOrderId, true)
                        } else {
                            NotificationHelper.sendPaymentSuccessNotification(this, draft.uid, currentOrderId)
                            NotificationHelper.sendOrderCreatedNotification(this, draft.uid, currentOrderId, false)
                        }
                    } catch (e: Exception) {
                        logError("Notification Error: ${e.message}")
                    }

                    hideLoader()
                    safeToast("Order Placed Successfully")
                    
                    logEvent("NAVIGATING_SUCCESS_SCREEN")
                    try {
                        UploadManager.reset(this)
                        val intent = Intent(this, PaymentSuccessActivity::class.java).apply {
                            putExtra("orderId", currentOrderId)
                            putExtra("amount", draft.totalAmount)
                            putExtra("method", if (method == "cod") "Cash On Delivery" else "Razorpay")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        startActivity(intent)
                        logEvent("SUCCESS_ACTIVITY_OPENED_SUCCESSFULLY")
                        finish()
                    } catch (e: Exception) {
                        logError("RAZORPAY_CRASH (Navigation): ${e.stackTraceToString()}")
                        safeToast("Order placed successfully, but navigation failed.")
                        finish()
                    }
                }
                .addOnFailureListener { e ->
                    isProcessing = false
                    hideLoader()
                    logError("Firebase Save Failed: ${e.stackTraceToString()}")
                    showErrorDialog("Failed to create order: ${e.message}")
                }
        } catch (e: Exception) {
            isProcessing = false
            hideLoader()
            logError("RAZORPAY_CRASH (saveOrder): ${e.stackTraceToString()}")
            showErrorDialog("An unexpected error occurred: ${e.message}")
        }
    }

    private fun showLoader(message: String) {
        try {
            binding.tvLoaderMessage.text = message
            binding.rlLoader.visibility = View.VISIBLE
        } catch (e: Exception) {
            logError("Error showing loader: ${e.message}")
        }
    }

    private fun hideLoader() {
        try {
            binding.rlLoader.visibility = View.GONE
        } catch (e: Exception) {
            logError("Error hiding loader: ${e.message}")
        }
    }

    private fun safeToast(message: String) {
        try {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Toast error: ${e.message}")
        }
    }

    private fun showErrorDialog(message: String, onDismiss: (() -> Unit)? = null) {
        if (isFinishing || isDestroyed) return
        try {
            AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK") { d, _ ->
                    d.dismiss()
                    onDismiss?.invoke()
                }
                .show()
        } catch (e: Exception) {
            logError("Dialog error: ${e.message}")
            safeToast(message)
            onDismiss?.invoke()
        }
    }

    private fun logEvent(event: String) {
        Log.d(TAG, event)
        try {
            FirebaseCrashlytics.getInstance().log(event)
        } catch (e: Exception) {
            Log.e(TAG, "Crashlytics log failed: ${e.message}")
        }
    }

    private fun logError(message: String) {
        Log.e(TAG, message)
        try {
            FirebaseCrashlytics.getInstance().log("ERROR: $message")
            FirebaseCrashlytics.getInstance().recordException(Exception(message))
        } catch (e: Exception) {
            Log.e(TAG, "Crashlytics record failed: ${e.message}")
        }
    }
}
