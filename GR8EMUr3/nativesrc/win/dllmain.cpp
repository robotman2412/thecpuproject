// dllmain.cpp : Defines the entry point for the DLL application.
//#include "pch.h"
#include "framework.h"
#include "net_scheffers_robot_emu_GR8CPURev3_1.h"
#include "GR8EMUr3_2.h"
#include "stdlib.h"
#include "stdio.h"

BOOL APIENTRY DllMain( HMODULE hModule,
                       DWORD  ul_reason_for_call,
                       LPVOID lpReserved
                     )
{
    switch (ul_reason_for_call)
    {
    case DLL_PROCESS_ATTACH:
    case DLL_THREAD_ATTACH:
    case DLL_THREAD_DETACH:
    case DLL_PROCESS_DETACH:
        break;
    }
    return TRUE;
}

JNIEnv *currentEnv = NULL;
jobject currentInst = NULL;

uint8_t gr8cpu_mmio_read(uint16_t address, bool notouchy) {
    JNIEnv *env = currentEnv;
    jobject inst = currentInst;
    jclass instanceClass = env->GetObjectClass(inst);

    jmethodID mthRead = env->GetMethodID(instanceClass, "readMMIO", "(IZ)B");

    return env->CallByteMethod(inst, mthRead, (jint) address, (jboolean) notouchy);
}

void gr8cpu_mmio_write(uint16_t address, uint8_t value) {
    JNIEnv *env = currentEnv;
    jobject inst = currentInst;
    jclass instanceClass = env->GetObjectClass(inst);

    jmethodID mthRead = env->GetMethodID(instanceClass, "writeMMIO", "(IB)V");

    env->CallVoidMethod(inst, mthRead, (jint) address, (jbyte) value);
}

JNIEXPORT jint JNICALL Java_net_scheffers_robot_emu_GR8CPURev3_11_nativeTick(JNIEnv *env, jobject inst, jint nCycles, jint tickMode) {
    currentEnv = env;
    currentInst = inst;
    jclass instanceClass = env->GetObjectClass(inst);

    jfieldID fldBus = env->GetFieldID(instanceClass, "bus", "B");
    jfieldID fldAdrBus = env->GetFieldID(instanceClass, "adrBus", "S");

    jfieldID fldRegA = env->GetFieldID(instanceClass, "regA", "B");
    jfieldID fldRegB = env->GetFieldID(instanceClass, "regB", "B");
    jfieldID fldRegX = env->GetFieldID(instanceClass, "regX", "B");
    jfieldID fldRegY = env->GetFieldID(instanceClass, "regY", "B");
    jfieldID fldRegIR = env->GetFieldID(instanceClass, "regIR", "B");

    jfieldID fldRegPC = env->GetFieldID(instanceClass, "regPC", "S");
    jfieldID fldRegAR = env->GetFieldID(instanceClass, "regAR", "S");
    jfieldID fldRegST = env->GetFieldID(instanceClass, "stackPtr", "S");
    jfieldID fldAlo = env->GetFieldID(instanceClass, "alo", "S");

    jfieldID fldFlagCout = env->GetFieldID(instanceClass, "flagCout", "Z");
    jfieldID fldFlagZero = env->GetFieldID(instanceClass, "flagZero", "Z");
    jfieldID fldFlagIRQ = env->GetFieldID(instanceClass, "flagIRQ", "Z");
    jfieldID fldFlagNMI = env->GetFieldID(instanceClass, "flagNMI", "Z");
    jfieldID fldFlagHWI = env->GetFieldID(instanceClass, "flagHWI", "Z");
    
    jfieldID fldStage = env->GetFieldID(instanceClass, "stage", "B");
    jfieldID fldMode = env->GetFieldID(instanceClass, "mode", "B");

    jfieldID fldRom = env->GetFieldID(instanceClass, "rom", "[B");
    jfieldID fldRam = env->GetFieldID(instanceClass, "ram", "[B");

    jfieldID fldBrks = env->GetFieldID(instanceClass, "breakpoints", "[S");
    jfieldID fldSkip = env->GetFieldID(instanceClass, "skipping", "B");
    jfieldID fldSkipDepth = env->GetFieldID(instanceClass, "skipDepth", "B");

    jfieldID fldIsa = env->GetFieldID(instanceClass, "isa", "[I");

    jfieldID fldNumCycles = env->GetFieldID(instanceClass, "numCycles", "J");
    jfieldID fldNumInsns = env->GetFieldID(instanceClass, "numInsns", "J");
    jfieldID fldNumSubs = env->GetFieldID(instanceClass, "numSubs", "J");

    gr8cpurev3 cpu;
    cpu.bus = env->GetByteField(inst, fldBus);
    cpu.adrBus = env->GetShortField(inst, fldAdrBus);

    cpu.regA = env->GetByteField(inst, fldRegA);
    cpu.regB = env->GetByteField(inst, fldRegB);
    cpu.regX = env->GetByteField(inst, fldRegX);
    cpu.regY = env->GetByteField(inst, fldRegY);
    cpu.regIR = env->GetByteField(inst, fldRegIR);

    cpu.regPC = env->GetShortField(inst, fldRegPC);
    cpu.regAR = env->GetShortField(inst, fldRegAR);
    cpu.stackPtr = env->GetShortField(inst, fldRegST);
    cpu.alo = env->GetShortField(inst, fldAlo);

    cpu.stage = env->GetByteField(inst, fldStage);
    cpu.mode = env->GetByteField(inst, fldMode);

    cpu.flagCout = env->GetBooleanField(inst, fldFlagCout);
    cpu.flagZero = env->GetBooleanField(inst, fldFlagZero);
    cpu.flagIRQ = env->GetBooleanField(inst, fldFlagIRQ);
    cpu.flagNMI = env->GetBooleanField(inst, fldFlagNMI);
    cpu.flagHWI = env->GetBooleanField(inst, fldFlagHWI);

    cpu.skipping = env->GetByteField(inst, fldSkip);
    cpu.skipDepth = env->GetShortField(inst, fldSkipDepth);

    jbyteArray ramArr = (jbyteArray) env->GetObjectField(inst, fldRam);
    jbyteArray romArr = (jbyteArray) env->GetObjectField(inst, fldRom);
    jshortArray brksArr = (jshortArray) env->GetObjectField(inst, fldBrks);
    jintArray isaArr = (jintArray) env->GetObjectField(inst, fldIsa);
    
    if (ramArr == NULL || romArr == NULL || brksArr == NULL || isaArr == NULL || env->GetArrayLength(ramArr) < 65536) {
        return EXC_ERR;
    }
    cpu.romLen = env->GetArrayLength(romArr);
    cpu.isaRomLen = env->GetArrayLength(isaArr);
    cpu.breakpointsLen = env->GetArrayLength(brksArr);

    cpu.ram = (uint8_t *) env->GetByteArrayElements(ramArr, NULL);
    cpu.rom = (uint8_t *) env->GetByteArrayElements(romArr, NULL);
    cpu.breakpoints = (uint16_t *) env->GetShortArrayElements(brksArr, NULL);
    cpu.isaRom = (uint32_t *) env->GetIntArrayElements(isaArr, NULL);

    int res = gr8cpurev3_tick(&cpu, nCycles, tickMode);

    env->ReleaseByteArrayElements(ramArr, (jbyte *) cpu.ram, 0);
    env->ReleaseByteArrayElements(romArr, (jbyte *) cpu.rom, 0);
    env->ReleaseShortArrayElements(brksArr, (jshort *) cpu.breakpoints, 0);
    env->ReleaseIntArrayElements(isaArr, (jint *) cpu.isaRom, 0);

    env->SetByteField(inst, fldBus, cpu.bus);
    env->SetShortField(inst, fldAdrBus, cpu.adrBus);

    env->SetByteField(inst, fldRegA, cpu.regA);
    env->SetByteField(inst, fldRegB, cpu.regB);
    env->SetByteField(inst, fldRegX, cpu.regX);
    env->SetByteField(inst, fldRegY, cpu.regY);
    env->SetByteField(inst, fldRegIR, cpu.regIR);

    env->SetShortField(inst, fldRegPC, cpu.regPC);
    env->SetShortField(inst, fldRegAR, cpu.regAR);
    env->SetShortField(inst, fldRegST, cpu.stackPtr);
    env->SetShortField(inst, fldAlo, cpu.alo);
    
    env->SetByteField(inst, fldStage, cpu.stage);
    env->SetByteField(inst, fldMode, cpu.mode);

    env->SetBooleanField(inst, fldFlagCout, cpu.flagCout);
    env->SetBooleanField(inst, fldFlagZero, cpu.flagZero);
    env->SetBooleanField(inst, fldFlagNMI, cpu.flagNMI);
    env->SetBooleanField(inst, fldFlagIRQ, cpu.flagIRQ);
    env->SetBooleanField(inst, fldFlagHWI, cpu.flagHWI);

    env->SetByteField(inst, fldSkip, cpu.skipping);
    env->SetShortField(inst, fldSkipDepth, cpu.skipDepth);

    //_flushall();
    
    return res;
}

