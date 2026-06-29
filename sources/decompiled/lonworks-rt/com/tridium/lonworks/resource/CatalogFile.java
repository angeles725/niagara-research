package com.tridium.lonworks.resource;

import com.tridium.lonworks.util.LonByteArrayUtil;
import java.io.IOException;

public class CatalogFile extends ResourceFile {
   int stale;
   int numDirec;
   int numLangFiles;
   int numTypeFiles;
   int numFPTFiles;
   int numFormatFiles;
   int[] dirDirec;
   int[] dirLangFiles;
   int[] dirTypeFiles;
   int[] dirFPTFiles;
   int[] dirFormatFiles;
   CatalogFile.DirInfo[] dirs;
   CatalogFile.FileInfo[] langFiles;
   CatalogFile.FileInfo[] typeFiles;
   CatalogFile.FileInfo[] fptFiles;
   CatalogFile.FileInfo[] formatFiles;

   @Override
   public void parse(ResFileInputStream in) throws IOException {
      this.parseHeader(in);
      this.stale = in.readUnsigned8();
      this.numDirec = in.readUnsigned16();
      this.numLangFiles = in.readUnsigned16();
      this.numTypeFiles = in.readUnsigned16();
      this.numFPTFiles = in.readUnsigned16();
      this.numFormatFiles = in.readUnsigned16();
      this.dirDirec = new int[this.numDirec];

      for (int i = 0; i < this.numDirec; i++) {
         this.dirDirec[i] = in.readDirectory(this);
      }

      this.dirLangFiles = new int[this.numLangFiles];

      for (int i = 0; i < this.numLangFiles; i++) {
         this.dirLangFiles[i] = in.readDirectory(this);
      }

      this.dirTypeFiles = new int[this.numTypeFiles];

      for (int i = 0; i < this.numTypeFiles; i++) {
         this.dirTypeFiles[i] = in.readDirectory(this);
      }

      this.dirFPTFiles = new int[this.numFPTFiles];

      for (int i = 0; i < this.numFPTFiles; i++) {
         this.dirFPTFiles[i] = in.readDirectory(this);
      }

      this.dirFormatFiles = new int[this.numFormatFiles];

      for (int i = 0; i < this.numFormatFiles; i++) {
         this.dirFormatFiles[i] = in.readDirectory(this);
      }

      this.dirs = new CatalogFile.DirInfo[this.numDirec];

      for (int i = 0; i < this.numDirec; i++) {
         in.seek(this.dirDirec[i]);
         this.dirs[i] = new CatalogFile.DirInfo(in);
      }

      this.langFiles = new CatalogFile.FileInfo[this.numLangFiles];

      for (int i = 0; i < this.numLangFiles; i++) {
         in.seek(this.dirLangFiles[i]);
         this.langFiles[i] = new CatalogFile.FileInfo(in);
      }

      this.typeFiles = new CatalogFile.FileInfo[this.numTypeFiles];

      for (int i = 0; i < this.numTypeFiles; i++) {
         in.seek(this.dirTypeFiles[i]);
         this.typeFiles[i] = new CatalogFile.FileInfo(in);
      }

      this.fptFiles = new CatalogFile.FileInfo[this.numFPTFiles];

      for (int i = 0; i < this.numFPTFiles; i++) {
         in.seek(this.dirFPTFiles[i]);
         this.fptFiles[i] = new CatalogFile.FileInfo(in);
      }

      this.formatFiles = new CatalogFile.FileInfo[this.numFormatFiles];

      for (int i = 0; i < this.numFormatFiles; i++) {
         in.seek(this.dirFormatFiles[i]);
         this.formatFiles[i] = new CatalogFile.FileInfo(in);
      }
   }

   @Override
   public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append(super.toString());
      sb.append("stale          = ").append(this.stale).append("\n");
      sb.append("numDirec       = ").append(this.numDirec).append("\n");
      sb.append("numLangFiles   = ").append(this.numLangFiles).append("\n");
      sb.append("numTypeFiles   = ").append(this.numTypeFiles).append("\n");
      sb.append("numFPTFiles    = ").append(this.numFPTFiles).append("\n");
      sb.append("numFormatFiles = ").append(this.numFormatFiles).append("\n");

      for (int i = 0; i < this.numDirec; i++) {
         sb.append("dirDirec[" + i + "]    = ").append(Integer.toString(this.dirDirec[i], 16)).append("\n");
      }

      for (int i = 0; i < this.numLangFiles; i++) {
         sb.append("dirLangFiles[" + i + "]    = ").append(Integer.toString(this.dirLangFiles[i], 16)).append("\n");
      }

      for (int i = 0; i < this.numTypeFiles; i++) {
         sb.append("dirTypeFiles[" + i + "]    = ").append(Integer.toString(this.dirTypeFiles[i], 16)).append("\n");
      }

      for (int i = 0; i < this.numFPTFiles; i++) {
         sb.append("dirFPTFiles[" + i + "]    = ").append(Integer.toString(this.dirFPTFiles[i], 16)).append("\n");
      }

      for (int i = 0; i < this.numFormatFiles; i++) {
         sb.append("dirFormatFiles[" + i + "]    = ").append(Integer.toString(this.dirFormatFiles[i], 16)).append("\n");
      }

      try {
         for (int i = 0; i < this.numDirec; i++) {
            this.dirs[i].toString(sb);
         }

         for (int i = 0; i < this.numLangFiles; i++) {
            this.langFiles[i].toString(sb);
         }

         for (int i = 0; i < this.numTypeFiles; i++) {
            this.typeFiles[i].toString(sb);
         }

         for (int i = 0; i < this.numFPTFiles; i++) {
            this.fptFiles[i].toString(sb);
         }

         for (int i = 0; i < this.numFormatFiles; i++) {
            this.formatFiles[i].toString(sb);
         }
      } catch (Throwable var3) {
         System.out.println(var3);
      }

      return sb.toString();
   }

   static class DirInfo {
      public int index;
      public int recSize;
      public String dirPath;

      DirInfo(ResFileInputStream in) throws IOException {
         this.index = in.readUnsigned16();
         this.recSize = in.readUnsigned16();
         this.dirPath = in.readLString();
      }

      public void toString(StringBuffer sb) {
         sb.append("index        = ").append(this.index).append("\n");
         sb.append("recSize     = ").append(this.recSize).append("\n");
         sb.append("dirPath      = ").append(this.dirPath).append("\n");
      }
   }

   static class FileInfo {
      public int index;
      public int recSize;
      public int fileType;
      public int scope;
      public byte[] refId;
      public int direcIndex;
      public int majorDataVer;
      public int minorDataVer;
      public int locale;
      public String fileName;

      FileInfo(ResFileInputStream in) throws IOException {
         this.index = in.readUnsigned16();
         this.recSize = in.readUnsigned16();
         this.fileType = in.readUnsigned8();
         this.scope = in.readUnsigned8();
         this.refId = in.readByteArray(8);
         this.direcIndex = in.readUnsigned16();
         this.majorDataVer = in.readUnsigned16();
         this.minorDataVer = in.readUnsigned8();
         this.locale = in.readSigned32();
         this.fileName = in.readLString();
      }

      public void toString(StringBuffer sb) {
         sb.append("index        = ").append(this.index).append("\n");
         sb.append("recSize      = ").append(this.recSize).append("\n");
         sb.append("fileType     = ").append(this.fileType).append("\n");
         sb.append("scope        = ").append(this.scope).append("\n");
         sb.append("refId        = ").append(LonByteArrayUtil.toString(this.refId)).append("\n");
         sb.append("direcIndex   = ").append(this.direcIndex).append("\n");
         sb.append("DataVer maj.min = ").append(this.majorDataVer).append('.').append(this.minorDataVer).append("\n");
         sb.append("locale       = ").append(Integer.toString(this.locale, 16)).append("\n");
         sb.append("fileName     = ").append(this.fileName).append("\n");
      }
   }
}
