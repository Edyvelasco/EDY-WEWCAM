package com.example

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class MjpegServer {
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _activeClients = ConcurrentHashMap<String, Socket>()
    private val _activeClientsCount = MutableStateFlow(0)
    val activeClientsCount: StateFlow<Int> = _activeClientsCount.asStateFlow()

    private val _dataRateBytesPerSec = MutableStateFlow(0L)
    val dataRateBytesPerSec: StateFlow<Long> = _dataRateBytesPerSec.asStateFlow()

    private val bytesTransmitted = AtomicLong(0)

    private var latestFrame: ByteArray? = null
    private val frameLock = Any()
    private var frameId = 0L

    fun updateFrame(jpegBytes: ByteArray) {
        synchronized(frameLock) {
            latestFrame = jpegBytes
            frameId++
        }
    }

    fun start(port: Int, onStarted: (String) -> Unit, onError: (String) -> Unit) {
        if (serverJob != null) return

        serverJob = scope.launch {
            try {
                val socket = ServerSocket(port)
                serverSocket = socket
                onStarted("Server started on port $port")

                // Launch speed monitor
                launch {
                    while (serverJob?.isActive == true) {
                        delay(1000)
                        val bytes = bytesTransmitted.getAndSet(0)
                        _dataRateBytesPerSec.value = bytes
                    }
                }

                while (serverJob?.isActive == true) {
                    val client = socket.accept()
                    val clientId = client.remoteSocketAddress.toString()
                    _activeClients[clientId] = client
                    _activeClientsCount.value = _activeClients.size

                    launch {
                        handleClient(client, clientId)
                    }
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Unknown server error")
            } finally {
                stop()
            }
        }
    }

    fun stop() {
        serverJob?.cancel()
        serverJob = null
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            // Ignore
        }
        serverSocket = null

        _activeClients.values.forEach {
            try {
                it.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
        _activeClients.clear()
        _activeClientsCount.value = 0
        _dataRateBytesPerSec.value = 0
    }

    private suspend fun handleClient(client: Socket, clientId: String) {
        withContext(Dispatchers.IO) {
            try {
                val reader = client.getInputStream().reader()
                val writer = client.getOutputStream()

                // Read request header lines
                val requestLine = reader.readRequestLine() ?: return@withContext
                Log.d("MjpegServer", "Request: $requestLine")

                if (requestLine.startsWith("GET /stream")) {
                    sendStreamHeaders(writer)
                    streamFrames(writer)
                } else if (requestLine.startsWith("GET / ") || requestLine.startsWith("GET /index")) {
                    sendHtmlResponse(writer, client.localAddress?.hostAddress ?: "localhost")
                } else {
                    send404(writer)
                }
            } catch (e: Exception) {
                Log.e("MjpegServer", "Error handling client $clientId: ${e.message}")
            } finally {
                _activeClients.remove(clientId)
                _activeClientsCount.value = _activeClients.size
                try {
                    client.close()
                } catch (e: Exception) {
                    // Ignore
                }
                Log.d("MjpegServer", "Client disconnected: $clientId")
            }
        }
    }

    private fun java.io.InputStream.reader(): java.io.BufferedReader {
        return java.io.BufferedReader(java.io.InputStreamReader(this))
    }

    private fun java.io.BufferedReader.readRequestLine(): String? {
        return try {
            readLine()
        } catch (e: Exception) {
            null
        }
    }

    private fun sendStreamHeaders(out: OutputStream) {
        val headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: multipart/x-mixed-replace; boundary=--frame\r\n" +
                "Connection: keep-alive\r\n" +
                "Cache-Control: no-cache, private\r\n" +
                "Pragma: no-cache\r\n" +
                "Max-Age: 0\r\n" +
                "Expires: 0\r\n\r\n"
        out.write(headers.toByteArray())
        out.flush()
    }

    private suspend fun streamFrames(out: OutputStream) {
        var lastStreamedFrameId = -1L
        while (serverJob?.isActive == true) {
            var frameToStream: ByteArray? = null
            var currentFrameId = -1L

            synchronized(frameLock) {
                if (frameId > lastStreamedFrameId) {
                    frameToStream = latestFrame
                    currentFrameId = frameId
                }
            }

            if (frameToStream != null) {
                lastStreamedFrameId = currentFrameId
                val frameHeader = "--frame\r\n" +
                        "Content-Type: image/jpeg\r\n" +
                        "Content-Length: ${frameToStream!!.size}\r\n\r\n"
                out.write(frameHeader.toByteArray())
                out.write(frameToStream!!)
                out.write("\r\n".toByteArray())
                out.flush()
                bytesTransmitted.addAndGet((frameHeader.length + frameToStream!!.size + 2).toLong())
            }

            delay(15) // Limit rate monitoring checking to save CPU
        }
    }

    private fun sendHtmlResponse(out: OutputStream, hostIp: String) {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>CamLink Web Camera</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                        background-color: #0c0f17;
                        color: #e2e8f0;
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        justify-content: center;
                        min-height: 100vh;
                        margin: 0;
                        padding: 20px;
                    }
                    .container {
                        max-width: 900px;
                        width: 100%;
                        text-align: center;
                        background: #181d29;
                        padding: 30px;
                        border-radius: 16px;
                        box-shadow: 0 10px 25px rgba(0,0,0,0.5);
                        border: 1px solid #2d3748;
                    }
                    h1 {
                        color: #38bdf8;
                        margin-bottom: 5px;
                        font-weight: 700;
                    }
                    .subtitle {
                        color: #94a3b8;
                        margin-bottom: 20px;
                        font-size: 1.1em;
                    }
                    .video-wrapper {
                        background: #000;
                        border-radius: 12px;
                        overflow: hidden;
                        border: 2px solid #38bdf8;
                        margin-bottom: 25px;
                        line-height: 0;
                        box-shadow: 0 0 20px rgba(56, 189, 248, 0.2);
                    }
                    img {
                        max-width: 100%;
                        height: auto;
                    }
                    .instructions {
                        text-align: left;
                        background: #0f131a;
                        padding: 20px;
                        border-radius: 8px;
                        border-left: 4px solid #38bdf8;
                    }
                    ol {
                        padding-left: 20px;
                        line-height: 1.6;
                        margin: 10px 0 0 0;
                    }
                    li {
                        margin-bottom: 8px;
                    }
                    code {
                        background: #1e293b;
                        padding: 3px 6px;
                        border-radius: 4px;
                        font-family: monospace;
                        color: #f472b6;
                        font-size: 0.95em;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>CamLink Web Camera</h1>
                    <div class="subtitle">Transmisión de Cámara en Alta Calidad vía Wi-Fi o USB</div>
                    <div class="video-wrapper">
                        <img src="/stream" alt="Live Camera Stream" />
                    </div>
                    <div class="instructions">
                        <h3 style="margin: 0 0 10px 0; color: #f8fafc;">Cómo usar en OBS Studio / Reproductores:</h3>
                        <p style="margin: 0; color: #94a3b8;">Sigue estos sencillos pasos para conectar tu cámara:</p>
                        <ol>
                            <li>Abre OBS Studio en tu ordenador.</li>
                            <li>Añade una fuente de tipo <b>Navegador (Browser Source)</b>.</li>
                            <li>En el campo de URL introduce: <code>http://$hostIp:8080/stream</code> (o <code>http://localhost:8080/stream</code> si usas USB).</li>
                            <li>Configura el ancho y alto del Navegador para que coincidan con la resolución elegida en la app (ej. 1920x1080 o 1280x720).</li>
                            <li><i>Opcional (WiFi/VLC):</i> También puedes abrir esta URL en VLC Media Player como un flujo de red.</li>
                        </ol>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        val response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: ${html.toByteArray().size}\r\n" +
                "Connection: close\r\n\r\n" +
                html
        out.write(response.toByteArray())
        out.flush()
    }

    private fun send404(out: OutputStream) {
        val body = "404 Not Found"
        val response = "HTTP/1.1 404 Not Found\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: ${body.length}\r\n" +
                "Connection: close\r\n\r\n" +
                body
        out.write(response.toByteArray())
        out.flush()
    }
}
