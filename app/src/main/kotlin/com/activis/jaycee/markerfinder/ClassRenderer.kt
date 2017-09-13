/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.activis.jaycee.markerfinder

import com.google.atap.tangoservice.TangoPoseData

import android.content.Context

import android.util.Log
import android.view.MotionEvent
import org.rajawali3d.materials.Material
import org.rajawali3d.materials.textures.ATexture
import org.rajawali3d.materials.textures.StreamingTexture
import org.rajawali3d.math.Matrix4
import org.rajawali3d.math.Quaternion
import org.rajawali3d.primitives.ScreenQuad
import org.rajawali3d.renderer.Renderer

import java.util.HashMap

import javax.microedition.khronos.opengles.GL10

import com.projecttango.tangosupport.TangoSupport

/**
 * Renderer that implements a basic augmented reality scene using Rajawali.
 * It creates a scene with a background using color camera image, and renders the bounding box and
 * three axes of any marker that has been detected in the image.
 */
class ClassRenderer(context: Context) : Renderer(context)
{

    private val textureCoords0 = floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f)

    // Rajawali texture used to render the Tango color camera.
    private lateinit var tangoCameraTexture: ATexture

    // Keeps track of whether the scene camera has been configured.
    var isSceneCameraConfigured: Boolean = false
        private set

    // All markers
    private lateinit var markerObjects: MutableMap<String, ClassMarkerObject>

    private var backgroundQuad: ScreenQuad? = null

    override fun initScene()
    {
        markerObjects = HashMap()

        // Create a quad covering the whole background and assign a texture to it where the
        // Tango color camera contents will be rendered.
        val tangoCameraMaterial = Material()
        tangoCameraMaterial.colorInfluence = 0f

        if(backgroundQuad == null)
        {
            backgroundQuad = ScreenQuad()
            backgroundQuad?.geometry?.setTextureCoords(textureCoords0)
        }

        // We need to use Rajawali's {@code StreamingTexture} since it sets up the texture
        // for GL_TEXTURE_EXTERNAL_OES rendering.
        tangoCameraTexture = StreamingTexture("camera", null as StreamingTexture.ISurfaceListener?)

        try
        {
            tangoCameraMaterial.addTexture(tangoCameraTexture)
            backgroundQuad?.material = tangoCameraMaterial
        }
        catch (e: ATexture.TextureException)
        {
            Log.e(TAG, "Exception creating texture for RGB camera contents", e)
        }

        currentScene.addChildAt(backgroundQuad, 0)
    }

    /**
     * Update marker objects and scene.
     */
    fun updateMarkers(markerList: List<TangoSupport.Marker>)
    {
        if (markerList.isNotEmpty())
        {
            val scene = currentScene

            // Create objects based on new markers
            for (i in markerList.indices)
            {
                val marker = markerList[i]
                marker.translation
                Log.w(TAG, "Marker detected[" + i + "] = " + marker.content)
                // Remove the marker object from scene if it exists.
                val existingObject = markerObjects[marker.content]
                existingObject.let {
                    existingObject?.removeFromScene(scene)
                }

                // Create a new marker object and add it to scene.
                val newObject = ClassMarkerObject(marker)
                markerObjects.put(marker.content, newObject)
                newObject.addToScene(scene)
            }
        }
    }

    /**
     * Update background texture's UV coordinates when device orientation is changed (i.e., change
     * between landscape and portrait mode).
     * This must be run in the OpenGL thread.
     */
    fun updateColorCameraTextureUvGlThread(rotation: Int)
    {
        if(backgroundQuad == null)
        {
            backgroundQuad = ScreenQuad()
        }

        val textureCoords = TangoSupport.getVideoOverlayUVBasedOnDisplayRotation(textureCoords0, rotation)
        backgroundQuad?.geometry?.setTextureCoords(textureCoords, true)
        backgroundQuad?.geometry?.reload()
    }

    /**
     * Update the scene camera based on the provided pose in Tango start of service frame.
     * The camera pose should match the pose of the camera color at the time of the last rendered
     * RGB frame, which can be retrieved with this.getTimestamp();
     *
     *
     * NOTE: This must be called from the OpenGL render thread; it is not thread-safe.
     */
    fun updateRenderCameraPose(cameraPose: TangoPoseData)
    {
        val rotation = cameraPose.rotationAsFloats
        val translation = cameraPose.translationAsFloats
        val quaternion = Quaternion(rotation[3].toDouble(), rotation[0].toDouble(), rotation[1].toDouble(), rotation[2].toDouble())
        // Conjugating the Quaternion is needed because Rajawali uses left-handed convention for
        // quaternions.
        currentCamera.setRotation(quaternion.conjugate())
        currentCamera.setPosition(translation[0].toDouble(), translation[1].toDouble(), translation[2].toDouble())
    }

    /**
     * It returns the ID currently assigned to the texture where the Tango color camera contents
     * should be rendered.
     * NOTE: This must be called from the OpenGL render thread; it is not thread-safe.
     */
    val textureId: Int
        get() = if (tangoCameraTexture == null) -1 else tangoCameraTexture.textureId

    /**
     * We need to override this method to mark the camera for re-configuration (set proper
     * projection matrix) since it will be reset by Rajawali on surface changes.
     */
    override fun onRenderSurfaceSizeChanged(gl: GL10, width: Int, height: Int)
    {
        super.onRenderSurfaceSizeChanged(gl, width, height)
        isSceneCameraConfigured = false
    }

    /**
     * Sets the projection matrix for the scene camera to match the parameters of the color camera,
     * provided by the `TangoCameraIntrinsics`.
     */
    fun setProjectionMatrix(matrixFloats: FloatArray)
    {
        currentCamera.projectionMatrix = Matrix4(matrixFloats)
    }

    override fun onOffsetsChanged(xOffset: Float, yOffset: Float, xOffsetStep: Float, yOffsetStep: Float, xPixelOffset: Int, yPixelOffset: Int) { }

    override fun onTouchEvent(event: MotionEvent) {}

    companion object
    {
        private val TAG = ClassRenderer::class.java.simpleName
    }
}