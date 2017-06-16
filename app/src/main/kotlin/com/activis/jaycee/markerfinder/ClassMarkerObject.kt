package com.activis.jaycee.markerfinder

import android.graphics.Color
import android.util.Log
import com.projecttango.tangosupport.TangoSupport
import org.rajawali3d.Object3D
import org.rajawali3d.materials.Material
import org.rajawali3d.math.vector.Vector3
import org.rajawali3d.primitives.Line3D
import org.rajawali3d.primitives.Sphere
import org.rajawali3d.scene.Scene
import java.util.Stack

class ClassMarkerObject(val marker: TangoSupport.Marker)
{
    private val TAG: String = "MarkerObject"

    private var markerCentre: Sphere
    private var markerBorder: Line3D

    internal var markerObject: Object3D

    private val material: Material

    init
    {
        material = Material()
        material.color = Color.RED

        val markerCentreCoords: Vector3 = Vector3(marker.translation[0], marker.translation[1], marker.translation[2])
        val cornerCoordStack: Stack<Vector3> = makeMarkerBorder(marker)

        markerBorder = Line3D(cornerCoordStack, 10.0f)
        markerBorder.material = material

        markerCentre = Sphere(0.1f, 24, 24)
        markerCentre.position = markerCentreCoords
        markerCentre.material = material

        markerObject = Object3D()
    }

    fun makeMarkerBorder(marker: TangoSupport.Marker): Stack<Vector3>
    {
        val markerCornerBottomLeft: Vector3 = Vector3(marker.corners3d[0][0].toDouble(), marker.corners3d[0][1].toDouble(), marker.corners3d[0][2].toDouble())
        val markerCornerBottomRight: Vector3 = Vector3(marker.corners3d[1][0].toDouble(), marker.corners3d[1][1].toDouble(), marker.corners3d[1][2].toDouble())
        val markerCornerTopRight: Vector3 = Vector3(marker.corners3d[2][0].toDouble(), marker.corners3d[2][1].toDouble(), marker.corners3d[2][2].toDouble())
        val markerCornerTopLeft: Vector3 = Vector3(marker.corners3d[3][0].toDouble(), marker.corners3d[3][1].toDouble(), marker.corners3d[3][2].toDouble())

        val cornerCoordStack: Stack<Vector3> = Stack()
        cornerCoordStack.add(markerCornerBottomLeft)
        cornerCoordStack.add(markerCornerBottomRight)
        cornerCoordStack.add(markerCornerBottomRight)
        cornerCoordStack.add(markerCornerTopRight)
        cornerCoordStack.add(markerCornerTopRight)
        cornerCoordStack.add(markerCornerTopLeft)
        cornerCoordStack.add(markerCornerTopLeft)
        cornerCoordStack.add(markerCornerBottomLeft)

        return cornerCoordStack
    }

    fun transformObject()
    {
        markerBorder.scale = Vector3(1/0.14, 1/0.14, 1/0.14)
        markerCentre.position = Vector3(markerCentre.x/0.14, markerCentre.y/0.14, markerCentre.z/0.14)
    }

    fun makeMarker()
    {
        markerObject.addChild(markerBorder)
        markerObject.addChild(markerCentre)
    }

    fun updatePosition(marker: TangoSupport.Marker)
    {
        /* Remove old marker objects before update */
        markerObject.removeChild(markerBorder)
        markerObject.removeChild(markerCentre)

        /* Update individual components' positions */
        markerBorder = Line3D(makeMarkerBorder(marker), 10.0f)
        markerBorder.material = material
        markerCentre.position = Vector3(marker.translation[0], marker.translation[1], marker.translation[2])

        /* Transform their position and place back on parent object */
        transformObject()
        makeMarker()
    }
}