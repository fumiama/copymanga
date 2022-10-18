package top.fumiama.copymanga.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import top.fumiama.dmzj.copymanga.R

class SettingsFragment: PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_setting, rootKey)
        if(settingsPref == null) settingsPref = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }
    }

    companion object {
        var settingsPref: SharedPreferences? = SettingsFragment().context?.let {PreferenceManager.getDefaultSharedPreferences(it)}
    }
}
