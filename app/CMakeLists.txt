cmake_minimum_required(VERSION 3.4.1)
set(CMAKE_VERBOSE_MAKEFILE on)

include_directories(/home/jaycee/workspace/openal/openal-src/openal-soft/include )

add_library(openal SHARED IMPORTED )
set_target_properties(openal PROPERTIES IMPORTED_LOCATION /home/jaycee/AndroidStudioProjects/MarkerFinder/app/src/main/jniLibs/${ANDROID_ABI}/libopenal.so )

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -v -I /home/jaycee/AndroidStudioProjects/MarkerFinder/app/src/main/cpp/include")

add_library( soundGenerator
             SHARED
             /home/jaycee/AndroidStudioProjects/MarkerFinder/app/src/main/cpp/SoundGenerator.cpp )

target_link_libraries( soundGenerator
                       openal
                       log
                       m)

add_library( javaInterface
             SHARED
             /home/jaycee/AndroidStudioProjects/MarkerFinder/app/src/main/cpp/JavaInterface.cpp )

target_link_libraries( # Specifies the target library.
                       javaInterface
                       soundGenerator
                       )
