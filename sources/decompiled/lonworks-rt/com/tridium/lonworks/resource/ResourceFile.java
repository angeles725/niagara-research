package com.tridium.lonworks.resource;

import java.io.IOException;

public abstract class ResourceFile {
   private static final int MAX_FILE_INFO_STRING = 256;
   private static final int SIZE_DESC_STRING = 252;
   private static final int SIZE_CRE_STRING = 256;
   private static final int SIZE_URL_STRING = 256;
   protected static final int NUM_SCOPES = 7;
   protected static final int NUM_PROG_ID_BYTES = 8;
   public static final int LDRF_CATALOG = 1;
   public static final int LDRF_LANG_RESOURCE = 2;
   public static final int LDRF_TYPE = 3;
   public static final int LDRF_FPT = 4;
   public static final int LDRF_FORMAT = 5;
   public boolean userFlag;
   public int fileType;
   String desc;
   String creator;
   String sURL;
   int resDescScope;
   int resDescIndex;
   int resCreScope;
   int resCreIndex;
   int majorDataVer;
   int year;
   int month;
   int day;
   int hour;
   int minute;
   int second;
   int minorDataVer;
   int majorFmtVer;
   int minorFmtVer;
   int headerSize;
   int headerCRC;
   int dataAddress;
   int dataSize;
   public String fileName = "";

   public abstract void parse(ResFileInputStream var1) throws IOException;

   public void parseHeader(ResFileInputStream in) throws IOException {
      this.userFlag = in.readCharacter() == 'U';
      this.fileType = ResourceFileUtil.typeStringToInt(in.readString(3));
      in.fileType = this.fileType;
      this.desc = in.readString(252);
      this.creator = in.readString(256);
      this.sURL = in.readString(256);
      this.resDescScope = in.readUnsigned8();
      this.resDescIndex = in.readUnsigned24();
      this.resCreScope = in.readUnsigned8();
      this.resCreIndex = in.readUnsigned24();
      this.majorDataVer = in.readUnsigned16();
      this.year = in.readUnsigned16();
      this.month = in.readUnsigned8();
      this.day = in.readUnsigned8();
      this.hour = in.readUnsigned8();
      this.minute = in.readUnsigned8();
      this.second = in.readUnsigned8();
      this.minorDataVer = in.readUnsigned8();
      this.majorFmtVer = in.readUnsigned8();
      in.majorFmtVer = this.majorFmtVer;
      this.verifyVersion();
      this.minorFmtVer = in.readUnsigned8();
      in.minorFmtVer = this.minorFmtVer;
      if ((this.fileType != 3 || this.majorFmtVer < 3) && (this.fileType == 3 || this.majorFmtVer < 2)) {
         this.headerSize = in.readUnsigned16();
      } else {
         this.headerSize = in.readSigned32();
      }

      this.headerCRC = in.readUnsigned16();
      this.dataAddress = in.readSigned32();
      this.dataSize = in.readSigned32();
   }

   private void verifyVersion() throws IOException {
      int majFmtLow = 0;
      int majFmtHigh = 0;
      switch (this.fileType) {
         case 1:
            majFmtLow = 1;
            majFmtHigh = 2;
            break;
         case 2:
            majFmtLow = 1;
            majFmtHigh = 3;
            break;
         case 3:
            majFmtLow = 2;
            majFmtHigh = 6;
            break;
         case 4:
            majFmtLow = 1;
            majFmtHigh = 5;
      }

      if (this.majorFmtVer < majFmtLow || this.majorFmtVer > majFmtHigh) {
         throw new IOException("Unsupported format version " + this.majorFmtVer + " for resournce type " + ResourceFileUtil.typeIntToString(this.fileType));
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("userFlag      = ").append(this.userFlag ? "User" : "Standard").append("\n");
      sb.append("fileType      = ").append(ResourceFileUtil.typeIntToString(this.fileType)).append("\n");
      sb.append("desc          = ").append(this.desc).append("\n");
      sb.append("creator       = ").append(this.creator).append("\n");
      sb.append("sURL          = ").append(this.sURL).append("\n");
      sb.append("resDescScope  = ").append(this.resDescScope).append(',').append(this.resDescIndex).append("\n");
      sb.append("resCreScope   = ").append(this.resCreScope).append(',').append(this.resCreIndex).append("\n");
      sb.append("DataVer       = ").append(this.majorDataVer).append('.').append(this.minorDataVer).append("\n");
      sb.append("FmtVer        = ").append(this.majorFmtVer).append('.').append(this.minorFmtVer).append("\n");
      sb.append("Data          = ")
         .append(this.month)
         .append('/')
         .append(this.day)
         .append('/')
         .append(this.year)
         .append(' ')
         .append(this.hour)
         .append('.')
         .append(this.minute)
         .append(':')
         .append(this.second)
         .append("\n");
      sb.append("headerSize    = ").append(this.headerSize);
      sb.append("  headerCRC   = ").append(this.headerCRC).append("\n");
      sb.append("dataAddress   = ").append(this.dataAddress);
      sb.append("  dataSize    = ").append(this.dataSize).append("\n");
      return sb.toString();
   }
}
