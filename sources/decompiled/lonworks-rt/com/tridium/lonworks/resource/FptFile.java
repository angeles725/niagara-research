package com.tridium.lonworks.resource;

import com.tridium.lonworks.util.LonByteArrayUtil;
import java.io.IOException;
import javax.baja.nre.util.Array;

public class FptFile extends ResourceFile {
   public byte[] refId = new byte[8];
   public int scope;
   public int[] resDeps = new int[7];
   public int[] typDeps = new int[7];
   public int numFPTs;
   public int numEmptyFPTs = 0;
   public int[] dirFPTIndex;
   public int[] dirFPTName;
   public int[] dirFPTKey;
   public Fpt[] fpts;

   @Override
   public void parse(ResFileInputStream in) throws IOException {
      this.parseHeader(in);
      this.refId = in.readByteArray(8);
      this.scope = in.readUnsigned8();

      for (int i = 0; i < 7; i++) {
         this.resDeps[i] = in.readUnsigned16();
      }

      for (int i = 0; i < 7; i++) {
         this.typDeps[i] = in.readUnsigned16();
      }

      this.numFPTs = in.readUnsigned16();
      if (this.majorFmtVer >= 4) {
         this.numEmptyFPTs = in.readUnsigned16();
      }

      this.dirFPTIndex = new int[this.numFPTs];

      for (int i = 0; i < this.numFPTs; i++) {
         this.dirFPTIndex[i] = in.readDirectory(this);
      }

      int fptNameCnt = this.numFPTs - this.numEmptyFPTs;
      this.dirFPTName = new int[fptNameCnt];

      for (int i = 0; i < fptNameCnt; i++) {
         this.dirFPTName[i] = in.readDirectory(this);
      }

      this.dirFPTKey = new int[fptNameCnt];

      for (int i = 0; i < fptNameCnt; i++) {
         this.dirFPTKey[i] = in.readDirectory(this);
      }

      Array<Fpt> a = new Array(Fpt.class);

      for (int i = 0; i < this.numFPTs; i++) {
         if (this.dirFPTIndex[i] != 0) {
            in.seek(this.dirFPTIndex[i]);
            a.add(new Fpt(in));
         }
      }

      this.fpts = (Fpt[])a.trim();
   }

   @Override
   public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append(super.toString());
      sb.append("refId         = ").append(LonByteArrayUtil.toString(this.refId)).append("\n");
      sb.append("scope         = ").append(this.scope).append("\n");

      for (int i = 0; i < 7; i++) {
         sb.append("resDeps[" + i + "]    = ").append(this.resDeps[i]).append("\n");
      }

      for (int i = 0; i < 7; i++) {
         sb.append("typDeps[" + i + "]    = ").append(this.typDeps[i]).append("\n");
      }

      sb.append("numFPTs       = ").append(this.numFPTs).append("\n");
      sb.append("numEmptyFPTs  = ").append(this.numEmptyFPTs).append("\n");

      try {
         for (int i = 0; i < this.fpts.length; i++) {
            this.fpts[i].toString(sb);
         }
      } catch (Throwable var3) {
         System.out.println(var3);
      }

      return sb.toString();
   }

   public Fpt getFptByKey(int key) {
      for (int i = 0; i < this.fpts.length; i++) {
         if (this.fpts[i].key == key) {
            return this.fpts[i];
         }
      }

      return null;
   }
}
