#include <ESP8266WiFi.h>
#include <Ticker.h>
#include <AsyncMqttClient.h>
#include "main.hpp"

#define MQTT_HOST IPAddress(192, 168, 178, 6)
#define MQTT_PORT 1883

#ifdef ESP32
#define MemSerial Serial1
#define ClkSerial Serial2
#else
#include <SoftwareSerial.h>
SoftwareSerial swSerial0;
SoftwareSerial swSerial1;
#define ClkSerial swSerial0
#define MemSerial swSerial1
#endif

char messageBuf[8192];
AsyncMqttClient mqttClient;
Ticker mqttReconnectTimer;

WiFiEventHandler wifiConnectHandler;
WiFiEventHandler wifiDisconnectHandler;
Ticker wifiReconnectTimer;

char clkBuf[1024];
char memBuf[8192];
int clkIndex;
int memIndex;
bool isRunning;

void connectToMqtt() {
	Serial.println("Connecting to MQTT...");
	mqttClient.connect();
}

void connectToWifi() {
	Serial.println("Connecting to Wi-Fi...");
	WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
}

void onWifiConnect(const WiFiEventStationModeGotIP& event) {
	Serial.println("Connected to Wi-Fi.");
	connectToMqtt();
}

void onWifiDisconnect(const WiFiEventStationModeDisconnected& event) {
	Serial.println("Disconnected from Wi-Fi.");
	mqttReconnectTimer.detach();
	wifiReconnectTimer.once(2, connectToWifi);
}

void onMqttConnect(bool sessionPresent) {
	Serial.println("Connected to MQTT.");
	mqttClient.subscribe(CTRL_TOPIC, 2);
	mqttClient.subscribe(RESP_TOPIC, 2);
	mqttClient.publish(RESP_TOPIC, 2, 0, "connect");
	if (!isRunning) {
		mqttClient.publish(RESP_TOPIC, 2, 0, "halt_ack");
	} else {
		mqttClient.publish(RESP_TOPIC, 2, 0, "run_ack");
	}
}

void onMqttDisconnect(AsyncMqttClientDisconnectReason reason) {
	Serial.println("Disconnected from MQTT.");

	if (WiFi.isConnected()) {
		mqttReconnectTimer.once(2, connectToMqtt);
	}
}

void onMqttMessage(char* topic, char* payload, AsyncMqttClientMessageProperties properties, size_t len, size_t index, size_t total) {
    if (payload == NULL) return;
    //printf("Mesg len=%d index=%d total=%d\n", len, index, total);
    if (total >= sizeof(messageBuf)) {
        if (index == 0) {
			printf("Message too long: (%d / %d)\n", len, sizeof(messageBuf));
		}
    } else {
		// Copy to a separate buffer.
        memcpy(&messageBuf[index], payload, len);
		if (index + len < total) {
			// There is more to come.
			return;
		}
		// Yes.
		len = total;
		// Add null terminator.
        messageBuf[len] = 0;
        printf("Mesg (%s): %s\n", topic, messageBuf);
		// Check if it's the right topic.
		if (strcmp(topic, CTRL_TOPIC) != 0) return;
		// Split message into parts.
		String msg(messageBuf);
		int x = msg.indexOf(':');
		x = x >= 0 ? x : msg.length();
		String strCmd = msg.substring(0, x);
		String strArg = msg.substring(x + 1);
		// Find command for message.
		command_t cmd = getCommand(&strCmd);
		if (cmd == HALT) {
			handleHalt(strArg);
		} else if (cmd == RESET) {
			handleReset(strArg);
		} else if (cmd == RUN) {
			handleRun(strArg);
		} else if (cmd == READ) {
			handleRead(strArg);
		} else if (cmd == WRITE) {
			handleWrite(strArg);
		} else if (cmd == POLL_STATUS) {
			handlePollStatus(strArg);
		} else if (cmd == UNKNOWN) {
			String resp("error:");
			resp.concat(strCmd);
			resp.concat(":unknown_command");
			mqttClient.publish(RESP_TOPIC, 2, 0, resp.c_str());
		} else {
			String resp("error:");
			resp.concat(strCmd);
			resp.concat(":unimplemented");
			mqttClient.publish(RESP_TOPIC, 2, 0, resp.c_str());
		}
    }
}

void setup() {
	Serial.begin(115200);
#ifdef ESP32
	ClkSerial.begin(9600)
	MemSerial.begin(19200);
#else
	ClkSerial.begin((uint32_t) 9600, SoftwareSerialConfig::SWSERIAL_8N1, (int8_t) D5, (int8_t) D6);
	MemSerial.begin((uint32_t) 9600, SoftwareSerialConfig::SWSERIAL_8N1, (int8_t) D1, (int8_t) D2);
#endif
	
    putchar('\n');
    putchar('\n');

	wifiConnectHandler = WiFi.onStationModeGotIP(onWifiConnect);
	wifiDisconnectHandler = WiFi.onStationModeDisconnected(onWifiDisconnect);

	mqttClient.onConnect(onMqttConnect);
	mqttClient.onDisconnect(onMqttDisconnect);
	mqttClient.onMessage(onMqttMessage);
	mqttClient.setServer(MQTT_HOST, MQTT_PORT);
	mqttClient.setWill(RESP_TOPIC, 2, 0, "disconnect");
	
	isRunning = 0;
	clkIndex = -1;
	memIndex = -1;
	txClk("halt");
	txClk("reset");
	
	connectToWifi();
}

void loop() {
	while (ClkSerial.available()) {
		char c = ClkSerial.read();
		if (c == 2) {
			clkIndex = 0;
		} else if (clkIndex != -1) if (c == 3) {
			clkBuf[clkIndex] = 0;
			clkIndex = -1;
			rxClk(clkBuf);
			return;
		} else {
			if (clkIndex >= sizeof(clkBuf) - 1) {
				clkIndex = -1;
				return;
			}
			clkBuf[clkIndex ++] = c;
		}
	}
	while (MemSerial.available()) {
		char c = MemSerial.read();
		if (c == 2) {
			memIndex = 0;
		} else if (memIndex != -1) if (c == 3) {
			memBuf[memIndex] = 0;
			rxMem(memBuf);
			return;
		} else {
			if (memIndex >= sizeof(memBuf) - 1) {
				memIndex = -1;
				return;
			}
			memBuf[memIndex ++] = c;
		}
	}
}

void txClk(char *msg) {
	ClkSerial.write(2);
	ClkSerial.print(msg);
	ClkSerial.write(3);
}

void txMem(char *msg) {
	MemSerial.write(2);
	MemSerial.print(msg);
	MemSerial.write(3);
}

void rxClk(char *msg) {
	printf("Rx clk: %s\n", msg);
	if (strcmp(msg, "halt_ack") == 0) {
		mqttClient.publish(RESP_TOPIC, 2, 0, msg);
		isRunning = 0;
	} else if (strcmp(msg, "reset_ack") == 0) {
		mqttClient.publish(RESP_TOPIC, 2, 0, msg);
		isRunning = 0;
	} else if (strcmp(msg, "run_ack") == 0) {
		mqttClient.publish(RESP_TOPIC, 2, 0, msg);
		isRunning = 1;
	} else {
		mqttClient.publish(RESP_TOPIC, 2, 0, ERR_GENERIC ":unknown");
	}
}

void rxMem(char *msg) {
	printf("Rx mem: %s\n", msg);
	if (strncmp(msg, "read_ack", 8) == 0) {
		mqttClient.publish(RESP_TOPIC, 2, 0, msg);
	} else if (strcmp(msg, "write_ack") == 0) {
		mqttClient.publish(RESP_TOPIC, 2, 0, msg);
	} else {
		mqttClient.publish(RESP_TOPIC, 2, 0, ERR_GENERIC ":unknown");
	}
}

void handleHalt(String arg) {
	txClk("halt");
	printf("Halting CPU.\n");
	//mqttClient.publish(RESP_TOPIC, 2, 0, "halt_ack");
}

void handleReset(String) {
	txClk("reset");
	printf("Resetting CPU.\n");
	//mqttClient.publish(RESP_TOPIC, 2, 0, "reset_ack");
}

void handleRun(String) {
	txClk("run");
	printf("Running CPU.\n");
	//mqttClient.publish(RESP_TOPIC, 2, 0, "run_ack");
}

void handleRead(String strArg) {
	int x = strArg.indexOf(':');
	if (x < 0) {
		printf("ERR: Read format invalid: missing colons.\n");
		mqttClient.publish(RESP_TOPIC, 2, 0, ERR_READ ":missing_colons");
		return;
	}
	String addr = strArg.substring(0, x);
	String len = strArg.substring(x + 1);
	if (!verifyHex(&addr) || !verifyHex(&len)) {
		printf("ERR: Read format invalid: invalid HEX.\n");
		mqttClient.publish(RESP_TOPIC, 2, 0, ERR_WRITE ":invalid_hex");
		return;
	}
	int _addr = unhex(&addr);
	int _len = unhex(&len);
	printf("Forwarding read.\n");
	txMem(messageBuf);
}

void handleWrite(String strArg) {
	int x = strArg.indexOf(':');
	int y = strArg.lastIndexOf(':');
	if (x < 0 || y < 0 || x == y) {
		printf("ERR: Write format invalid: missing colons.\n");
		mqttClient.publish(RESP_TOPIC, 2, 0, ERR_WRITE ":missing_colons");
		return;
	}
	String addr = strArg.substring(0, x);
	String len = strArg.substring(x + 1, y);
	String data = strArg.substring(y + 1);
	if (!verifyHex(&addr) || !verifyHex(&len)) {
		printf("ERR: Write format invalid: invalid HEX.\n");
		mqttClient.publish(RESP_TOPIC, 2, 0, ERR_WRITE ":invalid_hex");
		return;
	}
	int _addr = unhex(&addr);
	int _len = unhex(&len);
	y = 0;
	String temp;
	for (int i = 0; i < _len - 1; i++) {
		x = data.indexOf(',', y);
		temp = data.substring(y, x);
		if (!verifyHex(&temp)) {
			printf("ERR: Write format invalid: invalid HEX.\n");
			mqttClient.publish(RESP_TOPIC, 2, 0, ERR_WRITE ":invalid_hex");
			return;
		}
		if (x < 0) {
			printf("ERR: Write format invalid: less data than expected.\n");
			mqttClient.publish(RESP_TOPIC, 2, 0, ERR_WRITE ":too_little_data");
			return;
		}
		y = x + 1;
	}
	if (data.indexOf(',', y) >= 0) {
		printf("ERR: Write format invalid: more data than expected.\n");
		mqttClient.publish(RESP_TOPIC, 2, 0, ERR_WRITE ":too_much_data");
		return;
	}
	temp = data.substring(y);
	if (!verifyHex(&temp)) {
		printf("ERR: Write format invalid: invalid HEX.\n");
		mqttClient.publish(RESP_TOPIC, 2, 0, ERR_WRITE ":invalid_hex");
		return;
	}
	printf("Forwarding write.\n");
	txMem(messageBuf);
	//mqttClient.publish(RESP_TOPIC, 2, 0, "write_ack");
}

void handlePollStatus(String) {
	mqttClient.publish(RESP_TOPIC, 2, 0, "connect");
	if (!isRunning) {
		mqttClient.publish(RESP_TOPIC, 2, 0, "halt_ack");
	} else {
		mqttClient.publish(RESP_TOPIC, 2, 0, "run_ack");
	}
}

command_t getCommand(String *msg) {
	if (msg->equals("halt")) {
		return HALT;
	} else if (msg->equals("reset")) {
		return RESET;
	} else if (msg->equals("run")) {
		return RUN;
	} else if (msg->equals("write")) {
		return WRITE;
	} else if (msg->equals("read")) {
		return READ;
	} else if (msg->equals("poll_status")) {
		return POLL_STATUS;
	} else {
		return UNKNOWN;
	}
}

bool verifyHex(String *msg) {
	for (int i = 0; i < msg->length(); i++) {
		char c = msg->charAt(i);
		if (!(c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F')) {
			return 0;
		}
	}
	return 1;
}

int unhex(String *msg) {
	int val = 0;
	for (int i = 0; i < msg->length(); i++) {
		char c = msg->charAt(i);
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



