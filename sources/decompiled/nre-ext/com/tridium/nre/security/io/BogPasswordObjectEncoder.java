package com.tridium.nre.security.io;

import com.tridium.nre.security.AbstractAesAlgorithmBundle;
import com.tridium.nre.security.Aes256PasswordManager;
import com.tridium.nre.security.AesAlgorithmBundle;
import com.tridium.nre.security.AliasedAesAlgorithmBundle;
import com.tridium.nre.security.EncryptionKeySource;
import com.tridium.nre.security.ISecretBytesSupplier;
import com.tridium.nre.security.KeyRing;
import com.tridium.nre.security.NiagaraBasicPermission;
import com.tridium.nre.security.PBEEncodingInfo;
import com.tridium.nre.security.PBEEncodingKey;
import com.tridium.nre.security.SecretBytes;
import com.tridium.nre.security.SecretChars;
import com.tridium.nre.util.IElement;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Objects;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.xml.XElem;
import javax.baja.xml.XWriter;

public class BogPasswordObjectEncoder implements AutoCloseable {
   private final ISecretBytesSupplier keySupplier;
   private final PBEEncodingInfo pbeEncodingInfo;
   private final EncryptionKeySource keySource;
   private final KeyRing keyRing;
   public static final NiagaraBasicPermission GET_BOG_KEY_PERMISSION = new NiagaraBasicPermission("GET_BOG_KEY");

   private BogPasswordObjectEncoder(SecretBytes secretBytes) throws Exception {
      Objects.requireNonNull(secretBytes);
      this.keySupplier = ISecretBytesSupplier.wrap(secretBytes.newCopy());
      this.pbeEncodingInfo = PBEEncodingInfo.NULL;
      this.keySource = EncryptionKeySource.shared;
      this.keyRing = null;
   }

   private BogPasswordObjectEncoder(SecretChars passPhrase) throws Exception {
      Objects.requireNonNull(passPhrase);
      PBEEncodingKey key = new PBEEncodingKey(passPhrase);
      this.keySupplier = key;
      this.pbeEncodingInfo = key;
      this.keySource = EncryptionKeySource.external;
      this.keyRing = null;
   }

   private BogPasswordObjectEncoder(String encodedValidator, String encodingSaltHex, int encodingIterationCount) throws IOException {
      Objects.requireNonNull(encodedValidator);
      Objects.requireNonNull(encodingSaltHex);
      this.pbeEncodingInfo = new PBEEncodingInfo(encodedValidator, encodingSaltHex, encodingIterationCount);
      this.keySupplier = null;
      this.keySource = EncryptionKeySource.external;
      this.keyRing = null;
   }

   private BogPasswordObjectEncoder(PBEEncodingKey pbeKey) throws IOException {
      Objects.requireNonNull(pbeKey);
      this.keySupplier = pbeKey.newCopy();
      this.pbeEncodingInfo = (PBEEncodingInfo)this.keySupplier;
      this.keySource = EncryptionKeySource.external;
      this.keyRing = null;
   }

   private BogPasswordObjectEncoder(EncryptionKeySource keySource) {
      Objects.requireNonNull(keySource);
      this.keySource = keySource;
      this.keySupplier = null;
      this.pbeEncodingInfo = PBEEncodingInfo.NULL;
      this.keyRing = null;
   }

   private BogPasswordObjectEncoder(KeyRing keyRing) {
      Objects.requireNonNull(keyRing);
      this.keySource = EncryptionKeySource.keyring;
      this.keySupplier = null;
      this.pbeEncodingInfo = PBEEncodingInfo.NULL;
      this.keyRing = keyRing;
   }

   public static BogPasswordObjectEncoder makeNone() {
      return new BogPasswordObjectEncoder(EncryptionKeySource.none);
   }

   public static BogPasswordObjectEncoder makeKeyring() {
      return new BogPasswordObjectEncoder(EncryptionKeySource.keyring);
   }

   public static BogPasswordObjectEncoder makeKeyring(KeyRing keyRing) {
      return new BogPasswordObjectEncoder(keyRing);
   }

   public static BogPasswordObjectEncoder make(ISecretBytesSupplier secretBytesSupplier) throws Exception {
      if (secretBytesSupplier == null) {
         return makeNone();
      } else {
         return secretBytesSupplier instanceof PBEEncodingKey ? makeExternal((PBEEncodingKey)secretBytesSupplier) : makeShared(secretBytesSupplier.get());
      }
   }

   public static BogPasswordObjectEncoder makeShared(SecretBytes keyBytes) throws Exception {
      return new BogPasswordObjectEncoder(keyBytes);
   }

   public static BogPasswordObjectEncoder makeExternal(SecretChars passPhraseChars) throws Exception {
      return passPhraseChars == null ? makeNone() : new BogPasswordObjectEncoder(passPhraseChars);
   }

   public static BogPasswordObjectEncoder makeExternal(PBEEncodingKey encodingKey) throws Exception {
      return encodingKey == null ? makeNone() : new BogPasswordObjectEncoder(encodingKey);
   }

   public static BogPasswordObjectEncoder parseBogHeader(IElement bogHeaderElement, EncryptionKeySource requiredKeySource) throws IOException {
      Objects.requireNonNull(bogHeaderElement);
      Objects.requireNonNull(requiredKeySource);
      String encodedValidator = bogHeaderElement.get("reversibleEncodingValidator", null);
      String encodingSaltHex = bogHeaderElement.get("reversibleEncodingSalt", null);
      int encodingIterationCount = bogHeaderElement.geti("reversibleEncodingIterationCount", -1);

      try {
         boolean fipsMode = Boolean.parseBoolean(bogHeaderElement.get("FIPSEnabled"));
      } catch (Exception e) {
         boolean fipsMode = false;
      }

      String headerKeySourceString = bogHeaderElement.get("reversibleEncodingKeySource", null);
      if (requiredKeySource.equals(EncryptionKeySource.shared)) {
         throw new IllegalArgumentException("Cannot require shared key source");
      }

      if (!EncryptionKeySource.undefined.name().equals(headerKeySourceString) && !EncryptionKeySource.shared.name().equals(headerKeySourceString)) {
         EncryptionKeySource headerKeySource;
         if (headerKeySourceString == null) {
            if (requiredKeySource.equals(EncryptionKeySource.undefined)) {
               if (encodedValidator == null) {
                  headerKeySource = EncryptionKeySource.none;
               } else {
                  headerKeySource = encodedValidator.startsWith("[null.") ? EncryptionKeySource.none : EncryptionKeySource.external;
               }
            } else if (requiredKeySource.equals(EncryptionKeySource.keyring)) {
               if (encodedValidator != null) {
                  throw new IOException("Unexpected key source");
               }

               headerKeySource = EncryptionKeySource.keyring;
            } else if (requiredKeySource.equals(EncryptionKeySource.none)) {
               if (encodedValidator == null) {
                  encodedValidator = "[null.1]=";
               }

               if (!encodedValidator.startsWith("[null.")) {
                  throw new IOException("Unexpected key source");
               }

               headerKeySource = EncryptionKeySource.none;
            } else {
               if (encodedValidator == null) {
                  encodedValidator = "[null.1]=";
               }

               headerKeySource = encodedValidator.startsWith("[null.") ? EncryptionKeySource.none : EncryptionKeySource.external;
            }
         } else {
            headerKeySource = EncryptionKeySource.valueOf(headerKeySourceString);
            if (!requiredKeySource.equals(EncryptionKeySource.undefined)) {
               if (requiredKeySource.equals(EncryptionKeySource.external)) {
                  if (headerKeySource.equals(EncryptionKeySource.none)) {
                     if (encodedValidator != null && !encodedValidator.startsWith("[none.")) {
                        throw new IOException("Unexpected validator");
                     }
                  } else {
                     if (!headerKeySource.equals(EncryptionKeySource.external)) {
                        throw new IOException("Unexpected key source");
                     }

                     if (encodedValidator == null || encodingSaltHex == null || encodingIterationCount < 0) {
                        throw new IOException("Required fields missing");
                     }

                     if (encodedValidator.startsWith("[null.")) {
                        throw new IOException("Unexpected validator");
                     }
                  }
               } else if (requiredKeySource.equals(EncryptionKeySource.none)) {
                  if (!headerKeySource.equals(EncryptionKeySource.none)) {
                     throw new IOException("Unexpected key source");
                  }

                  if (encodedValidator != null && !encodedValidator.startsWith("[none.")) {
                     throw new IOException("Unexpected validator");
                  }
               } else {
                  if (!headerKeySource.equals(EncryptionKeySource.keyring)) {
                     throw new IOException("Unexpected key source");
                  }

                  if (encodedValidator != null || encodingSaltHex != null || encodingIterationCount >= 0) {
                     throw new IOException("Unexpected validator");
                  }
               }
            }
         }

         return headerKeySource.equals(EncryptionKeySource.external)
            ? new BogPasswordObjectEncoder(encodedValidator, encodingSaltHex, encodingIterationCount)
            : new BogPasswordObjectEncoder(headerKeySource);
      } else {
         throw new IOException("BOG document has invalid key source");
      }
   }

   public void populateBogHeaderElement(XElem headerElement) {
      Objects.requireNonNull(headerElement);
      headerElement.removeAttr("reversibleEncodingKeySource");
      headerElement.removeAttr("reversibleEncodingValidator");
      headerElement.removeAttr("reversibleEncodingSalt");
      headerElement.removeAttr("reversibleEncodingIterationCount");
      headerElement.removeAttr("FIPSEnabled");
      headerElement.addAttr("reversibleEncodingKeySource", this.keySource.name());
      headerElement.addAttr("FIPSEnabled", "false");
      if (!this.keySource.equals(EncryptionKeySource.keyring)) {
         headerElement.addAttr("reversibleEncodingValidator", this.getEncodedValidator());
         if (this.keySource.equals(EncryptionKeySource.external)) {
            headerElement.addAttr("reversibleEncodingSalt", this.pbeEncodingInfo.getEncodingSaltHex());
            headerElement.addAttr("reversibleEncodingIterationCount", String.valueOf(this.pbeEncodingInfo.getEncodingIterationCount()));
         }
      }
   }

   public void writeBogHeader(XWriter w, String bogVersion) {
      w.w("<bajaObjectGraph version=\"" + bogVersion + "\" reversibleEncodingKeySource=\"" + this.keySource.name() + "\" ");
      boolean futureFIPSRelatedCheckbox = false;
      w.w("FIPSEnabled=\"" + (futureFIPSRelatedCheckbox ? "true" : "false") + "\" ");
      if (this.keySource.equals(EncryptionKeySource.none) || this.keySource.equals(EncryptionKeySource.external)) {
         w.w("reversibleEncodingValidator=\"" + this.getEncodedValidator() + "\" ");
      }

      if (this.keySource.equals(EncryptionKeySource.external)) {
         w.w(
            "reversibleEncodingSalt=\""
               + this.pbeEncodingInfo.getEncodingSaltHex()
               + "\" reversibleEncodingIterationCount=\""
               + this.pbeEncodingInfo.getEncodingIterationCount()
               + "\" "
         );
      }

      w.w(">\n");
   }

   public String encodePassword(SecretBytes passwordBytes) throws Exception {
      return this.encodePassword(passwordBytes, AesAlgorithmBundle.getInstance(), null);
   }

   public String encodePassword(SecretBytes passwordBytes, AbstractAesAlgorithmBundle algorithmBundle, String alias) throws Exception {
      byte[] ivBytes = new byte[16];
      new SecureRandom().nextBytes(ivBytes);
      String ivHex = ByteArrayUtil.toHexString(ivBytes);
      byte[] cipher;
      if (this.keySource.equals(EncryptionKeySource.keyring)) {
         if (this.keyRing == null) {
            throw new IOException("Missing encryption key");
         }

         Aes256PasswordManager manager = alias != null ? Aes256PasswordManager.getManager(this.keyRing, alias) : Aes256PasswordManager.getManager(this.keyRing);
         cipher = manager.encrypt(passwordBytes.get(), ivBytes, algorithmBundle.getAesTransformation());
      } else {
         if (this.keySupplier == null) {
            throw new IOException("Missing encryption key");
         }

         cipher = Aes256PasswordManager.encrypt(passwordBytes.get(), ivBytes, this.keySupplier.get().get(), algorithmBundle.getAesTransformation());
      }

      if (cipher == null) {
         throw new IOException("Could not encrypt password");
      }

      String[] data = new String[algorithmBundle.getDataElementCount()];
      data[algorithmBundle.getIvIndex()] = ivHex;
      data[algorithmBundle.getCipherIndex()] = ByteArrayUtil.toHexString(cipher);
      if (algorithmBundle instanceof AliasedAesAlgorithmBundle) {
         data[0] = alias;
      }

      return algorithmBundle.encode(data);
   }

   public ISecretBytesSupplier passPhraseToKey(SecretChars passPhrase) throws IOException {
      try {
         if (this.keySource.equals(EncryptionKeySource.none)) {
            throw new IOException("Bog document does not allow passwords that use reversible encryption algorithm");
         } else if (this.keySource.equals(EncryptionKeySource.shared)) {
            return this.keySupplier.newCopy();
         } else if (this.keySource.equals(EncryptionKeySource.keyring)) {
            throw new IOException("Bog document does not use external pass phrase");
         } else {
            return this.pbeEncodingInfo.makePBEKey(passPhrase);
         }
      } catch (IOException | SecurityException rethrow) {
         throw rethrow;
      } catch (Exception e) {
         throw new IOException(e);
      }
   }

   public PBEEncodingInfo getPbeEncodingInfo() {
      return this.pbeEncodingInfo;
   }

   public String getEncodedValidator() {
      return this.pbeEncodingInfo.getEncodedValidator();
   }

   public long getEncodingIterationCount() {
      return this.pbeEncodingInfo.getEncodingIterationCount();
   }

   public String getEncodingSaltHex() {
      return this.pbeEncodingInfo.getEncodingSaltHex();
   }

   public EncryptionKeySource getKeySource() {
      return this.keySource;
   }

   public ISecretBytesSupplier getPassPhraseEncodingKey() {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         sm.checkPermission(GET_BOG_KEY_PERMISSION);
      }

      return this.keySupplier;
   }

   @Override
   public void close() {
      if (this.keySupplier != null) {
         this.keySupplier.close();
      }
   }
}
