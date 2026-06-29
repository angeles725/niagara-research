package com.tridium.bacnet.stack;

import com.tridium.bacnet.asn.BacnetNotificationParameters;
import com.tridium.bacnet.asn.CovNotificationParameters;
import com.tridium.bacnet.asn.EventNotificationParameters;
import com.tridium.bacnet.services.unconfirmed.IAmRequest;
import com.tridium.bacnet.stack.client.BBacnetClientLayer;
import com.tridium.bacnet.stack.network.BBacnetNetworkLayer;
import com.tridium.bacnet.stack.server.BBacnetServerLayer;
import com.tridium.bacnet.stack.transport.BBacnetTransportLayer;
import com.tridium.bacnet.stack.transport.IdManager;
import com.tridium.bacnet.timers.Timers;
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
import javax.baja.bacnet.io.BBacnetComm;
import javax.baja.bacnet.io.BacnetServiceListener;
import javax.baja.bacnet.io.FileData;
import javax.baja.bacnet.io.PropertyReference;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.io.RangeData;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.nre.util.LongHashMap.Iterator;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDouble;
import javax.baja.sys.BEnum;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "client",
      type = "BBacnetClientLayer",
      defaultValue = "new BBacnetClientLayer()"
   ), @NiagaraProperty(
      name = "server",
      type = "BBacnetServerLayer",
      defaultValue = "new BBacnetServerLayer()"
   ), @NiagaraProperty(
      name = "transport",
      type = "BBacnetTransportLayer",
      defaultValue = "new BBacnetTransportLayer()"
   ), @NiagaraProperty(
      name = "network",
      type = "BBacnetNetworkLayer",
      defaultValue = "new BBacnetNetworkLayer()"
   )})
@NiagaraAction(
   name = "dumpDeviceRegistry"
)
public class BBacnetStack extends BBacnetComm implements IAmListener {
   public static final Property client = newProperty(0, new BBacnetClientLayer(), null);
   public static final Property server = newProperty(0, new BBacnetServerLayer(), null);
   public static final Property transport = newProperty(0, new BBacnetTransportLayer(), null);
   public static final Property network = newProperty(0, new BBacnetNetworkLayer(), null);
   public static final Action dumpDeviceRegistry = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BBacnetStack.class);

   public BBacnetClientLayer getClient() {
      return (BBacnetClientLayer)this.get(client);
   }

   public void setClient(BBacnetClientLayer v) {
      this.set(client, v, null);
   }

   public BBacnetServerLayer getServer() {
      return (BBacnetServerLayer)this.get(server);
   }

   public void setServer(BBacnetServerLayer v) {
      this.set(server, v, null);
   }

   public BBacnetTransportLayer getTransport() {
      return (BBacnetTransportLayer)this.get(transport);
   }

   public void setTransport(BBacnetTransportLayer v) {
      this.set(transport, v, null);
   }

   public BBacnetNetworkLayer getNetwork() {
      return (BBacnetNetworkLayer)this.get(network);
   }

   public void setNetwork(BBacnetNetworkLayer v) {
      this.set(network, v, null);
   }

   public void dumpDeviceRegistry() {
      this.invoke(dumpDeviceRegistry, null, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public void descendantsStarted() {
      Timers.start();
      this.getServer().registerIAmListener(this);
   }

   public void stopped() {
      this.getServer().unregisterIAmListener(this);
      Timers.stop();
   }

   public void stopStack() {
      this.getServer().stackStopped();
      this.getTransport().stackStopped();
      this.getNetwork().stackStopped();
   }

   @Override
   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BBacnetNetwork;
   }

   public BBacnetNetwork bacnet() {
      return (BBacnetNetwork)this.getParent();
   }

   @Override
   public void receiveIAm(IAmRequest iAm, BBacnetAddress addr) {
      this.bacnet().updateDeviceInfo(iAm.getObjectId(), addr, iAm.getMaxAPDULengthAccepted(), iAm.getSegmentationSupported(), iAm.getVendorId());
   }

   public void doDumpDeviceRegistry() {
      DeviceRegistry.dump();
      System.out.println("Invoke ID Manager dump:");
      IdManager.dumpTable();
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetStack", 2);
      Iterator it = DeviceRegistry.addressIterator();
      int i = 0;

      while (it.hasNext()) {
         DeviceEntry n = (DeviceEntry)it.next();
         out.prop(++i + "", n);
      }

      out.endProps();
      IdManager.spy(out);
      Timers.spy(out);
   }

   @Override
   public void acknowledgeAlarm(
      BBacnetAddress deviceAddress,
      long acknowledgingProcessId,
      BBacnetObjectIdentifier eventObjectId,
      BBacnetEventState eventStateAcknowledged,
      BBacnetTimeStamp timestamp,
      String acknowledgementSource,
      BBacnetTimeStamp timeOfAcknowledgement,
      BCharacterSetEncoding encoding
   ) throws BacnetException {
      this.getClient()
         .acknowledgeAlarm(
            deviceAddress, acknowledgingProcessId, eventObjectId, eventStateAcknowledged, timestamp, acknowledgementSource, timeOfAcknowledgement, encoding
         );
   }

   @Override
   public void confirmedCovNotification(
      BBacnetAddress address,
      long subscriberProcessId,
      BBacnetObjectIdentifier initiatingDeviceId,
      BBacnetObjectIdentifier monitoredObjectId,
      long timeRemaining,
      PropertyValue[] listOfValues
   ) throws BacnetException {
      CovNotificationParameters cnp = new CovNotificationParameters(subscriberProcessId, initiatingDeviceId, monitoredObjectId, timeRemaining, listOfValues);
      this.getClient().confirmedCovNotification(address, cnp);
   }

   @Override
   public void confirmedEventNotification(
      BBacnetAddress address,
      long processId,
      BBacnetObjectIdentifier initiatingDeviceId,
      BBacnetObjectIdentifier eventObjectId,
      BBacnetTimeStamp timeStamp,
      long notificationClass,
      int priority,
      BEnum eventType,
      String messageText,
      BBacnetNotifyType notifyType,
      boolean ackRequired,
      BEnum fromState,
      BEnum toState,
      BacnetNotificationParameters eventValues,
      BCharacterSetEncoding encoding
   ) throws BacnetException {
      EventNotificationParameters enp = new EventNotificationParameters(
         processId,
         initiatingDeviceId,
         eventObjectId,
         timeStamp,
         notificationClass,
         priority,
         eventType,
         messageText,
         notifyType,
         ackRequired,
         fromState,
         toState,
         eventValues,
         encoding
      );
      this.getClient().confirmedEventNotification(address, enp);
   }

   @Override
   public Vector getAlarmSummary(BBacnetAddress address) throws BacnetException {
      return this.getClient().getAlarmSummary(address);
   }

   @Override
   public Vector getEnrollmentSummary(
      BBacnetAddress address,
      int acknowledgmentFilter,
      BBacnetRecipientProcess enrollmentFilter,
      int eventStateFilter,
      BEnum eventTypeFilter,
      int[] priorityFilter,
      long notificationClassFilter
   ) throws BacnetException {
      return this.getClient()
         .getEnrollmentSummary(address, acknowledgmentFilter, enrollmentFilter, eventStateFilter, eventTypeFilter, priorityFilter, notificationClassFilter);
   }

   @Override
   public Vector getEventInformation(BBacnetAddress address, BBacnetObjectIdentifier lastReceivedObjectId) throws BacnetException {
      return this.getClient().getEventInformation(address, lastReceivedObjectId);
   }

   @Override
   public void subscribeCov(BBacnetAddress address, long processId, BBacnetObjectIdentifier objectId, boolean issueConfirmedNotifications, long lifetime) throws BacnetException {
      this.getClient().subscribeCov(address, processId, objectId, issueConfirmedNotifications, lifetime);
   }

   @Override
   public void unsubscribeCov(BBacnetAddress address, long processId, BBacnetObjectIdentifier objectId) throws BacnetException {
      this.getClient().unsubscribeCov(address, processId, objectId);
   }

   @Override
   public void subscribeCovProperty(
      BBacnetAddress address,
      long processId,
      BBacnetObjectIdentifier objectId,
      boolean issueConfirmedNotifications,
      long lifetime,
      PropertyReference monitoredPropertyIdentifier,
      BDouble covIncrement
   ) throws BacnetException {
      this.getClient().subscribeCovProperty(address, processId, objectId, issueConfirmedNotifications, lifetime, monitoredPropertyIdentifier, covIncrement);
   }

   @Override
   public void unsubscribeCovProperty(BBacnetAddress address, long processId, BBacnetObjectIdentifier objectId, PropertyReference monitoredPropertyIdentifier) throws BacnetException {
      this.getClient().unsubscribeCovProperty(address, processId, objectId, monitoredPropertyIdentifier);
   }

   @Override
   public FileData atomicReadFileRecord(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId, int start, long count) throws BacnetException {
      return this.getClient().atomicReadFile(deviceAddress, objectId, 1, start, count);
   }

   @Override
   public FileData atomicReadFileStream(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId, int start, long count) throws BacnetException {
      return this.getClient().atomicReadFile(deviceAddress, objectId, 0, start, count);
   }

   @Override
   public int atomicWriteFileRecord(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId, int start, long count, BBacnetOctetString[] fileRecordData) throws BacnetException {
      return this.getClient().atomicWriteFileRecord(deviceAddress, objectId, start, count, fileRecordData);
   }

   @Override
   public int atomicWriteFileStream(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId, int start, byte[] fileData) throws BacnetException {
      return this.getClient().atomicWriteFileStream(deviceAddress, objectId, start, fileData);
   }

   @Override
   public void addListElement(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId, int propertyId, byte[] listOfElements) throws BacnetException {
      this.getClient().addListElement(deviceAddress, objectId, propertyId, listOfElements);
   }

   @Override
   public void addListElement(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex, byte[] listOfElements) throws BacnetException {
      this.getClient().addListElement(deviceAddress, objectId, propertyId, propertyArrayIndex, listOfElements);
   }

   @Override
   public void removeListElement(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId, int propertyId, byte[] listOfElements) throws BacnetException {
      this.getClient().removeListElement(deviceAddress, objectId, propertyId, listOfElements);
   }

   @Override
   public void removeListElement(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex, byte[] listOfElements) throws BacnetException {
      this.getClient().removeListElement(deviceAddress, objectId, propertyId, propertyArrayIndex, listOfElements);
   }

   @Override
   public BBacnetObjectIdentifier createObject(BBacnetAddress deviceAddress, int objectType, Array listOfInitialValues) throws BacnetException {
      return this.getClient().createObject(deviceAddress, objectType, listOfInitialValues);
   }

   @Override
   public BBacnetObjectIdentifier createObject(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId, Array listOfInitialValues) throws BacnetException {
      return this.getClient().createObject(deviceAddress, objectId, listOfInitialValues);
   }

   @Override
   public void deleteObject(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId) throws BacnetException {
      this.getClient().deleteObject(deviceAddress, objectId);
   }

   @Override
   public byte[] readProperty(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId, int propertyId) throws BacnetException {
      return this.getClient().readProperty(deviceAddress, objectId, propertyId);
   }

   @Override
   public byte[] readProperty(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex) throws BacnetException {
      return this.getClient().readProperty(deviceAddress, objectId, propertyId, propertyArrayIndex);
   }

   @Override
   public Vector readPropertyMultiple(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId, Vector propRefs) throws BacnetException {
      return this.getClient().readPropertyMultiple(deviceAddress, objectId, propRefs);
   }

   @Override
   public Vector readPropertyMultiple(BBacnetAddress deviceAddress, Vector readAccessSpecs) throws BacnetException {
      return this.getClient().readPropertyMultiple(deviceAddress, readAccessSpecs);
   }

   @Override
   public RangeData readRange(
      BBacnetAddress deviceAddress,
      BBacnetObjectIdentifier objectId,
      int propertyId,
      int propertyArrayIndex,
      int rangeType,
      long referenceIndex,
      BBacnetDateTime referenceTime,
      int count
   ) throws BacnetException {
      return this.getClient().readRange(deviceAddress, objectId, propertyId, propertyArrayIndex, rangeType, referenceIndex, referenceTime, count);
   }

   @Override
   public void writeProperty(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex, byte[] encodedValue) throws BacnetException {
      this.getClient().writeProperty(deviceAddress, objectId, propertyId, propertyArrayIndex, encodedValue);
   }

   @Override
   public void writeProperty(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId, int propertyId, byte[] encodedValue) throws BacnetException {
      this.getClient().writeProperty(deviceAddress, objectId, propertyId, encodedValue);
   }

   @Override
   public void writeProperty(
      BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex, byte[] encodedValue, int priorityLevel
   ) throws BacnetException {
      this.getClient().writeProperty(deviceAddress, objectId, propertyId, propertyArrayIndex, encodedValue, priorityLevel);
   }

   @Override
   public void writePropertyMultiple(BBacnetAddress deviceAddress, Vector writeAccessSpecs) throws BacnetException {
      this.getClient().writePropertyMultiple(deviceAddress, writeAccessSpecs);
   }

   @Override
   public void deviceCommunicationControl(
      BBacnetAddress deviceAddress, BBacnetCommControl enableDisable, BRelTime duration, String password, BCharacterSetEncoding encoding
   ) throws BacnetException {
      this.getClient().deviceCommunicationControl(deviceAddress, enableDisable, duration, password, encoding);
   }

   @Override
   public byte[] confirmedPrivateTransfer(BBacnetAddress deviceAddress, int vendorId, int serviceNumber, byte[] serviceParameters) throws BacnetException {
      return this.getClient().confirmedPrivateTransfer(deviceAddress, vendorId, serviceNumber, serviceParameters);
   }

   @Override
   public void reinitializeDevice(
      BBacnetAddress deviceAddress, BBacnetReinitializedDeviceState reinitializedStateOfDevice, String password, BCharacterSetEncoding encoding
   ) throws BacnetException {
      this.getClient().reinitializeDevice(deviceAddress, reinitializedStateOfDevice, password, encoding);
   }

   @Override
   public void iAm() {
      this.getServer().iAm();
   }

   @Override
   public void iHave(BBacnetObjectIdentifier objectId, String objectName, BCharacterSetEncoding encoding) {
      this.getServer().iHave(objectId, objectName, encoding);
   }

   @Override
   public void unconfirmedCovNotification(
      BBacnetAddress address,
      long subscriberProcessId,
      BBacnetObjectIdentifier initiatingDeviceId,
      BBacnetObjectIdentifier monitoredObjectId,
      long timeRemaining,
      PropertyValue[] listOfValues
   ) throws BacnetException {
      CovNotificationParameters cnp = new CovNotificationParameters(subscriberProcessId, initiatingDeviceId, monitoredObjectId, timeRemaining, listOfValues);
      this.getClient().unconfirmedCovNotification(address, cnp);
   }

   @Override
   public void unconfirmedEventNotification(
      BBacnetAddress address,
      long processId,
      BBacnetObjectIdentifier initiatingDeviceId,
      BBacnetObjectIdentifier eventObjectId,
      BBacnetTimeStamp timeStamp,
      long notificationClass,
      int priority,
      BEnum eventType,
      String messageText,
      BBacnetNotifyType notifyType,
      boolean ackRequired,
      BEnum fromState,
      BEnum toState,
      BacnetNotificationParameters eventValues,
      BCharacterSetEncoding encoding
   ) throws BacnetException {
      EventNotificationParameters enp = new EventNotificationParameters(
         processId,
         initiatingDeviceId,
         eventObjectId,
         timeStamp,
         notificationClass,
         priority,
         eventType,
         messageText,
         notifyType,
         ackRequired,
         fromState,
         toState,
         eventValues,
         encoding
      );
      this.getClient().unconfirmedEventNotification(address, enp);
   }

   @Override
   public void unconfirmedPrivateTransfer(BBacnetAddress deviceAddress, int vendorId, int serviceNumber, byte[] serviceParameters) throws BacnetException {
      this.getClient().unconfirmedPrivateTransfer(deviceAddress, vendorId, serviceNumber, serviceParameters);
   }

   @Override
   public void timeSynch(BBacnetRecipient recipient) throws BacnetException {
      this.getClient().timeSynch(recipient);
   }

   @Override
   public void whoHas(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId) throws BacnetException {
      this.getClient().whoHas(deviceAddress, objectId);
   }

   @Override
   public void whoHas(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId, int lowLimit, int highLimit) throws BacnetException {
      this.getClient().whoHas(deviceAddress, objectId, lowLimit, highLimit);
   }

   @Override
   public void whoHas(BBacnetAddress deviceAddress, String objectName, BCharacterSetEncoding charset) throws BacnetException {
      this.getClient().whoHas(deviceAddress, objectName, charset);
   }

   @Override
   public void whoHas(BBacnetAddress deviceAddress, String objectName, BCharacterSetEncoding charset, int lowLimit, int highLimit) throws BacnetException {
      this.getClient().whoHas(deviceAddress, objectName, charset, lowLimit, highLimit);
   }

   @Override
   public void whoIs(BBacnetAddress deviceAddress) throws BacnetException {
      this.getClient().whoIs(deviceAddress);
   }

   @Override
   public void whoIs(BBacnetAddress deviceAddress, int lowLimit, int highLimit) throws BacnetException {
      this.getClient().whoIs(deviceAddress, lowLimit, highLimit);
   }

   @Override
   public void utcTimeSynch(BBacnetRecipient recipient) throws BacnetException {
      this.getClient().utcTimeSynch(recipient);
   }

   @Override
   public void registerBacnetListener(BacnetServiceListener listener, int serviceIndex) {
      this.getServer().registerBacnetServiceListener(listener, serviceIndex);
   }

   @Override
   public void unregisterBacnetListener(BacnetServiceListener listener, int serviceIndex) {
      this.getServer().unregisterBacnetServiceListener(listener, serviceIndex);
   }
}
