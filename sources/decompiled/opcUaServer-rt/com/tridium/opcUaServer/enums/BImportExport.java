package com.tridium.opcUaServer.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("Export"), @Range("Import")}
)
public final class BImportExport extends BFrozenEnum {
   public static final int EXPORT = 0;
   public static final int IMPORT = 1;
   public static final BImportExport Export = new BImportExport(0);
   public static final BImportExport Import = new BImportExport(1);
   public static final BImportExport DEFAULT = Export;
   public static final Type TYPE = Sys.loadType(BImportExport.class);

   public static BImportExport make(int ordinal) {
      return (BImportExport)Export.getRange().get(ordinal, false);
   }

   public static BImportExport make(String tag) {
      return (BImportExport)Export.getRange().get(tag);
   }

   private BImportExport(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
