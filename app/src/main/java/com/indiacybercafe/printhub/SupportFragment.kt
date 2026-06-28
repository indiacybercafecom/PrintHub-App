package com.indiacybercafe.printhub

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.indiacybercafe.printhub.adapters.FaqAdapter
import com.indiacybercafe.printhub.databinding.FragmentSupportBinding
import com.indiacybercafe.printhub.models.FaqModel
import com.indiacybercafe.printhub.models.TicketModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SupportFragment : Fragment() {

    private var _binding: FragmentSupportBinding? = null
    private val binding get() = _binding!!

    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().getReference("supportTickets")
    private val storage = FirebaseStorage.getInstance().getReference("support_screenshots")

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.all { it.value }) {
            showImageSourceDialog()
        } else {
            Toast.makeText(requireContext(), "Permissions required for camera/gallery", Toast.LENGTH_SHORT).show()
        }
    }

    private val getGalleryImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            displayImagePreview(it)
        }
    }

    private val getCameraImage = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri != null) {
            selectedImageUri = cameraImageUri
            displayImagePreview(cameraImageUri!!)
        }
    }

    private fun displayImagePreview(uri: Uri) {
        binding.ivScreenshotPreview.apply {
            visibility = View.VISIBLE
            Glide.with(this).load(uri).into(this)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSupportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Handle Safe Area Insets for Toolbar
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        setupFaq()
        setupCategoryDropdown()
        setupClickListeners()
    }

    private fun setupFaq() {
        val faqs = listOf(
            FaqModel("How long does printing take?", "Most orders are printed within 1–24 hours."),
            FaqModel("When are uploaded files deleted?", "Uploaded files are permanently deleted after successful delivery."),
            FaqModel("Can I cancel my order?", "Orders can be cancelled before printing starts."),
            FaqModel("How do refunds work?", "Refunds are processed within 3–7 business days."),
            FaqModel("Can I recover deleted files?", "No. Deleted files cannot be recovered.")
        )

        binding.rvFaq.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = FaqAdapter(faqs)
        }
    }

    private fun setupCategoryDropdown() {
        val categories = arrayOf(
            "Payment Issue", "Upload Problem", "Printing Issue",
            "Delivery Issue", "Refund Request", "Technical Problem", "Other"
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.actvCategory.setAdapter(adapter)
    }

    private fun setupClickListeners() {
        binding.btnCallSupport.setOnClickListener { makeCall("+919203251821") }
        binding.btnWhatsappSupport.setOnClickListener { openWhatsapp("+919203251821") }
        binding.btnEmailSupport.setOnClickListener { sendEmail("printhub@indiacybercafe.com") }
        
        binding.btnTrackOrder.setOnClickListener {
            // Logic to track order or show search field
            binding.mainSupport.smoothScrollTo(0, binding.tilOrderIdSearch.top)
            binding.etOrderIdSearch.requestFocus()
        }

        binding.btnTrackQuick.setOnClickListener {
            val orderId = binding.etOrderIdSearch.text.toString()
            if (orderId.isNotEmpty()) {
                Toast.makeText(requireContext(), "Tracking Order: $orderId", Toast.LENGTH_SHORT).show()
                // Implement actual tracking navigation if possible
            } else {
                binding.tilOrderIdSearch.error = "Enter Order ID"
            }
        }

        binding.btnOrderSupport.setOnClickListener {
            val orderId = binding.etOrderIdSearch.text.toString()
            if (orderId.isNotEmpty()) {
                binding.etTicketOrderId.setText(orderId)
                binding.mainSupport.smoothScrollTo(0, binding.tilCategory.top)
            } else {
                binding.tilOrderIdSearch.error = "Enter Order ID"
            }
        }

        binding.btnUploadScreenshot.setOnClickListener {
            checkAndRequestPermissions()
        }

        binding.btnSubmitTicket.setOnClickListener {
            validateAndSubmit()
        }

        binding.tvWebsite.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://printhub.indiacybercafe.com"))
            startActivity(intent)
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsNeeded = mutableListOf(Manifest.permission.CAMERA)
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES)
        }

        val allGranted = permissionsNeeded.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            showImageSourceDialog()
        } else {
            permissionLauncher.launch(permissionsNeeded.toTypedArray())
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Camera", "Gallery")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Image Source")
            .setItems(options) { _, which ->
                if (which == 0) openCamera() else getGalleryImage.launch("image/*")
            }
            .show()
    }

    private fun openCamera() {
        try {
            val photoFile = createImageFile()
            cameraImageUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )
            getCameraImage.launch(cameraImageUri)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to create image file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun validateAndSubmit() {
        val category = binding.actvCategory.text.toString()
        val orderId = binding.etTicketOrderId.text.toString()
        val message = binding.etMessage.text.toString()

        if (category.isEmpty()) {
            binding.tilCategory.error = "Select a category"
            return
        }
        binding.tilCategory.error = null

        if (message.isEmpty()) {
            binding.tilMessage.error = "Please describe your issue"
            return
        }
        binding.tilMessage.error = null

        binding.btnSubmitTicket.isEnabled = false
        binding.btnSubmitTicket.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE

        if (selectedImageUri != null) {
            uploadImageAndSubmit(category, orderId, message)
        } else {
            submitToDatabase(category, orderId, message, "")
        }
    }

    private fun uploadImageAndSubmit(category: String, orderId: String, message: String) {
        val fileName = "ticket_${System.currentTimeMillis()}_${UUID.randomUUID()}"
        val ref = storage.child(fileName)

        ref.putFile(selectedImageUri!!).addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { uri ->
                submitToDatabase(category, orderId, message, uri.toString())
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Image upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
            submitToDatabase(category, orderId, message, "")
        }
    }

    private fun submitToDatabase(category: String, orderId: String, message: String, imageUrl: String) {
        val user = auth.currentUser
        val ticketId = database.push().key ?: UUID.randomUUID().toString()
        
        val ticket = TicketModel(
            ticketId = ticketId,
            uid = user?.uid ?: "",
            userName = user?.displayName ?: "Guest",
            phone = user?.phoneNumber ?: "",
            orderId = orderId,
            category = category,
            message = message,
            imageUrl = imageUrl,
            status = "Open",
            createdAt = System.currentTimeMillis()
        )

        database.child(ticketId).setValue(ticket).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(requireContext(), "Ticket submitted successfully!", Toast.LENGTH_LONG).show()
                clearForm()
            } else {
                Toast.makeText(requireContext(), "Submission failed", Toast.LENGTH_SHORT).show()
            }
            binding.btnSubmitTicket.isEnabled = true
            binding.btnSubmitTicket.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun clearForm() {
        binding.actvCategory.setText("")
        binding.etTicketOrderId.setText("")
        binding.etMessage.setText("")
        binding.ivScreenshotPreview.visibility = View.GONE
        selectedImageUri = null
    }

    private fun makeCall(phone: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
        startActivity(intent)
    }

    private fun openWhatsapp(phone: String) {
        val url = "https://api.whatsapp.com/send?phone=$phone"
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "WhatsApp not installed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendEmail(email: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
        intent.putExtra(Intent.EXTRA_SUBJECT, "Support Request - ICC PrintHub")
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No email app found", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
