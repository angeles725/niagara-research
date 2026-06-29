package com.tridium.lonworks.resource;

import com.tridium.lonworks.util.LonByteArrayUtil;
import java.io.IOException;
import javax.baja.nre.util.TextUtil;

public class LanguageFile extends ResourceFile {
   public byte[] refId;
   public int scope;
   public int locale;
   public int numStrings;
   public int numEmptyStrings = 0;
   public int[] dirStrings;
   public LanguageFile.LangRec[] langRecs;

   @Override
   public void parse(ResFileInputStream in) throws IOException {
      this.parseHeader(in);
      this.refId = in.readByteArray(8);
      this.scope = in.readUnsigned8();
      this.locale = in.readSigned32();
      this.numStrings = in.readUnsigned24();
      if (this.majorFmtVer > 2) {
         this.numEmptyStrings = in.readUnsigned24();
      }

      this.dirStrings = new int[this.numStrings];

      for (int i = 0; i < this.numStrings; i++) {
         this.dirStrings[i] = in.readDirectory(this);
      }

      this.langRecs = new LanguageFile.LangRec[this.numStrings];

      for (int i = 0; i < this.numStrings; i++) {
         in.seek(this.dirStrings[i]);
         this.langRecs[i] = new LanguageFile.LangRec(in);
      }
   }

   @Override
   public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append(super.toString());
      sb.append("refId      = ").append(LonByteArrayUtil.toString(this.refId)).append("\n");
      sb.append("scope      = ").append(this.scope).append("\n");
      sb.append("locale     = ").append(Integer.toString(this.locale, 16)).append("\n");
      sb.append("numStrings = ").append(this.numStrings).append("\n");

      for (int i = 0; i < this.numStrings; i++) {
         this.langRecs[i].toString(sb);
      }

      return sb.toString();
   }

   public static class LangRec {
      public int index;
      public int recSize;
      public int hashCode;
      public int unicode;
      public String resStr;

      LangRec(ResFileInputStream in) throws IOException {
         this.index = in.readUnsigned24();
         this.recSize = in.readUnsigned16();
         this.hashCode = in.readUnsigned16();
         this.unicode = in.readUnsigned8();
         int strLen = this.recSize - 2 - 1;
         this.resStr = in.readString(strLen);
      }

      public void toString(StringBuffer sb) {
         sb.append(TextUtil.padRight(Integer.toString(this.index), 3)).append(" ");
         if (this.unicode != 0) {
            sb.append("[unicode = ").append(this.unicode).append("] ");
         }

         sb.append(this.resStr).append("\n");
      }
   }
}
