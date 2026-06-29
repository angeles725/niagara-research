package com.tridium.fox.sys.data;

import java.util.List;
import javax.baja.sys.Context;
import javax.baja.tag.Entity;

public interface EntityExportConsumer {
   boolean canAcceptEntitiesFromRemoteExport(Context var1);

   void consumeEntitiesFromRemoteExport(List<Entity> var1, Context var2);
}
