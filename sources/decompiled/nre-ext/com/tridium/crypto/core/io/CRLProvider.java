package com.tridium.crypto.core.io;

import java.security.cert.CRLException;
import java.security.cert.X509CRL;
import java.util.Set;

public interface CRLProvider {
   Set<X509CRL> getCRLs() throws CRLException;
}
