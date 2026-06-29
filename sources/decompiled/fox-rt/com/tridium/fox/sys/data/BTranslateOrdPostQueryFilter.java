package com.tridium.fox.sys.data;

import java.util.logging.Level;
import java.util.stream.Stream;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdQuery;
import javax.baja.naming.SlotPath;
import javax.baja.nav.BINavNode;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.space.BHandleScheme;
import javax.baja.sys.BObject;
import javax.baja.sys.BStruct;
import javax.baja.sys.Context;
import javax.baja.sys.NotRunningException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.tag.Entity;

@NiagaraType
public final class BTranslateOrdPostQueryFilter extends BStruct implements BIPostQueryFilter {
   public static final Type TYPE = Sys.loadType(BTranslateOrdPostQueryFilter.class);
   public static final BTranslateOrdPostQueryFilter INSTANCE = new BTranslateOrdPostQueryFilter();

   public Type getType() {
      return TYPE;
   }

   @Override
   public Stream<Entity> postQueryFilter(Stream<Entity> queryResults, Context cx) {
      return queryResults.map(e -> new TranslatedEntity(e, BTranslateOrdPostQueryFilter::translateOrd));
   }

   public static BOrd translateOrd(BOrd ord, BObject base) {
      if (ord.isNull()) {
         return ord;
      } else {
         BOrd normalizedOrd = ord.normalize();
         OrdQuery[] queries = normalizedOrd.parse();
         boolean convertedHandleToSlotPath = false;
         boolean hostOrSessionOrd = true;

         for (int i = 0; i < queries.length; i++) {
            hostOrSessionOrd &= queries[i].isHost() || queries[i].isSession();
            if (BHandleScheme.INSTANCE.getId().equals(queries[i].getScheme())) {
               try {
                  BObject obj = BOrd.make(queries, 0, i + 1).get(base);
                  SlotPath path = obj.asComponent().getSlotPath();
                  if (path == null) {
                     throw new NotRunningException("Cannot retrieve SlotPath for component (it may not be mounted).");
                  }

                  convertedHandleToSlotPath = true;
                  queries[i] = path;
               } catch (Exception var9) {
                  logNonTranslatableOrd(ord, base, var9);
                  return normalizedOrd.relativizeToSession();
               }
            }
         }

         if (convertedHandleToSlotPath) {
            normalizedOrd = BOrd.make(queries);
         } else if (hostOrSessionOrd) {
            logNonTranslatableOrd(ord, base, null);
            return normalizedOrd;
         }

         return normalizedOrd.relativizeToSession();
      }
   }

   private static void logNonTranslatableOrd(BOrd ord, BObject base, Exception e) {
      if (BDataChannel.logger.isLoggable(Level.FINE)) {
         StringBuilder sb = new StringBuilder();
         sb.append("For remote query, could not translate ORD found in Entity: '").append(ord).append('\'');
         if (base instanceof BINavNode) {
            sb.append(" against base '").append(((BINavNode)base).getNavOrd()).append('\'');
         }

         sb.append(". Leaving ORD untranslated in remote query result.");
         BDataChannel.logger.log(Level.FINE, sb.toString(), (Throwable)e);
      }
   }
}
