package org.torproject.vpn.ui.exitselection.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.torproject.onionmasq.OnionMasq
import org.torproject.onionmasq.errors.CountryCodeException
import org.torproject.onionmasq.errors.ProxyStoppedException
import org.torproject.vpn.ui.exitselection.data.ExitNodeAdapter
import org.torproject.vpn.utils.PreferenceHelper
import java.util.Locale

class ExitSelectionBottomSheetViewModel(application: Application) : AndroidViewModel(application) {

    private val _list = MutableLiveData<List<ViewTypeDependentModel>>(mutableListOf())
    val list: LiveData<List<ViewTypeDependentModel>> = _list
    private val preferenceHelper = PreferenceHelper(application)
    private val _fitHalfExpandedContent = MutableLiveData(preferenceHelper.automaticExitNodeSelection)
    val fitHalfExpandedContent: LiveData<Boolean> = _fitHalfExpandedContent


    fun requestExitNodes() {
        val automaticExitNodeSelected = preferenceHelper.automaticExitNodeSelection
        _list.postValue(getExitNodeList(automaticExitNodeSelected))
    }

    private fun getExitNodeList(automaticNodeSelection: Boolean): List<ViewTypeDependentModel> {
        val modelList = mutableListOf<ViewTypeDependentModel>()
        modelList.add(ExitNodeTableHeaderModel(automaticNodeSelection))
        if (!automaticNodeSelection) {
            val exitNodeCountry = preferenceHelper.exitNodeCountry
            val countryMap = HashMap<String, ExitNodeCellModel>()
            val exitNodeCountries = preferenceHelper.relayCountries
            if (exitNodeCountries.isEmpty()) {
                val locales = Locale.getAvailableLocales()
                for (locale in locales) {
                   addExitNodeCountryToMap(countryMap, locale, exitNodeCountry)
                }
            } else {
                for (countryCode in exitNodeCountries) {
                    val locale = Locale("", countryCode.uppercase())
                    addExitNodeCountryToMap(countryMap, locale, exitNodeCountry)
                }
            }
            modelList.addAll(ArrayList(countryMap.values).sorted())
        }
        return modelList
    }

    private fun addExitNodeCountryToMap(map: HashMap<String, ExitNodeCellModel>, locale: Locale, selectedExitNodeCountry: String?) {
        if (locale.country.isNullOrEmpty() || locale.country.length != 2) {
            return
        }
        map[locale.country.lowercase()] = ExitNodeCellModel(
            locale.country.lowercase(),
            locale.displayCountry,
            locale.country.lowercase() == selectedExitNodeCountry
        )
    }

    fun onExitNodeSelected(pos: Int, model: ExitNodeCellModel) {
        list.value?.let {
            val mutableList = it.toMutableList()
            mutableList.onEach { model ->
                if (model.getViewType() == ExitNodeAdapter.CELL) {
                    (model as ExitNodeCellModel).selected = false
                }
            }
            (mutableList[pos] as ExitNodeCellModel).selected = true
            _list.postValue(mutableList)
            preferenceHelper.exitNodeCountry = model.countryCode
            setCountryCode(model.countryCode)
        }
    }

    fun onAutomaticExitNodeChanged(model: ExitNodeTableHeaderModel) {
        _list.postValue(getExitNodeList(model.selected))
        _fitHalfExpandedContent.postValue(model.selected)
        preferenceHelper.automaticExitNodeSelection = model.selected
        if (model.selected) {
            setCountryCode(null)
        } else {
            setCountryCode(preferenceHelper.exitNodeCountry)
        }
    }

    private fun setCountryCode(code: String?) {
        try {
            OnionMasq.setCountryCode(code)
            OnionMasq.refreshCircuits()
        } catch (e: CountryCodeException) {
            e.printStackTrace()
        } catch (e: ProxyStoppedException) {
            e.printStackTrace()
        }
    }
}