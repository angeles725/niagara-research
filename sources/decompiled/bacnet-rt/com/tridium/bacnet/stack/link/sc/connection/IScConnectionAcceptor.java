package com.tridium.bacnet.stack.link.sc.connection;

import java.util.logging.Logger;

public interface IScConnectionAcceptor extends IScConnectionManager {
   Logger getLogger();

   boolean canAcceptConnections();

   BAcceptingConnection fetchConnection() throws Exception;

   int getMaxBvlcLength();
}
