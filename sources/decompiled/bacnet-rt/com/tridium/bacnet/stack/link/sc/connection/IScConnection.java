package com.tridium.bacnet.stack.link.sc.connection;

import java.util.logging.Logger;
import javax.baja.bacnet.enums.BBacnetErrorCode;

public interface IScConnection {
   Logger getLogger();

   StringBuilder getLogInfo();

   void webSocketConnected(IScWebSocket var1);

   void webSocketFailed(BBacnetErrorCode var1, String var2);

   void messageReceived(byte[] var1, int var2, int var3);
}
