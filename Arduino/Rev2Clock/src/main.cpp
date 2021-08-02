#include <Arduino.h>

#define CU_CLOCK_PIN 3
#define REGC_CLOCK_PIN 4
#define CORE_CLOCK_PIN 2
#define CORE_RST_PIN 5

#define IN_D_PIN 0
#define OUT_D_PIN 0

#define POT_PIN 1 // Analog pin 1, for now.
#define POT_OFF_TRESH 30
#define POT_FAST_TRESH 1004

int BUS_PINS[8] = {
	0, 0, 0, 0, 0, 0, 0, 0
};

boolean prevCoreClk;

uint8_t cpuBus; //only and constantly set when clock is low

char buffer[1024];
int bufferIndex;

//all times in micros
unsigned long lastStateTx;
unsigned long lastRAMSync;
unsigned long lastCUClock;
unsigned long lastCClock;

boolean stateCClock;
boolean stateCUClock;
boolean clockActive;
boolean ctrlClockActive;
boolean msgAvl;
unsigned long clockDelay; //delay in micros

void chkSerial();
void handleMsg();

void setup() {
	Serial.begin(9600);
	prevCoreClk = false;
	pinMode(CU_CLOCK_PIN, OUTPUT);
	pinMode(REGC_CLOCK_PIN, OUTPUT);
	pinMode(CORE_CLOCK_PIN, INPUT);
	pinMode(CORE_RST_PIN, INPUT);
	// pinMode(IN_D_PIN, INPUT);
	// pinMode(OUT_D_PIN, INPUT);
	// for (int i = 0; i < 8; i++) {
	//     pinMode(BUS_PINS[i], INPUT);
	// }
	lastStateTx = 0;
	lastRAMSync = 0;
	lastCUClock = 0;
	lastCClock = 0;
	stateCUClock = false;
	stateCClock = false;
	clockActive = false;
	ctrlClockActive = true;
	clockDelay = 1000000;
	bufferIndex = -1;
	msgAvl = 0;
	digitalWrite(CU_CLOCK_PIN, true);
}

void loop() {
	unsigned long time = micros();
	
	if (time > lastCClock + 1000) { // 1ms
		lastCClock = time;
		stateCClock = !stateCClock;
		digitalWrite(REGC_CLOCK_PIN, stateCClock);
	}
	if (clockDelay > 100000 || !clockActive) {
		chkSerial();
	}
	if (time > lastCUClock + clockDelay && clockActive) {
		lastCUClock = time;
		stateCUClock = !stateCUClock;
		//uint8_t bibble = stateCUClock ? 0x08 : 0x00;
		//PORTD = PORTD & 0xF3 | bibble;
		digitalWrite(CU_CLOCK_PIN, stateCUClock);
		chkSerial();
	}
	else
	{
		uint32_t pot = 1023 - analogRead(POT_PIN);
		if (pot < POT_OFF_TRESH) {
			clockActive = false;
		}
		else if (pot > POT_FAST_TRESH) {
			clockDelay = 20;
			clockActive = ctrlClockActive;
		}
		else
		{
			//clockDelay = 1000000 - 976 * pot;
			clockDelay = 12000000 / (pot + 16) - 11200;
			clockDelay >>= 1;
			clockActive = ctrlClockActive;
		}
	}
}

void chkSerial() {
	if (msgAvl) {
		buffer[bufferIndex] = 0;
		bufferIndex = -1;
		handleMsg();
		return;
	}
	while (Serial.available()) {
		char c = Serial.read();
		if (c == 2) {
			bufferIndex = 0;
			buffer[0] = 0;
		} else if (bufferIndex != -1) {
			if (c == 3) {
				msgAvl = 1;
				return;
			} else {
				if (bufferIndex >= sizeof(buffer) - 1) {
					bufferIndex = -1;
					return;
				}
				buffer[bufferIndex ++] = c;
			}
		}
	}
}

void handleMsg() {
	msgAvl = 0;
	Serial.write(2);
	if (strcmp(buffer, "halt") == 0) {
		ctrlClockActive = 0;
		Serial.print("halt_ack");
	} else if (strcmp(buffer, "run") == 0) {
		ctrlClockActive = 1;
		Serial.print("run_ack");
	} else if (strcmp(buffer, "reset") == 0) {
		stateCUClock = 0;
		ctrlClockActive = 0;
		digitalWrite(CORE_CLOCK_PIN, 0);
		pinMode(CORE_RST_PIN, OUTPUT);
		digitalWrite(CORE_RST_PIN, 1);
		delay(100);
		digitalWrite(CORE_RST_PIN, 0);
		pinMode(CORE_RST_PIN, INPUT);
		Serial.print("reset_ack");
	} else {
		Serial.print("unknown_cmd");
	}
	Serial.write(3);
	Serial.flush();
}
