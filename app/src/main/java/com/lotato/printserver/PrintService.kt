package com.lotato.printserver

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.sunmi.peripheral.printer.InnerPrinterCallback
import com.sunmi.peripheral.printer.InnerPrinterManager
import com.sunmi.peripheral.printer.SunmiPrinterService

class PrintService : Service() {

    private var httpServer: PrintHttpServer? = null
    private var sunmiPrinterService: SunmiPrinterService? = null

    companion object {
        const val CHANNEL_ID = "lotato_print_channel"
        const val NOTIF_ID = 1001
    }

    private val printerCallback = object : InnerPrinterCallback() {
        override fun onConnected(service: SunmiPrinterService) {
            sunmiPrinterService = service
        }
        override fun onDisconnected() {
            sunmiPrinterService = null
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("Démarrage..."))

        // Connexion imprimante
        InnerPrinterManager.getInstance().bindService(this, printerCallback)

        // Démarrage serveur HTTP
        httpServer = PrintHttpServer { sunmiPrinterService }
        httpServer?.start()

        updateNotification("✅ Serveur actif sur port ${PrintHttpServer.PORT}")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // Le service redémarre automatiquement si tué
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        httpServer?.stop()
        InnerPrinterManager.getInstance().unBindService(this, printerCallback)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "LOTATO Print Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Serveur d'impression LOTATO en arrière-plan"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LOTATO PrintBridge")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_print)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIF_ID, buildNotification(text))
    }
}

