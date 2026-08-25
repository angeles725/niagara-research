package com.tridium.crypto.core.cert;

import com.tridium.nre.security.SecurityInitializer;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.AccessController;
import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Hashtable;
import java.util.Objects;
import javax.baja.nre.util.ByteArrayUtil;
import javax.crypto.SecretKey;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

public class NKey extends PemSource {
   private Key key;
   private static Hashtable<ASN1ObjectIdentifier, String> keyAlgorithms = new Hashtable<>();

   public static NKey make(Key key) {
      return new NKey(key);
   }

   public static NKey make(String s) throws Exception {
      return new NKey(decodeFromString(s));
   }

   private NKey(Key key) {
      this.key = key;
   }

   public boolean isPrivate() {
      return this.key instanceof PrivateKey;
   }

   public boolean isPublic() {
      return this.key instanceof PublicKey;
   }

   public boolean isSecret() {
      return this.key instanceof SecretKey;
   }

   public String getAlgorithm() {
      return this.key.getAlgorithm();
   }

   public String encodeToString() throws Exception {
      return PemSource.getPEMString(this.key);
   }

   public static String encodeToString(Key key) throws Exception {
      return PemSource.getPEMString(key);
   }

   public static Key decodeFromString(String s) throws Exception {
      return AccessController.doPrivileged(
         () -> {
            Object obj = PemSource.getFromPEM(s);
            if (obj instanceof KeyPair) {
               return ((KeyPair)obj).getPrivate();
            } else if (obj instanceof PEMKeyPair) {
               return new JcaPEMKeyConverter()
                  .setProvider(SecurityInitializer.getInstance().getCryptoProvider().getProvider())
                  .getKeyPair((PEMKeyPair)obj)
                  .getPrivate();
            } else if (obj instanceof Key) {
               return (Key)obj;
            } else if (obj instanceof SubjectPublicKeyInfo) {
               return new JcaPEMKeyConverter()
                  .setProvider(SecurityInitializer.getInstance().getCryptoProvider().getProvider())
                  .getPublicKey((SubjectPublicKeyInfo)obj);
            } else if (obj instanceof PrivateKeyInfo) {
               return new JcaPEMKeyConverter()
                  .setProvider(SecurityInitializer.getInstance().getCryptoProvider().getProvider())
                  .getPrivateKey((PrivateKeyInfo)obj);
            } else {
               throw new IOException("Unable to decode key");
            }
         }
      );
   }

   public static PublicKey fromPrivateKey(PrivateKey key) throws Exception {
      String enc = PemSource.getPEMString(key);
      Object obj = PemSource.getFromPEM(enc);
      if (obj instanceof KeyPair) {
         return ((KeyPair)obj).getPublic();
      } else if (obj instanceof PEMKeyPair) {
         return new JcaPEMKeyConverter()
            .setProvider(SecurityInitializer.getInstance().getCryptoProvider().getProvider())
            .getPublicKey(((PEMKeyPair)obj).getPublicKeyInfo());
      } else {
         throw new IOException("Unable to extract public key");
      }
   }

   @Override
   public String toString() {
      return toString(this.key);
   }

   public static String toString(Key key) {
      if (key instanceof RSAPrivateCrtKey) {
         return toStringRSAPrivateCrtKey(key);
      } else if (key instanceof RSAPrivateKey) {
         return toStringRSAPrivateKey(key);
      } else if (key instanceof DSAPrivateKey) {
         return toStringDSAPrivateKey(key);
      } else if (key instanceof DSAPublicKey) {
         return toStringDSAPublicKey(key);
      } else {
         return key instanceof RSAPublicKey ? toStringRSAPublicKey(key) : key.toString();
      }
   }

   private static String toStringRSAPrivateCrtKey(Key key) {
      StringBuilder buf = new StringBuilder();
      RSAPrivateCrtKey pkey = (RSAPrivateCrtKey)key;
      byte[] bytes = pkey.getPrivateExponent().toByteArray();
      buf.append("RSAPrivateCrtKey: (").append(bytes.length * 8).append(" bit)\n");
      buf.append("   algorithm: " + pkey.getAlgorithm() + "\n");
      buf.append("   modulus:\n");
      StringWriter out = new StringWriter();
      PrintWriter pout = new PrintWriter(out);
      bytes = pkey.getModulus().toByteArray();
      ByteArrayUtil.hexDump("      ", pout, bytes, 0, bytes.length);
      pout.close();
      buf.append(out.getBuffer());
      buf.append("   publicExponent:\n");
      out = new StringWriter();
      pout = new PrintWriter(out);
      bytes = pkey.getPublicExponent().toByteArray();
      ByteArrayUtil.hexDump("     ", pout, bytes, 0, bytes.length);
      pout.close();
      buf.append(out.getBuffer());
      buf.append("   privateExponent:\n");
      out = new StringWriter();
      pout = new PrintWriter(out);
      bytes = pkey.getPrivateExponent().toByteArray();
      ByteArrayUtil.hexDump("     ", pout, bytes, 0, bytes.length);
      pout.close();
      buf.append(out.getBuffer());
      buf.append("   prime1:\n");
      out = new StringWriter();
      pout = new PrintWriter(out);
      bytes = pkey.getPrimeP().toByteArray();
      ByteArrayUtil.hexDump("     ", pout, bytes, 0, bytes.length);
      pout.close();
      buf.append(out.getBuffer());
      buf.append("   prime2:\n");
      out = new StringWriter();
      pout = new PrintWriter(out);
      bytes = pkey.getPrimeQ().toByteArray();
      ByteArrayUtil.hexDump("     ", pout, bytes, 0, bytes.length);
      pout.close();
      buf.append(out.getBuffer());
      buf.append("   exponent1:\n");
      out = new StringWriter();
      pout = new PrintWriter(out);
      bytes = pkey.getPrimeExponentP().toByteArray();
      ByteArrayUtil.hexDump("     ", pout, bytes, 0, bytes.length);
      pout.close();
      buf.append(out.getBuffer());
      buf.append("   exponent2:\n");
      out = new StringWriter();
      pout = new PrintWriter(out);
      bytes = pkey.getPrimeExponentQ().toByteArray();
      ByteArrayUtil.hexDump("     ", pout, bytes, 0, bytes.length);
      pout.close();
      buf.append(out.getBuffer());
      buf.append("   coefficient:\n");
      out = new StringWriter();
      pout = new PrintWriter(out);
      bytes = pkey.getCrtCoefficient().toByteArray();
      ByteArrayUtil.hexDump("     ", pout, bytes, 0, bytes.length);
      pout.close();
      buf.append(out.getBuffer());
      return buf.toString();
   }

   private static String toStringRSAPrivateKey(Key key) {
      StringBuilder buf = new StringBuilder();
      RSAPrivateKey pkey = (RSAPrivateKey)key;
      byte[] bytes = pkey.getPrivateExponent().toByteArray();
      buf.append("RSAPrivateKey: (").append(bytes.length * 8).append(" bit)\n");
      buf.append("   algorithm: " + pkey.getAlgorithm() + "\n");
      buf.append("   privateExponent:\n");
      StringWriter out = new StringWriter();
      PrintWriter pout = new PrintWriter(out);
      ByteArrayUtil.hexDump("     ", pout, bytes, 0, bytes.length);
      pout.close();
      buf.append(out.getBuffer());
      return buf.toString();
   }

   private static String toStringDSAPrivateKey(Key key) {
      StringBuilder buf = new StringBuilder();
      DSAPrivateKey pkey = (DSAPrivateKey)key;
      DSAPublicKey pubkey = null;

      try {
         pubkey = (DSAPublicKey)fromPrivateKey(pkey);
      } catch (Exception var9) {
      }

      int length = 0;
      if (pubkey != null) {
         length = pubkey.getY().toByteArray().length;
         buf.append("DSAPrivateKey: (").append(length * 8).append(" bit)\n");
         buf.append("   algorithm: " + pkey.getAlgorithm() + "\n");
         buf.append("   priv:\n");
         StringWriter out = new StringWriter();
         PrintWriter pout = new PrintWriter(out);
         byte[] bytes = pkey.getX().toByteArray();
         ByteArrayUtil.hexDump("     ", pout, bytes, 0, bytes.length);
         pout.close();
         buf.append(out.getBuffer());
         buf.append("   pub:\n");
         out = new StringWriter();
         pout = new PrintWriter(out);
         bytes = pubkey.getY().toByteArray();
         ByteArrayUtil.hexDump("     ", pout, bytes, 0, bytes.length);
         pout.close();
         buf.append(out.getBuffer());
         DSAParams params = pkey.getParams();
         buf.append("   P:\n");
         out = new StringWriter();
         pout = new PrintWriter(out);
         bytes = params.getP().toByteArray();
         ByteArrayUtil.hexDump("     ", pout, bytes, 0, bytes.length);
         pout.close();
         buf.append(out.getBuffer());
         buf.append("   Q:\n");
         out = new StringWriter();
         pout = new PrintWriter(out);
         bytes = params.getQ().toByteArray();
         ByteArrayUtil.hexDump("     ", pout, bytes, 0, bytes.length);
         pout.close();
         buf.append(out.getBuffer());
         buf.append("   G:\n");
         out = new StringWriter();
         pout = new PrintWriter(out);
         bytes = params.getG().toByteArray();
         ByteArrayUtil.hexDump("     ", pout, bytes, 0, bytes.length);
         pout.close();
         buf.append(out.getBuffer());
         return buf.toString();
      } else {
         return null;
      }
   }

   private static String toStringDSAPublicKey(Key key) {
      StringBuilder buf = new StringBuilder();
      DSAPublicKey pkey = (DSAPublicKey)key;
      int length = pkey.getY().toByteArray().length;
      buf.append("DSAPublicKey: (").append(length * 8).append(" bit)\n");
      buf.append("   algorithm: " + pkey.getAlgorithm() + "\n");
      buf.append("   pub:\n");
      StringWriter out = new StringWriter();
      PrintWriter pout = new PrintWriter(out);
      byte[] bytes = pkey.getY().toByteArray();
      ByteArrayUtil.hexDump("     ", pout, bytes, 0, bytes.length);
      pout.close();
      buf.append(out.getBuffer());
      return buf.toString();
   }

   private static String toStringRSAPublicKey(Key key) {
      StringBuilder buf = new StringBuilder();
      RSAPublicKey pkey = (RSAPublicKey)key;
      int length = pkey.getPublicExponent().toByteArray().length;
      buf.append("RSAPublicKey: (").append(length * 8).append(" bit)\n");
      buf.append("   algorithm: " + pkey.getAlgorithm() + "\n");
      buf.append("   pub:\n");
      StringWriter out = new StringWriter();
      PrintWriter pout = new PrintWriter(out);
      byte[] bytes = pkey.getPublicExponent().toByteArray();
      ByteArrayUtil.hexDump("     ", pout, bytes, 0, bytes.length);
      pout.close();
      buf.append(out.getBuffer());
      return buf.toString();
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }

      if (obj == null) {
         return false;
      }

      if (!(obj instanceof NKey)) {
         return false;
      }

      NKey other = (NKey)obj;
      if (this.key == null) {
         if (other.key != null) {
            return false;
         }
      } else if (!this.key.equals(other.key)) {
         return false;
      }

      return true;
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(this.key);
   }

   static {
      keyAlgorithms.put(PKCSObjectIdentifiers.rsaEncryption, "RSA");
      keyAlgorithms.put(X9ObjectIdentifiers.id_dsa, "DSA");
   }
}
