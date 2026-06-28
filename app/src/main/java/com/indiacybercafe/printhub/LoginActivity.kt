package com.indiacybercafe.printhub

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.TranslateAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthProvider
import com.indiacybercafe.printhub.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authManager = FirebaseAuthManager()
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var resendTimer: CountDownTimer? = null
    
    // Timeout handler for OTP requests
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null

    companion object {
        private const val TAG = "LoginActivity"
        private const val REQUEST_TIMEOUT_MS = 30000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupListeners()
        setupOtpInputs()
        setupFooter()
        setupBackNavigation()
        runEntranceAnimations()
    }

    private fun setupUI() {
        binding.etPhoneNumber.requestFocus()
        binding.progressBar.visibility = View.GONE
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.otpSection.visibility == View.VISIBLE) {
                    showMobileInput() // Also resets state
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun runEntranceAnimations() {
        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration = 400
            fillAfter = true
        }
        binding.headerLayout.startAnimation(fadeIn)
        binding.loginCard.startAnimation(fadeIn)
    }

    private fun setupListeners() {
        binding.btnSendOtp.setOnClickListener {
            val phone = binding.etPhoneNumber.text.toString().trim()
            if (phone.length == 10) {
                // Clear any previous state before a fresh send
                resetVerificationState()
                sendOtp("+91$phone", null)
            } else {
                binding.etPhoneNumber.error = "Enter valid 10-digit mobile number"
            }
        }

        binding.btnVerifyOtp.setOnClickListener {
            verifyOtp()
        }

        binding.btnBack.setOnClickListener {
            showMobileInput()
        }

        binding.btnResendOtp.setOnClickListener {
            val phone = binding.etPhoneNumber.text.toString().trim()
            sendOtp("+91$phone", resendToken)
        }

        binding.etPhoneNumber.setOnEditorActionListener { _, _, event ->
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER) {
                binding.btnSendOtp.performClick()
                true
            } else false
        }
    }

    private fun setLoading(isLoading: Boolean, message: String = "") {
        // Cancel existing timeout if any
        timeoutRunnable?.let { handler.removeCallbacks(it) }

        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSendOtp.isEnabled = !isLoading
        binding.btnVerifyOtp.isEnabled = !isLoading
        binding.etPhoneNumber.isEnabled = !isLoading
        
        if (isLoading) {
            if (message.isNotEmpty()) {
                if (binding.mobileInputSection.visibility == View.VISIBLE) {
                    binding.btnSendOtp.text = message
                } else {
                    binding.btnVerifyOtp.text = message
                }
            }
            
            // Set 30s timeout to prevent infinite loading
            timeoutRunnable = Runnable {
                if (binding.progressBar.visibility == View.VISIBLE) {
                    setLoading(false)
                    Toast.makeText(this, "Unable to send OTP. Please try again.", Toast.LENGTH_LONG).show()
                }
            }
            handler.postDelayed(timeoutRunnable!!, REQUEST_TIMEOUT_MS)
        } else {
            binding.btnSendOtp.text = "Send OTP"
            binding.btnVerifyOtp.text = "Verify OTP"
        }
    }

    private fun sendOtp(phoneNumber: String, token: PhoneAuthProvider.ForceResendingToken?) {
        setLoading(true, if (token == null) "Sending OTP..." else "Resending...")
        
        authManager.sendOtp(phoneNumber, this, token,
            onCodeSent = { vId, newToken ->
                setLoading(false)
                verificationId = vId
                resendToken = newToken
                showOtpInput(phoneNumber)
                startResendTimer()
            },
            onVerificationCompleted = { credential ->
                Log.d(TAG, "Instant verification triggered")
                authManager.signInWithCredential(credential,
                    onSuccess = {
                        setLoading(false)
                        val uid = authManager.getCurrentUserUid()
                        if (uid != null) checkUserAndNavigate(uid)
                    },
                    onFailure = { e ->
                        setLoading(false)
                        Toast.makeText(this, "Auto-verification failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onVerificationFailed = { e ->
                setLoading(false)
                Log.e(TAG, "OTP Send Failed: ${e.message}", e)
                
                val message = when (e) {
                    is FirebaseAuthInvalidCredentialsException -> "Invalid phone number format."
                    is FirebaseTooManyRequestsException -> "Too many requests. Please try again later."
                    else -> e.localizedMessage ?: "Verification failed. Check your internet."
                }
                
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun verifyOtp() {
        val otp = getOtpFromInputs()
        if (otp.length == 6 && verificationId != null) {
            setLoading(true, "Verifying...")
            
            authManager.verifyOtp(verificationId!!, otp,
                onSuccess = {
                    val uid = authManager.getCurrentUserUid()
                    if (uid != null) {
                        checkUserAndNavigate(uid)
                    }
                },
                onFailure = { e ->
                    setLoading(false)
                    Log.e(TAG, "OTP Verification Failed: ${e.message}", e)
                    Toast.makeText(this, e.message ?: "Invalid OTP", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            if (verificationId == null) {
                Toast.makeText(this, "Session expired. Send OTP again.", Toast.LENGTH_SHORT).show()
                showMobileInput()
            } else {
                Toast.makeText(this, "Please enter 6-digit OTP", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkUserAndNavigate(uid: String) {
        authManager.checkUserExists(uid) { exists ->
            if (exists) {
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                try {
                    val intent = Intent(this, Class.forName("com.indiacybercafe.printhub.ProfileSetupActivity"))
                    startActivity(intent)
                } catch (e: ClassNotFoundException) {
                    startActivity(Intent(this, MainActivity::class.java))
                }
            }
            finishAffinity()
        }
    }

    private fun showOtpInput(phoneNumber: String) {
        binding.mobileInputSection.visibility = View.GONE
        binding.otpSection.visibility = View.VISIBLE
        
        val slideUp = TranslateAnimation(0f, 0f, 100f, 0f).apply {
            duration = 200
        }
        binding.otpSection.startAnimation(slideUp)

        binding.tvOtpSentTo.text = "OTP sent to $phoneNumber"
        binding.cardTitle.text = "Enter OTP"
        binding.cardSubtitle.text = "We've sent a code to your phone"
        
        clearOtpInputs()
        binding.otp1.requestFocus()
    }

    private fun showMobileInput() {
        resetVerificationState()
        binding.otpSection.visibility = View.GONE
        binding.mobileInputSection.visibility = View.VISIBLE
        binding.cardTitle.text = "Welcome Back"
        binding.cardSubtitle.text = "Sign in with your mobile number"
        binding.etPhoneNumber.requestFocus()
    }

    private fun resetVerificationState() {
        verificationId = null
        resendToken = null
        resendTimer?.cancel()
        resendTimer = null
        setLoading(false)
        clearOtpInputs()
        timeoutRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun clearOtpInputs() {
        binding.otp1.setText("")
        binding.otp2.setText("")
        binding.otp3.setText("")
        binding.otp4.setText("")
        binding.otp5.setText("")
        binding.otp6.setText("")
    }

    private fun startResendTimer() {
        binding.btnResendOtp.isEnabled = false
        resendTimer?.cancel()
        resendTimer = object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.btnResendOtp.text = "Resend OTP (${millisUntilFinished / 1000}s)"
            }

            override fun onFinish() {
                binding.btnResendOtp.isEnabled = true
                binding.btnResendOtp.text = "Resend OTP"
            }
        }.start()
    }

    private fun setupOtpInputs() {
        val boxes = arrayOf(binding.otp1, binding.otp2, binding.otp3, binding.otp4, binding.otp5, binding.otp6)
        
        for (i in boxes.indices) {
            boxes[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1 && i < boxes.size - 1) {
                        boxes[i + 1].requestFocus()
                    }
                    if (getOtpFromInputs().length == 6) {
                        hideKeyboard()
                        verifyOtp()
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            boxes[i].setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                    if (boxes[i].text.isEmpty() && i > 0) {
                        boxes[i - 1].requestFocus()
                        boxes[i - 1].setText("")
                        return@setOnKeyListener true
                    }
                }
                false
            }
            
            if (i == boxes.size - 1) {
                boxes[i].setOnEditorActionListener { _, _, event ->
                    if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER) {
                        verifyOtp()
                        true
                    } else false
                }
            }
        }
    }

    private fun getOtpFromInputs(): String {
        return binding.otp1.text.toString() +
                binding.otp2.text.toString() +
                binding.otp3.text.toString() +
                binding.otp4.text.toString() +
                binding.otp5.text.toString() +
                binding.otp6.text.toString()
    }

    private fun setupFooter() {
        val fullText = "By continuing, you agree to our Terms & Privacy Policy"
        val spannableString = SpannableString(fullText)
        
        val primaryColor = ContextCompat.getColor(this, R.color.primary_brown)

        val termsClick = object : ClickableSpan() {
            override fun onClick(widget: View) {
                Toast.makeText(this@LoginActivity, "Terms clicked", Toast.LENGTH_SHORT).show()
            }
            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.color = primaryColor
            }
        }

        val privacyClick = object : ClickableSpan() {
            override fun onClick(widget: View) {
                Toast.makeText(this@LoginActivity, "Privacy Policy clicked", Toast.LENGTH_SHORT).show()
            }
            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.color = primaryColor
            }
        }

        val termsStart = fullText.indexOf("Terms")
        val termsEnd = termsStart + "Terms".length
        val privacyStart = fullText.indexOf("Privacy Policy")
        val privacyEnd = privacyStart + "Privacy Policy".length

        if (termsStart != -1) {
            spannableString.setSpan(termsClick, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        if (privacyStart != -1) {
            spannableString.setSpan(privacyClick, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        binding.tvFooter.text = spannableString
        binding.tvFooter.movementMethod = LinkMovementMethod.getInstance()
        binding.tvFooter.highlightColor = Color.TRANSPARENT
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    override fun onDestroy() {
        resetVerificationState()
        super.onDestroy()
    }
}
