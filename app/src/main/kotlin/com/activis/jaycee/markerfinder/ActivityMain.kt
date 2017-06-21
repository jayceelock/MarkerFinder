package com.activis.jaycee.markerfinder

import android.app.Activity
import android.content.Context
import android.hardware.display.DisplayManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.Display
import com.google.atap.tangoservice.*
import com.google.atap.tangoservice.experimental.TangoImageBuffer
import com.projecttango.tangosupport.TangoSupport
import org.rajawali3d.view.SurfaceView
import java.util.concurrent.atomic.AtomicBoolean

class ActivityMain : Activity()
{
    private val TAG = "ActivityMain.class"
    private val INVALID_TEXTURE_ID: Int = 0

    lateinit internal var surfaceView : SurfaceView
    lateinit internal var renderer : ClassRenderer
    internal var tangoImageBuffer: TangoImageBuffer? = null

    internal var tango: Tango? = null

    internal var tangoIsConnected: Boolean = false
    internal var frameIsAvailable: AtomicBoolean = AtomicBoolean(false)

    internal var displayRotation: Int = 0
    internal var connectedGLThreadID: Int = INVALID_TEXTURE_ID

    internal var RGBTimestamp: Double = 0.0
    internal var cameraPoseTimestamp: Double = 0.0

    override fun onCreate(savedInstate: Bundle?)
    {
        super.onCreate(savedInstate)
        setContentView(R.layout.activity_main)

        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(object : DisplayManager.DisplayListener
        {
            override fun onDisplayAdded(displayId: Int) {}

            override fun onDisplayChanged(displayId: Int)
            {
                synchronized(this@ActivityMain)
                {
                    setDisplayRotation()
                }
            }

            override fun onDisplayRemoved(displayId: Int) {}
        }, null)

        renderer = setupRenderer()
        surfaceView = findViewById(R.id.surfaceview) as SurfaceView
        surfaceView.setSurfaceRenderer(renderer)
    }

    override fun onResume()
    {
        super.onResume()

        surfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        if(!tangoIsConnected)
        {
            tango = Tango(this, Runnable
            {
                    synchronized(this@ActivityMain)
                    {
                        try
                        {
                            var framePairList: ArrayList<TangoCoordinateFramePair> = ArrayList()
                            framePairList.add(TangoCoordinateFramePair(TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE, TangoPoseData.COORDINATE_FRAME_DEVICE))

                            tango?.connectListener(framePairList, ClassTangoUpdateCallback(this@ActivityMain))
                            tango?.experimentalConnectOnFrameListener(TangoCameraIntrinsics.TANGO_CAMERA_COLOR, ClassTangoUpdateCallback(this@ActivityMain))

                            var tangoConfig: TangoConfig? = tango?.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT)
                            tangoConfig?.putBoolean(TangoConfig.KEY_BOOLEAN_COLORCAMERA, true)
                            tangoConfig?.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true)
                            tangoConfig?.putBoolean(TangoConfig.KEY_BOOLEAN_DRIFT_CORRECTION, true)
                            tangoConfig?.putBoolean(TangoConfig.KEY_BOOLEAN_LOWLATENCYIMUINTEGRATION, true)

                            tangoConfig?.putInt(TangoConfig.KEY_INT_DEPTH_MODE, TangoConfig.TANGO_DEPTH_MODE_POINT_CLOUD)

                            tango?.connect(tangoConfig)
                            TangoSupport.initialize(tango)
                            tangoIsConnected = true
                            setDisplayRotation()
                        }
                        catch (e: TangoOutOfDateException)
                        {
                            Log.e(TAG, "Tango core out of date, please update: " + e)
                        }
                        catch (e: TangoErrorException)
                        {
                            Log.e(TAG, "Tango connection error: " + e)
                        }
                    }
            })
        }
    }

    override fun onPause()
    {
        synchronized(this@ActivityMain)
        {
            if(tangoIsConnected)
            {
                tango?.disconnectCamera(TangoCameraIntrinsics.TANGO_CAMERA_COLOR)
                tango?.disconnect()
                tango = null

                tangoIsConnected = false
                connectedGLThreadID = INVALID_TEXTURE_ID
            }
        }
        super.onPause()
    }

    fun setupRenderer() : ClassRenderer
    {
        val renderer = ClassRenderer(this@ActivityMain)

        renderer.currentScene.registerFrameCallback(ClassRajawaliFrameCallback(this@ActivityMain))

        return renderer
    }

    fun setDisplayRotation()
    {
        val display: Display = windowManager.defaultDisplay
        displayRotation = display.rotation

        surfaceView.queueEvent{
            if(tangoIsConnected)
            {
                renderer.updateColorCameraTextureUvGlThread(displayRotation)
            }
        }
    }
}