'''
Created on Sep 2, 2013

@author: Porter
'''

from collections import defaultdict

from Memory import Memory
                      
class DCPU(object):
    '''
    The general DCPU class
    '''


    def __init__(self):
        '''
        Constructor
        '''
        self.reset()
        
    def load_program(self, program):
        if type(program) == list:
            print('loading program ' + str(program))
            self.memory.load_program(program)
        
    def reset(self):
        self.memory = Memory()
        self.registers = defaultdict(lambda: 0)
        self.SP = 0
        self.PC = 0
        self.EX = 0
        self.IA = 0
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
        print('opcode: '+`opcode`)
        if (opcode != 0x0):   #Basic Opcode
            b = ((instruction >> 5) & 0x1f)
            a = ((instruction >> 10) & 0x3f)
            self.basic_op(opcode, b, a)
        else: # Non-basic Opcode
            opcode = ((instruction >> 5) & 0x1f)
            a = ((instruction >> 10) & 0x1f)
            self.non_basic_op(opcode, a)
            
    def basic_op(self, opcode, b, a):
        b_read, b_write = self.operand(b)
        a_read, a_write = self.operand(a)
        print('basic op: '+`b`+', '+`a`)
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
            
    
    def non_basic_op(self, opcode, a):
        pass
    
    def operand(self, a):
        # register[A,B,C,X,Y,Z,I,J]
        if a < 0x8 :
            def read(): return self.registers[a]
            def write(v): self.registers[a] = v
            return (read, write)
        # [register]
        elif a < 0x10:
            r = a - 0x8
            def read(): return self.memory[self.registers[r]]
            def write(v): self.memory[self.registers[r]] = v
            return (read, write)
        # next word
        elif a  == 0x1f:
            self.cycles += 1
            def read(): return self.next_word()
            def write(v): pass
            return (read, write)
        # values [0xffff,0x1e]
        else:
            r = a - 0x21;
            def read(): return r
            def write(v): pass
            return (read, write)