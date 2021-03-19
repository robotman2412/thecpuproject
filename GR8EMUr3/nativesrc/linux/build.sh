#!/bin/bash

# Compilaton.
g++ -c -fPIC ../common/GR8EMUr3_2.cpp -o build/GR8EMUr3_2.o
g++ -c -fPIC -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux -I../common linuxjni.cpp -o build/linuxjni.o

# Linking.
g++ -shared -fPIC -o build/GR8EMUr3_1.so build/linuxjni.o build/GR8EMUr3_2.o -lc

# Copy output.
cp build/GR8EMUr3_1.so ../../natives/GR8EMUr3_1.so
