package com.tridium.bacnet.asn.notificationParameters;

import javax.baja.bacnet.datatypes.BBacnetBitString;

public interface IOutOfRange {
   int getChoiceType();

   Number getExceedingValue();

   BBacnetBitString getStatusFlags();

   Number getDeadband();

   Number getExceededLimit();
}
