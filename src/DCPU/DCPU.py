'''
Created on Sep 2, 2013

@author: Porter
'''

from collections import defaultdict

from .Memory import Memory

def memoize(function):
    value = None
    def return_function():
        nonlocal value
        if not value:
            value = function()
        return value
    return return_function
                      
class DCPU(object):
    '''
    The general DCPU class
    '''
    
    register_index = {"A":0, "B":1, "C":2,  "X":3,  "Y":4,   "Z":5,
                      "I":6, "J":7, "PC":8, "SP":9, "EX":10, "IA":11}

    def __init__(self):
        '''
        Constructor
        '''
        self.reset()
        
    def __setattr__(self, name, value):
        if name in self.register_index:
            self.registers[self.register_index[name]] = value
        super(DCPU, self).__setattr__(name, value)
        
    def __getattr__(self, name):
        if name in self.register_index:
            return self.registers[self.register_index[name]]
        super(DCPU, self).__setattr__(name)
        
    def load_program(self, program):
        if type(program) == list:
            print('loading program ' + str([hex(a) for a in program]))
            self.memory.load_program(program)
        
    def reset(self):
        self.memory = Memory(0x10000)
        # Registers: [A,B,C,X,Y,Z,I,J,PC,SP,EX,IA]
        self.registers = Memory(12)
        self.cycles = 0
        self.queueing = False
        self.hardware = {}
        self.interrupts = {}
        
    def next_word(self):
        addr = self.PC
        self.PC += 1
        return self.memory[addr]
    
    def step(self):
        #=======================================================================
        # if not self.queueing and self.interrupts.size() > 0:
        #     stackPush((char) (getPC()))
        #     stackPush(getA())
        #     setPC(getIA())
        #     setA(interrupts.remove(0))
        #=======================================================================
        instruction = self.next_word()
        opcode = instruction & 0x1f
        print('opcode: '+hex(opcode))
        # Basic Opcode
        if (opcode != 0x0):
            b = ((instruction >> 5) & 0x1f)     # word = [aaaaaa][bbbbb][ooooo] 
            a = ((instruction >> 10) & 0x3f)    #           |       |      \5bit opcode
            self.basic_op(opcode, b, a)         #           |     5bit operand
                                                #        6 bit operand
        # Non-basic Opcode
        else:
            opcode = ((instruction >> 5) & 0x1f)# word = [aaaaaa][ooooo][00000] 
            a = ((instruction >> 10) & 0x3f)    #           |       |      \NULL
            self.non_basic_op(opcode, a)        #           |     5bit opcode
                                                #        6 bit operand
            
    def basic_op(self, opcode, b, a):
        b_read, b_write = self.operand(b)
        a_read, a_write = self.operand(a)
        print('basic op '+hex(opcode)+': '+hex(b)+', '+hex(a))
        # SET b, a
        if opcode == 0x01:
            self.cycles += 1
            b_write(a_read())
        # ADD b, a
        elif opcode == 0x02:
            self.cycles += 2
            fsum = b_read() + a_read()
            if fsum > 0xffff :
                self.EX = 0x0001
            else:
                self.EX = 0x0000
            b_write(fsum);
        # SUB b, a
        elif opcode == 0x03:
            self.cycles += 2
            fdif = b_read() - a_read()
            if fdif < 0x0:
                self.EX = 0xffff
            else:
                self.EX = 0x0000
            b_write(fdif)
        # MUL b, a
        elif opcode == 0x04:
            self.cycles += 2
            fprod = b_read() * a_read()
            self.EX = ((fprod >> 16) & 0xffff)
            b_write(fprod)
        # DIV b, a
        elif opcode == 0x06:
            self.cycles += 3
            fdiv = b_read() // a_read()
            if a_read() != 0x0:
                self.EX = (((b_read() << 16) // a_read()) & 0xffff)
                b_write(fdiv)
            else:
                self.EX = 0x0000
                b_write(0x0)
        # MOD b, a
        elif opcode == 0x08:
            self.cycles += 3
            if a_read() == 0:
                b_write(0x0)
            else:
                b_write(b_read() % a_read())
        # AND b, a
        elif opcode == 0x0a:
            self.cycles += 1
            b_write(b_read() & a_read())
        # BOR b, a
        elif opcode == 0x0b:
            self.cycles += 1
            b_write(b_read() | a_read())
#         # XOR b, a
#         elif opcode == 0x0c:
#             self.cycles += 1;
#             bop.write((char) (bop.read() ^ aop.read()));
#         # SHR b, a
#         elif opcode == 0x0d:
#             self.cycles += 1;
#             bop.write((char) (bop.read() >> aop.read()));
#             setEX((char) (((bop.read() << 16) >> aop.read()) & 0xffff));
#         # SHL b, a
#         elif opcode == 0x0f:
#             self.cycles += 1;
#             bop.write((char) (bop.read() << aop.read()));
#             setEX((char) (((bop.read() << aop.read()) >> 16) & 0xffff));
#         # IFB b, a
#         elif opcode == 0x10:
#             self.cycles += 1;
#             boolean result = (bop.read() & aop.read()) != 0;
#             if (result) 
#                 break;
#             else {
#                 // TODO: Chain conditionals
#                 nextWord();
#             }
        # IFE b, a
        elif opcode == 0x12:
            self.cycles += 2
            if b_read() == a_read():
                pass
            else:
                # TODO: Chain conditionals
                self.next_word();
        # IFN b, a
        elif opcode == 0x13:
            self.cycles += 2
            if b_read() != a_read():
                pass
            else:
                # TODO: Chain conditionals
                self.next_word();
#         # ADX b, a
#         elif opcode == 0x1a:
#             self.cycles += 3;
#             int sx = bop.read() + aop.read() + getEX();
#             if (sx > 0xffff) 
#                 setEX((char) 0x0001);
#             else
#                 setEX((char) 0x0000);
#             bop.write((char) sx);
#         # SBX b, a
#         elif opcode == 0x1b:
#             self.cycles += 3;
#             int dx = bop.read() - aop.read() + getEX();
#             if (dx < 0x0) 
#                 setEX((char) 0xffff);
#             else
#                 setEX((char) 0x0000);
#             bop.write((char) dx);
            
    
    def non_basic_op(self, opcode, a):
        print('non-basic op '+hex(opcode)+': '+hex(a))
#         // JSR a
#         case 0x01:
#             cycles += 3;
#             stackPush((char) (getPC() + 1));
#             setPC(aop.read());
#             break;
#         // INT a
#         case 0x08:
#             cycles += 4;
#             sendInterrupt(aop.read());
#             break;
#         // IAG a
#         case 0x09:
#             cycles += 1;
#             aop.write(getIA());
#             break;
#         // IAS a
#         case 0x0a:
#             cycles += 1;
#             setIA(aop.read());
#             break;
#         // RFI a
#         case 0x0b:
#             cycles += 3;
#             setA(stackPop());
#             setPC(stackPop());
#             queueing = false;
#             break;
#         // IAQ a
#         case 0x0c:
#             cycles += 2;
#             if (aop.read() > 0)
#                 queueing = true;
#             else
#                 queueing = false;
#             break;
#         // HWN a
#         case 0x10:
#             cycles += 2;
#             aop.write((char) hardware.size());
#             break;
#         // HWQ a
#         case 0x11:
#             cycles += 4;
#             Hardware h = hardware.get(aop.read());
#             setA((char) (h.hardwareID & 0xffff));
#             setB((char) ((h.hardwareID>>16) & 0xffff));
#             
#             setC((char) h.hardwareVersion);
#             
#             setX((char) (h.manufacturer & 0xffff));
#             setY((char) ((h.manufacturer>>16) & 0xffff));
#             break;
#         // HWI a
#         case 0x12:
#             cycles += 4;
#             hardware.get(aop.read()).interrupt();
#             break;
#         }
    
    def operand(self, a):
        # register[A,B,C,X,Y,Z,I,J]
        if a < 0x08 :
            def read(): return self.registers[a]
            def write(v): self.registers[a] = v
            return (read, write)
        # [register]
        elif a < 0x10:
            r = a - 0x08
            def read(): return self.memory[self.registers[r]]
            def write(v): self.memory[self.registers[r]] = v
            return (read, write)
        # PUSH or POP
        elif a == 0x18:
            @memoize
            def read(): return self.stack_pop()
            def write(v): self.stack_push(v)
            return (read, write)
        # PEEK
        elif a == 0x19:
            def read(): return self.memory[self.SP]
            def write(v): self.memory[self] = v
            return (read, write)
        # PICK n
        elif a == 0x1a:
            self.cycles += 1
            word = self.next_word();
            def read(): return self.memory[self.SP + word]
            def write(v): self.memory[self.SP + word] = v
            return (read, write)
        # PC
        elif a == 0x1c:
            def read(): return self.PC
            def write(v): self.PC = v
            return (read, write)
        # next word
        elif a  == 0x1f:
            self.cycles += 1
            word = self.next_word()
            def read(): return word
            def write(v): pass
            return (read, write)
        # values [0xffff,0x1e]
        else:
            r = a - 0x21;
            def read(): return r
            def write(v): pass
            return (read, write)
        
    def stack_push(self, v):
        self.SP -= 1
        self.memory[self.SP] = v
        
    def stack_pop(self):
        v = self.memory[self.SP]
        self.SP += 1
        return v