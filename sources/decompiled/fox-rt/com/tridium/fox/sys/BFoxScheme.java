package com.tridium.fox.sys;

import com.tridium.authn.AuthenticationClient;
import javax.baja.naming.BHost;
import javax.baja.naming.BStationSessionScheme;
import javax.baja.naming.OrdQuery;
import javax.baja.naming.OrdQueryList;
import javax.baja.naming.OrdTarget;
import javax.baja.naming.SyntaxException;
import javax.baja.naming.UnresolvedException;
import javax.baja.nre.annotations.NiagaraSingleton;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.AuthenticationException;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType(
   ordScheme = "fox"
)
@NiagaraSingleton
public class BFoxScheme extends BStationSessionScheme {
   public static final BFoxScheme INSTANCE = new BFoxScheme();
   public static final Type TYPE = Sys.loadType(BFoxScheme.class);
   public static final int DEFAULT_PORT = 1911;
   public static final String SCHEME_NAME = "fox";

   public Type getType() {
      return TYPE;
   }

   private BFoxScheme() {
      super("fox");
   }

   public OrdQuery parse(String queryBody) {
      return new BFoxScheme.FoxQuery(queryBody);
   }

   public OrdTarget resolve(OrdTarget base, OrdQuery query) throws SyntaxException, UnresolvedException {
      return this.resolve(base, query, null);
   }

   public OrdTarget resolve(OrdTarget base, OrdQuery query, AuthenticationClient client) throws SyntaxException, UnresolvedException {
      BHost host = null;
      BObject obj = base.get();
      if (obj instanceof BHost) {
         host = (BHost)obj;
      } else {
         host = ((BComponent)obj).getHost();
      }

      BFoxScheme.FoxQuery foxQuery = (BFoxScheme.FoxQuery)query;
      int port = foxQuery.port;
      BFoxSession session = BFoxSession.make(null, host, port, false);
      session.getConnection().setAuthenticationClient(client);

      try {
         session = BFoxSession.connect(session);
         return new OrdTarget(base, session);
      } catch (LocalizableServerException var10) {
         if (var10.getLexiconModule().equals("fox") && var10.getLexiconKey().equals("error.NoPermissionForStation")) {
            session.disconnect();
         }

         throw var10;
      } catch (AuthenticationException var11) {
         throw var11;
      } catch (Throwable var12) {
         throw new UnresolvedException(query.toString(), var12);
      }
   }

   public int getDefaultPort() {
      return 1911;
   }

   public static class FoxQuery implements OrdQuery {
      int port;

      FoxQuery(String body) {
         body = body.trim();
         if (body.length() == 0) {
            this.port = 1911;
         } else {
            this.port = Integer.parseInt(body);
         }
      }

      FoxQuery(int port) {
         this.port = port;
      }

      public boolean isHost() {
         return false;
      }

      public boolean isSession() {
         return true;
      }

      public void normalize(OrdQueryList list, int index) {
         list.shiftToHost(index);
      }

      public String getScheme() {
         return "fox";
      }

      public String getBody() {
         StringBuilder sb = new StringBuilder();
         if (this.port != 1911) {
            sb.append(String.valueOf(this.port));
         }

         return sb.toString();
      }

      @Override
      public String toString() {
         return "fox:" + this.getBody();
      }

      public int getPort() {
         return this.port;
      }
   }
}
