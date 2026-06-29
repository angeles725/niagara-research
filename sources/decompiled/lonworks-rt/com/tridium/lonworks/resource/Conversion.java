package com.tridium.lonworks.resource;

import com.tridium.lonworks.util.NameUtil;
import com.tridium.lonworks.xml.XEnumDef;
import com.tridium.lonworks.xml.XTypeDef;
import java.util.Vector;
import javax.baja.file.BIFile;
import javax.baja.naming.BOrd;
import javax.baja.nre.util.IntHashMap;
import javax.baja.xml.XElem;
import javax.baja.xml.XParser;
import javax.baja.xml.XWriter;

public class Conversion {
   Conversion.UnitPrompt unitPrompt = null;
   public static final String UNMODIFIED = "???";
   private IntHashMap map = new IntHashMap(256);
   Vector<Conversion.TypeClass> typeVect = new Vector<>();
   Vector<Conversion.BooleanEnum> boolEnumVect = new Vector<>();
   Vector<Conversion.Rename> enumDefRenameVect = new Vector<>();
   Vector<Conversion.Rename> typeDefRenameVect = new Vector<>();
   BIFile ifile = null;
   private boolean modified = false;

   public Conversion(String fileName) {
      BOrd ord = BOrd.make("file:" + fileName);
      this.ifile = (BIFile)ord.resolve().get();

      try {
         XParser p = XParser.make(this.ifile.getInputStream());
         this.load(p);
      } catch (Throwable var4) {
      }
   }

   public Conversion(BIFile f) {
      this.ifile = f;

      try {
         XParser p = XParser.make(this.ifile.getInputStream());
         this.load(p);
      } catch (Throwable var3) {
      }
   }

   public void flush() {
      if (this.modified) {
         try {
            this.doFlush(new XWriter(this.ifile.getOutputStream()));
         } catch (Throwable var2) {
            var2.printStackTrace();
         }
      }
   }

   public String getConversionString(int scope, int index, String original) {
      int hashCode = this.getHash(scope, index);
      Conversion.StringRec rec = (Conversion.StringRec)this.map.get(hashCode);
      if (rec == null || !rec.original.equals(original)) {
         rec = new Conversion.StringRec();
         rec.scope = scope;
         rec.index = index;
         rec.original = original;
         rec.newString = this.unitPrompt != null ? this.unitPrompt.getUserString(scope, index, original) : "???";
         this.map.put(hashCode, rec);
         this.modified = true;
         return rec.newString;
      } else {
         return !rec.newString.equals("???") ? rec.newString : original;
      }
   }

   public boolean isBooleanEnum(String name) {
      String ename = NameUtil.toJavaName(name, true);

      for (int i = 0; i < this.boolEnumVect.size(); i++) {
         if (this.boolEnumVect.elementAt(i).enumName.equals(ename)) {
            return true;
         }
      }

      return false;
   }

   public void renameEnumDef(XEnumDef xenum) {
      String ename = xenum.getName();

      for (int i = 0; i < this.enumDefRenameVect.size(); i++) {
         Conversion.Rename ren;
         if ((ren = this.enumDefRenameVect.elementAt(i)).name.equals(ename)) {
            int cnt = ren.origName.length;

            for (int j = 0; j < cnt; j++) {
               xenum.rename(ren.origName[j], ren.newName[j]);
            }
         }
      }
   }

   public void renameTypeDef(XTypeDef xtype) {
      String tname = xtype.getName();

      for (int i = 0; i < this.typeDefRenameVect.size(); i++) {
         Conversion.Rename ren;
         if ((ren = this.typeDefRenameVect.elementAt(i)).name.equals(tname)) {
            int cnt = ren.origName.length;

            for (int j = 0; j < cnt; j++) {
               xtype.rename(ren.origName[j], ren.newName[j]);
            }
         }
      }
   }

   private void load(XParser p) throws Exception {
      XElem root = p.parse();
      XElem[] q = root.elems("resString");

      for (int i = 0; i < q.length; i++) {
         Conversion.StringRec rec = new Conversion.StringRec();
         rec.scope = Integer.decode(q[i].get("scp"));
         rec.index = Integer.decode(q[i].get("ndx"));
         rec.original = q[i].get("org");
         rec.newString = q[i].get("new");
         this.map.put(this.getHash(rec.scope, rec.index), rec);
      }

      q = root.elems("typeSpec");

      for (int i = 0; i < q.length; i++) {
         Conversion.TypeClass rec = new Conversion.TypeClass();
         rec.typeDef = q[i].get("typeDef");
         rec.bajaModule = q[i].get("bajaModule", null);
         rec.className = q[i].get("class");
         this.typeVect.addElement(rec);
      }

      q = root.elems("booleanEnum");

      for (int i = 0; i < q.length; i++) {
         Conversion.BooleanEnum rec = new Conversion.BooleanEnum();
         rec.enumName = q[i].get("ename");
         this.boolEnumVect.addElement(rec);
      }

      q = root.elems("rename");

      for (int i = 0; i < q.length; i++) {
         String type = q[i].attrName(0);
         Conversion.Rename ren = new Conversion.Rename();
         ren.name = q[i].attrValue(0);
         if (type.equals("enumDef")) {
            this.enumDefRenameVect.addElement(ren);
         } else {
            if (!type.equals("typeDef")) {
               throw new RuntimeException("rename entry must have \"enumDef\" or \"typeDef\" attribute.");
            }

            this.typeDefRenameVect.addElement(ren);
         }

         int cnt = q[i].contentSize();
         ren.origName = new String[cnt];
         ren.newName = new String[cnt];

         for (int j = 0; j < cnt; j++) {
            XElem xe = (XElem)q[i].content(j);
            ren.origName[j] = xe.name();
            ren.newName[j] = xe.get("newName");
         }
      }
   }

   private void doFlush(XWriter writer) {
      try {
         writer.prolog().nl().w("<conversion>\n");
         Conversion.StringRec[] recs = this.getOrderedList();

         for (int i = 0; i < recs.length; i++) {
            Conversion.StringRec rec = recs[i];
            writer.w("  <resString")
               .attr(" scp", Integer.toString(rec.scope))
               .attr(" ndx", Integer.toString(rec.index))
               .attr(" org", rec.original)
               .attr(" new", rec.newString)
               .w("/>\n");
         }

         for (int i = 0; i < this.typeVect.size(); i++) {
            Conversion.TypeClass rec = this.typeVect.elementAt(i);
            writer.w("  <typeSpec").attr(" typeDef", rec.typeDef).attr(" bajaModule", rec.bajaModule).attr(" class", rec.className).w("/>\n");
         }

         for (int i = 0; i < this.boolEnumVect.size(); i++) {
            Conversion.BooleanEnum rec = this.boolEnumVect.elementAt(i);
            writer.w("  <booleanEnum").attr(" ename", rec.enumName).w("/>\n");
         }

         for (int i = 0; i < this.enumDefRenameVect.size(); i++) {
            Conversion.Rename ren = this.enumDefRenameVect.elementAt(i);
            this.writeRenames(" enumDef", writer, ren);
         }

         for (int i = 0; i < this.typeDefRenameVect.size(); i++) {
            Conversion.Rename ren = this.typeDefRenameVect.elementAt(i);
            this.writeRenames(" typeDef", writer, ren);
         }

         writer.w("</conversion>\n");
         writer.flush();
         writer.close();
      } catch (Throwable var5) {
         var5.printStackTrace();
      }
   }

   private void writeRenames(String type, XWriter writer, Conversion.Rename ren) {
      writer.w("  <rename").attr(type, ren.name).w(">\n");
      int cnt = ren.origName.length;

      for (int i = 0; i < cnt; i++) {
         writer.w("    <").w(ren.origName[i]).attr(" newName", ren.newName[i]).w("/>\n");
      }

      writer.w("  </rename>\n");
   }

   private Conversion.StringRec[] getOrderedList() {
      try {
         Conversion.StringRec[] recs = new Conversion.StringRec[this.map.size()];
         recs = (Conversion.StringRec[])this.map.toArray(recs);
         int i = 0;

         while (i < recs.length - 1) {
            Conversion.StringRec rec = recs[i];
            if (rec.isGreaterThan(recs[i + 1])) {
               recs[i] = recs[i + 1];
               recs[i + 1] = rec;
               i = i == 0 ? ++i : --i;
            } else {
               i++;
            }
         }

         return recs;
      } catch (Throwable var4) {
         var4.printStackTrace();
         return new Conversion.StringRec[0];
      }
   }

   private int getHash(int scope, int index) {
      return (scope << 16) + index;
   }

   public void setUnitPrompt(Conversion.UnitPrompt u) {
      this.unitPrompt = u;
   }

   static class BooleanEnum {
      String enumName;
   }

   static class Rename {
      String name;
      String[] origName;
      String[] newName;
   }

   static class StringRec {
      int scope;
      int index;
      String original;
      String newString;

      boolean isGreaterThan(Conversion.StringRec r) {
         if (this.scope > r.scope) {
            return true;
         } else {
            return this.scope < r.scope ? false : this.index > r.index;
         }
      }
   }

   static class TypeClass {
      String typeDef = "";
      String bajaModule;
      String className;
   }

   public abstract static class UnitPrompt {
      public abstract String getUserString(int var1, int var2, String var3);
   }
}
