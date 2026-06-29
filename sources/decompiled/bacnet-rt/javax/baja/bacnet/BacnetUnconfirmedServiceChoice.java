package javax.baja.bacnet;

public interface BacnetUnconfirmedServiceChoice {
   int I_AM = 0;
   int I_HAVE = 1;
   int UNCONFIRMED_COV_NOTIFICATION = 2;
   int UNCONFIRMED_EVENT_NOTIFICATION = 3;
   int UNCONFIRMED_PRIVATE_TRANSFER = 4;
   int UNCONFIRMED_TEXT_MESSAGE = 5;
   int TIME_SYNCHRONIZATION = 6;
   int WHO_HAS = 7;
   int WHO_IS = 8;
   int UTC_TIME_SYNCHRONIZATION = 9;
   int WRITE_GROUP = 10;
   String[] TAGS = new String[]{
      "i-Am",
      "i-Have",
      "unconfirmedCovNotification",
      "unconfirmedEventNotification",
      "unconfirmedPrivateTransfer",
      "unconfirmedTextMessage",
      "timeSynchronization",
      "who-Has",
      "who-Is",
      "utcTimeSynchronization",
      "writeGroup"
   };
}
