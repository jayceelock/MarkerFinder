package com.activis.jaycee.markerfinder

object JNINativeInterface
{
    init {
        System.loadLibrary("javaInterface")
    }

    external fun init(): Boolean
    external fun kill(): Boolean

    external fun play(src: FloatArray, list: FloatArray, gain: Float, pitch: Float)
}
