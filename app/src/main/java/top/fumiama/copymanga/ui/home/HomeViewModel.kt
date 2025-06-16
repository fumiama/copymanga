package top.fumiama.copymanga.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {
    val indexStructure = MutableLiveData<String?>(null)

    fun saveIndexStructure(index: String?) {
        indexStructure.value = index
    }
}
