package com.tridium.fox.session;

import com.tridium.authn.LoginFailureCause;
import com.tridium.fox.message.FoxMessage;

public interface FoxConnection {
   void initHello(FoxMessage var1) throws Exception;

   void sessionOpened(FoxSession var1);

   FoxResponse process(FoxRequest var1) throws Throwable;

   void circuitOpened(FoxCircuit var1) throws Throwable;

   void sessionClosed(FoxSession var1, Throwable var2);

   default void sessionClosed(FoxSession session, Throwable cause, LoginFailureCause failureCause) {
      this.sessionClosed(session, cause);
   }

   void error(String var1, Throwable var2);

   Thread makeThread(ThreadGroup var1, Runnable var2, String var3);
}
