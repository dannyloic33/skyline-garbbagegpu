/*
 * SPDX-License-Identifier: MPL-2.0
 * Copyright © 2023 Skyline Team and Contributors (https://github.com/skyline-emu/)
 */

package emu.skyline.utils

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import emu.skyline.R
import emu.skyline.SkylineApplication
import emu.skyline.di.getSettings

/**
 * An [ActivityLifecycleCallbacks] that applies [DynamicColors] to activities based on user preference
 */
class DynamicColorsActivityLifecycleCallbacks : ActivityLifecycleCallbacks {
    private fun getDynamicColorsOptions() = DynamicColorsOptions.Builder().apply {
        if (!SkylineApplication.context.getSettings().useMaterialYou)
            setThemeOverlay(R.style.AppTheme_Overlay)
    }.build()

    override fun onActivityPreCreated(activity : Activity, savedInstanceState : Bundle?) {
        DynamicColors.applyToActivityIfAvailable(activity, getDynamicColorsOptions())
    }

    override fun onActivityCreated(activity : Activity, savedInstanceState : Bundle?) {}
    override fun onActivityStarted(activity : Activity) {}
    override fun onActivityResumed(activity : Activity) {}
    override fun onActivityPaused(activity : Activity) {}
    override fun onActivityStopped(activity : Activity) {}
    override fun onActivitySaveInstanceState(activity : Activity, outState : Bundle) {}
    override fun onActivityDestroyed(activity : Activity) {}
}
