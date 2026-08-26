package com.leshoraa.kore.service.receiver

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.leshoraa.kore.core.common.ServiceLocator

/**
 * Receiver to handle Bluetooth state changes (ON/OFF).
 */
class BluetoothStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            when (state) {
                BluetoothAdapter.STATE_OFF -> {
                    Log.w("BluetoothReceiver", "Bluetooth turned OFF. Disconnecting service.")
                    ServiceLocator.provideBleManager(context).disconnect()
                }
                BluetoothAdapter.STATE_ON -> {
                    Log.i("BluetoothReceiver", "Bluetooth turned ON. Ready for connection.")
                }
            }
        }
    }
}
