package com.alimaharramly.gpstracker.utils

import android.content.pm.PackageManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.alimaharramly.gpstracker.R


fun Fragment.openFragment(f: Fragment) {
    (activity as AppCompatActivity).supportFragmentManager
        .beginTransaction()
        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
        .replace(R.id.placeHolder, f).commit()
}

fun AppCompatActivity.openFragment(f: Fragment) {
    if (supportFragmentManager.fragments.isNotEmpty()){
        if (supportFragmentManager.fragments[0].javaClass == f.javaClass) return // Don't open if it's the same fragment
    }
    supportFragmentManager
        .beginTransaction()
        .replace(R.id.placeHolder, f).commit()
}

fun Fragment.showToast(s: String){
    Toast.makeText(activity, s, Toast.LENGTH_SHORT).show()
}


fun Fragment.checkPermission(p: String): Boolean {
    return ContextCompat.checkSelfPermission(requireContext(), p) == PackageManager.PERMISSION_GRANTED
}
