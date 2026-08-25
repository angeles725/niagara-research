package com.tridium.niagarad.license;

import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.platform.PlatformUtil;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.AccessController;
import java.util.Locale;
import javax.baja.xml.XElem;
import javax.baja.xml.XException;
import javax.baja.xml.XParser;

public class LicenseFile {
   public String path;
   public String error;
   public String hostId;
   public String vendor;
   public long generated;
   public long expiration;
   private static final long MILLIS_IN_36_HOURS = 129600000L;

   public LicenseFile(String path) {
      this.path = path;
      this.error = null;
      this.hostId = null;
      this.vendor = null;
      this.generated = -1L;
      this.expiration = -1L;
   }

   public void load(LicenseManager licMan, InputStream in) throws Exception {
      String localHostId = LicenseFile.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE.getHostId();
      if (localHostId == null) {
         this.error = "This machine has no host ID";
      } else {
         try (InputStream licenseInputStream = in != null ? in : new BufferedInputStream(new FileInputStream(this.path))) {
            XElem root = XParser.make(licenseInputStream).parse(false);
            if (root == null) {
               this.error = "Could not parse license file as an xml document";
            } else if (!root.qname().equalsIgnoreCase("license")) {
               this.error = "'license' was not root element in license file";
            } else {
               this.vendor = root.get("vendor", null);
               if (this.vendor == null) {
                  this.error = "Missing license vendor";
               } else {
                  this.hostId = root.get("hostId");
                  if (!localHostId.equalsIgnoreCase(this.hostId)) {
                     this.error = "HostId does not match";
                  } else {
                     long now = System.currentTimeMillis();
                     long longAgo = LicenseUtil.parseDate("2015-01-01", true);
                     String generatedString = root.get("generated", null);
                     if (generatedString == null) {
                        this.error = "Missing license generated date";
                     } else {
                        this.generated = LicenseUtil.parseDate(generatedString, true);
                        if (this.generated < 0L) {
                           this.error = "Invalid license generated date";
                        } else {
                           if ("tridium".equalsIgnoreCase(this.vendor)) {
                              licMan.tridiumGeneratedDate = this.generated;
                           }

                           if (now < longAgo) {
                              this.error = "Current system time appears invalid, date before 2015-01-01";
                           } else if (now < this.generated - 129600000L) {
                              this.error = "Current date is earlier than license generated date";
                           } else {
                              String expirationString = root.get("expiration", null);
                              if (expirationString == null) {
                                 this.error = "Missing license expiration date";
                              } else {
                                 this.expiration = LicenseUtil.parseDate(expirationString, false);
                                 if (this.expiration < 0L) {
                                    this.error = "Invalid license expiration date";
                                 } else if (now > this.expiration) {
                                    this.error = "License file is expired";
                                 } else {
                                    XElem[] featureElems = root.elems("feature");

                                    for (XElem featureElem : featureElems) {
                                       this.loadFeature(licMan, featureElem);
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         } catch (XException e) {
            this.error = "Invalid XML: " + e.getMessage();
         } catch (Throwable e) {
            this.error = e.toString();
         }
      }
   }

   public boolean isValid() {
      return this.error == null;
   }

   private void loadFeature(LicenseManager licMan, XElem elem) {
      String name = elem.get("name");
      if (LicenseManager.FEATURE_WHITELIST.contains(name.toLowerCase(Locale.ENGLISH))) {
         long featureExp = Long.MAX_VALUE;
         String s = elem.get("expiration", null);
         if (s != null) {
            featureExp = LicenseUtil.parseDate(s, false);
         }

         if (this.expiration < featureExp) {
            featureExp = this.expiration;
         }

         Feature feature = new Feature(this.vendor, name, featureExp);

         for (int i = 0; i < elem.attrSize(); i++) {
            String attrName = elem.attrName(i);
            String attrValue = elem.attrValue(i);
            if (!attrName.equalsIgnoreCase("name") && !attrName.equalsIgnoreCase("expiration")) {
               feature.props.setProperty(attrName, attrValue);
            }
         }

         licMan.addFeature(feature);
      }
   }

   private static final class PlatformProviderHolder {
      private static final IPlatformProvider PLATFORM_PROVIDER_INSTANCE = AccessController.doPrivileged(PlatformUtil::getPlatformProvider);
   }
}
