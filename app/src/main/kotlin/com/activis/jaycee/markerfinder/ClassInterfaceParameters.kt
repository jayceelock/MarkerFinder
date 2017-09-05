package com.activis.jaycee.markerfinder

import android.content.Context

import java.io.Serializable

internal class ClassInterfaceParameters(context: Context) : Serializable {

    private val distHighLimPitch: Float
    private val distLowLimPitch: Float
    private val distHighLimGain: Float
    private val distLowLimGain: Float
    private var pitchHighLim: Float = 0.toFloat()
    private var pitchLowLim: Float = 0.toFloat()
    private var pitchGradient: Float = 0.toFloat()
    private var pitchIntercept: Float = 0.toFloat()
    private var gainHighLim: Float = 0.toFloat()
    private var gainLowLim: Float = 0.toFloat()
    private var gainGradient: Float = 0.toFloat()
    private var gainIntercept: Float = 0.toFloat()
    val distanceThreshold: Float

    var vibrationDelay: Int = 0
    val voiceTiming: Int

    private val activityMain: ActivityMain

    init {
        val PREF_FILE_NAME = context.getString(R.string.pref_file_name)

        val pitchDistHigh = context.getString(R.string.pref_name_pitch_dist_high)
        val pitchDistLow = context.getString(R.string.pref_name_pitch_dist_low)
        val gainDistHigh = context.getString(R.string.pref_name_gain_dist_high)
        val gainDistLow = context.getString(R.string.pref_name_gain_dist_low)
        val pitchHigh = context.getString(R.string.pref_name_pitch_high)
        val pitchLow = context.getString(R.string.pref_name_pitch_low)
        val gainHigh = context.getString(R.string.pref_name_gain_high)
        val gainLow = context.getString(R.string.pref_name_gain_low)
        val vibration = context.getString(R.string.pref_name_vibration_delay)
        val distanceThreshold = context.getString(R.string.pref_name_distance_threshold)
        val voiceTimer = context.getString(R.string.pref_name_voice_timing)

        val prefs = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)

        // If one doesn't exist, none do...not good logic I guess, but I'm lazy
        if (!prefs.contains(context.getString(R.string.pref_name_pitch_high))) {
            val editor = prefs.edit()

            // Fields do not exist yet. Save default values to fields
            editor.putFloat(pitchDistHigh, 4f)
            editor.putFloat(pitchDistLow, -4f)
            editor.putFloat(gainDistHigh, 6f)
            editor.putFloat(gainDistLow, 0f)
            editor.putFloat(pitchHigh, 11f)
            editor.putFloat(pitchLow, 7f)
            editor.putFloat(gainHigh, 1f)
            editor.putFloat(gainLow, 0.5f)
            editor.putInt(vibration, 60)
            editor.putFloat(distanceThreshold, 1.15f)
            editor.putInt(voiceTimer, 5000)

            editor.apply()
        }

        /* Set the only constant: the distance limits */
        this.distHighLimPitch = prefs.getFloat(pitchDistHigh, 4f)
        this.distLowLimPitch = prefs.getFloat(pitchDistLow, -4f)
        this.distHighLimGain = prefs.getFloat(gainDistHigh, 6f)
        this.distLowLimGain = prefs.getFloat(gainDistLow, 0f)

        /* Initialise the parameters to some defaults */
        this.pitchHighLim = prefs.getFloat(pitchHigh, 11f)
        this.pitchLowLim = prefs.getFloat(pitchLow, 7f)
        updatePitchParams(pitchHighLim, pitchLowLim)

        this.gainHighLim = prefs.getFloat(gainHigh, 1f)
        this.gainLowLim = prefs.getFloat(gainLow, 0.5f)
        updateGainParams(gainHighLim, gainLowLim)

        /* Set Vibration params */
        this.vibrationDelay = prefs.getInt(vibration, 60)

        /* Set obstacle detection alert distance threshold */
        this.distanceThreshold = prefs.getFloat(distanceThreshold, 1.15f)

        this.voiceTiming = prefs.getInt(voiceTimer, 5000)

        activityMain = context as ActivityMain
    }

    fun updatePitchParams(highLim: Float, lowLim: Float) {
        pitchHighLim = highLim
        pitchLowLim = lowLim

        pitchGradient = (pitchLowLim - pitchHighLim) / (distLowLimPitch - distHighLimPitch)
        pitchIntercept = pitchLowLim - pitchGradient * distLowLimPitch
    }

    fun updateGainParams(highLim: Float, lowLim: Float) {
        gainHighLim = highLim
        gainLowLim = lowLim

        gainGradient = (gainLowLim - gainHighLim) / (distLowLimGain - distHighLimGain)
        gainIntercept = gainLowLim - gainGradient * distLowLimGain
    }

    fun getPitch(elevation: Double): Float {
        var elevation = elevation
        val pitch: Float

        // Compensate for the Tango's default position being 90deg upright
        elevation -= Math.PI / 2
        if (elevation >= Math.PI / 2) {
            pitch = Math.pow(2.0, pitchLowLim.toDouble()).toFloat()
        } else if (elevation <= -Math.PI / 2) {
            pitch = Math.pow(2.0, pitchHighLim.toDouble()).toFloat()
        } else {
            val gradientAngle = Math.toDegrees(Math.atan((pitchHighLim - pitchLowLim) / Math.PI))

            val grad = Math.tan(Math.toRadians(gradientAngle)).toFloat()
            val intercept = (pitchHighLim - Math.PI / 2 * grad).toFloat()

            pitch = Math.pow(2.0, grad * -elevation + intercept).toFloat()
        }

        return pitch
    }

    fun getPitch(srcX: Double, srcY: Double, listX: Double, listY: Double): Float {
        val diffX = listX - srcX
        val diffY = listY - srcY

        val elevation = Math.atan2(diffY, diffX).toFloat()

        if (elevation >= Math.PI / 2) {
            return Math.pow(2.0, pitchLowLim.toDouble()).toFloat()
        } else if (elevation <= -Math.PI / 2) {
            return Math.pow(2.0, pitchHighLim.toDouble()).toFloat()
        } else {
            val gradientAngle = Math.toDegrees(Math.atan((pitchHighLim - pitchLowLim) / Math.PI))

            val grad = Math.tan(Math.toRadians(gradientAngle)).toFloat()
            val intercept = (pitchHighLim - Math.PI / 2 * grad).toFloat()

            return Math.pow(2.0, (grad * -elevation + intercept).toDouble()).toFloat()
        }
    }

    fun getGain(src: Double, list: Double): Float {
        val diffd = list - src

        // Use absolute difference, because you might end up behind the marker
        val diff = Math.sqrt(diffd * diffd).toFloat()

        return if (diff >= distHighLimGain) {
            gainHighLim
        } else if (diff <= distLowLimGain) {
            gainLowLim
        } else {
            gainGradient * diff + gainIntercept
        }
    }

    fun getGain(distance: Double): Float {
        // Use absolute difference, because you might end up behind the marker
        val diff = Math.sqrt(distance * distance).toFloat()

        return if (diff >= distHighLimGain) {
            gainHighLim
        } else if (diff <= distLowLimGain) {
            gainLowLim
        } else {
            gainGradient * diff + gainIntercept
        }
    }

    val gainLimits: FloatArray
        get() = floatArrayOf(gainLowLim, gainHighLim)
    val pitchLimits: FloatArray
        get() = floatArrayOf(pitchLowLim, pitchHighLim)

    companion object {
        private val TAG = ClassInterfaceParameters::class.java.simpleName
    }
}
