package com.tridium.bacnet.stack.link.sc.connection;

import com.tridium.bacnet.stack.link.sc.message.ScNpdu;

public interface IScConnectionManager {
   String getSubProtocol();

   void activateConnection(BAbstractConnection var1) throws DuplicateVmacException;

   void deactivateConnection(BAbstractConnection var1);

   void forwardNpdu(long var1, long var3, ScNpdu var5) throws Exception;
}
