#!/usr/bin/env python3
# Minimal read-only QNX6 (Power-Safe) reader for the JACE-8000 SD image.
# Enumerates the directory tree and optionally extracts a file. READ-ONLY.
import struct, sys, os

BS = 1024
import os
IMGPATH = os.environ.get("QNX6_IMG", "")  # set QNX6_IMG=/path/to/disk.img

class QNX6:
    def __init__(self, img, part_off):
        self.f = open(img, "rb")
        self.P = part_off
        self.f.seek(self.P + 0x2000)
        sb = self.f.read(0x1000)
        magic, = struct.unpack('<I', sb[0:4])
        assert magic == 0x68191122, "not qnx6"
        self.blocksize, self.num_inodes, self.free_inodes, self.num_blocks = struct.unpack('<IIII', sb[48:64])
        self.inode_rn = self._rn(sb, 72)     # (size, ptr[16], levels)
        self.longfile_rn = self._rn(sb, 232)
        self.OFF = self._detect_off()
        self.inode_table = self._read_file(*self.inode_rn)
        self.longfile = self._read_file(*self.longfile_rn)

    def _rn(self, sb, off):
        size, = struct.unpack('<Q', sb[off:off+8])
        ptr = list(struct.unpack('<16I', sb[off+8:off+72]))
        levels = sb[off+72]
        return (size, ptr, levels)

    def _rawblk(self, b, off):
        self.f.seek(self.P + (b + off) * BS)
        return self.f.read(BS)

    def _detect_off(self):
        size, ptr, levels = self.inode_rn
        for OFF in range(0, 40):
            b0 = self._rawblk(ptr[0], OFF)  # only valid when levels collapses; verify via full walk below instead
            if len(b0) < 128:
                continue
            # try full resolution at this OFF: read logical block 0 of the inode file
            try:
                self.OFF = OFF
                data0 = self._read_logical_block(ptr, levels, 0)
                di_mode, = struct.unpack('<H', data0[32:34])
                di_size, = struct.unpack('<Q', data0[0:8])
                if (di_mode & 0xF000) == 0x4000 and 0 < di_size < 50_000_000:
                    return OFF
            except Exception:
                continue
        raise RuntimeError("could not detect block offset")

    def _leaf_blocks(self, ptrs, levels):
        """Yield ordered data-block numbers from a ptr array with `levels` of indirection."""
        for p in ptrs:
            if p == 0xffffffff:
                continue
            if levels == 0:
                yield p
            else:
                blk = self._rawblk(p, self.OFF)
                sub = struct.unpack('<%dI' % (BS // 4), blk)
                yield from self._leaf_blocks(sub, levels - 1)

    def _read_logical_block(self, ptrs, levels, L):
        # get the L-th leaf block
        i = 0
        for b in self._leaf_blocks(ptrs, levels):
            if i == L:
                return self._rawblk(b, self.OFF)
            i += 1
        raise IndexError(L)

    def _read_file(self, size, ptrs, levels):
        out = bytearray()
        for b in self._leaf_blocks(ptrs, levels):
            out += self._rawblk(b, self.OFF)
            if len(out) >= size:
                break
        return bytes(out[:size])

    def inode(self, n):
        e = self.inode_table[(n-1)*128:(n)*128]
        di_size, = struct.unpack('<Q', e[0:8])
        di_mode, = struct.unpack('<H', e[32:34])
        ptr = list(struct.unpack('<16I', e[36:100]))
        levels = e[100]
        return dict(size=di_size, mode=di_mode, ptr=ptr, levels=levels)

    def read_inode_data(self, ino):
        i = self.inode(ino)
        return self._read_file(i['size'], i['ptr'], i['levels'])

    def _longname(self, idx):
        rec = self._read_logical_block(self.longfile_rn[1], self.longfile_rn[2], idx)
        ln, = struct.unpack('<H', rec[0:2])
        return rec[2:2+ln].decode('latin1', 'replace')

    def listdir(self, ino):
        data = self.read_inode_data(ino)
        out = []
        for o in range(0, len(data), 32):
            ent = data[o:o+32]
            if len(ent) < 32:
                break
            de_ino, = struct.unpack('<I', ent[0:4])
            de_size = ent[4]
            if de_ino == 0:
                continue
            if de_size == 0xff:
                lidx, = struct.unpack('<I', ent[8:12])
                name = self._longname(lidx)
            else:
                name = ent[5:5+de_size].decode('latin1', 'replace')
            if name in ('.', '..'):
                continue
            out.append((name, de_ino))
        return out

    def resolve(self, path):
        """Resolve an absolute path to (inode_number, inode_dict). Returns None if not found."""
        ino = 1
        parts = [p for p in path.split('/') if p]
        for part in parts:
            found = None
            for name, cino in self.listdir(ino):
                if name == part:
                    found = cino; break
            if found is None:
                return None
            ino = found
        return ino, self.inode(ino)

    def extract(self, path, outpath):
        r = self.resolve(path)
        if r is None:
            raise FileNotFoundError(path)
        ino, i = r
        data = self.read_inode_data(ino)
        with open(outpath, 'wb') as o:
            o.write(data)
        return len(data)

    def walk(self, ino=1, path="", depth=0, maxdepth=40):
        if depth > maxdepth:
            return
        for name, cino in sorted(self.listdir(ino)):
            i = self.inode(cino)
            p = path + "/" + name
            isdir = (i['mode'] & 0xF000) == 0x4000
            yield (p, i['mode'], i['size'], isdir)
            if isdir:
                yield from self.walk(cino, p, depth+1, maxdepth)


if __name__ == "__main__":
    if not IMGPATH:
        sys.exit("set QNX6_IMG=/path/to/raw-disk.img")
    part = sys.argv[1] if len(sys.argv) > 1 else "P2"
    off = {"P2": 135266304, "P3": 3711959040}[part]
    fs = QNX6(IMGPATH, off)
    sys.stderr.write("%s OFF=%d blocksize=%d inodes=%d\n" % (part, fs.OFF, fs.blocksize, fs.num_inodes))
    if len(sys.argv) > 2 and sys.argv[2] == "extract":
        n = fs.extract(sys.argv[3], sys.argv[4])
        sys.stderr.write("extracted %s -> %s (%d bytes)\n" % (sys.argv[3], sys.argv[4], n))
        sys.exit(0)
    ndir = nfile = 0
    for p, mode, size, isdir in fs.walk():
        t = "d" if isdir else "-"
        print("%s %10d %s" % (t, size, p))
        if isdir: ndir += 1
        else: nfile += 1
    sys.stderr.write("totals: dirs=%d files=%d\n" % (ndir, nfile))
