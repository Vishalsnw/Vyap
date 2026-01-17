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

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private var logoUri: Uri? = null
    private var signatureUri: Uri? = null

    private val selectLogo = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { 
            logoUri = it
            Toast.makeText(requireContext(), "Logo selected", Toast.LENGTH_SHORT).show()
        }
    }

    private val selectSignature = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { 
            signatureUri = it
            Toast.makeText(requireContext(), "Signature selected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
            val db = AppDatabase.getDatabase(requireContext())
            val profile = db.businessProfileDao().getProfile()
            profile?.let {
                binding.editBizName.setText(it.name)
                binding.editBizAddress.setText(it.address)
                binding.editBizPhone.setText(it.phone)
                binding.editBizGstin.setText(it.gstin)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}