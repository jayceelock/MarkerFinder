import json

from ctypes import *
from sys import getsizeof
from binascii import *

# addr = long(6763272))
addr = 0x673308
size = 1000

print hexlify(string_at(id(addr), size=size)).decode("hex")
