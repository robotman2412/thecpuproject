package net.scheffers.robot.cpucontrol;

import org.json.JSONObject;

public class QueueEntry {
	
	public String author;
	public String name;
	public byte[] binary;
	public long runTime;
	
	public QueueEntry(String author, String name, byte[] binary, long runTime) {
		this.author = author;
		this.name = name;
		this.binary = binary;
		this.runTime = runTime;
	}
	
	public JSONObject toJSONObject() {
		JSONObject obj = new JSONObject();
		obj.put("author", author);
		obj.put("name", name);
		obj.put("duration", runTime);
		return obj;
	}
	
}
