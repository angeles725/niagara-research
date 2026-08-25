package com.tridium.crypto.core.provider;

import java.util.Enumeration;

public interface IProvider {
   String getName();

   double getVersion();

   String getDescription();

   Enumeration<? extends IProviderSection> sections();
}
