package net.scheffers.robot.emu.modules;

import jutils.guiv2.GUIElement;
import net.scheffers.robot.emu.GR8CPURev3_1;
import net.scheffers.robot.emu.GR8EMUConstants;
import net.scheffers.robot.emu.GR8EMUr3_1;
import processing.core.PApplet;
import processing.core.PConstants;

public class RegisterFlags extends GUIElement implements PConstants, GR8EMUConstants {
	
	public String name;
	
	public RegisterFlags(PApplet p, int x, int y, String name) {
		super(p, x, y, thingyWidth, thingyHeight);
		this.name = name;
	}
	
	@Override
	public void render() {
		p.fill(thingyColor);
		p.stroke(0);
		p.strokeWeight(1);
		p.pushMatrix();
		p.translate(x, y);
		p.rect(0, 0, thingyWidth, thingyHeight);
		
		p.textAlign(CENTER);
		p.textFont(GR8EMUr3_1.font12, 12);
		p.fill(0);
		p.text(name, thingyWidth * 0.5f, 14);
		
		GR8CPURev3_1 inst = GR8EMUr3_1.inst.emulator.instance;
		flagBit(0, inst.flagCout, "C");
		flagBit(1, inst.flagZero, "0");
		flagBit(2, inst.flagIRQ, "I");
		flagBit(3, inst.flagNMI, "N");
		flagBit(4, /*inst.*/false, "");
		flagBit(5, /*inst.*/false, "");
		flagBit(6, /*inst.*/false, "");
		flagBit(7, inst.flagHWI, "H");
		
		p.popMatrix();
	}
	
	protected void flagBit(int x, boolean flag, String text) {
		if (flag) {
			p.fill(lightBlueOn);
		} else {
			p.fill(lightBlueOff);
		}
		p.ellipse(35 + 18 * x, 34, 16, 16);
		p.textFont(GR8EMUr3_1.font12, 12);
		p.stroke(0);
		p.fill(0);
		p.text(text, 35 + 18 * x, 54);
	}
	
	@Override
	public void mousePressed() {
		int x = (p.mouseX - this.x - 26) / 18;
		if (p.mouseY < this.y + 25 || p.mouseY > this.y + 43 || x < 0 || x > 7) {
			return;
		}
		GR8CPURev3_1 inst = GR8EMUr3_1.inst.emulator.instance;
		switch (x) {
			case(0): // Carry flag.
				inst.flagCout ^= true;
				break;
			case(1): // Zero flag.
				inst.flagZero ^= true;
				break;
			case(2): // IRQ enable flag.
				inst.flagIRQ ^= true;
				break;
			case(3): // NMI enable flag.
				inst.flagNMI ^= true;
				break;
			case(7): // Hardware interrupt flag.
				inst.flagHWI ^= true;
				break;
		}
	}
	
	@Override
	public void mouseReleased() {
		
	}
	
	@Override
	public void keyPressed() {
		
	}
	
	@Override
	public void keyReleased() {
		
	}
	
}
