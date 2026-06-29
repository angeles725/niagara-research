package com.tridium.fox.sys.user;

import com.tridium.fox.session.FoxRequest;
import com.tridium.fox.session.FoxResponse;
import com.tridium.fox.session.FoxSession;
import com.tridium.fox.session.InvalidCommandException;
import com.tridium.fox.sys.BFoxChannel;
import java.security.AccessController;
import java.util.Objects;
import javax.baja.authn.BAuthenticationScheme;
import javax.baja.authn.BPasswordAuthenticationScheme;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.BAbstractAuthenticator;
import javax.baja.security.BPassword;
import javax.baja.security.BPasswordAuthenticator;
import javax.baja.security.BPasswordCache;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Context;
import javax.baja.sys.LocalizableException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.user.BUser;
import javax.baja.util.Lexicon;

@NiagaraType
public class BUserChannel extends BFoxChannel {
   public static final Type TYPE = Sys.loadType(BUserChannel.class);
   private BOrd homePage;
   private BOrd navFile;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BUserChannel() {
      super("user");
   }

   @Override
   public void checkProcess(FoxRequest req) throws Throwable {
      if (!req.command.equals("fetchPrefs")) {
         super.checkProcess(req);
      }
   }

   @Override
   public FoxResponse process(FoxRequest req) throws Exception {
      String command = req.command;
      if (Objects.equals(command, "fetchPrefs")) {
         return this.fetchPrefs(req);
      } else if (Objects.equals(command, "setAuthenticator")) {
         return this.setAuthenticator(req);
      } else if (Objects.equals(command, "resetSessionTimeout")) {
         return this.resetSessionTimeout(req);
      } else if (Objects.equals(command, "getSessionTimeRemaining")) {
         return this.getSessionTimeRemaining(req);
      } else {
         throw new InvalidCommandException(command);
      }
   }

   public BOrd getHomePage() {
      return this.homePage;
   }

   public BOrd getNavFile() {
      return this.navFile;
   }

   public void fetchPrefs() throws Exception {
      FoxResponse resp = this.sendSync(this.makeRequest("fetchPrefs"));
      this.homePage = BOrd.make(resp.getString("homePage"));
      this.navFile = BOrd.make(resp.getString("navFile"));
   }

   private FoxResponse fetchPrefs(FoxRequest req) throws Exception {
      Objects.requireNonNull(this.getServerConnection());
      BUser user = this.getServerConnection().getUser();
      Objects.requireNonNull(user);
      FoxResponse resp = new FoxResponse(req);
      resp.add("homePage", user.getHomePage().toString());
      resp.add("navFile", user.getNavFile().toString());
      return resp;
   }

   @Override
   protected final boolean useSharedKeyEncryption() {
      return true;
   }

   public void setAuthenticator(BAbstractAuthenticator authenticator, boolean forceChange) throws Exception {
      FoxRequest req = this.makeRequest("setAuthenticator");
      req.add("authenticator", this.marshal(authenticator, null));
      req.add("forceChange", forceChange);
      FoxResponse resp = this.sendSync(req);
      String err = resp.getString("err", null);
      if (err != null) {
         throw new BajaRuntimeException(err);
      }
   }

   private FoxResponse setAuthenticator(FoxRequest req) throws Exception {
      try {
         Objects.requireNonNull(this.getServerConnection());
         BUser user = this.getServerConnection().getUser();
         Objects.requireNonNull(user);
         BAuthenticationScheme currentAuthScheme = user.getAuthenticationScheme();
         BAbstractAuthenticator newAuthenticator = (BAbstractAuthenticator)this.unmarshal(req.getString("authenticator"), null);
         if (newAuthenticator instanceof BPasswordCache) {
            BPasswordCache newPwAuthenticator = (BPasswordCache)newAuthenticator;
            BPassword newPwValue = newPwAuthenticator.getPassword();
            if (!(currentAuthScheme instanceof BPasswordAuthenticationScheme) || !(user.getAuthenticator() instanceof BPasswordAuthenticator)) {
               throw new LocalizableException(Lexicon.make("fox"), "user.incompatibleAuthenticator");
            }

            BPasswordAuthenticator currentPwAuthenticator = (BPasswordAuthenticator)user.getAuthenticator();
            if (currentPwAuthenticator.getPassword().validate(newPwValue) && req.getBoolean("forceChange", false)) {
               throw new LocalizableException(Lexicon.make("baja"), "user.strongPassword.alreadyUsed");
            }

            newPwValue = BPassword.make(AccessController.doPrivileged(newPwValue::getValue));
            currentPwAuthenticator.set(BPasswordAuthenticator.password, newPwValue, Context.forceValidate);
            currentPwAuthenticator.getPasswordConfig().setForceResetAtNextLogin(false);
         } else {
            if (currentAuthScheme instanceof BPasswordAuthenticationScheme) {
               throw new LocalizableException(Lexicon.make("fox"), "user.incompatibleAuthenticator");
            }

            user.setAuthenticator(newAuthenticator);
         }

         return new FoxResponse(req);
      } catch (Exception var8) {
         FoxResponse resp = new FoxResponse(req);
         resp.add("err", var8.getMessage());
         return resp;
      }
   }

   public long getSessionTimeRemaining() throws Exception {
      if (this.getConnection().getRemoteVersion().compareTo(FoxSession.VERSION_4_4) < 0) {
         return -1L;
      } else {
         FoxRequest req = this.makeRequest("getSessionTimeRemaining");
         FoxResponse resp = this.sendSync(req);
         return resp.getTime("sessionTimeRemaining");
      }
   }

   private FoxResponse getSessionTimeRemaining(FoxRequest request) throws Exception {
      FoxResponse resp = new FoxResponse(request);
      resp.add("sessionTimeRemaining", this.getConnection().session().getSessionTimeRemaining());
      return resp;
   }

   public long resetSessionTimeout(long offset) throws Exception {
      if (this.getConnection().getRemoteVersion().compareTo(FoxSession.VERSION_4_4) < 0) {
         return -1L;
      } else {
         FoxRequest req = this.makeRequest("resetSessionTimeout");
         req.add("offset", offset);
         FoxResponse resp = this.sendSync(req);
         return resp.getTime("sessionTimeRemaining");
      }
   }

   private FoxResponse resetSessionTimeout(FoxRequest request) throws Exception {
      FoxSession session = this.getConnection().session();
      session.resetSessionTimeout(request.getTime("offset"));
      FoxResponse resp = new FoxResponse(request);
      resp.add("sessionTimeRemaining", session.getSessionTimeRemaining());
      return resp;
   }
}
