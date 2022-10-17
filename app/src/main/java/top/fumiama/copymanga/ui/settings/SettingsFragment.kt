package top.fumiama.copymanga.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SeekBarPreference
import top.fumiama.dmzj.copymanga.R

class SettingsFragment: PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_setting, rootKey)
        settingsPref = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }
    }

    companion object {
        var settingsPref: SharedPreferences? = null
    }
}
