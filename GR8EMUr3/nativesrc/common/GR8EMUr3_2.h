#pragma once

#include "stdint.h"

#ifdef __cplusplus
extern "C" {
#endif
	
#define TICK_NORMAL 0
#define TICK_STEP_OVER 1
#define TICK_STEP_IN 2
#define TICK_STEP_OUT 3

#define EXC_ERR -1
#define EXC_NORM 0
#define EXC_HALT 1
#define EXC_BRK 2
#define EXC_OVERFLOW 3
#define EXC_NOINSN 4
#define EXC_WAIT_STEP 5
#define EXC_RESET 6
#define EXC_TCON 7
	
#define _C_AIA 1 << 0
#define _C_AIB 1 << 1
#define _C_AIO 1 << 2
#define _C_in_0 1 << 3
#define _C_OPTN0 1 << 4
#define _C_FCY 1 << 5
#define _C_in_1 1 << 6
#define _C_in_2 1 << 7
#define _C_in_3 1 << 8
#define _C_out_0 1 << 9
#define _C_out_1 1 << 10
#define _C_out_2 1 << 11
#define _C_out_3 1 << 12
#define _C_ina_0 1 << 13
#define _C_FRI 1 << 14
#define _C_FCX 1 << 15
#define _C_OPTN2 1 << 16
#define _C_ina_1 1 << 17
#define _C_outa_0 1 << 18
#define _C_outa_1 1 << 19
#define _C_INC 1 << 20
#define _C_outa_2 1 << 21
#define _C_HLT 1 << 22
#define _C_FNMI 1 << 23
#define _C_FIRQ 1 << 24
#define _C_STR 1 << 25
#define _C_OMGWTF 1 << 26
#define _C_ADC 1 << 27
#define _C_OPTN3 1 << 28
#define _C_OPTN1 1 << 29
#define _C_ADRHI 1 << 30
#define _C_RSTB 1 << 31

//      _I_null 0
#define _I_RIA 1
#define _I_RIB 2
#define _I_RIX 3
#define _I_RIY 4
#define _I_IRI 5
#define _I_ISALO 6
#define _I_ISAHI 7
#define _I_STILO 8
#define _I_STIHI 9
#define _I_IST 10
#define _I_FRIB 11
#define _I_INTIL 12
#define _I_INTIH 13
#define _I_ERRIL 14
#define _I_ERRIHI 15

//      _O_null 0
#define _O_ROA 1
#define _O_ROB 2
#define _O_ROX 3
#define _O_ROY 4
#define _O_ILD 5
#define _O_IRO 6
#define _O_COBLO 7
#define _O_COBHI 8
#define _O_STOLO 9
#define _O_STOHI 10
#define _O_ALO 11
#define _O_FROB 12
//      _O_null 13
#define _O_ADROLO 14
#define _O_ADROHI 15

//      _IA_null 0
#define _IA_JMP 1
#define _IA_JBC 2
//      _IA_null 3

#define _OA_PCA 0
#define _OA_ARA 1
#define _OA_STA 2
#define _OA_INTRA 3
#define _OA_ERRA 4
//      _OA_null 5
//      _OA_null 6
//      _OA_null 7

#define CALL_OPCODE 0x02
#define RETURN_OPCODE 0x03

#define MODE_EXEC 0x00
#define MODE_LOAD 0x01
#define MODE_IRQ  0x02
#define MODE_NMI  0x03

#define SKIP_STEP_OVER 0x01
#define SKIP_STEP_OUT 0x02
#define SKIP_STEP_OUT_OVER 0x03

#define MAX_INSN_LEN 16

typedef struct gr8cpurev3_t {
	// ==== FLAGS ====
	bool flagCout, flagZero;				// ALU output flags.
	bool flagIRQ, flagNMI;					// Interrupt enable flags.
	bool flagHWI;							// Interrupt status flags.
	bool wasHWI;							// Set when a hardware interrupt is triggered.
	// ==== BUSSES ====
	uint8_t bus;							// Central data bus.
	uint16_t adrBus;						// Central address bus (before post-processor).
	uint16_t alo;							// ALU out.
	// ==== STATE ====
	uint8_t stage, mode;					// Control unit state.
	int64_t schduledIRQ;					// IRQ scheduled by number of cycles.
	int64_t schduledNMI;					// NMI scheduled by number of cycles.
	// ==== REGISTERS ====
	uint8_t regA, regB, regX, regY, regIR;	// 8-bit registers.
	uint16_t regPC, regAR, stackPtr;		// 16-bit registers.
	uint16_t regIRQ, regNMI;				// Interrupt vectors.
	// ==== DEBUGGER ====
	uint8_t skipping;						// For step out and step over.
	uint16_t skipDepth;						// How deep in methods we are.
	uint16_t *breakpoints;					// Breakpoints.
	uint32_t breakpointsLen;				// Number of breakpoints.
	bool debugIRQ, debugNMI;				// Debugger interrupts.
	// ==== INSTRUCTION SET ====
	uint32_t *isaRom;						// Instruction set ROM.
	uint32_t isaRomLen;						// Length of instruction set ROM.
	// ==== MEMORY ====
	uint8_t *ram;							// Must always be 65536 in size.
	uint8_t *rom;							// Program ROM.
	uint32_t romLen;						// Length of the ROM.
	// ==== STATISTICS ====
	uint64_t numCycles;						// The number of emulated clock cycles.
	uint64_t numInsns;						// The number of emulated instrucitons.
	uint64_t numSubs;						// The number of emulated subroutine calls.
} gr8cpurev3;

extern int gr8cpurev3_tick(gr8cpurev3_t *cpu, int maxTicks, int tickMode);
extern int gr8cpurev3_pretick(gr8cpurev3_t *cpu);
extern int gr8cpurev3_posttick(gr8cpurev3_t *cpu);

#ifdef __cplusplus
}
#endif
