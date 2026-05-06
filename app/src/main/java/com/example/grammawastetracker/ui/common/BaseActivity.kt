package com.example.grammawastetracker.ui.common

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.example.grammawastetracker.utils.LocaleHelper

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }
}
