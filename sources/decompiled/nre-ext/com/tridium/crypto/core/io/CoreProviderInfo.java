package com.tridium.crypto.core.io;

import com.tridium.crypto.core.provider.IProvider;
import com.tridium.crypto.core.provider.NProvider;
import java.util.Enumeration;

public class CoreProviderInfo implements ICoreProviderInfo {
   @Override
   public Enumeration<? extends IProvider> providers() throws Exception {
      return NProvider.getProviderElements();
   }

   @Override
   public IProvider getProvider(String name) throws Exception {
      return NProvider.getProvider(name);
   }
}
