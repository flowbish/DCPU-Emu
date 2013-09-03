'''
Created on Sep 2, 2013

@author: Porter
'''

from collections import defaultdict

class Memory(object):

    def __init__(self):
        self._MAX_MEMORY = 0x10000
        self.memory = defaultdict(lambda: 0)
        
    def __setitem__(self, addr, value):
        if addr >= self._MAX_MEMORY:
            raise IndexError
        self.memory[addr] = value
        
    def __getitem__(self, addr):
        if addr >= self._MAX_MEMORY:
            raise IndexError
        return self.memory[addr]
    
    def __delitem__(self, addr):
        if addr >= self._MAX_MEMORY:
            raise IndexError
        self.memory[addr] = 0
        
    def load_program(self, program):
        self.memory = defaultdict(lambda: 0, zip(range(len(program)), program))