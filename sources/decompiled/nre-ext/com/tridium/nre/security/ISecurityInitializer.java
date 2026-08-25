package com.tridium.nre.security;

public interface ISecurityInitializer {
   ISecurityInfoProvider getSecurityInfoProvider();

   void initSecurityInfo(boolean var1);

   boolean isFips();

   FipsInformation getFipsInformation();

   CryptoProvider getCryptoProvider();
}
