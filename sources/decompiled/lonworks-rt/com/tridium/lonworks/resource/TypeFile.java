package com.tridium.lonworks.resource;

import com.tridium.lonworks.util.LonByteArrayUtil;
import java.io.IOException;

public class TypeFile extends ResourceFile {
   public byte[] refId = new byte[8];
   public int scope;
   int[] resDeps = new int[7];
   int[] typDeps = new int[7];
   int numNVTs;
   int numCPTs;
   int numEnumSets;
   int numEmptyNVTs = 0;
   int numEmptyCPTs = 0;
   int numEmptyEnumSets = 0;
   int[] dirNVTIndex;
   int[] dirNVTName;
   int[] dirCPTIndex;
   int[] dirCPTName;
   int[] dirEnumIndex;
   int[] dirEnumTag;
   int[] dirEnumFile;
   public Type[] nvTypes;
   public CpType[] cpTypes;
   public EnumSet[] enumSets;
   public static final String SNVT = "SNVT_";
   public static final String UNVT = "UNVT_";
   public static final String SCPT = "SCPT_";
   public static final String UCPT = "UCPT_";

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

      this.numNVTs = in.readUnsigned16();
      this.numCPTs = in.readUnsigned16();
      this.numEnumSets = in.readUnsigned16();
      if (this.majorFmtVer > 3) {
         this.numEmptyNVTs = in.readUnsigned16();
         this.numEmptyCPTs = in.readUnsigned16();
         this.numEmptyEnumSets = in.readUnsigned16();
      }

      this.dirNVTIndex = new int[this.numNVTs];

      for (int i = 0; i < this.numNVTs; i++) {
         this.dirNVTIndex[i] = in.readDirectory(this);
      }

      int nvtNameCnt = this.numNVTs - this.numEmptyNVTs;
      this.dirNVTName = new int[nvtNameCnt];

      for (int i = 0; i < nvtNameCnt; i++) {
         this.dirNVTName[i] = in.readDirectory(this);
      }

      this.dirCPTIndex = new int[this.numCPTs];

      for (int i = 0; i < this.numCPTs; i++) {
         this.dirCPTIndex[i] = in.readDirectory(this);
      }

      int cptNameCnt = this.numCPTs - this.numEmptyCPTs;
      this.dirCPTName = new int[cptNameCnt];

      for (int i = 0; i < cptNameCnt; i++) {
         this.dirCPTName[i] = in.readDirectory(this);
      }

      this.dirEnumIndex = new int[this.numEnumSets];

      for (int i = 0; i < this.numEnumSets; i++) {
         this.dirEnumIndex[i] = in.readDirectory(this);
      }

      int enumNameCnt = this.numEnumSets - this.numEmptyEnumSets;
      this.dirEnumTag = new int[enumNameCnt];

      for (int i = 0; i < enumNameCnt; i++) {
         this.dirEnumTag[i] = in.readDirectory(this);
      }

      this.dirEnumFile = new int[enumNameCnt];

      for (int i = 0; i < enumNameCnt; i++) {
         this.dirEnumFile[i] = in.readDirectory(this);
      }

      this.nvTypes = new Type[this.numNVTs];

      for (int i = 0; i < this.numNVTs; i++) {
         if (this.dirNVTIndex[i] != 0) {
            in.seek(this.dirNVTIndex[i]);

            try {
               this.nvTypes[i] = new Type(in, this.userFlag ? "UNVT_" : "SNVT_", this.scope);
            } catch (Throwable var7) {
               System.out.println(var7);
               this.nvTypes[i] = null;
            }
         }
      }

      this.cpTypes = new CpType[this.numCPTs];

      for (int ix = 0; ix < this.numCPTs; ix++) {
         if (this.dirCPTIndex[ix] != 0) {
            in.seek(this.dirCPTIndex[ix]);
            this.cpTypes[ix] = new CpType(in, this.userFlag ? "UCPT_" : "SCPT_", this.scope);
         }
      }

      this.enumSets = new EnumSet[this.numEnumSets];

      for (int ixx = 0; ixx < this.numEnumSets; ixx++) {
         if (this.dirEnumIndex[ixx] != 0) {
            int address = this.dirEnumIndex[ixx];
            in.seek(address);
            this.enumSets[ixx] = new EnumSet(in, address);
         }
      }
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

      sb.append("numNVTs       = ").append(this.numNVTs).append("  numEmptyNVTs     = ").append(this.numEmptyNVTs).append("\n");
      sb.append("numCPTs       = ").append(this.numCPTs).append("  numEmptyCPTs     = ").append(this.numEmptyCPTs).append("\n");
      sb.append("numEnumSets   = ").append(this.numEnumSets).append("  numEmptyEnumSets = ").append(this.numEmptyEnumSets).append("\n");

      for (int i = 0; i < this.numNVTs; i++) {
         this.nvTypes[i].toString(sb);
      }

      for (int i = 0; i < this.numCPTs; i++) {
         this.cpTypes[i].toString(sb);
      }

      for (int i = 0; i < this.numEnumSets; i++) {
         this.enumSets[i].toString(sb);
      }

      return sb.toString();
   }
}
