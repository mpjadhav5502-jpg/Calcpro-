package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.BuildConfig
import com.example.R
import com.example.data.ads.AdManager
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * Real Google AdMob Banner slot.
 * - Never renders for Premium users.
 * - Collapses cleanly (0.dp) if Play Services/AdMob fails to load.
 * - Placed only on non-keypad surfaces (Dashboard / History / Settings).
 */
@Composable
fun AdBannerSlot(
    adManager: AdManager,
    isPremium: Boolean,
    modifier: Modifier = Modifier
) {
    if (isPremium || !BuildConfig.ADS_ENABLED) return

    var isLoaded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val adView = remember(context) {
        adManager.createBannerAdView(context) { loaded ->
            isLoaded = loaded
        }
    }

    DisposableEffect(adView) {
        onDispose {
            adView?.destroy()
        }
    }

    if (adView != null && isLoaded) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.ad_notice),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                AndroidView(
                    factory = { adView },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Real Google AdMob Native Ad Card.
 * - Renders only when a real NativeAd object is returned by AdLoader.
 * - Never shows placeholder or fake advertisements.
 */
@Composable
fun AdNativeCard(
    adManager: AdManager,
    isPremium: Boolean,
    modifier: Modifier = Modifier
) {
    if (isPremium || !BuildConfig.ADS_ENABLED) return

    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    LaunchedEffect(Unit) {
        adManager.loadNativeAd(
            onLoaded = { loaded ->
                nativeAd?.destroy()
                nativeAd = loaded
            },
            onFailed = {
                nativeAd = null
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            nativeAd?.destroy()
        }
    }

    val currentAd = nativeAd ?: return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        AndroidView(
            factory = { ctx ->
                NativeAdView(ctx).apply {
                    setNativeAd(currentAd)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentAd.headline ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ad_notice),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            currentAd.body?.let { bodyText ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = bodyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            currentAd.callToAction?.let { cta ->
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {},
                    enabled = false,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(cta)
                }
            }
        }
    }
}
