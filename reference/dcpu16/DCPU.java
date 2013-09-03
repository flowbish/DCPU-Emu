package com.flowbish.dcpu16;

import java.util.ArrayList;
import java.util.List;

/**
 * CPU of the DCPU-16
 * @author Flowbish
 * @version not finished yet
 *
 */
public class DCPU {
	private Memory memory;
	private char[] registers;
	private char SP;
	private char PC;
	private char EX;
	private char IA;
	private long cycles;
	private boolean queueing;
	
	private List<Hardware> hardware;	
	private List<Character> interrupts;
	
	public DCPU() {
		memory = new Memory();
		reset();
	}
	
	/**
	 * Loads a program from a byte array and pushes it into
	 * memory starting at 0x0000
	 * @param program - byte array of instructions, two bytes
	 * 	                per instruction
	 */
	public void loadProgram(byte[] program) {
		for (int i = 0; i < program.length; i += 2) {
			char value = (char) ((program[i]) << 8);
			if (i + 1 < program.length) {
				if (program[i+1] >= 0)
					value += program[i+1];
				else
					value += program[i+1] + 0x100;
			}
			memory.setAddress((char) (i/2), value);
		}
	}
	
	private void reset() {
		memory = new Memory();
		registers = new char[8];
		SP = 0x0;
		PC = 0x0;
		EX = 0x0;
		IA = 0x0;
		cycles = 0;
		queueing = false;
		
		hardware = new ArrayList<Hardware>();
		interrupts = new ArrayList<Character>();
	}
	
	private char nextWord() {
		char addr = getPC();
		setPC((char) (addr + 1));
		return memory.getAddress(addr);
	}
	
	public void step() {
		if (!queueing && interrupts.size() > 0) {
			stackPush((char) (getPC()));
			stackPush(getA());
			setPC(getIA());
			setA(interrupts.remove(0));
		}
		char instruction = nextWord();
		char opcode = (char) (instruction & 0x1f);
		if (opcode != 0x0) { // Basic Opcode
			char b = (char)((instruction >> 5) & 0x1f);
			char a = (char)((instruction >> 10) & 0x3f);
			basicOp(opcode, b, a);
		}
		else { // Non-basic Opcode
			opcode = (char)((instruction >> 5) & 0x1f);
			char a = (char)((instruction >> 10) & 0x1f); 
			nonBasicOp(opcode, a);
		}
	}
	
	/**
	 * Performs a basic instruction.
	 * @param opcode  the opcode of the instruction (lower 5 bits)
	 * @param b - the b value of the instruction (middle 5 bits)
	 * @param a - the a value of the instruction (upper 6 bits)
	 */
	private void basicOp(char opcode, char b, char a) {
		Writable aop = operand(a);
		Writable bop = operand(b);
		switch(opcode) {
		// SET b, a
		case 0x01:
			cycles += 1;
			bop.write(aop.read());
			break;
		// ADD b, a
		case 0x02:
			cycles += 2;
			int sum = bop.read() + aop.read();
			if (sum > 0xffff) 
				setEX((char) 0x0001);
			else
				setEX((char) 0x0000);
			bop.write((char) sum);
			break;
		// SUB b, a
		case 0x03:
			cycles += 2;
			int dif = bop.read() - aop.read();
			if (dif < 0x0) 
				setEX((char) 0xffff);
			else
				setEX((char) 0x0000);
			bop.write((char) dif);
			break;
		// MUL b, a
		case 0x04:
			cycles += 2;
			int prod = bop.read() * aop.read();
			setEX((char) ((prod >> 16) & 0xffff));
			bop.write((char) prod);
			break;
		// DIV b, a
		case 0x06:
			cycles += 3;
			int div = bop.read() / aop.read();
			if (aop.read() != 0x0) {
				setEX((char) (((bop.read() << 16) / aop.read()) & 0xffff));
				bop.write((char) div);
			}
			else {
				setEX((char) 0x0000);
				bop.write((char) 0x0);
			}
			break;
		// MOD b, a
		case 0x08:
			cycles += 3;
			if (aop.read() == 0)
				bop.write((char) 0x0);
			else
				bop.write((char) (bop.read() % aop.read()));
			break;
		// AND b, a
		case 0x0a:
			cycles += 1;
			bop.write((char) (bop.read() & aop.read()));
			break;
		// BOR b, a
		case 0x0b:
			cycles += 1;
			bop.write((char) (bop.read() | aop.read()));
			break;
		// XOR b, a
		case 0x0c:
			cycles += 1;
			bop.write((char) (bop.read() ^ aop.read()));
			break;
		// SHR b, a
		case 0x0d:
			cycles += 1;
			bop.write((char) (bop.read() >> aop.read()));
			setEX((char) (((bop.read() << 16) >> aop.read()) & 0xffff));
			break;
		// SHL b, a
		case 0x0f:
			cycles += 1;
			bop.write((char) (bop.read() << aop.read()));
			setEX((char) (((bop.read() << aop.read()) >> 16) & 0xffff));
			break;
		// IFB b, a
		case 0x10:
			cycles += 1;
			boolean result = (bop.read() & aop.read()) != 0;
			if (result) 
				break;
			else {
				// TODO: Chain conditionals
				nextWord();
			}
			break;
		// IFE b, a
		case 0x12:
			cycles += 1;
			result = (bop.read() == aop.read());
			if (result) 
				break;
			else {
				// TODO: Chain conditionals
				nextWord();
			}
			break;
		// ADX b, a
		case 0x1a:
			cycles += 3;
			int sx = bop.read() + aop.read() + getEX();
			if (sx > 0xffff) 
				setEX((char) 0x0001);
			else
				setEX((char) 0x0000);
			bop.write((char) sx);
			break;
		// SBX b, a
		case 0x1b:
			cycles += 3;
			int dx = bop.read() - aop.read() + getEX();
			if (dx < 0x0) 
				setEX((char) 0xffff);
			else
				setEX((char) 0x0000);
			bop.write((char) dx);
			break;
		}
	}
	
	/**
	 * Performs a non-basic instruction, that is, where the lowest 5 bits are unset.
	 * @param opcode - the opcode of the instruction (middle 5 bits)
	 * @param a - the a value of the instruction (upper 6 bits)
	 */
	private void nonBasicOp(char opcode, char a) {
		Writable aop = operand(a);
		switch (opcode) {
		// JSR a
		case 0x01:
			cycles += 3;
			stackPush((char) (getPC() + 1));
			setPC(aop.read());
			break;
		// INT a
		case 0x08:
			cycles += 4;
			sendInterrupt(aop.read());
			break;
		// IAG a
		case 0x09:
			cycles += 1;
			aop.write(getIA());
			break;
		// IAS a
		case 0x0a:
			cycles += 1;
			setIA(aop.read());
			break;
		// RFI a
		case 0x0b:
			cycles += 3;
			setA(stackPop());
			setPC(stackPop());
			queueing = false;
			break;
		// IAQ a
		case 0x0c:
			cycles += 2;
			if (aop.read() > 0)
				queueing = true;
			else
				queueing = false;
			break;
		// HWN a
		case 0x10:
			cycles += 2;
			aop.write((char) hardware.size());
			break;
		// HWQ a
		case 0x11:
			cycles += 4;
			Hardware h = hardware.get(aop.read());
			setA((char) (h.hardwareID & 0xffff));
			setB((char) ((h.hardwareID>>16) & 0xffff));
			
			setC((char) h.hardwareVersion);
			
			setX((char) (h.manufacturer & 0xffff));
			setY((char) ((h.manufacturer>>16) & 0xffff));
			break;
		// HWI a
		case 0x12:
			cycles += 4;
			hardware.get(aop.read()).interrupt();
			break;
		}
	}
	
	private Writable operand(final char a) {
		//register[A,B,C,X,Y,Z,I,J]
		if (a < 0x8) {
			return new Writable() {
				public void write(char value) {
					registers[a] = value;
					
				}
				public char read() {
					return registers[a];
				}
			};
		}
		// [register]
		else if (a < 0x10) {
			final int r = a - 0x8;
			return new Writable() {
				public void write(char value) {
					memory.setAddress(registers[r], value);
					
				}
				public char read() {
					return memory.getAddress(registers[r]);
				}
			};
		}
		// [register + next word]
		else if (a < 0x18) {
			final char word = nextWord();
			final int r = a - 0x10;
			return new Writable() {
				public void write(char value) {
					memory.setAddress((char) (registers[r] + word), value);
					
				}
				public char read() {
					return memory.getAddress((char) (registers[r] + word));
				}
			};
		}
		// PUSH or POP
		else if (a == 0x18) {
			return new Writable() {
				private Character data = null;
				public void write(char value) { // PUSH
					stackPush(value);
				}
				public char read() { // POP
					if (data == null) {
						data = stackPop();
					}
					return data;
				}
			};
		}
		// PEEK
		else if (a == 0x19) {
			return new Writable() {
				public void write(char value) {
					memory.setAddress(getSP(), value);
				}
				public char read() {
					return memory.getAddress(getSP());
				}
			};
		}
		// PICK n
		else if (a == 0x1a) {
			cycles += 1;
			final char word = nextWord();
			return new Writable() {
				public void write(char value) {
					memory.setAddress((char) (getSP() + word), value);
				}
				public char read() {
					return memory.getAddress((char) (getSP() + word));
				}
			};
		}
		// SP
		else if (a == 0x1b) {
			return new Writable() {
				public void write(char value) {
					setSP(value);
				}
				public char read() {
					return getSP();
				}
			};
		}
		// PC
		else if (a == 0x1c) {
			return new Writable() {
				public void write(char value) {
					setPC(value);
				}
				public char read() {
					return getPC();
				}
			};
		}
		// EX
		else if (a == 0x1d) {
			return new Writable() {
				public void write(char value) {
					setEX(value);
				}
				public char read() {
					return getEX();
				}
			};
		}
		// [next word]
		else if (a  == 0x1e) {
			cycles += 1;
			final char word = nextWord();
			return new Writable() {
				public void write(char value) {
					memory.setAddress(word, value);
				}
				public char read() {
					return memory.getAddress(word);
				}
			};
		}
		// next word
		else if (a  == 0x1f) {
			cycles += 1;
			final char word = nextWord();
			return new Writable() {
				public void write(char value) {
					// Literal Assignment
				}
				public char read() {
					return word;
				}
			};
		}
		// values [0xffff,0x1e]
		else {
			final int r = a - 0x21;
			return new Writable() {
				public void write(char value) {
					// Cannot write to a literal
				}
				public char read() {
					return (char) r;
				}
			};
		}
	}
	
	
	/**
	 * Send an interrupt to the DCPU
	 * @param message - 16 bit word message
	 */
	public void sendInterrupt(char message) {
		interrupts.add(message);
	}
	
	public void addHardware(Hardware h) {
		h.setCPU(this);
		hardware.add(h);
	}
	
	/**
	 * Returns instance of Memory used by this CPU
	 * @return instance of Memory used by this CPU
	 */
	public Memory getMemory() {
		return memory;
	}
	public char getA() {
		return registers[0];
	}
	public void setA(char value) {
		registers[0] = value;
	}
	public char getB() {
		return registers[1];
	}
	public void setB(char value) {
		registers[1] = value;
	}
	public char getC() {
		return registers[2];
	}
	public void setC(char value) {
		registers[2] = value;
	}
	public char getX() {
		return registers[3];
	}
	public void setX(char value) {
		registers[3] = value;
	}
	public char getY() {
		return registers[4];
	}
	public void setY(char value) {
		registers[4] = value;
	}
	public char getZ() {
		return registers[5];
	}
	public void setZ(char value) {
		registers[5] = value;
	}
	public char getI() {
		return registers[6];
	}
	public void setI(char value) {
		registers[6] = value;
	}
	public char getJ() {
		return registers[7];
	}
	public void setJ(char value) {
		registers[7] = value;
	}
	public char getSP() {
		return SP;
	}
	public void setSP(char value) {
		SP = value;
	}
	private void stackPush(char value) {
		setSP((char) (getSP() - 1));
		memory.setAddress(getSP(), value);
	}
	private char stackPop() {
		char sp = getSP();
		setSP((char) (sp + 1));
		return memory.getAddress(sp);
	}
	public char getPC() {
		return PC;
	}
	public void setPC(char value) {
		PC = value;
	}
	public char getEX() {
		return EX;
	}
	public void setEX(char value) {
		EX = value;
	}
	public char getIA() {
		return IA;
	}
	public void setIA(char value) {
		IA = value;
	}
	public long getCycles() {
		return cycles;
	}
}
