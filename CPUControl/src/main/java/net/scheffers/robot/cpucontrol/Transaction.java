package net.scheffers.robot.cpucontrol;

import org.java_websocket.WebSocket;
import org.json.JSONObject;

import java.util.concurrent.CompletableFuture;

public class Transaction extends CompletableFuture<String> {
	
	public WebSocket ws;
	public String id;
	public String type;
	public JSONObject resp;
	
	public Transaction(WebSocket ws, String type, String id) {
		this.ws = ws;
		this.type = type;
		this.id = id;
		this.resp = new JSONObject();
	}
	
	public void success() {
		resp.put("type", type);
		resp.put("result", "success");
		resp.put("transactionId", id);
		ws.send(resp.toString());
		complete(null);
	}
	
	public void error(String reason) {
		resp.put("type", type);
		resp.put("result", "error");
		resp.put("error", reason);
		resp.put("transactionId", id);
		ws.send(resp.toString());
		complete(reason);
	}
	
}
