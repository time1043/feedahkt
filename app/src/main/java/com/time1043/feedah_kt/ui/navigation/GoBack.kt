package com.time1043.feedah_kt.ui.navigation

import androidx.navigation.NavController

/**
 * popBackStack, with a cold-start deep-link fallback: a back button on a
 * screen with no navigation history would do nothing, so it goes home
 * instead (mirror of src/lib/back.ts). Every back-only screen's onBack
 * runs through this.
 */
fun NavController.goBack() {
    if (!popBackStack()) {
        navigate(Home) {
            // Cold start with no history: rebuild the stack at home.
            popUpTo(0) { inclusive = true }
        }
    }
}
