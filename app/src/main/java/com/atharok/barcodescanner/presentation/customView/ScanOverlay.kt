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
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toRect
import com.atharok.barcodescanner.R

class ScanOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
): View(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        const val RATIO = 0.7f
    }

    private val viewfinderRadius: Float

    private val backgroundPaint: Paint

    // Masque = plein écran moins le viseur (EVEN_ODD) : pas besoin de calque logiciel ni de CLEAR
    private val maskPath: Path = Path().apply { fillType = Path.FillType.EVEN_ODD }

    private val viewfinderCornerPaint: Paint

    // Longueur des segments droits des coins (0 = coins en arc uniquement)
    private val viewfinderCornerLength: Float
    private val viewfinderCornerPath: Path = Path()

    private val scanLineColor: Int
    private val scanLinePaint: Paint?
    private val scanLineGlowPaint: Paint?
    private val scanLineGlowRect: RectF = RectF()

    private val viewfinderRect: RectF = RectF()

    fun getViewfinderRect(): Rect = viewfinderRect.toRect()

    init{
        context.theme.obtainStyledAttributes(attrs, R.styleable.ScanOverlay, defStyleAttr, defStyleRes).apply {
            try {
                viewfinderRadius = getDimension(R.styleable.ScanOverlay_viewfinder_radius, getDP(40f))
                viewfinderCornerPaint = Paint(ANTI_ALIAS_FLAG).apply {
                    color = getColor(R.styleable.ScanOverlay_viewfinder_corner_color, Color.WHITE)
                    style = Paint.Style.STROKE
                    strokeWidth = getDimension(R.styleable.ScanOverlay_viewfinder_corner_thickness, getDP(2f))
                    strokeCap = Paint.Cap.ROUND
                }
                viewfinderCornerLength = getDimension(R.styleable.ScanOverlay_viewfinder_corner_length, 0f)
                scanLineColor = getColor(R.styleable.ScanOverlay_scan_line_color, Color.TRANSPARENT)
                if (scanLineColor != Color.TRANSPARENT) {
                    scanLinePaint = Paint(ANTI_ALIAS_FLAG).apply {
                        color = scanLineColor
                        style = Paint.Style.STROKE
                        strokeWidth = getDP(2f)
                    }
                    // Halo en dégradé (moins coûteux qu'un BlurMaskFilter en rendu logiciel)
                    scanLineGlowPaint = Paint().apply {
                        style = Paint.Style.FILL
                    }
                } else {
                    scanLinePaint = null
                    scanLineGlowPaint = null
                }
                backgroundPaint = Paint().apply {
                    color = getColor(R.styleable.ScanOverlay_overlay_mask_color, Color.parseColor("#80000000"))
                    style = Paint.Style.FILL
                }
            } finally {
                recycle()
            }
        }
    }

    private fun getDP(value: Float): Float = value * Resources.getSystem().displayMetrics.density

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        calculateRectangleDimension(measuredWidth, measuredHeight)
    }

    protected override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Background (with the viewfinder cut out)
        canvas.drawPath(maskPath, backgroundPaint)

        if (viewfinderCornerLength > 0f) {
            drawBracketCorners(canvas)
        } else {
            drawArcCorners(canvas)
        }

        // Scan line
        if (scanLinePaint != null && scanLineGlowPaint != null) {
            val y = viewfinderRect.centerY()
            canvas.drawRect(scanLineGlowRect, scanLineGlowPaint)
            canvas.drawLine(scanLineGlowRect.left, y, scanLineGlowRect.right, y, scanLinePaint)
        }
    }

    private fun drawBracketCorners(canvas: Canvas) {
        val r = viewfinderRadius
        val l = viewfinderCornerLength
        val rect = viewfinderRect

        viewfinderCornerPath.reset()

        // Top Left
        viewfinderCornerPath.moveTo(rect.left, rect.top + r + l)
        viewfinderCornerPath.lineTo(rect.left, rect.top + r)
        viewfinderCornerPath.arcTo(rect.left, rect.top, rect.left + 2 * r, rect.top + 2 * r, 180f, 90f, false)
        viewfinderCornerPath.lineTo(rect.left + r + l, rect.top)

        // Top Right
        viewfinderCornerPath.moveTo(rect.right - r - l, rect.top)
        viewfinderCornerPath.lineTo(rect.right - r, rect.top)
        viewfinderCornerPath.arcTo(rect.right - 2 * r, rect.top, rect.right, rect.top + 2 * r, 270f, 90f, false)
        viewfinderCornerPath.lineTo(rect.right, rect.top + r + l)

        // Bottom Right
        viewfinderCornerPath.moveTo(rect.right, rect.bottom - r - l)
        viewfinderCornerPath.lineTo(rect.right, rect.bottom - r)
        viewfinderCornerPath.arcTo(rect.right - 2 * r, rect.bottom - 2 * r, rect.right, rect.bottom, 0f, 90f, false)
        viewfinderCornerPath.lineTo(rect.right - r - l, rect.bottom)

        // Bottom Left
        viewfinderCornerPath.moveTo(rect.left + r + l, rect.bottom)
        viewfinderCornerPath.lineTo(rect.left + r, rect.bottom)
        viewfinderCornerPath.arcTo(rect.left, rect.bottom - 2 * r, rect.left + 2 * r, rect.bottom, 90f, 90f, false)
        viewfinderCornerPath.lineTo(rect.left, rect.bottom - r - l)

        canvas.drawPath(viewfinderCornerPath, viewfinderCornerPaint)
    }

    private fun drawArcCorners(canvas: Canvas) {
        // Corner
        val cornerSize = viewfinderRadius * 2f

        // Top Left
        canvas.drawArc(viewfinderRect.left, viewfinderRect.top, viewfinderRect.left+cornerSize, viewfinderRect.top+cornerSize, -90f, -90f, false, viewfinderCornerPaint)

        //Top Right
        canvas.drawArc(viewfinderRect.right-cornerSize, viewfinderRect.top, viewfinderRect.right, viewfinderRect.top+cornerSize, -90f, 90f, false, viewfinderCornerPaint)

        // Bottom Right
        canvas.drawArc(viewfinderRect.right-cornerSize, viewfinderRect.bottom-cornerSize, viewfinderRect.right, viewfinderRect.bottom, 90f, -90f, false, viewfinderCornerPaint)

        // Bottom Left
        canvas.drawArc(viewfinderRect.left, viewfinderRect.bottom-cornerSize, viewfinderRect.left+cornerSize, viewfinderRect.bottom, 90f, 90f, false, viewfinderCornerPaint)
    }

    private fun calculateRectangleDimension(width: Int, height: Int) {
        val overlayWidth = width.toFloat()
        val overlayHeight = height.toFloat()

        val viewfinderSize = overlayHeight.coerceAtMost(overlayWidth) * RATIO

        val centerX = overlayWidth / 2f
        val centerY = overlayHeight / 2f

        val left = centerX - viewfinderSize / 2f
        val right = centerX + viewfinderSize / 2f
        val top = centerY - viewfinderSize / 2f
        val bottom = centerY + viewfinderSize / 2f

        viewfinderRect.set(left, top, right, bottom)

        maskPath.reset()
        maskPath.addRect(0f, 0f, overlayWidth, overlayHeight, Path.Direction.CW)
        maskPath.addRoundRect(viewfinderRect, viewfinderRadius, viewfinderRadius, Path.Direction.CW)

        scanLineGlowPaint?.let {
            val inset = viewfinderSize * 0.08f
            val glowHeight = getDP(8f)
            scanLineGlowRect.set(left - inset, centerY - glowHeight, right + inset, centerY + glowHeight)
            val glowColor = Color.argb(110, Color.red(scanLineColor), Color.green(scanLineColor), Color.blue(scanLineColor))
            it.shader = LinearGradient(
                0f, scanLineGlowRect.top, 0f, scanLineGlowRect.bottom,
                intArrayOf(Color.TRANSPARENT, glowColor, Color.TRANSPARENT), null,
                Shader.TileMode.CLAMP
            )
        }
    }
}