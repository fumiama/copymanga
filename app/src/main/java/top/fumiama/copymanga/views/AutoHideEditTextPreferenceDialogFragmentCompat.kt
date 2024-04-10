package top.fumiama.copymanga.views

import android.animation.ObjectAnimator
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import androidx.annotation.Keep
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreferenceDialogFragmentCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AutoHideEditTextPreferenceDialogFragmentCompat(private val settingsFragmentView: View): EditTextPreferenceDialogFragmentCompat() {
    var exit = false
    override fun show(manager: FragmentManager, tag: String?) {
        super.show(manager, tag)
        lifecycleScope.launch { goUp() }
    }
    override fun onDestroy() {
        super.onDestroy()
        exit = true
    }
    private suspend fun goUp() = withContext(Dispatchers.IO) {
        var round = 0
        while(!exit) {
            var diff = round
            while (!exit && ((round == 0 && diff == 0) || (round > 0 && diff != 0))) {
                delay(200)
                if (dialog == null) continue
                // https://github.com/mikepenz/MaterialDrawer/blob/aa9136fb4f5b3a80460fe5f47213985026d20c88/library/src/main/java/com/mikepenz/materialdrawer/util/KeyboardUtil.java
                val r = Rect()
                //r will be populated with the coordinates of your view that area still visible.
                settingsFragmentView.getWindowVisibleDisplayFrame(r)
                //get screen height and calculate the difference with the usable area from the r
                val height = settingsFragmentView.context.resources.displayMetrics.heightPixels
                diff = height - r.bottom
                Log.d("MySF", "diff: $diff")
            }
            Log.d("MySF", "diff out while: $diff")
            if (diff <= 0 && round == 0) return@withContext
            Log.d("MySF", "f.dialog is $dialog")
            withContext(Dispatchers.Main) {
                dialog?.window?.apply {
                    val attr = attributes
                    if (diff != 0) {
                        Log.d("MySF", "animate from ${attr.y} to ${attr.y-diff/2}")
                        ObjectAnimator.ofInt(WindowAttributeSetter(this), "y", attr.y, attr.y-diff/2).setDuration(233).start()
                    } else {
                        Log.d("MySF", "animate from ${attr.y} to 0")
                        ObjectAnimator.ofInt(WindowAttributeSetter(this), "y", attr.y, 0).setDuration(233).start()
                        round = -1
                    }
                }
            }
            round++
        }
    }
    inner class WindowAttributeSetter(private val window: Window) {
        @Keep
        fun setY(y: Int) {
            val attr = window.attributes
            attr.y = y
            Log.d("MySF", "set y to $y")
            window.attributes = attr
        }
    }
    companion object {
        fun newInstance(view: View, key: String?): AutoHideEditTextPreferenceDialogFragmentCompat {
            val fragment = AutoHideEditTextPreferenceDialogFragmentCompat(view)
            val b = Bundle(1)
            b.putString(ARG_KEY, key)
            fragment.setArguments(b)
            return fragment
        }
    }
}
