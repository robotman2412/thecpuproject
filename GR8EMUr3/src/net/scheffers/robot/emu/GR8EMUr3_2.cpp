
#include "pch.h"
#include "GR8EMUr3_2.h"
#include "stdlib.h"
#include "stdio.h"


/*

Inputs 8-bit:
 0  null
 1  RIA
 2  RIB
 3  RIX
 4  RID
 5  IRI
 6  ISALO
 7  ISAHI
 8  STILO
 9  STIHI
 10 IST
 11 FRIB
 12 INTIL
 13 INTIH
 14 ERRIL
 15 ERRIH

Outputs 8-bit:
 0  null
 1  ROA
 2  ROB
 3  ROX
 4  ROD
 5  ILD
 6  IRO
 7  COBLO
 8  COBHI
 9  STOLO
 10 STOHI
 11 ALO
 12 FROB
 13 ???
 14 ADROLO
 15 ADROHI

Inputs 16-bit:
 0 null
 1 JMP
 2 JBC
 3 null

Outputs 16-bit:
 0 PCA
 1 ARA
 2 STA
 3 null

*/

extern uint8_t gr8cpu_mmio_read(uint16_t address, bool notouchy);
extern void gr8cpu_mmio_write(uint16_t address, uint8_t value);

int gr8cpurev3_tick(gr8cpurev3_t *cpu, int maxTicks) {
	for (int i = 0; i < maxTicks; i++) {
		int a = gr8cpurev3_pretick(cpu);
		if (a != EXC_NORM) return a;
		a = gr8cpurev3_posttick(cpu);
		if (a != EXC_NORM) return a;
	}
	return gr8cpurev3_pretick(cpu);
}

// If notouchy is nonzero, anything that activates on read will not be activated.
// Another read is done in posttick, with notouchy off so as to do stuff.
uint8_t gr8cpurev3_readmem(gr8cpurev3_t *cpu, uint16_t address, bool notouchy) {
	if (address < cpu->romLen) {
		return cpu->rom[address];
	}
	else if ((address & 0xFF00) == 0xFE00) {
		return gr8cpu_mmio_read(address, notouchy);
	}
	else
	{
		return cpu->ram[address];
	}
}

void gr8cpurev3_writemem(gr8cpurev3_t *cpu, uint16_t address, uint8_t value) {
	if (address < cpu->romLen) {
		// You can't write ROM, so we'll write RAM instead.
		cpu->ram[address] = value;
	}
	else if ((address & 0xFF00) == 0xFE00) {
		gr8cpu_mmio_write(address, value);
	}
	else
	{
		cpu->ram[address] = value;
	}
}

uint16_t gr8cpurev3_find_address(gr8cpurev3_t *cpu, int ctrl) {
	uint16_t address = cpu->adrBus;
	if (ctrl & _C_FCX) {
		address += cpu->regX;
	}
	if (ctrl & _C_ADC) {
		address ++;
	}
	return address;
}

int gr8cpurev3_branch_condition(gr8cpurev3_t *cpu, int ctrl) {
	int res = 0;
	int mode = ((ctrl & _C_OPTN0) ? 1 : 0)
			  +((ctrl & _C_OPTN1) ? 2 : 0);
	switch (mode) {
	case(0):
		res = cpu->flagZero;
		break;
	case(1):
		res = !cpu->flagZero && cpu->flagCout;
		break;
	case(2):
		res = !(cpu->flagZero || cpu->flagCout);
		break;
	case(3):
		res = cpu->flagCout;
		break;
	}
	if (ctrl & _C_OPTN2) {
		return !res;
	}
	else
	{
		return res;
	}
}

void gr8cpurev3_do_alu(gr8cpurev3_t *cpu, int ctrl) {
	// Get A and B.
	uint16_t a = (ctrl & _C_ADRHI) ? cpu->regX : cpu->regA;
	uint16_t b = cpu->regB;
	uint16_t cIn = (ctrl & _C_OPTN1) ? (cpu->flagCout ? 1 : 0) : ((ctrl & _C_OPTN2) ? 1 : 0);

	// Invert Le Inputas.
	if (ctrl & _C_AIA) a ^= 0x00ff;
	if (ctrl & _C_AIB) b ^= 0x00ff;

	uint16_t out = 0;
	if (ctrl & _C_OPTN0) {
		if (ctrl & _C_OPTN3) {
			if (ctrl & _C_AIB) {
				// Rotate right.
				out = (a >> 1) | ((a << 7) & 0x80);
			}
			else
			{
				// Rotate left.
				out = (a << 1) | (a >> 7);
			}
		}
		else
		{
			if (ctrl & _C_AIB) {
				// Shift right.
				out = (a >> 1) | (cIn << 7) | ((a << 8) & 0x100);
			}
			else
			{
				// Shift left.
				out = (a << 1) | cIn;
			}
		}
	}
	else
	{
		if (ctrl & _C_ADC) {
			if (ctrl & _C_OPTN3) {
				// Bitwise OR.
				out = a | b;
			}
			else
			{
				// Bitwise XOR.
				out = a ^ b;
			}
		}
		else
		{
			// Add.
			out = a + b + cIn;
		}
	}

	// Do the output thingy.
	if (ctrl & _C_AIO) out ^= 0x00ff;

	cpu->alo = out & 0x01ff;
}

// Gets the state of the bus ready.
int gr8cpurev3_pretick(gr8cpurev3_t *cpu) {
	int ctrl, ctrlAddr;
	if (cpu->mode == 0) {
		ctrlAddr = ((cpu->regIR & 0x7F) << 4) | cpu->stage;
	}
	else
	{
		ctrlAddr = (cpu->mode << 4) | cpu->stage | (1 << 11);
	}
	if (ctrlAddr >= cpu->isaRomLen) {
		printf("Error: no instruction (preTick, out of nounds).\n");
		printf("addr=%08x, ir=%02x, stage=%02x, mode=%02x\n", ctrlAddr, cpu->regIR, cpu->stage, cpu->mode);
		return EXC_NOINSN;
	}
	else
	{
		ctrl = cpu->isaRom[ctrlAddr];
		if (ctrl == 0) {
			printf("Error: no instruction (preTick, ctrl is null).\n");
			printf("addr=%08x, ir=%02x, stage=%02x, mode=%02x\n", ctrlAddr, cpu->regIR, cpu->stage, cpu->mode);
			return EXC_NOINSN;
		}
	}
	
	// Start by getting read / write signals.
	int in = ((ctrl & _C_in_0) ? 1 : 0)
			+((ctrl & _C_in_1) ? 2 : 0)
			+((ctrl & _C_in_2) ? 4 : 0)
			+((ctrl & _C_in_3) ? 8 : 0);

	int out = ((ctrl & _C_out_0) ? 1 : 0)
			+((ctrl & _C_out_1) ? 2 : 0)
			+((ctrl & _C_out_2) ? 4 : 0)
			+((ctrl & _C_out_3) ? 8 : 0);

	int ina = ((ctrl & _C_ina_0) ? 1 : 0)
			+((ctrl & _C_ina_1) ? 2 : 0);

	int outa = ((ctrl & _C_outa_0) ? 1 : 0)
			+((ctrl & _C_outa_1) ? 2 : 0);
	
	if (ctrl & _C_RSTB) {
		cpu->regB = 0;
	}

	// Calc ALU.
	gr8cpurev3_do_alu(cpu, ctrl);

	// Output read stuff to busses.
	// Address bus first, as the data bus may depend on it.
	cpu->adrBus = 0;
	switch (outa) {
	case (_OA_PCA):
		cpu->adrBus = cpu->regPC;
		break;
	case (_OA_ARA):
		cpu->adrBus = cpu->regAR;
		break;
	case (_OA_STA):
		cpu->adrBus = cpu->stackPtr;
		break;
	}

	uint16_t address = gr8cpurev3_find_address(cpu, ctrl);

	cpu->bus = 0;
	switch (out) {
	case (_O_ROA):
		cpu->bus = cpu->regA;
		break;
	case (_O_ROB):
		cpu->bus = cpu->regB;
		break;
	case (_O_ROX):
		cpu->bus = cpu->regX;
		break;
	case (_O_ROD):
		cpu->bus = cpu->regD;
		break;
	case (_O_ILD):
		// Do a no-touchy read.
		cpu->bus = gr8cpurev3_readmem(cpu, address, 1);
		break;
	case (_O_IRO):
		cpu->bus = cpu->regIR;
		break;
	case (_O_COBLO):
		cpu->bus = cpu->regPC & 0x00ff;
		break;
	case (_O_COBHI):
		cpu->bus = (cpu->regPC >> 8) & 0x00ff;
		break;
	case (_O_STOLO):
		cpu->bus = cpu->stackPtr & 0x00ff;
		break;
	case (_O_STOHI):
		cpu->bus = (cpu->stackPtr >> 8) & 0x00ff;
		break;
	case (_O_ALO):
		cpu->bus = cpu->alo;
		break;
	case (_O_FROB):
		//TODO: flags
		break;
	case (_O_ADROLO):
		cpu->bus = cpu->adrBus & 0x00ff;
		break;
	case (_O_ADROHI):
		cpu->bus = (cpu->adrBus >> 8) & 0x00ff;
		break;
	}

	return ctrl == 0 ? EXC_ERR : EXC_NORM;
}

// Applies changes in states.
int gr8cpurev3_posttick(gr8cpurev3_t *cpu) {
	int ctrl, ctrlAddr;
	if (cpu->mode == 0) {
		ctrlAddr = ((cpu->regIR & 0x7F) << 4) | cpu->stage;
	}
	else
	{
		ctrlAddr = (cpu->mode << 4) | cpu->stage | (1 << 11);
	}
	if (ctrlAddr >= cpu->isaRomLen) {
		printf("Error: no instruction (postTick, out of nounds).\n");
		printf("addr=%08x, ir=%02x, stage=%02x, mode=%02x\n", ctrlAddr, cpu->regIR, cpu->stage, cpu->mode);
		return EXC_NOINSN;
	}
	else
	{
		ctrl = cpu->isaRom[ctrlAddr];
		if (ctrl == 0) {
			printf("Error: no instruction (postTick, ctrl is null).\n");
			printf("addr=%08x, ir=%02x, stage=%02x, mode=%02x\n", ctrlAddr, cpu->regIR, cpu->stage, cpu->mode);
			return EXC_NOINSN;
		}
	}

	// Start by getting read / write signals.
	int in = ((ctrl & _C_in_0) ? 1 : 0)
			+((ctrl & _C_in_1) ? 2 : 0)
			+((ctrl & _C_in_2) ? 4 : 0)
			+((ctrl & _C_in_3) ? 8 : 0);

	int out = ((ctrl & _C_out_0) ? 1 : 0)
			+((ctrl & _C_out_1) ? 2 : 0)
			+((ctrl & _C_out_2) ? 4 : 0)
			+((ctrl & _C_out_3) ? 8 : 0);

	int ina = ((ctrl & _C_ina_0) ? 1 : 0)
			+((ctrl & _C_ina_1) ? 2 : 0);

	int outa = ((ctrl & _C_outa_0) ? 1 : 0)
			+((ctrl & _C_outa_1) ? 2 : 0);
	//printf("in=%d, out=%d, ina=%d, outa=%d\n", in, out, ina, outa);
	
	if (ctrl & _C_HLT) {
		return EXC_HALT;
	}

	uint16_t address = gr8cpurev3_find_address(cpu, ctrl);

	if (out == _O_ILD) {
		// Do a touchy read.
		cpu->bus = gr8cpurev3_readmem(cpu, address, 0);
	}

	switch (in) {
	case(_I_RIA):
		cpu->regA = cpu->bus;
		break;
	case(_I_RIB):
		cpu->regB = cpu->bus;
		break;
	case(_I_RIX):
		cpu->regX = cpu->bus;
		break;
	case(_I_RID):
		cpu->regD = cpu->bus;
		break;
	case(_I_IRI):
		cpu->regIR = cpu->bus;
		break;
	case(_I_ISALO):
		cpu->regAR = cpu->bus | (cpu->regAR & 0xff00);
		break;
	case(_I_ISAHI):
		cpu->regAR = (cpu->bus << 8) | (cpu->regAR & 0x00ff);
		break;
	case(_I_STILO):
		cpu->stackPtr = cpu->bus | (cpu->stackPtr & 0xff00);
		break;
	case(_I_STIHI):
		cpu->stackPtr = (cpu->bus << 8) | (cpu->stackPtr & 0x00ff);
		break;
	case(_I_IST):
		gr8cpurev3_writemem(cpu, address, cpu->bus);
		break;
	case(_I_FRIB):
		// TODO: flags
		break;
	case(_I_INTIL):
		// TODO: interrupt vector in low
		break;
	case(_I_INTIH):
		// TODO: interrupt vector in high
		break;
	case(_I_ERRIL):
		// TODO: error vector in low
		break;
	case(_I_ERRIHI):
		// TODO: error vector in high
		break;
	}

	switch (ina) {
	case(_IA_JMP):
		cpu->regPC = cpu->adrBus;
		break;
	case(_IA_JBC):
		if (gr8cpurev3_branch_condition(cpu, ctrl)) {
			cpu->regPC = cpu->adrBus;
		}
		break;
	}

	if (ctrl & _C_INC) {
		if (ctrl & _C_OPTN0) {
			if (ctrl & _C_ADRHI) {
				if ((cpu->stackPtr & 0xff) == 0x00) {
					return EXC_OVERFLOW;
				}
				cpu->stackPtr --;
			}
			else
			{
				if ((cpu->stackPtr & 0xff) == 0xff) {
					return EXC_OVERFLOW;
				}
				cpu->stackPtr ++;
			}
		}
		else
		{
			cpu->regPC ++;
		}
	}
	if (ctrl & _C_FRI) {
		cpu->flagZero = (cpu->alo & 0xFF) == 0;
		cpu->flagCout = cpu->alo >> 8;
	}
	if (ctrl & _C_STR) {
		cpu->stage = 0;
		cpu->mode = (cpu->mode & 0x1) ^ 0x1;
	}
	else
	{
		cpu->stage ++;
		cpu->stage &= 0xf;
	}
	return EXC_NORM;
}

