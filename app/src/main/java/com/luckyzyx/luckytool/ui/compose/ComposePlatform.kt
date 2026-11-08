package com.luckyzyx.luckytool.ui.compose

import android.os.Build

/** 背景模糊依赖 RenderEffect，API 31 起才有；低于该版本底栏与胶囊退回不透明样式。 */
internal fun isRenderEffectSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
