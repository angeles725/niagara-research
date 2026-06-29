package com.tridium.lonworks.xml;

import java.util.Vector;

public class XLonInterfaceFile extends XLonData {
   public Vector<XLonInterfaceFile> imports = new Vector<>();
   public Vector<XEnumDef> enums = new Vector<>();
   public Vector<XTypeDef> types = new Vector<>();
   public Vector<XLonDevice> devices = new Vector<>();
   private String file = "";
   public String protect = "";

   @Override
   public void addAttribute(String name, Object obj) {
      if (obj instanceof XEnumDef) {
         this.enums.addElement((XEnumDef)obj);
      } else if (obj instanceof XTypeDef) {
         this.types.addElement((XTypeDef)obj);
      } else if (obj instanceof XLonDevice) {
         this.devices.addElement((XLonDevice)obj);
      } else if (obj instanceof XLonInterfaceFile) {
         this.imports.addElement((XLonInterfaceFile)obj);
      }
   }

   public void setFile(String file) {
      this.file = file;
   }

   public String getFile() {
      return this.file;
   }

   public XLonDevice getLonDevice() {
      return this.devices.size() == 0 ? null : this.devices.elementAt(0);
   }

   public XTypeDef resolveTypeDef(XTypeDef outerType, String name) {
      for (int i = 0; i < this.types.size(); i++) {
         XTypeDef t = this.types.elementAt(i);
         if (t != outerType && t.getName().equals(name)) {
            return t;
         }
      }

      for (int ix = 0; ix < this.imports.size(); ix++) {
         XTypeDef def = this.imports.elementAt(ix).resolveTypeDef(name);
         if (def != null && def != outerType) {
            return def;
         }
      }

      return null;
   }

   public XTypeDef resolveTypeDef(String name) {
      return this.resolveTypeDef(null, name);
   }

   public boolean includes(String lnml) {
      for (int i = 0; i < this.imports.size(); i++) {
         XLonInterfaceFile xfile = this.imports.elementAt(i);
         if (xfile.getName().equals(lnml) || xfile.includes(lnml)) {
            return true;
         }
      }

      return false;
   }

   public XEnumDef resolveEnumDef(String name) {
      for (int i = 0; i < this.enums.size(); i++) {
         XEnumDef t = this.enums.elementAt(i);
         if (t.getName().equals(name)) {
            return t;
         }
      }

      for (int ix = 0; ix < this.imports.size(); ix++) {
         XEnumDef def = this.imports.elementAt(ix).resolveEnumDef(name);
         if (def != null) {
            return def;
         }
      }

      return null;
   }

   public String resolveTypeDefToLnml(String name) {
      for (int i = 0; i < this.types.size(); i++) {
         XTypeDef t = this.types.elementAt(i);
         if (t.getName().equals(name)) {
            return this.getName();
         }
      }

      for (int ix = 0; ix < this.imports.size(); ix++) {
         XLonInterfaceFile xfile = this.imports.elementAt(ix);
         XTypeDef def = xfile.resolveTypeDef(name);
         if (def != null) {
            return xfile.getName();
         }
      }

      return null;
   }

   public String resolveEnumDefToLnml(String name) {
      for (int i = 0; i < this.enums.size(); i++) {
         XEnumDef t = this.enums.elementAt(i);
         if (t.getName().equals(name)) {
            return this.getName();
         }
      }

      for (int ix = 0; ix < this.imports.size(); ix++) {
         XLonInterfaceFile xfile = this.imports.elementAt(ix);
         XEnumDef def = xfile.resolveEnumDef(name);
         if (def != null) {
            return xfile.getName();
         }
      }

      return null;
   }

   public XTypeDef getXType(String typeScope) {
      for (int i = 0; i < this.types.size(); i++) {
         XTypeDef t = this.types.elementAt(i);
         if (!t.isCpType() && t.getTypeScope().equals(typeScope)) {
            return t;
         }
      }

      return null;
   }

   public XCpTypeDef getXCpType(String typeScope) {
      for (int i = 0; i < this.types.size(); i++) {
         XTypeDef t = this.types.elementAt(i);
         if (t.isCpType() && t.getTypeScope().equals(typeScope)) {
            return (XCpTypeDef)t;
         }
      }

      return null;
   }
}
