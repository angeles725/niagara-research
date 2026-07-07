package com.tridium.template.file;

import javax.baja.file.types.text.BPxFile;
import javax.baja.naming.BOrd;

public class MemoryPxFileRef extends PxFileRef {
   private BOrd memoryPxOrd;

   public MemoryPxFileRef(BPxFile pxFile, BOrd pxOrd, BOrd memoryPxOrd, String pxName) {
      super(pxFile, pxOrd, pxName);
      this.memoryPxOrd = memoryPxOrd;
   }

   @Override
   public BOrd getPxOrd() {
      return this.memoryPxOrd;
   }

   public BOrd getFileSystemOrd() {
      return this.pxOrd;
   }
}
