package com.tridium.bacnet.stack.link.ip;

public interface BacnetNetworkAdapter {
   String getIdentifier();

   String getDescription();

   String getAddress();

   boolean isLoopback();
}
