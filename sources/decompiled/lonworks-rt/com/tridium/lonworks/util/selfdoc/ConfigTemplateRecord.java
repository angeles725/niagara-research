package com.tridium.lonworks.util.selfdoc;

public class ConfigTemplateRecord {
   private String rec;
   private int hdr;
   private String select;
   private int typeScope;
   private int flag;
   private int configIndex;
   private int length;
   private int dimension = 1;
   private String max = "";
   private String min = "";
   private boolean minSpecified = false;
   private boolean maxSpecified = false;
   private String ds = null;

   public ConfigTemplateRecord(String s) {
      this.rec = s;
      this.parseRecord(s);
   }

   public ConfigTemplateRecord(byte[] dat) {
      this.rec = this.recordToString(dat);
      this.parseRecord(this.rec);
   }

   public int getHdr() {
      return this.hdr;
   }

   public String getSelect() {
      return this.select;
   }

   public int getTypeScope() {
      return this.typeScope;
   }

   public int getConfigIndex() {
      return this.configIndex;
   }

   public int getLength() {
      return this.length;
   }

   public int getFlags() {
      return this.flag;
   }

   public boolean isMfgOnly() {
      return (this.flag & 144) == 144;
   }

   public boolean isReset() {
      return (this.flag & 8) == 8;
   }

   public boolean isReadOnly() {
      return (this.flag & 4) == 4;
   }

   public boolean isReadWrite() {
      return !this.isReadOnly() && !this.isMfgOnly();
   }

   public boolean isOffline() {
      return (this.flag & 2) == 2;
   }

   public boolean isDisable() {
      return (this.flag & 1) == 1;
   }

   public boolean isScpt() {
      return this.typeScope == 0;
   }

   public boolean hasMax() {
      return this.maxSpecified;
   }

   public String getMax() {
      return this.max;
   }

   public boolean hasMin() {
      return this.minSpecified;
   }

   public String getMin() {
      return this.min;
   }

   public int getDimension() {
      return this.dimension;
   }

   public boolean isArray() {
      return this.dimension > 0;
   }

   public int getNumRecs() {
      return this.dimension > 0 ? this.dimension : 1;
   }

   public String getComment() {
      int pos = this.rec.indexOf(59);
      return pos >= 0 ? this.rec.substring(pos + 1) : "";
   }

   public int getObjectIndex() {
      return this.select != null && this.select.length() != 0 && this.select.indexOf(46) == -1 && this.select.indexOf(45) == -1
         ? Integer.valueOf(this.select)
         : -1;
   }

   public String getDataSpecies() {
      return this.ds == null ? "DsMfgNv(" + this.length + ")" : this.ds;
   }

   public void setDataSpecies(String ds) {
      this.ds = ds;
   }

   private void parseRecord(String s) {
      ConfigTemplateRecord.RecordTokenizer tok = new ConfigTemplateRecord.RecordTokenizer(s);
      this.hdr = tok.getIntField();
      this.select = tok.getField();
      String flgs = tok.getField();
      this.typeScope = Integer.parseInt(flgs.substring(0, 1));
      if (flgs.length() > 3) {
         this.flag = Integer.parseInt(flgs.substring(3), 16) & 0xFF;
      } else {
         this.flag = flgs.getBytes()[1] & 255;
      }

      this.configIndex = tok.getIntField();
      this.length = tok.getIntField();
      if (tok.hasMoreRecords()) {
         this.dimension = tok.getIntField();
         if (this.dimension <= 0) {
            this.dimension = 1;
         }

         String rngMod = tok.getField();
         if (rngMod.indexOf(124) >= 0) {
            System.out.println("\nDetected unsupported structured member range modifiers " + rngMod);
         } else {
            if (rngMod.length() > 0) {
               int colPos = rngMod.indexOf(58);
               if (colPos > 0) {
                  this.min = rngMod.substring(0, colPos);
                  this.minSpecified = true;
               }

               if (colPos < rngMod.length() - 1) {
                  this.max = rngMod.substring(colPos + 1);
                  this.maxSpecified = true;
               }
            }
         }
      }
   }

   public String recordToString(byte[] rec) {
      int binLoc = 0;

      for (int i = 0; i < rec.length; i++) {
         if ((rec[i] & 128) != 0) {
            binLoc = i;
            break;
         }
      }

      return binLoc == 0
         ? new String(rec)
         : new String(rec, 0, binLoc) + "\\x" + Integer.toString(rec[binLoc] & 255, 16) + new String(rec, binLoc + 1, rec.length - binLoc - 1);
   }

   public String toFileString() {
      return this.rec;
   }

   @Override
   public String toString() {
      return "hdr = "
         + this.hdr
         + " select = "
         + this.select
         + " isScpt = "
         + this.isScpt()
         + " flag = "
         + this.flag
         + " configIndex = "
         + this.configIndex
         + " length = "
         + this.length
         + " dimension = "
         + this.dimension;
   }

   private static class RecordTokenizer {
      private String s;
      private int offset;

      public RecordTokenizer(String s) {
         this.s = s;
         this.offset = 0;
      }

      public String getField() {
         int endOfField = this.s.indexOf(44, this.offset);
         if (this.s.length() <= this.offset) {
            return "";
         } else {
            if (endOfField == -1) {
               endOfField = this.s.length();
            }

            String field = this.s.substring(this.offset, endOfField);
            this.offset = endOfField + 1;
            return field;
         }
      }

      public int getIntField() {
         String s = this.getField();
         return s.length() == 0 ? -1 : Integer.valueOf(s);
      }

      public boolean hasMoreRecords() {
         return this.s.length() > this.offset;
      }
   }
}
