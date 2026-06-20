package com.indiacybercafe.printhub

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.database.FirebaseDatabase

class PrintHubApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        FirebaseApp.initializeApp(this)

        try {
            val database = FirebaseDatabase.getInstance()
            database.setPersistenceEnabled(true)
            
            // Enable offline syncing for key paths
            database.getReference("services").keepSynced(true)
            database.getReference("settings").keepSynced(true)
            database.getReference("users").keepSynced(true)
            database.getReference("orders").keepSynced(true)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }

        FirebaseAppCheck.getInstance()
            .installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
    }
}
