package com.activis.jaycee.markerfinder

import android.app.Activity
import android.os.AsyncTask
import android.util.Log

import com.google.atap.tangoservice.TangoPoseData

import java.io.BufferedReader
import java.io.IOException
import java.io.PrintWriter
import java.io.StringReader
import java.net.Socket

class ClassMetrics
{
    private lateinit var poseData: TangoPoseData

    private var dataStreamer: WifiDataSend = WifiDataSend()
    private var timeStamp: Double = 0.toDouble()

    internal var vibrationIntensity = 0f
        set(intensity)
        {
            field = intensity
        }
    internal var gain = 0f
        set(gain)
        {
            field = gain
        }
    internal var pitch = 0f
        set(pitch)
        {
            field = pitch
        }
    internal var distanceToObstacle = 0.0
        set(dist)
        {
            field = dist
        }
    internal var targetPosition = DoubleArray(3)
        set(target)
        {
            field = target
        }

    companion object
    {
        private val TAG = ClassMetrics::class.java.simpleName

        private val DELIMITER = ","
    }

    /* TODO: Add current interface params */

    /* Trigger CSV update, write to wifi */
    fun setTimestamp(timestamp: Double, transmit: Boolean)
    {
        this.timeStamp = timestamp

        /*
        time - x - y - z - roll - pitch (deg) - yaw - distToObs - vibration - gain - pitch (Hz)
         */

        var csvString = ""

        /* Timestamp */
        csvString += this.timeStamp.toString()
        csvString += DELIMITER

        /* Position */
        csvString += poseData.translation[0].toString()
        csvString += DELIMITER
        csvString += poseData.translation[1].toString()
        csvString += DELIMITER
        csvString += poseData.translation[2].toString()
        csvString += DELIMITER

        /* Rotation */
        csvString += poseData.rotation[0].toString()
        csvString += DELIMITER
        csvString += poseData.rotation[1].toString()
        csvString += DELIMITER
        csvString += poseData.rotation[2].toString()
        csvString += DELIMITER
        csvString += poseData.rotation[3].toString()
        csvString += DELIMITER

        /* Distance to Obstacle */
        csvString += distanceToObstacle.toString()
        csvString += DELIMITER

        /* Vibration intensity */
        csvString += vibrationIntensity.toString()
        csvString += DELIMITER

        /* Audio Gain */
        csvString += gain.toString()
        csvString += DELIMITER

        /* Audio pitch */
        csvString += pitch.toString()
        csvString += DELIMITER

        /* Target position */
        for (i in this.targetPosition.indices)
        {
            csvString += targetPosition[i].toString()
            csvString += DELIMITER
        }

        /* WRITE TO WIFI PORT */
        if (transmit && dataStreamer.status != AsyncTask.Status.RUNNING)
        {
            Log.d(TAG, "wifi transmitting")
            dataStreamer = WifiDataSend()
            dataStreamer.execute(csvString)
        }
    }

    fun setPoseData(pose: TangoPoseData)
    {
        this.poseData = pose
    }

    private inner class WifiDataSend : AsyncTask<String, Void, Void>()
    {
        private val serverIdAddress = "172.23.156.88"
        private val connectionPort = 6666

        override fun doInBackground(vararg strings: String): Void?
        {
            try
            {
                val socket = Socket(serverIdAddress, connectionPort)
                val stream = socket.getOutputStream()
                val writer = PrintWriter(stream)

                var charsRead: Int
                val bufferLen = 1024
                val tempBuffer = CharArray(bufferLen)

                val bufferedReader = BufferedReader(StringReader(strings[0]))

                do
                {
                    charsRead = bufferedReader.read(tempBuffer, 0, bufferLen)
                    writer.print(tempBuffer)
                } while(charsRead != -1)
                writer.write("\n")

                writer.flush()
                writer.close()

                socket.close()
            } catch (e: IOException)
            {
                Log.e(TAG, "Wifi write error: ", e)
            }
            return null
        }
    }
}
