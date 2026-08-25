package com.tridium.crypto.core.exchange;

import com.tridium.nre.security.KeyDerivationAlgorithmBundle;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import org.bouncycastle.tls.crypto.SRP6Group;
import org.bouncycastle.tls.crypto.SRP6StandardGroups;
import org.bouncycastle.tls.crypto.TlsHash;
import org.bouncycastle.tls.crypto.impl.jcajce.JcaTlsHash;

public final class SRP6AlgorithmBundle extends KeyDerivationAlgorithmBundle {
   private static final String VERSION = "1";
   private final String groupSize;
   private final String digest;

   public static SRP6AlgorithmBundle make(String groupSize, String digest) {
      return new SRP6AlgorithmBundle(groupSize, digest);
   }

   public static SRP6AlgorithmBundle make(int groupSize, String digest) {
      return new SRP6AlgorithmBundle(String.valueOf(groupSize), digest);
   }

   private SRP6AlgorithmBundle(String groupSize, String digest) {
      this.groupSize = groupSize;
      this.digest = digest;
   }

   @Override
   public String getKeyDerivationAlgorithmName() {
      return "srp6";
   }

   @Override
   public int getKeyLength() {
      try {
         MessageDigest messageDigest = MessageDigest.getInstance(this.digest);
         return messageDigest.getDigestLength();
      } catch (Exception e) {
         return 0;
      }
   }

   @Override
   public String getAlgorithmType() {
      return this.getKeyDerivationAlgorithmName() + '-' + this.groupSize + '-' + this.digest;
   }

   @Override
   public String getAlgorithmVersion() {
      return "1";
   }

   @Override
   public int getDataElementCount() {
      return 0;
   }

   public TlsHash getTlsHash() {
      return getTlsHashFromString(this.digest);
   }

   public SRP6Group getParameters() {
      return getParamsFromGroupSize(this.groupSize);
   }

   private static SRP6Group getParamsFromGroupSize(String groupSize) {
      switch (groupSize) {
         case "1024":
            return SRP6StandardGroups.rfc5054_1024;
         case "1536":
            return SRP6StandardGroups.rfc5054_1536;
         case "2048":
            return SRP6StandardGroups.rfc5054_2048;
         case "3072":
            return SRP6StandardGroups.rfc5054_3072;
         case "4096":
            return SRP6StandardGroups.rfc5054_4096;
         case "6144":
            return SRP6StandardGroups.rfc5054_6144;
         case "8192":
            return SRP6StandardGroups.rfc5054_8192;
         default:
            throw new IllegalArgumentException(groupSize + " is not a valid RFC5054 group size");
      }
   }

   private static TlsHash getTlsHashFromString(String digest) {
      Objects.requireNonNull(digest, "SRP6 digest cannot be null");
      switch (digest) {
         case "sha256":
         case "sha512":
            try {
               return new JcaTlsHash(MessageDigest.getInstance(digest));
            } catch (NoSuchAlgorithmException e) {
               throw new IllegalArgumentException(digest + " is not a supported digest for SRP6");
            }
         default:
            throw new IllegalArgumentException(digest + " is not a supported digest for SRP6");
      }
   }
}
