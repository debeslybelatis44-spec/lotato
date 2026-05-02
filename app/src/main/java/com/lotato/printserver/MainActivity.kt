package com.lotato.printserver

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStop = findViewById<Button>(R.id.btnStop)

        tvStatus.text = """
            🖨️ LOTATO PrintBridge
            
            Serveur local : http://localhost:8787
            
            Routes disponibles :
            GET  /status    → état imprimante
            POST /print     → imprimer
            
            Ce service fonctionne en arrière-plan.
            Ton PWA peut envoyer des impressions
            même quand cette fenêtre est fermée.
        """.trimIndent()

        btnStart.setOnClickListener {
            val intent = Intent(this, PrintService::class.java)
            startForegroundService(intent)
            tvStatus.text = "✅ Serveur démarré sur le port 8787\n\nTon PWA peut maintenant imprimer."
        }

        btnStop.setOnClickListener {
            val intent = Intent(this, PrintService::class.java)
            stopService(intent)
            tvStatus.text = "⏹️ Serveur arrêté."
        }
    }
}

