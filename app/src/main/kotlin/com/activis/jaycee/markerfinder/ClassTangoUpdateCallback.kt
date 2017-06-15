package com.activis.jaycee.markerfinder

import android.opengl.GLSurfaceView
import android.util.Log
import com.google.atap.tangoservice.Tango
import com.google.atap.tangoservice.TangoCameraIntrinsics
import com.google.atap.tangoservice.TangoErrorException
import com.google.atap.tangoservice.TangoPoseData
import com.google.atap.tangoservice.experimental.TangoImageBuffer
import com.projecttango.tangosupport.TangoSupport

class ClassTangoUpdateCallback(activityMain: ActivityMain): Tango.TangoUpdateCallback(), Tango.OnFrameAvailableListener
{
    private val TAG: String = "TangoUpdateCallback"

    override fun onFrameAvailable(imageBuffer: TangoImageBuffer?, cameraId: Int)
    {
        val oglTcolorPose = TangoSupport.getPoseAtTime(
                activityMain.RGBTimestamp,
                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR,
                TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                TangoSupport.TANGO_SUPPORT_ENGINE_TANGO,
                TangoSupport.ROTATION_IGNORED)

        val markerParam: TangoSupport.MarkerParam = TangoSupport.MarkerParam()
        markerParam.markerSize = 0.14
        markerParam.type = TangoSupport.TANGO_MARKER_ARTAG

        try
        {
            var markerList: List<TangoSupport.Marker> = TangoSupport.detectMarkers(imageBuffer, cameraId, oglTcolorPose.translation, oglTcolorPose.rotation, markerParam)
            Log.d(TAG, String.format("Found marker: %d", markerList.size))
        }
        catch(e: TangoErrorException)
        {
            Log.e(TAG, "Marker error: ", e)
        }
    }

    private val activityMain: ActivityMain = activityMain

    override fun onFrameAvailable(cameraId: Int)
    {
        if (cameraId == TangoCameraIntrinsics.TANGO_CAMERA_COLOR)
        {
            if (activityMain.surfaceView?.renderMode !== GLSurfaceView.RENDERMODE_WHEN_DIRTY)
            {
                activityMain.surfaceView?.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
            }
            activityMain.frameIsAvailable.set(true)
            activityMain.surfaceView?.requestRender()
        }
    }
}