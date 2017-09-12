#include <JavaInterface.hpp>

static SoundGeneratorSpace::SoundGenerator sound;

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT bool JNICALL
Java_com_activis_jaycee_targetfinder_JNINativeInterface_init(JNIEnv* env, jobject obj)
{
    sound.init();
    sound.startSound();

    return true;
}

JNIEXPORT bool JNICALL
Java_com_activis_jaycee_targetfinder_JNINativeInterface_kill(JNIEnv* env, jobject obj)
{
    sound.endSound();
    sound.kill();

    return true;
}

JNIEXPORT void JNICALL
Java_com_activis_jaycee_targetfinder_JNINativeInterface_play(JNIEnv* env, jobject obj, jfloatArray src, jfloatArray list, jfloat gain, jfloat pitch)
{
    sound.play(env, src, list, gain, pitch);

    return;
}

#ifdef __cplusplus
}
#endif
