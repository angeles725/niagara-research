package javax.baja.bacnet.io;

public interface PropertyValue {
   int getPropertyId();

   int getPropertyArrayIndex();

   byte[] getPropertyValue();

   int getPriority();

   ErrorType getPropertyAccessError();

   int getErrorClass();

   int getErrorCode();

   boolean isError();

   void writeAsn(AsnOutput var1);

   void readAsn(AsnInput var1) throws AsnException, RejectException;
}
