package com.flowbish.dcpu16;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class Clock extends Hardware {
	private Timer clockTimer;
	private long lastTime = 0;
	private int clockSpeed = 0;
	private char inturrruptMessage = 0;
	
	public void interrupt() {
		switch (getCPU().getA()) {
		case 0:
			//		The B register is read, and the clock will tick 60/B times per second.
			//		If B is 0, the clock is turned off.
			clockSpeed = getCPU().getB();
			setClockRun();
			lastTime = System.currentTimeMillis();
			break;
		case 1:
			//		Store number of ticks elapsed since last call to 0 in C register
			if (clockSpeed != 0) {
				long now = System.currentTimeMillis();
				char ticks = (char) ((now - lastTime) / 1000 * clockSpeed);
				getCPU().setC(ticks);
			}
			break;
		case 2:
			//		If register B is non-zero, turn on interrupts with message B. If B is zero,
			//		disable interrupts
			inturrruptMessage = getCPU().getB();
			break;
		default:
			break;
		}
	}
	
	private void setClockRun() {
		clockTimer.cancel();
		if (clockSpeed != 0) {
			clockTimer = new Timer();
			clockTimer.scheduleAtFixedRate(new clockTask(), 0, (clockSpeed * 1000) / 60);
		}		
	}

	private class clockTask extends TimerTask {
		public void run() {
			if (inturrruptMessage != 0)
				pushInterrupt(inturrruptMessage);
		}
	}

}
