package com.example.notification

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class HydrationSoundType(
    val id: String,
    val title: String,
    val description: String
) {
    ALARM_RING("ALARM_RING", "Alarm Bell Ring", "Loud, unmistakable alarm chime"),
    WATER_DROP("WATER_DROP", "Water Droplet Chime", "Gentle crystal water ripple chime"),
    DIGITAL_BEEP("DIGITAL_BEEP", "Digital Alert Beep", "Crisp triple electronic timer beep"),
    ZEN_CHIME("ZEN_CHIME", "Zen Temple Chime", "Peaceful resonant harmonic chime");

    companion object {
        fun fromId(id: String): HydrationSoundType {
            return entries.firstOrNull { it.id == id } ?: WATER_DROP
        }
    }
}

enum class MedSoundType(
    val id: String,
    val title: String,
    val description: String
) {
    MEDICAL_CHIME("MEDICAL_CHIME", "Medical Chime", "Clear, pleasant 4-tone harmonic reminder chime"),
    ALARM_BELL("ALARM_BELL", "Alarm Bell", "Loud & urgent alarm ringing chime"),
    DIGITAL_BEEP("DIGITAL_BEEP", "Hospital Monitor Alert", "Rhythmic digital beep alert");

    companion object {
        fun fromId(id: String): MedSoundType {
            return entries.firstOrNull { it.id == id } ?: MEDICAL_CHIME
        }
    }
}

enum class SleepSoundType(
    val id: String,
    val title: String,
    val description: String
) {
    GENTLE_ALARM("GENTLE_ALARM", "Gentle Alarm", "Soft rising warm melodic harmonic chimes"),
    SOFT_BELL("SOFT_BELL", "Soft Bell", "Deep soothing resonant acoustic bell strike"),
    CALM_CHIME("CALM_CHIME", "Calm Chime", "Meditative crystal harmonic chimes"),
    NATURE_SOUND("NATURE_SOUND", "Nature Sound", "Peaceful ambient night crickets and gentle stream"),
    CLASSIC_ALARM("CLASSIC_ALARM", "Classic Alarm", "Pleasant melodic morning clock alarm");

    companion object {
        fun fromId(id: String): SleepSoundType {
            return entries.firstOrNull { it.id == id } ?: GENTLE_ALARM
        }
    }
}

class AlarmSoundPlayer(private val context: Context) {

    companion object {
        @Volatile
        private var sharedActivePlayer: AlarmSoundPlayer? = null

        fun stopActiveSound() {
            sharedActivePlayer?.stop()
        }
    }

    private val playerScope = CoroutineScope(Dispatchers.Default)
    private var currentJob: Job? = null
    private var currentAudioTrack: AudioTrack? = null
    private var currentRingtone: Ringtone? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    @Synchronized
    fun play(soundType: String, volume: Float = 1.0f, onFinished: (() -> Unit)? = null) {
        stop()
        sharedActivePlayer = this
        _isPlaying.value = true

        currentJob = playerScope.launch {
            try {
                playSoundOnce(soundType, volume)
            } catch (e: CancellationException) {
                // Expected when playback job is cancelled
            } catch (e: Exception) {
                Log.e("AlarmSoundPlayer", "Error playing sound: $soundType", e)
            } finally {
                _isPlaying.value = false
                onFinished?.invoke()
            }
        }
    }

    @Synchronized
    fun playContinuous(soundType: String, volume: Float = 1.0f, maxDurationSeconds: Int = 45, onFinished: (() -> Unit)? = null) {
        stop()
        sharedActivePlayer = this
        _isPlaying.value = true

        currentJob = playerScope.launch {
            val startTime = System.currentTimeMillis()
            val maxDurationMs = maxDurationSeconds * 1000L
            try {
                while (isActive && (System.currentTimeMillis() - startTime < maxDurationMs)) {
                    playSoundOnce(soundType, volume)
                    delay(800)
                }
            } catch (e: CancellationException) {
                // Expected when continuous sound is stopped or dismissed
            } catch (e: Exception) {
                Log.e("AlarmSoundPlayer", "Error in playContinuous: $soundType", e)
            } finally {
                _isPlaying.value = false
                onFinished?.invoke()
            }
        }
    }

    private suspend fun playSoundOnce(soundType: String, volume: Float = 1.0f) {
        when (soundType) {
            HydrationSoundType.WATER_DROP.id -> {
                playSynthesizedSound(generateWaterDrop(), volume)
            }
            HydrationSoundType.DIGITAL_BEEP.id, MedSoundType.DIGITAL_BEEP.id -> {
                playSynthesizedSound(generateDigitalBeep(), volume)
            }
            HydrationSoundType.ZEN_CHIME.id, SleepSoundType.CALM_CHIME.id -> {
                playSynthesizedSound(generateZenChime(), volume)
            }
            MedSoundType.MEDICAL_CHIME.id -> {
                playSynthesizedSound(generateMedicalChime(), volume)
            }
            MedSoundType.ALARM_BELL.id, HydrationSoundType.ALARM_RING.id -> {
                playSynthesizedSound(generateAlarmRing(), volume)
            }
            SleepSoundType.GENTLE_ALARM.id -> {
                playSynthesizedSound(generateGentleAlarm(), volume)
            }
            SleepSoundType.SOFT_BELL.id -> {
                playSynthesizedSound(generateSoftBell(), volume)
            }
            SleepSoundType.NATURE_SOUND.id -> {
                playSynthesizedSound(generateNatureSound(), volume)
            }
            SleepSoundType.CLASSIC_ALARM.id -> {
                playSynthesizedSound(generateClassicAlarm(), volume)
            }
            else -> {
                playSynthesizedSound(generateGentleAlarm(), volume)
            }
        }
    }

    @Synchronized
    fun stop() {
        currentJob?.cancel()
        currentJob = null

        try {
            currentRingtone?.stop()
            currentRingtone = null
        } catch (e: Exception) {
            // Ignore
        }

        try {
            currentAudioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    stop()
                }
                release()
            }
            currentAudioTrack = null
        } catch (e: Exception) {
            // Ignore
        }

        _isPlaying.value = false
    }

    private suspend fun playSystemRingtone(volume: Float = 1.0f) {
        var ringtone: Ringtone? = null
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            ringtone = RingtoneManager.getRingtone(context, alarmUri)
            currentRingtone = ringtone
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone?.volume = volume.coerceIn(0.05f, 1.0f)
            }
            ringtone?.play()

            // Let system ringtone play for 2.5 seconds
            delay(2500)
            ringtone?.stop()
        } catch (e: Exception) {
            // If system ringtone fails (e.g. headless emulator), fallback to synthetic sound
            playSynthesizedSound(generateAlarmRing(), volume)
        } finally {
            ringtone?.stop()
        }
    }

    private fun playSynthesizedSound(samples: ShortArray, volume: Float = 1.0f) {
        val sampleRate = 44100
        val effectiveVolume = volume.coerceIn(0.05f, 1.0f)
        val scaledSamples = if (effectiveVolume < 0.99f) {
            ShortArray(samples.size) { idx ->
                (samples[idx] * effectiveVolume).toInt().coerceIn(-32768, 32767).toShort()
            }
        } else samples

        val bufferSize = scaledSamples.size * 2

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        currentAudioTrack = audioTrack

        audioTrack.write(scaledSamples, 0, scaledSamples.size)
        try {
            audioTrack.setVolume(effectiveVolume)
        } catch (e: Exception) {
            // Ignore
        }
        audioTrack.play()

        val durationMs = (scaledSamples.size * 1000L) / sampleRate
        Thread.sleep(durationMs.coerceAtLeast(100L))

        try {
            if (audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                audioTrack.stop()
            }
            audioTrack.release()
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Synthesizes an energetic alarm ring: 4 bursts of alternating dual-frequency beeps (880Hz / 1174Hz)
     */
    private fun generateAlarmRing(): ShortArray {
        val sampleRate = 44100
        val burstDurationSec = 0.18f
        val pauseDurationSec = 0.07f
        val burstSamples = (burstDurationSec * sampleRate).toInt()
        val pauseSamples = (pauseDurationSec * sampleRate).toInt()
        val bursts = 4
        val totalSamples = bursts * (burstSamples + pauseSamples)
        val buffer = ShortArray(totalSamples)

        var idx = 0
        for (b in 0 until bursts) {
            val f1 = if (b % 2 == 0) 880.0 else 1046.5
            val f2 = if (b % 2 == 0) 1174.6 else 1318.5

            for (i in 0 until burstSamples) {
                val t = i.toDouble() / sampleRate
                // Envelope with soft attack and decay to avoid clicks
                val env = when {
                    i < 200 -> i / 200.0
                    i > burstSamples - 400 -> (burstSamples - i) / 400.0
                    else -> 1.0
                }
                val wave = (sin(2.0 * PI * f1 * t) * 0.6 + sin(2.0 * PI * f2 * t) * 0.4) * env
                buffer[idx++] = (wave * 30000).toInt().coerceIn(-32768, 32767).toShort()
            }
            for (i in 0 until pauseSamples) {
                buffer[idx++] = 0
            }
        }
        return buffer
    }

    /**
     * Synthesizes a soothing, crystal clear water droplet sound (*plink*)
     * with an initial upward glide and natural exponential decay.
     */
    private fun generateWaterDrop(): ShortArray {
        val sampleRate = 44100
        val totalSec = 1.2f
        val totalSamples = (totalSec * sampleRate).toInt()
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            // Main droplet: starts at 1350 Hz, swoops up to 2100 Hz within 40ms, then decays
            val dropFreq = 1350.0 + 800.0 * (1.0 - exp(-t * 80.0))
            val dropEnv = exp(-t * 12.0)
            val wave1 = sin(2.0 * PI * dropFreq * t) * dropEnv

            // Gentle ripple echo at t = 0.22s
            var wave2 = 0.0
            if (t > 0.22) {
                val t2 = t - 0.22
                val rippleFreq = 1600.0 + 400.0 * (1.0 - exp(-t2 * 60.0))
                val rippleEnv = exp(-t2 * 14.0) * 0.65
                wave2 = sin(2.0 * PI * rippleFreq * t2) * rippleEnv
            }

            // Harmonic chime ring in background (528 Hz)
            val ringEnv = exp(-t * 2.5) * 0.25
            val ringWave = sin(2.0 * PI * 528.0 * t) * ringEnv

            val combined = (wave1 + wave2 + ringWave) * 28000
            buffer[i] = combined.toInt().coerceIn(-32768, 32767).toShort()
        }
        return buffer
    }

    /**
     * Synthesizes 3 rapid crisp digital timer beeps
     */
    private fun generateDigitalBeep(): ShortArray {
        val sampleRate = 44100
        val beepDuration = (0.09f * sampleRate).toInt()
        val pauseDuration = (0.06f * sampleRate).toInt()
        val beeps = 3
        val totalSamples = beeps * (beepDuration + pauseDuration)
        val buffer = ShortArray(totalSamples)

        val freq = 1320.0 // crisp high beep
        var idx = 0
        for (b in 0 until beeps) {
            for (i in 0 until beepDuration) {
                val t = i.toDouble() / sampleRate
                val env = when {
                    i < 150 -> i / 150.0
                    i > beepDuration - 200 -> (beepDuration - i) / 200.0
                    else -> 1.0
                }
                val wave = sin(2.0 * PI * freq * t) * env
                buffer[idx++] = (wave * 29000).toInt().coerceIn(-32768, 32767).toShort()
            }
            for (i in 0 until pauseDuration) {
                buffer[idx++] = 0
            }
        }
        return buffer
    }

    /**
     * Synthesizes a deep, resonant zen temple chime with rich harmonics
     */
    private fun generateZenChime(): ShortArray {
        val sampleRate = 44100
        val totalSec = 2.4f
        val totalSamples = (totalSec * sampleRate).toInt()
        val buffer = ShortArray(totalSamples)

        val baseFreq = 528.0 // Solfeggio "transformation" note
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val decay = exp(-t * 2.2)
            val w1 = sin(2.0 * PI * baseFreq * t) * 0.55
            val w2 = sin(2.0 * PI * (baseFreq * 2.0) * t) * 0.28
            val w3 = sin(2.0 * PI * (baseFreq * 3.01) * t) * 0.17

            val wave = (w1 + w2 + w3) * decay * 28000
            buffer[i] = wave.toInt().coerceIn(-32768, 32767).toShort()
        }
        return buffer
    }

    /**
     * Synthesizes a soothing, crystal clear medical chime:
     * A bright, harmonious 4-note ascending chord sequence (C5, E5, G5, C6) with natural decay
     */
    private fun generateMedicalChime(): ShortArray {
        val sampleRate = 44100
        val noteDuration = 0.22f
        val noteSamples = (noteDuration * sampleRate).toInt()
        val freqs = doubleArrayOf(523.25, 659.25, 783.99, 1046.50) // C5, E5, G5, C6
        val totalSamples = noteSamples * freqs.size + (0.5f * sampleRate).toInt()
        val buffer = ShortArray(totalSamples)

        for ((idx, f) in freqs.withIndex()) {
            val startOffset = idx * noteSamples
            for (i in 0 until (totalSamples - startOffset)) {
                val t = i.toDouble() / sampleRate
                val decay = exp(-t * 3.5)
                val wave = (sin(2.0 * PI * f * t) * 0.7 + sin(2.0 * PI * (f * 2) * t) * 0.3) * decay
                val sampleVal = (wave * 26000).toInt().coerceIn(-32768, 32767).toShort()
                val targetIndex = startOffset + i
                if (targetIndex < totalSamples) {
                    val mixed = (buffer[targetIndex] + sampleVal).coerceIn(-32768, 32767)
                    buffer[targetIndex] = mixed.toShort()
                }
            }
        }
        return buffer
    }

    /**
     * Synthesizes a warm, gentle ascending 4-tone harmonic chime
     */
    private fun generateGentleAlarm(): ShortArray {
        val sampleRate = 44100
        val noteDuration = 0.28f
        val noteSamples = (noteDuration * sampleRate).toInt()
        val freqs = doubleArrayOf(392.00, 523.25, 659.25, 783.99) // G4, C5, E5, G5 warm chord
        val totalSamples = noteSamples * freqs.size + (0.8f * sampleRate).toInt()
        val buffer = ShortArray(totalSamples)

        for ((idx, f) in freqs.withIndex()) {
            val startOffset = idx * noteSamples
            for (i in 0 until (totalSamples - startOffset)) {
                val t = i.toDouble() / sampleRate
                val fadeIn = (i.toDouble() / (sampleRate * 0.04)).coerceAtMost(1.0)
                val decay = exp(-t * 2.8) * fadeIn
                val wave = (sin(2.0 * PI * f * t) * 0.75 + sin(2.0 * PI * (f * 2) * t) * 0.25) * decay
                val sampleVal = (wave * 24000).toInt().coerceIn(-32768, 32767).toShort()
                val targetIndex = startOffset + i
                if (targetIndex < totalSamples) {
                    val mixed = (buffer[targetIndex] + sampleVal).coerceIn(-32768, 32767)
                    buffer[targetIndex] = mixed.toShort()
                }
            }
        }
        return buffer
    }

    /**
     * Synthesizes a deep, soothing resonant acoustic bell strike with exponential decay
     */
    private fun generateSoftBell(): ShortArray {
        val sampleRate = 44100
        val totalSec = 2.2f
        val totalSamples = (totalSec * sampleRate).toInt()
        val buffer = ShortArray(totalSamples)
        val baseFreq = 440.0 // A4 warm bell
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val attack = (i.toDouble() / (sampleRate * 0.015)).coerceAtMost(1.0)
            val decay = exp(-t * 1.8) * attack
            val w1 = sin(2.0 * PI * baseFreq * t) * 0.6
            val w2 = sin(2.0 * PI * (baseFreq * 2.01) * t) * 0.25
            val w3 = sin(2.0 * PI * (baseFreq * 2.98) * t) * 0.15
            val sampleVal = ((w1 + w2 + w3) * decay * 26000).toInt().coerceIn(-32768, 32767)
            buffer[i] = sampleVal.toShort()
        }
        return buffer
    }

    /**
     * Synthesizes peaceful ambient night sound with gentle stream trickle and cricket chirps
     */
    private fun generateNatureSound(): ShortArray {
        val sampleRate = 44100
        val totalSec = 2.5f
        val totalSamples = (totalSec * sampleRate).toInt()
        val buffer = ShortArray(totalSamples)

        var lastRandom = 0.0
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val white = (Math.random() * 2.0 - 1.0)
            lastRandom = (lastRandom * 0.92) + (white * 0.08)
            val streamWave = lastRandom * 0.45

            var cricket = 0.0
            val chirpCycle = (t % 0.8)
            if (chirpCycle in 0.1..0.25) {
                val chirpT = chirpCycle - 0.1
                val chirpEnv = sin(PI * (chirpT / 0.15))
                cricket = sin(2.0 * PI * 4200.0 * t) * chirpEnv * 0.5
            }

            val wave = (streamWave + cricket) * 24000
            buffer[i] = wave.toInt().coerceIn(-32768, 32767).toShort()
        }
        return buffer
    }

    /**
     * Synthesizes pleasant classic melodic morning clock alarm
     */
    private fun generateClassicAlarm(): ShortArray {
        val sampleRate = 44100
        val totalSec = 2.0f
        val totalSamples = (totalSec * sampleRate).toInt()
        val buffer = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val cycle = (t % 0.4)
            if (cycle < 0.15) {
                val freq = if ((t % 0.8) < 0.4) 800.0 else 1000.0
                val env = sin(PI * (cycle / 0.15))
                val wave = sin(2.0 * PI * freq * t) * env * 27000
                buffer[i] = wave.toInt().coerceIn(-32768, 32767).toShort()
            } else {
                buffer[i] = 0
            }
        }
        return buffer
    }

    fun vibratePattern(pattern: LongArray = longArrayOf(0, 300, 200, 300, 200, 500)) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmSoundPlayer", "Vibrate pattern error", e)
        }
    }

    fun vibrate(durationMs: Long = 500L) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(durationMs)
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmSoundPlayer", "Vibrate error", e)
        }
    }
}
