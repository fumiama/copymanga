package top.fumiama.copymanga.ui.settings

import android.animation.ObjectAnimator
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import androidx.annotation.Keep
import androidx.preference.EditTextPreference
import androidx.preference.EditTextPreferenceDialogFragmentCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import top.fumiama.copymanga.tools.ui.UITools
import top.fumiama.dmzj.copymanga.R
import java.lang.Thread.sleep

class SettingsFragment: PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        Thread {
            sleep(300)
            activity?.runOnUiThread {
                setPreferencesFromResource(R.xml.pref_setting, rootKey)
            }
        }.start()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        context?.let { c ->
            view.setPadding(0, 0, 0, UITools.getNavigationBarHeight(c))
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is EditTextPreference) {
            Log.d("MySF", "preference is EditTextPreference")
            val f = EditTextPreferenceDialogFragmentCompat.newInstance(preference.key)
            f.setTargetFragment(this, 0)
            f.show(parentFragmentManager, null)
            Thread {
                var diff = 0
                var cnt = 0
                while (diff == 0 && cnt++ < 20) {
                    sleep(50)
                    if (f.dialog == null) continue
                    val v = view?:return@Thread
                    // https://github.com/mikepenz/MaterialDrawer/blob/aa9136fb4f5b3a80460fe5f47213985026d20c88/library/src/main/java/com/mikepenz/materialdrawer/util/KeyboardUtil.java
                    val r = Rect()
                    //r will be populated with the coordinates of your view that area still visible.
                    v.getWindowVisibleDisplayFrame(r)
                    //get screen height and calculate the difference with the useable area from the r
                    val height = v.context.resources.displayMetrics.heightPixels
                    diff = height - r.bottom
                    Log.d("MySF", "diff: $diff")
                }
                Log.d("MySF", "diff out while: $diff")
                if (diff <= 0) return@Thread
                Log.d("MySF", "f.dialog is ${f.dialog}")
                f.activity?.runOnUiThread {
                    f.dialog?.window?.apply {
                        val attr = attributes
                        Log.d("MySF", "animate from ${attr.y} to ${attr.y-diff/2}")
                        ObjectAnimator.ofInt(WindowAttributeSetter(this), "y", attr.y, attr.y-diff/2).setDuration(233).start()
                    }
                }
            }.start()
            return
        }
        super.onDisplayPreferenceDialog(preference)
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
}
