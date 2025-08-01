package com.example.iva_vpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream

class IvaVpnService: VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val builder = Builder()
            .setSession("PrivateVPN")
            .addAddress("10.0.0.2", 24)
            .addDnsServer("8.8.8.8")
            .addRoute("0.0.0.0", 0)

        vpnInterface = builder.establish()

        vpnInterface?.fileDescriptor?.let {
            GlobalScope.launch(Dispatchers.IO) {
                val input = FileInputStream(it)
                val output = FileOutputStream(it)
                val packet = ByteArray(32767)

                while (true) {
                    val length = input.read(packet)
                    if (length > 0) {
                        // Здесь можно отправлять трафик на сервер
                    }
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        vpnInterface?.close()
        super.onDestroy()
    }
}