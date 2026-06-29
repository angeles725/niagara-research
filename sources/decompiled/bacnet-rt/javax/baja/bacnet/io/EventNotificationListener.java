package javax.baja.bacnet.io;

import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetTimeStamp;
import javax.baja.bacnet.enums.BBacnetNotifyType;
import javax.baja.bacnet.enums.BCharacterSetEncoding;
import javax.baja.sys.BEnum;

public interface EventNotificationListener extends BacnetServiceListener {
   void receiveConfirmedEventNotification(
      BBacnetAddress var1,
      long var2,
      BBacnetObjectIdentifier var4,
      BBacnetObjectIdentifier var5,
      BBacnetTimeStamp var6,
      long var7,
      int var9,
      BEnum var10,
      String var11,
      BBacnetNotifyType var12,
      boolean var13,
      BEnum var14,
      BEnum var15,
      byte[] var16,
      BCharacterSetEncoding var17
   );

   void receiveUnconfirmedEventNotification(
      BBacnetAddress var1,
      long var2,
      BBacnetObjectIdentifier var4,
      BBacnetObjectIdentifier var5,
      BBacnetTimeStamp var6,
      long var7,
      int var9,
      BEnum var10,
      String var11,
      BBacnetNotifyType var12,
      boolean var13,
      BEnum var14,
      BEnum var15,
      byte[] var16,
      BCharacterSetEncoding var17
   );
}
