package net.scheffers.robot.emu.modules;

import jutils.guiv2.GUIElement;
import net.scheffers.robot.emu.EMUConstants;
import net.scheffers.robot.emu.Emulator;
import processing.core.PApplet;
import processing.core.PConstants;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class Register8Bit extends GUIElement implements PConstants, EMUConstants {
	
	public Supplier<Integer> valueSupplier;
	public Consumer<Integer> valueUpdater;
	public int value;
	public String name;
	public int selected;
	public boolean isNegative;
	
	public Register8Bit(PApplet p, int x, int y, String name) {
		super(p, x, y, thingyWidth, thingyHeight);
		this.name = name;
	}
	
	@Override
	public void render() {
		if (valueSupplier != null) {
			value = valueSupplier.get();
		}
		value &= 0xff;
		
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
		
		int temp = value;
		for (int i = 0; i < 8; i++) {
			if ((temp & 0x80) > 0) {
				p.fill(lightRedOn);
			} else {
				p.fill(lightRedOff);
			}
			p.ellipse(i * 18 + 37, 34, 16, 16);
			temp <<= 1;
		}
		
		if (selected == 1) {
			p.noFill();
			p.stroke(0xff03f4fc);
			p.rect(26, 53, 28, 18);
		}
		else if (selected == 2) {
			p.noFill();
			p.stroke(0xff03f4fc);
			p.rect(78, 53, 43, 18);
		}
		else if (selected == 3) {
			p.noFill();
			p.stroke(0xff03f4fc);
			p.rect(131, 53, 57, 18);
		}
		
		p.textFont(Emulator.font48, 24);
		p.stroke(0);
		p.fill(0);
		p.text(String.format("%02x", value), thingyWidth * 0.20f, thingyHeight - 5);
		p.text(value, thingyWidth * 0.50f, thingyHeight - 5);
		if (isNegative && value == 0) {
			p.text("-", thingyWidth * 0.80f, thingyHeight - 5);
		} else {
			p.text((byte) value, thingyWidth * 0.80f, thingyHeight - 5);
		}
		
		p.popMatrix();
	}
	
	public void setValue(int value) {
		valueUpdater.accept(value & 0xff);
	}
	
	@Override
	public void mousePressed() {
		if (p.mouseX > x + 26 && p.mouseX < x + 54 && p.mouseY > y + 53 && p.mouseY < y + 71) {
			selected = 1;
		}
		else if (p.mouseX > x + 78 && p.mouseX < x + 121 && p.mouseY > y + 53 && p.mouseY < y + 71) {
			selected = 2;
		}
		else if (p.mouseX > x + 131 && p.mouseX < x + 188 && p.mouseY > y + 53 && p.mouseY < y + 71) {
			selected = 3;
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
				value = (value << 4) & 0xff;
				value |= p.key - '0';
			} else if (p.key >= 'a' && p.key <= 'f') {
				value = (value << 4) & 0xff;
				value |= p.key - 'a' + 0x0a;
			} else if (p.key >= 'A' && p.key <= 'F') {
				value = (value << 4) & 0xff;
				value |= p.key - 'A' + 0x0a;
			}
			valueUpdater.accept(value);
		}
		else if (selected == 2) {
			if (p.key == BACKSPACE || p.key == DELETE) {
				value /= 10;
			}
			else if (p.key >= '0' && p.key <= '9' && value < 100) {
				value *= 10;
				value += p.key - '0';
			}
			valueUpdater.accept(value & 0xff);
		}
		else if (selected == 3) {
			if (p.key == BACKSPACE || p.key == DELETE) {
				isNegative = (byte) value < 0;
				value = (byte) value / 10;
			}
			else if (p.key >= '0' && p.key <= '9' && Math.abs((byte) value) < 100) {
				if (isNegative && value == 0) {
					value -= p.key - '0';
				} else {
					value = (byte) value * 10;
					if (value >= 0) {
						value += p.key - '0';
					} else {
						value -= p.key - '0';
					}
				}
			}
			else if (p.key == '-') {
				value = -value;
				isNegative = !isNegative;
			}
			valueUpdater.accept(value & 0xff);
		}
	}
	
	@Override
	public void keyReleased() {
		
	}
	
}
