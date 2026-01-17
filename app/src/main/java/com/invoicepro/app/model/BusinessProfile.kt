package com.invoicepro.app.model

import android.content.Context
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_profile")
data class BusinessProfile(
    @PrimaryKey val id: Int = 0,
    val name: String,
    val address: String,
    val gstin: String?,
    val phone: String,
    val logoPath: String? = null,
    val signaturePath: String? = null
)
