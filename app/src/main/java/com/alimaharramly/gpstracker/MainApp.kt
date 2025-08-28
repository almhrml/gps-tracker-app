package com.alimaharramly.gpstracker

import android.app.Application
import com.alimaharramly.gpstracker.db.MainDb

class MainApp: Application() {
    val database by lazy { MainDb.getDatabase(this) }

}