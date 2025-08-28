package com.alimaharramly.gpstracker.fragments

import android.graphics.Color
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.alimaharramly.gpstracker.R

class SettingsFragment : PreferenceFragmentCompat() {
    private lateinit var timePref: Preference
    private lateinit var colorPref: Preference


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_preference, rootKey) // Загружаем настройки из XML
        init() // Инициализируем настройки
    }

    private fun init() {
        timePref = findPreference("update_time_key")!! // Находим Preference по ключу
        colorPref = findPreference("color_key")!!
        val changeListener = OnChangeListener() // Создаём слушатель изменений
        timePref.onPreferenceChangeListener = changeListener // Устанавливаем его для timePref
        colorPref.onPreferenceChangeListener = changeListener // Устанавливаем его для colorPref

        initPrefs() // Инициализируем настройки
    }

    private fun OnChangeListener(): Preference.OnPreferenceChangeListener {
        return Preference.OnPreferenceChangeListener {
            pref, value ->
             when(pref.key){
                 "update_time_key" -> OnTimeChangeListener(value.toString())
                 "color_key" -> pref.icon?.setTint(Color.parseColor(value.toString()))
             }
            true
        }
    }

    private fun OnTimeChangeListener(value: String) {
            val nameArray =
                resources.getStringArray(R.array.loc_time_update_name) // Массив отображаемых названий
            val valueArray =
                resources.getStringArray(R.array.loc_time_update_value) // Массив значений
            val title =
                timePref.title.toString().substringBefore(":") // Удаляем все после ":" каждое изменение
            val pos = valueArray.indexOf(value) // Определяем индекс нового значения
            timePref.title = "$title: ${nameArray[pos]}" // Обновляем заголовок
        }



    private fun initPrefs() {
        val pref = timePref.preferenceManager.sharedPreferences
        val nameArray = resources.getStringArray(R.array.loc_time_update_name)
        val valueArray = resources.getStringArray(R.array.loc_time_update_value)
        val title = timePref.title
        val pos = valueArray.indexOf(pref?.getString("update_time_key", "3000"))
        timePref.title = "$title: ${nameArray[pos]}"

        val trackColor = pref?.getString("color_key", "#0096FF")
        colorPref.icon?.setTint(Color.parseColor(trackColor))

    }
}
