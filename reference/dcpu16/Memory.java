package com.flowbish.dcpu16;

/**
 * Memory storage thingy for DCPU-16
 * @author Flowbish
 *
 */
public class Memory {
	private char[] memory;
	
	public final int MAX_MEMORY = 0x10000;
	
	public Memory() {
		memory = new char[MAX_MEMORY];
	}
	
	/**
	 * Sets memory address <code>addr</code> to <code>value</code>
	 * @param addr - memory address to write to
	 * @param value - 16 bit integer to write
	 */
	public void setAddress(char addr, char value) {
		memory[addr] = (char)value;
		
	}
	
	/**
	 * Returns the value at memory address <code>addr</code>
	 * @param addr - memory address to return
	 * @return 16 bit integer
	 */
	public char getAddress(char addr) {
		return memory[addr];
	}

}
