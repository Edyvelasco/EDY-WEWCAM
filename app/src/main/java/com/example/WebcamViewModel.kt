package com.example

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

data class CameraDeviceOption(
    val id: String,
    val lensFacing: Int,
    val name: String
)

class WebcamViewModel(application: Application) : AndroidViewModel(application) {

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    private val _serverIp = MutableStateFlow<String?>(null)
    val serverIp: StateFlow<String?> = _serverIp.asStateFlow()

    private val _serverPort = MutableStateFlow(8080)
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()

    private val _fps = MutableStateFlow(0)
    val fps: StateFlow<Int> = _fps.asStateFlow()

    private val _jpegQuality = MutableStateFlow(85)
    val jpegQuality: StateFlow<Int> = _jpegQuality.asStateFlow()

    private val _resolutionWidth = MutableStateFlow(1280)
    val resolutionWidth: StateFlow<Int> = _resolutionWidth.asStateFlow()

    private val _resolutionHeight = MutableStateFlow(720)
    val resolutionHeight: StateFlow<Int> = _resolutionHeight.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(false)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    private val _availableCameras = MutableStateFlow<List<CameraDeviceOption>>(emptyList())
    val availableCameras: StateFlow<List<CameraDeviceOption>> = _availableCameras.asStateFlow()

    private val _selectedCameraId = MutableStateFlow<String?>(null)
    val selectedCameraId: StateFlow<String?> = _selectedCameraId.asStateFlow()

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _isAutofocusEnabled = MutableStateFlow(true)
    val isAutofocusEnabled: StateFlow<Boolean> = _isAutofocusEnabled.asStateFlow()

    private val _isMirrorEnabled = MutableStateFlow(false)
    val isMirrorEnabled: StateFlow<Boolean> = _isMirrorEnabled.asStateFlow()

    private val _isGridEnabled = MutableStateFlow(false)
    val isGridEnabled: StateFlow<Boolean> = _isGridEnabled.asStateFlow()

    private val _serverStatusMessage = MutableStateFlow("Inactivo")
    val serverStatusMessage: StateFlow<String> = _serverStatusMessage.asStateFlow()

    val mjpegServer = MjpegServer()

    private val frameCount = AtomicInteger(0)
    private var fpsJob: Job? = null
    private var ipUpdateJob: Job? = null

    init {
        updateIpAddress()
        ipUpdateJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                updateIpAddress()
                delay(5000)
            }
        }
    }

    fun updateIpAddress() {
        val ip = NetworkUtils.getLocalIpAddress()
        _serverIp.value = ip
    }

    fun startServer() {
        if (_isServerRunning.value) return
        val port = _serverPort.value
        mjpegServer.start(
            port = port,
            onStarted = { msg ->
                _isServerRunning.value = true
                _serverStatusMessage.value = "Transmitiendo"
                Log.d("WebcamViewModel", msg)
                startFpsCounter()
            },
            onError = { err ->
                _isServerRunning.value = false
                _serverStatusMessage.value = "Error: $err"
                Log.e("WebcamViewModel", err)
            }
        )
    }

    fun stopServer() {
        mjpegServer.stop()
        _isServerRunning.value = false
        _serverStatusMessage.value = "Inactivo"
        stopFpsCounter()
    }

    fun setPort(port: Int) {
        if (!_isServerRunning.value) {
            _serverPort.value = port.coerceIn(1024, 65535)
        }
    }

    fun setJpegQuality(quality: Int) {
        _jpegQuality.value = quality.coerceIn(10, 100)
    }

    fun setResolution(width: Int, height: Int) {
        _resolutionWidth.value = width
        _resolutionHeight.value = height
    }

    fun setAvailableCameras(cameras: List<CameraDeviceOption>) {
        _availableCameras.value = cameras
        // Automatically select the first camera if none is selected
        if (_selectedCameraId.value == null && cameras.isNotEmpty()) {
            val defaultBack = cameras.firstOrNull { it.lensFacing == 1 }
            _selectedCameraId.value = defaultBack?.id ?: cameras.first().id
            _isFrontCamera.value = (defaultBack?.lensFacing == 0)
        }
    }

    fun selectCamera(cameraId: String) {
        _selectedCameraId.value = cameraId
        val camera = _availableCameras.value.find { it.id == cameraId }
        if (camera != null) {
            _isFrontCamera.value = (camera.lensFacing == 0) // 0 is LENS_FACING_FRONT
        }
    }

    fun toggleCamera() {
        val currentIsFront = _isFrontCamera.value
        val targetLensFacing = if (currentIsFront) 1 else 0 // 1 = BACK, 0 = FRONT
        val targetCamera = _availableCameras.value.firstOrNull { it.lensFacing == targetLensFacing }
        if (targetCamera != null) {
            selectCamera(targetCamera.id)
        } else {
            _isFrontCamera.value = !currentIsFront
            _selectedCameraId.value = null
        }
        _isTorchOn.value = false
    }

    fun toggleTorch() {
        _isTorchOn.value = !_isTorchOn.value
    }

    fun toggleAutofocus() {
        _isAutofocusEnabled.value = !_isAutofocusEnabled.value
    }

    fun toggleMirror() {
        _isMirrorEnabled.value = !_isMirrorEnabled.value
    }

    fun toggleGrid() {
        _isGridEnabled.value = !_isGridEnabled.value
    }

    fun recordFrameCaptured() {
        frameCount.incrementAndGet()
    }

    private fun startFpsCounter() {
        fpsJob?.cancel()
        frameCount.set(0)
        fpsJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _fps.value = frameCount.getAndSet(0)
            }
        }
    }

    private fun stopFpsCounter() {
        fpsJob?.cancel()
        fpsJob = null
        _fps.value = 0
    }

    override fun onCleared() {
        super.onCleared()
        stopServer()
        ipUpdateJob?.cancel()
    }
}
