package net.scheffers.robot.emu.debugger;

import jutils.gui.style.TextureButtonStyle;
import jutils.guiv2.*;
import net.scheffers.robot.emu.EMUConstants;
import net.scheffers.robot.emu.Emulator;
import net.scheffers.robot.emu.GR8CPURev3_1;
import net.scheffers.robot.hyperasm.AssemblerCore;
import net.scheffers.robot.hyperasm.Label;
import net.scheffers.robot.hyperasm.Pass2Out;
import net.scheffers.robot.xasm.expression.Expression;
import processing.core.PApplet;
import processing.core.PImage;
import processing.event.MouseEvent;

import java.util.*;

public class Debugger extends PApplet implements EMUConstants {
	
	public final Emulator emu;
	public final GR8CPURev3_1 cpu;
	
	public Map<String, Integer> verboseColorMap;
	public Map<String, Integer> colorMap;
	public String literalRegex = "^(#.+|'.+'|\".+\"|(\\$|0x)[0-9a-fA-F]+|[0-9a-fA-F]+h|%[01]+|0[oq][0-7]+|[0-7]+[oq]|[<>]\\.?[a-zA-Z_][a-zA-Z_0-9]*)$";
	
	/**
	 * CPU control buttons.
	 */
	public TextureButton resetButton, playButton, pauseButton, cycleButton, stepOverButton, stepInButton, stepOutButton;
	public TextInput evalInput;
	public Button setBreakButton, triggerIRQ, triggerNMI;
	public GUIScreen screen;
	public int tabulations = 12;
	public short evaluated;
	public int evalLength;
	public int asmOffset;
	public short lastPC;
	public boolean hasTheEval;
	public List<Variable> variables;
	
	public PImage iconImage;
	
	public Set<Short> breakpoints = new HashSet<>();
	
	public Debugger() {
		emu = Emulator.inst;
		cpu = emu.emuThread.cpu;
		verboseColorMap = new HashMap<>();
		colorMap = new HashMap<>();
		
		//region verbose
		verboseColorMap = new HashMap<>();
		verboseColorMap.put("load", 0xff0000ff);
		verboseColorMap.put("copy", 0xff0000ff);
		verboseColorMap.put("add", 0xff0000ff);
		verboseColorMap.put("sub", 0xff0000ff);
		verboseColorMap.put("comp", 0xff0000ff);
		verboseColorMap.put("push", 0xff0000ff);
		verboseColorMap.put("jump", 0xff0000ff);
		verboseColorMap.put("or", 0xff0000ff);
		verboseColorMap.put("and", 0xff0000ff);
		verboseColorMap.put("xor", 0xff0000ff);
		verboseColorMap.put("shift", 0xff0000ff);
		verboseColorMap.put("store", 0xff0000ff);
		verboseColorMap.put("halt", 0xff0000ff);
		verboseColorMap.put("inc", 0xff0000ff);
		verboseColorMap.put("dec", 0xff0000ff);
		verboseColorMap.put("rot", 0xff0000ff);
		verboseColorMap.put("pull", 0xff0000ff);
		verboseColorMap.put("call", 0xff0000ff);
		verboseColorMap.put("return", 0xff0000ff);
		verboseColorMap.put("pop", 0xff0000ff);
		verboseColorMap.put("stack", 0xff0000ff);
		//verboseColorMap.put("peek", 0xff0000ff);
		//verboseColorMap.put("swap", 0xff0000ff);
		verboseColorMap.put("irq", 0xff0000ff);
		verboseColorMap.put("nmi", 0xff0000ff);
		
		verboseColorMap.put("to", 0xff3030ff);
		verboseColorMap.put("config", 0xff3030ff);
		verboseColorMap.put("cont", 0xff3030ff);
		verboseColorMap.put("if", 0xff3030ff);
		verboseColorMap.put("unless", 0xff3030ff);
		verboseColorMap.put("pointer", 0xff3030ff);
		verboseColorMap.put("table", 0xff3030ff);
		
		verboseColorMap.put("a", 0xff8080c0);
		verboseColorMap.put("b", 0xff8080c0);
		verboseColorMap.put("x", 0xff8080c0);
		verboseColorMap.put("y", 0xff8080c0);
		verboseColorMap.put("l", 0xff8080c0);
		verboseColorMap.put("r", 0xff8080c0);
		verboseColorMap.put("sl", 0xff8080c0);
		verboseColorMap.put("sh", 0xff8080c0);
		verboseColorMap.put("f", 0xff8080c0);
		verboseColorMap.put("carry", 0xff8080c0);
		
		verboseColorMap.put("data", 0xff0000ff);
		verboseColorMap.put("byte", 0xff0000ff);
		verboseColorMap.put("bytes", 0xff0000ff);
		verboseColorMap.put("reserve", 0xff0000ff);
		//endregion verbose
		
		//region standard
		colorMap.put("bki", 0xff0000ff);
		colorMap.put("brk", 0xff0000ff);
		colorMap.put("call", 0xff0000ff);
		colorMap.put("ret", 0xff0000ff);
		colorMap.put("psh", 0xff0000ff);
		colorMap.put("pul", 0xff0000ff);
		colorMap.put("pop", 0xff0000ff);
		colorMap.put("jmp", 0xff0000ff);
		colorMap.put("beq", 0xff0000ff);
		colorMap.put("bne", 0xff0000ff);
		colorMap.put("bgt", 0xff0000ff);
		colorMap.put("ble", 0xff0000ff);
		colorMap.put("blt", 0xff0000ff);
		colorMap.put("bge", 0xff0000ff);
		colorMap.put("bcs", 0xff0000ff);
		colorMap.put("bcc", 0xff0000ff);
		colorMap.put("mov", 0xff0000ff);
		colorMap.put("add", 0xff0000ff);
		colorMap.put("sub", 0xff0000ff);
		colorMap.put("cmp", 0xff0000ff);
		colorMap.put("inc", 0xff0000ff);
		colorMap.put("dec", 0xff0000ff);
		colorMap.put("shl", 0xff0000ff);
		colorMap.put("shr", 0xff0000ff);
		colorMap.put("addc", 0xff0000ff);
		colorMap.put("subc", 0xff0000ff);
		colorMap.put("cmpc", 0xff0000ff);
		colorMap.put("shlc", 0xff0000ff);
		colorMap.put("shrc", 0xff0000ff);
		colorMap.put("incc", 0xff0000ff);
		colorMap.put("decc", 0xff0000ff);
		colorMap.put("rol", 0xff0000ff);
		colorMap.put("ror", 0xff0000ff);
		colorMap.put("and", 0xff0000ff);
		colorMap.put("or", 0xff0000ff);
		colorMap.put("xor", 0xff0000ff);
		colorMap.put("sirq", 0xff0000ff);
		colorMap.put("cirq", 0xff0000ff);
		colorMap.put("virq", 0xff0000ff);
		colorMap.put("vnmi", 0xff0000ff);
		colorMap.put("vst", 0xff0000ff);
		colorMap.put("hlt", 0xff0000ff);
		colorMap.put("gptr", 0xff0000ff);
		colorMap.put("rti", 0xff0000ff);
		
		colorMap.put("a", 0xff8080c0);
		colorMap.put("b", 0xff8080c0);
		colorMap.put("x", 0xff8080c0);
		colorMap.put("y", 0xff8080c0);
		colorMap.put("f", 0xff8080c0);
		colorMap.put("stl", 0xff8080c0);
		colorMap.put("sth", 0xff8080c0);
		
		colorMap.put("data", 0xff0000ff);
		colorMap.put("byte", 0xff0000ff);
		colorMap.put("bytes", 0xff0000ff);
		colorMap.put("reserve", 0xff0000ff);
		colorMap.put("pie", 0xff0000ff);
		
		colorMap.put("[", 0xff808000);
		colorMap.put("]", 0xff808000);
		colorMap.put("(", 0xff008080);
		colorMap.put(")", 0xff008080);
		//endregion standard
		
		variables = new LinkedList<>();
	}
	
	@Override
	public void settings() {
		size(680, 300);
	}
	
	@Override
	public void setup() {
		textFont(Emulator.font12, 12);
		surface.setResizable(true);
		
		screen = new GUIScreen(this);
		
		if (iconImage != null) {
			surface.setIcon(iconImage);
		}
		
		//region gui
		resetButton = new TextureButton(this, 0, 5, 40, 40, false, new TextureButtonStyle(
				emu.reset, emu.resetHover, emu.resetPressed, emu.resetDisabled
		), () -> emu.resetButton.onPress.run(null));
		screen.add(resetButton);
		
		pauseButton = new TextureButton(this, 40, 5, 40, 40, false, new TextureButtonStyle(
				emu.pause, emu.pauseHover, emu.pausePressed, emu.pauseDisabled
		), () -> emu.emuThread.doTick = false);
		screen.add(pauseButton);
		
		playButton = new TextureButton(this, 80, 5, 40, 40, false, new TextureButtonStyle(
				emu.play, emu.playHover, emu.playPressed, emu.playDisabled
		), () -> emu.emuThread.doTick = true);
		screen.add(playButton);
		
		cycleButton = new TextureButton(this, 120, 5, 40, 40, false, new TextureButtonStyle(
				emu.cycle, emu.cycleHover, emu.cyclePressed, emu.cycleDisabled
		), () -> {synchronized (emu.emuThread){emu.emuThread.forceTick++;}});
		screen.add(cycleButton);
		
		stepOverButton = new TextureButton(this, 160, 5, 40, 40, false, new TextureButtonStyle(
				emu.stepOver, emu.stepOverHover, emu.stepOverPressed, emu.stepOverDisabled
		), emu.emuThread::stepOver);
		screen.add(stepOverButton);
		
		stepInButton = new TextureButton(this, 200, 5, 40, 40, false, new TextureButtonStyle(
				emu.stepIn, emu.stepInHover, emu.stepInPressed, emu.stepInDisabled
		), emu.emuThread::stepIn);
		screen.add(stepInButton);
		
		stepOutButton = new TextureButton(this, 240, 5, 40, 40, false, new TextureButtonStyle(
				emu.stepOut, emu.stepOutHover, emu.stepOutPressed, emu.stepOutDisabled
		), emu.emuThread::stepOut);
		screen.add(stepOutButton);
		
		evalInput = new TextInput(this, 280, 10, 150, 20, TextInputType.STRING);
		screen.add(evalInput);
		
		setBreakButton = new Button(this, 440, 10, 120, 20, "set breakpoint", false, this::buttonBreaks);
		screen.add(setBreakButton);
		
		triggerIRQ = new Button(this, 570, 10, 40, 20, "IRQ", false, cpu::triggerIRQ);
		screen.add(triggerIRQ);
		
		triggerIRQ = new Button(this, 620, 10, 40, 20, "NMI", false, cpu::triggerNMI);
		screen.add(triggerIRQ);
		//endregion gui
	}
	
	@Override
	public void draw() {
		if (frameCount <= 1) hide();
		background(0xffffff);
		textAlign(CORNER);
		drawAsm();
		drawVars();
		drawStats();
		screen.render();
		if (hasTheEval) {
			fill(0xff3f3f3f);
			StringBuilder bin = new StringBuilder(evalLength * 2);
			for (int i = evalLength - 1; i >= 0; i--) {
				bin.append(String.format("%02x", cpu.ram[(i + evaluated) & 0xffff]));
			}
			text(String.format("= $%04x ($%s)", evaluated, bin), 380, 42);
		}
		
		pauseButton.enabled = emu.emuThread.doTick;
		playButton.enabled = !emu.emuThread.doTick;
		cycleButton.enabled = !emu.emuThread.doTick;
		stepOverButton.enabled = !emu.emuThread.doTick;
		stepInButton.enabled = !emu.emuThread.doTick;
		stepOutButton.enabled = !emu.emuThread.doTick;
	}
	
	public void drawAsm() {
		Pass2Out dump = emu.assemblyData;
		if (dump == null) {
			return;
		}
		pushMatrix();
		float spaceWidth = textWidth(' ');
		float lineHeight = 13;
		float lineOffset = 2 + spaceWidth * 11;
		translate(0, 50);
		int numDisp = (int) ((height - 50) / lineHeight + 0.5000000001);
		int length = dump.lineLengths.length;
		if (addressToIndex(cpu.regPC) != addressToIndex(lastPC)) {
			asmOffset = addressToIndex(cpu.regPC);
			lastPC = cpu.regPC;
		}
		int offset = asmOffset;
		if (offset >= length) offset = length - numDisp;
		if (offset <= 0) offset = 0;
		int yOffset = 0;
		int fuck = 4;
		for (int i = offset; i >= 0; i--) {
			if (dump.tokensOut[i] != null && dump.tokensOut[i].length > 0) {
				if (dump.tokensOut[i].length == 1 && dump.tokensOut[i][0].length() == 0) {
					continue;
				}
				offset = i;
				if (--fuck < 1) break;
			}
		}
		if (offset >= length) offset = length - numDisp;
		if (offset <= 0) offset = 0;
		for (int i = 0; yOffset < numDisp && i + offset < length; i++) {
			int index = i + offset;
			if (dump.tokensOut[index] != null && dump.tokensOut[index].length > 0) {
				if (dump.tokensOut[index].length == 1 && dump.tokensOut[index][0].length() == 0) {
					continue;
				}
				if (cpu.regPC >= dump.lineStartAddresses[index] && cpu.regPC < dump.lineStartAddresses[index] + dump.lineLengths[index]) {
					fill(0xfffffea8);
					noStroke();
					rect(0, yOffset * lineHeight + 1, (int) (width * 2 / 3), lineHeight + 1);
					stroke(0);
				}
				if (dump.lineLengths[index] > 0 && breakpoints.contains((short) dump.lineStartAddresses[index])) {
					fill(0xffff5050);
					noStroke();
					beginShape();
					vertex(0, yOffset * lineHeight + 1);
					vertex(74, yOffset * lineHeight + 1);
					vertex(78, yOffset * lineHeight + 1.5f + lineHeight * 0.5f);
					vertex(74, yOffset * lineHeight + 2 + lineHeight);
					vertex(0, yOffset * lineHeight + 2 + lineHeight);
					endShape(CLOSE);
					stroke(0);
				}
				yOffset++;
			}
		}
		yOffset = 0;
		for (int i = 0; yOffset < numDisp && i + offset < length; i++) {
			int index = i + offset;
			if (dump.tokensOut[index] != null && dump.tokensOut[index].length > 0) {
				if (dump.tokensOut[index].length == 1 && dump.tokensOut[index][0].length() == 0) {
					continue;
				}
				String number = String.format("%4d $%04x", dump.tokenLineNums[index], dump.lineStartAddresses[index]);
				fill(0xff3f3f3f);
				text(number, 2, yOffset * lineHeight + lineHeight);
				fill(0xff000000);
				text(dump.tokensOut[index][0], lineOffset, yOffset * lineHeight + 13);
				if (dump.tokensOut[index][0].length() + 4 > tabulations) {
					tabulations = dump.tokensOut[index][0].length() + 4;
				}
				yOffset++;
			}
		}
		lineOffset = 2 + spaceWidth * 11 + tabulations * spaceWidth;
		yOffset = 0;
		for (int i = 0; yOffset < numDisp && i + offset < length; i++) {
			int index = i + offset;
			if (dump.tokensOut[index] != null && dump.tokensOut[index].length > 0) {
				if (dump.tokensOut[index].length == 1 && dump.tokensOut[index][0].length() == 0) {
					continue;
				}
				float wow = 0;
				for (int x = 1; x < dump.tokensOut[index].length; x++) {
					String text = dump.tokensOut[index][x];
					if (text.equals("#") && x < dump.tokensOut[index].length - 1) {
						text += dump.tokensOut[index][x + 1];
						x ++;
						fill(0xffff0000);
					} else if (text.matches("^[,\\])]$")) {
						wow -= spaceWidth;
					}
					if (text.matches(literalRegex)) {
						fill(0xffff0000);
					} else if (dump.isa.isVerbose) {
						fill(verboseColorMap.getOrDefault(text.toLowerCase(), 0xff000000));
					} else {
						fill(colorMap.getOrDefault(text.toLowerCase(), 0xff000000));
					}
					text(text, lineOffset + wow, yOffset * lineHeight + 13);
					wow += textWidth(text) + spaceWidth;
					if (text.matches("^[\\[(]$")) {
						wow -= spaceWidth;
					}
				}
				yOffset++;
			}
		}
		popMatrix();
	}
	
	public void drawVars() {
		float lineHeight = 13;
		pushMatrix();
		translate((int) (width * 2 / 3), 50);
		fill(0xff0000ff);
		int nameOffset = 0;
		for (int i = 0; i < variables.size(); i++) {
			Variable var = variables.get(i);
			var.updateValue();
			text(var.typeName, 0, i * lineHeight + 13);
			nameOffset = Math.max(nameOffset, (int) textWidth(var.typeName) + 15);
		}
		popMatrix();
//		Variable var = new Variable();
//		var.labelName = "_alloc_space_lo";
//		var.name = "_alloc_space";
//		var.address = 0x2030;
//		var.numBytes = 2;
//		var.updateTypeName();
//		variables.add(var);
	}
	
	public void drawStats() {
		pushMatrix();
		translate(width * 0.6666666f + 20, 60);
		fill(0);
		text("Statistics:", 0, 0);
		text("Num cycles:", 0, 20);
		text("Num calls:",  0, 40);
		text("Num insns:",  0, 60);
		text("Num MMIO R:", 0, 80);
		text("Num MMIO W:", 0, 100);
		textAlign(RIGHT);
		text(formatNicely(Emulator.inst.emuThread.cpu.numCycles), 200, 20);
		text(formatNicely(Emulator.inst.emuThread.cpu.numSubs),   200, 40);
		text(formatNicely(Emulator.inst.emuThread.cpu.numInsns),  200, 60);
		text(formatNicely(Emulator.inst.emuThread.cpu.numMMIOR),  200, 80);
		text(formatNicely(Emulator.inst.emuThread.cpu.numMMIOW),  200, 100);
		int y = 120;
		textAlign(CORNER);
		text("Breakpoints:", 0, 120);
		for (short at : breakpoints.toArray(new Short[0])) {
			y += 20;
			String label = addressToLabel(at & 0xffff);
			textAlign(CORNER);
			if (label == null) {
				fill(0xffff0000);
				text(String.format("0x%04x", at & 0xffff), 0, y);
			} else {
				fill(0xffff0000);
				text(String.format("0x%04x", at & 0xffff), 0, y);
				textAlign(RIGHT);
				fill(0xff000000);
				text(label, 200, y);
			}
		}
		popMatrix();
	}
	
	public String formatNicely(long value) {
		String raw = "" + value;
		char[] arr = new char[raw.length()+(raw.length()-1)/3];
		int x = arr.length - 1;
		for (int i = raw.length() - 1; i >= 0; i--) {
			arr[x] = raw.charAt(i);
			x --;
			if ((raw.length() - i) % 3 == 0 && i < raw.length() - 1 && i > 0) {
				arr[x] = ' ';
				x --;
			}
		}
		return new String(arr);
	}
	
	public void buttonBreaks() {
		attemptToEval();
		if (hasTheEval) {
			breakpoints.add(evaluated);
		}
		updateBreakpoints();
	}
	
	public void updateBreakpoints() {
		synchronized (cpu) {
			cpu.breakpoints = new short[breakpoints.size()];
			Short[] how = breakpoints.toArray(new Short[0]);
			for (int i = 0; i < breakpoints.size(); i++) {
				cpu.breakpoints[i] = how[i];
			}
		}
	}
	
	public void attemptToEval() {
		hasTheEval = false;
		Pass2Out dump;
		if (emu.assemblyData == null) {
			dump = new Pass2Out();
		}
		else
		{
			dump = emu.assemblyData;
		}
		try {
			int index = evalInput.text.lastIndexOf(':');
			if (index == -1) {
				long[] heck = Expression.resolve("", AssemblerCore.tokeniseLine(evalInput.text.trim()), dump, 0);
				if (heck.length == 1) {
					hasTheEval = true;
					evaluated = (short) heck[0];
					evalLength = 1;
				}
			} else {
				String pre = evalInput.text.substring(0, index).trim();
				String post = evalInput.text.substring(index + 1).trim();
				long[] heck = Expression.resolve("", AssemblerCore.tokeniseLine(pre), dump, 0);
				long[] len = Expression.resolve("", AssemblerCore.tokeniseLine(post), dump, 0);
				if (heck.length == 1) {
					hasTheEval = true;
					evaluated = (short) heck[0];
					if (len.length == 1) {
						evalLength = (int) len[0];
					}
				}
			}
		} catch (Exception ignored) {
			
		}
	}
	
	//region UI
	public void attemptToBreakThePoint() {
		Pass2Out dump = emu.assemblyData;
		if (dump == null) {
			return;
		}
		int y = mouseY - 50;
		if (mouseY < 50 || mouseX > 80) {
			return;
		}
		float spaceWidth = textWidth(' ');
		float lineHeight = 13;
		float lineOffset = 2 + spaceWidth * 11;
		translate(0, 50);
		int numDisp = (int) ((height - 50) / lineHeight + 0.5000000001);
		int length = dump.lineLengths.length;
		if (addressToIndex(cpu.regPC) != addressToIndex(lastPC)) {
			asmOffset = addressToIndex(cpu.regPC);
			lastPC = cpu.regPC;
		}
		int offset = asmOffset;
		if (offset >= length) offset = length - numDisp;
		if (offset <= 0) offset = 0;
		int yOffset = 0;
		int fuck = 4;
		for (int i = offset; i >= 0; i--) {
			if (dump.tokensOut[i] != null && dump.tokensOut[i].length > 0) {
				if (dump.tokensOut[i].length == 1 && dump.tokensOut[i][0].length() == 0) {
					continue;
				}
				offset = i;
				if (--fuck < 1) break;
			}
		}
		if (offset >= length) offset = length - numDisp;
		if (offset <= 0) offset = 0;
		y /= lineHeight;
		for (int i = 0; yOffset < numDisp && i + offset < length; i++) {
			int index = i + offset;
			if (dump.tokensOut[index] != null && dump.tokensOut[index].length > 0) {
				if (dump.tokensOut[index].length == 1 && dump.tokensOut[index][0].length() == 0) {
					continue;
				}
				if (y == yOffset) {
					short address = (short) dump.lineStartAddresses[index];
					if (breakpoints.contains(address)) {
						breakpoints.remove(address);
					}
					else
					{
						breakpoints.add(address);
					}
					updateBreakpoints();
					return;
				}
				// text(0, 2, yOffset * lineHeight + lineHeight);
				yOffset++;
			}
		}
	}
	
	public void breakingPointerLists() {
		int x = mouseX - Math.round(width * 0.6666666f + 20);
		int y = (mouseY - 120 - 60) / 20;
		if (y < 0 || y >= breakpoints.size() || x < 0) return;
		if (mouseButton == RIGHT) {
			short at = breakpoints.toArray(new Short[0])[y];
			breakpoints.remove(at);
		} else {
			short at = breakpoints.toArray(new Short[0])[y];
			int index = addressToIndex(at & 0xffff);
			if (index >= 0) {
				asmOffset = index;
			}
		}
	}
	
	@Override
	public void mousePressed() {
		attemptToBreakThePoint();
		breakingPointerLists();
		screen.mousePressed();
	}
	
	@Override
	public void mouseReleased() {
		screen.mouseReleased();
	}
	
	@Override
	public void mouseWheel(MouseEvent event) {
		Pass2Out dump = emu.assemblyData;
		if (dump == null) {
			return;
		}
		int amount = event.getCount();
		int length = dump.lineLengths.length;
		if (amount < 0) {
			for (int i = asmOffset; i > 0 && amount <= 0; i--) {
				if (dump.tokensOut[i] != null && dump.tokensOut[i].length > 0) {
					if (dump.tokensOut[i].length == 1 && dump.tokensOut[i][0].length() == 0) {
						continue;
					}
					amount ++;
					asmOffset = i;
				}
			}
		} else if (amount > 0) {
			for (int i = asmOffset; i < length && amount >= 0; i++) {
				if (dump.tokensOut[i] != null && dump.tokensOut[i].length > 0) {
					if (dump.tokensOut[i].length == 1 && dump.tokensOut[i][0].length() == 0) {
						continue;
					}
					amount --;
					asmOffset = i;
				}
			}
		}
	}
	
	@Override
	public void keyPressed() {
		screen.keyPressed();
		attemptToEval();
	}
	
	@Override
	public void keyReleased() {
		screen.keyReleased();
	}
	//endregion UI
	
	public int addressToIndex(int address) {
		Pass2Out dump = emu.assemblyData;
		if (dump == null) {
			return -1;
		}
		int line = 0;
		for (int i = 0; i < dump.lineStartAddresses.length; i++) {
			if (address >= dump.lineStartAddresses[i] && address < dump.lineStartAddresses[i] + dump.lineLengths[i]) {
				return i;
			}
		}
		return -1;
	}
	
	public String addressToLabel(int address) {
		Pass2Out dump = emu.assemblyData;
		if (dump == null) {
			return null;
		}
		for (Map.Entry<String, Label> set : dump.labels.entrySet()) {
			String name = set.getKey();
			Label label = set.getValue();
			if (label.address == address) {
				return name;
			}
		}
		return null;
	}
	
	@Override
	public void exit() {
		if (key != ESC) {
			hide();
		}
	}
	
	public void hide() {
		surface.setVisible(false);
		frameRate(5);
	}
	
	public void show() {
		surface.setVisible(true);
		frameRate(60);
	}
	
}
