package com.tridium.platDaemon.ui.widget;

import com.tridium.platform.daemon.BDaemonSession;
import com.tridium.platform.ui.BDaemonCnxHandler;
import com.tridium.workbench.auth.AuthUtil;
import com.tridium.workbench.auth.BHostCnxHandler;
import com.tridium.workbench.auth.CredentialsList;
import java.net.ConnectException;
import javax.baja.security.BICredentials;

public class DaemonCredentialsManager {
   public static void setCredentials(BDaemonSession session, BICredentials credentials) throws ConnectException {
      session.setCredentials(credentials, true);
      String realm = session.getAuthenticationRealmName();
      if (CredentialsList.INSTANCE.lookup(realm) != null) {
         AuthUtil.saveCredentials(realm, credentials);
      }

      BHostCnxHandler handler = new BDaemonCnxHandler();
      handler.setHost(session.getHost());
      handler.setPort(session.getPort());
      handler.setCredentials(credentials);
      String realm2 = handler.toAuthenticationRealm();
      if (!realm2.equals(realm) && CredentialsList.INSTANCE.lookup(realm2) != null) {
         AuthUtil.saveCredentials(realm2, credentials);
      }
   }
}
