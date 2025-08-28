package com.alimaharramly.gpstracker.utils

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import com.alimaharramly.gpstracker.R
import com.alimaharramly.gpstracker.databinding.SaveDialogBinding
import com.alimaharramly.gpstracker.db.TrackItem
import com.alimaharramly.gpstracker.fragments.MainFragment

object DialogManager {
    fun showEnableDialog(context: Context, listener: Listener) {
        val builder = AlertDialog.Builder(context)
        val dialog = builder.create()
        dialog.setTitle(R.string.location_disabled)
        dialog.setMessage(context.getString(R.string.location_dialog_message))

        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes")
        { _, _ ->
            listener.onClick()
        }

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No")
        { _, _ ->
            dialog.dismiss()
        }

        dialog.show()
    }

    fun showSaveDialog(context: Context, item: TrackItem?, listener: Listener) {
        val builder = AlertDialog.Builder(context)
        val binding = SaveDialogBinding.inflate(LayoutInflater.from(context), null, false)
        builder.setView(binding.root)
        val dialog = builder.create()
        binding.apply {
            val time =  "${item?.time}"
            val velocity = "Velocity: ${item?.velocity} km/h"
            val distanceKm = item?.distance?.div(1000) ?: 0.0
            val distanceMeters = item?.distance ?: 0.0
            val distance = "Distance: ${"%.0f".format(distanceMeters)} m / ${"%.2f".format(distanceKm)} km"



            tvTime.text =  time
            tvVelocity.text = velocity
            tvDistance.text = distance

            bSave.setOnClickListener {
                listener.onClick()
                dialog.dismiss()
            }
            bCancel.setOnClickListener {
                dialog.dismiss()
            }
        }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    interface Listener {
        fun onClick()
    }
}
