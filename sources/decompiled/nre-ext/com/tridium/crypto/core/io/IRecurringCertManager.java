package com.tridium.crypto.core.io;

import java.security.cert.X509Certificate;

interface IRecurringCertManager {
   void usedRecurringCert(X509Certificate var1);

   void removeRecurringCert(X509Certificate var1);

   void saveRecurringCerts() throws Exception;

   void loadRecurringCerts() throws Exception;

   void clearRecurringCerts();
}
