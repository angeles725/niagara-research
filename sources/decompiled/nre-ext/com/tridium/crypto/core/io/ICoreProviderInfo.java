package com.tridium.crypto.core.io;

import com.tridium.crypto.core.provider.IProvider;
import java.util.Enumeration;

public interface ICoreProviderInfo {
   Enumeration<? extends IProvider> providers() throws Exception;

   IProvider getProvider(String var1) throws Exception;
}
