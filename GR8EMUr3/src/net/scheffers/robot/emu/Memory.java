package net.scheffers.robot.emu;

import jutils.guiv2.GUIElement;
import processing.core.PApplet;
import processing.core.PConstants;

import java.util.function.Supplier;

public class Memory extends GUIElement implements GR8EMUConstants {
	
	public Supplier<Number> addressSource;
	public GR8CPURev3_1 cpu;
	public int address;
	public int offset;
	public int selected = -1;
	public String formatStr = "%04x";
	public String name;
	
	public Memory(PApplet p, int x, int y, String name) {
		super(p, x, y, thingyWidth, thingyHeight * 4);
		this.name = name;
	}
	
	@Override
	public void render() {
		if (addressSource != null) {
			address = addressSource.get().intValue() & 0xffff;
		}
		
		p.fill(thingyColor);
		p.stroke(0);
		p.strokeWeight(1);
		p.pushMatrix();
		p.translate(x, y);
		p.rect(0, 0, thingyWidth, thingyHeight * 4);
		
		p.textAlign(PConstants.CENTER);
		p.textFont(GR8EMUr3_1.font12, 12);
		p.fill(0);
		p.text(name, thingyWidth * 0.5f, 14);
		
		p.fill(0x7fffffff);
		p.rect(38, 18, 147, 275);
		
		p.fill(0);
		p.textAlign(PConstants.LEFT);
		
		offset = address & 0xffffff80;
		int len = 21 * 8;
		if (offset + len >= 65536) {
			offset = 65536 - len;
		}
		
		for (int y = 0; y < 21; y++) {
			p.text(String.format(formatStr, offset + y * 8), 5, 30 + 13 * y);
			for (int x = 0; x < 8; x++) {
				// Find le value.
				int index = offset + y * 8 + x;
				byte val;
				int md;
				if (index < cpu.rom.length) {
					val = cpu.rom[index];
					md = 1;
				} else if ((index & 0xff00) == 0xfe00) {
					val = cpu.readMMIO(index, true);
					md = 2;
				} else {
					val = cpu.ram[index];
					md = 0;
				}
				
				// Draw le background.
				switch (md) {
					case (0):
					default:
						break;
					case(1):
						p.fill(0xffffd6d6);
						p.noStroke();
						p.rect(40 + 18 * x, 19 + 13 * y, 19, 13);
						p.fill(0);
						p.stroke(0);
						break;
					case(2):
						p.fill(0xffffe5b0);
						p.noStroke();
						p.rect(40 + 18 * x, 19 + 13 * y, 19, 13);
						p.fill(0);
						p.stroke(0);
						break;
				}
				
				// Draw le value.
				if (index == selected) {
					p.noStroke();
					p.fill(0xff0000ff);
					p.rect(41 + 18 * x, 19 + 13 * y, 17, 13);
					p.fill(255);
					p.text(String.format("%02x", val), 42 + 18 * x, 30 + 13 * y);
					p.fill(0);
					p.stroke(0);
				} else if (index == address) {
					p.rect(41 + 18 * x, 19 + 13 * y, 16, 12);
					p.fill(255);
					p.text(String.format("%02x", val), 42 + 18 * x, 30 + 13 * y);
					p.fill(0);
				} else {
					p.text(String.format("%02x", val), 42 + 18 * x, 30 + 13 * y);
				}
			}
		}
		
		p.textFont(GR8EMUr3_1.font12, 12);
		
		p.popMatrix();
	}
	
	@Override
	public void mousePressed() {
		int selX = (p.mouseX - x - 41) / 18;
		int selY = (p.mouseY - y - 19) / 13;
		if (selX >= 0 && selX < 8 && selY >= 0 && selY < 21) {
			selected = offset + selY * 8 + selX;
		} else {
			selected = -1;
		}
	}
	
	@Override
	public void mouseReleased() {
		
	}
	
	@Override
	public void keyPressed() {
		if (selected == -1) return;
		int val;
		// Read.
		if (selected < cpu.rom.length) {
			val = cpu.rom[selected];
		} else if ((selected & 0xff00) == 0xfe00) {
			val = cpu.readMMIO(selected, true);
		} else {
			val = cpu.ram[selected];
		}
		// Manipulate.
		if (p.key >= '0' && p.key <= '9') {
			val = (val << 4) & 0xff;
			val |= p.key - '0';
		} else if (p.key >= 'a' && p.key <= 'f') {
			val = (val << 4) & 0xff;
			val |= p.key - 'a' + 0x0a;
		} else if (p.key >= 'A' && p.key <= 'F') {
			val = (val << 4) & 0xff;
			val |= p.key - 'A' + 0x0a;
		} else if (p.keyCode == PConstants.LEFT) {
			selected--;
			selected &= 0xffff;
			return;
		} else if (p.keyCode == PConstants.RIGHT) {
			selected++;
			selected &= 0xffff;
			return;
		} else if (p.keyCode == PConstants.UP) {
			selected -= 8;
			selected &= 0xffff;
			return;
		} else if (p.keyCode == PConstants.DOWN) {
			selected += 8;
			selected &= 0xffff;
			return;
		}
		// Write.
		if (selected < cpu.rom.length) {
			cpu.rom[selected] = (byte) (val & 0xff);
		} else if ((selected & 0xff00) != 0xfe00) {
			cpu.ram[selected] = (byte) (val & 0xff);
		}
	}
	
	@Override
	public void keyReleased() {
		
	}
}
