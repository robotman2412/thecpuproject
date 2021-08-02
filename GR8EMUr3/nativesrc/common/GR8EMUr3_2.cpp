
#include "GR8EMUr3_2.h"
#include "stdlib.h"
#include "stdio.h"


/*

Inputs 8-bit:
 0  null
 1  RIA
 2  RIB
 3  RIX
 4  RIY
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
 4  ROY
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

int gr8cpurev3_tick(gr8cpurev3_t *cpu, int maxTicks, int tickOp) {
	int tickMode = tickOp >> 16;
	int tickArgRts  = tickOp & 0xff;
	int tickArgJsr  = (tickOp >> 8) & 0xff;
	int a; // Return code.
	if (tickMode != TICK_NORMAL && maxTicks < MAX_INSN_LEN) {
		// Ensure there is always enough cycles to complete at least one instruction.
		maxTicks = MAX_INSN_LEN;
	}
	if (cpu->skipping == SKIP_STEP_OVER) {
		// If we're still busy skipping, continue here instead of doing anything else.
		int i = 0;
		do {
			if (i >= maxTicks) {
				cpu->skipping = SKIP_STEP_OVER;
				a = gr8cpurev3_pretick(cpu);
				if (a != EXC_NORM) return a;
				return EXC_TCON;
			}
			a = gr8cpurev3_pretick(cpu);
			if (a != EXC_NORM) return a;
			a = gr8cpurev3_posttick(cpu);
			if (a != EXC_NORM) return a;
			if (cpu->mode == MODE_LOAD && cpu->stage == 0 && (cpu->regIR & 0x7f) == tickArgRts) {
				// Decrement depth after return instruction.
				cpu->skipDepth --;
			}
			if (cpu->mode == MODE_LOAD && cpu->stage == 0 && (cpu->regIR & 0x7f) == tickArgJsr) {
				// Increment depth after return instruction.
				cpu->skipDepth ++;
			}
			i ++;
		} while (cpu->skipDepth > 0);
		cpu->skipping = 0;
	}
	else if(tickMode == TICK_STEP_OVER) {
		/* Step over instruction if it is the specified variant. */
		int i = 0;
		// Finish at least one instruction.
		for (; i < maxTicks; i++) {
			a = gr8cpurev3_pretick(cpu);
			if (a != EXC_NORM) return a;
			a = gr8cpurev3_posttick(cpu);
			if (a != EXC_NORM) return a;
			if (cpu->mode == MODE_LOAD && cpu->stage == 0) {
				// Stop if the instruction has finished.
				break;
			}
		}
		// If the instruction matches the one to skip, execute until the PC matches again.
		if ((cpu->regIR & 0x7f) == tickArgJsr) {
			cpu->skipDepth = 1;
			do {
				if (i >= maxTicks) {
					cpu->skipping = SKIP_STEP_OVER;
					a = gr8cpurev3_pretick(cpu);
					if (a != EXC_NORM) return a;
					return EXC_TCON;
				}
				a = gr8cpurev3_pretick(cpu);
				if (a != EXC_NORM) return a;
				a = gr8cpurev3_posttick(cpu);
				if (a != EXC_NORM) return a;
				if (cpu->mode == MODE_LOAD && cpu->stage == 0 && (cpu->regIR & 0x7f) == tickArgRts) {
					// Decrement depth after return instruction.
					cpu->skipDepth --;
				}
				if (cpu->mode == MODE_LOAD && cpu->stage == 0 && (cpu->regIR & 0x7f) == tickArgJsr) {
					// Increment depth after return instruction.
					cpu->skipDepth ++;
				}
				i ++;
			} while (cpu->skipDepth > 0);
			cpu->skipping = 0;
		}
	}
	else if (tickMode == TICK_STEP_IN) {
		/* Single instruction. */
		for (int i = 0; i < maxTicks; i++) {
			a = gr8cpurev3_pretick(cpu);
			if (a != EXC_NORM) return a;
			a = gr8cpurev3_posttick(cpu);
			if (a != EXC_NORM) return a;
			if (cpu->mode == MODE_LOAD && cpu->stage == 0) {
				// If mode is MODE_LOAD and stage is 0, then an instruction has finished executing.
				break;
			}
		}
	}
	else if (tickMode == TICK_STEP_OUT) {
		/* Run instructions until return instruction is hit. */
		int i = 0;
		cpu->skipDepth = 1;
		do {
			if (i >= maxTicks) {
				cpu->skipping = SKIP_STEP_OVER;
				a = gr8cpurev3_pretick(cpu);
				if (a != EXC_NORM) return a;
				return EXC_TCON;
			}
			a = gr8cpurev3_pretick(cpu);
			if (a != EXC_NORM) return a;
			a = gr8cpurev3_posttick(cpu);
			if (a != EXC_NORM) return a;
			if (cpu->mode == MODE_LOAD && cpu->stage == 0 && (cpu->regIR & 0x7f) == tickArgRts) {
				// Decrement depth after return instruction.
				cpu->skipDepth --;
			}
			if (cpu->mode == MODE_LOAD && cpu->stage == 0 && (cpu->regIR & 0x7f) == tickArgJsr) {
				// Increment depth after return instruction.
				cpu->skipDepth ++;
			}
			i ++;
		} while (cpu->skipDepth > 0);
		cpu->skipping = 0;
	}
	else
	{
		/* Normal tick. */
		for (int i = 0; i < maxTicks; i++) {
			a = gr8cpurev3_pretick(cpu);
			if (a != EXC_NORM) return a;
			a = gr8cpurev3_posttick(cpu);
			if (a != EXC_NORM) return a;
		}
	}
	/* Pretick so that the emulator gets to see the right thing. */
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
		// Indexing.
		address += cpu->regX;
	}
	else if (ctrl & _C_FCY) {
		// Indexing.
		address += cpu->regY;
	}
	if (ctrl & _C_ADC) {
		// Bullshit.
		address ++;
	}
	if (ctrl & _C_OPTN3 && cpu->regIR & 0x80) {
		// PIE.
		address += cpu->regPC;
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
	uint16_t a = (ctrl & _C_FCY) ? cpu->regY : ((ctrl & _C_ADRHI) ? cpu->regX : cpu->regA);
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

uint8_t gr8cpurev3_readflags(gr8cpurev3_t *cpu) {
	return (cpu->flagHWI ? 1 : 0)
		  +(cpu->flagNMI ? 16 : 0)
		  +(cpu->flagIRQ ? 32 : 0)
		  +(cpu->flagZero ? 64 : 0)
		  +(cpu->flagCout ? 128 : 0);
}

void gr8cpurev3_writeflags(gr8cpurev3_t *cpu, uint8_t value) {
	cpu->flagHWI = (value & 0x01) > 0;
	cpu->flagNMI = (value & 0x10) > 0;
	cpu->flagIRQ = (value & 0x20) > 0;
	cpu->flagZero = (value & 0x40) > 0;
	cpu->flagCout = (value & 0x80) > 0;
}

void gr8cpurev3_poll_interrupts(gr8cpurev3_t *cpu) {
	if (cpu->flagNMI) {
		if (cpu->debugNMI) {
			cpu->mode = MODE_NMI;
			cpu->debugNMI = 0;
			cpu->wasHWI = 1;
		} else if (cpu->schduledNMI == 0) {
			cpu->mode = MODE_NMI;
			cpu->schduledNMI = -1;
			cpu->wasHWI = 1;
		}
	}
	if (cpu->flagIRQ) {
		if (cpu->debugIRQ) {
			cpu->mode = MODE_IRQ;
			cpu->debugIRQ = 0;
			cpu->wasHWI = 1;
		} else if (cpu->schduledIRQ == 0) {
			cpu->mode = MODE_IRQ;
			cpu->schduledIRQ = -1;
			cpu->wasHWI = 1;
		}
	}
}

// Gets the state of the bus ready.
int gr8cpurev3_pretick(gr8cpurev3_t *cpu) {
	uint32_t ctrl, ctrlAddr;
	if (cpu->mode == 0) {
		ctrlAddr = ((cpu->regIR & 0x7F) << 4) | cpu->stage;
	}
	else
	{
		ctrlAddr = (cpu->mode << 4) | cpu->stage | (1 << 11);
	}
	if (ctrlAddr >= cpu->isaRomLen) {
		printf("Error: no instruction (preTick, out of bounds).\n");
		printf("addr=%08x, ir=%02x, stage=%02x, mode=%02x\n", ctrlAddr, cpu->regIR, cpu->stage, cpu->mode);
		fflush(stdout);
		return EXC_NOINSN;
	}
	else
	{
		ctrl = cpu->isaRom[ctrlAddr];
		if (ctrl == 0) {
			printf("Error: no instruction (preTick, ctrl is null).\n");
			printf("addr=%08x, ir=%02x, stage=%02x, mode=%02x\n", ctrlAddr, cpu->regIR, cpu->stage, cpu->mode);
			fflush(stdout);
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
			+((ctrl & _C_outa_1) ? 2 : 0)
			+((ctrl & _C_outa_2) ? 4 : 0);
	
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
	case (_OA_INTRA):
		cpu->adrBus = cpu->regIRQ;
		break;
	case (_OA_ERRA):
		cpu->adrBus = cpu->regNMI;
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
	case (_O_ROY):
		cpu->bus = cpu->regY;
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
		cpu->bus = (uint8_t) (cpu->alo & 0xff);
		break;
	case (_O_FROB):
		cpu->bus = gr8cpurev3_readflags(cpu);
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
	uint32_t ctrl, ctrlAddr;
	if (cpu->mode == 0) {
		ctrlAddr = ((cpu->regIR & 0x7F) << 4) | cpu->stage;
	}
	else
	{
		ctrlAddr = (cpu->mode << 4) | cpu->stage | (1 << 11);
	}
	if (ctrlAddr >= cpu->isaRomLen) {
		printf("Error: no instruction (postTick, out of bounds).\n");
		printf("addr=%08x, ir=%02x, stage=%02x, mode=%02x\n", ctrlAddr, cpu->regIR, cpu->stage, cpu->mode);
		fflush(stdout);
		return EXC_NOINSN;
	}
	else
	{
		ctrl = cpu->isaRom[ctrlAddr];
		if (ctrl == 0) {
			printf("Error: no instruction (postTick, ctrl is null).\n");
			printf("addr=%08x, ir=%02x, stage=%02x, mode=%02x\n", ctrlAddr, cpu->regIR, cpu->stage, cpu->mode);
			fflush(stdout);
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
	
	if (ctrl & _C_FIRQ) {
		cpu->flagIRQ = !(ctrl & _C_OPTN0);
	}
	if (ctrl & _C_FNMI) {
		cpu->flagNMI = !(ctrl & _C_OPTN0);
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
	case(_I_RIY):
		cpu->regY = cpu->bus;
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
		gr8cpurev3_writeflags(cpu, cpu->bus);
		break;
	case(_I_INTIL):
		cpu->regIRQ = cpu->bus | (cpu->regIRQ & 0xff00);
		break;
	case(_I_INTIH):
		cpu->regIRQ = (cpu->bus << 8) | (cpu->regIRQ & 0x00ff);
		break;
	case(_I_ERRIL):
		cpu->regNMI = cpu->bus | (cpu->regNMI & 0xff00);
		break;
	case(_I_ERRIHI):
		cpu->regNMI = (cpu->bus << 8) | (cpu->regNMI & 0x00ff);
		break;
	}

	switch (ina) {
	case(_IA_JMP):
		cpu->regPC = gr8cpurev3_find_address(cpu, ctrl);
		break;
	case(_IA_JBC):
		if (gr8cpurev3_branch_condition(cpu, ctrl)) {
			cpu->regPC = gr8cpurev3_find_address(cpu, ctrl);
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
		if (ctrl & _C_OPTN1) {
			cpu->flagZero = (cpu->alo & 0xFF) == 0 && cpu->flagZero;
		}
		else
		{
			cpu->flagZero = (cpu->alo & 0xFF) == 0;
		}
		cpu->flagCout = cpu->alo >> 8;
	}
	cpu->numCycles ++;
	if (cpu->schduledIRQ > 0) cpu->schduledIRQ --;
	if (cpu->schduledNMI > 0) cpu->schduledNMI --;
	if ((ctrl & _C_STR) || (ctrl & _C_OMGWTF && (cpu->regIR & 0x80) == 0)) {
		cpu->stage = 0;
		cpu->flagHWI |= cpu->wasHWI;
		cpu->wasHWI = 0;
		if (cpu->mode) {
			// From load to exec.
			cpu->mode = 0;
		}
		else
		{
			// From exec to load.
			cpu->mode = 1;
			cpu->numInsns ++;
			if (cpu->regIR == RETURN_OPCODE) {
				cpu->numSubs ++;
			}
			// Check for breakpoints before the next instruction is loaded.
			for (uint32_t i = 0; i < cpu->breakpointsLen; i++) {
				if (cpu->regPC == cpu->breakpoints[i]) {
					// Le breakpoint hit.
					return EXC_BRK;
				}
			}
			// Check for interrupts.
			gr8cpurev3_poll_interrupts(cpu);
		}
	}
	else
	{
		cpu->stage ++;
		cpu->stage &= 0xf;
	}
	return EXC_NORM;
}

