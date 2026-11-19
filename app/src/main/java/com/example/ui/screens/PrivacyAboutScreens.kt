package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.ui.components.CalcProTopBar

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CalcProTopBar(title = stringResource(R.string.privacy_policy), onBack = onBack)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 620.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PolicySection(
                    title = "1. Local-First Calculation Data",
                    body = "CalcPro operates offline-first. Your standard, scientific, GST, discount, percentage, and unit calculations and history records are stored exclusively on your device using an internal SQLite (Room) database and DataStore Preferences. CalcPro never uploads or sells your calculation history."
                )
                PolicySection(
                    title = "2. No Account or Personal Tracking",
                    body = "CalcPro does not require user registration, login, phone numbers, contacts, location, camera, or storage permissions. Only INTERNET and ACCESS_NETWORK_STATE permissions are declared to fetch optional live currency rates and handle Play Billing/Ads."
                )
                PolicySection(
                    title = "3. Currency Exchange Rates",
                    body = "When you tap Refresh in the Currency Converter, CalcPro requests public exchange rates from Open ExchangeRate-API (open.er-api.com). No personal identifiers or calculation inputs are transmitted."
                )
                PolicySection(
                    title = "4. Advertising & Billing Disclosures",
                    body = "For non-premium users, CalcPro may display Google AdMob banners or native ads when configured. Google AdMob may process device advertising identifiers subject to Google's Privacy Policy and regional consent requirements. Upgrading to CalcPro Premium via Google Play Billing removes all advertisements."
                )
                PolicySection(
                    title = "5. Deleting Your Data",
                    body = "You can permanently delete individual records or clear all saved calculation history anytime from History or Settings, or by uninstalling CalcPro."
                )
            }
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CalcProTopBar(title = stringResource(R.string.about_calcpro), onBack = onBack)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 540.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_calcpro_logo),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(92.dp).clip(RoundedCornerShape(22.dp))
                )
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                PolicySection(
                    title = "Mathematical Engine",
                    body = "CalcPro uses 34-digit DECIMAL128 BigDecimal arithmetic for exact standard, GST, discount, percentage, and unit math, paired with DEG/RAD trigonometric and logarithmic evaluation."
                )
                PolicySection(
                    title = "Offline & Multilingual",
                    body = "Designed for fast startup, low battery usage, and full offline productivity in English, Hindi, and Marathi."
                )
            }
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
