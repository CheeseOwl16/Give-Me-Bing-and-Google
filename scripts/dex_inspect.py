import struct

class Dex:
    def __init__(self,data):
        self.data=data
        self.strings=[self.string(self.u32(self.u32(60)+4*i)) for i in range(self.u32(56))]
        self.types=[self.strings[self.u32(self.u32(68)+4*i)] for i in range(self.u32(64))]
        self.protos=[]
        for i in range(self.u32(72)):
            pos=self.u32(76)+12*i; result=self.types[self.u32(pos+4)]; args=self.u32(pos+8)
            params='' if not args else ''.join(self.types[self.u16(args+4+2*j)] for j in range(self.u32(args)))
            self.protos.append('('+params+')'+result)
        self.method_ids=[]
        for i in range(self.u32(88)):
            pos=self.u32(92)+8*i
            self.method_ids.append((self.types[self.u16(pos)],self.strings[self.u32(pos+4)],self.protos[self.u16(pos+2)]))
        self.field_ids=[]
        for i in range(self.u32(80)):
            pos=self.u32(84)+8*i
            self.field_ids.append((self.types[self.u16(pos)],self.strings[self.u32(pos+4)],self.types[self.u16(pos+2)]))
        self.methods={}; self.fields={}; self.classes=[]
        for i in range(self.u32(96)):
            pos=self.u32(100)+32*i; self.classes.append(self.types[self.u32(pos)])
            off=self.u32(pos+24)
            if not off: continue
            sf,off=self.uleb(off); inf,off=self.uleb(off); dm,off=self.uleb(off); vm,off=self.uleb(off)
            for count,method in [(sf,False),(inf,False),(dm,True),(vm,True)]:
                index=0
                for _ in range(count):
                    delta,off=self.uleb(off); access,off=self.uleb(off); index+=delta
                    if method:
                        code,off=self.uleb(off); self.methods[self.method_ids[index]]={'access':access,'code_offset':code}
                    else:self.fields[self.field_ids[index]]=access
    def u16(self,off):return struct.unpack_from('<H',self.data,off)[0]
    def u32(self,off):return struct.unpack_from('<I',self.data,off)[0]
    def uleb(self,off):
        value=0; shift=0
        while True:
            b=self.data[off]; off+=1; value|=(b&127)<<shift
            if b<128:return value,off
            shift+=7
    def string(self,off):
        _,off=self.uleb(off); end=self.data.index(0,off)
        return self.data[off:end].decode('utf-8','replace')
