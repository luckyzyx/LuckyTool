package com.luckyzyx.luckytool.ui.activity

import android.content.Intent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.luckyzyx.luckytool.ui.application.ActivityLifecycleManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuProvider
import androidx.core.view.children
import androidx.lifecycle.Lifecycle
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.canhub.cropper.CropImageView.CropResult
import com.canhub.cropper.CropImageView.OnCropImageCompleteListener
import com.canhub.cropper.CropImageView.OnSetImageUriCompleteListener
import com.canhub.cropper.R
import com.canhub.cropper.parcelable
import com.luckyzyx.luckytool.R as LuckyToolR
import com.luckyzyx.luckytool.utils.ThemeUtils
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
class CropImageActivity : AppCompatActivity(), MenuProvider,
    OnSetImageUriCompleteListener, OnCropImageCompleteListener {

    private var cropImageView: CropImageView? = null

    private var cropImageUri: Uri? = null
    private lateinit var cropImageOptions: CropImageOptions

    private lateinit var key: String

    private val pickImageGallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            onPickImageResult(uri)
        }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ThemeUtils.initTheme(this)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        setContentView(LuckyToolR.layout.activity_crop_image)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }
        ActivityLifecycleManager.registerActivity(this)

        addMenuProvider(this, this, Lifecycle.State.RESUMED)

        val toolbar = findViewById<Toolbar>(LuckyToolR.id.toolbar)
        setSupportActionBar(toolbar)

        cropImageView = findViewById(LuckyToolR.id.cropImageView)

        val bundle = intent.getBundleExtra(CropImage.CROP_IMAGE_EXTRA_BUNDLE)
        key = bundle?.getString("key") ?: ""
        cropImageUri = bundle?.parcelable(CropImage.CROP_IMAGE_EXTRA_SOURCE)
        cropImageOptions =
            bundle?.parcelable(CropImage.CROP_IMAGE_EXTRA_OPTIONS) ?: CropImageOptions()

        supportActionBar?.apply {
            title = cropImageOptions.activityTitle.ifEmpty { "" }
            setDisplayHomeAsUpEnabled(true)
        }
        toolbar.setNavigationOnClickListener {
            setResultCancel()
            onBackPressedDispatcher.onBackPressed()
        }

        if (cropImageUri == null || cropImageUri == Uri.EMPTY) {
            pickImageGallery.launch("image/*")
        } else {
            cropImageView?.setImageUriAsync(cropImageUri)
        }
    }

    public override fun onStart() {
        super.onStart()
        cropImageView?.setOnSetImageUriCompleteListener(this)
        cropImageView?.setOnCropImageCompleteListener(this)
    }

    public override fun onStop() {
        super.onStop()
        cropImageView?.setOnSetImageUriCompleteListener(null)
        cropImageView?.setOnCropImageCompleteListener(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityLifecycleManager.unregisterActivity(this)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        if (cropImageOptions.skipEditing) return
        menuInflater.inflate(R.menu.crop_image_menu, menu)

        if (!cropImageOptions.allowRotation) {
            menu.removeItem(R.id.ic_rotate_left_24)
            menu.removeItem(R.id.ic_rotate_right_24)
        } else if (cropImageOptions.allowCounterRotation) {
            menu.findItem(R.id.ic_rotate_left_24).isVisible = true
        }

        if (!cropImageOptions.allowFlipping) menu.removeItem(R.id.ic_flip_24)

        menu.children.forEachIndexed { _, menuItem ->
            if (ThemeUtils.isNightMode(resources.configuration)) {
                menuItem.iconTintList = ColorStateList.valueOf(Color.WHITE)
            }
        }
    }

    override fun onMenuItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.crop_image_menu_crop -> {
            cropImage()
            true
        }

        R.id.ic_rotate_left_24 -> {
            rotateImage(-cropImageOptions.rotationDegrees)
            true
        }

        R.id.ic_rotate_right_24 -> {
            rotateImage(cropImageOptions.rotationDegrees)
            true
        }

        R.id.ic_flip_24_horizontally -> {
            cropImageView?.flipImageHorizontally()
            true
        }

        R.id.ic_flip_24_vertically -> {
            cropImageView?.flipImageVertically()
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    override fun onSetImageUriComplete(view: CropImageView, uri: Uri, error: Exception?) {
        if (error == null) {
            if (cropImageOptions.initialCropWindowRectangle != null) {
                cropImageView?.cropRect = cropImageOptions.initialCropWindowRectangle
            }

            if (cropImageOptions.initialRotation > 0) {
                cropImageView?.rotatedDegrees = cropImageOptions.initialRotation
            }

            if (cropImageOptions.skipEditing) cropImage()
        } else {
            setResult(null, error, 1)
        }
    }

    override fun onCropImageComplete(view: CropImageView, result: CropResult) {
        setResult(result.uriContent, result.error, result.sampleSize)
    }

    private fun onPickImageResult(resultUri: Uri?) {
        when (resultUri) {
            null -> setResultCancel()
            else -> {
                cropImageUri = resultUri
                cropImageView?.setImageUriAsync(cropImageUri)
            }
        }
    }

    /**
     * Execute crop image and save the result tou output uri.
     */
    private fun cropImage() {
        if (cropImageOptions.noOutputImage) {
            setResult(null, null, 1)
        } else {
            cropImageView?.croppedImageAsync(
                saveCompressFormat = cropImageOptions.outputCompressFormat,
                saveCompressQuality = cropImageOptions.outputCompressQuality,
                reqWidth = cropImageOptions.outputRequestWidth,
                reqHeight = cropImageOptions.outputRequestHeight,
                options = cropImageOptions.outputRequestSizeOptions,
                customOutputUri = cropImageOptions.customOutputUri,
            )
        }
    }

    /**
     * Rotate the image in the crop image view.
     */
    private fun rotateImage(degrees: Int) {
        cropImageView?.rotateImage(degrees)
    }

    /**
     * Result with cropped image data or error if failed.
     */
    private fun setResult(uri: Uri?, error: Exception?, sampleSize: Int) {
        setResult(
            error?.let { CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE } ?: RESULT_OK,
            getResultIntent(uri, error, sampleSize),
        )
        finish()
    }

    /**
     * Cancel of cropping activity.
     */
    private fun setResultCancel() {
        setResult(RESULT_CANCELED)
        finish()
    }

    /**
     * Get intent instance to be used for the result of this activity.
     */
    private fun getResultIntent(uri: Uri?, error: Exception?, sampleSize: Int): Intent {
        val result = CropImage.ActivityResult(
            originalUri = cropImageView?.imageUri,
            uriContent = uri,
            error = error,
            cropPoints = cropImageView?.cropPoints,
            cropRect = cropImageView?.cropRect,
            rotation = cropImageView?.rotatedDegrees ?: 0,
            wholeImageRect = cropImageView?.wholeImageRect,
            sampleSize = sampleSize,
        )
        val intent = Intent()
        intent.extras?.let(intent::putExtras)
        intent.putExtra("key", key)
        intent.putExtra(CropImage.CROP_IMAGE_EXTRA_RESULT, result)
        return intent
    }
}
