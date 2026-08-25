package com.tridium.crypto.core.cert;

import com.tridium.crypto.core.util.BouncyCastleHelper;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.DLTaggedObject;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;

public class ASN1HostId extends AttributeTypeAndValue {
   public static final ASN1ObjectIdentifier HOST_ID_OID = new ASN1ObjectIdentifier("1.3.6.1.4.1.4131.2");

   public ASN1HostId(String hostId) {
      super(HOST_ID_OID, new DLTaggedObject(true, 0, new DERUTF8String(hostId)));
   }

   private ASN1HostId(ASN1ObjectIdentifier id, ASN1TaggedObject obj) {
      super(id, obj);
   }

   public static ASN1HostId getInstance(Object o) {
      if (o instanceof ASN1HostId) {
         return (ASN1HostId)o;
      }

      if (o != null) {
         ASN1Sequence seq = ASN1Sequence.getInstance(o);
         if (seq.size() == 2) {
            if (seq.getObjectAt(1) instanceof ASN1TaggedObject) {
               return new ASN1HostId((ASN1ObjectIdentifier)seq.getObjectAt(0), (ASN1TaggedObject)seq.getObjectAt(1));
            } else if (seq.getObjectAt(1) instanceof DERUTF8String) {
               DERUTF8String str = (DERUTF8String)seq.getObjectAt(1);
               return new ASN1HostId((ASN1ObjectIdentifier)seq.getObjectAt(0), new DLTaggedObject(true, 0, str));
            } else {
               throw new IllegalArgumentException("invalid type: " + seq.getObjectAt(1).getClass().getSimpleName());
            }
         } else {
            throw new IllegalArgumentException("asn1 sequence size is incorrect, expecting a size of 2");
         }
      } else {
         throw new IllegalArgumentException("null value in getInstance()");
      }
   }

   public String getHostId() {
      ASN1TaggedObject taggedObject = (ASN1TaggedObject)this.getValue();
      return BouncyCastleHelper.getASN1TaggedObjectValue(taggedObject);
   }
}
