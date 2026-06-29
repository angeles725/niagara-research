package com.tridium.bacnet.stack.link.ip.util;

import com.tridium.bacnet.stack.link.ip.BacnetNetworkAdapter;
import java.net.NetworkInterface;
import java.net.SocketException;

public class BacnetIpAdapter implements BacnetNetworkAdapter {
   private String identifier;
   private String description;
   private String address;
   private boolean loopback = false;

   public BacnetIpAdapter(NetworkInterface netIf, String address) throws SocketException {
      this(netIf.getName(), netIf.getDisplayName(), netIf.isLoopback(), address);
   }

   public BacnetIpAdapter(String id, String description, boolean loopback, String address) {
      this.loopback = loopback;
      this.identifier = id;
      this.description = description;
      this.address = address;
   }

   @Override
   public String getIdentifier() {
      return this.identifier;
   }

   @Override
   public String getDescription() {
      return this.description;
   }

   @Override
   public String getAddress() {
      return this.address;
   }

   @Override
   public boolean isLoopback() {
      return this.loopback;
   }
}
