package javax.baja.bacnet.io;

public interface ErrorType {
   int getErrorClass();

   int getErrorCode();

   void writeEncoded(AsnOutput var1);

   void readEncoded(AsnInput var1) throws AsnException;
}
