package com.invoicepro.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.invoicepro.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Basic Navigation Setup Placeholder
        binding.textViewHello.text = "Welcome to InvoicePro Dashboard"
    }
}
