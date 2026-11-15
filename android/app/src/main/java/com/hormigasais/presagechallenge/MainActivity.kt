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

    // Private experimental state: only latest scalar samples/timestamps and a count are retained in RAM.
    private val maxLbhPairSkewMs = 100L // provisional lab tolerance; not a SmartSpectra contract
    private var latestPulseFields: SmartSpectraFieldMapper.MeasurementWithConfidenceFields? = null
    private var latestBreathingFields: SmartSpectraFieldMapper.MeasurementWithConfidenceFields? = null
    private var lastEncodedPulseTimestampMicros: Long? = null
    private var lastEncodedBreathingTimestampMicros: Long? = null
    private var lastConsensusPulseTimestampMicros: Long? = null
    private var lastConsensusBreathingTimestampMicros: Long? = null
    private val sessionConsensusEvaluator = SmartSpectraConsensusEvaluator(
        SmartSpectraConsensusEvaluator.Policy(
            windowDurationMs = 20_000L, // candidate window for this private experiment
            maxPairSkewMs = maxLbhPairSkewMs, // existing provisional 100 ms pair tolerance
            maxInterSampleGapMs = 3_000L, // provisional engineering guard; not clinical evidence
        )
    )
    private var locallyEncodedFrameCount: Long = 0

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
            // Conservar las últimas muestras en memoria para manejar listas vacías entre emisiones.
            val pulseMeasurement = metrics
                ?.cardio
                ?.pulseRateList
                ?.filter { it.timestamp > 0L }
                ?.maxByOrNull { it.timestamp }
            val breathingMeasurement = metrics
                ?.breathing
                ?.rateList
                ?.filter { it.timestamp > 0L }
                ?.maxByOrNull { it.timestamp }

            if (pulseMeasurement != null) {
                latestPulseFields = SmartSpectraFieldMapper.MeasurementWithConfidenceFields(
                    value = pulseMeasurement.value,
                    confidence = pulseMeasurement.confidence,
                    timestampMicros = pulseMeasurement.timestamp,
                    stable = pulseMeasurement.stable,
                )
                pulseView.text = "Pulse: ${pulseMeasurement.value} bpm"
                statusView.text = "SmartSpectra pulse available"
            }
            if (breathingMeasurement != null) {
                latestBreathingFields = SmartSpectraFieldMapper.MeasurementWithConfidenceFields(
                    value = breathingMeasurement.value,
                    confidence = breathingMeasurement.confidence,
                    timestampMicros = breathingMeasurement.timestamp,
                    stable = breathingMeasurement.stable,
                )
            }

            val pulseFields = latestPulseFields
            val breathingFields = latestBreathingFields
            if (pulseFields != null && breathingFields != null) {
                val pulseTime = pulseFields.timestampMicros
                val breathingTime = breathingFields.timestampMicros
                val timestampRegressed =
                    (lastConsensusPulseTimestampMicros != null && pulseTime < lastConsensusPulseTimestampMicros!!) ||
                    (lastConsensusBreathingTimestampMicros != null && breathingTime < lastConsensusBreathingTimestampMicros!!)
                if (timestampRegressed) {
                    sessionConsensusEvaluator.clear()
                    lastConsensusPulseTimestampMicros = null
                    lastConsensusBreathingTimestampMicros = null
                }
                val isNewPairedSample =
                    (lastConsensusPulseTimestampMicros == null || pulseTime > lastConsensusPulseTimestampMicros!!) &&
                    (lastConsensusBreathingTimestampMicros == null || breathingTime > lastConsensusBreathingTimestampMicros!!)
                if (isNewPairedSample) {
                    lastConsensusPulseTimestampMicros = pulseTime
                    lastConsensusBreathingTimestampMicros = breathingTime
                    val decision = sessionConsensusEvaluator.ingest(pulseFields, breathingFields)
                    when (decision.state) {
                        SmartSpectraConsensusEvaluator.State.COLLECTING -> {
                            statusView.text = "Experimental stability window: collecting"
                        }
                        SmartSpectraConsensusEvaluator.State.RESET -> {
                            statusView.text = "Experimental stability reset; collecting a new window"
                        }
                        SmartSpectraConsensusEvaluator.State.SESSION_STABLE_EXPERIMENTAL -> {
                            val readyPulse = decision.acceptedPulse
                            val readyBreathing = decision.acceptedBreathing
                            if (readyPulse != null && readyBreathing != null) {
                                when (val encoded = SmartSpectraFieldMapper.encodeSdkPair(
                                    pulse = readyPulse,
                                    breathing = readyBreathing,
                                    maxSkewMs = maxLbhPairSkewMs,
                                )) {
                                    is PresageLbhBridge.Result.Accepted -> {
                                        try {
                                            if (encoded.frame.size == 16) {
                                                locallyEncodedFrameCount += 1
                                                lastEncodedPulseTimestampMicros = readyPulse.timestampMicros
                                                lastEncodedBreathingTimestampMicros = readyBreathing.timestampMicros
                                                statusView.text = "Experimental 20-second stability window passed; frame encoded (#$locallyEncodedFrameCount)"
                                            }
                                        } finally {
                                            // No conservar, loguear ni transmitir el payload.
                                            encoded.frame.fill(0)
                                        }
                                    }
                                    is PresageLbhBridge.Result.Rejected -> {
                                        sessionConsensusEvaluator.clear()
                                        lastConsensusPulseTimestampMicros = null
                                        lastConsensusBreathingTimestampMicros = null
                                        statusView.text = "Experimental pair rejected; stability window cleared"
                                    }
                                }
                            } else {
                                sessionConsensusEvaluator.clear()
                                statusView.text = "Experimental stability result incomplete; window cleared"
                            }
                        }
                    }
                }
            }
        }

        sdk.error.observe(this) { error ->
            if (error != null) {
                statusView.text =
                    "Error SmartSpectra: ${error.message}"
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
                "Si continúas, SmartSpectra usará la cámara frontal para procesar señales de pulso y respiración. " +
                    "Esta versión experimental exige una ventana temporal candidata de 20 segundos con pares nuevos de pulso/respiración, antes de codificar una trama LBH fija de 16 bytes en memoria. " +
                    "Solo acepta en el evaluador muestras con stable=true, confidence finita y positiva, timestamps crecientes, diferencia de par de hasta 100 ms y huecos entre pares no mayores que 3 segundos. " +
                    "Los 20 segundos, 100 ms y 3 segundos son parámetros provisionales de ensayo, no umbrales médicos, garantías clínicas ni requisitos de los SDK. " +
                    "Los campos y la trama no se guardan, escriben en logs ni se transmiten mediante esta app; el array de bytes se borra tras codificar. " +
                    "El procesamiento y la telemetría de Presage son independientes; consulta su documentación y política de privacidad. " +
                    "Esta app no desactiva la telemetría del SDK. Puedes cancelar antes de iniciar o detener la medición después. No es un dispositivo médico."
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

    private fun resetLocalLbhState() {
        latestPulseFields = null
        latestBreathingFields = null
        lastEncodedPulseTimestampMicros = null
        lastEncodedBreathingTimestampMicros = null
        lastConsensusPulseTimestampMicros = null
        lastConsensusBreathingTimestampMicros = null
        sessionConsensusEvaluator.clear()
        locallyEncodedFrameCount = 0
    }

    private fun startMeasurement() {
        resetLocalLbhState()
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
        resetLocalLbhState()
        lifecycleScope.launch {
            runCatching { sdk.stop() }
                .onFailure { error ->
                    statusView.text =
                        "No se pudo detener SmartSpectra: ${error.message}"
                }
        }
    }

    override fun onPause() {
        super.onPause()
        resetLocalLbhState()
        lifecycleScope.launch {
            when (sdk.processingStatus.value) {
                ProcessingStatus.RUNNING,
                ProcessingStatus.STARTING,
                ProcessingStatus.STOPPING -> runCatching { sdk.stop() }
                else -> Unit
            }
        }
    }
}
