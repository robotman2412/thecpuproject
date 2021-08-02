package emur3;

import jutils.guiv2.Button;
import processing.core.PApplet;

public class GR8EMUJamHandler extends PApplet {
	
	Button openDebugger;
	
	public GR8EMUJamHandler() {
		
	}
	
	public void settings() {
		size(370, 130);
	}
	
	public void setup() {
		surface.setTitle("CPU jam condition");
		Runnable debugMode = new Runnable() {
			public void run() {
				exit();
				PApplet.runSketch(new String[] {"GR8EMUDebugger"}, new GR8EMUDebugger());
			}
		};
		openDebugger = new Button(this, 5, 105, 50, 20, "Open debugger...", false, debugMode);
	}
	
	public void draw() {
		if (!GR8EMU.jam) {
			exit();
		}
		background(255);
		fill(0);
		textAlign(CORNER);
		text(String.format("PC: $%04x, Inst: $%02x, Stack size: %d\nJam reason:\n%s: %s", GR8EMU.PC, GR8EMU.IR, GR8EMU.stackPointer & 0xff, GR8EMU.jamCode, GR8EMU.jamCondition), 2, 14);
		openDebugger.render();
	}
	
	public void mousePressed() {
		openDebugger.mousePressed();
	}
	
	public void mouseReleased() {
		openDebugger.mouseReleased();
	}
	
	@Override
	public void exit() {
		surface.stopThread();
		surface.setVisible(false);
	}
	
}
