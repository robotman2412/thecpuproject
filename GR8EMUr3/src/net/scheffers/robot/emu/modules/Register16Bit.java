package net.scheffers.robot.emu.modules;

import jutils.guiv2.GUIElement;
import net.scheffers.robot.emu.EMUConstants;
import net.scheffers.robot.emu.Emulator;
import processing.core.PApplet;
import processing.core.PConstants;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class Register16Bit extends GUIElement implements PConstants, EMUConstants {
	
	public Supplier<Integer> valueSupplier;
	public Consumer<Integer> valueUpdater;
	public int value;
	public String name;
	public int selected;
	
	public Register16Bit(PApplet p, int x, int y, String name) {
		super(p, x, y, thingyWidth, thingyHeight);
		this.name = name;
	}
	
	@Override
	public void render() {
		if (valueSupplier != null) {
			value = valueSupplier.get();
		}
		value &= 0xffff;
		p.fill(thingyColor);
		p.stroke(0);
		p.strokeWeight(1);
		p.pushMatrix();
		p.translate(x, y);
		p.rect(0, 0, thingyWidth, thingyHeight);
		
		p.textAlign(CENTER);
		p.textFont(Emulator.font12, 12);
		p.fill(0);
		p.text(name, thingyWidth * 0.5f, 14);
		
		int temp0 = (value & 0xff00) >> 8;
		int temp1 = value & 0x00ff;
		for (int i = 0; i < 8; i++) {
			if ((temp0 & 0x80) > 0) {
				p.fill(lightBlueOn);
			} else {
				p.fill(lightBlueOff);
			}
			p.ellipse(i * 18 + 39, 26, 16, 16);
			if ((temp1 & 0x80) > 0) {
				p.fill(lightYellowOn);
			} else {
				p.fill(lightYellowOff);
			}
			p.ellipse(i * 18 + 35, 44, 16, 16);
			temp0 <<= 1;
			temp1 <<= 1;
		}
		
		if (selected == 1) {
			p.noFill();
			p.stroke(0xff03f4fc);
			p.rect(38, 53, 58, 18);
		}
		else if (selected == 2) {
			p.noFill();
			p.stroke(0xff03f4fc);
			p.rect(100, 53, 78, 18);
		}
		
		p.textFont(Emulator.font48, 24);
		p.stroke(0);
		p.fill(0);
		p.text(String.format("%04x", value), thingyWidth * 0.333f, thingyHeight - 5);
		p.text(value, thingyWidth * 0.7f, thingyHeight - 5);
		
		p.popMatrix();
	}
	
	public void setValue(int value) {
		valueUpdater.accept(value & 0xffff);
	}
	
	@Override
	public void mousePressed() {
		if (p.mouseX > x + 38 && p.mouseX < x + 96 && p.mouseY > y + 53 && p.mouseY < y + 71) {
			selected = 1;
		}
		else if (p.mouseX > x + 100 && p.mouseX < x + 178 && p.mouseY > y + 53 && p.mouseY < y + 71) {
			selected = 2;
		}
		else
		{
			selected = 0;
		}
	}
	
	@Override
	public void mouseReleased() {
		
	}
	
	@Override
	public void keyPressed() {
		if (valueUpdater == null || selected == 0) return;
		if (selected == 1) {
			if (p.key >= '0' && p.key <= '9') {
				value = (value << 4) & 0xffff;
				value |= p.key - '0';
			} else if (p.key >= 'a' && p.key <= 'f') {
				value = (value << 4) & 0xffff;
				value |= p.key - 'a' + 0x0a;
			} else if (p.key >= 'A' && p.key <= 'F') {
				value = (value << 4) & 0xffff;
				value |= p.key - 'A' + 0x0a;
			}
			valueUpdater.accept(value);
		}
		else if (selected == 2) {
			if (p.key == BACKSPACE || p.key == DELETE) {
				value /= 10;
			}
			else if (p.key >= '0' && p.key <= '9' && value < 10000) {
				value *= 10;
				value += p.key - '0';
			}
			valueUpdater.accept(value & 0xffff);
		}
	}
	
	@Override
	public void keyReleased() {
		
	}
	
}
