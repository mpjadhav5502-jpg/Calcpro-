package com.example

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.local.CalculationCategory
import com.example.ui.navigation.CalcRoute
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.CurrencyConverterScreen
import com.example.ui.screens.DiscountCalculatorScreen
import com.example.ui.screens.GstCalculatorScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PercentageCalculatorScreen
import com.example.ui.screens.PremiumScreen
import com.example.ui.screens.PrivacyPolicyScreen
import com.example.ui.screens.ScientificCalculatorScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StandardCalculatorScreen
import com.example.ui.screens.UnitConverterScreen
import com.example.ui.theme.CalcProTheme
import com.example.ui.viewmodel.CalculatorViewModel
import com.example.ui.viewmodel.CurrencyViewModel
import com.example.ui.viewmodel.FinancialToolsViewModel
import com.example.ui.viewmodel.HistoryViewModel
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.UnitConverterViewModel
import com.example.util.LocaleHelper
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = (application as CalcProApplication).container

        setContent {
            val mainViewModel: MainViewModel = viewModel(
                factory = MainViewModel.Factory(
                    settingsDataStore = appContainer.settingsDataStore,
                    historyRepository = appContainer.historyRepository,
                    billingRepository = appContainer.billingRepository,
                    adManager = appContainer.adManager
                )
            )
            val mainState by mainViewModel.uiState.collectAsStateWithLifecycle()

            val baseContext = LocalContext.current
            val baseConfig = LocalConfiguration.current
            val localizedContext = remember(baseContext, mainState.settings.language.code) {
                LocaleHelper.localizedContext(baseContext, mainState.settings.language.code)
            }
            val localizedConfig = remember(baseConfig, mainState.settings.language.code) {
                Configuration(baseConfig).apply {
                    setLocale(Locale.forLanguageTag(mainState.settings.language.code))
                }
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedConfig
            ) {
                CalcProTheme(
                    themeMode = mainState.settings.themeMode,
                    isPremium = mainState.isPremium
                ) {
                    CalcProNavHost(
                        appContainer = appContainer,
                        mainViewModel = mainViewModel,
                        mainState = mainState,
                        activity = this
                    )
                }
            }
        }
    }
}

@Composable
private fun CalcProNavHost(
    appContainer: AppContainer,
    mainViewModel: MainViewModel,
    mainState: com.example.ui.viewmodel.MainUiState,
    activity: ComponentActivity
) {
    val navController = rememberNavController()

    val calculatorViewModel: CalculatorViewModel = viewModel(
        factory = CalculatorViewModel.Factory(
            historyRepository = appContainer.historyRepository,
            settingsDataStore = appContainer.settingsDataStore
        )
    )

    val financialViewModel: FinancialToolsViewModel = viewModel(
        factory = FinancialToolsViewModel.Factory(
            historyRepository = appContainer.historyRepository,
            settingsDataStore = appContainer.settingsDataStore
        )
    )

    val unitViewModel: UnitConverterViewModel = viewModel(
        factory = UnitConverterViewModel.Factory(
            historyRepository = appContainer.historyRepository,
            settingsDataStore = appContainer.settingsDataStore
        )
    )

    val currencyViewModel: CurrencyViewModel = viewModel(
        factory = CurrencyViewModel.Factory(
            currencyRepository = appContainer.currencyRepository,
            historyRepository = appContainer.historyRepository,
            settingsDataStore = appContainer.settingsDataStore
        )
    )

    val historyViewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModel.Factory(appContainer.historyRepository)
    )

    NavHost(
        navController = navController,
        startDestination = CalcRoute.Splash,
        enterTransition = { fadeIn(animationSpec = tween(220)) },
        exitTransition = { fadeOut(animationSpec = tween(180)) }
    ) {
        composable<CalcRoute.Splash> {
            SplashScreen(
                hasCompletedOnboarding = mainState.settings.hasCompletedOnboarding,
                onNavigateToOnboarding = {
                    navController.navigate(CalcRoute.Onboarding) {
                        popUpTo<CalcRoute.Splash> { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(CalcRoute.Home) {
                        popUpTo<CalcRoute.Splash> { inclusive = true }
                    }
                }
            )
        }

        composable<CalcRoute.Onboarding> {
            OnboardingScreen(
                selectedLanguage = mainState.settings.language,
                onLanguageSelected = { mainViewModel.setLanguage(it) },
                onFinishOnboarding = {
                    mainViewModel.completeOnboarding()
                    navController.navigate(CalcRoute.Home) {
                        popUpTo<CalcRoute.Onboarding> { inclusive = true }
                    }
                }
            )
        }

        composable<CalcRoute.Home> {
            HomeDashboardScreen(
                isPremium = mainState.isPremium,
                adManager = appContainer.adManager,
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable<CalcRoute.StandardCalculator> {
            val calcState by calculatorViewModel.uiState.collectAsStateWithLifecycle()
            StandardCalculatorScreen(
                uiState = calcState,
                settings = mainState.settings,
                viewModel = calculatorViewModel,
                onBack = { navController.popBackStack() },
                onOpenScientific = { navController.navigate(CalcRoute.ScientificCalculator) },
                onOpenHistory = { navController.navigate(CalcRoute.History) }
            )
        }

        composable<CalcRoute.ScientificCalculator> {
            val calcState by calculatorViewModel.uiState.collectAsStateWithLifecycle()
            ScientificCalculatorScreen(
                uiState = calcState,
                settings = mainState.settings,
                viewModel = calculatorViewModel,
                onBack = { navController.popBackStack() },
                onOpenStandard = { navController.navigate(CalcRoute.StandardCalculator) },
                onOpenHistory = { navController.navigate(CalcRoute.History) }
            )
        }

        composable<CalcRoute.PercentageCalculator> {
            val pctState by financialViewModel.percentageState.collectAsStateWithLifecycle()
            PercentageCalculatorScreen(
                uiState = pctState,
                settings = mainState.settings,
                viewModel = financialViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable<CalcRoute.GstCalculator> {
            val gstState by financialViewModel.gstState.collectAsStateWithLifecycle()
            GstCalculatorScreen(
                uiState = gstState,
                settings = mainState.settings,
                viewModel = financialViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable<CalcRoute.DiscountCalculator> {
            val discState by financialViewModel.discountState.collectAsStateWithLifecycle()
            DiscountCalculatorScreen(
                uiState = discState,
                settings = mainState.settings,
                viewModel = financialViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable<CalcRoute.UnitConverter> {
            val unitState by unitViewModel.uiState.collectAsStateWithLifecycle()
            UnitConverterScreen(
                uiState = unitState,
                viewModel = unitViewModel,
                onBack = {
                    appContainer.adManager.showInterstitialAtTransition(activity, mainState.isPremium)
                    navController.popBackStack()
                }
            )
        }

        composable<CalcRoute.CurrencyConverter> {
            val currencyState by currencyViewModel.uiState.collectAsStateWithLifecycle()
            CurrencyConverterScreen(
                uiState = currencyState,
                viewModel = currencyViewModel,
                onBack = {
                    appContainer.adManager.showInterstitialAtTransition(activity, mainState.isPremium)
                    navController.popBackStack()
                }
            )
        }

        composable<CalcRoute.History> {
            val historyItems by historyViewModel.historyItems.collectAsStateWithLifecycle()
            val searchQuery by historyViewModel.searchQuery.collectAsStateWithLifecycle()
            HistoryScreen(
                items = historyItems,
                searchQuery = searchQuery,
                isPremium = mainState.isPremium,
                adManager = appContainer.adManager,
                onSearchChange = { historyViewModel.setSearchQuery(it) },
                onDeleteItem = { historyViewModel.deleteItem(it) },
                onClearAll = { historyViewModel.clearAll() },
                onReuseExpression = { expr, category ->
                    calculatorViewModel.loadExpressionFromHistory(expr)
                    val target = if (category == CalculationCategory.SCIENTIFIC) {
                        CalcRoute.ScientificCalculator
                    } else {
                        CalcRoute.StandardCalculator
                    }
                    navController.navigate(target)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<CalcRoute.Settings> {
            SettingsScreen(
                settings = mainState.settings,
                isPremium = mainState.isPremium,
                onThemeSelect = { mainViewModel.setThemeMode(it) },
                onLanguageSelect = { mainViewModel.setLanguage(it) },
                onPrecisionChange = { mainViewModel.setDecimalPrecision(it) },
                onThousandsChange = { mainViewModel.setThousandsSeparator(it) },
                onSoundChange = { mainViewModel.setSoundEnabled(it) },
                onVibrationChange = { mainViewModel.setVibrationEnabled(it) },
                onClearHistory = { mainViewModel.clearAllHistory() },
                onOpenPremium = { navController.navigate(CalcRoute.Premium) },
                onOpenPrivacy = { navController.navigate(CalcRoute.PrivacyPolicy) },
                onOpenAbout = { navController.navigate(CalcRoute.About) },
                onBack = { navController.popBackStack() }
            )
        }

        composable<CalcRoute.Premium> {
            PremiumScreen(
                isPremium = mainState.isPremium,
                billingReady = mainState.billingReady,
                products = mainState.products,
                purchaseState = mainState.purchaseState,
                onPurchaseClick = { act, prod -> mainViewModel.launchPurchase(act, prod) },
                onRestorePurchases = { mainViewModel.restorePurchases() },
                onRefreshBilling = { mainViewModel.refreshBillingProducts() },
                onBack = {
                    mainViewModel.clearPurchaseMessage()
                    navController.popBackStack()
                }
            )
        }

        composable<CalcRoute.PrivacyPolicy> {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }

        composable<CalcRoute.About> {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
