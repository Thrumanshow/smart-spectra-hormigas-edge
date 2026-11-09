package com.hormigasais.presagechallenge

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.presagetech.smartspectra.CameraSelection
import com.presagetech.smartspectra.ProcessingStatus
import com.presagetech.smartspectra.SmartSpectraConfig
import com.presagetech.smartspectra.SmartSpectraSdk
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private val sdk by lazy { SmartSpectraSdk.shared }

    private lateinit var statusView: TextView
    private lateinit var pulseView: TextView
    private lateinit var startButton: Button

    private val cameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startMeasurement()
        } else {
            statusView.text = "Permiso de cámara denegado; medición detenida."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sdk.config.apiKey = BuildConfig.PRESAGE_API_KEY
        sdk.config.imageOutputEnabled = false
        sdk.useCamera(CameraSelection.Front)
        sdk.config.requestedMetrics =
            SmartSpectraConfig.breathingMetrics + SmartSpectraConfig.cardioMetrics

        buildScreen()
        observeSdk()
        statusView.text = "Inactivo. Pulsa Iniciar para leer el consentimiento."
    }

    private fun buildScreen() {
        val preview = PreviewView(this).apply {
            contentDescription = "Vista previa de la cámara"
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
        sdk.config.previewSurfaceProvider = preview.surfaceProvider

        statusView = TextView(this).apply {
            textSize = 16f
            setPadding(20, 16, 20, 8)
        }

        pulseView = TextView(this).apply {
            text = "Pulso: -- bpm"
            textSize = 24f
            setPadding(20, 8, 20, 16)
        }

        startButton = Button(this).apply {
            text = "Iniciar"
        }

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 24)
            addView(statusView)
            addView(pulseView)
            addView(startButton)
        }

        val root = FrameLayout(this)
        root.addView(
            preview,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        root.addView(
            controls,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            )
        )

        setContentView(root)
    }

    private fun observeSdk() {
        sdk.metrics.observe(this) { metrics ->
            val pulse = metrics
                ?.cardio
                ?.pulseRateList
                ?.lastOrNull { it.timestamp > 0 }
                ?.value

            if (pulse != null) {
                pulseView.text = "Pulso: ${pulse.roundToInt()} bpm"
                statusView.text = "SmartSpectra está procesando la señal."
            }
        }

        sdk.error.observe(this) { error ->
            if (error != null) {
                statusView.text =
                    "Error SmartSpectra: ${error.message ?: "sin detalle"}"
            }
        }

        sdk.processingStatus.observe(this) { state ->
            when (state) {
                ProcessingStatus.RUNNING -> {
                    statusView.text = "Medición activa."
                    startButton.text = "Detener"
                    startButton.isEnabled = true
                }
                ProcessingStatus.STARTING -> {
                    statusView.text = "Iniciando medición…"
                    startButton.isEnabled = false
                }
                ProcessingStatus.STOPPING -> {
                    statusView.text = "Deteniendo…"
                    startButton.isEnabled = false
                }
                ProcessingStatus.IDLE, ProcessingStatus.ERROR -> {
                    startButton.text = "Iniciar"
                    startButton.isEnabled = true
                }
                else -> Unit
            }
        }

        startButton.setOnClickListener {
            if (sdk.processingStatus.value == ProcessingStatus.RUNNING) {
                stopMeasurement()
            } else {
                showConsent()
            }
        }
    }

    private fun showConsent() {
        AlertDialog.Builder(this)
            .setTitle("Uso de la cámara y señales")
            .setMessage(
                "Si continúas, SmartSpectra usará la cámara frontal para procesar " +
                    "señales fisiológicas, como el pulso. Esta app de laboratorio " +
                    "no guarda deliberadamente video ni envía tramas LBH. El SDK " +
                    "puede tener su propio tratamiento de datos o telemetría; consulta " +
                    "la documentación y política de Presage. Presage informa que la " +
                    "telemetría está activada por defecto y envía un resumen agregado " +
                    "de diagnóstico por sesión. Según Presage, no incluye video sin " +
                    "procesar, valores medidos ni identificadores estables. Esta app " +
                    "no desactiva la telemetría. Puedes cancelar antes de iniciar o " +
                    "detener la medición después. No es una herramienta médica."
            )
            .setPositiveButton("Aceptar y continuar") { _, _ ->
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    startMeasurement()
                } else {
                    cameraPermission.launch(Manifest.permission.CAMERA)
                }
            }
            .setNegativeButton("Cancelar") { _, _ ->
                statusView.text = "Consentimiento no otorgado; cámara inactiva."
            }
            .show()
    }

    private fun startMeasurement() {
        if (BuildConfig.PRESAGE_API_KEY.isBlank()) {
            statusView.text =
                "Falta la clave local de desarrollo; no se inició la cámara."
            return
        }

        lifecycleScope.launch {
            runCatching { sdk.start() }
                .onFailure { error ->
                    statusView.text =
                        "No se pudo iniciar SmartSpectra: ${error.message}"
                }
        }
    }

    private fun stopMeasurement() {
        lifecycleScope.launch {
            runCatching { sdk.stop() }
                .onFailure { error ->
                    statusView.text =
                        "No se pudo detener SmartSpectra: ${error.message}"
                }
        }
    }

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch {
            if (sdk.processingStatus.value == ProcessingStatus.RUNNING) {
                runCatching { sdk.stop() }
            }
        }
    }
}
