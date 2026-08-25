package com.tridium.crypto.core.cert;

import com.tridium.crypto.core.io.ICoreExemptionStore;
import com.tridium.nre.util.IPAddressUtil;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.util.Array;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import org.bouncycastle.cert.jcajce.JcaX500NameUtil;

public class TridiumHostnameVerifier implements HostnameVerifier {
   private static final String[] BAD_COUNTRY_2LDS = new String[]{"ac", "co", "com", "ed", "edu", "go", "gouv", "gov", "info", "lg", "ne", "net", "or", "org"};
   private static final List<String> LOCALHOST = Arrays.asList(
      "127.0.0.1", "localhost", "localhost.localdomain", "::1", "localhost6", "localhost6.localdomain6"
   );
   private final ICoreExemptionStore exemptionStore;
   private static final Logger logger = Logger.getLogger("crypto.hostnameverifier");

   public TridiumHostnameVerifier(ICoreExemptionStore exemptionStore) {
      this.exemptionStore = exemptionStore;
   }

   @Override
   public boolean verify(String host, SSLSession sslSession) {
      return this.verify(host, sslSession, sslSession.getPeerPort());
   }

   public boolean verify(String host, SSLSession sslSession, int port) {
      try {
         Certificate[] chain = sslSession.getPeerCertificates();
         return this.verify(host, Arrays.copyOf(chain, chain.length, X509Certificate[].class), port);
      } catch (Exception e) {
         logger.log(Level.FINE, "error verifying hostname", e);
         return false;
      }
   }

   public boolean verify(String host, X509Certificate[] chain) {
      return this.verify(host, chain, -1);
   }

   public boolean verify(String host, X509Certificate[] chain, int port) {
      if (host != null && chain != null) {
         boolean match = false;
         String hostName = host.toLowerCase();
         String matchedName = null;
         X509Certificate cert = chain[0];
         if (isLocalHost(hostName) && isLocalHost(PemSource.extractCommonName(JcaX500NameUtil.getSubject(cert)))) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine(String.format("verified %s with certificate %s (localhost)", hostName, cert.getSubjectX500Principal().getName()));
            }

            return true;
         } else {
            String[] certHostNames = getCertificateHosts(cert);

            for (String certHostName1 : certHostNames) {
               String certHostName = certHostName1.toLowerCase();
               if (verifyHostname(hostName, certHostName)) {
                  match = true;
                  matchedName = certHostName;
               }

               if (match) {
                  if (logger.isLoggable(Level.FINE)) {
                     logger.fine(String.format("verified %s with certificate %s (%s)", hostName, cert.getSubjectX500Principal().getName(), matchedName));
                  }

                  return true;
               }
            }

            try {
               if (port != -1 && this.exemptionStore != null) {
                  NHostExemption exemption = AccessController.doPrivileged(() -> this.exemptionStore.getExemption(hostName + ":" + port));
                  if (exemption != null && exemption.getApproved() && exemption.getCertificate().getCertificate().equals(cert)) {
                     if (logger.isLoggable(Level.FINE)) {
                        logger.fine(String.format("certificate %s verified as exempt as %s", cert.getSubjectX500Principal().getName(), hostName));
                     }

                     return true;
                  }

                  try {
                     String reverseDnsHost = InetAddress.getByName(host).getHostName().toLowerCase();
                     exemption = AccessController.doPrivileged(() -> this.exemptionStore.getExemption(reverseDnsHost + ":" + port));
                     if (exemption != null && exemption.getApproved() && exemption.isReverseDns() && exemption.getCertificate().getCertificate().equals(cert)) {
                        if (logger.isLoggable(Level.FINE)) {
                           logger.fine(
                              String.format(
                                 "certificate %s verified as exempt as %s using reverse DNS lookup", cert.getSubjectX500Principal().getName(), reverseDnsHost
                              )
                           );
                        }

                        return true;
                     }
                  } catch (UnknownHostException e) {
                     if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Error checking for reverse DNS exemption: " + e);
                     }
                  }
               }
            } catch (Exception e) {
               logger.log(Level.FINE, "Error verifying hostname", e);
            }

            if (logger.isLoggable(Level.FINE)) {
               logger.fine(String.format("unable to verify hostname %s with certificate %s", hostName, cert.getSubjectX500Principal().getName()));
            }

            return false;
         }
      } else {
         logger.fine("error verifying hostname: received null");
         return false;
      }
   }

   public static boolean verifyHostname(String actualHostnameRaw, String expectedHostnameRaw) {
      String expectedHostname = expectedHostnameRaw.toLowerCase(Locale.ENGLISH);
      String actualHostname = actualHostnameRaw.toLowerCase(Locale.ENGLISH);
      return expectedHostname.startsWith("*.")
            && expectedHostname.lastIndexOf(46) > 0
            && acceptableCountryWildcard(expectedHostname)
            && !IPAddressUtil.isIpv6Address(actualHostname)
            && !IPAddressUtil.isIpv4Address(actualHostname)
            && !IPAddressUtil.isIpv4MappedAddress(actualHostname)
         ? countDots(actualHostname) == countDots(expectedHostname) && actualHostname.endsWith(expectedHostname.substring(1))
         : actualHostname.equalsIgnoreCase(expectedHostname);
   }

   private static String[] getCertificateHosts(X509Certificate cert) {
      Array<String> addresses = new Array<>(String.class);

      try {
         String commonNameAddress = PemSource.extractCommonName(JcaX500NameUtil.getSubject(cert));
         addresses.add(commonNameAddress);
         logger.finest(() -> String.format("Found hostname <%s> in certificate Common Name", commonNameAddress));
      } catch (Exception e) {
         logger.log(Level.FINE, "error verifying hostname", e);
      }

      try {
         Collection<?> collection = cert.getSubjectAlternativeNames();
         if (collection != null) {
            for (Object listObject : collection) {
               List<?> list = (List<?>)listObject;
               int generalNameType = (Integer)list.get(0);
               switch (generalNameType) {
                  case 2:
                  case 6:
                  case 7:
                     try {
                        String sanName = list.get(1).toString();
                        addresses.add(sanName);
                        logger.finest(() -> String.format("Found hostname <%s> in certificate SAN", sanName));
                     } catch (Exception var8) {
                     }
               }
            }
         }
      } catch (Exception e) {
         logger.log(Level.FINE, "error verifying hostname", e);
      }

      return addresses.trim();
   }

   private static int countDots(String s) {
      int count = 0;

      for (int i = 0; i < s.length(); i++) {
         if (s.charAt(i) == '.') {
            count++;
         }
      }

      return count;
   }

   private static boolean acceptableCountryWildcard(String host) {
      int hostLen = host.length();
      if (hostLen >= 7 && hostLen <= 9 && host.charAt(hostLen - 3) == '.') {
         String s = host.substring(2, hostLen - 3);
         int x = Arrays.binarySearch(BAD_COUNTRY_2LDS, s);
         return x < 0;
      } else {
         return true;
      }
   }

   private static boolean isLocalHost(String host) {
      return LOCALHOST.contains(host.toLowerCase());
   }
}
