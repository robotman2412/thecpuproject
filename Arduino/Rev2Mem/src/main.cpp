#include <Arduino.h>

#define DATA_START_PIN 11
#define ADDR_START_PIN 3
#define MEM_CLK_PIN 2
#define DATA_OVERRIDE_PIN 19
#define ADDR_OVERRIDE_PIN 20

uint8_t cpuBus; //only and constantly set when clock is low

char hex[16] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
char buffer[1024];
int bufferIndex;

void chkSerial();
void handleMsg();
void overrideData(bool);
void overrideAddr(bool);
void writeByte(int,int);
int readByte(int);
int unhex(const char*);

void setup() {
	Serial.begin(9600);
	overrideData(0);
	overrideAddr(0);
}

void loop() {
	chkSerial();
}

void chkSerial() {
	while (Serial.available()) {
		char c = Serial.read();
		if (c == 2) {
			bufferIndex = 0;
			buffer[0] = 0;
		} else if (bufferIndex != -1) {
			if (c == 3) {
				handleMsg();
				bufferIndex = -1;
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
	Serial.write(2);
	if (strncmp(buffer, "write:", 6) == 0) {
		// Skip the command.
		char *ptr = &buffer[6];
		// Split into parts.
		int x = strchr(ptr, ':') - ptr;
		int y = strrchr(ptr, ':') - ptr;
		// Find start address.
		ptr[x] = 0;
		int startAddr = unhex(ptr);
		// Find length.
		ptr[y] = 0;
		int length = unhex(&ptr[x + 1]);
		// Decode data.
		ptr = &ptr[y + 1];
		y = 0;
		// Set up pins.
		overrideAddr(1);
		overrideData(1);
		// Write most bytes.
		for (int i = 0; i < length - 1; i++) {
			// Find end of byte.
			x = strchr(&ptr[y], ',') - ptr;
			ptr[x] = 0;
			// Find data.
			int data = unhex(&ptr[y]);
			// Write memory.
			writeByte(startAddr + i, data);
			y = x + 1;
		}
		// Find last data byte.
		int data = unhex(&ptr[y]);
		// Write last byte.
		writeByte(startAddr + length - 1, data);
		// Wrap up.
		overrideAddr(0);
		overrideData(0);
		Serial.print("write_ack");
	} else if (strncmp(buffer, "read:", 5) == 0) {
		// Skip the command.
		char *ptr = &buffer[5];
		// Split into parts.
		int x = strchr(ptr, ':') - ptr;
		// Find start address.
		ptr[x] = 0;
		int startAddr = unhex(ptr);
		// Find length.
		int length = unhex(&ptr[x + 1]);
		// Set up pins.
		overrideAddr(1);
		// Read most bytes.
		Serial.print("read_ack:");
		char txBuf[4] = "00,";
		for (int i = 0; i < length - 1; i++) {
			// Read memory.
			int data = readByte(startAddr + i);
			// Transmit.
			txBuf[0] = hex[data >> 4];
			txBuf[1] = hex[data & 15];
			Serial.print(txBuf);
		}
		// Read last byte.
		int data = readByte(startAddr + length - 1);
		// Transmit.
		txBuf[0] = hex[data >> 4];
		txBuf[1] = hex[data & 15];
		txBuf[2] = 0;
		Serial.print(txBuf);
		// Wrap up.
		overrideAddr(0);
	} else {
		Serial.print("unknown_cmd");
	}
	Serial.write(3);
	Serial.flush();
}

void overrideData(bool v) {
	int mode = v ? OUTPUT : INPUT;
	if (!v) digitalWrite(MEM_CLK_PIN, 0);
	pinMode(MEM_CLK_PIN, mode);
	if (!v) digitalWrite(DATA_OVERRIDE_PIN, 0);
	pinMode(DATA_OVERRIDE_PIN, mode);
	if (v) digitalWrite(DATA_OVERRIDE_PIN, 1);
	for (int i = 0; i < 8; i ++) {
		if (!v) digitalWrite(DATA_START_PIN + i, 0);
		pinMode(DATA_START_PIN + i, mode);
	}
}

void overrideAddr(bool v) {
	int mode = v ? OUTPUT : INPUT;
	if (!v) digitalWrite(ADDR_OVERRIDE_PIN, 0);
	pinMode(ADDR_OVERRIDE_PIN, mode);
	if (v) digitalWrite(ADDR_OVERRIDE_PIN, 1);
	for (int i = 0; i < 8; i ++) {
		if (!v) digitalWrite(ADDR_START_PIN + i, 0);
		pinMode(ADDR_START_PIN + i, mode);
	}
}

void writeByte(int addr, int data) {
	// Set data and address lines.
	for (int i = 0; i < 8; i ++) {
		digitalWrite(ADDR_START_PIN + i, addr & 1);
		digitalWrite(DATA_START_PIN + i, data & 1);
		addr >>= 1;
		data >>= 1;
	}
	// Half a millisecond per write, 2KHz is well within spec.
	delayMicroseconds(250);
	digitalWrite(MEM_CLK_PIN, 1);
	delayMicroseconds(250);
	digitalWrite(MEM_CLK_PIN, 0);
}

int readByte(int addr) {
	// Set address lines.
	for (int i = 0; i < 8; i ++) {
		digitalWrite(ADDR_START_PIN + i, addr & 1);
		addr >>= 1;
	}
	// Half a millisecond per read, 2KHz is well within spec.
	delayMicroseconds(500);
	// Read data lines.
	int out = 0;
	for (int i = 7; i >= 0; i --) {
		out <<= 1;
		out |= digitalRead(DATA_START_PIN + i);
	}
	return out;
}

int unhex(const char *msg) {
	int val = 0;
	int len = strlen(msg);
	for (int i = 0; i < len; i++) {
		char c = msg[i];
		val <<= 4;
		if (c >= '0' && c <= '9') {
			val |= c - '0';
		} else if (c >= 'a' && c <= 'f') {
			val |= c - 'a' + 0xa;
		} else if (c >= 'A' && c <= 'F') {
			val |= c - 'A' + 0xa;
		}
	}
	return val;
}
