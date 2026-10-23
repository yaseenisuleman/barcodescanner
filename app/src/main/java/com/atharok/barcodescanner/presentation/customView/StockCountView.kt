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

package com.atharok.barcodescanner.presentation.customView

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.atharok.barcodescanner.R
import com.atharok.barcodescanner.databinding.TemplateStockCountBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Comptage de stock : interface uniquement (valeurs d'exemple), rien n'est enregistré.
 */
class StockCountView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): LinearLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val SAMPLE_SYSTEM_QUANTITY = 10
        private const val SAMPLE_COUNTED_QUANTITY = 12
    }

    private val viewBinding = TemplateStockCountBinding.inflate(LayoutInflater.from(context), this)

    private var countedQuantity = SAMPLE_COUNTED_QUANTITY

    init {
        orientation = VERTICAL

        viewBinding.templateStockCountSystemQuantityTextView.text = SAMPLE_SYSTEM_QUANTITY.toString()
        viewBinding.templateStockCountLastScannedTextView.text =
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        viewBinding.templateStockCountDecreaseButton.setOnClickListener {
            if (countedQuantity > 0) {
                countedQuantity--
                updateQuantity()
            }
        }
        viewBinding.templateStockCountIncreaseButton.setOnClickListener {
            countedQuantity++
            updateQuantity()
        }

        updateQuantity()
    }

    private fun updateQuantity() {
        viewBinding.templateStockCountQuantityTextView.text = countedQuantity.toString()
        viewBinding.templateStockCountCountedQuantityTextView.text = countedQuantity.toString()

        val difference = countedQuantity - SAMPLE_SYSTEM_QUANTITY
        if (difference == 0) {
            viewBinding.templateStockCountDiscrepancyLayout.visibility = View.GONE
        } else {
            viewBinding.templateStockCountDiscrepancyLayout.visibility = View.VISIBLE
            viewBinding.templateStockCountDiscrepancyTextView.text = context.getString(
                if (difference > 0) R.string.discrepancy_value else R.string.discrepancy_value_negative,
                abs(difference), SAMPLE_SYSTEM_QUANTITY, countedQuantity
            )
        }
    }
}
