package com.tridium.template.file;

import java.util.Objects;
import javax.baja.file.types.text.BPxFile;
import javax.baja.naming.BOrd;

public class PxFileRef {
   BPxFile pxFile;
   BOrd pxOrd;
   String pxName;
   Object pxEditor;

   public PxFileRef(BPxFile pxFile, BOrd pxOrd, String pxName) {
      this.pxFile = pxFile;
      this.pxOrd = pxOrd;
      this.pxName = pxName;
      this.pxEditor = null;
   }

   public void setPxFile(BPxFile pxFile) {
      this.pxFile = pxFile;
   }

   public void setPxOrd(BOrd pxOrd) {
      this.pxOrd = pxOrd;
   }

   public void setPxName(String pxName) {
      this.pxName = pxName;
   }

   public void setPxEditor(Object pxEditor) {
      this.pxEditor = pxEditor;
   }

   public BPxFile getPxFile() {
      return this.pxFile;
   }

   public BOrd getPxOrd() {
      return this.pxOrd;
   }

   public String getPxName() {
      return this.pxName;
   }

   public Object getPxEditor() {
      return this.pxEditor;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         PxFileRef pxFileRef = (PxFileRef)o;
         return Objects.equals(this.getPxOrd(), pxFileRef.getPxOrd());
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.getPxOrd().hashCode();
   }
}
