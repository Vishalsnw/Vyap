# InvoicePro Android App

## Project Overview
InvoicePro is a modern Android application for managing invoices, products, and customers. It uses MVVM architecture, Room Database for local storage, and a custom PDF engine for invoice generation.

## Important Note
**Do not try to build the application in the Replit environment.** The GitHub Actions workflow (`.github/workflows/build.yml`) is configured to handle the entire build process, including downloading the Gradle wrapper and building the APK/AAB artifacts.

## Architecture
- **UI**: Material 3, ViewBinding, Fragments
- **Database**: Room Persistence Library
- **Architecture**: MVVM (Model-View-ViewModel)
- **Utilities**: PDF Generator, Input Validation
