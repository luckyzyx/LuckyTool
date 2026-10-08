package com.luckyzyx.luckytool.ui.compose.scopes.related

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.luckyzyx.luckytool.BuildConfig
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.contract.CropImageContract
import com.luckyzyx.luckytool.data.CropImageContractOptions
import com.luckyzyx.luckytool.ui.compose.components.PrefRow
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.FileUtils
import com.luckyzyx.luckytool.utils.LogUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.putString
import com.luckyzyx.luckytool.utils.showToast

/**
 * 指纹相关页（旧 ui.fragment.scopes.related.FingerPrintRelated 的 Compose 等价物）。
 * 点击"替换指纹图标路径"项 → CropImageContract 裁剪选图（与旧实现同源），成功后
 * putString 写入路径 + restart。
 */
object FingerPrintRelatedPage {

    val spec = ScopePageSpec(
        pageKey = "finger_print_related",
        prefsName = ModulePrefs,
        packName = "com.android.systemui",
        scopes = arrayOf("com.android.systemui"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        list(
            key = "remove_fingerprint_icon_mode",
            title = c.getString(R.string.remove_fingerprint_icon_mode),
            entries = c.resources.getStringArray(R.array.remove_fingerprint_icon_mode_entries),
            entryValues = arrayOf("0", "1", "2", "3"),
            default = "0",
            summary = c.getString(R.string.current_mode) + ": %s",
        )
        switch(
            key = "replace_fingerprint_icon_switch",
            title = c.getString(R.string.replace_fingerprint_icon_switch),
            summary = c.getString(R.string.replace_fingerprint_icon_switch_summary),
            onChange = { restart?.invoke() },
        )
        if (state.getBoolean("replace_fingerprint_icon_switch")) {
            val pathTitle = c.getString(R.string.replace_fingerprint_icon_path)
            val path = state.getString("replace_fingerprint_icon_path", "") ?: ""
            val pathSummary = if (path.isBlank()) "Null" else path
            custom(
                key = "replace_fingerprint_icon_path",
                title = pathTitle,
                summary = pathSummary,
            ) {
                val launcher = rememberLauncherForActivityResult(CropImageContract()) {
                    if (it.second.isSuccessful) {
                        val uri = it.second.uriContent
                        val resultPath = uri?.path ?: ""
                        if (uri == null || uri == Uri.EMPTY) {
                            return@rememberLauncherForActivityResult
                        }
                        if (resultPath.isNotBlank()) {
                            c.showToast(resultPath)
                            c.putString(ModulePrefs, it.first, resultPath)
                            restart?.invoke()
                        }
                    } else {
                        LogUtils.e("CropImage", it.first, it.second.error.toString(), true)
                    }
                }
                val bitmap = remember(path) {
                    if (path.isBlank()) null else BitmapFactory.decodeFile(path)
                }
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    PrefRow(
                        title = pathTitle,
                        summary = pathSummary,
                        leading = {
                            bitmap?.let {
                                Image(
                                    it.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                )
                            }
                        },
                        onClick = {
                            val cacheImageFile = FileUtils.createCacheFile(c, "png")
                            val cacheImageUri = FileProvider.getUriForFile(
                                c, "${BuildConfig.APPLICATION_ID}.FileProvider", cacheImageFile
                            )
                            launcher.launch(
                                "replace_fingerprint_icon_path" to CropImageContractOptions(
                                    null, CropImageOptions().apply {
                                        activityTitle = pathTitle
                                        cropShape = CropImageView.CropShape.RECTANGLE
                                        guidelines = CropImageView.Guidelines.ON_TOUCH
                                        aspectRatioX = 216
                                        aspectRatioY = 216
                                        maxCropResultWidth = 216
                                        maxCropResultHeight = 216
                                        fixAspectRatio = true
                                        customOutputUri = cacheImageUri
                                        outputCompressFormat = Bitmap.CompressFormat.PNG
                                        outputCompressQuality = 100
                                    }
                                )
                            )
                        },
                    )
                }
            }
        }
    }
}
