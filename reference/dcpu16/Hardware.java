package com.flowbish.dcpu16;

public abstract class Hardware {
	public final int manufacturer;
	public final int hardwareID;
	public final int hardwareVersion;
	private DCPU cpu;
	
	public Hardware(DCPU cpu) {
		this.cpu = cpu;
		manufacturer = 0;
		hardwareID = 0;
		hardwareVersion = 0;
	}
	
	public Hardware() {
		manufacturer = 0;
		hardwareID = 0;
		hardwareVersion = 0;
	}
	
	public abstract void interrupt();
	
	protected final void pushInterrupt(char message) {
		cpu.sendInterrupt(message);
	}
	
	public DCPU getCPU() {
		return cpu;
	}
	
	public void setCPU(DCPU value) {
		cpu = value;
	}
}
