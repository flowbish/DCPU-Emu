'''
Created on Sep 2, 2013

@author: Porter
'''

from collections import defaultdict

class Memory(object):
    
    def __init__(self, max_adressable_memory=0x10000, max_byte_size=0xffff):
        self._MAX_ADRESSABLE_MEMORY = max_adressable_memory
        self._MAX_BYTE_SIZE = max_byte_size
        self.memory = defaultdict(lambda: 0)
        
    def __setitem__(self, addr, value):
        if addr >= self._MAX_ADRESSABLE_MEMORY:
            raise IndexError
        self.memory[addr] = value % self._MAX_BYTE_SIZE
        
    def __getitem__(self, addr):
        if addr >= self._MAX_ADRESSABLE_MEMORY:
            raise IndexError
        return self.memory[addr]
    
    def __delitem__(self, addr):
        if addr >= self._MAX_ADRESSABLE_MEMORY:
            raise IndexError
        self.memory[addr] = 0
        
    def keys(self):
        return self.memory.keys()
    
    def items(self):
        return self.memory.items()
        
    def load_program(self, program):
        self.memory = defaultdict(lambda: 0, zip(range(len(program)), program))