package com.tridium.bacnet.stack.link.ip.util;

import com.tridium.bacnet.stack.link.ip.BacnetNetworkAdapter;
import java.net.SocketException;
import java.util.Collection;

public interface NetworkInterfaceProvider {
   Collection<BacnetNetworkAdapter> getInterfaces() throws SocketException;
}
