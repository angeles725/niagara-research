package com.tridium.fox.sys.data;

import com.tridium.fox.sys.BFoxClientConnection;
import com.tridium.fox.sys.BFoxSession;
import com.tridium.fox.sys.broker.BFoxStationSpace;
import com.tridium.query.BSingleScheme;
import com.tridium.sys.station.BStationScheme;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import javax.baja.entityIo.json.JsonEntityEncoder;
import javax.baja.entityIo.json.JsonEntityEncoder.Options;
import javax.baja.naming.BOrd;
import javax.baja.naming.BOrdList;
import javax.baja.naming.OrdQuery;
import javax.baja.naming.OrdTarget;
import javax.baja.nav.BINavNode;
import javax.baja.nre.annotations.NiagaraSingleton;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.query.BIQueryHandler;
import javax.baja.query.BQueryScheme;
import javax.baja.registry.TypeInfo;
import javax.baja.space.BISpaceNode;
import javax.baja.space.BSpace;
import javax.baja.sys.BObject;
import javax.baja.sys.BSingleton;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.tag.Entity;
import javax.baja.util.CloseableIterator;
import javax.baja.util.CloseableIteratorWrapper;
import javax.baja.util.Version;

@NiagaraType
@NiagaraSingleton
public final class BFoxQueryHandler extends BSingleton implements BIQueryHandler {
   public static final BFoxQueryHandler INSTANCE = new BFoxQueryHandler();
   public static final Type TYPE = Sys.loadType(BFoxQueryHandler.class);
   private static final BIPostQueryFilter[] queryFilters = new BIPostQueryFilter[]{BTranslateOrdPostQueryFilter.INSTANCE};
   private static final Version VER_4_9 = new Version("4.9");
   private static final CloseableIterator<Entity> EMPTY_ITERATOR = new CloseableIteratorWrapper(Collections.emptyIterator());
   private static final Collection<TypeInfo> QUERY_SCHEMES;

   public Type getType() {
      return TYPE;
   }

   public boolean canHandle(OrdTarget scope, BQueryScheme scheme) {
      BSpace space = scope.getSpace();
      return space != null && space.getType().is(BFoxStationSpace.TYPE);
   }

   public CloseableIterator<Entity> query(OrdTarget scope, OrdQuery query) {
      BFoxSession foxSession = (BFoxSession)scope.getSpace().getSession();
      BFoxClientConnection conn = foxSession.getConnection();
      BDataChannel channel = (BDataChannel)conn.getChannels().get("data");

      try {
         BOrd ord = null;
         BObject base = scope.get();
         if (base instanceof BISpaceNode) {
            ord = ((BISpaceNode)base).getAbsoluteOrd();
         }

         ord = getLookAheadOrd(foxSession, scope, ord, query);
         if (ord == null) {
            return EMPTY_ITERATOR;
         } else {
            Options encodingOptions = new Options(JsonEntityEncoder.shouldEncodeTags(scope), JsonEntityEncoder.shouldEncodeRelations(scope));
            Stream<Entity> entityStream = channel.resolveEntities(BOrdList.make(ord), 0, -1, encodingOptions, null, queryFilters);
            return new BFoxQueryHandler.RemoteEntityIterator(entityStream);
         }
      } catch (RuntimeException var10) {
         throw var10;
      } catch (Exception var11) {
         throw new BajaRuntimeException(var11);
      }
   }

   public static BOrd getLookAheadOrd(BFoxSession session, OrdTarget base, BOrd baseOrd, OrdQuery query) {
      BObject rootBaseObj = base.get();

      for (OrdTarget baseOrdTarget = base.getBaseOrdTarget(); baseOrdTarget != null; baseOrdTarget = baseOrdTarget.getBaseOrdTarget()) {
         rootBaseObj = baseOrdTarget.get();
      }

      BOrd ord = rootBaseObj instanceof BINavNode ? ((BINavNode)rootBaseObj).getNavOrd() : base.getSpace().getNavOrd();
      boolean remoteVersionSupportsSingleScheme = isRemoteVersion4_9(session);

      for (int i = 0; i < base.depth(); i++) {
         OrdQuery ordQuery = base.queryAt(i);
         ord = ord != null && !ord.isNull() ? BOrd.make(ord, ordQuery) : BOrd.make(ordQuery);
         if (query == ordQuery) {
            if (++i < base.depth()) {
               ordQuery = base.queryAt(i);
               String ordScheme = ordQuery.getScheme();
               TypeInfo ordSchemeType = Sys.getRegistry().getOrdScheme(ordScheme);
               if (QUERY_SCHEMES.stream().anyMatch(t -> ordSchemeType.is(t))) {
                  return null;
               }

               if (remoteVersionSupportsSingleScheme && ordSchemeType.is(BSingleScheme.TYPE)) {
                  ord = BOrd.make(ord, ordQuery);
               }
            }
            break;
         }

         if ((BStationScheme.INSTANCE.getId().equals(ordQuery.getScheme()) || "nspace".equals(ordQuery.getScheme()))
            && i + 1 < base.depth()
            && "bql".equals(base.queryAt(i + 1).getScheme())) {
            ord = BOrd.make(ord, "slot:");
         }
      }

      if (ord != null) {
         ord = ord.relativizeToSession();
      }

      return ord;
   }

   private static boolean isRemoteVersion4_9(BObject obj) {
      try {
         Version remoteVersion = (Version)obj.fw(404, "baja", null, null, null);
         return remoteVersion.compareTo(VER_4_9) >= 0;
      } catch (Exception var2) {
         return false;
      }
   }

   static {
      List<TypeInfo> querySchemes = new ArrayList<>();
      querySchemes.add(BQueryScheme.TYPE.getTypeInfo());

      try {
         querySchemes.add(Sys.getRegistry().getOrdScheme("bql"));
      } catch (Exception var2) {
      }

      QUERY_SCHEMES = Collections.unmodifiableList(querySchemes);
   }

   private static class RemoteEntityIterator implements CloseableIterator<Entity> {
      private final Stream<Entity> entityStream;
      private final Iterator<Entity> entityIterator;

      RemoteEntityIterator(Stream<Entity> entityStream) {
         this.entityStream = entityStream;
         this.entityIterator = entityStream.iterator();
      }

      public void close() {
         this.entityStream.close();
      }

      public boolean hasNext() {
         return this.entityIterator.hasNext();
      }

      public Entity next() {
         return this.entityIterator.next();
      }
   }
}
