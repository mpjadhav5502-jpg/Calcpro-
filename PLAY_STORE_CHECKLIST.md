# CalcPro — Build, Release, AdMob & Play Store Preparation Guide

## 1. App Configuration
- **App Name**: `CalcPro` (`res/values/strings.xml`, `res/values-hi/strings.xml`, `res/values-mr/strings.xml`)
- **Application ID**: `com.aistudio.calcpro.vkwmrx` (`app/build.gradle.kts`)
- **Version Code / Name**: `1` / `1.0.0`
- **Min / Target / Compile SDK**: `24` / `36` / `36`
- **Signing & Release**: Configured in `app/build.gradle.kts` (`signingConfigs.release` reading `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`).

## 2. Build, Test, APK & AAB Commands
- **Run Unit & Robolectric Tests**:
  `gradle :app:testDebugUnitTest`
- **Build Debug APK**:
  `gradle :app:assembleDebug` -> `app/build/outputs/apk/debug/app-debug.apk`
- **Build Signed Release APK** (set `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`):
  `gradle :app:assembleRelease` -> `app/build/outputs/apk/release/app-release.apk`
- **Build Signed Release App Bundle (AAB)** for Google Play:
  `gradle :app:bundleRelease` -> `app/build/outputs/bundle/release/app-release.aab`

## 3. AdMob Production Setup
CalcPro currently uses Google's official test AdMob IDs in `app/build.gradle.kts`:
- `ADMOB_APP_ID`: `ca-app-pub-3940256099942544~3347511713`
- `ADMOB_BANNER_ID`: `ca-app-pub-3940256099942544/6300978111`
- `ADMOB_NATIVE_ID`: `ca-app-pub-3940256099942544/2247696110`
- `ADMOB_INTERSTITIAL_ID`: `ca-app-pub-3940256099942544/1033173712`

Before production release:
1. Register CalcPro in Google AdMob and create Banner, Native, and Interstitial ad units.
2. Replace the test IDs in `app/build.gradle.kts` (or inject via environment variables / BuildConfig).
3. Ensure `app-ads.txt` is hosted if required.

## 4. Google Play Billing Setup
Configured product SKUs in `app/build.gradle.kts`:
- One-time In-App Purchase: `calcpro_premium_lifetime`
- Monthly Subscription: `calcpro_premium_monthly`
- Yearly Subscription: `calcpro_premium_yearly`

In Google Play Console -> Monetize -> Products:
1. Upload a signed `.aab` to Internal Testing first.
2. Create an In-app product `calcpro_premium_lifetime` and Subscriptions `calcpro_premium_monthly` / `calcpro_premium_yearly`.
3. Add license testers in Play Console to test real purchases and Restore Purchases.

## 5. Play Store Listing & Data Safety Checklist
- **Category**: Productivity / Tools
- **Data Collection**:
  - Calculation history & preferences: Stored locally on device only (not collected or shared).
  - Currency exchange rate requests: Anonymous HTTPS GET to `open.er-api.com`; no personal data sent.
  - Advertising & Billing (if enabled): Declare Google AdMob Advertising ID and Google Play Billing purchase tokens in Play Console Data Safety form.
- **Screenshot Guidance**: Capture Phone (16:9 / 9:19.5) and 7"/10" Tablet screenshots of Home Dashboard, Standard Calculator (Dark & Light), Scientific Calculator (DEG mode), GST/Discount Breakdown, Unit Converter, and Currency Converter.
