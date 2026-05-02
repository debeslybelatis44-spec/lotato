package com.lotato.printserver

import android.util.Log
import com.sunmi.peripheral.printer.SunmiPrinterService
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject

/**
 * Serveur HTTP local sur le port 8787
 * Ton PWA envoie une requête POST à http://localhost:8787/print
 */
class PrintHttpServer(
    private val getPrinterService: () -> SunmiPrinterService?
) : NanoHTTPD(8787) {

    companion object {
        private const val TAG = "PrintHttpServer"
        const val PORT = 8787
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        // ── CORS headers pour autoriser les requêtes du PWA ──────────────────
        val corsHeaders = mapOf(
            "Access-Control-Allow-Origin" to "*",
            "Access-Control-Allow-Methods" to "GET, POST, OPTIONS",
            "Access-Control-Allow-Headers" to "Content-Type"
        )

        // Pré-vol OPTIONS (CORS)
        if (method == Method.OPTIONS) {
            val response = newFixedLengthResponse(Response.Status.OK, "text/plain", "OK")
            corsHeaders.forEach { (k, v) -> response.addHeader(k, v) }
            return response
        }

        // ── Route GET /status ─────────────────────────────────────────────────
        if (method == Method.GET && uri == "/status") {
            val status = JSONObject().apply {
                put("server", "LOTATO PrintBridge")
                put("version", "1.0.0")
                put("port", PORT)
                put("printer", if (getPrinterService() != null) "connected" else "disconnected")
            }
            val response = newFixedLengthResponse(
                Response.Status.OK,
                "application/json",
                status.toString()
            )
            corsHeaders.forEach { (k, v) -> response.addHeader(k, v) }
            return response
        }

        // ── Route POST /print ─────────────────────────────────────────────────
        if (method == Method.POST && uri == "/print") {
            return try {
                val contentLength = session.headers["content-length"]?.toInt() ?: 0
                val bodyBytes = ByteArray(contentLength)
                session.inputStream.read(bodyBytes, 0, contentLength)
                val body = String(bodyBytes, Charsets.UTF_8)

                val json = JSONObject(body)
                val type = json.optString("type", "text")

                val service = getPrinterService()
                if (service == null) {
                    return errorResponse("Imprimante non connectée", corsHeaders)
                }

                when (type) {
                    "ticket" -> printTicket(service, json)
                    "text"   -> printText(service, json.optString("text", ""))
                    else     -> printText(service, json.optString("text", body))
                }

                val result = JSONObject().apply {
                    put("success", true)
                    put("message", "Impression envoyée")
                }
                val response = newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    result.toString()
                )
                corsHeaders.forEach { (k, v) -> response.addHeader(k, v) }
                response

            } catch (e: Exception) {
                Log.e(TAG, "Erreur impression", e)
                errorResponse(e.message ?: "Erreur inconnue", corsHeaders)
            }
        }

        // ── Route inconnue ────────────────────────────────────────────────────
        val response = newFixedLengthResponse(
            Response.Status.NOT_FOUND,
            "application/json",
            """{"error":"Route non trouvée: $uri"}"""
        )
        corsHeaders.forEach { (k, v) -> response.addHeader(k, v) }
        return response
    }

    // ── Impression texte brut ─────────────────────────────────────────────────
    private fun printText(service: SunmiPrinterService, text: String) {
        service.printerInit(null)
        service.setAlignment(0, null)
        service.setFontSize(20f, null)
        service.printText("$text\n", null)
        service.printAndFeedPaper(60, null)
        service.cutPaper(null)
    }

    // ── Impression ticket structuré ───────────────────────────────────────────
    private fun printTicket(service: SunmiPrinterService, json: JSONObject) {
        service.printerInit(null)

        // En-tête
        val header = json.optString("header", "LOTATO PRO")
        service.setAlignment(1, null)
        service.setFontSize(28f, null)
        service.printText("$header\n", null)
        service.setFontSize(18f, null)
        service.printText("================================\n", null)

        // Lignes
        service.setAlignment(0, null)
        service.setFontSize(20f, null)
        val lines = json.optJSONArray("lines")
        if (lines != null) {
            for (i in 0 until lines.length()) {
                service.printText("${lines.getString(i)}\n", null)
            }
        }

        // Pied
        val footer = json.optString("footer", "")
        if (footer.isNotEmpty()) {
            service.setAlignment(1, null)
            service.printText("================================\n", null)
            service.setFontSize(18f, null)
            service.printText("$footer\n", null)
        }

        service.printAndFeedPaper(80, null)
        service.cutPaper(null)
    }

    // ── Réponse d'erreur ──────────────────────────────────────────────────────
    private fun errorResponse(
        message: String,
        corsHeaders: Map<String, String>
    ): Response {
        val body = JSONObject().apply {
            put("success", false)
            put("error", message)
        }
        val response = newFixedLengthResponse(
            Response.Status.INTERNAL_ERROR,
            "application/json",
            body.toString()
        )
        corsHeaders.forEach { (k, v) -> response.addHeader(k, v) }
        return response
    }
}

