# InvoicePro - Android App (Kotlin)

## Overview
InvoicePro is a production-ready Android application for GST billing automation, tailored for small businesses in India. It follows a clean MVVM architecture and is built with Kotlin.

## Tech Stack
- **Language**: Kotlin
- **Architecture**: MVVM (Model-View-ViewModel)
- **UI**: Material Design 3
- **Database**: Room (Local/Offline-first)
- **PDF Generation**: Android `PdfDocument` API

## Core Features
- ✅ **Dashboard**: Monthly sales and GST summaries.
- ✅ **Customer Management**: Add/Edit/Delete customer profiles with GSTIN support.
- ✅ **Product Management**: Manage goods/services with predefined GST rates.
- ✅ **Invoice Automation**: Auto-calculation of CGST, SGST, and IGST.
- ✅ **PDF Engine**: Professional, watermark-free PDF generation for local storage and sharing.
- ✅ **Business Profile**: One-time setup for business branding.

## File Structure
- `app/src/main/java/com/invoicepro/app/data`: Room database and DAOs.
- `app/src/main/java/com/invoicepro/app/model`: Data entities (Customer, Product, Invoice, BusinessProfile).
- `app/src/main/java/com/invoicepro/app/ui`: Activities and Fragments (Dashboard, Main).
- `app/src/main/java/com/invoicepro/app/util`: Utility classes (PDF Generator).
- `app/src/main/res/layout`: XML layout definitions.

## Recent Changes
- **2026-01-15**: 
    - Initialized core project structure and Gradle configs.
    - Implemented Room entities and Database layer with Singleton pattern.
    - Added `DashboardFragment`, `CustomerFragment`, `ProductFragment`, and `CreateInvoiceFragment`.
    - Implemented `PdfGenerator` utility.
    - Configured Material 3 themes and Android Manifest.
