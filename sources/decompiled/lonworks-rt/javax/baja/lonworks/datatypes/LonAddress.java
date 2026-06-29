package javax.baja.lonworks.datatypes;

public interface LonAddress {
   int UNASSIGNED = 0;
   int SUBNET_NODE = 1;
   int NEURON_ID = 2;
   int BROADCAST = 3;
   int IMPLICIT = 126;
   int LOCAL = 127;

   int getAddressType();
}
