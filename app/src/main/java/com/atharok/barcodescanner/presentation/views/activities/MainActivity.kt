/*
 * Barcode Scanner
 * Copyright (C) 2021  Atharok
 *
 * This file is part of Barcode Scanner.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.atharok.barcodescanner.presentation.views.activities

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commitNow
import com.atharok.barcodescanner.BuildConfig
import com.atharok.barcodescanner.R
import com.atharok.barcodescanner.databinding.ActivityMainBinding
import com.atharok.barcodescanner.databinding.TemplateMainHeaderBinding
import com.atharok.barcodescanner.presentation.viewmodel.DynamicShortcutViewModel
import com.atharok.barcodescanner.presentation.views.fragments.main.MainBarcodeCreatorListFragment
import com.atharok.barcodescanner.presentation.views.fragments.main.MainBarcodeHistoryFragment
import com.atharok.barcodescanner.presentation.views.fragments.main.MainCameraXScannerFragment
import com.atharok.barcodescanner.presentation.views.fragments.main.MainSettingsFragment
import com.atharok.barcodescanner.presentation.views.fragments.main.MainVaultFragment
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.color.MaterialColors
import com.google.android.material.shape.MaterialShapeDrawable
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity: BaseActivity() {

    companion object {
        private const val ITEM_ID_KEY = "itemIdKey"
        private const val TITLE_RES_KEY = "titleResId"
    }

    private var currentItemId: Int = R.id.menu_navigation_bottom_view_scan
    private var titleRes: Int = R.string.title_scan

    private val mainCameraXScannerFragment: MainCameraXScannerFragment by inject()
    private val mainHistoryFragment: MainBarcodeHistoryFragment by inject()
    private val mainBarcodeCreatorListFragment: MainBarcodeCreatorListFragment by inject()
    private val mainSettingsFragment: MainSettingsFragment by inject()
    private val mainVaultFragment: MainVaultFragment by inject()

    private val viewBinding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    override val rootView: View get() = viewBinding.root

    private val headerBinding: TemplateMainHeaderBinding by lazy {
        TemplateMainHeaderBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            configureShortcuts()
        }

        savedInstanceState?.let {
            currentItemId = it.getInt(ITEM_ID_KEY, R.id.menu_navigation_bottom_view_scan)
            titleRes = it.getInt(TITLE_RES_KEY, R.string.title_scan)
        }

        setSupportActionBar(viewBinding.activityMainActivityLayout.toolbar)
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(false)// On n'affiche pas l'icone "retour" dans la MainActivity
            it.setDisplayShowTitleEnabled(false)
            it.setTitle(titleRes)
        }
        configureHeader(viewBinding.activityMainActivityLayout.toolbar)

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            configureNavigationRailView()
        } else {
            configureBottomNavigationMenu()
        }

        setContentView(rootView)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(ITEM_ID_KEY, currentItemId)
        outState.putInt(TITLE_RES_KEY, titleRes)
        super.onSaveInstanceState(outState)
    }

    // ---- Configuration ----

    /**
     * Permet de coloriser la bar de navigation (en bas) de la même couleur que la BottomNavigationView.
     */
    private fun configureNavigationBarColor(bottomNavigationView: BottomNavigationView) {
        if (Build.VERSION.SDK_INT < 35 && (Build.VERSION.SDK_INT >= 27 || settingsManager.useDarkTheme())) {
            val bottomNavigationViewBackground = bottomNavigationView.background
            if (bottomNavigationViewBackground is MaterialShapeDrawable) {
                @Suppress("DEPRECATION")
                this.window.navigationBarColor = bottomNavigationViewBackground.resolvedTintColor
            }
        }
    }

    private fun configureHeader(toolbar: MaterialToolbar) {
        toolbar.addView(headerBinding.root, Toolbar.LayoutParams(
            Toolbar.LayoutParams.MATCH_PARENT,
            Toolbar.LayoutParams.WRAP_CONTENT
        ))
        // Header fixe : le contenu (caméra, boutons du bas) garde toute sa hauteur visible
        (toolbar.layoutParams as? AppBarLayout.LayoutParams)?.scrollFlags = 0
        headerBinding.templateMainHeaderSettingsButton.setOnClickListener {
            selectItem(R.id.menu_navigation_bottom_view_settings)
        }
    }

    private fun updateHeader(id: Int) {
        when (id) {
            R.id.menu_navigation_bottom_view_scan -> showHeader(R.string.app_name, R.string.header_scan_subtitle, R.drawable.ic_bar_code_24, highlighted = true)
            R.id.menu_navigation_bottom_view_vault -> showHeader(R.string.header_vault_title, R.string.header_vault_subtitle, R.drawable.outline_inventory_24)
            R.id.menu_navigation_bottom_view_history -> showHeader(R.string.title_history, R.string.header_history_subtitle, R.drawable.baseline_history_24)
            R.id.menu_navigation_bottom_view_create -> showHeader(R.string.title_bar_code_creator, R.string.header_generate_subtitle, R.drawable.baseline_create_24)
            R.id.menu_navigation_bottom_view_settings -> showHeader(R.string.title_settings, R.string.header_settings_subtitle, R.drawable.baseline_settings_24)
        }
        headerBinding.templateMainHeaderSettingsButton.visibility =
            if (id == R.id.menu_navigation_bottom_view_scan) View.VISIBLE else View.GONE
        headerBinding.templateMainHeaderSyncedTextView.visibility =
            if (id == R.id.menu_navigation_bottom_view_vault) View.VISIBLE else View.GONE
    }

    /**
     * Le "highlighted" header (onglet Scan) utilise une tuile sombre et un statut vert.
     */
    private fun showHeader(@StringRes title: Int, @StringRes subtitle: Int, @DrawableRes icon: Int, highlighted: Boolean = false) {
        headerBinding.templateMainHeaderTitle.setText(title)
        headerBinding.templateMainHeaderSubtitle.setText(subtitle)
        headerBinding.templateMainHeaderIcon.setImageResource(icon)
        if (highlighted) {
            headerBinding.templateMainHeaderTile.setBackgroundResource(R.drawable.header_tile)
            headerBinding.templateMainHeaderIcon.setColorFilter(getColor(R.color.header_tile_icon))
            headerBinding.templateMainHeaderSubtitle.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.status_dot, 0, 0, 0)
            headerBinding.templateMainHeaderSubtitle.setTextColor(MaterialColors.getColor(headerBinding.root, androidx.appcompat.R.attr.colorPrimary))
        } else {
            headerBinding.templateMainHeaderTile.setBackgroundResource(R.drawable.icon_badge)
            headerBinding.templateMainHeaderIcon.setColorFilter(MaterialColors.getColor(headerBinding.root, com.google.android.material.R.attr.colorOnPrimaryContainer))
            headerBinding.templateMainHeaderSubtitle.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
            headerBinding.templateMainHeaderSubtitle.setTextColor(MaterialColors.getColor(headerBinding.root, com.google.android.material.R.attr.colorOnSurfaceVariant))
        }
    }

    private fun selectItem(id: Int) {
        viewBinding.activityMainMenuBottomNavigation?.selectedItemId = id
        viewBinding.activityMainNavigationRail?.selectedItemId = id
    }

    fun showScanTab() = selectItem(R.id.menu_navigation_bottom_view_scan)

    private fun configureBottomNavigationMenu() {
        viewBinding.activityMainMenuBottomNavigation?.let { bottomNavigationView ->
            configureNavigationBarColor(bottomNavigationView)
            bottomNavigationView.selectedItemId = currentItemId
            bottomNavigationView.setOnItemSelectedListener {
                changeView(it.itemId)
            }
            initializeFirstFragmentToShow {
                bottomNavigationView.selectedItemId = it
            }
        }
    }

    private fun configureNavigationRailView() {
        viewBinding.activityMainNavigationRail?.let { navigationRailView ->
            navigationRailView.selectedItemId = currentItemId
            navigationRailView.setOnItemSelectedListener {
                changeView(it.itemId)
            }
            initializeFirstFragmentToShow {
                navigationRailView.selectedItemId = it
            }
        }
    }

    private fun initializeFirstFragmentToShow(onChangeItem: (selectedItemId: Int) -> Unit) {
        if(supportFragmentManager.fragments.isEmpty()) {
            // Utile lorsqu'on ouvre l'application via un shortcut
            val selectedItemId = when (intent?.action) {
                "${BuildConfig.APPLICATION_ID}.SCAN" -> R.id.menu_navigation_bottom_view_scan
                "${BuildConfig.APPLICATION_ID}.HISTORY" -> R.id.menu_navigation_bottom_view_history
                "${BuildConfig.APPLICATION_ID}.CREATE" -> R.id.menu_navigation_bottom_view_create
                "android.intent.action.APPLICATION_PREFERENCES" -> R.id.menu_navigation_bottom_view_settings
                else -> currentItemId
            }
            onChangeItem(selectedItemId)
            changeView(selectedItemId)
        }
    }

    // ---- Change Fragment ----

    private fun changeView(id: Int): Boolean {
        currentItemId = id
        updateHeader(id)
        return when (id) {
            R.id.menu_navigation_bottom_view_scan -> changeFragment(
                mainCameraXScannerFragment,
                R.string.title_scan
            )

            R.id.menu_navigation_bottom_view_vault -> changeFragment(
                mainVaultFragment,
                R.string.header_vault_title
            )

            R.id.menu_navigation_bottom_view_history -> changeFragment(
                mainHistoryFragment,
                R.string.title_history
            )

            R.id.menu_navigation_bottom_view_create -> changeFragment(
                mainBarcodeCreatorListFragment,
                R.string.title_bar_code_creator
            )

            R.id.menu_navigation_bottom_view_settings -> changeFragment(
                mainSettingsFragment,
                R.string.title_settings
            )

            else -> false
        }
    }

    private fun changeFragment(fragment: Fragment, titleResource: Int): Boolean {
        titleRes = titleResource
        supportActionBar?.setTitle(titleResource)
        supportFragmentManager.commitNow {
            setReorderingAllowed(false)
            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            replace(viewBinding.activityMainFrameLayout.id, fragment)
            //addToBackStack(null) // Permet de revenir aux fragments affichés précédement via le bouton back
        }
        return true
    }

    // ---- Shortcuts ----

    private val shortcutViewModel: DynamicShortcutViewModel by viewModel()

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun configureShortcuts() {
        shortcutViewModel.createShortcuts() // Create shortcuts if they don't exist
    }

    // ---- UI ----

    fun showSnackbar(text: String) {
        showSnackbar(
            text = text,
            anchorView = viewBinding.activityMainMenuBottomNavigation
        )
    }

    fun showSnackbar(text: String, actionText: String, action: (View) -> Unit) {
        showSnackbar(
            text = text,
            actionText = actionText,
            action = action,
            anchorView = viewBinding.activityMainMenuBottomNavigation
        )
    }

    // ---- Theme ----

    fun updateTheme() {
        settingsManager.reload()
        recreate()
    }
}