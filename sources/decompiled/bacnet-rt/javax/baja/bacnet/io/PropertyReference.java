package javax.baja.bacnet.io;

public interface PropertyReference {
   int getPropertyId();

   int getPropertyArrayIndex();

   void writeAsn(AsnOutput var1);

   void readAsn(AsnInput var1) throws AsnException;
}
