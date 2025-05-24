package top.fumiama.copymanga.ui.settings

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.fumiama.copymanga.api.Config.proxyUrl
import top.fumiama.copymanga.view.interaction.UITools
import top.fumiama.copymanga.view.AutoHideEditTextPreferenceDialogFragmentCompat
import top.fumiama.dmzj.copymanga.R

class SettingsFragment: PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                delay(300)
                withContext(Dispatchers.Main) {
                    setPreferencesFromResource(R.xml.pref_setting, rootKey)
                    findPreference<ListPreference>(getString(R.string.darkModeKeyID))?.setOnPreferenceChangeListener {  _, newValue ->
                        when ((newValue as String).toInt()) {
                            0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                            2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        }
                        true // 允许保存新值
                    }
                    context?.let { PreferenceManager.getDefaultSharedPreferences(it) }?.let {
                        if (it.getString(getString(R.string.reverseProxyKeyID), "") != proxyUrl) {
                            findPreference<SwitchPreferenceCompat>(getString(R.string.apiProxyKeyID))?.isVisible = false
                            findPreference<SwitchPreferenceCompat>(getString(R.string.imgProxyKeyID))?.isVisible = false
                            findPreference<EditTextPreference>(getString(R.string.imgProxyCodeKeyID))?.isVisible = false
                        }
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        context?.let { c ->
            view.apply { post { setPadding(0, 0, 0, UITools.getNavigationBarHeight(c)) } }
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is EditTextPreference) {
            Log.d("MySF", "preference is EditTextPreference")
            val f = view?.let { AutoHideEditTextPreferenceDialogFragmentCompat.newInstance(it, preference.key) }?:return
            f.setTargetFragment(this, 0)
            f.show(parentFragmentManager, null)
            return
        }
        super.onDisplayPreferenceDialog(preference)
    }
}
