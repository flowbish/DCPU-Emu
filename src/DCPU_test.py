'''
Created on Sep 2, 2013

@author: Porter
'''
import struct

from DCPU import DCPU

def print_info(dcpu):
    s = "["
    for reg, i in dcpu.register_index.items():
        s += reg + "=" + hex(dcpu.registers[i]) + " "
    s += "] "
    s += "cyles=" + str(dcpu.cycles)
    s += '\n'
    s += str(dcpu.memory.items())
    print(s)
    
def load_program(path):
    with open(path,'rb') as f:
        prog = []
        b = f.read(2)
        while b:
            prog.append(b[1] + (b[0] << 8))
            b = f.read(2)
        return prog
    
testprog2 = [0x9001, 0x7c21, 0x0033, 0x8803, 0x0442, 0x8413, 0x9381, 0x8b83]

if __name__ == '__main__':
    #prog = load_program('../dcpu_test.bin')
    prog = testprog2
    print([hex(a) for a in prog])
    d = DCPU.DCPU()
    d.load_program(prog)
    steps = 1
    for i in range(17):
        print('--------STEP '+str(steps)+'----------')
        d.step()
        print_info(d)
        steps += 1