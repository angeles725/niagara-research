package com.tridium.lonworks.resource;

import com.tridium.lonworks.util.LonByteArrayUtil;
import java.io.IOException;

public class Fpt {
   int index;
   int recSize;
   boolean obsolete;
   boolean inheritMark = false;
   int key;
   String name;
   int length;
   int resNmScope;
   int resNmIndex;
   int resCmtScope;
   int resCmtIndex;
   int flags = 0;
   int numManNVs;
   int numOptNVs;
   int numManCPs;
   int numOptCPs;
   int principalNV;
   Fpt.Nv[] nvs;
   Fpt.Cp[] cps;
   private static final int MN_BIT = 2;
   private static final int MX_BIT = 1;
   private static final int DF_BIT = 4;
   private static final int IV_BIT = 8;
   private static final int FPTNV_INVALID_VAL_RG = 4;
   private static final int FPTNV_MIN_VAL_RG = 2;
   private static final int FPTNV_MAX_VAL_RG = 1;

   Fpt(ResFileInputStream in) throws IOException {
      this.index = in.readUnsigned16();
      this.recSize = in.readUnsigned16();
      this.key = in.readUnsigned16();
      if (in.majorFmtVer > 1) {
         this.obsolete = in.readBool();
      }

      if (in.majorFmtVer > 2) {
         this.inheritMark = in.readBool();
      }

      this.name = in.readLString();
      this.resNmScope = in.readUnsigned8();
      this.resNmIndex = in.readUnsigned24();
      this.resCmtScope = in.readUnsigned8();
      this.resCmtIndex = in.readUnsigned24();
      if (in.majorFmtVer >= 5) {
         this.flags = in.readUnsigned16();
      }

      this.numManNVs = in.readUnsigned8();
      this.numOptNVs = in.readUnsigned8();
      this.numManCPs = in.readUnsigned8();
      this.numOptCPs = in.readUnsigned8();
      this.principalNV = in.readUnsigned8();
      this.nvs = new Fpt.Nv[this.numManNVs + this.numOptNVs];

      for (int i = 0; i < this.nvs.length; i++) {
         this.nvs[i] = new Fpt.Nv(in);
      }

      this.cps = new Fpt.Cp[this.numManCPs + this.numOptCPs];

      for (int i = 0; i < this.cps.length; i++) {
         this.cps[i] = new Fpt.Cp(in);
      }

      if (in.majorFmtVer < 3) {
         for (int i = 0; i < this.nvs.length; i++) {
            this.nvs[i].memberNumber = i + 1;
         }

         for (int i = 0; i < this.cps.length; i++) {
            this.cps[i].memberNumber = i + 1;
         }
      }
   }

   @Override
   public String toString() {
      StringBuffer sb = new StringBuffer();
      this.toString(sb);
      return sb.toString();
   }

   public void toString(StringBuffer sb) {
      sb.append("index       = ").append(this.index).append("\n");
      sb.append("recSize     = ").append(this.recSize).append("\n");
      sb.append("obsolete    = ").append(this.obsolete).append("\n");
      sb.append("inheritMark = ").append(this.inheritMark).append("\n");
      sb.append("key         = ").append(this.key).append("\n");
      sb.append("name        = ").append(this.name).append("\n");
      sb.append("length      = ").append(this.length).append("\n");
      sb.append("Name Scope  = ").append(this.resNmScope).append(",").append(this.resNmIndex).append("\n");
      sb.append("Comment Scope = ").append(this.resCmtScope).append(',').append(this.resCmtIndex).append("\n");
      sb.append("numManNVs   = ").append(this.numManNVs).append("\n");
      sb.append("numOptNVs   = ").append(this.numOptNVs).append("\n");
      sb.append("numManCPs   = ").append(this.numManCPs).append("\n");
      sb.append("numOptCPs   = ").append(this.numOptCPs).append("\n");
      sb.append("principalNV = ").append(this.principalNV).append("\n");
      sb.append("** nvs ** ").append("\n");

      for (int i = 0; i < this.nvs.length; i++) {
         this.nvs[i].toString(sb);
      }

      sb.append("** cps ** ").append("\n");

      for (int i = 0; i < this.cps.length; i++) {
         this.cps[i].toString(sb);
      }
   }

   public Fpt.Nv getMemberNv(int memberNumber) {
      for (int i = 0; i < this.nvs.length; i++) {
         if (this.nvs[i].memberNumber == memberNumber) {
            return this.nvs[i];
         }
      }

      return null;
   }

   public Fpt.Cp getMemberCp(int scope, int index) {
      for (int i = 0; i < this.cps.length; i++) {
         if (this.cps[i].cpTScope == scope && this.cps[i].cpTIndex == index) {
            return this.cps[i];
         }
      }

      return null;
   }

   static class Cp {
      String cpName;
      int memberNumber = -1;
      int appliesTo;
      boolean mandatory;
      int resNmScope;
      int resNmIndex;
      int resCmtScope;
      int resCmtIndex;
      int flags = 0;
      int cpTScope;
      int cpTIndex;
      int modifyArray;
      int byteArrayLen;
      boolean hasDefault = false;
      byte[] defaultVal;
      boolean hasMin = false;
      byte[] valMin;
      boolean hasMax = false;
      byte[] valMax;
      boolean hasInv = false;
      public byte[] inv = null;

      Cp(ResFileInputStream in) throws IOException {
         in.readUnsigned16();
         this.appliesTo = in.readUnsigned16();
         if (in.majorFmtVer > 2) {
            this.memberNumber = in.readUnsigned16();
         }

         if (in.majorFmtVer > 3) {
            in.readUnsigned16();
            in.readUnsigned16();
         }

         this.cpName = in.readLString();
         this.mandatory = in.readBool();
         this.resNmScope = in.readUnsigned8();
         this.resNmIndex = in.readUnsigned24();
         this.resCmtScope = in.readUnsigned8();
         this.resCmtIndex = in.readUnsigned24();
         if (in.majorFmtVer >= 5) {
            this.flags = in.readUnsigned16();
         }

         this.cpTScope = in.readUnsigned8();
         this.cpTIndex = in.readUnsigned16();
         this.modifyArray = in.readUnsigned8();
         int controlBits = in.readUnsigned8();
         this.byteArrayLen = in.readUnsigned16();
         if ((controlBits & 4) != 0) {
            this.defaultVal = in.readByteArray(this.byteArrayLen);
            this.hasDefault = true;
         }

         if ((controlBits & 2) != 0) {
            this.valMin = in.readByteArray(this.byteArrayLen);
            this.hasMin = true;
         }

         if ((controlBits & 1) != 0) {
            this.valMax = in.readByteArray(this.byteArrayLen);
            this.hasMax = true;
         }

         if ((controlBits & 8) != 0) {
            this.inv = in.readByteArray(this.byteArrayLen);
            this.hasInv = true;
         }
      }

      @Override
      public String toString() {
         StringBuffer sb = new StringBuffer();
         this.toString(sb);
         return sb.toString();
      }

      public void toString(StringBuffer sb) {
         sb.append("CP:").append(this.cpName).append("\n");
         sb.append(" memberNumber = ").append(this.memberNumber);
         sb.append(" appliesTo    = ").append(this.appliesTo).append("\n");
         sb.append(" mandatory    = ").append(this.mandatory).append("\n");
         sb.append(" scope Name = ").append(this.resNmScope).append(',').append(this.resNmIndex);
         sb.append("  Comment = ").append(this.resCmtScope).append(',').append(this.resCmtIndex);
         sb.append("  CpType = ").append(this.cpTScope).append(',').append(this.cpTIndex).append("\n");
         sb.append(" modifyArray  = ").append(this.modifyArray);
         sb.append(" byteArrayLen = ").append(this.byteArrayLen).append("\n");
         if (this.hasDefault) {
            sb.append("defaultVal = ").append(LonByteArrayUtil.toString(this.defaultVal)).append("\n");
         }

         if (this.hasMin) {
            sb.append("valMin = ").append(LonByteArrayUtil.toString(this.valMin)).append("\n");
         }

         if (this.hasMax) {
            sb.append("valMax = ").append(LonByteArrayUtil.toString(this.valMax)).append("\n");
         }

         if (this.hasInv) {
            sb.append("inv = ").append(LonByteArrayUtil.toString(this.inv)).append("\n");
         }
      }
   }

   static class Nv {
      String nvName;
      int memberNumber = -1;
      boolean mandatory;
      int resNmScope;
      int resNmIndex;
      int resCmtScope;
      int resCmtIndex;
      int flags = 0;
      int nvTScope;
      int nvTIndex;
      int dirPollServ;
      int byteArrayLen;
      boolean hasMin = false;
      byte[] valMin;
      boolean hasMax = false;
      byte[] valMax;
      boolean hasInvalid = false;
      byte[] invalid;

      Nv(ResFileInputStream in) throws IOException {
         in.readUnsigned16();
         if (in.majorFmtVer > 2) {
            this.memberNumber = in.readUnsigned16();
         }

         this.nvName = in.readLString();
         this.mandatory = in.readBool();
         this.resNmScope = in.readUnsigned8();
         this.resNmIndex = in.readUnsigned24();
         this.resCmtScope = in.readUnsigned8();
         this.resCmtIndex = in.readUnsigned24();
         if (in.majorFmtVer >= 5) {
            this.flags = in.readUnsigned16();
         }

         this.nvTScope = in.readUnsigned8();
         this.nvTIndex = in.readUnsigned16();
         this.dirPollServ = in.readUnsigned8();
         this.byteArrayLen = in.readUnsigned8();
         if ((this.dirPollServ & 2) != 0) {
            this.valMin = in.readByteArray(this.byteArrayLen);
            this.hasMin = true;
         }

         if ((this.dirPollServ & 1) != 0) {
            this.valMax = in.readByteArray(this.byteArrayLen);
            this.hasMax = true;
         }

         if ((this.dirPollServ & 4) != 0) {
            this.invalid = in.readByteArray(this.byteArrayLen);
            this.hasInvalid = true;
         }
      }

      @Override
      public String toString() {
         StringBuffer sb = new StringBuffer();
         this.toString(sb);
         return sb.toString();
      }

      public void toString(StringBuffer sb) {
         sb.append("NV:").append(this.nvName).append("\n");
         sb.append(" memberNumber = ").append(this.memberNumber).append("\n");
         sb.append(" mandatory    = ").append(this.mandatory).append("\n");
         sb.append(" scope Name = ").append(this.resNmScope).append(',').append(this.resNmIndex);
         sb.append("  Comment = ").append(this.resCmtScope).append(',').append(this.resCmtIndex);
         sb.append("  NvType = ").append(this.nvTScope).append(',').append(this.nvTIndex).append("\n");
         sb.append(" dirPollServ  = ").append(Integer.toString(this.dirPollServ, 16));
         sb.append("  byteArrayLen = ").append(this.byteArrayLen).append("\n");
         if (this.hasMin) {
            sb.append("valMin = ").append(LonByteArrayUtil.toString(this.valMin)).append("\n");
         }

         if (this.hasMax) {
            sb.append("valMax = ").append(LonByteArrayUtil.toString(this.valMax)).append("\n");
         }
      }
   }
}
