package net.scheffers.robot.cpucontrol;

import jutils.IOUtils;
import jutils.JUtils;
import net.scheffers.robot.hyperasm.AssemblerCore;
import net.scheffers.robot.hyperasm.Pass0Out;
import net.scheffers.robot.hyperasm.Pass1Out;
import net.scheffers.robot.hyperasm.Pass2Out;
import net.scheffers.robot.hyperasm.exception.CompilerError;
import net.scheffers.robot.hyperasm.exception.CompilerWarning;
import net.scheffers.robot.hyperasm.importing.ImportSupplier;
import net.scheffers.robot.hyperasm.isa.InstructionSet;
import org.eclipse.paho.client.mqttv3.*;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class CPUControlR2 {
	
	public static MqttAsyncClient mqtt;
	
	public static Map<String, Consumer<String>> commandMap;
	public static String path;
	public static Pass2Out lastAssembled;
	public static byte[] lastOutput;
	
	public static InstructionSet isa;
	public static boolean sendingCommand;
	public static IMqttActionListener connectListener;
	public static MqttCallback mqttCallback;
	
	public static final int WEBSOCKET_PORT = 5343;
	public static final String CPU_CONTROL_TOPIC = "cpu_ctrl_r2";
	public static final String CPU_RESPOND_TOPIC = "cpu_resp_r2";
	
	public static CPUDaemon cpu;
	public static WebSocketDaemon wsServer;
	public static ExecutorService executor;
	
	public static LinkedBlockingQueue<QueueEntry> queue;
	public static boolean isQueueDirty;
	public static long defaultRunTime = 15 * 1000;
	public static long currentRunStart;
	public static long currentRunTime;
	
	public static byte[] loadJarBytes(String resource) {
		try {
			InputStream stream = CPUControlR2.class.getClassLoader().getResourceAsStream("/rsrc/" + resource);
			if (stream == null) {
				throw new FileNotFoundException(resource);
			}
			byte[] heck = new byte[stream.available()];
			int n = stream.read(heck);
			return heck;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static void main(String[] args) {
		executor = Executors.newCachedThreadPool();
		cpu = new CPUDaemon();
		wsServer = new WebSocketDaemon(WEBSOCKET_PORT);
		queue = new LinkedBlockingQueue<>();
		
		// D:\logisim projects\GR8CPU Rev2.1\programs\ALU test.asm
		
		connectListener = new IMqttActionListener() {
			@Override
			public void onSuccess(IMqttToken iMqttToken) {
				System.out.println("MQTT connected");
				System.out.print("CpuControlR2> ");
				try {
					mqtt.setCallback(mqttCallback);
					mqtt.subscribe(CPU_RESPOND_TOPIC, 2);
					cpu.pollStatus();
				} catch (MqttException e) {
					e.printStackTrace();
				}
			}
			
			@Override
			public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
				System.err.println("could not connect MQTT");
				try {
					mqtt.connect(0, this);
				} catch (MqttException e) {
					e.printStackTrace();
				}
			}
		};
		mqttCallback = new MqttCallback() {
			@Override
			public void connectionLost(Throwable a) {
				try {
					mqtt.connect(0, connectListener);
				} catch (MqttException e) {
					e.printStackTrace();
					System.err.println("could not connect MQTT");
					System.exit(1);
				}
			}
			
			@Override
			public void messageArrived(String a, MqttMessage b) throws Exception {
				CPUControlR2.onMessage(a, b);
			}
			
			@Override
			public void deliveryComplete(IMqttDeliveryToken a) {
			}
		};
		try {
			mqtt = new MqttAsyncClient("tcp://192.168.178.6:1883", "CpuControlR2");
			mqtt.connect(0, connectListener);
		} catch (MqttException e) {
			e.printStackTrace();
			System.err.println("could not connect MQTT");
		}
		
		path = System.getProperty("user.dir");
		if (!path.endsWith("/")) path += '/';
		
		try {
			isa = new InstructionSet(new JSONObject(new String(IOUtils.readBytes("D:\\logisim projects\\GR8CPU Rev2.1\\IS\\ISAr2_1.json"))));
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		commandMap = new HashMap<>();
		commandMap.put("?", CPUControlR2::help);
		commandMap.put("help", CPUControlR2::help);
		commandMap.put("as", CPUControlR2::asm);
		commandMap.put("asm", CPUControlR2::asm);
		commandMap.put("run", CPUControlR2::run);
		commandMap.put("halt", CPUControlR2::halt);
		commandMap.put("cont", CPUControlR2::cont);
		commandMap.put("time", CPUControlR2::time);
		commandMap.put("skip", CPUControlR2::skip);
		commandMap.put("clear", CPUControlR2::clear);
		commandMap.put("raw", CPUControlR2::raw);
		commandMap.put("exit", CPUControlR2::exit);
		
		executor.execute(CPUControlR2::queueLoop);
		wsServer.start();
		
		while (true) {
			String line;
			try {
				line = JUtils.awaitLine("CpuControlR2> ").trim().replaceAll("\\s+", " ");
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			try {
				int i = line.indexOf(' ');
				String cmd = line;
				String arg = "";
				if (i > 0) {
					cmd = line.substring(0, i);
					arg = line.substring(i + 1);
				}
				Consumer<String> command = commandMap.get(cmd);
				if (command == null) {
					help("");
				} else {
					command.accept(arg);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public static void onMessage(String topic, MqttMessage message) {
		if (CPU_RESPOND_TOPIC.equals(topic)) {
			executor.execute(() -> cpu.onMessage(message));
		}
	}
	
	public static void sendMessage(String topic, String message) {
		try {
			mqtt.publish(topic, new MqttMessage(message.getBytes(StandardCharsets.UTF_8)));
		} catch (MqttException e) {
			throw new RuntimeException(e);
		}
	}
	
	//region commands
	public static void help(String ignored) {
		System.out.println("Available commands:");
		System.out.println("as (-v) [file]          Assemble file. -v: verbose assembly");
		System.out.println("run                     Run assembled file on CPU.");
		System.out.println("halt                    Halts the CPU without removing the program.");
		System.out.println("cont                    Continues the CPU without resetting.");
		System.out.println("time                    Gets or sets the amount of time per program.");
		System.out.println("skip                    Skips the current program, if there is anything in the queue.");
		System.out.println("clear                   Clears the queue, without skipping the current program.");
		//System.out.println("enable                  Enable online CPU control.");
		//System.out.println("disable                 Disable online CPU control.");
		System.out.println("dump ([start] [end])    Create a dump of the CPU's memory and ROM.");
		//System.out.println("stats                   Print out various statistics.");
		System.out.println("raw                     Sends a raw command to the CPU.");
		System.out.println("exit                    Quit.");
		//System.out.println("                        ");
	}
	
	public static void asm(String args) {
		boolean verbose = false;
		if (args.startsWith("-v ")) {
			verbose = true;
			args = args.substring(3);
		}
		lastOutput = null;
		try {
			lastAssembled = AssemblerCore.simpleFullAssemble(args, isa);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if (lastAssembled.warnings.size() > 0) {
			for (CompilerWarning warning : lastAssembled.warnings) {
				System.err.println(warning.source + ":" + warning.line);
				System.err.println(warning.getMessage());
			}
		}
		if (lastAssembled.errors.size() > 0) {
			for (CompilerError error : lastAssembled.errors) {
				System.err.println(error.source + ":" + error.line);
				System.err.println(error.getMessage());
			}
		} else {
			int offs = lastAssembled.removePrefixPadding ? (int) lastAssembled.paddingLength : 0;
			lastOutput = new byte[(int) lastAssembled.totalLength - offs];
			for (int i = 0; i < lastOutput.length; i++) {
				lastOutput[i] = (byte) lastAssembled.wordsOut[i + offs];
			}
			System.out.printf("Program size: %d / 256 bytes (%d%%)\n", lastAssembled.totalLength, (int) (lastAssembled.totalLength / 2.56));
		}
	}
	
	public static void run(String args) {
		if (lastOutput == null) {
			System.out.println("No output to write.");
		} else if (!sendingCommand) {
			System.out.println("Running program...");
			//System.out.print("Halting CPU...     ");
			//boolean res = cpu.halt().join();
			//if (res) System.out.println("OK");
			//else { System.out.println(cpu.lastError); return; }
			System.out.print("Resetting...       ");
			boolean res = cpu.reset().join();
			if (res) System.out.println("OK");
			else {
				System.out.println(cpu.lastError);
				return;
			}
			System.out.print("Writing...         ");
			res = cpu.write(0, lastOutput).join();
			if (res) System.out.println("OK");
			else {
				System.out.println(cpu.lastError);
				return;
			}
			System.out.print("Running...         ");
			res = cpu.run().join();
			if (res) System.out.println("OK");
			else {
				System.out.println(cpu.lastError);
			}
		}
	}
	
	public static void halt(String s) {
		System.out.print("Halting CPU...     ");
		boolean res = cpu.halt().join();
		if (res) System.out.println("OK");
		else {
			System.out.println(cpu.lastError);
		}
	}
	
	public static void time(String s) {
		if (s.length() == 0) {
			int sec = (int) (defaultRunTime / 1000);
			int min = sec / 60;
			sec %= 60;
			int hrs = min / 60;
			min %= 60;
			System.out.printf("Default run time is %02d:%02d:%02d\n", hrs, min, sec);
		} else if (s.matches("([0-9]+:){0,2}[0-9]+")) {
			String[] ohWell = s.split(":");
			long time = 0;
			for (String wow : ohWell) {
				time *= 60;
				time += Integer.parseInt(wow);
			}
			defaultRunTime = time * 1000;
		} else {
			System.out.println("Invalid time format.");
		}
	}
	
	public static void skip(String s) {
		if (currentRunTime >= 200) {
			currentRunTime = 0;
		}
	}
	
	public static void clear(String s) {
		synchronized (queue) {
			queue.clear();
			sendQueueUpdate();
		}
	}
	
	public static void cont(String s) {
		System.out.print("Continuing CPU...  ");
		boolean res = cpu.run().join();
		if (res) System.out.println("OK");
		else {
			System.out.println(cpu.lastError);
		}
	}
	
	public static void exit(String args) {
		System.exit(0);
	}
	
	public static void raw(String args) {
		sendMessage(CPU_CONTROL_TOPIC, args);
	}
	//endregion commands
	
	public static CompletableFuture<String> runBin(byte[] binary) {
		AsyncPlanner<String> planner = new AsyncPlanner<>();
		planner.plan((x, p) -> {
			if ((boolean) x) p.next(cpu.write(0, binary));
			else p.complete(cpu.lastError);
		}).plan((x, p) -> {
			if ((boolean) x) p.next(cpu.run());
			else p.complete(cpu.lastError);
		}).plan((x, p) -> {
			if ((boolean) x) p.complete(null);
			else p.complete(cpu.lastError);
		}).exceptionally((x) -> {
			x.printStackTrace();
			return x.getMessage();
		});
		return planner.start(cpu.reset());
	}
	
	public static CompletableFuture<String> runEntry(QueueEntry entry) {
		CompletableFuture<String> future = runBin(entry.binary);
		JSONObject obj = new JSONObject();
		obj.put("type", "info");
		obj.put("info", "running_program");
		obj.put("duration", entry.runTime);
		obj.put("author", entry.author);
		obj.put("name", entry.name);
		obj.put("transactionId", "broadcast");
		future.thenAccept((s) -> {
			if (s != null) {
				System.err.println("Error running: " + s);
			}
			wsServer.broadcast(obj.toString());
		});
		return future;
	}
	
	public static void queueLoop() {
		while (true) {
			try {
				QueueEntry entry;
				synchronized (queue) {
					if (queue.isEmpty()) {
						Thread.yield();
						continue;
					}
					// This blocks if empty.
					entry = queue.take();
					sendQueueUpdate();
				}
				runEntry(entry).join();
				currentRunStart = System.currentTimeMillis();
				currentRunTime = entry.runTime;
				while (System.currentTimeMillis() < currentRunStart + currentRunTime) {
					Thread.yield();
				}
			} catch (InterruptedException ignored) {
				
			}
		}
	}
	
	public static void addToQueue(QueueEntry entry) {
		synchronized (queue) {
			queue.add(entry);
			sendQueueUpdate();
		}
	}
	
	public static void sendQueueUpdate() {
		JSONArray queueArr = new JSONArray();
		for (QueueEntry entry : queue) {
			queueArr.put(entry.toJSONObject());
		}
		JSONObject obj = new JSONObject();
		obj.put("type", "info");
		obj.put("info", "queue_content");
		obj.put("transactionId", "broadcast");
		obj.put("queue", queueArr);
		wsServer.broadcast(obj.toString());
	}
	
	protected static class CPUDaemon {
		public boolean isOnline;
		public boolean isHalted;
		public boolean runningType;
		public String runningName;
		
		public String lastError = null;
		public CPUPendingAction pendingAction;
		public CompletableFuture<Boolean> future;
		public byte[] pendingWrite;
		public int pendingWriteIndex;
		public int pendingWriteAddress;
		
		public int maximumWriteLength = 8192;
		public Timer timeoutTimer;
		
		public void onMessage(MqttMessage message) {
			String strOperation = new String(message.getPayload());
			String ctx = "";
			if (strOperation.indexOf(':') >= 0) {
				int x = strOperation.indexOf(':');
				ctx = strOperation.substring(x + 1);
				strOperation = strOperation.substring(0, x);
			}
			CPUOperation operation = CPUOperation.fromName(strOperation);
			switch (operation) {
				case HALT_ACK:
					isHalted = true;
					break;
				case RESET_ACK:
					isHalted = true;
					break;
				case RUN_ACK:
					isHalted = false;
					break;
				case CONNECT:
					isOnline = true;
					break;
				case DISCONNECT:
					isOnline = false;
					break;
				case ERROR:
					clearTimeout();
					pendingAction = null;
					pendingWrite = null;
					lastError = ctx;
					if (future != null) future.complete(false);
					//future = null;
					break;
				
			}
			if (pendingAction != null) switch (pendingAction) {
				case HALTING:
					if (operation == CPUOperation.DISCONNECT) {
						clearTimeout();
						pendingAction = null;
						lastError = "CPU Offline";
						if (future != null) future.complete(false);
						//future = null;
					} else if (operation == CPUOperation.HALT_ACK) {
						clearTimeout();
						pendingAction = null;
						lastError = null;
						if (future != null) future.complete(true);
						//future = null;
					}
					break;
				case RUNNING:
					if (operation == CPUOperation.DISCONNECT) {
						clearTimeout();
						pendingAction = null;
						lastError = "cpu_offline";
						if (future != null) future.complete(false);
						//future = null;
					} else if (operation == CPUOperation.RUN_ACK) {
						clearTimeout();
						pendingAction = null;
						lastError = null;
						if (future != null) future.complete(true);
						//future = null;
					}
					break;
				case RESETTING:
					if (operation == CPUOperation.DISCONNECT) {
						clearTimeout();
						pendingAction = null;
						lastError = "cpu_offline";
						if (future != null) future.complete(false);
						//future = null;
					} else if (operation == CPUOperation.RESET_ACK) {
						clearTimeout();
						pendingAction = null;
						lastError = null;
						if (future != null) future.complete(true);
						//future = null;
					}
					break;
				case WRITING:
					if (operation == CPUOperation.DISCONNECT) {
						pendingAction = null;
						lastError = "cpu_offline";
						pendingWrite = null;
						if (future != null) future.complete(false);
						//future = null;
					} else if (operation == CPUOperation.WRITE_ACK) {
						handleWrite();
					}
					break;
				case POLL_ONLINE:
					clearTimeout();
					pendingAction = null;
					lastError = null;
					if (future != null) future.complete(true);
					//future = null;
					break;
			}
		}
		
		protected void handleWrite() {
			if (pendingWriteIndex >= pendingWrite.length) {
				clearTimeout();
				pendingAction = null;
				lastError = null;
				future.complete(true);
				//future = null;
			} else {
				int len = Math.min(pendingWrite.length - pendingWriteIndex, maximumWriteLength);
				StringBuilder msg = new StringBuilder(String.format("write:%x:%x:%02x", pendingWriteIndex + pendingWriteAddress, len, pendingWrite[pendingWriteIndex]));
				for (int i = 1; i < len; i++) {
					msg.append(String.format(",%02x", pendingWrite[pendingWriteIndex + i]));
				}
				pendingWriteIndex += len;
				sendMessage(CPU_CONTROL_TOPIC, msg.toString());
				setTimeout();
			}
		}
		
		protected void clearTimeout() {
			if (timeoutTimer != null) {
				timeoutTimer.cancel();
				timeoutTimer.purge();
				timeoutTimer = new Timer();
			} else {
				timeoutTimer = new Timer();
			}
		}
		
		protected void setTimeout() {
			if (timeoutTimer == null) timeoutTimer = new Timer();
			else {
				timeoutTimer.cancel();
				timeoutTimer.purge();
				timeoutTimer = new Timer();
			}
			timeoutTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					pendingAction = null;
					lastError = "cpu_timeout";
					if (future != null) {
						future.complete(false);
					}
					//pendingWrite = null;
				}
			}, 5000);
		}
		
		public CompletableFuture<Boolean> halt() {
			if (pendingAction != null) {
				lastError = "action_pending:" + pendingAction.name();
				return CompletableFuture.completedFuture(false);
			} else if (!isOnline) {
				lastError = "cpu_offline";
				return CompletableFuture.completedFuture(false);
			} else if (isHalted) {
				return CompletableFuture.completedFuture(true);
			} else {
				setTimeout();
				pendingAction = CPUPendingAction.HALTING;
				future = new CompletableFuture<>();
				sendMessage(CPU_CONTROL_TOPIC, "halt");
				return future;
			}
		}
		
		public CompletableFuture<Boolean> write(int i, byte[] arr) {
			if (pendingAction != null) {
				lastError = "action_pending:" + pendingAction.name();
				return CompletableFuture.completedFuture(false);
			} else if (!isOnline) {
				lastError = "cpu_offline";
				return CompletableFuture.completedFuture(false);
			} else if (!isHalted) {
				lastError = "cpu_not_halted";
				return CompletableFuture.completedFuture(false);
			} else {
				pendingAction = CPUPendingAction.WRITING;
				pendingWrite = arr.clone();
				pendingWriteIndex = 0;
				pendingWriteAddress = i;
				setTimeout();
				CompletableFuture<Boolean> f = new CompletableFuture<>();
				future = f;
				handleWrite();
				return f;
			}
		}
		
		public CompletableFuture<Boolean> reset() {
			if (pendingAction != null) {
				lastError = "action_pending:" + pendingAction.name();
				return CompletableFuture.completedFuture(false);
			} else if (!isOnline) {
				lastError = "cpu_offline";
				return CompletableFuture.completedFuture(false);
			} else {
				setTimeout();
				pendingAction = CPUPendingAction.RESETTING;
				future = new CompletableFuture<>();
				sendMessage(CPU_CONTROL_TOPIC, "reset");
				return future;
			}
		}
		
		public CompletableFuture<Boolean> run() {
			if (pendingAction != null) {
				lastError = "action_pending:" + pendingAction.name();
				return CompletableFuture.completedFuture(false);
			} else if (!isOnline) {
				lastError = "cpu_offline";
				return CompletableFuture.completedFuture(false);
			} else if (!isHalted) {
				return CompletableFuture.completedFuture(true);
			} else {
				setTimeout();
				pendingAction = CPUPendingAction.RUNNING;
				future = new CompletableFuture<>();
				sendMessage(CPU_CONTROL_TOPIC, "run");
				return future;
			}
		}
		
		public CompletableFuture<Boolean> pollStatus() {
			while (future != null) {
				future.join();
			}
			pendingAction = CPUPendingAction.POLL_ONLINE;
			clearTimeout();
			future = new CompletableFuture<>();
			timeoutTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					isOnline = false;
					pendingAction = null;
					future.complete(true);
					//future = null;
				}
			}, 5000);
			sendMessage(CPU_CONTROL_TOPIC, "poll_status");
			return future;
		}
	}
	
	public enum CPURunningType {
		HALTED,
		ONLINE_SUBMISSION,
		MANUAL_SUBMISSION,
		PLACEHOLDER
	}
	
	public enum CPUPendingAction {
		HALTING("halt"),
		WRITING("write"),
		RESETTING("reset"),
		RUNNING("run"),
		POLL_ONLINE("poll_status"),
		UNKNOWN("unknown");
		public final String name;
		
		CPUPendingAction(String name) {
			this.name = name;
		}
		
		public static CPUPendingAction fromName(String s) {
			if (s == null) return UNKNOWN;
			for (CPUPendingAction op : values()) {
				if (op.name.equalsIgnoreCase(s)) {
					return op;
				}
			}
			return UNKNOWN;
		}
		
	}
	
	public enum CPUOperation {
		ERROR("error"),
		HALT_ACK("halt_ack"),
		RESET_ACK("reset_ack"),
		RUN_ACK("run_ack"),
		CONNECT("connect"),
		DISCONNECT("disconnect"),
		WRITE_ACK("write_ack"),
		WRITE_PROGRESS("write_prog"),
		UNKNOWN("unkown");
		public final String name;
		
		CPUOperation(String name) {
			this.name = name;
		}
		
		public static CPUOperation fromName(String s) {
			if (s == null) return UNKNOWN;
			for (CPUOperation op : values()) {
				if (op.name.equalsIgnoreCase(s)) {
					return op;
				}
			}
			return UNKNOWN;
		}
		
	}
	
	public static class WebSocketDaemon extends WebSocketServer {
		
		public WebSocketDaemon(int port) {
			super(new InetSocketAddress(port));
		}
		
		@Override
		public void onStart() {
			System.out.println("WebSocket server started.");
			System.out.print("CpuControlR2> ");
		}
		
		@Override
		public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
			
		}
		
		@Override
		public void onClose(WebSocket webSocket, int i, String s, boolean b) {
			
		}
		
		@Override
		public void onMessage(WebSocket ws, String raw) {
			JSONObject obj = new JSONObject(raw);
			String type = obj.getString("type");
			Transaction transaction = new Transaction(ws, type, obj.getString("transactionId"));
			switch (type) {
				case ("action"):
					handleAction(transaction, obj);
					break;
				case ("info"):
					handleInfo(transaction, obj);
					break;
				default:
					transaction.error("unknown_type");
					break;
			}
		}
		
		@Override
		public void onError(WebSocket webSocket, Exception e) {
			try {
				webSocket.close();
			} catch (Exception ignored) {
				
			}
		}
		
		protected void handleAction(Transaction transaction, JSONObject obj) {
			String action = obj.getString("action");
			// TODO: Permission craps.
			switch (action) {
				case ("halt"):
					createRespListener(transaction, cpu.halt());
					break;
				case ("cont"):
					createRespListener(transaction, cpu.run());
					break;
				case ("run"):
					executor.execute(() -> assembleAndRun(transaction, obj));
					break;
				default:
					transaction.error("unknown_action");
					break;
			}
		}
		
		protected void handleInfo(Transaction transaction, JSONObject obj) {
			String info = obj.getString("info");
			switch (info) {
				case ("program"):
					// TODO
					break;
				default:
					transaction.error("unknown_info");
					break;
			}
		}
		
		protected void createRespListener(Transaction tr, CompletableFuture<Boolean> future) {
			future.thenAccept((x) -> {
				JSONObject resp = new JSONObject();
				if (x) {
					tr.success();
				} else {
					tr.error(cpu.lastError);
				}
			});
			future.exceptionally((x) -> {
				tr.error("exception:" + x.getClass().getSimpleName() + ":" + x.getMessage());
				return null;
			});
		}
		
		protected void assembleAndRun(Transaction tr, JSONObject obj) {
			try {
				JSONObject files = obj.getJSONObject("files");
				ImportSupplier supplier = new ImportSupplier() {
					@Override
					public boolean isFileAvailable(String name) {
						return files.has(name);
					}
					
					@Override
					public String getStringFile(String name) throws IOException {
						if (!files.has(name) || !(files.get(name) instanceof String)) {
							throw new FileNotFoundException(name);
						}
						return files.getString(name);
					}
					
					@Override
					public byte[] getByteFile(String name) throws IOException {
						if (!files.has(name) || !(files.get(name) instanceof String)) {
							throw new FileNotFoundException(name);
						}
						return files.getString(name).getBytes(AssemblerCore.IBM437);
					}
					
					@Override
					public long[] getBinaryFile(String name, int bits, boolean isLittleEndian) throws IOException {
						return new long[0];
					}
				};
				String fileName = "main.asm";
				String[] rawLines = supplier.getStringFile(fileName).split("\\r\\n|\\r|\\n");
				String[][] tokensedLines = new String[rawLines.length][];
				for (int i = 0; i < rawLines.length; i++) {
					tokensedLines[i] = AssemblerCore.tokeniseLine(rawLines[i]);
				}
				Pass0Out pass0 = AssemblerCore.pass0(tokensedLines, supplier, fileName);
				Pass1Out pass1 = AssemblerCore.pass1(pass0, isa, AssemblerCore.IBM437);
				Pass2Out pass2 = AssemblerCore.pass2(pass1, isa, AssemblerCore.IBM437);
				if (pass2.warnings.size() > 0) {
					JSONArray warnArr = new JSONArray();
					for (CompilerWarning warning : pass2.warnings) {
						warnArr.put("[WARN] " + warning.source + ":" + warning.line + "\n" + warning.getMessage());
					}
					tr.resp.put("warnings", warnArr);
				}
				if (pass2.errors.size() > 0) {
					JSONArray errArr = new JSONArray();
					for (CompilerError error : pass2.errors) {
						errArr.put("[WARN] " + error.source + ":" + error.line + "\n" + error.getMessage());
					}
					tr.resp.put("errors", errArr);
					tr.error("asm_error");
				} else {
					int offs = pass2.removePrefixPadding ? (int) pass2.paddingLength : 0;
					byte[] bin = new byte[pass2.wordsOut.length - offs];
					for (int i = 0; i < bin.length; i++) {
						bin[i] = (byte) pass2.wordsOut[i];
					}
					QueueEntry entry = new QueueEntry(obj.getString("author"), obj.getString("name"), bin, defaultRunTime);
					addToQueue(entry);
					tr.success();
				}
			} catch (IOException e) {
				JSONObject resp = new JSONObject();
				tr.error(e.getClass().getName() + ": " + e.getMessage());
			}
		}
		
	}
	
}
