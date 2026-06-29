package com.tridium.fox.sys;

import com.tridium.fox.message.FoxMessage;
import javax.baja.naming.BHost;

public interface NiagaraStation extends FoxConnectionTarget {
   String getStationName();

   BHost getRemoteHost();

   int getFoxPort();

   String getScheme();

   void clientOpened();

   void clientClosed();

   void serverOpened();

   void serverClosed();

   default void serverConnectionStopped(BFoxServerConnection conn) {
   }

   void initHello(FoxMessage var1) throws Exception;

   void pingOk();

   void pingFail(String var1);

   boolean isFatalFault();

   String getFaultCause();
}
