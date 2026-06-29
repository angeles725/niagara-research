package javax.baja.lonworks.datatypes;

import java.io.IOException;
import javax.baja.lonworks.enums.BAddressType;
import javax.baja.lonworks.enums.BLonReceiveTimer;
import javax.baja.lonworks.enums.BLonRepeatTimer;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BInterface;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public interface BIAddressEntry extends BInterface {
   Type TYPE = Sys.loadType(BIAddressEntry.class);

   BAddressType getAddressType();

   boolean isGroupAddress();

   boolean isSubnetNodeAddress();

   boolean isTurnAroundAddress();

   int getSize();

   int getGroupOrSubnet();

   int getMemberOrNode();

   int getDescriptor();

   int getDomain();

   BLonRepeatTimer getRepeatTimer();

   int getRetries();

   BLonReceiveTimer getReceiveTimer();

   BLonRepeatTimer getTransmitTimer();

   boolean isSameAddress(BIAddressEntry var1);

   @Override
   boolean equals(Object var1);

   String encodeToString() throws IOException;

   BSubnetNode getSubnetNodeAddress();

   boolean isExtended();

   String toString(Context var1);
}
