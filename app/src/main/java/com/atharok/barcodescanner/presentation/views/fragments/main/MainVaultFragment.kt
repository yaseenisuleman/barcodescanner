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

package com.atharok.barcodescanner.presentation.views.fragments.main

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.atharok.barcodescanner.R
import com.atharok.barcodescanner.common.extensions.getColorStateListFromAttrRes
import com.atharok.barcodescanner.databinding.FragmentMainVaultBinding
import com.atharok.barcodescanner.databinding.RecyclerViewItemVaultBinding
import com.atharok.barcodescanner.presentation.views.activities.BaseActivity
import com.atharok.barcodescanner.presentation.views.activities.MainActivity

/**
 * Stock Vault: interface uniquement (données d'exemple), aucune logique d'inventaire.
 */
class MainVaultFragment : Fragment() {

    private data class SampleItem(
        val sku: String,
        val name: String,
        val barcode: String,
        val location: String,
        val counted: Int,
        val expected: Int,
        val synced: Boolean
    )

    private val sampleItems = listOf(
        SampleItem("EX-001", "Example Stock Item", "6001234567890", "Aisle 1 / A1", 12, 12, true),
        SampleItem("PRD-042", "Cleaning Solution 5L", "6009876543210", "Aisle 2 / B3", 5, 5, true),
        SampleItem("TOOL-017", "Cordless Drill 18V", "6012345678901", "Bin C4", 3, 3, true),
        SampleItem("SAF-006", "Safety Gloves (Pair)", "6023456789012", "Aisle 3 / D1", 8, 10, false),
        SampleItem("FIX-003", "Duct Tape 50mm", "6034567890123", "Bin E2", 4, 4, true)
    )

    private var _binding: FragmentMainVaultBinding? = null
    private val viewBinding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMainVaultBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sampleItems.forEach { addItemView(viewBinding.fragmentMainVaultList, it) }

        viewBinding.fragmentMainVaultScanNextButton.setOnClickListener {
            (requireActivity() as? MainActivity)?.showScanTab()
        }
        viewBinding.fragmentMainVaultExportButton.setOnClickListener { showNotAvailable() }
        viewBinding.fragmentMainVaultFilterButton.setOnClickListener { showNotAvailable() }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val activity: Activity = requireActivity()
        if(activity is BaseActivity){
            activity.lockDeviceRotation(false)
        }
    }

    private fun addItemView(parent: LinearLayout, item: SampleItem) {
        val itemBinding = RecyclerViewItemVaultBinding.inflate(layoutInflater, parent, false)
        itemBinding.recyclerViewItemVaultSkuTextView.text = getString(R.string.vault_sku_label, item.sku)
        itemBinding.recyclerViewItemVaultTitleTextView.text = item.name
        itemBinding.recyclerViewItemVaultBarcodeTextView.text = item.barcode
        itemBinding.recyclerViewItemVaultLocationTextView.text = item.location
        itemBinding.recyclerViewItemVaultCountedTextView.text =
            getString(R.string.vault_counted_value, item.counted, item.expected)

        val status = itemBinding.recyclerViewItemVaultStatusTextView
        if (item.synced) {
            status.setText(R.string.status_synced)
        } else {
            val context = requireContext()
            status.setText(R.string.status_pending)
            status.backgroundTintList = context.getColorStateList(R.color.status_pending_background)
            status.setTextColor(context.getColorStateListFromAttrRes(R.attr.colorMedium))
            status.compoundDrawableTintList = context.getColorStateListFromAttrRes(R.attr.colorMedium)
        }
        itemBinding.root.setOnClickListener { showNotAvailable() }
        parent.addView(itemBinding.root)
    }

    private fun showNotAvailable() {
        (requireActivity() as? MainActivity)?.showSnackbar(getString(R.string.placeholder_not_available))
    }
}
