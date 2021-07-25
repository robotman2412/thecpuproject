
var ws;
var transactions = {};
var runningStartTime = 0;
var runningMaxTime = 0;

let TRANSACTION_TIMEOUT = 7500;

function startCPUControl() {
	startWebSocket();
	setInterval(timeLoop, 100);
}

function startWebSocket() {
	ws = new WebSocket("ws://localhost:5343/cpu_control.ws");
	ws.onopen = wsOnOpen;
	ws.onclose = wsOnClose;
	ws.onerror = wsOnError;
	ws.onmessage = wsOnMessage;
}

function timeLoop() {
	var now = new Date().getTime();
	var remaining;
	if (now > runningStartTime + runningMaxTime) {
		remaining = 0;
	} else {
		remaining = runningMaxTime + runningStartTime - now;
	}
	var strTime = formatMinutes(Math.round(remaining / 1000));
	document.getElementById("program_remaining").innerHTML = escapeHtml(strTime);
}

function wsOnOpen(event) {
	console.log(event);
}

function wsOnClose(event) {
	console.log(event);
	startWebSocket();
}

function wsOnError(event) {
	console.log(event);
}

function wsOnMessage(event) {
	console.log("< " + event.data);
	var data = JSON.parse(event.data);
	var tr = transactions[data.transactionId];
	if (tr) {
		clearTimeout(tr.timer);
		if (data.result == "success") {
			tr.resolve(data);
		} else {
			tr.reject(data);
		}
		delete transactions[data.transactionId];
	} else if (data.transactionId == "broadcast") {
		// TODO: something?
	} else if (data.transactionId) {
		console.warn("Response missing for transaction " + data.transactionId);
	} else {
		console.warn("Response missing transaction ID.");
	}
	switch (data.type) {
		case ("action"):
			handleActionResp(data);
			break;
		case ("info"):
			handleInfoResp(data);
			break;
		default:
			console.warn("Response missing type.");
			break;
	}
}

function handleActionResp(data) {
	
}

function handleInfoResp(data) {
	switch (data.info) {
		case ("running_program"):
			handleRunningProgramInfo(data);
			break;
		case ("queue_content"):
			handleQueueContentInfo(data);
			break;
	}
}

function handleRunningProgramInfo(data) {
	console.log(data);
	var programTitle = document.getElementById("program_title");
	var programAuthor = document.getElementById("program_author");
	programAuthor.innerHTML = "<b>Submitted by </b>" + escapeHtml(data.author);
	programTitle.innerHTML = escapeHtml(data.name);
	runningStartTime = new Date().getTime();
	runningMaxTime = data.duration;
}

function handleQueueContentInfo(data) {
	var queueContentElem = document.getElementById("queue_content");
	var val = "";
	for (i in data.queue) {
		var entry = data.queue[i];
		val = "<br><span>" + escapeHtml(entry.name) + "</span><span class=\"queue-author\">" + escapeHtml(entry.author) + "</span>" + val;
	}
	queueContentElem.innerHTML = val;
}

function randomStringId() {
	let hex = "0123456789ABCDEF";
	var out = "";
	for (i = 0; i < 32; i++) {
		out += hex[Math.floor(Math.random()*16)];
	}
	return out;
}

function wsSend(data) {
	var id = randomStringId();
	data.transactionId = id;
	var str = JSON.stringify(data);
	console.log("> " + str);
	ws.send(str);
	var promise = new Promise((a,b)=>{data.resolve=a;data.reject=b;});
	var timeoutResult = {
		result: "error",
		error: "host_timeout",
		transactionId: id
	};
	data.timer = setTimeout(()=>data.reject(timeoutResult), TRANSACTION_TIMEOUT);
	transactions[id] = data;
	return promise;
}

function cmd(action) {
	wsSend({type: "action", action: action}).then(console.log, console.error);
}

function run() {
	var author = document.getElementById("author_input").value;
	var name = document.getElementById("name_input").value;
	var val = document.getElementById("program_input").value;
	wsSend({
		type: "action",
		action: "run",
		runType: "asm",
		author: author,
		name: name,
		files: {
			"main.asm": val
		}
	}).then(console.log, console.error);
}










