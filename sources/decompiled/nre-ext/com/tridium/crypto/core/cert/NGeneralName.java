package com.tridium.crypto.core.cert;

import com.tridium.crypto.core.util.BouncyCastleHelper;
import com.tridium.crypto.core.util.OidMap;
import com.tridium.json.JSONObject;
import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.platform.PlatformUtil;
import com.tridium.nre.util.IPAddressUtil;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.util.ByteArrayUtil;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.GeneralName;

public final class NGeneralName {
   private static final String[] GENERAL_NAME_TAGS = new String[]{
      "otherName", "rfc822Name", "dNSName", "x400Address", "directoryName", "ediPartyName", "uniformResourceIdentifier", "iPAddress", "registeredID"
   };
   private final GeneralName generalName;
   private static final Logger LOGGER = Logger.getLogger("crypto.cert");

   public static NGeneralName make(GeneralName name) {
      return new NGeneralName(name);
   }

   public static NGeneralName makeDirectoryName(String directoryName) {
      if (directoryName != null && !directoryName.trim().isEmpty()) {
         GeneralName generalName = new GeneralName(4, directoryName);
         return new NGeneralName(generalName);
      } else {
         throw new IllegalArgumentException("directoryName cannot be blank");
      }
   }

   public static NGeneralName makeDirectoryName(X500Name directoryName) {
      if (directoryName != null && !directoryName.toString().trim().isEmpty()) {
         GeneralName generalName = new GeneralName(4, directoryName);
         return new NGeneralName(generalName);
      } else {
         throw new IllegalArgumentException("directoryName cannot be blank");
      }
   }

   public static NGeneralName makeHostName(String hostName) throws UnknownHostException {
      return IPAddressUtil.isNumericAddr(hostName) ? makeIpAddress(InetAddress.getByName(hostName)) : makeDnsName(hostName);
   }

   public static NGeneralName makeDnsName(String dnsName) {
      if (dnsName != null && !dnsName.trim().isEmpty()) {
         GeneralName generalName = new GeneralName(2, dnsName);
         return new NGeneralName(generalName);
      } else {
         throw new IllegalArgumentException("dnsName cannot be blank");
      }
   }

   public static NGeneralName makeIpAddress(InetAddress ipAddress) {
      if (ipAddress == null) {
         throw new IllegalArgumentException("ipAddress cannot be blank");
      }

      GeneralName generalName = new GeneralName(7, new DEROctetString(ipAddress.getAddress()));
      return new NGeneralName(generalName);
   }

   public static NGeneralName makeUniformResourceIdentifier(String uri) {
      if (uri != null && !uri.trim().isEmpty()) {
         GeneralName generalName = new GeneralName(6, uri);
         return new NGeneralName(generalName);
      } else {
         throw new IllegalArgumentException("uri cannot be blank");
      }
   }

   public static NGeneralName makeEmailName(String email) {
      if (email != null && !email.trim().isEmpty()) {
         GeneralName generalName = new GeneralName(1, email);
         return new NGeneralName(generalName);
      } else {
         throw new IllegalArgumentException("email cannot be blank");
      }
   }

   public static NGeneralName makeRegisteredID(ASN1ObjectIdentifier oid) {
      if (oid == null) {
         throw new IllegalArgumentException("oid cannot be blank");
      }

      GeneralName generalName = new GeneralName(8, oid);
      return new NGeneralName(generalName);
   }

   public static NGeneralName makeHostID(String hostId) {
      if (hostId != null && !hostId.trim().isEmpty()) {
         ASN1HostId asn1HostId = new ASN1HostId(hostId);
         GeneralName generalName = new GeneralName(0, asn1HostId);
         return new NGeneralName(generalName);
      } else {
         throw new IllegalArgumentException("hostId cannot be blank");
      }
   }

   public static NGeneralName makeHostID() {
      return makeHostID(NGeneralName.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE.getHostId());
   }

   private NGeneralName(GeneralName generalName) {
      this.generalName = generalName;
   }

   public GeneralName getName() {
      return this.generalName;
   }

   public int getTagNo() {
      return this.generalName.getTagNo();
   }

   public ASN1Encodable getASN1Name() {
      return this.generalName.getName();
   }

   public JSONObject getJSON() {
      JSONObject obj = new JSONObject();
      switch (this.generalName.getTagNo()) {
         case 0:
            ASN1Sequence sequence = ASN1Sequence.getInstance(this.generalName.getName());
            ASN1ObjectIdentifier otherOid = ASN1ObjectIdentifier.getInstance(sequence.getObjectAt(0));
            ASN1Encodable encValue = sequence.getObjectAt(1);
            JSONObject otherName = new JSONObject();
            otherName.put("oid", OidMap.get(otherOid.getId()));
            if (encValue instanceof ASN1TaggedObject) {
               otherName.put("value", BouncyCastleHelper.getASN1TaggedObjectValue((ASN1TaggedObject)encValue));
            } else {
               otherName.put("value", encValue.toString());
            }

            obj.put(GENERAL_NAME_TAGS[this.generalName.getTagNo()], otherName);
            break;
         case 1:
         case 2:
         case 6:
            obj.put(GENERAL_NAME_TAGS[this.generalName.getTagNo()], BouncyCastleHelper.getIA5StringValue(this.generalName));
            break;
         case 3:
         case 5:
            obj.put(GENERAL_NAME_TAGS[this.generalName.getTagNo()], this.generalName.getName().toASN1Primitive().toString());
            break;
         case 4:
            X500Name x500Name = X500Name.getInstance(this.generalName.getName());
            obj.put(GENERAL_NAME_TAGS[this.generalName.getTagNo()], x500Name.toString());
            break;
         case 7:
            ASN1OctetString iPAddressOctet = ASN1OctetString.getInstance(this.generalName.getName());

            try {
               InetAddress ipAddress = InetAddress.getByAddress(iPAddressOctet.getOctets());
               obj.put(GENERAL_NAME_TAGS[this.generalName.getTagNo()], ipAddress.getHostAddress());
            } catch (UnknownHostException e) {
               obj.put(GENERAL_NAME_TAGS[this.generalName.getTagNo()], ByteArrayUtil.toHexString(iPAddressOctet.getOctets()));
            }
            break;
         case 8:
            ASN1ObjectIdentifier oid = ASN1ObjectIdentifier.getInstance(this.generalName.getName());
            obj.put(GENERAL_NAME_TAGS[this.generalName.getTagNo()], OidMap.get(oid));
      }

      return obj;
   }

   public String encodeToString() {
      JSONObject obj = new JSONObject();
      obj.put("tagNo", this.getTagNo());
      obj.put("value", this.getValueAsString());
      return obj.toString();
   }

   public String getValueAsString() {
      String encoded = null;
      switch (this.generalName.getTagNo()) {
         case 1:
         case 2:
         case 6:
            encoded = BouncyCastleHelper.getIA5StringValue(this.generalName);
            break;
         case 3:
         case 5:
         default:
            try {
               encoded = ByteArrayUtil.toHexString(this.generalName.getEncoded());
            } catch (IOException e) {
               LOGGER.log(Level.SEVERE, "Unable to obtain encoded form of generalName for '" + this.generalName.getName() + "'", e);
            }
            break;
         case 4:
            X500Name x500Name = X500Name.getInstance(this.generalName.getName());
            encoded = x500Name.toString();
            break;
         case 7:
            ASN1OctetString iPAddressOctet = ASN1OctetString.getInstance(this.generalName.getName());

            try {
               InetAddress ipAddress = InetAddress.getByAddress(iPAddressOctet.getOctets());
               encoded = ipAddress.getHostAddress();
            } catch (UnknownHostException e) {
               encoded = ByteArrayUtil.toHexString(iPAddressOctet.getOctets());
            }
            break;
         case 8:
            ASN1ObjectIdentifier oid = ASN1ObjectIdentifier.getInstance(this.generalName.getName());
            encoded = oid.getId();
      }

      return encoded;
   }

   public static NGeneralName decodeFromString(String val) throws IOException {
      try {
         JSONObject obj = new JSONObject(val);
         int tagNo = obj.getInt("tagNo");
         String value = obj.getString("value");
         GeneralName name;
         switch (tagNo) {
            case 1:
            case 2:
            case 4:
            case 6:
            case 7:
            case 8:
               name = new GeneralName(tagNo, value);
               break;
            case 3:
            case 5:
            default:
               name = GeneralName.getInstance(ByteArrayUtil.hexStringToBytes(value));
         }

         return make(name);
      } catch (Exception e) {
         if (e instanceof IOException) {
            throw e;
         } else {
            throw new IOException("unable to decode general name", e);
         }
      }
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == null) {
         return false;
      } else if (this.getClass() == obj.getClass()) {
         return this.generalName == null ? ((NGeneralName)obj).generalName == null : this.generalName.equals(((NGeneralName)obj).generalName);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.generalName);
   }

   private static final class PlatformProviderHolder {
      private static final IPlatformProvider PLATFORM_PROVIDER_INSTANCE = AccessController.doPrivileged(PlatformUtil::getPlatformProvider);
   }
}
