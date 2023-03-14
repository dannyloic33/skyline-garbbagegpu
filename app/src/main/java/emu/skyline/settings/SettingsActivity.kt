/*
 * SPDX-License-Identifier: MPL-2.0
 * Copyright © 2020 Skyline Team and Contributors (https://github.com/skyline-emu/)
 */

package emu.skyline.settings

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.view.Menu
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.WindowCompat
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.forEach
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.internal.ToolbarUtils
import emu.skyline.R
import emu.skyline.data.AppItemTag
import emu.skyline.databinding.SettingsActivityBinding
import emu.skyline.utils.WindowInsetsHelper

class SettingsActivity : AppCompatActivity() {
    val binding by lazy { SettingsActivityBinding.inflate(layoutInflater) }

    /**
     * The instance of [PreferenceFragmentCompat] that is shown inside [R.id.settings]
     * Retrieves extras from the intent if any and instantiates the appropriate fragment
     */
    private val preferenceFragment by lazy {
        if (intent.hasExtra(AppItemTag))
            GameSettingsFragment().apply { arguments = intent.extras }
        else
            GlobalSettingsFragment()
    }

    /**
     * This initializes all of the elements in the activity and displays the settings fragment
     */
    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsHelper.applyToActivity(binding.root)

        setSupportActionBar(binding.titlebar.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        var layoutDone = false // Tracks if the layout is complete to avoid retrieving invalid attributes
        binding.coordinatorLayout.viewTreeObserver.addOnTouchModeChangeListener { isTouchMode ->
            val layoutUpdate = {
                val params = binding.settings.layoutParams as CoordinatorLayout.LayoutParams
                if (!isTouchMode) {
                    binding.titlebar.appBarLayout.setExpanded(true)
                    params.height = binding.coordinatorLayout.height - binding.titlebar.toolbar.height
                } else {
                    params.height = CoordinatorLayout.LayoutParams.MATCH_PARENT
                }

                binding.settings.layoutParams = params
                binding.settings.requestLayout()
            }

            if (!layoutDone) {
                binding.coordinatorLayout.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        // We need to wait till the layout is done to get the correct height of the toolbar
                        binding.coordinatorLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        layoutUpdate()
                        layoutDone = true
                    }
                })
            } else {
                layoutUpdate()
            }
        }

        fun enableMarquee(textView : TextView) {
            textView.ellipsize = TextUtils.TruncateAt.MARQUEE
            textView.isSelected = true
            textView.marqueeRepeatLimit = -1
        }

        // Set a temporary subtitle because the retrieval of the subtitle TextView fails if subtitle is null
        supportActionBar?.subtitle = "sub"

        @SuppressLint("RestrictedApi")
        val toolbarTitleTextView = ToolbarUtils.getTitleTextView(binding.titlebar.toolbar)
        @SuppressLint("RestrictedApi")
        val toolbarSubtitleTextView = ToolbarUtils.getSubtitleTextView(binding.titlebar.toolbar)
        toolbarTitleTextView?.let { enableMarquee(it) }
        toolbarSubtitleTextView?.let { enableMarquee(it) }

        // Reset the subtitle to null
        supportActionBar?.subtitle = null

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, preferenceFragment)
            .commit()
    }

    override fun onCreateOptionsMenu(menu : Menu?) : Boolean {
        menuInflater.inflate(R.menu.settings_menu, menu)
        val menuItem = menu!!.findItem(R.id.app_bar_search)
        val searchView = menuItem.actionView as SearchView
        searchView.queryHint = getString(R.string.search)

        searchView.setOnQueryTextFocusChangeListener { _, focus ->
            (binding.titlebar.toolbar.layoutParams as AppBarLayout.LayoutParams).scrollFlags =
                if (focus)
                    AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL
                else
                    AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query : String) : Boolean {
                return false
            }

            override fun onQueryTextChange(newText : String) : Boolean {
                val queries = newText.split(",").toTypedArray()
                if (newText.isNotEmpty()) {
                    preferenceFragment.preferenceScreen.forEach { preferenceCategory ->
                        val categoryVisible = queries.any { preferenceCategory.title?.contains(it, true) ?: false }
                        // Tracks whether all preferences under this category are hidden
                        var areAllPrefsHidden = true
                        // Tracks whether there is at least one preference under this category that has a key
                        var hasNonNullPreferenceKey = false
                        (preferenceCategory as PreferenceCategory).forEach { preference ->
                            preference.isVisible = categoryVisible || (preference.key != null && queries.any { preference.title?.contains(it, true) ?: false })
                            if (preference.isVisible && areAllPrefsHidden)
                                areAllPrefsHidden = false
                            hasNonNullPreferenceKey = hasNonNullPreferenceKey || preference.key != null
                        }
                        // Hide PreferenceCategory if none of its preferences match the search and neither the category title
                        preferenceCategory.isVisible = hasNonNullPreferenceKey && (!areAllPrefsHidden || categoryVisible)
                    }
                } else { // If user input is empty, show all preferences
                    preferenceFragment.preferenceScreen.forEach { preferenceCategory ->
                        preferenceCategory.isVisible = true
                        (preferenceCategory as PreferenceCategory).forEach { preference ->
                            preference.isVisible = true
                        }
                    }
                }
                return true
            }
        })
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * This handles on calling [onBackPressed] when [KeyEvent.KEYCODE_BUTTON_B] is lifted
     */
    override fun onKeyUp(keyCode : Int, event : KeyEvent?) : Boolean {
        if (keyCode == KeyEvent.KEYCODE_BUTTON_B) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }

        return super.onKeyUp(keyCode, event)
    }

    override fun finish() {
        setResult(RESULT_OK)
        super.finish()
    }
}
