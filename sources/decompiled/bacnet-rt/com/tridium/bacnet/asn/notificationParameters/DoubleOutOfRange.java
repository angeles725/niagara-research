package com.tridium.bacnet.asn.notificationParameters;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.io.AsnException;

public class DoubleOutOfRange extends OutOfRange {
   public DoubleOutOfRange() {
   }

   public DoubleOutOfRange(Number exceedingValue, BBacnetBitString statusFlags, Number deadband, Number exceededLimit) {
      super(exceedingValue, statusFlags, deadband, exceededLimit);
   }

   @Override
   public int getChoiceType() {
      return 14;
   }

   @Override
   public String getLongName() {
      return " double-out-of-range\n";
   }

   @Override
   public Number readNumber(AsnInputStream in, int tag) throws AsnException {
      return in.readDouble(tag);
   }

   @Override
   public void writeNumber(AsnOutputStream out, int tag, Number n) {
      out.writeDouble(tag, n.doubleValue());
   }
}
