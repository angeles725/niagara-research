package com.tridium.crypto.core.cert.ext;

import com.tridium.crypto.core.cert.CertificateParseException;
import com.tridium.crypto.core.cert.NGeneralName;
import com.tridium.json.JSONArray;
import com.tridium.json.JSONObject;
import com.tridium.json.JSONUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.baja.nre.util.ByteArrayUtil;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.ReasonFlags;

public final class NCRLDistributionPoints extends NBaseCRLDistributionPoints {
   private static final GeneralName[] EMPTY_GENERAL_NAMES = new GeneralName[0];
   private static final DistributionPoint[] EMPTY_DISTRIBUTION_POINTS = new DistributionPoint[0];

   NCRLDistributionPoints(Extension extension) {
      super(extension, Extension.cRLDistributionPoints);
   }

   public static NCRLDistributionPoints make(boolean isCritical, DistributionPoint[] distributionPoints) throws IOException {
      CRLDistPoint crlDistPoint = new CRLDistPoint(distributionPoints);
      Extension crlDistributionPointsExtension = new Extension(Extension.cRLDistributionPoints, isCritical, crlDistPoint.getEncoded("DER"));
      return new NCRLDistributionPoints(crlDistributionPointsExtension);
   }

   @Override
   public String encodeToString() {
      JSONObject crlDistributionPointObject = new JSONObject();
      JSONObject valueObject = new JSONObject();

      for (DistributionPoint point : this.crlDistPoint.getDistributionPoints()) {
         JSONObject distributionPointObject = new JSONObject();
         appendDistributionPoints(point, distributionPointObject);
         appendReasonFlags(point, distributionPointObject);
         appendCrlIssuer(point, distributionPointObject);
         valueObject.append("distributionPoints", distributionPointObject);
      }

      crlDistributionPointObject.put("oid", this.getOid().getId());
      crlDistributionPointObject.put("isCritical", this.isCritical());
      crlDistributionPointObject.put("value", valueObject);
      return crlDistributionPointObject.toString();
   }

   static NCRLDistributionPoints doDecodeFromString(String val) throws IOException, CertificateParseException {
      try {
         boolean isCritical = false;
         JSONObject obj = new JSONObject(val);
         if (obj.has("oid") && new ASN1ObjectIdentifier(obj.getString("oid")).equals(Extension.cRLDistributionPoints)) {
            if (obj.has("isCritical")) {
               isCritical = obj.getBoolean("isCritical");
            }

            JSONObject valueObj = obj.getJSONObject("value");
            List<DistributionPoint> distributionPoints = new ArrayList<>();
            JSONArray distributionPointsArray = valueObj.getJSONArray("distributionPoints");

            for (int i = 0; i < distributionPointsArray.length(); i++) {
               distributionPoints.add(decodeDistributionPoint(distributionPointsArray.getJSONObject(i)));
            }

            return make(isCritical, distributionPoints.toArray(EMPTY_DISTRIBUTION_POINTS));
         }
      } catch (Exception e) {
         if (!(e instanceof IOException) && !(e instanceof CertificateParseException)) {
            throw new IOException("error decoding NCRLDistributionPoint from string", e);
         }

         throw e;
      }

      throw new IOException("error decoding NCRLDistributionPoint from string");
   }

   private static void appendDistributionPoints(DistributionPoint point, JSONObject distributionPointObject) {
      DistributionPointName name = point.getDistributionPoint();
      if (name != null) {
         if (name.getType() == 0) {
            GeneralNames fullNames = GeneralNames.getInstance(name.getName());
            if (fullNames != null) {
               for (GeneralName fullName : fullNames.getNames()) {
                  NGeneralName nFullName = NGeneralName.make(fullName);
                  distributionPointObject.append("fullName", new JSONObject(nFullName.encodeToString()));
               }
            }
         } else {
            try {
               byte[] encodedName = name.getName().toASN1Primitive().getEncoded("DER");
               distributionPointObject.put("nameRelativeToCRLIssuer", ByteArrayUtil.toHexString(encodedName));
            } catch (IOException e) {
               if (logger.isLoggable(Level.FINE)) {
                  logger.log(Level.WARNING, "error encoding distribution point name", e);
               } else {
                  logger.log(Level.WARNING, "error encoding distribution point name");
               }
            }
         }
      }
   }

   private static void appendCrlIssuer(DistributionPoint point, JSONObject distributionPointObject) {
      GeneralNames crlIssuer = point.getCRLIssuer();
      if (crlIssuer != null) {
         for (GeneralName name : crlIssuer.getNames()) {
            NGeneralName nName = NGeneralName.make(name);
            distributionPointObject.append("crlIssuers", new JSONObject(nName.encodeToString()));
         }
      }
   }

   private static void appendReasonFlags(DistributionPoint point, JSONObject distributionPointObject) {
      ReasonFlags reasonFlags = point.getReasons();
      if (reasonFlags != null) {
         distributionPointObject.put("reasons", reasonFlags.intValue());
      }
   }

   private static DistributionPoint decodeDistributionPoint(JSONObject distributionPointObject) throws IOException, CertificateParseException {
      return new DistributionPoint(
         decodeDistributionPointName(distributionPointObject), decodeReasonFlags(distributionPointObject), decodeCrlIssuers(distributionPointObject)
      );
   }

   private static DistributionPointName decodeDistributionPointName(JSONObject distributionPointObject) throws IOException, CertificateParseException {
      if (distributionPointObject.has("nameRelativeToCRLIssuer")) {
         byte[] encodedName = ByteArrayUtil.hexStringToBytes(distributionPointObject.getString("nameRelativeToCRLIssuer"));
         RDN rdns = RDN.getInstance(ASN1Primitive.fromByteArray(encodedName));
         return new DistributionPointName(1, rdns);
      }

      if (!distributionPointObject.has("fullName")) {
         return null;
      }

      JSONArray fullNameArray = distributionPointObject.getJSONArray("fullName");
      List<GeneralName> fullNames = new ArrayList<>();

      for (int i = 0; i < fullNameArray.length(); i++) {
         try {
            fullNames.add(NGeneralName.decodeFromString(JSONUtil.getString(fullNameArray, i)).getName());
         } catch (Exception e) {
            throw new CertificateParseException("crlDistributionPointsFullName", new JSONObject(JSONUtil.getString(fullNameArray, i)).getString("value"));
         }
      }

      return new DistributionPointName(0, new GeneralNames(fullNames.toArray(EMPTY_GENERAL_NAMES)));
   }

   private static ReasonFlags decodeReasonFlags(JSONObject distributionPointObject) {
      return distributionPointObject.has("reasons") ? new ReasonFlags(distributionPointObject.getInt("reasons")) : null;
   }

   private static GeneralNames decodeCrlIssuers(JSONObject distributionPointObject) throws IOException, CertificateParseException {
      if (!distributionPointObject.has("crlIssuers")) {
         return null;
      }

      JSONArray crlIssuersArray = distributionPointObject.getJSONArray("crlIssuers");
      List<GeneralName> generalNames = new ArrayList<>();

      for (int i = 0; i < crlIssuersArray.length(); i++) {
         try {
            NGeneralName crlIssuerName = NGeneralName.decodeFromString(JSONUtil.getString(crlIssuersArray, i));
            generalNames.add(crlIssuerName.getName());
         } catch (Exception e) {
            throw new CertificateParseException("crlDistributionPointsCrlIssuers", new JSONObject(JSONUtil.getString(crlIssuersArray, i)).getString("value"));
         }
      }

      return new GeneralNames(generalNames.toArray(EMPTY_GENERAL_NAMES));
   }
}
