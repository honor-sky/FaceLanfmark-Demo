package com.google.mediapipe.examples.facelandmarker.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.View

class CircleOutlineView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = 0xFF000000.toInt()  // 검정색
        style = Paint.Style.STROKE  // 테두리만 그리기
        strokeWidth = 5f            // 테두리 두께
        isAntiAlias = true          // 곡선 부드럽게
    }

    // cm를 픽셀로 변환하는 함수
    private fun cmToPx(cm: Float): Float {
        val metrics: DisplayMetrics = resources.displayMetrics
        return (cm * metrics.xdpi) / 2.54f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val diameterInPx = cmToPx(5.0f)  // 11.7cm → 픽셀로 변환
        val radius = diameterInPx / 2

        val centerX = width / 2f
        val centerY = height / 2f

        // 원의 테두리 그리기
        canvas.drawCircle(centerX, centerY, radius, paint)
    }
}