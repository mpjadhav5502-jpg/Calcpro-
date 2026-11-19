package com.example.ui.navigation

import kotlinx.serialization.Serializable

sealed interface CalcRoute {
    @Serializable object Splash : CalcRoute
    @Serializable object Onboarding : CalcRoute
    @Serializable object Home : CalcRoute
    @Serializable object StandardCalculator : CalcRoute
    @Serializable object ScientificCalculator : CalcRoute
    @Serializable object PercentageCalculator : CalcRoute
    @Serializable object GstCalculator : CalcRoute
    @Serializable object DiscountCalculator : CalcRoute
    @Serializable object UnitConverter : CalcRoute
    @Serializable object CurrencyConverter : CalcRoute
    @Serializable object History : CalcRoute
    @Serializable object Settings : CalcRoute
    @Serializable object Premium : CalcRoute
    @Serializable object PrivacyPolicy : CalcRoute
    @Serializable object About : CalcRoute
}
