#!/bin/bash

# Compilaton.
g++ -c -fPIC ../common/GR8EMUr3_2.cpp -o build/GR8EMUr3_2.o
g++ -c -fPIC -I${JAVA_HOME}/include -I${JAVA_HOME}/include/darwin -I../common linuxjni.cpp -o build/linuxjni.o

# Linking.
g++ -shared -fPIC -o build/lib_GR8EMUr3_1.jnilib build/macjni.o build/GR8EMUr3_2.o -lc

# Copy output.
cp build/lib_GR8EMUr3_1.jnilib ../../natives/lib_GR8EMUr3_1.jnilib
