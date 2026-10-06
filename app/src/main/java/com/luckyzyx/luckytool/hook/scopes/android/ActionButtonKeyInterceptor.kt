package com.luckyzyx.luckytool.hook.scopes.android

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.media.IAudioService
import android.os.Handler
import android.os.Looper
import android.os.ServiceManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.oplus.os.ILinearmotorVibratorService
import com.oplus.os.LinearmotorVibrator
import com.oplus.os.WaveformEffect
import org.lsposed.lsparanoid.Obfuscate

/**
 * 快捷键（Action Button Key）拦截
 *
 * 总功能包含两个独立选项：
 *
 * 选项1 - 无操作接管（开关 action_button_nothing_enable）：
 *         开关开启且系统快捷键功能为"无操作"（oplus_action_button_switch_state == "nothing"）时，
 *         拦截 780 物理按键事件，自行判定并记录 单击 / 双击 / 长按 / 超长按 四种操作
 *
 * 选项2 - 自定义响铃切换（开关 action_button_ring_cycle_enable）：
 *         开关开启且系统快捷键功能为"响铃/振动/静音"（ring_mode）时，
 *         拦截原生注入的 782 长按事件，替换原生三态循环（静音→响铃→振动）为自定义循环
 *         （响铃↔振动 / 响铃↔静音，由 action_button_ring_cycle_mode 决定；
 *         三态 ring_vibrate_silent 与官方循环一致，放行原生链路不替换）
 *
 * 原生判定：按下 <495ms = 单击（注入 781），>=495ms = 长按（注入 782），无双击与超长按逻辑
 *
 * 选项2 作用域选择 framework 而非 gesture：
 *  - 原生切换由 gesture 模块 g0（ActionKeyStartApp）收到 782 后执行 setRingerModeInternal，
 *    该接口带 oplusAuthCheck 校验，gesture 侧替换需绕过认证或复刻其 Messenger 通知逻辑
 *  - framework 拦截 782 后 gesture 收不到长按事件，原生切换链路天然被替换，时机与原生一致（495ms），息屏可用
 *  - 切换走 AudioManager internal 路径（反射 hidden API，system_server 进程豁免限制）：
 *    切静音不联动免打扰；若走公开 setRingerMode（external 路径），ZenModeHelper 会在
 *    切静音时强制开启 DND（ZEN_MODE_IMPORTANT_INTERRUPTIONS）
 *
 * 扩展方式：新增自定义 mode 时在 takeOverType 的 when 中扩展分支并返回新的接管类型，
 * 再在对应 hook 点按接管类型执行处理逻辑
 */
@Obfuscate
object ActionButtonKeyInterceptor : YukiBaseHooker() {

    private const val TAG = "LuckyTool_ActionButtonKey"

    private const val KEYCODE_ACTION_BUTTON = 780
    private const val KEYCODE_ACTION_BUTTON_LONG_PRESS = 782
    private const val ACTION_SWITCH_NOTHING = "nothing"
    private const val ACTION_SWITCH_RING_MODE = "ring_mode"
    private const val LONG_PRESS_TIMEOUT = 495L
    private const val DOUBLE_TAP_TIMEOUT = 400L
    private const val SUPER_LONG_PRESS_TIMEOUT = 3000L
    private const val SUPER_LONG_PRESS_LEFT = 2505L  //3000 - 495，长按注入后再等 2505ms 即超长按

    //选项2 自定义铃声循环模式（action_button_ring_cycle_mode）
    private const val RING_CYCLE_RING_VIBRATE = "ring_vibrate"  //响铃 <-> 振动
    private const val RING_CYCLE_RING_SILENT = "ring_silent"    //响铃 <-> 静音
    private const val RING_CYCLE_ALL = "ring_vibrate_silent"    //响铃 -> 振动 -> 静音

    //接管类型（takeOverType 分发结果）
    private const val TAKE_OVER_NONE = 0
    private const val TAKE_OVER_NOTHING = 1      //选项1：无操作接管
    private const val TAKE_OVER_RING_MODE = 2    //选项2：自定义响铃切换

    private val mHandler = Handler(Looper.getMainLooper())

    //以下状态仅在 mHandler 线程访问

    //选项1：无操作接管（四类事件判定）
    private var mLastDownTime = 0L
    private var mLastTapUpTime = 0L
    private var mSingleTapPending = false
    private var mPendingDoubleTap = false
    private var mLongPressFired = false
    private var mSuperLongPressFired = false

    //四类操作动作封装（各自维护状态并记录日志，后续可替换为实际动作）
    private val mSingleTapRunnable = Runnable {
        mSingleTapPending = false
        Log.d(TAG, "快捷键单击 singleTap")
    }

    private val mDoubleTapRunnable = Runnable {
        mPendingDoubleTap = false
        mLastTapUpTime = 0L
        Log.d(TAG, "快捷键双击 doubleTap")
    }

    private val mLongPressRunnable = Runnable {
        mLongPressFired = true
        Log.d(TAG, "快捷键长按 longPress")
    }

    private val mSuperLongPressRunnable = Runnable {
        mSuperLongPressFired = true
        Log.d(TAG, "快捷键超长按 superLongPress")
    }

    //选项2：自定义响铃切换（超长按计时，每次按键创建新实例以捕获 context）
    private var mRingSuperLongPressRunnable: Runnable? = null

    private var audioService: IAudioService? = null
    private var linearmotorVibratorService: ILinearmotorVibratorService? = null

    //配置：初始化读一次快照，运行时由 dataChannel 实时同步（模块 UI 修改后无需重启作用域）
    private var mNothingEnable = false
    private var mRingCycleEnable = false
    private var mRingCycleMode = RING_CYCLE_ALL

    override fun onHook() {
        mNothingEnable = preferences(ModulePrefs).getBoolean("action_button_nothing_enable", false)
        mRingCycleEnable =
            preferences(ModulePrefs).getBoolean("action_button_ring_cycle_enable", false)
        mRingCycleMode =
            preferences(ModulePrefs).getString("action_button_ring_cycle_mode", RING_CYCLE_ALL)

        dataChannel.wait<Boolean>("action_button_nothing_enable") { mNothingEnable = it }
        dataChannel.wait<Boolean>("action_button_ring_cycle_enable") { mRingCycleEnable = it }
        dataChannel.wait<String>("action_button_ring_cycle_mode") { mRingCycleMode = it }

        //Source StrategyActionButtonKeyLaunchApp
        "com.android.server.policy.StrategyActionButtonKeyLaunchApp".toClass().resolve().apply {
            //选项1：无操作接管，拦截 780 物理按键事件
            firstMethod { name = "actionInterceptKeyBeforeQueueing" }.intercept {
                //取参失败一律放行官方代码（返回 0 会吞掉按键事件，导致按键失灵）
                val instance = firstMethod { name = "getInstance" }.invoke()
                    ?: return@intercept proceed()
                val context = firstField { type = Context::class; superclass() }.of(instance)
                    .get<Context>() ?: return@intercept proceed()
                val event = arg<KeyEvent>(0) ?: return@intercept proceed()
                val keyCode = arg<Int>(2) ?: return@intercept proceed()
                val down = arg<Boolean>(3) ?: return@intercept proceed()
                if (keyCode != KEYCODE_ACTION_BUTTON || event.repeatCount != 0) {
                    return@intercept proceed()
                }

                //接管条件：无操作接管开关开启（分发见 takeOverType）
                if (takeOverType(context) != TAKE_OVER_NOTHING) return@intercept proceed()

                //接管：判定与日志统一在 handler 线程执行，拦截并返回 0（原方法同样吞掉 780）
                mHandler.post { handleNothingEvent(down, SystemClock.uptimeMillis()) }
                0
            }

            //选项2：自定义响铃切换，拦截原生注入的 782 长按事件（静态方法）
            firstMethod { name = "injectActionButtonPressKeyEvent" }.intercept {
                val instance = firstMethod { name = "getInstance" }.invoke()
                    ?: return@intercept proceed()
                val context = firstField { type = Context::class; superclass() }.of(instance)
                    .get<Context>() ?: return@intercept proceed()
                val event = firstArg<KeyEvent>() ?: return@intercept proceed()
                if (event.keyCode != KEYCODE_ACTION_BUTTON_LONG_PRESS) return@intercept proceed()

                //接管条件：自定义响铃切换开关开启（分发见 takeOverType）
                if (takeOverType(context) != TAKE_OVER_RING_MODE) return@intercept proceed()

                initRingInjectService()
                //拦截 782：不调用 proceed 即跳过原生注入，动作统一在 handler 线程执行
                mHandler.post { handleRingInject(event.action, context) }
                null
            }
        }
    }

    /**
     * 读取当前系统快捷键功能，when 分发返回接管类型
     */
    private fun takeOverType(context: Context): Int {
        val switchState = try {
            Settings.System.getString(context.contentResolver, "oplus_action_button_switch_state")
        } catch (_: Throwable) {
            null
        }
        return when (switchState) {
            ACTION_SWITCH_NOTHING -> if (mNothingEnable) TAKE_OVER_NOTHING else TAKE_OVER_NONE
            //三态循环（ring_vibrate_silent）与官方行为一致，无需替换，放行原生链路
            ACTION_SWITCH_RING_MODE ->
                if (mRingCycleEnable && mRingCycleMode != RING_CYCLE_ALL) TAKE_OVER_RING_MODE
                else TAKE_OVER_NONE

            else -> TAKE_OVER_NONE
        }
    }

    //选项1：无操作接管，四类事件判定与日志
    @SuppressLint("ReplaceWithCoroutinesExtension")
    private fun handleNothingEvent(down: Boolean, now: Long) {
        if (down) {
            //处于双击等待窗口内的第二次按下
            if (mSingleTapPending) {
                mHandler.removeCallbacks(mSingleTapRunnable)
                mSingleTapPending = false
                mPendingDoubleTap = true
            }
            mLastDownTime = now
            mLongPressFired = false
            mSuperLongPressFired = false
            mHandler.postDelayed(mLongPressRunnable, LONG_PRESS_TIMEOUT)
            mHandler.postDelayed(mSuperLongPressRunnable, SUPER_LONG_PRESS_TIMEOUT)
            return
        }
        val interval = now - mLastDownTime
        if (interval < LONG_PRESS_TIMEOUT) {
            mHandler.removeCallbacks(mLongPressRunnable)
            mHandler.removeCallbacks(mSuperLongPressRunnable)
            if (mPendingDoubleTap && now - mLastTapUpTime < DOUBLE_TAP_TIMEOUT) {
                mDoubleTapRunnable.run()
            } else {
                mPendingDoubleTap = false
                mLastTapUpTime = now
                mSingleTapPending = true
                mHandler.postDelayed(mSingleTapRunnable, DOUBLE_TAP_TIMEOUT)
            }
        } else {
            mHandler.removeCallbacks(mSuperLongPressRunnable)
            if (!mLongPressFired) {
                //兜底：长按定时器未触发（理论上不会发生）
                mLongPressRunnable.run()
            }
            if (interval >= SUPER_LONG_PRESS_TIMEOUT && !mSuperLongPressFired) {
                //兜底：超长按定时器未触发
                mSuperLongPressRunnable.run()
            }
            resetTapState()
            Log.d(TAG, "按键时长 duration=${interval}ms")
        }
    }

    //选项2：自定义响铃切换，拦截 782 长按注入
    @SuppressLint("ReplaceWithCoroutinesExtension")
    private fun handleRingInject(action: Int, context: Context) {
        when (action) {
            KeyEvent.ACTION_DOWN -> {
                //长按成立点（物理按下约 495ms）：执行自定义切换
                switchRingMode(context)
                //继续按住不松到物理 3000ms 则视为超长按，再切换一次
                mRingSuperLongPressRunnable?.let { mHandler.removeCallbacks(it) }
                val pending = Runnable { switchRingMode(context) }
                mRingSuperLongPressRunnable = pending
                mHandler.postDelayed(pending, SUPER_LONG_PRESS_LEFT)
            }

            KeyEvent.ACTION_UP -> {
                //已松开，取消超长按计时
                mRingSuperLongPressRunnable?.let { mHandler.removeCallbacks(it) }
                mRingSuperLongPressRunnable = null
            }
        }
    }

    private fun initRingInjectService() {
        try {
            if (audioService == null) {
                val binder = ServiceManager.getService("audio")
                audioService = IAudioService.Stub.asInterface(binder)
            }
            if (linearmotorVibratorService == null) {
                val binder = ServiceManager.getService("linearmotor")
                linearmotorVibratorService = ILinearmotorVibratorService.Stub.asInterface(binder)
            }
        } catch (e: Throwable) {
            Log.d(TAG, "initRingInjectService 获取异常", e)
        }
    }

    private fun switchRingMode(context: Context) {
        val cycle = when (mRingCycleMode) {
            RING_CYCLE_RING_VIBRATE -> intArrayOf(
                AudioManager.RINGER_MODE_NORMAL, AudioManager.RINGER_MODE_VIBRATE
            )

            RING_CYCLE_RING_SILENT -> intArrayOf(
                AudioManager.RINGER_MODE_NORMAL, AudioManager.RINGER_MODE_SILENT
            )

            else -> intArrayOf(
                AudioManager.RINGER_MODE_NORMAL,
                AudioManager.RINGER_MODE_VIBRATE,
                AudioManager.RINGER_MODE_SILENT
            )
        }
        val current = audioService?.ringerModeInternal
        if (current == null) {
            Log.d(TAG, "铃声模式获取失败")
            return
        }
        val next = cycle.indexOf(current).let { index ->
            if (index < 0) cycle[0] else cycle[(index + 1) % cycle.size]
        }
        try {
            audioService?.setRingerModeInternal(next, "com.android.systemui")
            vibrateFeedback(context, next)
        } catch (e: Throwable) {
            Log.e(TAG, "铃声模式切换失败", e)
        }
    }

    /**
     * 振动反馈，对齐官方语义（OplusRMChangeVibrateHelper.vibrateOnRMChanged）：
     * 切到响铃 -> 308（EFFECT_ALERTSLIDER_DOWN）、切到振动 -> 430（EFFECT_ID_THREE_STAGE_KEY）、
     * 切到静音 -> 不振动
     * 优先走 LinearmotorVibrator（getSystemService("linearmotor")，与 SystemUI OplusVibrateHelper 同源），
     * 失败退回普通 Vibrator 时长振幅近似
     */
    private fun vibrateFeedback(context: Context, ringerMode: Int) {
        val effectId = when (ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> 308
            AudioManager.RINGER_MODE_VIBRATE -> 430
            else -> return  //RINGER_MODE_SILENT：官方不振动
        }
        try {
            val linearmotorVibrator = LinearmotorVibrator(context, linearmotorVibratorService)
            val effect = WaveformEffect.Builder().apply {
                setEffectType(effectId)
                setAsynchronous(true)
            }.build()
            linearmotorVibrator.vibrate(effect)
            Log.d(TAG, "线性马达振动 effectId=$effectId")
        } catch (e: Throwable) {
            Log.d(TAG, "线性马达振动失败", e)
        }
    }

    private fun resetTapState() {
        mHandler.removeCallbacks(mSingleTapRunnable)
        mSingleTapPending = false
        mPendingDoubleTap = false
        mLastTapUpTime = 0L
    }
}
