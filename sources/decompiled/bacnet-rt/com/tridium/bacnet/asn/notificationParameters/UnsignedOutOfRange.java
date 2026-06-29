package com.tridium.bacnet.asn.notificationParameters;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.io.AsnException;

public class UnsignedOutOfRange extends OutOfRange {
   public UnsignedOutOfRange() {
   }

   public UnsignedOutOfRange(Number exceedingValue, BBacnetBitString statusFlags, Number deadband, Number exceededLimit) {
      super(exceedingValue, statusFlags, deadband, exceededLimit);
   }

   @Override
   public int getChoiceType() {
      return 16;
   }

   @Override
   public String getLongName() {
      return " unsigned-out-of-range\n";
   }

   @Override
   public Number readNumber(AsnInputStream in, int tag) throws AsnException {
      return in.readUnsignedInteger(tag);
   }

   @Override
   public void writeNumber(AsnOutputStream out, int tag, Number n) {
      out.writeUnsignedInteger(tag, n.longValue());
   }
}
