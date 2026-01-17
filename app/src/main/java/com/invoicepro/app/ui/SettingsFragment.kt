package com.invoicepro.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.invoicepro.app.data.AppDatabase
import com.invoicepro.app.databinding.FragmentSettingsBinding
import com.invoicepro.app.model.BusinessProfile
import kotlinx.coroutines.launch

import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private var logoUri: Uri? = null
    private var signatureUri: Uri? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            Toast.makeText(requireContext(), "Permission Granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndRequestPermissions() {
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.Q) {
            // No runtime request needed for older versions or handle specifically if required
            return
        }
        
        val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private val selectLogo = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { 
            logoUri = it
            try {
                requireContext().contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {}
            binding.imageLogoPreview.setImageURI(it)
            binding.imageLogoPreview.visibility = View.VISIBLE
            Toast.makeText(requireContext(), "Logo selected", Toast.LENGTH_SHORT).show()
        }
    }

    private val selectSignature = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { 
            signatureUri = it
            try {
                requireContext().contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {}
            binding.imageSignaturePreview.setImageURI(it)
            binding.imageSignaturePreview.visibility = View.VISIBLE
            Toast.makeText(requireContext(), "Signature selected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkAndRequestPermissions()
        loadProfile()
        
        binding.btnSelectLogo.setOnClickListener { selectLogo.launch("image/*") }
        binding.btnSelectSignature.setOnClickListener { selectSignature.launch("image/*") }

        binding.btnSaveProfile.setOnClickListener {
            val name = binding.editBizName.text.toString()
            val address = binding.editBizAddress.text.toString()
            val phone = binding.editBizPhone.text.toString()
            val gstin = binding.editBizGstin.text.toString()

            if (name.isNotEmpty()) {
                val profile = BusinessProfile(
                    name = name,
                    address = address,
                    phone = phone,
                    gstin = gstin,
                    logoPath = logoUri?.toString(),
                    signaturePath = signatureUri?.toString()
                )
                lifecycleScope.launch {
                    val db = AppDatabase.getDatabase(requireContext())
                    db.businessProfileDao().insertProfile(profile)
                    Toast.makeText(requireContext(), "Profile Saved!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Business Name is required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                val profile = db.businessProfileDao().getProfile()
                profile?.let {
                    binding.editBizName.setText(it.name)
                    binding.editBizAddress.setText(it.address)
                    binding.editBizPhone.setText(it.phone)
                    binding.editBizGstin.setText(it.gstin)
                    
                    it.logoPath?.let { path ->
                        try {
                            logoUri = Uri.parse(path)
                            requireContext().contentResolver.takePersistableUriPermission(logoUri!!, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            binding.imageLogoPreview.setImageURI(logoUri)
                            binding.imageLogoPreview.visibility = View.VISIBLE
                        } catch (e: Exception) {
                            binding.imageLogoPreview.visibility = View.GONE
                        }
                    }
                    
                    it.signaturePath?.let { path ->
                        try {
                            signatureUri = Uri.parse(path)
                            requireContext().contentResolver.takePersistableUriPermission(signatureUri!!, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            binding.imageSignaturePreview.setImageURI(signatureUri)
                            binding.imageSignaturePreview.visibility = View.VISIBLE
                        } catch (e: Exception) {
                            binding.imageSignaturePreview.visibility = View.GONE
                        }
                    }
                }
            } catch (e: Exception) {
                // Silent error to prevent crash on load
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}