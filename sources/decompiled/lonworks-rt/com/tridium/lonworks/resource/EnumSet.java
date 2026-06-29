package com.tridium.lonworks.resource;

import java.io.IOException;
import javax.baja.nre.util.TextUtil;

public class EnumSet {
   public int index;
   public int recordSize;
   public String enumTag;
   public String enumFile;
   public int numEnums;
   public EnumSet.EnumMember[] members;
   public boolean isBoolean = false;

   public EnumSet(ResFileInputStream in, int address) throws IOException {
      this.index = in.readUnsigned16();
      this.recordSize = in.readUnsigned16();
      this.enumTag = in.readLString();
      this.enumFile = in.readLString();
      this.numEnums = in.readUnsigned16();
      if (in.majorFmtVer == 2) {
         this.numEnums = in.swap16(this.numEnums);
      }

      this.members = new EnumSet.EnumMember[this.numEnums];

      for (int i = 0; i < this.numEnums; i++) {
         this.members[i] = new EnumSet.EnumMember(in, address);
      }
   }

   @Override
   public String toString() {
      StringBuffer sb = new StringBuffer();
      this.toString(sb);
      return sb.toString();
   }

   public void toString(StringBuffer sb) {
      sb.append("index      = ").append(this.index).append("  recordSize = ").append(this.recordSize).append("\n");
      sb.append("enumTag    = ").append(this.enumTag).append("  enumFile   = ").append(this.enumFile).append("\n");
      sb.append("numEnums   = ").append(this.numEnums).append("\n");

      for (int i = 0; i < this.numEnums; i++) {
         this.members[i].toString(sb);
      }
   }

   public static class EnumMember {
      public int eValue;
      public int eResScope;
      public int eResIndex;
      public String eString;

      public EnumMember(ResFileInputStream in, int address) throws IOException {
         this.eValue = in.readSigned8();
         int relative = in.readUnsigned16();
         this.eResScope = in.readUnsigned8();
         this.eResIndex = in.readUnsigned24();
         long rememberWhere = in.getCurrentPosition();
         in.seek(address + relative);
         this.eString = in.readLString();
         in.seek(rememberWhere);
      }

      public void toString(StringBuffer sb) {
         sb.append(TextUtil.padRight(Integer.toString(this.eValue), 5)).append(TextUtil.padRight(this.eString, 20));
         sb.append("  ").append(this.eResScope).append(',').append(this.eResIndex).append("\n");
      }
   }
}
