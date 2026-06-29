package javax.baja.bacnet.io;

public interface ChangeListError {
   int ERROR_TYPE_TAG = 0;
   int FIRST_FAILED_ELEMENT_NUMBER_TAG = 1;

   long getFirstFailedElementNumber();

   void writeAsn(AsnOutput var1);

   void readAsn(AsnInput var1) throws AsnException;
}
