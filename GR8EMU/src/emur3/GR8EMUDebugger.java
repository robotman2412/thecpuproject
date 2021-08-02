package emur3;

import jutils.guiv2.GUI;
import jutils.guiv2.Button;
import jutils.guiv2.CheckBox;
import jutils.guiv2.GUIScreen;
import jutils.guiv2.Text;
import processing.core.PApplet;
import processing.event.MouseEvent;

public class GR8EMUDebugger extends PApplet {
	
	GUIScreen screen;
	Button stepOnce;
	Button start;
	Button stop;
	Button resetSim;
	MemView memView;
	CheckBox stopOnNop;
	Text tStopOnNop;
	
	public void settings() {
		size(840, 510);
	}
	
	public void setup() {
		surface.setTitle("GR8CPU Rev3 debugger");
		screen = new GUIScreen(this);
		screen.width = width;
		screen.height = height;
		memView = MemView.SIMPLE;
		screen.add(stepOnce = new Button(this, 5, 5, 65, 20, "Step once", false, new Runnable() {
			public void run() {
				GR8EMU.CPUMode = RunMode.ONCE;
				GR8EMU.jam = false;
			}
		}));
		screen.add(start = new Button(this, 75, 5, 65, 20, "Continue", false, new Runnable() {
			public void run() {
				GR8EMU.CPUMode = RunMode.CONTINUE;
				GR8EMU.jam = false;
			}
		}));
		screen.add(stop = new Button(this, 145, 5, 60, 20, "Stop", false, new Runnable() {
			public void run() {
				GR8EMU.CPUMode = RunMode.STOP;
			}
		}));
		screen.add(resetSim = new Button(this, 210, 5, 120, 20, "Reset simulation", false, new Runnable() {
			public void run() {
				GR8EMU.resetSim = true;
				GR8EMU.jam = false;
			}
		}));
		screen.add(stopOnNop = new CheckBox(this, 335, 5, 20, 20, new Runnable() {
			public void run() {
				GR8EMU.stopOnNop = stopOnNop.value;
			}
		}));
		surface.setResizable(true);
	}
	
	int dispofx;
	int dispofy;
	public void draw() {
		background(255);
		dispofx = width - 200;
		dispofy = 0;
		fill(0);
		textAlign(CORNER);
		textSize(12);
		tx("A", GR8EMU.A);
		tx("B", GR8EMU.B);
		tx("C", GR8EMU.C);
		tx("D", GR8EMU.D);
		tx("X", GR8EMU.X);
		tx("Y", GR8EMU.Y);
		tx("PC", GR8EMU.PC);
		tx("IR", GR8EMU.IR);
		tx("Stack pointer", GR8EMU.stackPointer);
		tx("AR", GR8EMU.adrReg);
		tx("IS stage", (byte)GR8EMU.ISTAGE);
		tx("C: " + GR8EMU.cccc);
		screen.render();
		switch (memView) {
			default:
				memView = MemView.SIMPLE;
			case SIMPLE:
				simpleMemView();
				break;
		}
	}
	
	int rowOffs;
	void simpleMemView() {
		int dx = 0;
		int dy = 30;
		textSize(10);
		strokeWeight(1);
		stroke(0);
		for (int y = 0; y < 32; y++) {
			int mY = dy + y * 15;
			fill(0);
			textAlign(CORNER);
			text(String.format("$%04x", (y + rowOffs) * 32), 3, mY + 13);
			textAlign(CENTER);
			for (int x = 0; x < 32; x++) {
				int adr = x + (y + rowOffs) * 32;
				int col0 = 0;
				int col1 = 0;
				int mX = dx + x * 20;
				if (adr == GR8EMU.RAMLastRead) {
					col0 = GUI.coloh(0x00ffff);
				}
				if (adr == GR8EMU.RAMLastWritten) {
					if (col0 == 0) {
						col1 = GUI.coloh(0xff7f00);
					}
					else
					{
						col1 = GUI.coloh(0xff7f00);
					}
				}
				if ((col0 == 0 || col1 == 0) && (adr & 0xff00) == (GR8EMU.stackPointer & 0xff00)) {
					if (col0 == 0) {
						col0 = GUI.coloh(0x007fff);
					}
					else
					{
						col1 = GUI.coloh(0x007fff);
					}
				}
				col0 = col0 != 0 ? col0 : GUI.color(255);
				fill(col0);
				rect(dx + x * 20 + 40, dy + y * 15, 20, 15);
				if (col1 != 0) {
					fill(col1);
					noStroke();
					triangle(mX + 1 + 40, mY + 15, mX + 60, mY + 15, mX + 60, mY + 1);
					stroke(0);
				}
				fill(0);
				text(String.format("%02x", GR8EMU.RAM[adr]), mX + 50, mY + 13);
			}
		}
	}

	void tx(String text, byte val) {
		text(String.format("%s: %02x %d %d", text, val, val, Byte.toUnsignedInt(val)), dispofx + 5, dispofy + 16);
		dispofy += 14;
	}
	
	void tx(String text) {
		text(text, dispofx + 5, dispofy + 16);
		dispofy += 14;
	}
	
	void tx(String text, int val) {
		text(String.format("%s: %04x %d %d", text, val, (int)(short)val, val), dispofx + 5, dispofy + 16);
		dispofy += 14;
	}
	
	public void mousePressed() {
		screen.mousePressed();
	}
	
	public void mouseReleased() {
		screen.mouseReleased();
	}
	
	public void keyPressed() {
		screen.keyPressed();
	}
	
	public void keyReleased() {
		screen.keyReleased();
	}
	
	@Override
	public void exit() {
		surface.stopThread();
		surface.setVisible(false);
	}
	
	@Override
	public void mouseWheel(MouseEvent event) {
		rowOffs += event.getCount() * 32;
		rowOffs = rowOffs < 0 ? 0 : rowOffs;
		rowOffs = rowOffs > 2015 ? 2015 : rowOffs;
	}
	
}
