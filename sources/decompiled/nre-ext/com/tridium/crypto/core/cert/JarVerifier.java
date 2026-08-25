package com.tridium.crypto.core.cert;

import com.tridium.nre.security.SecurityInitializer;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.CodeSigner;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Timestamp;
import java.security.cert.CertPath;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSSignedDataParser;
import org.bouncycastle.cms.CMSTypedStream;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.util.Store;

public class JarVerifier {
   private final File file;
   private final Map<String, MessageDigest> createdDigests = new HashMap<>();
   private static final int BUFFER_SIZE = 32768;
   private static final int VERIFIED_ENTRIES_BUFFER_SIZE = 5000;

   public JarVerifier(File file) {
      this.file = file;
   }

   public List<CodeSigner> verify() throws IOException {
      try (ZipFile zip = new ZipFile(this.file)) {
         Provider provider = SecurityInitializer.getInstance().getCryptoProvider().getProvider();
         DigestCalculatorProvider digestCalculatorProvider = new JcaDigestCalculatorProviderBuilder().setProvider(provider).build();
         JcaSimpleSignerInfoVerifierBuilder signerInfoVerifierBuilder = new JcaSimpleSignerInfoVerifierBuilder().setProvider(provider);
         JcaX509CertificateConverter certificateConverter = new JcaX509CertificateConverter();
         CertificateFactory certFactory = CertificateFactory.getInstance("X509");
         Map<String, CodeSigner> codeSignerMap = new HashMap<>();
         Map<String, String> sfEntries = new HashMap<>();
         Map<String, String> blockEntries = new HashMap<>();
         Map<String, String> expectedManifestDigests = new HashMap<>();
         Map<String, MessageDigest> workingManifestDigests = new HashMap<>();
         Enumeration<? extends ZipEntry> entries = zip.entries();
         ZipEntry zipEntry = entries.nextElement();
         if (zipEntry != null && "META-INF/".equalsIgnoreCase(zipEntry.getName().toLowerCase(Locale.ENGLISH))) {
            zipEntry = entries.nextElement();
         }

         if (zipEntry != null && zipEntry.getName().toLowerCase(Locale.ENGLISH).equalsIgnoreCase("META-INF/MANIFEST.MF")) {
            while (entries.hasMoreElements()) {
               zipEntry = entries.nextElement();
               String name = zipEntry.getName();
               String upperName = name.toUpperCase(Locale.ENGLISH);
               if (!upperName.startsWith("META-INF/") && !upperName.startsWith("/META-INF/")) {
                  break;
               }

               if (upperName.endsWith(".SF")) {
                  String key = upperName.substring(0, upperName.lastIndexOf(46));
                  sfEntries.put(key, name);
               } else if (!upperName.endsWith(".DSA") && !upperName.endsWith(".RSA") && !upperName.endsWith(".EC")) {
                  if (zipEntry.isDirectory()) {
                     continue;
                  }
                  break;
               } else {
                  String key = upperName.substring(0, upperName.lastIndexOf(46));
                  blockEntries.put(key, name);
               }
            }

            for (Entry<String, String> entry : sfEntries.entrySet()) {
               String sfEntryName = entry.getValue();
               String blockEntryName = blockEntries.get(entry.getKey());
               if (blockEntryName == null) {
                  throw new SecurityException("Missing block file for " + sfEntryName);
               }

               try (
                  InputStream blockIn = zip.getInputStream(zip.getEntry(blockEntryName));
                  InputStream sfIn = zip.getInputStream(zip.getEntry(sfEntryName));
               ) {
                  CMSSignedDataParser parser = new CMSSignedDataParser(digestCalculatorProvider, new CMSTypedStream(sfIn), blockIn);
                  JarVerifier.ManifestParser manifestParser = new JarVerifier.ManifestParser(parser.getSignedContent().getContentStream());
                  Map<String, String> mainAttrs = manifestParser.nextSection();

                  for (Entry<String, String> manifestEntry : mainAttrs.entrySet()) {
                     String name = manifestEntry.getKey();
                     if (name.endsWith("-DIGEST-MANIFEST")) {
                        expectedManifestDigests.put(entry.getKey(), name.substring(0, name.length() - 16) + ':' + manifestEntry.getValue());
                     }
                  }

                  parser.getSignedContent().drain();
                  Store<?> certStore = parser.getCertificates();
                  SignerInformationStore signerStore = parser.getSignerInfos();

                  for (SignerInformation signer : signerStore.getSigners()) {
                     X509CertificateHolder signerCert = (X509CertificateHolder)certStore.getMatches(signer.getSID()).iterator().next();
                     if (!signer.verify(signerInfoVerifierBuilder.build(signerCert))) {
                        throw new SecurityException("cannot verify signature block file " + entry.getKey());
                     }

                     Collection<?> certCollection = certStore.getMatches(null);
                     List<X509Certificate> certList = new ArrayList<>();

                     for (Object cert : certCollection) {
                        certList.add(certificateConverter.getCertificate((X509CertificateHolder)cert));
                     }

                     Timestamp timestamp = null;
                     AttributeTable unsignedAttrTable = signer.getUnsignedAttributes();
                     if (unsignedAttrTable != null) {
                        Attribute token = unsignedAttrTable.get(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken);
                        TimeStampToken tsToken = new TimeStampToken(
                           ContentInfo.getInstance(token.getAttrValues().getObjectAt(0).toASN1Primitive().getEncoded())
                        );
                        Date signingDate = tsToken.getTimeStampInfo().getGenTime();
                        Store<?> tscertStore = tsToken.getCertificates();
                        X509CertificateHolder tsSignerCert = (X509CertificateHolder)tscertStore.getMatches(tsToken.getSID()).iterator().next();

                        try {
                           JcaSimpleSignerInfoVerifierBuilder sigInfVerBuilder = new JcaSimpleSignerInfoVerifierBuilder();
                           SignerInformationVerifier verifier = sigInfVerBuilder.build(tsSignerCert);
                           tsToken.validate(verifier);
                        } catch (Exception e) {
                           throw new SecurityException("Failed to validate timestamp signature for block file " + entry.getKey(), e);
                        }

                        Collection<?> tsCerts = tscertStore.getMatches(null);
                        List<X509Certificate> tsCertList = new ArrayList<>();

                        for (Object cert : tsCerts) {
                           tsCertList.add(certificateConverter.getCertificate((X509CertificateHolder)cert));
                        }

                        CertPath tsPath = certFactory.generateCertPath(tsCertList);
                        timestamp = new Timestamp(signingDate, tsPath);
                     }

                     CertPath path = certFactory.generateCertPath(certList);
                     codeSignerMap.put(entry.getKey(), new CodeSigner(path, timestamp));
                  }
               }
            }

            if (codeSignerMap.isEmpty()) {
               return new ArrayList<>();
            }

            try (InputStream manifestInputStream = zip.getInputStream(zip.getEntry("META-INF/MANIFEST.MF"))) {
               InputStream digestInputStream = manifestInputStream;

               for (Entry<String, String> entry : expectedManifestDigests.entrySet()) {
                  String value = entry.getValue();
                  String digestAlg = value.substring(0, value.indexOf(58));
                  if (!workingManifestDigests.containsKey(digestAlg)) {
                     MessageDigest digest = MessageDigest.getInstance(digestAlg);
                     workingManifestDigests.put(digestAlg, digest);
                     digestInputStream = new DigestInputStream(digestInputStream, digest);
                  }
               }

               if (!this.processManifest(digestInputStream, zip)) {
                  return new ArrayList<>();
               }
            }

            List<CodeSigner> codeSigners = new ArrayList<>();
            Map<String, String> calculatedDigests = new HashMap<>();

            for (Entry<String, MessageDigest> entry : workingManifestDigests.entrySet()) {
               calculatedDigests.put(entry.getKey(), Base64.getEncoder().encodeToString(entry.getValue().digest()));
            }

            for (Entry<String, CodeSigner> entry : codeSignerMap.entrySet()) {
               String key = entry.getKey();
               String expectedDigest = expectedManifestDigests.get(key);
               if (expectedDigest != null) {
                  int colIndex = expectedDigest.indexOf(58);
                  String digestAlg = expectedDigest.substring(0, colIndex);
                  String digestValue = expectedDigest.substring(colIndex + 1);
                  String calculatedDigestValue = calculatedDigests.get(digestAlg);
                  if (digestValue.equals(calculatedDigestValue)) {
                     codeSigners.add(entry.getValue());
                     continue;
                  }
               }

               if (this.processSigFile(zip.getEntry(sfEntries.get(entry.getKey())), zip)) {
                  codeSigners.add(entry.getValue());
               }
            }

            return codeSigners;
         } else {
            return new ArrayList<>();
         }
      } catch (Exception e) {
         if (e instanceof SecurityException) {
            throw (SecurityException)e;
         } else if (e instanceof IOException) {
            throw (IOException)e;
         } else {
            throw new SecurityException(e.getMessage(), e);
         }
      }
   }

   private boolean processManifest(InputStream in, ZipFile zip) throws IOException, NoSuchAlgorithmException {
      Enumeration<? extends ZipEntry> entries = zip.entries();

      int entryCount;
      for (entryCount = 0; entries.hasMoreElements(); entryCount++) {
         entries.nextElement();
      }

      boolean[] verifiedEntriesArray = new boolean[entryCount];
      Set<String> verifiedEntriesBuffer = new HashSet<>();
      JarVerifier.ManifestParser manifestParser = new JarVerifier.ManifestParser(in);
      manifestParser.nextSection();
      byte[] buffer = new byte[32768];

      Map<String, String> section;
      while ((section = manifestParser.nextSection()) != null) {
         String digestAlg = null;
         String digest = null;
         String name = section.get("NAME");
         if (name == null) {
            throw new IOException("invalid manifest format");
         }

         for (Entry<String, String> entry : section.entrySet()) {
            String key = entry.getKey();
            if (key.endsWith("-DIGEST")) {
               digestAlg = key.substring(0, key.length() - 7);
               digest = entry.getValue();
            }
         }

         if (digestAlg != null) {
            ZipEntry entry = zip.getEntry(name);
            if (entry != null) {
               try (InputStream entryIn = zip.getInputStream(entry)) {
                  MessageDigest calculatedDigest = this.getDigest(digestAlg);

                  int len;
                  while ((len = entryIn.read(buffer)) != -1) {
                     calculatedDigest.update(buffer, 0, len);
                  }

                  String calculatedDigestString = Base64.getEncoder().encodeToString(calculatedDigest.digest());
                  if (!calculatedDigestString.equals(digest)) {
                     throw new SecurityException(digestAlg + " digest error for " + name);
                  }
               }

               verifiedEntriesBuffer.add(name);
               if (verifiedEntriesBuffer.size() >= 5000) {
                  entries = zip.entries();

                  for (int i = 0; entries.hasMoreElements(); i++) {
                     if (verifiedEntriesBuffer.contains(entries.nextElement().getName())) {
                        verifiedEntriesArray[i] = true;
                     }
                  }

                  verifiedEntriesBuffer = new HashSet<>();
               }
            }
         }
      }

      entries = zip.entries();

      for (int i = 0; entries.hasMoreElements(); i++) {
         ZipEntry entry = entries.nextElement();
         String name = entry.getName();
         if (!verifiedEntriesArray[i] && !verifiedEntriesBuffer.contains(name)) {
            String upperName = name.toUpperCase(Locale.ENGLISH);
            if (!entry.isDirectory()
               && !upperName.equals("META-INF/MANIFEST.MF")
               && (!upperName.startsWith("META-INF/") || !upperName.endsWith(".SF"))
               && !upperName.endsWith(".RSA")
               && !upperName.endsWith(".DSA")
               && !upperName.endsWith(".EC")) {
               return false;
            }
         }
      }

      return true;
   }

   private boolean processSigFile(ZipEntry sigEntry, ZipFile zip) throws IOException, NoSuchAlgorithmException {
      String expectedMainAttrsDigest = null;
      MessageDigest calculatedMainAttrsDigest = null;
      InputStream sigInputStream = zip.getInputStream(sigEntry);

      try {
         JarVerifier.ManifestParser sigParser = new JarVerifier.ManifestParser(sigInputStream);
         Map<String, String> mainAttrs = sigParser.nextSection();

         for (Entry<String, String> manifestEntry : mainAttrs.entrySet()) {
            String name = manifestEntry.getKey();
            if (name.endsWith("-DIGEST-MANIFEST-MAIN-ATTRIBUTES")) {
               calculatedMainAttrsDigest = MessageDigest.getInstance(name.substring(0, name.length() - 32));
               expectedMainAttrsDigest = manifestEntry.getValue();
            }
         }

         try (InputStream manifestInputStream = zip.getInputStream(zip.getEntry("META-INF/MANIFEST.MF"))) {
            JarVerifier.ManifestByteParser manifestParser = new JarVerifier.ManifestByteParser(manifestInputStream);
            byte[] manifestMainAttrs = manifestParser.nextSection();
            if (expectedMainAttrsDigest != null) {
               calculatedMainAttrsDigest.update(manifestMainAttrs);
               String calculatedDigestString = Base64.getEncoder().encodeToString(calculatedMainAttrsDigest.digest());
               if (!calculatedDigestString.equals(expectedMainAttrsDigest)) {
                  throw new SecurityException("Invalid signature file digest for Manifest main attributes");
               }
            }

            manifestParser.nextSection();

            byte[] section;
            while ((section = manifestParser.nextSection()) != null) {
               String name = JarVerifier.ManifestByteParser.parseSectionName(section);
               if (zip.getEntry(name) != null) {
                  Map<String, String> sigSection;
                  while ((sigSection = sigParser.nextSection()) != null && !name.equals(sigSection.get("NAME"))) {
                  }

                  if (sigSection == null) {
                     if (sigInputStream != null) {
                        try {
                           sigInputStream.close();
                        } catch (IOException var47) {
                        }
                     }

                     sigInputStream = zip.getInputStream(sigEntry);
                     sigParser = new JarVerifier.ManifestParser(sigInputStream);
                     sigParser.nextSection();

                     while ((sigSection = sigParser.nextSection()) != null && !name.equals(sigSection.get("NAME"))) {
                     }
                  }

                  if (sigSection == null) {
                     return false;
                  }

                  String digestAlg = null;
                  String expectedDigest = null;

                  for (Entry<String, String> entry : sigSection.entrySet()) {
                     String key = entry.getKey();
                     if (key.endsWith("-DIGEST")) {
                        digestAlg = key.substring(0, key.length() - 7);
                        expectedDigest = entry.getValue();
                        break;
                     }
                  }

                  if (digestAlg == null) {
                     return false;
                  }

                  MessageDigest digest = this.getDigest(digestAlg);
                  digest.update(section);
                  String calculatedDigestString = Base64.getEncoder().encodeToString(digest.digest());
                  if (!calculatedDigestString.equals(expectedDigest)) {
                     throw new SecurityException("invalid " + digestAlg + " signature file digest for " + name);
                  }
               }
            }
         }

         return true;
      } finally {
         if (sigInputStream != null) {
            try {
               sigInputStream.close();
            } catch (IOException var45) {
            }
         }
      }
   }

   private MessageDigest getDigest(String digestAlg) throws NoSuchAlgorithmException {
      MessageDigest digest = this.createdDigests.get(digestAlg);
      if (digest == null) {
         digest = MessageDigest.getInstance(digestAlg);
         this.createdDigests.put(digestAlg, digest);
      }

      return digest;
   }

   private static class ManifestByteParser {
      private final InputStream in;
      private final byte[] buffer = new byte[32768];
      private int offset;
      private int len;

      public ManifestByteParser(InputStream in) {
         this.in = in;
      }

      public byte[] nextSection() throws IOException {
         if (this.offset >= this.len - 1) {
            this.offset = 0;
            this.len = this.in.read(this.buffer);
            if (this.offset >= this.len - 1) {
               return null;
            }
         }

         boolean startOfLine = true;

         int pos;
         for (pos = this.offset; pos < this.len; pos++) {
            if (this.buffer[pos] == 10) {
               if (startOfLine) {
                  break;
               }

               startOfLine = true;
            } else if (this.buffer[pos] != 13) {
               startOfLine = false;
            }

            if (pos == this.len - 1) {
               System.arraycopy(this.buffer, this.offset, this.buffer, 0, this.len - this.offset);
               int newLen = this.in.read(this.buffer, this.len - 1, this.buffer.length - this.len);
               if (newLen != -1) {
                  this.len += newLen;
               }

               this.len = this.len - this.offset;
               pos -= this.offset;
               this.offset = 0;
            }
         }

         byte[] section = new byte[pos - this.offset + 1];
         System.arraycopy(this.buffer, this.offset, section, 0, pos - this.offset + 1);
         this.offset = pos + 1;
         return section;
      }

      public static String parseSectionName(byte[] section) throws IOException {
         StringBuilder nameBuilder = null;
         int lineStart = 0;

         for (int i = 0; i < section.length; i++) {
            if (section[i] == 10) {
               int lineLen = i - 1;
               if (section[i - 1] == 13) {
                  lineLen--;
               }

               if (nameBuilder == null) {
                  if (lineLen < 6 || !"NAME: ".equals(new String(section, lineStart, 6, StandardCharsets.UTF_8).toUpperCase(Locale.ENGLISH))) {
                     throw new IOException("invalid manifest format");
                  }

                  nameBuilder = new StringBuilder(new String(section, lineStart + 6, lineLen - 5, StandardCharsets.UTF_8));
                  if (i + 1 >= section.length || section[i + 1] != 32) {
                     return nameBuilder.toString();
                  }
               } else {
                  nameBuilder.append(new String(section, lineStart + 1, lineLen, StandardCharsets.UTF_8));
               }

               lineStart = i + 1;
            }
         }

         return nameBuilder.toString();
      }
   }

   private static class ManifestParser {
      BufferedReader reader;

      public ManifestParser(InputStream in) {
         this.reader = new BufferedReader(new InputStreamReader(in));
      }

      public Map<String, String> nextSection() throws IOException {
         Map<String, String> section = new HashMap<>();
         String name = null;
         boolean skipEmptyLines = true;
         StringBuilder valueBuilder = null;

         String line;
         while ((line = this.reader.readLine()) != null) {
            if (!line.isEmpty() || !skipEmptyLines) {
               skipEmptyLines = false;
               if (line.startsWith(" ")) {
                  if (name == null) {
                     throw new IOException("misplaced continuation line");
                  }

                  valueBuilder.append(line.substring(1));
               } else {
                  if (name != null) {
                     section.put(name.toUpperCase(Locale.ENGLISH), valueBuilder.toString());
                  }

                  if (line.isEmpty()) {
                     break;
                  }

                  int colIndex = line.indexOf(": ");
                  if (colIndex == -1) {
                     throw new IOException("invalid header field");
                  }

                  name = line.substring(0, colIndex);
                  valueBuilder = new StringBuilder(line.substring(colIndex + 2));
               }
            }
         }

         return section.isEmpty() ? null : section;
      }
   }
}
