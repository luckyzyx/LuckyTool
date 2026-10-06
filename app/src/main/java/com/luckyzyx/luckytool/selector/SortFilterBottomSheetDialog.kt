package com.luckyzyx.luckytool.selector

import android.content.Context
import android.widget.CompoundButton
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.highcapable.betterandroid.ui.extension.view.layoutInflater
import com.luckyzyx.luckytool.databinding.DialogSortFilterSelectorLayoutBinding
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
class SortFilterBottomSheetDialog(context: Context) : BottomSheetDialog(context) {

    private var binding =
        DialogSortFilterSelectorLayoutBinding.inflate(context.layoutInflater)

    init {
        setContentView(binding.root)

        binding.sortReverse.isVisible = false
        binding.sortLayout.isVisible = false
        binding.filterLayout.isVisible = false
    }

    fun setReverse(enable: Boolean, listener: CompoundButton.OnCheckedChangeListener?) {
        binding.sortReverse.apply {
            isVisible = enable
            setOnCheckedChangeListener(listener)
        }
    }

    fun setSortChips(
        enable: Boolean, chips: Array<String>?, checked: Int? = null,
        listener: ChipGroup.OnCheckedStateChangeListener?
    ) {
        binding.sortLayout.isVisible = enable
        binding.sortChips.apply {
            isSingleSelection = true
            chips?.forEachIndexed { index, title ->
                val chip = Chip(context).apply {
                    id = index
                    text = title
                    isCheckable = true
                    isClickable = true
                    isChecked = index == 0
                }
                addView(chip)
            }
            checked?.let { check(it) }
            setOnCheckedStateChangeListener(listener)
        }
    }

    fun setFilterChips(enable: Boolean, chips: Array<Chip>?) {
        binding.filterLayout.isVisible = enable
        binding.filterChips.apply {
            isSingleSelection = false
            chips?.forEachIndexed { _, chip ->
                addView(chip)
            }
        }
    }
}