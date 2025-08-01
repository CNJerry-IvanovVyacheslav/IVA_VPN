package com.example.iva_vpn

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.iva_vpn.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Кнопка "Старт"
        binding.vpnButton.setOnClickListener {
            val intent = VpnService.prepare(this)
            if (intent != null) {
                // Запрашиваем разрешение
                startActivityForResult(intent, 1)
            } else {
                // Разрешение уже есть, запускаем сервис
                onActivityResult(1, RESULT_OK, null)
            }
        }

        // Добавьте вторую кнопку в ваш layout с id="stopVpnButton"
        binding.stopVpnButton.setOnClickListener {
            val intent = Intent(this, IvaVpnService::class.java)
            intent.action = IvaVpnService.ACTION_STOP_VPN
            startService(intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            // Разрешение получено, запускаем VPN
            val intent = Intent(this, IvaVpnService::class.java)
            intent.action = IvaVpnService.ACTION_START_VPN
            startService(intent)
        }
    }
}