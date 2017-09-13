package com.activis.jaycee.markerfinder

import android.content.Context
import android.util.Log

import com.google.atap.tangoservice.TangoException
import com.google.atap.tangoservice.TangoPoseData

internal class RunnableSoundGenerator(context: Context) : Runnable
{
    private var tangoPose: TangoPoseData
    private var targetPose: DoubleArray = doubleArrayOf(0.0, 0.0, 0.0)

    private val activityMain: ActivityMain = context as ActivityMain

    init
    {
        tangoPose = TangoPoseData()
    }

    override fun run()
    {
        try
        {
            // val targetPoseVector = mVector(activityMain.getRenderer().getObjectPosition().x, activityMain.getRenderer().getObjectPosition().y, activityMain.getRenderer().getObjectPosition().z)
            val targetPoseVector = mVector(targetPose[0], targetPose[1], targetPose[2])

            val elevationAngle = ClassHelper.getElevationAngle(targetPoseVector, tangoPose)
            val xPositionListener = ClassHelper.getXPosition(targetPoseVector, tangoPose)
            val xPositionSource = targetPose[0]

            Log.d(TAG, String.format("xPos: %f", xPositionListener))

            val tempSrc = FloatArray(3)
            val tempList = FloatArray(3)

            for (i in tangoPose.translation.indices)
            {
                tempSrc[i] = targetPose[i].toFloat()
                tempList[i] = tangoPose.translation[i].toFloat()
            }

            // Get distance to objective, give voice confirm if it's close
            val xDist = targetPose[0] - tangoPose.translation[0]
            val yDist = targetPose[1] - tangoPose.translation[2]
            val zDist = targetPose[2] - tangoPose.translation[1]

            val distanceToObjective = Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist)

            val pitch = activityMain.interfaceParameters.getPitch(elevationAngle)
            val gain = activityMain.interfaceParameters.getGain(distanceToObjective)

            tempSrc[0] = xPositionSource.toFloat()
            tempList[0] = xPositionListener.toFloat()

            /* Update audio properties */
            activityMain.metrics.pitch = pitch
            activityMain.metrics.gain = gain

            JNINativeInterface.play(tempSrc, tempList, gain, pitch)
        }

        catch (e: TangoException)
        {
            Log.e(TAG, "Error getting the Tango pose: " + e)
        }

    }

    fun setTangoPose(tangoPose: TangoPoseData)
    {
        this.tangoPose = tangoPose

        activityMain.metrics.setPoseData(tangoPose)
        activityMain.metrics.setTimestamp(tangoPose.timestamp, true)

        this.run()
    }

    fun setTargetPose(targetPose: DoubleArray)
    {
        this.targetPose = targetPose
        activityMain.metrics.targetPosition = targetPose
    }

    companion object
    {
        private val TAG = RunnableSoundGenerator::class.java.simpleName
    }
}
