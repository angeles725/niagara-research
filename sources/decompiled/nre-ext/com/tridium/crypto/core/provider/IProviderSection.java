package com.tridium.crypto.core.provider;

import java.util.Enumeration;

public interface IProviderSection {
   String getName();

   Enumeration<? extends IProviderEntry> entries();
}
