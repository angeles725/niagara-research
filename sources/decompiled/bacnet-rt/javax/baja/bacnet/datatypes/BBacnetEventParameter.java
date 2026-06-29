package javax.baja.bacnet.datatypes;

import com.tridium.bacnet.BacUtil;
import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.enums.BBacnetLifeSafetyState;
import javax.baja.bacnet.enums.access.BBacnetAccessEvent;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.bacnet.virtual.BBacnetVirtualProperty;
import javax.baja.bacnet.virtual.BacnetVirtualUtil;
import javax.baja.category.BCategoryMask;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.BPermissions;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.BBlob;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDouble;
import javax.baja.sys.BFacets;
import javax.baja.sys.BFloat;
import javax.baja.sys.BInteger;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BSimple;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.units.BUnit;

@NiagaraType
@NiagaraProperty(
   name = "choice",
   type = "int",
   defaultValue = "BBacnetEventType.NONE",
   facets = {@Facet(
      name = "BFacets.FIELD_EDITOR",
      value = "\"bacnet:BacnetEventTypeFE\""
   )}
)
public final class BBacnetEventParameter extends BComponent implements BIBacnetDataType {
   public static final Property choice = newProperty(0, 20, BFacets.make("fieldEditor", "bacnet:BacnetEventTypeFE"));
   public static final Type TYPE = Sys.loadType(BBacnetEventParameter.class);
   private static final Logger logger = Logger.getLogger("bacnet.datatypes");
   private static final int MAX_TAG = 21;
   private static final BFacets SECONDS_FACETS = BFacets.makeInt(BUnit.getUnit("second"));
   public static final String TIME_DELAY_SLOT_NAME = "timeDelay";
   public static final String LIST_OF_VALUES_SLOT_NAME = "listOfValues";
   public static final String BITMASK_SLOT_NAME = "bitmask";
   public static final String REFERENCED_PROPERTY_INCREMENT_SLOT_NAME = "referencedPropertyIncrement";
   public static final String LIST_OF_ALARM_VALUES_SLOT_NAME = "listOfAlarmValues";
   public static final String FEEDBACK_PROPERTY_REFERENCE_SLOT_NAME = "feedbackPropertyReference";
   public static final String SETPOINT_REFERENCE_SLOT_NAME = "setpointReference";
   public static final String LOW_DIFF_LIMIT_SLOT_NAME = "lowDiffLimit";
   public static final String HIGH_DIFF_LIMIT_SLOT_NAME = "highDiffLimit";
   public static final String LOW_LIMIT_SLOT_NAME = "lowLimit";
   public static final String HIGH_LIMIT_SLOT_NAME = "highLimit";
   public static final String DEADBAND_SLOT_NAME = "deadband";
   public static final String NOTIFICATION_THRESHOLD_SLOT_NAME = "notificationThreshold";
   public static final String PREVIOUS_NOTIFICATION_COUNT_SLOT_NAME = "previousNotificationCount";
   public static final String STATUS_FLAGS_SLOT_NAME = "statusFlags";
   public static final String ACCESS_EVENT_TIME_REFERENCE_SLOT_NAME = "accessEventTimeReference";
   public static final int CHANGE_OF_BITSTRING_TAG = 0;
   public static final int CHANGE_OF_BITSTRING_TIME_DELAY_TAG = 0;
   public static final int CHANGE_OF_BITSTRING_BITMASK_TAG = 1;
   public static final int CHANGE_OF_BITSTRING_LIST_OF_BITSTRING_VALUES_TAG = 2;
   public static final int CHANGE_OF_STATE_TAG = 1;
   public static final int CHANGE_OF_STATE_TIME_DELAY_TAG = 0;
   public static final int CHANGE_OF_STATE_LIST_OF_VALUES_TAG = 1;
   public static final int CHANGE_OF_VALUE_TAG = 2;
   public static final int CHANGE_OF_VALUE_TIME_DELAY_TAG = 0;
   public static final int CHANGE_OF_VALUE_COV_CRITERIA_TAG = 1;
   public static final int CHANGE_OF_VALUE_BITMASK_TAG = 0;
   public static final int CHANGE_OF_VALUE_REFERENCED_PROPERTY_INCREMENT_TAG = 1;
   public static final int COMMAND_FAILURE_TAG = 3;
   public static final int COMMAND_FAILURE_TIME_DELAY_TAG = 0;
   public static final int COMMAND_FAILURE_FEEDBACK_PROPERTY_REFERENCE_TAG = 1;
   public static final int FLOATING_LIMIT_TAG = 4;
   public static final int FLOATING_LIMIT_TIME_DELAY_TAG = 0;
   public static final int FLOATING_LIMIT_SETPOINT_REFERENCE_TAG = 1;
   public static final int FLOATING_LIMIT_LOW_DIFF_LIMIT_TAG = 2;
   public static final int FLOATING_LIMIT_HIGH_DIFF_LIMIT_TAG = 3;
   public static final int FLOATING_LIMIT_DEADBAND_TAG = 4;
   public static final int OUT_OF_RANGE_TAG = 5;
   public static final int OUT_OF_RANGE_TIME_DELAY_TAG = 0;
   public static final int OUT_OF_RANGE_LOW_LIMIT_TAG = 1;
   public static final int OUT_OF_RANGE_HIGH_LIMIT_TAG = 2;
   public static final int OUT_OF_RANGE_DEADBAND_TAG = 3;
   public static final int COMPLEX_EVENT_TYPE_TAG = 6;
   public static final int BUFFER_READY_DEPRECATED_TAG = 7;
   public static final int BUFFER_READY_DEPRECATED_NOTIFICATION_THRESHOLD_TAG = 0;
   public static final int BUFFER_READY_DEPRECATED_PREVIOUS_NOTIFICATION_COUNT_TAG = 1;
   public static final int CHANGE_OF_LIFE_SAFETY_TAG = 8;
   public static final int CHANGE_OF_LIFE_SAFETY_TIME_DELAY_TAG = 0;
   public static final int CHANGE_OF_LIFE_SAFETY_LIST_OF_LIFE_SAFETY_ALARM_VALUES_TAG = 1;
   public static final int CHANGE_OF_LIFE_SAFETY_LIST_ALARM_VALUES_TAG = 2;
   public static final int CHANGE_OF_LIFE_SAFETY_MODE_PROPERTY_REFERENCES_TAG = 3;
   public static final int EXTENDED_TAG = 9;
   public static final int EXTENDED_VENDOR_ID_TAG = 0;
   public static final int EXTENDED_EXTENDED_EVENT_TYPE_TAG = 1;
   public static final int EXTENDED_PARAMETERS_TAG = 2;
   public static final int EXTENDED_REFERENCE_TAG = 0;
   public static final int BUFFER_READY_TAG = 10;
   public static final int BUFFER_READY_NOTIFICATION_THRESHOLD_TAG = 0;
   public static final int BUFFER_READY_PREVIOUS_NOTIFICATION_COUNT_TAG = 1;
   public static final int UNSIGNED_RANGE_TAG = 11;
   public static final int UNSIGNED_RANGE_TIME_DELAY_TAG = 0;
   public static final int UNSIGNED_RANGE_LOW_LIMIT_TAG = 1;
   public static final int UNSIGNED_RANGE_HIGH_LIMIT_TAG = 2;
   public static final String ACCESS_EVENT_PREFIX = "accessEvent";
   public static final int ACCESS_EVENT_TAG = 13;
   public static final int LIST_OF_ACCESS_EVENTS_TAG = 0;
   public static final int ACCESS_EVENT_TIME_REFERENCE_TAG = 1;
   public static final int DOUBLE_OOR_TAG = 14;
   public static final int SIGNED_OOR_TAG = 15;
   public static final int UNSIGNED_OOR_TAG = 16;
   public static final String CHANGE_OF_CHAR_STR_PREFIX = "chrstr";
   public static final int CHANGE_OF_CHAR_STR_TAG = 17;
   public static final int CHANGE_OF_CHAR_STR_TIME_DELAY_TAG = 0;
   public static final int CHANGE_OF_CHAR_STR_ALARM_VALUES_TAG = 1;
   public static final int MAX_ALARM_VALUES = 50;
   public static final int CHANGE_OF_STATUS_FLAGS_TAG = 18;
   public static final int CHANGE_OF_RELIABILITY_FLAGS_TAG = 19;
   public static final int COSF_TIME_DELAY_TAG = 0;
   public static final int COSF_SELECTED_FLAGS_TAG = 1;
   public static final int EP_EVENT_TYPE_NONE = 20;
   public static final int CHANGE_OF_DISCRETE_VALUE_TAG = 21;
   public static final int CHANGE_OF_DISCRETE_VALUE_TIME_DELAY_TAG = 0;

   public int getChoice() {
      return this.getInt(choice);
   }

   public void setChoice(int v) {
      this.setInt(choice, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public static BBacnetEventParameter makeChangeOfState(BRelTime timeDelay, BBacnetListOf listOfValues) {
      BBacnetEventParameter eventParams = new BBacnetEventParameter();
      eventParams.setChoice(1);
      eventParams.addTimeDelay(timeDelay);
      eventParams.add("listOfValues", listOfValues);
      return eventParams;
   }

   public static BBacnetEventParameter makeCommandFailure(BRelTime timeDelay, BBacnetDeviceObjectPropertyReference feedbackRef) {
      BBacnetEventParameter eventParams = new BBacnetEventParameter();
      eventParams.setChoice(3);
      eventParams.addTimeDelay(timeDelay);
      eventParams.add("feedbackPropertyReference", feedbackRef);
      return eventParams;
   }

   public static BBacnetEventParameter makeFloatingLimit(
      BRelTime timeDelay, BBacnetDeviceObjectPropertyReference setpointRef, float lowDiffLimit, float highDiffLimit, float deadband
   ) {
      BBacnetEventParameter eventParams = new BBacnetEventParameter();
      eventParams.setChoice(4);
      eventParams.addTimeDelay(timeDelay);
      eventParams.add("setpointReference", setpointRef);
      eventParams.add("lowDiffLimit", BFloat.make(lowDiffLimit));
      eventParams.add("highDiffLimit", BFloat.make(highDiffLimit));
      eventParams.add("deadband", BFloat.make(deadband));
      return eventParams;
   }

   public static BBacnetEventParameter makeOutOfRange(BRelTime timeDelay, float lowLimit, float highLimit, float deadband) {
      BBacnetEventParameter eventParams = new BBacnetEventParameter();
      eventParams.setChoice(5);
      eventParams.addTimeDelay(timeDelay);
      eventParams.add("lowLimit", BFloat.make(lowLimit));
      eventParams.add("highLimit", BFloat.make(highLimit));
      eventParams.add("deadband", BFloat.make(deadband));
      return eventParams;
   }

   public static BBacnetEventParameter makeBufferReady(long notificationThreshold, long previousNotificationCount) {
      BBacnetEventParameter eventParams = new BBacnetEventParameter();
      eventParams.setChoice(10);
      eventParams.add("notificationThreshold", BBacnetUnsigned.make(notificationThreshold));
      eventParams.add("previousNotificationCount", BBacnetUnsigned.make(previousNotificationCount));
      return eventParams;
   }

   public static BBacnetEventParameter makeSignedOutOfRange(BRelTime timeDelay, double lowLimit, double highLimit, double deadband) {
      BBacnetEventParameter eventParams = new BBacnetEventParameter();
      eventParams.setChoice(15);
      eventParams.addTimeDelay(timeDelay);
      eventParams.add("lowLimit", BDouble.make(lowLimit));
      eventParams.add("highLimit", BDouble.make(highLimit));
      eventParams.add("deadband", BDouble.make(deadband));
      return eventParams;
   }

   public static BBacnetEventParameter makeChangeOfCharacterString(BRelTime timeDelay, BBacnetListOf listOfValues) {
      BBacnetEventParameter eventParams = new BBacnetEventParameter();
      eventParams.setChoice(17);
      eventParams.addTimeDelay(timeDelay);
      eventParams.add("listOfAlarmValues", listOfValues);
      return eventParams;
   }

   public static BBacnetEventParameter makeChangeOfDiscreteValue(BRelTime timeDelay) {
      BBacnetEventParameter eventParams = new BBacnetEventParameter();
      eventParams.setChoice(21);
      eventParams.addTimeDelay(timeDelay);
      return eventParams;
   }

   private void addTimeDelay(BRelTime timeDelay) {
      this.add("timeDelay", BBacnetUnsigned.make(timeDelay.getSeconds()), 0, BFacets.makeInt(BUnit.getUnit("second")), null);
   }

   public void changed(Property p, Context cx) {
      if (this.isRunning()) {
         BComplex parent = this.getParent();
         if (parent != null) {
            parent.asComponent().changed(this.getPropertyInParent(), cx);
         }
      }
   }

   public final void subscribed() {
      BBacnetVirtualProperty vp = BacnetVirtualUtil.getVirtualProperty(this);
      if (vp != null) {
         vp.childSubscribed(this);
      }
   }

   public final void unsubscribed() {
      BBacnetVirtualProperty vp = BacnetVirtualUtil.getVirtualProperty(this);
      if (vp != null) {
         vp.childUnsubscribed(this);
      }
   }

   public BCategoryMask getAppliedCategoryMask() {
      return BacnetVirtualUtil.isVirtual(this) ? this.getParent().asComponent().getAppliedCategoryMask() : super.getAppliedCategoryMask();
   }

   public BCategoryMask getCategoryMask() {
      return BacnetVirtualUtil.isVirtual(this) ? this.getParent().asComponent().getCategoryMask() : super.getCategoryMask();
   }

   public BPermissions getPermissions(Context cx) {
      return BacnetVirtualUtil.isVirtual(this) ? this.getParent().asComponent().getPermissions(cx) : super.getPermissions(cx);
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeOpeningTag(this.getChoice());

      try {
         switch (this.getChoice()) {
            case 0:
               out.writeUnsigned(0, (BBacnetUnsigned)this.get("timeDelay"));
               out.writeBitString(1, (BBacnetBitString)this.get("bitmask"));
               out.writeOpeningTag(2);
               ((BBacnetListOf)this.get("listOfBitstringValues")).writeAsn(out);
               out.writeClosingTag(2);
               break;
            case 1:
               out.writeUnsigned(0, (BBacnetUnsigned)this.get("timeDelay"));
               out.writeOpeningTag(1);
               ((BBacnetListOf)this.get("listOfValues")).writeAsn(out);
               out.writeClosingTag(1);
               break;
            case 2:
               out.writeUnsigned(0, (BBacnetUnsigned)this.get("timeDelay"));
               out.writeOpeningTag(1);
               int covCriteriaChoice = ((BInteger)this.get("covCriteria")).getInt();
               switch (covCriteriaChoice) {
                  case 0:
                     out.writeBitString(0, (BBacnetBitString)this.get("bitmask"));
                     break;
                  case 1:
                     out.writeReal(1, (BFloat)this.get("referencedPropertyIncrement"));
                     break;
                  default:
                     throw new AsnException("Invalid tag: " + covCriteriaChoice);
               }

               out.writeClosingTag(1);
               break;
            case 3:
               out.writeUnsigned(0, (BBacnetUnsigned)this.get("timeDelay"));
               out.writeOpeningTag(1);
               ((BBacnetDeviceObjectPropertyReference)this.get("feedbackPropertyReference")).writeAsn(out);
               out.writeClosingTag(1);
               break;
            case 4:
               out.writeUnsigned(0, (BBacnetUnsigned)this.get("timeDelay"));
               out.writeOpeningTag(1);
               ((BBacnetDeviceObjectPropertyReference)this.get("setpointReference")).writeAsn(out);
               out.writeClosingTag(1);
               out.writeReal(2, (BFloat)this.get("lowDiffLimit"));
               out.writeReal(3, (BFloat)this.get("highDiffLimit"));
               out.writeReal(4, (BFloat)this.get("deadband"));
               break;
            case 5:
               out.writeUnsigned(0, (BBacnetUnsigned)this.get("timeDelay"));
               out.writeReal(1, (BFloat)this.get("lowLimit"));
               out.writeReal(2, (BFloat)this.get("highLimit"));
               out.writeReal(3, (BFloat)this.get("deadband"));
               break;
            case 6:
               throw new IllegalStateException("Complex Event Type not supported!");
            case 7:
               out.writeUnsigned(0, (BBacnetUnsigned)this.get("notificationThreshold"));
               out.writeUnsigned(1, (BBacnetUnsigned)this.get("previousNotificationCount"));
               break;
            case 8:
               out.writeUnsigned(0, (BBacnetUnsigned)this.get("timeDelay"));
               out.writeOpeningTag(1);
               ((BBacnetListOf)this.get("listOfLifeSafetyAlarmValues")).writeAsn(out);
               out.writeClosingTag(1);
               out.writeOpeningTag(2);
               ((BBacnetListOf)this.get("listOfAlarmValues")).writeAsn(out);
               out.writeClosingTag(2);
               out.writeOpeningTag(3);
               ((BBacnetDeviceObjectPropertyReference)this.get("modePropertyReference")).writeAsn(out);
               out.writeClosingTag(3);
               break;
            case 9:
               out.writeUnsigned(0, (BBacnetUnsigned)this.get("vendorId"));
               out.writeUnsigned(1, (BBacnetUnsigned)this.get("extendedEventType"));
               out.writeEncodedValue(2, ((BBlob)this.get("parameters")).copyBytes());
               break;
            case 10:
               out.writeUnsigned(0, (BBacnetUnsigned)this.get("notificationThreshold"));
               out.writeUnsigned(1, (BBacnetUnsigned)this.get("previousNotificationCount"));
               break;
            case 11:
               out.writeUnsigned(0, (BBacnetUnsigned)this.get("timeDelay"));
               out.writeUnsigned(1, (BBacnetUnsigned)this.get("lowLimit"));
               out.writeUnsigned(2, (BBacnetUnsigned)this.get("highLimit"));
            case 12:
            default:
               break;
            case 13:
               out.writeOpeningTag(0);

               for (BBacnetAccessEvent accessEvent : (BBacnetAccessEvent[])this.getChildren(BBacnetAccessEvent.class)) {
                  out.writeEnumerated(accessEvent);
               }

               out.writeClosingTag(0);
               BBacnetDeviceObjectPropertyReference accessEventTimeReference = (BBacnetDeviceObjectPropertyReference)this.get("accessEventTimeReference");
               out.writeOpeningTag(1);
               accessEventTimeReference.writeAsn(out);
               out.writeClosingTag(1);
               break;
            case 14:
               out.writeUnsigned(0, (BBacnetUnsigned)this.get("timeDelay"));
               out.writeDouble(1, (BDouble)this.get("lowLimit"));
               out.writeDouble(2, (BDouble)this.get("highLimit"));
               out.writeDouble(3, (BDouble)this.get("deadband"));
               break;
            case 15:
               out.writeUnsigned(0, (BBacnetUnsigned)this.get("timeDelay"));
               out.writeSignedInteger(1, ((BDouble)this.get("lowLimit")).getInt());
               out.writeSignedInteger(2, ((BDouble)this.get("highLimit")).getInt());
               out.writeUnsignedInteger(3, ((BDouble)this.get("deadband")).getInt());
               break;
            case 16:
               out.writeUnsigned(0, (BBacnetUnsigned)this.get("timeDelay"));
               out.writeUnsignedInteger(1, ((BDouble)this.get("lowLimit")).getInt());
               out.writeUnsignedInteger(2, ((BDouble)this.get("highLimit")).getInt());
               out.writeUnsignedInteger(3, ((BDouble)this.get("deadband")).getInt());
               break;
            case 17:
               out.writeUnsigned(0, (BBacnetUnsigned)this.get("timeDelay"));
               out.writeOpeningTag(1);
               ((BBacnetListOf)this.get("listOfAlarmValues")).writeAsn(out);
               out.writeClosingTag(1);
               break;
            case 18:
               out.writeUnsigned(0, (BBacnetUnsigned)this.get("timeDelay"));
               BBacnetBitString statusFlags = (BBacnetBitString)this.get("statusFlags");
               if (statusFlags != null) {
                  out.writeBitString(1, statusFlags);
               } else {
                  out.writeBitString(1, BacnetBitStringUtil.DEFAULT_STATUS);
               }
               break;
            case 19:
               throw new IllegalStateException("Change Of Reliability not supported!");
            case 20:
               out.writeNull();
               break;
            case 21:
               out.writeUnsigned(0, (BBacnetUnsigned)this.get("timeDelay"));
         }
      } catch (Exception var7) {
         logger.log(Level.SEVERE, "Exception occurred in writeAsn", (Throwable)var7);
      }

      out.writeClosingTag(this.getChoice());
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      int tag = in.peekTag();
      if (tag >= 0 && tag <= 21) {
         in.skipOpeningTag(tag);
         switch (tag) {
            case 0: {
               long timeDelayValue = in.readUnsignedInteger(0);
               BBacnetBitString bitmask = in.readBitString(1);
               BBacnetListOf listOfBitstringValues = new BBacnetListOf(BBacnetBitString.TYPE);
               listOfBitstringValues.readAsn(AsnInputStream.make(in.readEncodedValue(2)));
               in.skipClosingTag(tag);
               this.updateChoice(tag);
               this.setOrAdd("timeDelay", BBacnetUnsigned.make(timeDelayValue), (BFacets)SECONDS_FACETS.newCopy());
               this.setOrAdd("bitmask", bitmask);
               this.setOrAdd("listOfBitstringValues", listOfBitstringValues);
               break;
            }
            case 1: {
               long timeDelayValue = in.readUnsignedInteger(0);
               BBacnetListOf listOfValues = new BBacnetListOf(BBacnetPropertyStates.TYPE);
               listOfValues.readAsn(AsnInputStream.make(in.readEncodedValue(1)));
               in.skipClosingTag(tag);
               this.updateChoice(tag);
               this.setOrAdd("timeDelay", BBacnetUnsigned.make(timeDelayValue), (BFacets)SECONDS_FACETS.newCopy());
               this.setOrAdd("listOfValues", listOfValues);
               break;
            }
            case 2:
               long timeDelayValuex = in.readUnsignedInteger(0);
               in.skipOpeningTag(1);
               int covCriteria = in.peekTag();
               String covCriteriaSlotName;
               BSimple covCriteriaValue;
               switch (covCriteria) {
                  case 0:
                     covCriteriaSlotName = "bitmask";
                     covCriteriaValue = in.readBitString(covCriteria);
                     break;
                  case 1:
                     covCriteriaSlotName = "referencedPropertyIncrement";
                     covCriteriaValue = in.readFloat(covCriteria);
                     break;
                  default:
                     throw new AsnException("Invalid tag: " + tag);
               }

               in.skipClosingTag(1);
               in.skipClosingTag(tag);
               this.updateChoice(tag);
               this.setOrAdd("timeDelay", BBacnetUnsigned.make(timeDelayValuex), (BFacets)SECONDS_FACETS.newCopy());
               this.setOrAdd("covCriteria", BInteger.make(covCriteria));
               switch (covCriteria) {
                  case 0:
                     if (this.get("referencedPropertyIncrement") != null) {
                        this.remove("referencedPropertyIncrement", noWrite);
                     }
                     break;
                  case 1:
                     if (this.get("bitmask") != null) {
                        this.remove("bitmask", noWrite);
                     }
               }

               this.setOrAdd(covCriteriaSlotName, covCriteriaValue);
               break;
            case 3: {
               long timeDelayValue = in.readUnsignedInteger(0);
               in.skipOpeningTag(1);
               BBacnetDeviceObjectPropertyReference feedbackPropRef = new BBacnetDeviceObjectPropertyReference();
               feedbackPropRef.readAsn(in);
               in.skipClosingTag(1);
               in.skipClosingTag(tag);
               this.updateChoice(tag);
               this.setOrAdd("timeDelay", BBacnetUnsigned.make(timeDelayValue), (BFacets)SECONDS_FACETS.newCopy());
               this.setOrAdd("feedbackPropertyReference", feedbackPropRef);
               break;
            }
            case 4: {
               long timeDelayValue = in.readUnsignedInteger(0);
               in.skipOpeningTag(1);
               BBacnetDeviceObjectPropertyReference setpointRef = new BBacnetDeviceObjectPropertyReference();
               setpointRef.readAsn(in);
               in.skipClosingTag(1);
               BFloat lowDiffLimit = in.readFloat(2);
               BFloat highDiffLimit = in.readFloat(3);
               BFloat deadband = in.readFloat(4);
               in.skipClosingTag(tag);
               this.updateChoice(tag);
               this.setOrAdd("timeDelay", BBacnetUnsigned.make(timeDelayValue), (BFacets)SECONDS_FACETS.newCopy());
               this.setOrAdd("setpointReference", setpointRef);
               this.setOrAdd("lowDiffLimit", lowDiffLimit);
               this.setOrAdd("highDiffLimit", highDiffLimit);
               this.setOrAdd("deadband", deadband);
               break;
            }
            case 5: {
               long timeDelayValue = in.readUnsignedInteger(0);
               BFloat lowLimit = in.readFloat(1);
               BFloat highLimit = in.readFloat(2);
               BFloat deadband = in.readFloat(3);
               in.skipClosingTag(tag);
               this.updateChoice(tag);
               this.setOrAdd("timeDelay", BBacnetUnsigned.make(timeDelayValue), (BFacets)SECONDS_FACETS.newCopy());
               this.setOrAdd("lowLimit", lowLimit);
               this.setOrAdd("highLimit", highLimit);
               this.setOrAdd("deadband", deadband);
               break;
            }
            case 6:
               BValue[] alarmParameters = AsnUtil.fromAsn(in, 6);
               in.skipClosingTag(tag);
               this.updateChoice(tag);

               for (int i = 0; i < alarmParameters.length; i++) {
                  this.setOrAdd(null, alarmParameters[i]);
               }
               break;
            case 7: {
               long notificationThreshold = in.readUnsignedInteger(0);
               long previousNotificationCount = in.readUnsignedInteger(1);
               in.skipClosingTag(tag);
               this.updateChoice(tag);
               this.setOrAdd("notificationThreshold", BBacnetUnsigned.make(notificationThreshold));
               this.setOrAdd("previousNotificationCount", BBacnetUnsigned.make(previousNotificationCount));
               break;
            }
            case 8: {
               long timeDelayValue = in.readUnsignedInteger(0);
               BBacnetListOf lifeSafetyAlarmValues = new BBacnetListOf(BBacnetLifeSafetyState.TYPE);
               lifeSafetyAlarmValues.readAsn(AsnInputStream.make(in.readEncodedValue(1)));
               BBacnetListOf alarmValues = new BBacnetListOf(BBacnetLifeSafetyState.TYPE);
               alarmValues.readAsn(AsnInputStream.make(in.readEncodedValue(2)));
               in.skipOpeningTag(3);
               BBacnetDeviceObjectPropertyReference modePropRef = new BBacnetDeviceObjectPropertyReference();
               modePropRef.readAsn(in);
               in.skipClosingTag(3);
               in.skipClosingTag(tag);
               this.updateChoice(tag);
               this.setOrAdd("timeDelay", BBacnetUnsigned.make(timeDelayValue), (BFacets)SECONDS_FACETS.newCopy());
               this.setOrAdd("listOfLifeSafetyAlarmValues", lifeSafetyAlarmValues);
               this.setOrAdd("listOfAlarmValues", alarmValues);
               this.setOrAdd("modePropertyReference", modePropRef);
               break;
            }
            case 9:
               long vendorId = in.readUnsignedInteger(0);
               long eventType = in.readUnsignedInteger(1);
               byte[] parameters = in.readEncodedValue(2);
               in.skipClosingTag(tag);
               this.updateChoice(tag);
               this.setOrAdd("vendorId", BBacnetUnsigned.make(vendorId));
               this.setOrAdd("extendedEventType", BBacnetUnsigned.make(eventType));
               this.setOrAdd("parameters", BBlob.make(parameters));
               break;
            case 10: {
               long notificationThreshold = in.readUnsignedInteger(0);
               long previousNotificationCount = in.readUnsignedInteger(1);
               in.skipClosingTag(tag);
               this.updateChoice(tag);
               this.setOrAdd("notificationThreshold", BBacnetUnsigned.make(notificationThreshold));
               this.setOrAdd("previousNotificationCount", BBacnetUnsigned.make(previousNotificationCount));
               break;
            }
            case 11: {
               long timeDelayValue = in.readUnsignedInteger(0);
               long lowLimit = in.readUnsignedInteger(1);
               long highLimit = in.readUnsignedInteger(2);
               in.skipClosingTag(tag);
               this.updateChoice(tag);
               this.setOrAdd("timeDelay", BBacnetUnsigned.make(timeDelayValue), (BFacets)SECONDS_FACETS.newCopy());
               this.setOrAdd("lowLimit", BBacnetUnsigned.make(lowLimit));
               this.setOrAdd("highLimit", BBacnetUnsigned.make(highLimit));
            }
            case 12:
            default:
               break;
            case 13:
               List<BBacnetAccessEvent> accessEvents = new ArrayList<>();
               in.skipOpeningTag(0);
               in.peekTag();

               while (!in.isClosingTag(0)) {
                  accessEvents.add(BBacnetAccessEvent.make(in.readEnumerated()));
                  in.peekTag();
               }

               in.skipClosingTag(0);
               in.skipOpeningTag(1);
               BBacnetDeviceObjectPropertyReference accessEventTimeReference = new BBacnetDeviceObjectPropertyReference();
               accessEventTimeReference.readAsn(in);
               in.skipClosingTag(1);
               in.skipClosingTag(tag);
               this.updateChoice(tag);

               for (int i = 0; i < accessEvents.size(); i++) {
                  this.setOrAdd("accessEvent" + i, (BValue)accessEvents.get(i));
               }

               this.setOrAdd("accessEventTimeReference", accessEventTimeReference);
               break;
            case 14: {
               long timeDelayValue = in.readUnsignedInteger(0);
               double lowLimit = in.readDouble(1);
               double highLimit = in.readDouble(2);
               double deadband = in.readDouble(3);
               in.skipClosingTag(tag);
               this.updateChoice(tag);
               this.setOrAdd("timeDelay", BBacnetUnsigned.make(timeDelayValue), (BFacets)SECONDS_FACETS.newCopy());
               this.setOrAdd("lowLimit", BDouble.make(lowLimit));
               this.setOrAdd("highLimit", BDouble.make(highLimit));
               this.setOrAdd("deadband", BDouble.make(deadband));
               break;
            }
            case 15: {
               long timeDelayValue = in.readUnsignedInteger(0);
               int lowLimit = in.readSignedInteger(1);
               int highLimit = in.readSignedInteger(2);
               long deadband = in.readUnsignedInteger(3);
               in.skipClosingTag(tag);
               this.updateChoice(tag);
               this.setOrAdd("timeDelay", BBacnetUnsigned.make(timeDelayValue), (BFacets)SECONDS_FACETS.newCopy());
               this.setOrAdd("lowLimit", BDouble.make(lowLimit));
               this.setOrAdd("highLimit", BDouble.make(highLimit));
               this.setOrAdd("deadband", BDouble.make(deadband));
               break;
            }
            case 16: {
               long timeDelayValue = in.readUnsignedInteger(0);
               int lowLimit = in.readUnsignedInt(1);
               int highLimit = in.readUnsignedInt(2);
               int deadband = in.readUnsignedInt(3);
               in.skipClosingTag(tag);
               this.updateChoice(tag);
               this.setOrAdd("timeDelay", BBacnetUnsigned.make(timeDelayValue), (BFacets)SECONDS_FACETS.newCopy());
               this.setOrAdd("lowLimit", BDouble.make(lowLimit));
               this.setOrAdd("highLimit", BDouble.make(highLimit));
               this.setOrAdd("deadband", BDouble.make(deadband));
               break;
            }
            case 17: {
               long timeDelayValue = in.readUnsignedInteger(0);
               BBacnetListOf charStrValues = new BBacnetListOf(BString.TYPE);
               charStrValues.readAsn(AsnInputStream.make(in.readEncodedValue(1)));
               in.skipClosingTag(tag);
               this.updateChoice(tag);
               this.setOrAdd("timeDelay", BBacnetUnsigned.make(timeDelayValue), (BFacets)SECONDS_FACETS.newCopy());
               this.setOrAdd("listOfAlarmValues", charStrValues);
               break;
            }
            case 18: {
               long timeDelayValue = in.readUnsignedInteger(0);
               BBacnetBitString cosfSelectedFalgs = in.readBitString(1);
               in.skipClosingTag(tag);
               this.updateChoice(tag);
               this.setOrAdd("timeDelay", BBacnetUnsigned.make(timeDelayValue), (BFacets)SECONDS_FACETS.newCopy());
               this.setOrAdd("statusFlags", cosfSelectedFalgs);
               break;
            }
            case 19:
               throw new IllegalStateException("Change Of Reliability not supported!");
            case 20:
               in.readNull();
               in.skipClosingTag(tag);
               this.updateChoice(tag);
               break;
            case 21: {
               long timeDelayValue = in.readUnsignedInteger(0);
               in.skipClosingTag(tag);
               this.updateChoice(tag);
               this.setOrAdd("timeDelay", BBacnetUnsigned.make(timeDelayValue), (BFacets)SECONDS_FACETS.newCopy());
            }
         }
      } else {
         throw new AsnException("Invalid tag: " + tag);
      }
   }

   private void updateChoice(int tag) {
      if (tag != this.getChoice()) {
         this.removeAll(noWrite);
      }

      this.setInt(choice, tag, noWrite);
   }

   private void setOrAdd(String name, BValue value) {
      this.setOrAdd(name, value, null);
   }

   private void setOrAdd(String name, BValue value, BFacets facets) {
      BacUtil.setOrAdd(this, name, value, 0, facets, noWrite);
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetEventParameter", 2);
      out.prop("virtual", BacnetVirtualUtil.isVirtual(this));
      out.endProps();
   }
}
