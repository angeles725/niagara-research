package com.tridium.niagarad.security;

import com.tridium.crypto.core.bundle.CryptographicAlgorithmBundle;
import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.nre.auth.Sha512Crypt;
import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.platform.PlatformUtil;
import com.tridium.nre.security.Aes256PasswordManager;
import com.tridium.nre.security.AesAlgorithmBundle;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Properties;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.nre.util.SecurityUtil;
import javax.baja.nre.util.TextUtil;
import javax.servlet.http.HttpServletRequest;

public class SimpleAuthenticationDomain extends AuthenticationDomain {
   private final Properties props;
   private final boolean passwordsHashed;

   SimpleAuthenticationDomain(Properties pProps, boolean pPasswordsHashed, IPlatformProvider platformProvider) {
      this.passwordsHashed = pPasswordsHashed;
      boolean propertiesModified = false;
      String user = pProps.getProperty("user");
      if (user == null || user.isEmpty()) {
         user = platformProvider.getDefaultUsername();
         pProps.setProperty("user", user);
         propertiesModified = true;
      }

      String clearTextPassword = null;
      String hashedPassword = null;
      if (pProps.containsKey("password")) {
         if (this.passwordsHashed) {
            hashedPassword = pProps.getProperty("password");
            if (!Sha512Crypt.verifyHashTextFormat(hashedPassword)) {
               clearTextPassword = hashedPassword;
               byte[] salt = new byte[16];
               new SecureRandom().nextBytes(salt);
               hashedPassword = Sha512Crypt.Sha512_crypt(clearTextPassword, ByteArrayUtil.toHexString(salt), 10000);
               pProps.setProperty("password", hashedPassword);
               propertiesModified = true;
            }
         } else {
            String encryptedPassword = pProps.getProperty("password");

            try {
               clearTextPassword = decryptPassword(encryptedPassword);
            } catch (Exception io) {
               clearTextPassword = encryptedPassword;

               try {
                  encryptedPassword = encryptPassword(clearTextPassword);
               } catch (Exception e) {
                  throw new RuntimeException("Failed to encrypt password", e);
               }

               pProps.setProperty("password", encryptedPassword);
               propertiesModified = true;
            }
         }
      } else {
         clearTextPassword = platformProvider.getDefaultPassword();
         if (clearTextPassword == null) {
            throw new RuntimeException("Invalid default password value");
         }

         if (this.passwordsHashed) {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            hashedPassword = Sha512Crypt.Sha512_crypt(clearTextPassword, ByteArrayUtil.toHexString(salt), 10000);
            pProps.setProperty("password", hashedPassword);
            propertiesModified = true;
         } else {
            String encryptedPassword;
            try {
               encryptedPassword = encryptPassword(clearTextPassword);
            } catch (Exception e) {
               throw new RuntimeException("Failed to encrypt password", e);
            }

            pProps.setProperty("password", encryptedPassword);
            propertiesModified = true;
         }
      }

      String factoryDefaultPassword = platformProvider.getDefaultPassword();
      String factoryDefaultUser = platformProvider.getDefaultUsername();
      boolean usingDefaults = false;
      if (this.passwordsHashed) {
         if (Sha512Crypt.verifyPassword(factoryDefaultPassword, hashedPassword) && user.equals(factoryDefaultUser)) {
            usingDefaults = true;
         }
      } else if (clearTextPassword.equals(factoryDefaultPassword) && user.equals(factoryDefaultUser)) {
         usingDefaults = true;
      }

      if (usingDefaults) {
         NiagaraDaemon.getFilter().warning("platform file authenticator is using factory default credentials");
      }

      if (propertiesModified && pProps == NiagaraDaemon.props) {
         NiagaraDaemon.saveProperties();
      }

      if (propertiesModified && PlatformUtil.isNativePlatform() && !platformProvider.synchronizeUsers(user, clearTextPassword)) {
         throw new RuntimeException("Failed to synchronize SimpleAuthenticationDomain native account(s)");
      }

      this.props = new Properties();
      this.props.putAll(pProps);
   }

   @Override
   public AuthenticationInfo makeAuthInfo(String providedUserName, String providedPasswordValue) {
      AuthenticationInfo result = super.makeAuthInfo(providedUserName, providedPasswordValue);
      if (result != null) {
         return result;
      }

      if (providedUserName == null || providedUserName.isEmpty()) {
         return null;
      }

      if (providedPasswordValue != null && !providedPasswordValue.isEmpty()) {
         boolean correctCredentialsProvided;
         if (this.passwordsHashed) {
            String propsPasswordHash = this.getPasswordHash(providedUserName);
            if (Sha512Crypt.verifyHashTextFormat(providedPasswordValue)) {
               correctCredentialsProvided = SecurityUtil.equals(providedPasswordValue, propsPasswordHash);
            } else {
               correctCredentialsProvided = propsPasswordHash != null && Sha512Crypt.verifyPassword(providedPasswordValue, propsPasswordHash);
            }
         } else {
            String propsPasswordClearText = this.getPasswordClearText(providedUserName);
            correctCredentialsProvided = SecurityUtil.equals(providedPasswordValue, propsPasswordClearText);
         }

         if (correctCredentialsProvided) {
            result = new SimpleAuthenticationInfo(providedUserName, true, false);
         }

         return result;
      } else {
         return null;
      }
   }

   @Override
   public String getRealm(HttpServletRequest client) {
      return "Niagara-Admin";
   }

   @Override
   public String getDomainType() {
      return "file";
   }

   @Override
   public boolean supportsPasswordClearTextRetrieval() {
      return !this.passwordsHashed;
   }

   @Override
   public String getPasswordClearText(String providedUserName) {
      if (this.passwordsHashed) {
         return null;
      }

      String result = super.getPasswordClearText(providedUserName);
      if (result == null) {
         String propsUserName = this.props.getProperty("user", null);
         if (SecurityUtil.equals(propsUserName, providedUserName)) {
            String encryptedPassword = this.props.getProperty("password", null);
            if (encryptedPassword != null) {
               try {
                  result = decryptPassword(encryptedPassword);
               } catch (Exception var6) {
               }
            }
         }
      }

      return result;
   }

   private static String decryptPassword(String encryptedPassword) throws Exception {
      CryptographicAlgorithmBundle bundle = CryptographicAlgorithmBundle.getInstanceFor(encryptedPassword);
      String hexCipher;
      String hexIv;
      if (bundle instanceof AesAlgorithmBundle) {
         String[] data = bundle.decode(encryptedPassword);
         hexCipher = data[((AesAlgorithmBundle)bundle).getCipherIndex()];
         hexIv = data[((AesAlgorithmBundle)bundle).getIvIndex()];
      } else {
         if (bundle != null) {
            throw new IOException("invalid algorithm bundle");
         }

         bundle = AesAlgorithmBundle.make(256, "1");
         String[] passwordFields = TextUtil.split(encryptedPassword, ':');
         hexCipher = passwordFields[0];
         hexIv = passwordFields[1];
      }

      return Aes256PasswordManager.getManager(NiagaraDaemon.getSecurityInfoProvider().getKeyRing())
         .decrypt(hexCipher, hexIv, ((AesAlgorithmBundle)bundle).getAesTransformation());
   }

   private static String encryptPassword(String clearTextPassword) throws Exception {
      Aes256PasswordManager manager = Aes256PasswordManager.getManager(NiagaraDaemon.getSecurityInfoProvider().getKeyRing());
      byte[] ivBytes = new byte[16];
      new SecureRandom().nextBytes(ivBytes);
      String iv = ByteArrayUtil.toHexString(ivBytes);
      String cipher = ByteArrayUtil.toHexString(manager.encrypt(clearTextPassword, iv));
      AesAlgorithmBundle bundle = AesAlgorithmBundle.make(256);
      return bundle.encode(new String[]{iv, cipher});
   }

   @Override
   public boolean supportsPasswordHashRetrieval() {
      return this.passwordsHashed;
   }

   @Override
   public String getPasswordHash(String providedUserName) {
      if (!this.passwordsHashed) {
         return null;
      }

      String result = super.getPasswordHash(providedUserName);
      if (result == null) {
         String propsUserName = this.props.getProperty("user", null);
         if (SecurityUtil.equals(propsUserName, providedUserName)) {
            result = this.props.getProperty("password", null);
         }
      }

      return result;
   }
}
