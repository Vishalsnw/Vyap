package com.invoicepro.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.invoicepro.app.data.AppDatabase
import com.invoicepro.app.databinding.FragmentSettingsBinding
import com.invoicepro.app.model.BusinessProfile
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
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
                    gstin = gstin
                )
                lifecycleScope.launch {
                    // Save to DB or SharedPreferences
                    // For now, placeholder for Business Profile management
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
