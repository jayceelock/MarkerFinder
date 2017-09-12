#ifndef SOUND_H_
#define SOUND_H_

#include <jni.h>
#include <android/log.h>

#include "SoundGenerator.hpp"

#define APPNAME "sound.cpp"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT bool JNICALL Java_com_activis_jaycee_targetfinder_JNINativeInterface_init(JNIEnv* env, jobject obj);
JNIEXPORT bool JNICALL Java_com_activis_jaycee_targetfinder_JNINativeInterface_kill(JNIEnv* env, jobject obj);

JNIEXPORT void JNICALL Java_com_activis_jaycee_targetfinder_JNINativeInterface_play(JNIEnv* env, jobject obj, jfloatArray src, jfloatArray list, jfloat gain, jfloat pitch);

#ifdef __cplusplus
}
#endif

#endif