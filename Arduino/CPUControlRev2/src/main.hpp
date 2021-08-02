
#ifndef MAIN_HPP
#define MAIN_HPP

#include <WString.h>

#define WIFI_SSID "callisto"
#define WIFI_PASSWORD "deventerschans_19"

#define CTRL_TOPIC "cpu_ctrl_r2"
#define RESP_TOPIC "cpu_resp_r2"

#define ERR_GENERIC "error"
#define ERR_HLT     ERR_GENERIC ":halt"
#define ERR_RESET   ERR_GENERIC ":reset"
#define ERR_READ    ERR_GENERIC ":read"
#define ERR_WRITE   ERR_GENERIC ":write"
#define ERR_RUN     ERR_GENERIC ":run"

enum command_t {
	UNKNOWN,
	HALT,
    RESET,
	RUN,
	READ,
	WRITE,
    POLL_STATUS
};

command_t getCommand(String*);

void handleHalt(String);
void handleReset(String);
void handleRun(String);
void handleRead(String);
void handleWrite(String);
void handlePollStatus(String);
//void handle(String);

void txClk(char*);
void txMem(char*);
void rxClk(char*);
void rxMem(char*);

bool verifyHex(String*);
int unhex(String*);

#endif
