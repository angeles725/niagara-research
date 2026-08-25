package com.tridium.niagarad.http;

import java.security.SecureRandom;
import javax.baja.nre.util.TextUtil;
import javax.servlet.http.HttpServletRequest;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.HouseKeeper;

public class PlatformSessionIdManager extends DefaultSessionIdManager {
   private static final int SESS_ID_LEN = 25;
   private static SecureRandom rand = new SecureRandom();

   public PlatformSessionIdManager(Server server) {
      super(server);
      this._workerName = "";
      this._houseKeeper = new HouseKeeper();
      this._houseKeeper.setSessionIdManager(this);

      try {
         this._houseKeeper.setIntervalSec(190L);
      } catch (Exception var3) {
      }
   }

   public String newSessionId(long seedTerm) {
      String sessionId;
      do {
         byte[] sessionBytes = new byte[25];
         rand.nextBytes(sessionBytes);
         sessionId = TextUtil.bytesToHexString(sessionBytes);
      } while (this.isIdInUse(sessionId));

      return sessionId;
   }

   public void setWorkerName(String workerName) {
      super.setWorkerName("");
   }

   public String getExtendedId(String clusterId, HttpServletRequest request) {
      return clusterId;
   }
}
