package javax.baja.bacnet.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("acknowledgeAlarm"), @Range("confirmedCovNotification"), @Range("confirmedEventNotification"), @Range("getAlarmSummary"), @Range("getEnrollmentSummary"), @Range("subscribeCov"), @Range("atomicReadFile"), @Range("atomicWriteFile"), @Range("addListElement"), @Range("removeListElement"), @Range("createObject"), @Range("deleteObject"), @Range("readProperty"), @Range("readPropertyConditional"), @Range("readPropertyMultiple"), @Range("writeProperty"), @Range("writePropertyMultiple"), @Range("deviceCommunicationControl"), @Range("confirmedPrivateTransfer"), @Range("confirmedTextMessage"), @Range("reinitializeDevice"), @Range("vtOpen"), @Range("vtClose"), @Range("vtData"), @Range("authenticate"), @Range("requestKey"), @Range("iAm"), @Range("iHave"), @Range("unconfirmedCovNotification"), @Range("unconfirmedEventNotification"), @Range("unconfirmedPrivateTransfer"), @Range("unconfirmedTextMessage"), @Range("timeSynchronization"), @Range("whoHas"), @Range("whoIs"), @Range("readRange"), @Range("utcTimeSynchronization"), @Range("lifeSafetyOperation"), @Range("subscribeCovProperty"), @Range("getEventInformation"), @Range("writeGroup")},
   defaultValue = "acknowledgeAlarm"
)
public final class BBacnetServicesSupported extends BFrozenEnum {
   public static final int ACKNOWLEDGE_ALARM = 0;
   public static final int CONFIRMED_COV_NOTIFICATION = 1;
   public static final int CONFIRMED_EVENT_NOTIFICATION = 2;
   public static final int GET_ALARM_SUMMARY = 3;
   public static final int GET_ENROLLMENT_SUMMARY = 4;
   public static final int SUBSCRIBE_COV = 5;
   public static final int ATOMIC_READ_FILE = 6;
   public static final int ATOMIC_WRITE_FILE = 7;
   public static final int ADD_LIST_ELEMENT = 8;
   public static final int REMOVE_LIST_ELEMENT = 9;
   public static final int CREATE_OBJECT = 10;
   public static final int DELETE_OBJECT = 11;
   public static final int READ_PROPERTY = 12;
   public static final int READ_PROPERTY_CONDITIONAL = 13;
   public static final int READ_PROPERTY_MULTIPLE = 14;
   public static final int WRITE_PROPERTY = 15;
   public static final int WRITE_PROPERTY_MULTIPLE = 16;
   public static final int DEVICE_COMMUNICATION_CONTROL = 17;
   public static final int CONFIRMED_PRIVATE_TRANSFER = 18;
   public static final int CONFIRMED_TEXT_MESSAGE = 19;
   public static final int REINITIALIZE_DEVICE = 20;
   public static final int VT_OPEN = 21;
   public static final int VT_CLOSE = 22;
   public static final int VT_DATA = 23;
   public static final int AUTHENTICATE = 24;
   public static final int REQUEST_KEY = 25;
   public static final int I_AM = 26;
   public static final int I_HAVE = 27;
   public static final int UNCONFIRMED_COV_NOTIFICATION = 28;
   public static final int UNCONFIRMED_EVENT_NOTIFICATION = 29;
   public static final int UNCONFIRMED_PRIVATE_TRANSFER = 30;
   public static final int UNCONFIRMED_TEXT_MESSAGE = 31;
   public static final int TIME_SYNCHRONIZATION = 32;
   public static final int WHO_HAS = 33;
   public static final int WHO_IS = 34;
   public static final int READ_RANGE = 35;
   public static final int UTC_TIME_SYNCHRONIZATION = 36;
   public static final int LIFE_SAFETY_OPERATION = 37;
   public static final int SUBSCRIBE_COV_PROPERTY = 38;
   public static final int GET_EVENT_INFORMATION = 39;
   public static final int WRITE_GROUP = 40;
   public static final BBacnetServicesSupported acknowledgeAlarm = new BBacnetServicesSupported(0);
   public static final BBacnetServicesSupported confirmedCovNotification = new BBacnetServicesSupported(1);
   public static final BBacnetServicesSupported confirmedEventNotification = new BBacnetServicesSupported(2);
   public static final BBacnetServicesSupported getAlarmSummary = new BBacnetServicesSupported(3);
   public static final BBacnetServicesSupported getEnrollmentSummary = new BBacnetServicesSupported(4);
   public static final BBacnetServicesSupported subscribeCov = new BBacnetServicesSupported(5);
   public static final BBacnetServicesSupported atomicReadFile = new BBacnetServicesSupported(6);
   public static final BBacnetServicesSupported atomicWriteFile = new BBacnetServicesSupported(7);
   public static final BBacnetServicesSupported addListElement = new BBacnetServicesSupported(8);
   public static final BBacnetServicesSupported removeListElement = new BBacnetServicesSupported(9);
   public static final BBacnetServicesSupported createObject = new BBacnetServicesSupported(10);
   public static final BBacnetServicesSupported deleteObject = new BBacnetServicesSupported(11);
   public static final BBacnetServicesSupported readProperty = new BBacnetServicesSupported(12);
   public static final BBacnetServicesSupported readPropertyConditional = new BBacnetServicesSupported(13);
   public static final BBacnetServicesSupported readPropertyMultiple = new BBacnetServicesSupported(14);
   public static final BBacnetServicesSupported writeProperty = new BBacnetServicesSupported(15);
   public static final BBacnetServicesSupported writePropertyMultiple = new BBacnetServicesSupported(16);
   public static final BBacnetServicesSupported deviceCommunicationControl = new BBacnetServicesSupported(17);
   public static final BBacnetServicesSupported confirmedPrivateTransfer = new BBacnetServicesSupported(18);
   public static final BBacnetServicesSupported confirmedTextMessage = new BBacnetServicesSupported(19);
   public static final BBacnetServicesSupported reinitializeDevice = new BBacnetServicesSupported(20);
   public static final BBacnetServicesSupported vtOpen = new BBacnetServicesSupported(21);
   public static final BBacnetServicesSupported vtClose = new BBacnetServicesSupported(22);
   public static final BBacnetServicesSupported vtData = new BBacnetServicesSupported(23);
   public static final BBacnetServicesSupported authenticate = new BBacnetServicesSupported(24);
   public static final BBacnetServicesSupported requestKey = new BBacnetServicesSupported(25);
   public static final BBacnetServicesSupported iAm = new BBacnetServicesSupported(26);
   public static final BBacnetServicesSupported iHave = new BBacnetServicesSupported(27);
   public static final BBacnetServicesSupported unconfirmedCovNotification = new BBacnetServicesSupported(28);
   public static final BBacnetServicesSupported unconfirmedEventNotification = new BBacnetServicesSupported(29);
   public static final BBacnetServicesSupported unconfirmedPrivateTransfer = new BBacnetServicesSupported(30);
   public static final BBacnetServicesSupported unconfirmedTextMessage = new BBacnetServicesSupported(31);
   public static final BBacnetServicesSupported timeSynchronization = new BBacnetServicesSupported(32);
   public static final BBacnetServicesSupported whoHas = new BBacnetServicesSupported(33);
   public static final BBacnetServicesSupported whoIs = new BBacnetServicesSupported(34);
   public static final BBacnetServicesSupported readRange = new BBacnetServicesSupported(35);
   public static final BBacnetServicesSupported utcTimeSynchronization = new BBacnetServicesSupported(36);
   public static final BBacnetServicesSupported lifeSafetyOperation = new BBacnetServicesSupported(37);
   public static final BBacnetServicesSupported subscribeCovProperty = new BBacnetServicesSupported(38);
   public static final BBacnetServicesSupported getEventInformation = new BBacnetServicesSupported(39);
   public static final BBacnetServicesSupported writeGroup = new BBacnetServicesSupported(40);
   public static final BBacnetServicesSupported DEFAULT = acknowledgeAlarm;
   public static final Type TYPE = Sys.loadType(BBacnetServicesSupported.class);

   public static BBacnetServicesSupported make(int ordinal) {
      return (BBacnetServicesSupported)acknowledgeAlarm.getRange().get(ordinal, false);
   }

   public static BBacnetServicesSupported make(String tag) {
      return (BBacnetServicesSupported)acknowledgeAlarm.getRange().get(tag);
   }

   private BBacnetServicesSupported(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
