package com.tridium.template.file;

import javax.baja.file.BIFileStore;
import javax.baja.file.types.image.BImageFile;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BMemoryImageFile extends BImageFile {
   public static final Type TYPE = Sys.loadType(BMemoryImageFile.class);
   private BOrd localImageOrd;

   public Type getType() {
      return TYPE;
   }

   public BMemoryImageFile(BIFileStore store, BOrd localImageOrd) {
      super(store);
      this.localImageOrd = localImageOrd;
   }

   public BMemoryImageFile() {
   }

   public void setLocalImageOrd(BOrd localImageOrd) {
      this.localImageOrd = localImageOrd;
   }

   public BOrd getLocalImageOrd() {
      return this.localImageOrd;
   }
}
