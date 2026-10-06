@file:Suppress("unused")

package com.luckyzyx.luckytool.utils

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.os.SystemProperties
import android.provider.Settings
import android.service.quicksettings.TileService
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import android.util.ArrayMap
import android.util.Base64
import android.util.TypedValue
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.MenuRes
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.drake.net.utils.withDefault
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.highcapable.betterandroid.system.extension.component.clipboardManager
import com.highcapable.betterandroid.system.extension.component.copy
import com.highcapable.betterandroid.system.extension.component.sendBroadcast
import com.highcapable.betterandroid.ui.extension.component.base.getDrawableCompat
import com.highcapable.betterandroid.ui.extension.graphics.decodeToBitmapOrNull
import com.highcapable.betterandroid.ui.extension.view.toast
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.factory.dataChannel
import com.luckyzyx.luckytool.BuildConfig
import com.luckyzyx.luckytool.IGlobalFuncController
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.DisplayMode
import com.luckyzyx.luckytool.ui.activity.MainActivity
import com.oplus.miragewindow.OplusMirageOptions
import com.oplus.miragewindow.OplusMirageWindowManager
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ShellUtils
import com.topjohnwu.superuser.ipc.RootService
import org.json.JSONArray
import java.io.File
import java.util.regex.Pattern
import kotlin.math.roundToLong
import kotlin.random.Random
import kotlin.system.exitProcess

/**
 * 获取设备信息
 * @receiver Context
 * @return String
 */
fun Context.getDeviceInfo(
    controller: IGlobalFuncController? = null, isLog: Boolean = false
): String {
    val androidVer = "Android ${Build.VERSION.RELEASE}(${Build.VERSION.SDK_INT})"
    val osVer = "OS $getOSVersionName($getOSVersionCode)"
    return ArrayList<String>().apply {
        if (isLog) {
            add("${getString(R.string.module_version)} $getVersionName($getVersionCode)")
            add("${getString(R.string.root_source)} ${DeviceUtils.getRootVersion(this@getDeviceInfo)}")
            add("${getString(R.string.framework_version)} ${DeviceUtils.getFrameWorkVersion(this@getDeviceInfo)}")
        }
        add("${getString(R.string.model)}: $getFingerPrintBrand $getFingerPrintModel ${getModelMarketName()}")
        add("${getString(R.string.product)}: ${Build.PRODUCT} ${Build.DEVICE} ${controller?.cpuInfo} ${controller?.prjNameInfo} ${controller?.slotInfo}")
        add("${getString(R.string.system)}: $androidVer $osVer")
        add(
            "${getString(R.string.build_version)}: ${Build.DISPLAY} ${
                getManifestEndVersion(controller?.manifestVersion)
            }"
        )
        add("${getString(R.string.version)}: ${controller?.otaVersion}")
        add("${getString(R.string.flash)}: ${controller?.flashInfo}")

        if (controller?.snInfo == controller?.chipInfo) {
            add("PAS: ${controller?.pcbInfo} ${controller?.snInfo}")
        } else {
            add("POS: ${controller?.pcbInfo}")
            add("SAS: ${controller?.snInfo} ${controller?.chipInfo}")
        }

        if (isLog) {
            add(getMyManifesstVersion())
            add(DeviceUtils.getGuid())
            add(DeviceUtils.getRecruitId())
            add(DeviceUtils.getRegisterId())
        }
    }.let { formatStringAuto(it, "\n") }
}

/**
 * 检测包名是否存在
 * @receiver Context
 * @param packName String
 * @return Boolean
 */
fun Context.checkPackName(packName: String): Boolean {
    return PackageUtils(packageManager).getPackageInfo(packName, 0) != null
}

/**
 * 判断Activity是否存在
 * @receiver Context
 * @param intent Intent
 * @return Boolean
 */
fun Context.checkResolveActivity(intent: Intent): Boolean = safeOfFalse {
    return PackageUtils(packageManager).resolveActivity(intent, 0) != null
}

/**
 * 判断Activity是否存在
 * @receiver Context
 * @param packName String
 * @param className String
 * @return Boolean
 */
fun Context.checkResolveActivity(packName: String, className: String): Boolean = safeOfFalse {
    return checkResolveActivity(Intent().setClassName(packName, className))
}

/**
 * 跳转到APP
 * @receiver Context
 * @param packNames Array<String>
 */
fun Context.openApp(packNames: Array<String>) {
    openApp(packNames.firstOrNull())
}

/**
 * 跳转到APP
 * @receiver Context
 * @param packName String
 */
fun Context.openApp(packName: String?) {
    if (packName.isNullOrBlank()) return
    PackageUtils(packageManager).getLaunchIntentForPackage(packName)?.apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        startActivity(this)
    }
}

/**
 * Toast快捷方法
 * @receiver Context
 * @param id Int
 * @param long Boolean?
 */
fun Context.showToast(id: Int, long: Boolean? = false) {
    showToast(getString(id), long)
}

fun Context.showToast(msg: String, long: Boolean? = false) = if (long == true) {
    toast(msg, Toast.LENGTH_LONG)
} else {
    toast(msg)
}

/**
 * 获取自定义刷新率
 * @return [List]
 */
fun getFpsMode1(): ArrayList<Any?> {
    return ArrayList<Any?>().apply {
        add(0, DisplayMode(0, refreshRate = 30.0F))
        add(1, DisplayMode(1, refreshRate = 60.0F))
        add(2, DisplayMode(2, refreshRate = 90.0F))
        add(3, DisplayMode(3, refreshRate = 120.0F))
        add(4, DisplayMode(4, refreshRate = 144.0F))
    }
}

/**
 * 获取设备刷新率
 * @receiver Context
 * @return Array<String>
 */
fun getFpsMode2(): ArrayList<DisplayMode> {
    val list = ArrayList<DisplayMode>()
    val shellResults = ArrayList<String>()
    val command = CommandUtils.getFpsMode2
    Shell.cmd(command).to(shellResults).exec()
    shellResults.forEachIndexed { index, mode ->
        shellResults[index] = mode.replaceSpace.removePrefix("DisplayMode{").removeSuffix("}")
    }
    shellResults.forEach {
        val displayMode = DisplayMode()
        it.split(",").forEachIndexed { _, mode ->
            if (!mode.contains("=")) return@forEachIndexed
            val key = mode.substringBefore("=")
            val value = mode.substringAfter("=")
            when (key) {
                "id" -> displayMode.id = value.toInt()
                "width" -> displayMode.width = value.toInt()
                "height" -> displayMode.height = value.toInt()
                "xDpi" -> displayMode.xDpi = value.toFloat()
                "yDpi" -> displayMode.yDpi = value.toFloat()
                "refreshRate" -> displayMode.refreshRate = value.toFloat()
            }
        }
        list.add(displayMode)
    }
    return list
}

/**
 * 设置刷新率
 * @param context Context
 * @param refresh String?
 * @param name String
 */
fun setRefresh(context: Context, name: String, refresh: String?) {
    setParameter(context, name, "min_refresh_rate", refresh)
    setParameter(context, name, "peak_refresh_rate", refresh)
}

fun setRefresh(context: Context, name: String, minRefresh: String?, peakRefresh: String?) {
    setParameter(context, name, "min_refresh_rate", minRefresh)
    setParameter(context, name, "peak_refresh_rate", peakRefresh)
}

fun setParameter(context: Context, name: String, key: String?, value: String?) {
    val contentResolver = context.contentResolver
    safeOf({
        context.showToast("apply $name Hz failed!")
    }) {
        val contentValues = ContentValues(2)
        contentValues.put("name", key)
        contentValues.put("value", value)
        contentResolver.insert("content://settings/system".toUri(), contentValues)
//        context.toast("apply $name Hz success!")
    }
}

/**
 * 获取机型市场名
 * @return String?
 */
fun getModelMarketName(): String? {
    return SystemProperties.get("ro.vendor.oplus.market.name")
}

/**
 * 获取MyManifest版本
 * @return String?
 */
fun getMyManifesstVersion(): String {
    val str = SystemProperties.get("ro.oplus.image.my_manifest.version", "")
    val str2 = SystemProperties.get("ro.oplus.version.my_manifest", "")
//            return SystemProperties.get("ro.build.version.ota", "null")
    return str.ifBlank { str2.ifBlank { "null" } }
}

/**
 * 获取prop数据
 * @param key String
 * @return String
 */
fun getProp(key: String): String = ShellUtils.fastCmd("${CommandUtils.getprop} $key").let {
    if (it.isBlank()) "null" else formatSpace(it)
}

/**
 * 获取prop数据
 * @param key String
 * @return String
 */
fun getProp(key: String, def: String): String =
    ShellUtils.fastCmd("${CommandUtils.getprop} $key").let {
        if (it.isBlank()) def else formatSpace(it)
    }

/**
 * 发送广播以关闭折叠面板
 * @receiver TileService
 */
@SuppressLint("StartActivityAndCollapseDeprecated")
fun TileService.closeCollapse() {
    try {
        sendBroadcast { action = "LuckyTool_CloseCollapse" }
    } catch (_: Exception) {
        try {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(Intent(Intent.ACTION_VIEW).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {

        }
    }
}

/**
 * 正常编码中一般只会用到 [dp]/[sp] ;
 * 其中[dp]/[sp] 会根据系统分辨率将输入的dp/sp值转换为对应的px
 */
val Float.dp: Float // [xxhdpi](360 -> 1080)
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this, Resources.getSystem().displayMetrics
    )

val Int.dp: Int
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    ).toInt()

val Float.sp: Float // [xxhdpi](360 -> 1080)
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, this, Resources.getSystem().displayMetrics
    )

val Int.sp: Int
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    ).toInt()

/**
 * 复制到剪贴板
 * @receiver Context
 * @param string CharSequence
 */
fun Context.copyStr(string: CharSequence) {
    clipboardManager.copy(string, null)
}

/**
 * Base64转Bitmap图片
 * @param code String
 * @return Bitmap?
 */
fun base64ToBitmap(code: String): Bitmap? {
    val decode: ByteArray = Base64.decode(code.split(",")[1], Base64.DEFAULT)
    return decode.decodeToBitmapOrNull(0, decode.size)
}

/**
 * 返回MaterialDialog Title居中样式
 */
val dialogCentered get() = com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered

/**
 * 设置Preference图标显示
 * @receiver Context 上下文
 * @param resource Any? 传入对象
 * @param result Function2<Drawable?, Boolean, Unit> 输出
 */
fun Preference.setPrefsIconRes(resource: Any?, result: (Drawable?, Boolean) -> Unit) {
    if (context.getBoolean(SettingsPrefs, "hide_function_page_icon", false)) {
        result(null, false)
        return
    }
    val image: Drawable? = when (resource) {
        is Int -> context.resources.getDrawableCompat(resource, null)
        is Drawable -> resource
        is String -> AppUtils(context).getAppIcon(resource)
        else -> null
    }
    if (image == null || image.intrinsicWidth <= 0 || image.intrinsicHeight <= 0) {
        val icon =
            context.resources.getDrawableCompat(android.R.mipmap.sym_def_app_icon, null)
        result(icon, true)
        return
    }

    val bitmap = image.toBitmapOrNull(48.dp, 48.dp, null)
    if (bitmap == null) {
        result(null, false)
        return
    }

    val drawable = RoundedBitmapDrawableFactory.create(context.resources, bitmap)
    drawable.setAntiAlias(true)
    drawable.cornerRadius = 30F
    result(drawable, true)
}

/**
 * 修复Icon显示大小
 * @receiver Preference
 * @param icon Drawable?
 * @return Drawable?
 */
fun Preference.fixIconSize(icon: Drawable?): Drawable? {
    return if (icon != null && ((icon.intrinsicWidth < 48.dp) || (icon.intrinsicHeight < 48.dp))) {
        context.zoomDrawable(icon, 48.dp, 48.dp)
    } else icon
}

/**
 * 格式化Summary添加逗号
 * @param string Array<out String?>
 * @return String
 */
fun arraySummaryDot(vararg string: String?): String {
    return formatStringAuto(string.toList(), ",", false)
}

/**
 * 格式化Summary添加换行
 * @param strings Array<out String?>
 * @return String
 */
fun arraySummaryLine(vararg strings: String?): String {
    return formatStringAuto(strings.toList(), "\n", false)
}

/**
 * 格式化Summary添加冒号
 * @param string Array<out String?>
 * @return String
 */
fun getColonSummary(vararg string: String?): String {
    return formatStringAuto(string.toList(), ": ", false)
}

/**
 * 获取指定长度随机字符串
 * @param length Int 长度
 * @return String
 */
fun getRandomString(length: Int): String {
    val random = Random
    val sb = StringBuffer()
    for (i in 0 until length) {
        val number: Int = random.nextInt(3)
        var result: Long
        when (number) {
            0 -> {
                result = (Math.random() * 25 + 65).roundToLong()
                sb.append(Char(result.toUShort()).toString())
            }

            1 -> {
                result = (Math.random() * 25 + 97).roundToLong()
                sb.append(Char(result.toUShort()).toString())
            }

            2 -> sb.append(java.lang.String.valueOf(Random.nextInt(10)))
        }
    }
    return sb.toString()
}

/**
 * Hex To Byte
 */
fun hexToByte(inHex: String): Byte {
    return inHex.toInt(16).toByte()
}

/**
 * Base64加密
 * @param string String
 * @return String
 */
fun base64Encode(string: String): String {
    return Base64.encodeToString(string.toByteArray(), Base64.DEFAULT)
}

/**
 * Base64解密
 * @param string String
 * @return String
 */
fun base64Decode(string: String): String {
    return String(Base64.decode(string, Base64.DEFAULT))
}

/**
 * 判断语言首选项是否为中文
 * @param context Context
 * @return Boolean
 */
fun isZh(context: Context): Boolean {
    val locale = context.resources.configuration.locales
    val language = locale[0].language
    return language.endsWith("zh")
}

/**
 * 获取Apk绝对路径
 * @param packName String 包名
 * @return ArrayMap<String, String>
 */
fun getPackageAbsolutePath(
    packName: String, ignoreCase: Boolean = false
): ArrayMap<String, String> {
    val map = ArrayMap<String, String>()
    val list = ArrayList<String>()
    Shell.cmd(
        "${CommandUtils.pmlist} -f | ${CommandUtils.grep} $packName" + if (ignoreCase) " -i" else ""
    ).to(list).exec()
    list.forEachIndexed { _, str ->
        val pack = str.replace("package:", "")
        val key = pack.substringAfterLast("=")
        val value = pack.substringBeforeLast("=")
        map[key] = value
    }
    return map
}

/**
 * 获取Apk绝对目录
 * @param packName String
 * @param ignoreCase Boolean
 * @return ArrayMap<String, String>
 */
fun getPackageAbsoluteDir(
    packName: String, ignoreCase: Boolean = false
): ArrayList<String> {
    val list = ArrayList<String>()
    Shell.cmd(
        "${CommandUtils.findapp} -type d -${if (ignoreCase) "iname" else "name"} \"*${packName}*\" -print"
    ).to(list).exec()
    return list
}

/**
 * 根据包名卸载APP
 * @param packName String
 * @param userId String? 0/999
 */
fun uninstallApp(packName: String, userId: String? = "") {
    ShellUtils.fastCmd("${CommandUtils.pmuninstall} $packName ${if (userId.isNullOrEmpty()) "" else "--user $userId"}")
}

/**
 * 强制删除APP
 * @param packName String 包名
 */
fun forceUninstallApp(packName: String) {
    getPackageAbsolutePath(packName).forEach { (k, v) ->
        if (k == packName) ShellUtils.fastCmd(
            "${CommandUtils.chattr} -i -a $v",
            "${CommandUtils.rmrf} $v"
        )
    }
}

/**
 * 卸载模块
 */
suspend fun Context.removeModule() {
    getUsers().forEach { uninstallApp(BuildConfig.APPLICATION_ID, it) }
    getUsers().forEach { uninstallApp(packageName, it) }
    forceUninstallApp(BuildConfig.APPLICATION_ID)
    forceUninstallApp(packageName)
}

/**
 * 退出模块
 * @receiver Context
 */
fun Context.exitModule() {
    (this as MainActivity).finishAndRemoveTask()
    exitProcess(0)
}

/**
 * 绑定RootService反射服务
 * @receiver Context
 * @param serviceClazz Class<*>
 * @param onConnected Function2<ComponentName?, IBinder?, Unit>
 * @param onDisconnected Function1<ComponentName?, Unit>
 */
fun Context.bindRootService(
    serviceClazz: Class<*>,
    onConnected: (ComponentName?, IBinder?) -> Unit,
    onDisconnected: (ComponentName?) -> Unit = {}
) {
    val intent = Intent(this, serviceClazz)
    val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName?, iBinder: IBinder?) {
            onConnected(componentName, iBinder)
        }

        override fun onServiceDisconnected(componentName: ComponentName?) {
            onDisconnected(componentName)
        }
    }
    RootService.bind(intent, serviceConnection)
}

/**
 * 获取刷新率显示状态
 * @return Boolean
 */
fun getRefreshRateStatus(): Boolean = safeOfFalse {
//    Result: Parcel(NULL)
//    Result: Parcel(00000000    '....')
    val command = CommandUtils.getRefreshRateStatus
    val result = ShellUtils.fastCmd(command)
    if (result.isBlank()) return@safeOfFalse false
    return if (result.contains("Operation not permitted", true)) false
    else when (result.filterNumber.toIntOrNull()) {
        0 -> false
        1 -> true
        else -> false
    }
}

/**
 * 设置刷新率显示状态
 * @param status Boolean
 */
fun showRefreshRate(status: Boolean) {
    var command = CommandUtils.showRefreshRate
    command += if (status) "1" else "0"
    ShellUtils.fastCmd(command)
}

/**
 * 判断上下文跳转fragment设置标题
 * @receiver NavController
 * @param fragemntId Int
 * @param title CharSequence?
 */
fun NavController.navigatePage(fragemntId: Int, title: CharSequence?) = try {
    val bundle = Bundle().apply {
        if (!title.isNullOrBlank()) putCharSequence("title_text", title)
    }
    val navOptions = NavOptions.Builder().apply {
        setEnterAnim(R.anim.fragment_enter)
        setExitAnim(R.anim.fragment_exit)
        setPopEnterAnim(R.anim.fragment_enter_pop)
        setPopExitAnim(R.anim.fragment_exit_pop)
    }.build()
    navigate(fragemntId, bundle, navOptions)
} catch (_: IllegalArgumentException) {

}

/**
 * 跳转fragment传递参数 用于功能搜索适配器
 * @receiver NavController
 * @param action Int
 * @param bundle Bundle?
 */
fun NavController.navigatePage(action: Int, bundle: Bundle?) = try {
    val navOptions = NavOptions.Builder().apply {
        setEnterAnim(R.anim.fragment_enter)
        setExitAnim(R.anim.fragment_exit)
        setPopEnterAnim(R.anim.fragment_enter_pop)
        setPopExitAnim(R.anim.fragment_exit_pop)
    }.build()
    navigate(action, bundle, navOptions)
} catch (_: IllegalArgumentException) {

}


/**
 * 获取屏幕状态
 * (true -> 竖屏 ORIENTATION_PORTRAIT)
 * (false -> 横屏 ORIENTATION_LANDSCAPE)
 * @param view View/Context/Resources
 * @param result Function1<Boolean, Unit>
 */

fun getScreenOrientation(view: View, result: (Boolean) -> Unit) {
    getScreenOrientation(view.resources) { result(it) }
}

/**
 * 获取屏幕状态
 * (true -> 竖屏 ORIENTATION_PORTRAIT)
 * (false -> 横屏 ORIENTATION_LANDSCAPE)
 * @param context View/Context/Resources
 * @param result Function1<Boolean, Unit>
 */
fun getScreenOrientation(context: Context, result: (Boolean) -> Unit) {
    getScreenOrientation(context.resources) { result(it) }
}

/**
 * 获取屏幕状态
 * (true -> 竖屏 ORIENTATION_PORTRAIT)
 * (false -> 横屏 ORIENTATION_LANDSCAPE)
 * @param resource View/Context/Resources
 * @param result Function1<Boolean, Unit>
 */
fun getScreenOrientation(resource: Resources, result: (Boolean) -> Unit) {
    val mConfiguration: Configuration = resource.configuration
    if (mConfiguration.orientation == Configuration.ORIENTATION_PORTRAIT) {
        result(true)
    }
    if (mConfiguration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        result(false)
    }
}

/**
 * 获取设备用户
 * @return Array<String>
 */
suspend fun getUsers(): Array<String> {
    val command = CommandUtils.getUsers
    return withDefault {
        ShellUtils.fastCmd(command).let {
            if (it.isNotBlank()) {
                it.replaceSpace.split(",").toTypedArray()
            } else arrayOf()
        }
    }
}

/**
 * 获取文本颜色(ColorSpan)
 * @param char CharSequence
 * @return Int? 返回Span数组中下标为0的前景色
 */
fun getCharColor(char: CharSequence): Int? {
    val sp = SpannableString(char)
    val colorSpan = sp.getSpans(0, sp.length, classOf<ForegroundColorSpan>())
    return if (colorSpan != null && colorSpan.isNotEmpty()) colorSpan[0].foregroundColor else null
}

/**
 * 获取文本Span数组
 * @param char CharSequence
 * @return Array<out ForegroundColorSpan>?
 */
fun getCharSpans(char: CharSequence): Array<out ForegroundColorSpan>? {
    val colorSpans = SpannableString(char).getSpans(0, char.length, classOf<ForegroundColorSpan>())
    return if (colorSpans == null || colorSpans.isEmpty()) null else colorSpans
}

/**
 * 缩放Drawable
 * @receiver Context
 * @param drawable Drawable
 * @param width Int
 * @param height Int
 * @return Drawable
 */
fun Context.zoomDrawable(drawable: Drawable, width: Int, height: Int): Drawable {
    val oldBmp = drawable.toBitmap()
    val newBmp = oldBmp.scale(width, height)
    return newBmp.toDrawable(resources)
}

fun Context.verityPackage() = safeOf({ exitModule() }) {
    val packInfo =
        PackageUtils(packageManager).getPackageInfo(BuildConfig.APPLICATION_ID, 0) ?: return@safeOf
    if (packInfo.packageName != packageName || packInfo.versionName != getVersionName || packInfo.longVersionCode != getVersionCode.toLong()) {
        exitModule()
    }
}

/**
 * 判断是否MTK机型
 */
val isMTK get() = Pattern.compile("mt[0-9]*").matcher(Build.HARDWARE).find()

/**
 * 获取系统当前小时格式
 * @receiver Context
 * @return Boolean
 */
val Context.is24
    get() = Settings.System.getString(
        contentResolver, Settings.System.TIME_12_24
    ) == "24"

/**
 * 调用锁屏事件 (反射)
 * @param context Context
 */
fun closeScreen(context: Context) {
    val service = context.getSystemService(classOf<PowerManager>())
    service.asResolver().firstMethod {
        name = "goToSleep"
        parameters(Long::class)
    }.invoke(SystemClock.uptimeMillis())
}

/**
 * Fragment快捷设置MenuProvider
 * @receiver Fragment
 * @param menuId Int Menu Resource ID
 * @param onMenuSelected Function1<MenuItem, Boolean>
 */
fun Fragment.setupMenuProvider(@MenuRes menuId: Int, onMenuSelected: (MenuItem) -> Boolean) =
    (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) =
            menuInflater.inflate(menuId, menu)

        override fun onMenuItemSelected(menuItem: MenuItem) = onMenuSelected(menuItem)
    }, viewLifecycleOwner, Lifecycle.State.RESUMED)

/**
 * Fragment快捷设置MenuProvider
 * @receiver Fragment
 * @param menuProvider MenuProvider
 */
fun Fragment.setupMenuProvider(menuProvider: MenuProvider) =
    (requireActivity() as MenuHost).addMenuProvider(
        menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED
    )


fun logcatToFile(file: File): Boolean {
    return try {
        if (!file.exists()) file.createNewFile()
        ShellUtils.fastCmdResult("${CommandUtils.logcat} -d -f ${file.absolutePath}")
    } catch (e: Exception) {
        LogUtils.e("logcatToFile", "logcat", "$e", true)
        false
    }
}

/**
 * 旧 dataChannel 推送的等价物：键值写入 ModulePrefs（与宿主侧 Channel.wait 同一数据源）。
 * @param packName 原 dataChannel 目标包，现仅作兼容保留（不再按包路由）
 */
fun Context.sendPrefsValue(packName: String, key: String, newValue: Any) {
    dataChannel(packName).put(key, newValue)
}

/**
 * 旧 dataChannel put(key)（无值推送）没有 remote prefs 对应物；
 * 键值本身由调用方的 putXxx(ModulePrefs, ...) 已写入，本函数保留为兼容空操作。
 */
fun Context.sendPrefsKey(packName: String, key: String) {
    dataChannel(packName).put(key)
}

/**
 * 逆转字符串数组
 * @param str
 * @return
 */
fun stringReverse(str: String): String {
    return str.toList().reversed().toString().replaceSpace.replace(",", "").replace("[", "")
        .replace("]", "")
}

/**
 * 创建并显示BottomSheetDialog
 * @receiver Context
 * @param rootView View?
 * @return BottomSheetDialog
 */
fun Context.showBottomSheet(rootView: View? = null): BottomSheetDialog {
    return BottomSheetDialog(this).apply {
        if (rootView != null) setContentView(rootView)
        show()
    }
}

/**
 * 打开链接
 * @receiver Context
 * @param url String
 */
fun Context.openUrl(url: String) {
    startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
}

/**
 * JSON数组转字符串数组
 * @receiver JSONArray
 * @return ArrayList<String>
 */
fun JSONArray.toStringList(): ArrayList<String> {
    val list = ArrayList<String>()
    if (length() <= 0) return list
    for (i in 0 until length()) {
        val str = optString(i)
        if (str.isNotBlank()) list.add(str)
    }
    return list
}

/**
 * 数组字符串转数组
 * @receiver String
 * @return ArrayList<String>
 */
fun String.convertList(): ArrayList<String> {
    val list = ArrayList<String>()
    if (startsWith("[") && endsWith("]")) {
        val tmp = replaceSpace.removePrefix("[").removeSuffix("]").split(",")
        list.addAll(tmp)
    }
    return list
}

fun setSummaryProvider(preference: Preference) {
    when (preference) {
        is EditTextPreference -> preference.setSummaryProvider {
            EditTextPreference.SimpleSummaryProvider.getInstance().provideSummary(preference)
        }

        is ListPreference -> preference.setSummaryProvider {
            ListPreference.SimpleSummaryProvider.getInstance().provideSummary(preference)
        }
    }
}

fun getManifestEndVersion(string: String?): String {
    if (string.isNullOrBlank()) return ""
    string.split(".").apply {
        return if (size < 2) ""
        else "${this[lastIndex - 1]} ${this[lastIndex]}"
    }
}

/**
 * 启动后台运行
 * @param intent Intent
 */
fun startMirageWindow(intent: Intent?): Int {
    val makeBasic = OplusMirageOptions.makeBackgroundStreamModeOptions()
    return OplusMirageWindowManager.getInstance().startMirageWindowMode(
        intent, makeBasic.toBundle()
    )
}

fun createTextDrawable(context: Context, text: String): Drawable {
    // 创建一个 Paint 对象来设置文本的样式
    val paint = Paint().apply {
        color = Color.WHITE // 文本颜色为白色
        textSize = 14F.dp
        isAntiAlias = true
        textAlign = Paint.Align.LEFT
    }

    // 使用 TextPaint 测量文本的宽度和高度
    val textPaint = TextPaint(paint)

    // 测量文本的宽度
    val textWidth = textPaint.measureText(text)

    // 测量文本的高度
    val fontMetrics = paint.fontMetrics
    val textHeight = fontMetrics.bottom - fontMetrics.top

    // 创建一个足够大的 Bitmap 来容纳文本
    val bitmapWidth = (textWidth + 20f).toInt()  // 增加一些边距
    val bitmapHeight = (textHeight + 20f).toInt()  // 增加一些边距

    val bitmap = createBitmap(bitmapWidth, bitmapHeight)
    val canvas = Canvas(bitmap)

    // 设置背景为透明
    val backgroundPaint = Paint().apply {
        color = Color.TRANSPARENT
        isAntiAlias = true
    }
    canvas.drawRect(0f, 0f, bitmapWidth.toFloat(), bitmapHeight.toFloat(), backgroundPaint)

    val x = (bitmapWidth - textWidth) / 2f  // 水平居中
    val y = bitmapHeight / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2f  // 垂直居中

    // 在 Canvas 上绘制文本
    canvas.drawText(text, x, y, paint)

    // 返回 BitmapDrawable，背景是透明的
    return bitmap.toDrawable(context.resources)
}

fun PackageInfo.isSystem(): Boolean {
    val appInfo = applicationInfo ?: return false
    return appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 1
}