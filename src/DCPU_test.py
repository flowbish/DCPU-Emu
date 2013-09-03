'''
Created on Sep 2, 2013

@author: Porter
'''

from DCPU import DCPU

if __name__ == '__main__':
    d = DCPU.DCPU()
    d.load_program([0xb401, 0xe021, 0x0022])
    d.step()
    print(d.registers)
    d.step()
    print(d.registers)
    d.step()
    print(d.registers)