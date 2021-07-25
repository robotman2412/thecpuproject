package net.scheffers.robot.emu.modules;

import jutils.guiv2.GUIElement;
import net.scheffers.robot.emu.EMUConstants;
import net.scheffers.robot.emu.Emulator;
import processing.core.PApplet;
import processing.core.PConstants;

public class Keyboardonator extends GUIElement implements EMUConstants {
	
	public boolean selected;
	
	public Keyboardonator(PApplet mP) {
		super(mP);
		height = 20;
	}
	
	@Override
	public void render() {
		if (selected) {
			p.strokeWeight(3);
			p.stroke(0xff60afff);
		} else {
			p.strokeWeight(1);
			p.stroke(0xff7f7f7f);
		}
		width = Emulator.inst.ttyWidth * 7 + 4;
		x = p.width - width - 18;
		y = p.height - height - 10;
		
		p.fill(0);
		p.rect(x, y, width, height);
		
		p.fill(0xff00ff00);
		p.textFont(Emulator.font12, 12);
		
		for (int i = 0; i < Emulator.inst.ttyInputLen && i < Emulator.inst.ttyWidth; i++) {
			char c = (char) Emulator.inst.ttyInputBuffer[(Emulator.inst.ttyInputPos + i) % Emulator.inst.ttyInputBuffer.length];
			if (c == '\b') c = '←';
			else if (c == '\n') c = '↓';
			else if (c == '\r') c = '↓';
			else if (c < ' ') c = ' ';
			p.text(c, (i - Emulator.inst.ttyWidth) * 7 + p.width - 20, y + 15);
		}
	}
	
	@Override
	public void mousePressed() {
		selected = amIHovered();
	}
	
	@Override
	public void mouseReleased() {
		
	}
	
	@Override
	public void keyPressed() {
		if (selected) {
			if (p.key <= 0x7f) {
				Emulator.inst.ttyType((byte) p.key);
			}
			else if (p.keyCode == PConstants.UP) {
				Emulator.inst.ttyType((byte) 0x11);
			}
			else if (p.keyCode == PConstants.DOWN) {
				Emulator.inst.ttyType((byte) 0x12);
			}
			else if (p.keyCode == PConstants.LEFT) {
				Emulator.inst.ttyType((byte) 0x13);
			}
			else if (p.keyCode == PConstants.RIGHT) {
				Emulator.inst.ttyType((byte) 0x14);
			}
		}
	}
	
	@Override
	public void keyReleased() {
		
	}
	
}
