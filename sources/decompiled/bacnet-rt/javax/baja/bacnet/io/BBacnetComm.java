package javax.baja.bacnet.io;

import com.tridium.bacnet.asn.BacnetNotificationParameters;
import java.util.Vector;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetDateTime;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetOctetString;
import javax.baja.bacnet.datatypes.BBacnetRecipient;
import javax.baja.bacnet.datatypes.BBacnetRecipientProcess;
import javax.baja.bacnet.datatypes.BBacnetTimeStamp;
import javax.baja.bacnet.enums.BBacnetCommControl;
import javax.baja.bacnet.enums.BBacnetEventState;
import javax.baja.bacnet.enums.BBacnetNotifyType;
import javax.baja.bacnet.enums.BBacnetReinitializedDeviceState;
import javax.baja.bacnet.enums.BCharacterSetEncoding;
import javax.baja.bacnet.export.BLocalBacnetDevice;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDouble;
import javax.baja.sys.BEnum;
import javax.baja.sys.BIcon;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "commControl",
   type = "BBacnetCommControl",
   defaultValue = "BBacnetCommControl.enable",
   flags = 2
)
@NiagaraActions({@NiagaraAction(
      name = "enableComm"
   ), @NiagaraAction(
      name = "disableInitiation"
   ), @NiagaraAction(
      name = "disableComm"
   )})
public abstract class BBacnetComm extends BComponent {
   public static final Property commControl = newProperty(2, BBacnetCommControl.enable, null);
   public static final Action enableComm = newAction(0, null);
   public static final Action disableInitiation = newAction(0, null);
   public static final Action disableComm = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BBacnetComm.class);
   private static final BIcon icon = BIcon.std("commConfig.png");

   public BBacnetCommControl getCommControl() {
      return (BBacnetCommControl)this.get(commControl);
   }

   public void setCommControl(BBacnetCommControl v) {
      this.set(commControl, v, null);
   }

   public void enableComm() {
      this.invoke(enableComm, null, null);
   }

   public void disableInitiation() {
      this.invoke(disableInitiation, null, null);
   }

   public void disableComm() {
      this.invoke(disableComm, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BBacnetNetwork;
   }

   public boolean isSiblingLegal(BComponent sibling) {
      return !(sibling instanceof BBacnetComm);
   }

   private BBacnetNetwork net() {
      return (BBacnetNetwork)this.getParent();
   }

   public void doEnableComm() {
      this.setCommControl(BBacnetCommControl.enable);
   }

   public void doDisableInitiation() {
      this.setCommControl(BBacnetCommControl.disableInitiation);
   }

   public void doDisableComm() {
      this.setCommControl(BBacnetCommControl.disable);
   }

   public boolean isCommExecutionEnabled() {
      BLocalBacnetDevice local = this.net().getLocalDevice();
      local.loadSlots();
      return this.getCommControl() != BBacnetCommControl.disable && this.net().getStatus().isValid() && local.getObjectId().isValid();
   }

   public boolean isCommInitiationEnabled() {
      BLocalBacnetDevice local = this.net().getLocalDevice();
      local.loadSlots();
      return this.getCommControl() == BBacnetCommControl.enable && this.net().getStatus().isValid() && local.getObjectId().isValid();
   }

   public abstract void acknowledgeAlarm(
      BBacnetAddress var1,
      long var2,
      BBacnetObjectIdentifier var4,
      BBacnetEventState var5,
      BBacnetTimeStamp var6,
      String var7,
      BBacnetTimeStamp var8,
      BCharacterSetEncoding var9
   ) throws BacnetException;

   public abstract void confirmedCovNotification(
      BBacnetAddress var1, long var2, BBacnetObjectIdentifier var4, BBacnetObjectIdentifier var5, long var6, PropertyValue[] var8
   ) throws BacnetException;

   public abstract void confirmedEventNotification(
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
      BacnetNotificationParameters var16,
      BCharacterSetEncoding var17
   ) throws BacnetException;

   public abstract Vector getAlarmSummary(BBacnetAddress var1) throws BacnetException;

   public abstract Vector getEnrollmentSummary(BBacnetAddress var1, int var2, BBacnetRecipientProcess var3, int var4, BEnum var5, int[] var6, long var7) throws BacnetException;

   public abstract Vector getEventInformation(BBacnetAddress var1, BBacnetObjectIdentifier var2) throws BacnetException;

   public abstract void subscribeCov(BBacnetAddress var1, long var2, BBacnetObjectIdentifier var4, boolean var5, long var6) throws BacnetException;

   public abstract void unsubscribeCov(BBacnetAddress var1, long var2, BBacnetObjectIdentifier var4) throws BacnetException;

   public abstract void subscribeCovProperty(
      BBacnetAddress var1, long var2, BBacnetObjectIdentifier var4, boolean var5, long var6, PropertyReference var8, BDouble var9
   ) throws BacnetException;

   public abstract void unsubscribeCovProperty(BBacnetAddress var1, long var2, BBacnetObjectIdentifier var4, PropertyReference var5) throws BacnetException;

   public abstract FileData atomicReadFileRecord(BBacnetAddress var1, BBacnetObjectIdentifier var2, int var3, long var4) throws BacnetException;

   public abstract FileData atomicReadFileStream(BBacnetAddress var1, BBacnetObjectIdentifier var2, int var3, long var4) throws BacnetException;

   public abstract int atomicWriteFileRecord(BBacnetAddress var1, BBacnetObjectIdentifier var2, int var3, long var4, BBacnetOctetString[] var6) throws BacnetException;

   public abstract int atomicWriteFileStream(BBacnetAddress var1, BBacnetObjectIdentifier var2, int var3, byte[] var4) throws BacnetException;

   public abstract void addListElement(BBacnetAddress var1, BBacnetObjectIdentifier var2, int var3, byte[] var4) throws BacnetException;

   public abstract void addListElement(BBacnetAddress var1, BBacnetObjectIdentifier var2, int var3, int var4, byte[] var5) throws BacnetException;

   public abstract void removeListElement(BBacnetAddress var1, BBacnetObjectIdentifier var2, int var3, byte[] var4) throws BacnetException;

   public abstract void removeListElement(BBacnetAddress var1, BBacnetObjectIdentifier var2, int var3, int var4, byte[] var5) throws BacnetException;

   public abstract BBacnetObjectIdentifier createObject(BBacnetAddress var1, int var2, Array var3) throws BacnetException;

   public abstract BBacnetObjectIdentifier createObject(BBacnetAddress var1, BBacnetObjectIdentifier var2, Array var3) throws BacnetException;

   public abstract void deleteObject(BBacnetAddress var1, BBacnetObjectIdentifier var2) throws BacnetException;

   public abstract byte[] readProperty(BBacnetAddress var1, BBacnetObjectIdentifier var2, int var3) throws BacnetException;

   public abstract byte[] readProperty(BBacnetAddress var1, BBacnetObjectIdentifier var2, int var3, int var4) throws BacnetException;

   public abstract Vector readPropertyMultiple(BBacnetAddress var1, BBacnetObjectIdentifier var2, Vector var3) throws BacnetException;

   public abstract Vector readPropertyMultiple(BBacnetAddress var1, Vector var2) throws BacnetException;

   public abstract RangeData readRange(
      BBacnetAddress var1, BBacnetObjectIdentifier var2, int var3, int var4, int var5, long var6, BBacnetDateTime var8, int var9
   ) throws BacnetException;

   public abstract void writeProperty(BBacnetAddress var1, BBacnetObjectIdentifier var2, int var3, int var4, byte[] var5) throws BacnetException;

   public abstract void writeProperty(BBacnetAddress var1, BBacnetObjectIdentifier var2, int var3, byte[] var4) throws BacnetException;

   public abstract void writeProperty(BBacnetAddress var1, BBacnetObjectIdentifier var2, int var3, int var4, byte[] var5, int var6) throws BacnetException;

   public abstract void writePropertyMultiple(BBacnetAddress var1, Vector var2) throws BacnetException;

   public abstract void deviceCommunicationControl(BBacnetAddress var1, BBacnetCommControl var2, BRelTime var3, String var4, BCharacterSetEncoding var5) throws BacnetException;

   public abstract byte[] confirmedPrivateTransfer(BBacnetAddress var1, int var2, int var3, byte[] var4) throws BacnetException;

   public abstract void reinitializeDevice(BBacnetAddress var1, BBacnetReinitializedDeviceState var2, String var3, BCharacterSetEncoding var4) throws BacnetException;

   public abstract void iAm();

   public abstract void iHave(BBacnetObjectIdentifier var1, String var2, BCharacterSetEncoding var3);

   public abstract void unconfirmedCovNotification(
      BBacnetAddress var1, long var2, BBacnetObjectIdentifier var4, BBacnetObjectIdentifier var5, long var6, PropertyValue[] var8
   ) throws BacnetException;

   public abstract void unconfirmedEventNotification(
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
      BacnetNotificationParameters var16,
      BCharacterSetEncoding var17
   ) throws BacnetException;

   public abstract void unconfirmedPrivateTransfer(BBacnetAddress var1, int var2, int var3, byte[] var4) throws BacnetException;

   public abstract void timeSynch(BBacnetRecipient var1) throws BacnetException;

   public abstract void whoHas(BBacnetAddress var1, BBacnetObjectIdentifier var2) throws BacnetException;

   public abstract void whoHas(BBacnetAddress var1, BBacnetObjectIdentifier var2, int var3, int var4) throws BacnetException;

   public abstract void whoHas(BBacnetAddress var1, String var2, BCharacterSetEncoding var3) throws BacnetException;

   public abstract void whoHas(BBacnetAddress var1, String var2, BCharacterSetEncoding var3, int var4, int var5) throws BacnetException;

   public abstract void whoIs(BBacnetAddress var1) throws BacnetException;

   public abstract void whoIs(BBacnetAddress var1, int var2, int var3) throws BacnetException;

   public abstract void utcTimeSynch(BBacnetRecipient var1) throws BacnetException;

   public abstract void registerBacnetListener(BacnetServiceListener var1, int var2);

   public abstract void unregisterBacnetListener(BacnetServiceListener var1, int var2);

   public BIcon getIcon() {
      return icon;
   }
}
