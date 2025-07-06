package top.fumiama.copymangaweb.activity.viewmodel

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainViewModel: ViewModel() {
    val progress = MutableLiveData(0)
    val progressDuration = MutableLiveData(233L)
    val progressVisibility = MutableLiveData(View.VISIBLE)
    val fabVisibility = MutableLiveData(View.GONE)
    val showDlList = MutableLiveData(false)

    suspend fun updateLoadProgress(p: Int) = withContext(Dispatchers.Main) {
        if(progress.value == 100 && p < 100) {
            progress.value = 0
            progressVisibility.value = View.VISIBLE
            return@withContext
        }
        progress.value = p
    }

    suspend fun setFabVisibility(visible: Boolean) {
        withContext(Dispatchers.Main) {
            fabVisibility.value = if (visible) View.VISIBLE else View.GONE
        }
    }
}
