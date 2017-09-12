#ifndef SOUND_GENERATOR
#define SOUND_GENERATOR

#define SOUNDLOG "SoundGenerator.cpp"
#define NUM_BUFFERS 1//4
#define SOUND_LEN 8
#define SAMPLE_RATE 44100

#include <jni.h>
#include <malloc.h>
#include <math.h>
#include <stdlib.h>
#include <string.h>

#include <android/log.h>

#include <AL/al.h>
#include <AL/alc.h>
#include <AL/alext.h>

namespace SoundGeneratorSpace
{
    class SoundGenerator
    {
    public:
        SoundGenerator();
        ~SoundGenerator();

        // Initialise and kill OpenAL library
        bool init();
        bool kill();

        bool startSound();
        bool endSound();

        // Sound generating functions
        short* generateSoundWave(size_t bufferSize, jfloat pitch, short lastVal, bool onUpSwing);
        void play(JNIEnv* env, jfloatArray src, jfloatArray list, jfloat gain, jfloat pitch);
        void startPlay(jfloat pitch);
        void updatePlay(jfloat pitch);

        // Helper functions
        bool sourcePlaying();

    private:
        ALuint soundSrc;
        ALuint soundBuf[NUM_BUFFERS];
        bool playing = false;
    };
}

#endif