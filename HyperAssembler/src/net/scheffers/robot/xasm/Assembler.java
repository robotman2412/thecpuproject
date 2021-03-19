package net.scheffers.robot.xasm;

import java.util.HashMap;
import java.util.Map;

public class Assembler {
	
	public static Map<String, AssemblyDirective> directives = defaultDirectives();
	
	public static void main(String[] args) {
		
	}
	
	public static Map<String, AssemblyDirective> defaultDirectives() {
		Map<String, AssemblyDirective> map = new HashMap<>();
		
		//map.put("abort", null);
		map.put("align", AssemblyDirectives::align);
		//map.put("ascii", AssemblyDirectives::ascii);
		//map.put("asciz", AssemblyDirectives::asciz);
		
		return map;
	}
	
}
