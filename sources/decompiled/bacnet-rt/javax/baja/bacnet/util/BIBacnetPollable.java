package javax.baja.bacnet.util;

import javax.baja.bacnet.BBacnetDevice;
import javax.baja.driver.util.BIPollable;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatus;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public interface BIBacnetPollable extends BIPollable {
   Type TYPE = Sys.loadType(BIBacnetPollable.class);
   int BACNET_POLLABLE_DEVICE = 0;
   int BACNET_POLLABLE_PROXY_EXT = 1;
   int BACNET_POLLABLE_OBJECT = 2;
   int BACNET_POLLABLE_VIRTUAL = 3;
   int BACNET_POLLABLE_ENROLLMENT = 4;
   int BACNET_POLLABLE_REMOTE_EXT = 5;
   int BACNET_POLLABLE_OTHER = -1;

   BBacnetDevice device();

   int getPollableType();

   @Deprecated
   boolean poll();

   void readFail(String var1);

   void fromEncodedValue(byte[] var1, BStatus var2, Context var3);

   PollListEntry[] getPollListEntries();
}
