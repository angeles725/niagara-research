package com.tridium.fox.kerberos;

import java.io.IOException;
import java.security.AccessController;
import javax.baja.security.BICredentials;
import javax.baja.security.BUsernameAndPassword;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

public class KerberosCallbackHandler implements CallbackHandler {
   String username = "";
   String password = "";

   public KerberosCallbackHandler(BICredentials credentials) {
      if (credentials instanceof BUsernameAndPassword) {
         BUsernameAndPassword creds = (BUsernameAndPassword)credentials;
         this.username = creds.getUsername();
         this.password = AccessController.doPrivileged(creds.getPassword()::getValue);
      }
   }

   public KerberosCallbackHandler(String username, String password) {
      this.username = username;
      this.password = password;
   }

   @Override
   public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
      for (int i = 0; i < callbacks.length; i++) {
         if (callbacks[i] instanceof NameCallback) {
            NameCallback nc = (NameCallback)callbacks[i];
            nc.setName(this.username);
         } else {
            if (!(callbacks[i] instanceof PasswordCallback)) {
               throw new UnsupportedCallbackException(callbacks[i], "Unrecognized callback.");
            }

            PasswordCallback pc = (PasswordCallback)callbacks[i];
            pc.setPassword(this.password.toCharArray());
         }
      }
   }
}
