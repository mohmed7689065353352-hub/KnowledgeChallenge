package com.example.knowledgechallenge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.knowledgechallenge.ui.navigation.AppNavigation
import com.example.knowledgechallenge.ui.theme.KnowledgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as KnowledgeApp
        if (savedInstanceState == null) {
            app.consentManager.gatherConsent(this) { app.initializeAdsIfAllowed() }
        }
        app.initializeAdsIfAllowed()
        setContent {
            KnowledgeTheme { AppNavigation() }
        }
    }
}
