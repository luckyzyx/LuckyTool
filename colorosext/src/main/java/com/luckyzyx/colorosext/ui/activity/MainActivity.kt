package com.luckyzyx.colorosext.ui.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Process
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.BuildCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.joom.paranoid.Obfuscate
import com.luckyzyx.colorosext.R
import com.luckyzyx.colorosext.databinding.ActivityMainBinding
import com.luckyzyx.colorosext.utils.*
import kotlin.system.exitProcess

@Obfuscate
@Suppress("PrivatePropertyName")
class MainActivity : AppCompatActivity() {
    private val KEY_PREFIX = MainActivity::class.java.name + '.'
    private val EXTRA_SAVED_INSTANCE_STATE = KEY_PREFIX + "SAVED_INSTANCE_STATE"

    private lateinit var binding: ActivityMainBinding

    private fun newIntent(context: Context): Intent {
        return Intent(context, MainActivity::class.java)
    }

    private fun newIntent(savedInstanceState: Bundle, context: Context): Intent {
        return newIntent(context).putExtra(EXTRA_SAVED_INSTANCE_STATE, savedInstanceState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        checkTheme(this)
        setContentView(binding.root)

        checkPrefsRW()
        initNavigationFragment()
    }

    private fun initNavigationFragment(){
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_container) as NavHostFragment
        val navController: NavController = navHostFragment.navController
        val bottomNavigationView = binding.navView
        bottomNavigationView.setupWithNavController(navController)
        bottomNavigationView.labelVisibilityMode = BottomNavigationView.LABEL_VISIBILITY_SELECTED

    }

    private fun checkTheme(context: Context) {
        if (ThemeUtil(context).isDynamicColor()){
            DynamicColors.applyToActivityIfAvailable(this)
        }
        when(SPUtils.getString(this, SettingsPrefs,"dark_theme","MODE_NIGHT_FOLLOW_SYSTEM")){
            "MODE_NIGHT_FOLLOW_SYSTEM" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            "MODE_NIGHT_NO" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "MODE_NIGHT_YES" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("WorldReadableFiles")
    private fun checkPrefsRW() {
        try {
            getSharedPreferences(SettingsPrefs, MODE_WORLD_READABLE)
            getSharedPreferences(XposedPrefs, MODE_WORLD_READABLE)
            getSharedPreferences(MagiskPrefs, MODE_WORLD_READABLE)
        } catch (ignored: SecurityException) {
            MaterialAlertDialogBuilder(this)
                .setCancelable(false)
                .setMessage(getString(R.string.unsupported_xposed))
                .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int -> exitProcess(0) } //.setNegativeButton(R.string.ignore, null)
                .show()
        }
    }

    private val isParasitic get() = !Process.isApplicationUid(Process.myUid())

    @Suppress("DEPRECATION")
    fun restart() {
        if (BuildCompat.isAtLeastS() || isParasitic) {
            recreate()
        } else {
            try {
                val savedInstanceState = Bundle()
                onSaveInstanceState(savedInstanceState)
                finish()
                startActivity(newIntent(savedInstanceState, this))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            } catch (e: Throwable) {
                recreate()
            }
        }
    }
}