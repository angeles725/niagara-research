package com.tridium.bacnet.asn;

import com.tridium.bacnet.asn.notificationParameters.AccessEvent;
import com.tridium.bacnet.asn.notificationParameters.BufferReady;
import com.tridium.bacnet.asn.notificationParameters.ChangeOfBitstring;
import com.tridium.bacnet.asn.notificationParameters.ChangeOfCharacterString;
import com.tridium.bacnet.asn.notificationParameters.ChangeOfDiscreteValue;
import com.tridium.bacnet.asn.notificationParameters.ChangeOfLifeSafety;
import com.tridium.bacnet.asn.notificationParameters.ChangeOfReliability;
import com.tridium.bacnet.asn.notificationParameters.ChangeOfState;
import com.tridium.bacnet.asn.notificationParameters.ChangeOfStatusFlags;
import com.tridium.bacnet.asn.notificationParameters.ChangeOfValue;
import com.tridium.bacnet.asn.notificationParameters.CommandFailure;
import com.tridium.bacnet.asn.notificationParameters.ComplexEventType;
import com.tridium.bacnet.asn.notificationParameters.DoubleOutOfRange;
import com.tridium.bacnet.asn.notificationParameters.Extended;
import com.tridium.bacnet.asn.notificationParameters.FloatingLimit;
import com.tridium.bacnet.asn.notificationParameters.OutOfRange;
import com.tridium.bacnet.asn.notificationParameters.SignedOutOfRange;
import com.tridium.bacnet.asn.notificationParameters.UnsignedOutOfRange;
import com.tridium.bacnet.asn.notificationParameters.UnsignedRange;
import java.util.HashMap;
import javax.baja.bacnet.BacnetAlarmConst;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.datatypes.BBacnetDeviceObjectPropertyReference;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.data.BIDataValue;
import javax.baja.sys.Context;

public abstract class BacnetNotificationParameters implements AsnConst, BacnetAlarmConst {
   public static final int CHANGE_OF_BITSTRING_TAG = 0;
   public static final int CHANGE_OF_STATE_TAG = 1;
   public static final int CHANGE_OF_VALUE_TAG = 2;
   public static final int COMMAND_FAILURE_TAG = 3;
   public static final int FLOATING_LIMIT_TAG = 4;
   public static final int OUT_OF_RANGE_TAG = 5;
   public static final int COMPLEX_EVENT_TYPE_TAG = 6;
   public static final int CHANGE_OF_LIFE_SAFETY_TAG = 8;
   public static final int EXTENDED_TAG = 9;
   public static final int BUFFER_READY_TAG = 10;
   public static final int UNSIGNED_RANGE_TAG = 11;
   public static final int RESERVED_TAG = 12;
   public static final int ACCESS_EVENT_TAG = 13;
   public static final int DOUBLE_OOR_TAG = 14;
   public static final int SIGNED_OOR_TAG = 15;
   public static final int UNSIGNED_OOR_TAG = 16;
   public static final int CHANGE_OF_CHARSTRING_TAG = 17;
   public static final int CHANGE_OF_SF_TAG = 18;
   public static final int CHANGE_OF_RELIABILITY_TAG = 19;
   public static final int CHANGE_OF_DISCRETE_VALUE_TAG = 21;

   protected BacnetNotificationParameters() {
   }

   public static BacnetNotificationParameters make(BacnetNotificationParameters bnp) {
      if (bnp == null) {
         return null;
      } else {
         switch (bnp.getChoiceType()) {
            case 0:
               ChangeOfBitstring cob = (ChangeOfBitstring)bnp;
               return new ChangeOfBitstring(cob.getReferencedBitstring(), cob.getStatusFlags());
            case 1:
               ChangeOfState cos = (ChangeOfState)bnp;
               return new ChangeOfState(cos.getNewState(), cos.getStatusFlags());
            case 2:
               ChangeOfValue cov = (ChangeOfValue)bnp;
               if (cov.isValue()) {
                  return new ChangeOfValue(cov.getChangedValue(), cov.getStatusFlags());
               }

               return new ChangeOfValue(cov.getChangedBits(), cov.getStatusFlags());
            case 3:
               CommandFailure cmdf = (CommandFailure)bnp;
               return new CommandFailure(cmdf.getCommandValue(), cmdf.getStatusFlags(), cmdf.getFeedbackValue());
            case 4:
               FloatingLimit fl = (FloatingLimit)bnp;
               return new FloatingLimit(fl.getReferenceValue(), fl.getStatusFlags(), fl.getSetpointValue(), fl.getErrorLimit());
            case 5:
               OutOfRange oor = (OutOfRange)bnp;
               return new OutOfRange(oor.getExceedingValue(), oor.getStatusFlags(), oor.getDeadband(), oor.getExceededLimit());
            case 6:
               ComplexEventType cet = (ComplexEventType)bnp;
               return new ComplexEventType(cet.getListOfPropertyValues());
            case 7:
            case 12:
            case 20:
            default:
               return null;
            case 8:
               ChangeOfLifeSafety cols = (ChangeOfLifeSafety)bnp;
               return new ChangeOfLifeSafety(cols.getNewState(), cols.getNewMode(), cols.getStatusFlags(), cols.getOperationExpected());
            case 9:
               Extended e = (Extended)bnp;
               return new Extended(e.getVendorId(), e.getExtendedEventType(), e.getParameters());
            case 10:
               BufferReady bufr = (BufferReady)bnp;
               return new BufferReady(
                  (BBacnetDeviceObjectPropertyReference)bufr.getBufferProperty().newCopy(), bufr.getPreviousNotification(), bufr.getCurrentNotification()
               );
            case 11:
               UnsignedRange ur = (UnsignedRange)bnp;
               return new UnsignedRange(ur.getExceedingValue(), ur.getStatusFlags(), ur.getExceedingLimit());
            case 13:
               AccessEvent ae = (AccessEvent)bnp;
               return new AccessEvent(
                  ae.getAccessEvent(),
                  ae.getStatusFlags(),
                  ae.getAccessEventTag(),
                  ae.getAccessEventTimeStamp(),
                  ae.getAccessCredential(),
                  ae.getAuthenticationFactor()
               );
            case 14:
               DoubleOutOfRange door = (DoubleOutOfRange)bnp;
               return new DoubleOutOfRange(door.getExceedingValue(), door.getStatusFlags(), door.getDeadband(), door.getExceededLimit());
            case 15:
               SignedOutOfRange soor = (SignedOutOfRange)bnp;
               return new SignedOutOfRange(soor.getExceedingValue(), soor.getStatusFlags(), soor.getDeadband(), soor.getExceededLimit());
            case 16:
               UnsignedOutOfRange usoor = (UnsignedOutOfRange)bnp;
               return new UnsignedOutOfRange(usoor.getExceedingValue(), usoor.getStatusFlags(), usoor.getDeadband(), usoor.getExceededLimit());
            case 17:
               ChangeOfCharacterString cocs = (ChangeOfCharacterString)bnp;
               return new ChangeOfCharacterString(cocs.getChangedValue(), cocs.getStatusFlags(), cocs.getAlarmValue());
            case 18:
               ChangeOfStatusFlags cosf = (ChangeOfStatusFlags)bnp;
               return new ChangeOfStatusFlags(cosf.getPresentValue(), cosf.getStatusFlags());
            case 19:
               ChangeOfReliability cr = (ChangeOfReliability)bnp;
               return new ChangeOfReliability(cr.getReliability(), cr.getStatusFlags(), cr.getPropertyValues());
            case 21:
               ChangeOfDiscreteValue changeOfDiscreteValue = (ChangeOfDiscreteValue)bnp;
               return new ChangeOfDiscreteValue(changeOfDiscreteValue.getNewValue(), changeOfDiscreteValue.getStatusFlags());
         }
      }
   }

   public static BacnetNotificationParameters parseEncoded(BBacnetObjectIdentifier deviceId, AsnInputStream inputStream) throws AsnException {
      BacnetNotificationParameters np = null;
      switch (inputStream.peekTag()) {
         case 0:
            np = new ChangeOfBitstring();
            break;
         case 1:
            np = new ChangeOfState();
            break;
         case 2:
            np = new ChangeOfValue();
            break;
         case 3:
            np = new CommandFailure();
            break;
         case 4:
            np = new FloatingLimit();
            break;
         case 5:
            np = new OutOfRange();
            break;
         case 6:
            np = new ComplexEventType();
         case 7:
         case 12:
         case 20:
         default:
            break;
         case 8:
            np = new ChangeOfLifeSafety();
            break;
         case 9:
            np = new Extended();
            break;
         case 10:
            np = new BufferReady();
            break;
         case 11:
            np = new UnsignedRange();
            break;
         case 13:
            np = new AccessEvent();
            break;
         case 14:
            np = new DoubleOutOfRange();
            break;
         case 15:
            np = new SignedOutOfRange();
            break;
         case 16:
            np = new UnsignedOutOfRange();
            break;
         case 17:
            np = new ChangeOfCharacterString();
            break;
         case 18:
            np = new ChangeOfStatusFlags();
            break;
         case 19:
            np = new ChangeOfReliability();
            break;
         case 21:
            np = new ChangeOfDiscreteValue();
      }

      if (np != null) {
         np.readEncoded(deviceId, inputStream);
      }

      return np;
   }

   public abstract int getChoiceType();

   public abstract String toFacetString();

   public String toString(Context cx) {
      return cx != null && cx.equals(BacnetConst.facetsContext) ? this.toFacetString() : this.toString();
   }

   public abstract void addAlarmData(HashMap<String, ? super BIDataValue> var1);

   public abstract void writeEncoded(AsnOutputStream var1);

   public abstract void readEncoded(BBacnetObjectIdentifier var1, AsnInputStream var2) throws AsnException;

   public abstract void readEncoded(AsnInputStream var1) throws AsnException;
}
