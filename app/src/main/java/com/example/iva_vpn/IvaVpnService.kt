package com.example.iva_vpn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import androidx.core.app.NotificationCompat
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

// ✅ ИЗМЕНЕНИЕ 1: Реализуем интерфейс Tunnel
class IvaVpnService : VpnService(), Tunnel {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var backend: GoBackend? = null
    private var tunnelName: String = "IvaVpnTunnel" // Имя по умолчанию

    companion object {
        const val VPN_CHANNEL_ID = "iva_vpn_channel"
        const val ACTION_START_VPN = "com.example.iva_vpn.START_VPN"
        const val ACTION_STOP_VPN = "com.example.iva_vpn.STOP_VPN"
    }

    // ✅ ИЗМЕНЕНИЕ 2: Реализуем обязательный метод интерфейса
    override fun getName(): String = tunnelName

    override fun onCreate() {
        super.onCreate()
        backend = GoBackend(this)
    }

    override fun onStateChange(state: Tunnel.State) {
        println("📶 Состояние VPN изменилось: $state")
        // Если хочешь — можешь обновлять уведомление или UI здесь
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_VPN -> {
                startVpn()
                return START_STICKY
            }
            ACTION_STOP_VPN -> {
                stopVpn()
                return START_NOT_STICKY
            }
            else -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
    }

    private fun startVpn() {
        serviceScope.launch {
            try {
                val configString = """
                    [Interface]
                    PrivateKey = PASTE_YOUR_PRIVATE_KEY_HERE
                    Address = 10.8.0.2/24
                    DNS = 8.8.8.8

                    [Peer]
                    PublicKey = PASTE_SERVER_PUBLIC_KEY_HERE
                    AllowedIPs = 0.0.0.0/0
                    Endpoint = YOUR_SERVER_IP:PORT
                """.trimIndent()

                val config = Config.parse(configString.byteInputStream())

                // ✅ ИЗМЕНЕНИЕ 3: Исправленный вызов setState
                backend?.setState(this@IvaVpnService, Tunnel.State.UP, config)

                showNotification(true)
                println("✅ VPN запущен!")

            } catch (e: Exception) {
                println("❌ Ошибка запуска VPN: ${e.message}")
                e.printStackTrace()
                stopVpn()
            }
        }
    }

    private fun stopVpn() {
        serviceScope.launch {
            try {
                // ✅ ИЗМЕНЕНИЕ 4: Исправленный вызов setState
                backend?.setState(this@IvaVpnService, Tunnel.State.DOWN, null)
                println("🛑 VPN остановлен.")
            } catch (e: Exception) {
                println("❌ Ошибка остановки VPN: ${e.message}")
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun showNotification(isConnected: Boolean) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(VPN_CHANNEL_ID, "IVA VPN Service", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(this, VPN_CHANNEL_ID)
            .setContentTitle("IVA VPN")
            .setContentText(if (isConnected) "Подключено" else "Отключено")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        // ✅ ИЗМЕНЕНИЕ 5: Исправленный вызов getState
        if (backend?.getState(this) == Tunnel.State.UP) {
            stopVpn()
        }
        serviceScope.cancel()
        println("Сервис уничтожен.")
    }
}