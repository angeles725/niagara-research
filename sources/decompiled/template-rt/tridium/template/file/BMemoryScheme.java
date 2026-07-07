package com.tridium.template.file;

import javax.baja.file.FilePath;
import javax.baja.naming.BOrdScheme;
import javax.baja.naming.OrdQuery;
import javax.baja.naming.OrdTarget;
import javax.baja.nre.annotations.NiagaraSingleton;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BObject;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType(
   ordScheme = "memory"
)
@NiagaraSingleton
public class BMemoryScheme extends BOrdScheme {
   public static final BMemoryScheme INSTANCE = new BMemoryScheme();
   public static final Type TYPE = Sys.loadType(BMemoryScheme.class);

   public Type getType() {
      return TYPE;
   }

   public BMemoryScheme() {
      super("memory");
   }

   public OrdQuery parse(String queryBody) {
      return MemoryPath.make(queryBody);
   }

   public OrdTarget resolve(OrdTarget base, OrdQuery query) {
      FilePath path = (FilePath)query;
      BMemoryFileSpace fs = BMemoryFileSpace.INSTANCE;
      return new OrdTarget(base, (BObject)fs.resolveFile(path));
   }
}
