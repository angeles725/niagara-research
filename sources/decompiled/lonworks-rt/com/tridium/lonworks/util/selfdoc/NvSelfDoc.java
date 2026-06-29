package com.tridium.lonworks.util.selfdoc;

import java.util.StringTokenizer;
import javax.baja.lonworks.datatypes.BModifyFlags;
import javax.baja.lonworks.enums.BLonConfigScope;
import javax.baja.nre.util.Array;

public class NvSelfDoc {
   public String selfDoc;
   public NvSelfDoc.NvSelfDocData nv = null;
   public NvSelfDoc.ConfigNvSelfDocData nvConfig = null;
   public boolean empty = false;
   private int version;
   public int dimension = 1;
   public Exception ex = null;

   public NvSelfDoc(String selfDoc) {
      this(selfDoc, 1);
   }

   public NvSelfDoc(String selfDoc, int version) {
      this.selfDoc = selfDoc;
      this.version = version;
      this.parseNvSelfDocumentation(selfDoc);
   }

   private void parseNvSelfDocumentation(String line) {
      boolean error = false;

      try {
         if (line.startsWith("*")) {
            this.empty = true;
            return;
         }

         int locationOfComment = line.indexOf(";");
         if (locationOfComment >= 0) {
            line = line.substring(0, locationOfComment);
         }

         if (line.startsWith("\"")) {
            line = line.substring(1, line.length());
         }

         if (line.startsWith("@")) {
            this.nv = new NvSelfDoc.NvSelfDocData();
            this.nv.description = null;
            String s = line.substring(1, line.length());
            int suffixIndex = s.indexOf(59);
            if (suffixIndex != -1) {
               this.nv.description = s.substring(suffixIndex + 1);
               s = s.substring(0, suffixIndex);
            }

            if (s.length() == 0) {
               this.nv = null;
               return;
            }

            try {
               this.nv.mfgMember = s.indexOf(35) != -1;
               this.nv.changeableType = s.indexOf(63) != -1;
               StringTokenizer st = new StringTokenizer(s, "|#-?.[]");
               this.nv.firstObjIndex = this.intToken(st);
               if (s.indexOf(45) != -1) {
                  this.nv.lastObjIndex = this.intToken(st);
               }

               this.nv.memberNumber = this.intToken(st);
               if (s.indexOf(91) != -1) {
                  this.nv.memberArraySize = this.intToken(st);
               }
            } catch (Exception var13) {
               System.out.println("warning : unable to parse selfdoc string " + s);
               this.ex = var13;
            }
         } else if (line.startsWith("&")) {
            this.nvConfig = new NvSelfDoc.ConfigNvSelfDocData();
            String sx = line.substring(1, line.length());
            int suffixIndexx = sx.indexOf(59);
            if (suffixIndexx != -1) {
               this.nvConfig.description = sx.substring(suffixIndexx + 1);
               sx = sx.substring(0, suffixIndexx);
            }

            String[] toks = this.getTokenArray(sx);
            this.nvConfig.hdr = Integer.parseInt(toks[0]);
            if (this.nvConfig.hdr == 0) {
               this.nvConfig.select = "";
            } else {
               this.nvConfig.select = toks[1];
            }

            String flagText = toks[2];
            this.nvConfig.typeScope = Integer.parseInt(flagText.substring(0, 1));
            int pos = flagText.indexOf("x");
            if (pos > 0) {
               if (flagText.length() > 3) {
                  this.nvConfig.flg = Integer.parseInt(flagText.substring(pos + 1), 16) & 0xFF;
               } else {
                  this.nvConfig.flg = flagText.getBytes()[1] & 255;
               }
            } else if (flagText.length() == 2) {
               this.nvConfig.flg = flagText.charAt(1);
            } else {
               System.out.println("*****\nUnable to parse flag " + flagText);

               for (int i = 0; i < flagText.length(); i++) {
                  System.out.print(Integer.toString(flagText.charAt(i), 16) + " ");
               }

               System.out.println("\n" + line + "\n*****");
            }

            this.nvConfig.configIndex = Integer.parseInt(toks[3]);
            int tokndx = 4;
            if (this.version >= 3) {
               try {
                  if (toks.length > 4 && toks[4] != null) {
                     this.dimension = Integer.parseInt(toks[4]);
                  }

                  tokndx++;
               } catch (NumberFormatException var12) {
               }
            }

            if (toks.length > tokndx && toks[tokndx] != null) {
               String rngMod = toks[tokndx];
               if (rngMod.indexOf(124) >= 0) {
                  System.out.println("\nDetected unsupported structured member range modifiers " + rngMod);
               } else {
                  int colPos = rngMod.indexOf(58);
                  if (colPos > 0) {
                     this.nvConfig.min = rngMod.substring(0, colPos);
                     this.nvConfig.minSpecified = true;
                  }

                  if (colPos < rngMod.length() - 1) {
                     this.nvConfig.max = rngMod.substring(colPos + 1);
                     this.nvConfig.maxSpecified = true;
                  }
               }
            }

            tokndx++;
            if (toks.length > tokndx && toks[tokndx] != null) {
               this.nvConfig.changeType = toks[tokndx].indexOf(63) >= 0;
            }
         } else {
            error = true;
         }
      } catch (NumberFormatException var14) {
         System.err.println("Number format exception occurred while parsing nv self doc." + var14);
         var14.printStackTrace();
         error = true;
      } catch (Throwable var15) {
         error = true;
      }

      if (error) {
         System.out.println("\nUnable to parse nv self documentation \"" + line);
         this.nv = null;
         this.nvConfig = null;
         this.empty = true;
      }
   }

   private int intToken(StringTokenizer st) {
      return Integer.parseInt(st.nextToken().trim());
   }

   String[] getTokenArray(String s) {
      Array<String> a = new Array(String.class);
      int ndx = 0;
      int delimit = s.indexOf(",");

      while (ndx < s.length()) {
         if (ndx < delimit) {
            a.add(s.substring(ndx, delimit));
         } else {
            a.add(null);
         }

         ndx = delimit + 1;
         delimit = s.indexOf(",", ndx);
         if (delimit < 0) {
            delimit = s.length();
         }
      }

      return (String[])a.trim();
   }

   public boolean isNvDoc() {
      return this.nv != null;
   }

   public boolean isNciDoc() {
      return this.nvConfig != null;
   }

   public int getObjectIndex(int ndx) {
      if (this.nvConfig != null) {
         return this.nvConfig.getObjectIndex(ndx);
      } else {
         return this.nv != null ? this.nv.getObjectIndex(ndx) : -1;
      }
   }

   public String getObjectIndexString() {
      if (this.nvConfig != null) {
         return this.nvConfig.getObjectIndexString();
      } else {
         return this.nv != null ? this.nv.getObjectIndexString() : "";
      }
   }

   public int getMemberIndex(int ndx) {
      return this.nv != null ? this.nv.getMemberIndex(ndx) : -1;
   }

   public int getPerObjectNvCount() {
      return this.nv != null ? this.nv.getPerObjectNvCount() : 1;
   }

   public boolean isChangeableType() {
      return this.nv != null ? this.nv.changeableType : false;
   }

   public boolean isObjectArrayed() {
      if (this.nv != null) {
         return this.nv.isObjectArrayed();
      } else {
         return this.nvConfig != null ? this.nvConfig.isObjectArrayed() : false;
      }
   }

   public boolean isMfrMember() {
      return this.nv != null ? this.nv.mfgMember : false;
   }

   public int getConfigIndex() {
      return this.nvConfig != null ? this.nvConfig.configIndex : -1;
   }

   public BModifyFlags getModifyFlag() {
      return this.nvConfig != null ? BModifyFlags.fromFlags(this.nvConfig.flg) : BModifyFlags.DEFAULT;
   }

   public BLonConfigScope getScope() {
      return this.nvConfig != null ? BLonConfigScope.make(this.nvConfig.hdr) : BLonConfigScope.node;
   }

   public String getSelect() {
      return this.nvConfig != null ? this.nvConfig.select : "";
   }

   @Override
   public String toString() {
      if (this.nv != null) {
         return this.nv.toString();
      } else {
         return this.nvConfig != null ? this.nvConfig.toString() : "Uninitialized";
      }
   }

   public boolean isEmpty() {
      return this.empty;
   }

   public static class ConfigNvSelfDocData {
      public int hdr;
      public String select;
      int firstObjIndex;
      int lastObjIndex = 0;
      public int typeScope;
      public int flg;
      public int configIndex;
      public String min = "";
      public String max = "";
      public boolean minSpecified = false;
      public boolean maxSpecified = false;
      public boolean changeType = false;
      String description;

      public String getObjectIndexString() {
         return Integer.toString(this.firstObjIndex) + (this.lastObjIndex != 0 ? "-" + this.lastObjIndex : "");
      }

      public int getObjectIndex(int ndx) {
         return this.lastObjIndex == 0 ? this.firstObjIndex : this.firstObjIndex + ndx;
      }

      @Override
      public String toString() {
         StringBuilder sb = new StringBuilder();
         sb.append("SCPT : " + !this.isMfrDefined() + " typeScope=" + this.typeScope + "\n");
         sb.append("configIndex : " + this.configIndex + "\n");
         return sb.toString();
      }

      public boolean isObjectArrayed() {
         return this.lastObjIndex > this.firstObjIndex;
      }

      public boolean isMfrDefined() {
         return this.typeScope > 0;
      }
   }

   public static class NvSelfDocData {
      public boolean changeableType;
      public int firstObjIndex;
      public int lastObjIndex = 0;
      public int memberNumber;
      public int memberArraySize = 1;
      public boolean mfgMember;
      public String description;

      public int getObjectIndex(int ndx) {
         if (this.lastObjIndex == 0) {
            return this.firstObjIndex;
         } else {
            return this.memberArraySize <= 1 ? this.firstObjIndex + ndx : this.firstObjIndex + ndx / this.memberArraySize;
         }
      }

      public int getMemberIndex(int ndx) {
         if (this.memberArraySize <= 1) {
            return this.memberNumber;
         } else if (this.lastObjIndex == 0) {
            return this.memberNumber + ndx;
         } else {
            int objArraySize = this.lastObjIndex - this.firstObjIndex + 1;
            return this.memberNumber + ndx % objArraySize;
         }
      }

      @Override
      public String toString() {
         StringBuilder sb = new StringBuilder();
         sb.append("ObjIndex : " + this.firstObjIndex);
         if (this.lastObjIndex != 0) {
            sb.append(" - " + this.lastObjIndex);
         }

         sb.append("\nmemberNumber : " + this.memberNumber + "\n");
         if (this.memberArraySize != 0) {
            sb.append("memberArraySize : " + this.memberArraySize + "\n");
         }

         sb.append("mfgMember : " + this.mfgMember + "\n");
         sb.append("description : " + this.description + "\n");
         return sb.toString();
      }

      public String getObjectIndexString() {
         return Integer.toString(this.firstObjIndex) + (this.lastObjIndex != 0 ? "-" + this.lastObjIndex : "");
      }

      public boolean isObjectArrayed() {
         return this.lastObjIndex > this.firstObjIndex;
      }

      public int getPerObjectNvCount() {
         return this.memberArraySize;
      }
   }
}
