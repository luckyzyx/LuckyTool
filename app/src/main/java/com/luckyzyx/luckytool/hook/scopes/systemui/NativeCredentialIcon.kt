package com.luckyzyx.luckytool.hook.scopes.systemui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.DrawableWrapper
import android.widget.ImageView
import java.lang.ref.WeakReference

internal class NativeCredentialIcon(icon: Drawable, view: ImageView) : DrawableWrapper(icon.mutate()) {
    private val owner = WeakReference(view)

    override fun draw(canvas: Canvas) {
        val view = owner.get()
        // drawIconForTransaction 清空 callback 后直接绘制，绕过了原生 ImageView 的 bounds 和矩阵。
        if (callback != null || view == null || view.drawable !== this ||
            view.width <= 0 || view.height <= 0
        ) {
            super.draw(canvas)
            return
        }
        val contentWidth = view.width - view.paddingLeft - view.paddingRight
        val contentHeight = view.height - view.paddingTop - view.paddingBottom
        if (contentWidth <= 0 || contentHeight <= 0) return
        val icon = drawable ?: return
        val previousBounds = Rect(icon.bounds)
        val save = canvas.save()
        try {
            canvas.translate(
                bounds.exactCenterX() - view.width / 2f,
                bounds.exactCenterY() - view.height / 2f,
            )
            if (view.cropToPadding) {
                canvas.clipRect(
                    view.scrollX + view.paddingLeft, view.scrollY + view.paddingTop,
                    view.scrollX + view.width - view.paddingRight,
                    view.scrollY + view.height - view.paddingBottom,
                )
            }
            canvas.translate(view.paddingLeft.toFloat(), view.paddingTop.toFloat())
            canvas.concat(view.imageMatrix)
            val left = (intrinsicWidth - contentWidth) / 2
            val top = (intrinsicHeight - contentHeight) / 2
            icon.setBounds(left, top, left + contentWidth, top + contentHeight)
            icon.draw(canvas)
        } finally {
            icon.bounds = previousBounds
            canvas.restoreToCount(save)
        }
    }

    companion object {
        @SuppressLint("DiscouragedApi")
        fun create(context: Context, view: ImageView): Drawable {
            val id = context.resources.getIdentifier("ic_lock", "drawable", "com.android.systemui")
            check(id != 0) { "Native lock icon unavailable" }
            return NativeCredentialIcon(checkNotNull(context.getDrawable(id)), view)
        }
    }
}
