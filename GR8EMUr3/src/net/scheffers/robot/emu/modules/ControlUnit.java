package net.scheffers.robot.emu.modules;

import jutils.guiv2.GUIElement;
import net.scheffers.robot.emu.GR8CPURev3_1;
import net.scheffers.robot.emu.EMUConstants;
import net.scheffers.robot.emu.Emulator;
import processing.core.PApplet;
import processing.core.PConstants;

public class ControlUnit extends GUIElement implements PConstants, EMUConstants {
	
	public String[] controls = {
			"AIA", "AIB", "AIO", "in_0", "OPTN0", "-", "in_1", "in_2",
			"in_3", "out_0", "out_1", "out_2", "out_3", "ina_0", "FRI", "FCX",
			"OPTN2", "ina_1", "outa_0", "outa_1", "INC", "outa_2", "HLT", "-",
			"-", "STR", "-", "ADC", "OPTN3", "OPTN1", "ADRHI", "RSTB"
	};
	
	public GR8CPURev3_1 cpu;
	public Emulator.EmuThread emulator;
	public int selected;
	
	public ControlUnit(PApplet p, int x, int y, Emulator.EmuThread emulator) {
		super(p, x, y, thingyWidth, thingyHeight * 3);
		this.cpu = emulator.cpu;
		this.emulator = emulator;
	}
	
	@Override
	public void render() {
		p.fill(thingyColor);
		p.stroke(0);
		p.strokeWeight(1);
		p.pushMatrix();
		p.translate(x, y);
		p.rect(0, 0, thingyWidth, thingyHeight * 3);
		
		p.textAlign(PConstants.CENTER);
		p.textFont(Emulator.font12, 12);
		p.fill(0);
		p.text("control unit", thingyWidth * 0.5f, 14);
		
		int address;
		if (cpu.mode == 0) { // Find address in ISAROM.
			address = ((cpu.regIR & 0x7F) << 4) | cpu.stage;
		} else {
			address = (cpu.mode << 4) | cpu.stage | (1 << 11);
		}
		int ctrl;
		if (address >= cpu.isa.length) {
			ctrl = 0;
		} else {
			ctrl = cpu.isa[address]; // Get control signals.
		}
		
		int temp0 = (ctrl & 0xff000000) >>> 24;
		int temp1 = (ctrl & 0x00ff0000) >>> 16;
		int temp2 = (ctrl & 0x0000ff00) >>> 8;
		int temp3 = (ctrl & 0x000000ff);
		for (int i = 0; i < 8; i++) {
			if ((temp0 & 0x80) > 0) {
				p.fill(lightBlueOn);
			} else {
				p.fill(lightBlueOff);
			}
			p.ellipse(i * 18 + 43, 146, 16, 16);
			if ((temp1 & 0x80) > 0) {
				p.fill(lightBlueOn);
			} else {
				p.fill(lightBlueOff);
			}
			p.ellipse(i * 18 + 39, 166, 16, 16);
			if ((temp2 & 0x80) > 0) {
				p.fill(lightBlueOn);
			} else {
				p.fill(lightBlueOff);
			}
			p.ellipse(i * 18 + 35, 186, 16, 16);
			if ((temp3 & 0x80) > 0) {
				p.fill(lightBlueOn);
			} else {
				p.fill(lightBlueOff);
			}
			p.ellipse(i * 18 + 31, 206, 16, 16);
			temp0 <<= 1;
			temp1 <<= 1;
			temp2 <<= 1;
			temp3 <<= 1;
		}
		
		temp0 = cpu.mode;
		for (int i = 0; i < 2; i++) {
			if ((temp0 & 0x02) > 0) {
				p.fill(lightBlueOn);
			} else {
				p.fill(lightBlueOff);
			}
			p.ellipse(i * 18 + 31, 52, 16, 16);
			temp0 <<= 1;
		}
		
		temp1 = cpu.stage;
		for (int i = 0; i < 4; i++) {
			if ((temp1 & 0x08) > 0) {
				p.fill(lightBlueOn);
			} else {
				p.fill(lightBlueOff);
			}
			p.ellipse(i * 18 + 112, 52, 16, 16);
			temp1 <<= 1;
		}
		
		if (selected == 1) {
			p.noFill();
			p.stroke(0xff03f4fc);
			p.rect(26, 63, 28, 18);
		} else if (selected == 2) {
			p.noFill();
			p.stroke(0xff03f4fc);
			p.rect(125, 63, 28, 18);
		}
		
		p.textFont(Emulator.font48, 24);
		p.stroke(0);
		p.fill(0);
		p.text(String.format("%01x", cpu.mode), thingyWidth * 0.20f, 80);
		p.text(String.format("%01x", cpu.stage), thingyWidth * 0.70f, 80);
		
		p.textFont(Emulator.font12, 12);
		
		p.text("mode", thingyWidth * 0.20f, 40);
		p.text("stage", thingyWidth * 0.70f, 40);
		
		p.popMatrix();
	}
	
	@Override
	public void mousePressed() {
		if (p.mouseX > x + 26 && p.mouseX < x + 54 && p.mouseY > y + 63 && p.mouseY < y + 81) {
			selected = 1;
		} else if (p.mouseX > x + 125 && p.mouseX < x + 153 && p.mouseY > y + 63 && p.mouseY < y + 81) {
			selected = 2;
		} else {
			selected = 0;
		}
	}
	
	@Override
	public void mouseReleased() {
		
	}
	
	@Override
	public void keyPressed() {
		if (selected == 0) return;
		if (emulator.forceTick > 0 || emulator.doTick) {
			emulator.doTick = false;
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (selected == 1 && p.key >= '0' && p.key <= '3') {
			cpu.mode = (byte) (p.key - '0');
		} else if (selected == 2 && p.key >= '0' && p.key <= '9') {
			cpu.stage = (byte) (p.key - '0');
		} else if (selected == 2 && p.key >= 'a' && p.key <= 'f') {
			cpu.stage = (byte) (p.key - 'a' + 0xa);
		} else if (selected == 2 && p.key >= 'A' && p.key <= 'F') {
			cpu.stage = (byte) (p.key - 'A' + 0xa);
		}
	}
	
	@Override
	public void keyReleased() {
		
	}
}
