package com.tridium.bacnet.services;

import com.tridium.bacnet.asn.AsnConst;
import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.RejectException;

public abstract class BacnetServicePrimitive implements AsnConst {
   public static final int CONFIRMED = 0;
   public static final int UNCONFIRMED = 1;
   public static final int SIMPLE_ACK = 2;
   public static final int COMPLEX_ACK = 3;
   public static final int SEGMENT_ACK = 4;
   public static final int ERROR = 5;
   public static final int REJECT = 6;
   public static final int ABORT = 7;
   private static final int[] SERVICE_BITS = new int[]{
      0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 35, 37, 38, 39
   };
   private int serviceChoice;
   private int serviceType;

   protected BacnetServicePrimitive(int serviceType, int serviceChoice) {
      this.serviceType = serviceType;
      this.serviceChoice = serviceChoice;
   }

   public int getServiceType() {
      return this.serviceType;
   }

   public int getServiceChoice() {
      return this.serviceChoice;
   }

   public int getServiceBitIndex() {
      return SERVICE_BITS[this.serviceChoice];
   }

   public abstract void writeEncoded(AsnOutputStream var1);

   public abstract void readEncoded(AsnInputStream var1) throws AsnException, RejectException;

   public byte[] toEncodedBytes() {
      AsnOutputStream asnOut = AsnOutputStream.make();

      byte[] var2;
      try {
         this.writeEncoded(asnOut);
         var2 = asnOut.toByteArray();
      } finally {
         asnOut.release();
      }

      return var2;
   }
}
