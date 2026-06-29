package com.tridium.bacnet.stack.network.wiretap;

import com.tridium.bacnet.stack.network.NetworkPdu;

public interface OutgoingWiretap {
   void sendRequest(byte[] var1, NetworkPdu var2);
}
