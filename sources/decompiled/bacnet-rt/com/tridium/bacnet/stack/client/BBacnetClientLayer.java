package com.tridium.bacnet.stack.client;

import com.tridium.bacnet.asn.CovNotificationParameters;
import com.tridium.bacnet.asn.EventNotificationParameters;
import com.tridium.bacnet.asn.NBacnetPropertyReference;
import com.tridium.bacnet.asn.NBacnetPropertyValue;
import com.tridium.bacnet.asn.NEventSummary;
import com.tridium.bacnet.asn.NReadAccessResult;
import com.tridium.bacnet.asn.NReadAccessSpec;
import com.tridium.bacnet.services.BacnetComplexAck;
import com.tridium.bacnet.services.BacnetServicePrimitive;
import com.tridium.bacnet.services.confirmed.AcknowledgeAlarmRequest;
import com.tridium.bacnet.services.confirmed.AddListElementRequest;
import com.tridium.bacnet.services.confirmed.AtomicReadFileAck;
import com.tridium.bacnet.services.confirmed.AtomicReadFileRequest;
import com.tridium.bacnet.services.confirmed.AtomicWriteFileAck;
import com.tridium.bacnet.services.confirmed.AtomicWriteFileRequest;
import com.tridium.bacnet.services.confirmed.ConfirmedCovNotificationRequest;
import com.tridium.bacnet.services.confirmed.ConfirmedEventNotificationRequest;
import com.tridium.bacnet.services.confirmed.ConfirmedPrivateTransferAck;
import com.tridium.bacnet.services.confirmed.ConfirmedPrivateTransferRequest;
import com.tridium.bacnet.services.confirmed.CreateObjectAck;
import com.tridium.bacnet.services.confirmed.CreateObjectRequest;
import com.tridium.bacnet.services.confirmed.DeleteObjectRequest;
import com.tridium.bacnet.services.confirmed.DeviceCommunicationControlRequest;
import com.tridium.bacnet.services.confirmed.GetAlarmSummaryAck;
import com.tridium.bacnet.services.confirmed.GetAlarmSummaryRequest;
import com.tridium.bacnet.services.confirmed.GetEnrollmentSummaryAck;
import com.tridium.bacnet.services.confirmed.GetEnrollmentSummaryRequest;
import com.tridium.bacnet.services.confirmed.GetEventInformationAck;
import com.tridium.bacnet.services.confirmed.GetEventInformationRequest;
import com.tridium.bacnet.services.confirmed.ReadPropertyAck;
import com.tridium.bacnet.services.confirmed.ReadPropertyMultipleAck;
import com.tridium.bacnet.services.confirmed.ReadPropertyMultipleRequest;
import com.tridium.bacnet.services.confirmed.ReadPropertyRequest;
import com.tridium.bacnet.services.confirmed.ReadRangeAck;
import com.tridium.bacnet.services.confirmed.ReadRangeRequest;
import com.tridium.bacnet.services.confirmed.ReinitializeDeviceRequest;
import com.tridium.bacnet.services.confirmed.RemoveListElementRequest;
import com.tridium.bacnet.services.confirmed.SubscribeCovPropertyRequest;
import com.tridium.bacnet.services.confirmed.SubscribeCovRequest;
import com.tridium.bacnet.services.confirmed.WritePropertyMultipleRequest;
import com.tridium.bacnet.services.confirmed.WritePropertyRequest;
import com.tridium.bacnet.services.unconfirmed.TimeSynchronizationRequest;
import com.tridium.bacnet.services.unconfirmed.UnconfirmedCovNotificationRequest;
import com.tridium.bacnet.services.unconfirmed.UnconfirmedEventNotificationRequest;
import com.tridium.bacnet.services.unconfirmed.UnconfirmedPrivateTransferRequest;
import com.tridium.bacnet.services.unconfirmed.UtcTimeSynchronizationRequest;
import com.tridium.bacnet.services.unconfirmed.WhoHasRequest;
import com.tridium.bacnet.services.unconfirmed.WhoIsRequest;
import com.tridium.bacnet.stack.AppDebugListener;
import com.tridium.bacnet.stack.BBacnetStack;
import com.tridium.bacnet.stack.BacnetStackErrorCodes;
import com.tridium.bacnet.stack.BacnetStackException;
import com.tridium.bacnet.stack.DeviceRegistry;
import com.tridium.bacnet.stack.network.BNetworkPriority;
import com.tridium.bacnet.stack.transport.BBacnetTransportLayer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetConst;
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
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.enums.BBacnetReinitializedDeviceState;
import javax.baja.bacnet.enums.BCharacterSetEncoding;
import javax.baja.bacnet.export.BIBacnetExportObject;
import javax.baja.bacnet.export.BLocalBacnetDevice;
import javax.baja.bacnet.io.FileData;
import javax.baja.bacnet.io.PropertyReference;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.io.RejectException;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDouble;
import javax.baja.sys.BEnum;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Clock;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BBacnetClientLayer extends BComponent implements BacnetConst, BacnetStackErrorCodes {
   public static final Type TYPE = Sys.loadType(BBacnetClientLayer.class);
   public static final int LOWEST_LIFE_SAFETY_PRIORITY = 63;
   public static final int LOWEST_CRITICAL_EQUIPMENT_PRIORITY = 127;
   public static final int LOWEST_URGENT_PRIORITY = 191;
   public static final int LOWEST_NORMAL_PRIORITY = 255;
   protected static Logger logger = Logger.getLogger("bacnet.client");
   protected ArrayList<AppDebugListener> debugListeners = new ArrayList<>();

   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BBacnetStack;
   }

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
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("BacnetClientLayer.acknowledgeAlarm(" + eventObjectId + ";" + eventStateAcknowledged + ";" + timestamp + ")");
         }

         AcknowledgeAlarmRequest ackRequest = new AcknowledgeAlarmRequest(
            acknowledgingProcessId, eventObjectId, eventStateAcknowledged, timestamp, acknowledgementSource, timeOfAcknowledgement, encoding
         );
         this.debugSend(deviceAddress, ackRequest);
         BacnetServicePrimitive response = this.transport().sendConfirmedRequestSimple(ackRequest, deviceAddress, BNetworkPriority.normal);
         this.debugReceive(deviceAddress, response);
      }
   }

   public void confirmedCovNotification(BBacnetAddress address, CovNotificationParameters cnp) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         ConfirmedCovNotificationRequest ccnr = new ConfirmedCovNotificationRequest(cnp);
         this.debugSend(address, ccnr);
         BacnetServicePrimitive response = this.transport().sendConfirmedRequestSimple(ccnr, address, BNetworkPriority.normal);
         this.debugReceive(address, response);
      }
   }

   public void confirmedEventNotification(BBacnetAddress address, EventNotificationParameters enp) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("BacnetClientLayer.confirmedEventNotification(" + address + ");enp=" + enp);
         }

         ConfirmedEventNotificationRequest cenr = new ConfirmedEventNotificationRequest(enp);
         BNetworkPriority networkPriority = BNetworkPriority.normal;
         int eventPriority = enp.getPriority();
         if (eventPriority < 63) {
            networkPriority = BNetworkPriority.lifeSafety;
         } else if (eventPriority < 127) {
            networkPriority = BNetworkPriority.criticalEquipment;
         } else if (eventPriority < 191) {
            networkPriority = BNetworkPriority.urgent;
         }

         this.debugSend(address, cenr);
         BacnetServicePrimitive response = this.transport().sendConfirmedRequestSimple(cenr, address, networkPriority);
         this.debugReceive(address, response);
      }
   }

   public Vector getAlarmSummary(BBacnetAddress address) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("BacnetClientLayer.getAlarmSummary()");
         }

         GetAlarmSummaryRequest request = new GetAlarmSummaryRequest();
         this.debugSend(address, request);
         GetAlarmSummaryAck ack = (GetAlarmSummaryAck)this.transport().sendConfirmedRequestComplex(request, address, BNetworkPriority.normal);
         this.debugReceive(address, ack);
         return ack.getListOfAlarmSummaries();
      }
   }

   public Vector getEnrollmentSummary(
      BBacnetAddress address,
      int acknowledgmentFilter,
      BBacnetRecipientProcess enrollmentFilter,
      int eventStateFilter,
      BEnum eventTypeFilter,
      int[] priorityFilter,
      long notificationClassFilter
   ) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               "BacnetClientLayer.getEnrollmentSummary("
                  + acknowledgmentFilter
                  + ";"
                  + enrollmentFilter
                  + ";"
                  + eventStateFilter
                  + ";"
                  + eventTypeFilter
                  + ";"
                  + Arrays.toString(priorityFilter)
                  + ";"
                  + notificationClassFilter
                  + ")"
            );
         }

         GetEnrollmentSummaryRequest request = new GetEnrollmentSummaryRequest(
            acknowledgmentFilter, enrollmentFilter, eventStateFilter, eventTypeFilter, priorityFilter, notificationClassFilter
         );
         this.debugSend(address, request);
         GetEnrollmentSummaryAck ack = (GetEnrollmentSummaryAck)this.transport().sendConfirmedRequestComplex(request, address, BNetworkPriority.normal);
         this.debugReceive(address, ack);
         return ack.getListOfEnrollmentSummaries();
      }
   }

   public Vector getEventInformation(BBacnetAddress address, BBacnetObjectIdentifier objectId) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("BacnetClientLayer.getEventInformation(" + objectId + ")");
         }

         GetEventInformationRequest request = new GetEventInformationRequest(objectId);
         this.debugSend(address, request);
         GetEventInformationAck ack = (GetEventInformationAck)this.transport().sendConfirmedRequestComplex(request, address, BNetworkPriority.normal);
         this.debugReceive(address, ack);
         if (!ack.isMoreEvents()) {
            return ack.getListOfEventSummaries();
         } else {
            boolean more = true;
            Vector<NEventSummary> listOfEventSummaries = new Vector<>();
            BBacnetObjectIdentifier lastObjectId = null;

            while (more && ack != null) {
               ListIterator<NEventSummary> i = ack.getEventSummaries();

               while (i.hasNext()) {
                  NEventSummary eventSummary = i.next();
                  listOfEventSummaries.addElement(eventSummary);
                  lastObjectId = eventSummary.getObjectId();
               }

               request.setLastReceivedObjectId(lastObjectId);
               this.debugSend(address, request);
               ack = (GetEventInformationAck)this.transport().sendConfirmedRequestComplex(request, address, BNetworkPriority.normal);
               this.debugReceive(address, ack);
               if (ack != null) {
                  more = ack.isMoreEvents();
               }
            }

            return listOfEventSummaries;
         }
      }
   }

   public void subscribeCov(BBacnetAddress address, long processId, BBacnetObjectIdentifier objectId, boolean issueConfirmedNotifications, long lifetime) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         SubscribeCovRequest request = new SubscribeCovRequest(processId, objectId, issueConfirmedNotifications, lifetime);
         this.debugSend(address, request);
         BacnetServicePrimitive response = this.transport().sendConfirmedRequestSimple(request, address, BNetworkPriority.normal);
         this.debugReceive(address, response);
      }
   }

   public void unsubscribeCov(BBacnetAddress address, long processId, BBacnetObjectIdentifier objectId) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         SubscribeCovRequest request = new SubscribeCovRequest(processId, objectId);
         this.debugSend(address, request);
         BacnetServicePrimitive response = this.transport().sendConfirmedRequestSimple(request, address, BNetworkPriority.normal);
         this.debugReceive(address, response);
      }
   }

   public void subscribeCovProperty(
      BBacnetAddress address,
      long processId,
      BBacnetObjectIdentifier objectId,
      boolean issueConfirmedNotifications,
      long lifetime,
      PropertyReference monitoredPropertyIdentifier,
      BDouble covIncrement
   ) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         SubscribeCovPropertyRequest request = new SubscribeCovPropertyRequest(
            processId, objectId, issueConfirmedNotifications, lifetime, monitoredPropertyIdentifier, covIncrement
         );
         this.debugSend(address, request);
         BacnetServicePrimitive response = this.transport().sendConfirmedRequestSimple(request, address, BNetworkPriority.normal);
         this.debugReceive(address, response);
      }
   }

   public void unsubscribeCovProperty(BBacnetAddress address, long processId, BBacnetObjectIdentifier objectId, PropertyReference monitoredPropertyIdentifier) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         SubscribeCovPropertyRequest request = new SubscribeCovPropertyRequest(processId, objectId, monitoredPropertyIdentifier);
         this.debugSend(address, request);
         BacnetServicePrimitive response = this.transport().sendConfirmedRequestSimple(request, address, BNetworkPriority.normal);
         this.debugReceive(address, response);
      }
   }

   @Deprecated
   public FileData atomicReadFileStream(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId, int start, long count) throws BacnetException {
      return this.atomicReadFile(deviceAddress, objectId, 0, start, count);
   }

   public FileData atomicReadFile(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId, int accessMethod, int start, long count) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("Read " + count + " bytes from File " + objectId + ", starting at " + start);
         }

         AtomicReadFileRequest request = new AtomicReadFileRequest(accessMethod, objectId, start, count);
         this.debugSend(deviceAddress, request);
         BacnetComplexAck response = this.transport().sendConfirmedRequestComplex(request, deviceAddress, BNetworkPriority.normal);
         this.debugReceive(deviceAddress, response);
         AtomicReadFileAck ack = (AtomicReadFileAck)response;
         if (logger.isLoggable(Level.FINE)) {
            if (accessMethod == 0) {
               logger.fine("Read File data size:" + ack.getFileData().length);
            } else {
               logger.fine("Read File record count:" + ack.getFileRecordData().length);
            }
         }

         return ack;
      }
   }

   public int atomicWriteFileRecord(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId, int start, long count, BBacnetOctetString[] fileRecordData) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("Write " + fileRecordData.length + " records to File " + objectId + ", starting at record " + start);
         }

         AtomicWriteFileRequest request = new AtomicWriteFileRequest(objectId, start, count, fileRecordData);
         this.debugSend(deviceAddress, request);
         BacnetComplexAck response = this.transport().sendConfirmedRequestComplex(request, deviceAddress, BNetworkPriority.normal);
         this.debugReceive(deviceAddress, response);
         AtomicWriteFileAck ack = (AtomicWriteFileAck)response;
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("Write File ok: start record=" + ack.getFileStart());
         }

         return ack.getFileStart();
      }
   }

   public int atomicWriteFileStream(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId, int start, byte[] fileData) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         AtomicWriteFileRequest request = new AtomicWriteFileRequest(objectId, start, fileData);
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("Write " + fileData.length + " bytes to File " + objectId + ", starting at " + start);
         }

         this.debugSend(deviceAddress, request);
         BacnetComplexAck response = this.transport().sendConfirmedRequestComplex(request, deviceAddress, BNetworkPriority.normal);
         this.debugReceive(deviceAddress, response);
         AtomicWriteFileAck ack = (AtomicWriteFileAck)response;
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("Write File ok: start position=" + ack.getFileStart());
         }

         return ack.getFileStart();
      }
   }

   public void addListElement(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId, int propertyId, byte[] listOfElements) throws BacnetException {
      this.addListElement(deviceAddress, objectId, propertyId, -1, listOfElements);
   }

   public void addListElement(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex, byte[] listOfElements) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("BacnetClientLayer.addListElement(" + objectId + ";" + propertyId + ";" + propertyArrayIndex + ")");
         }

         AddListElementRequest request = new AddListElementRequest(objectId, propertyId, propertyArrayIndex, listOfElements);
         this.debugSend(deviceAddress, request);
         BacnetServicePrimitive response = this.transport().sendConfirmedRequestSimple(request, deviceAddress, BNetworkPriority.normal);
         this.debugReceive(deviceAddress, response);
      }
   }

   public void removeListElement(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId, int propertyId, byte[] listOfElements) throws BacnetException {
      this.removeListElement(deviceAddress, objectId, propertyId, -1, listOfElements);
   }

   public void removeListElement(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex, byte[] listOfElements) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("BacnetClientLayer.removeListElement(" + objectId + ";" + propertyId + ";" + propertyArrayIndex + ")");
         }

         RemoveListElementRequest request = new RemoveListElementRequest(objectId, propertyId, propertyArrayIndex, listOfElements);
         this.debugSend(deviceAddress, request);
         BacnetServicePrimitive response = this.transport().sendConfirmedRequestSimple(request, deviceAddress, BNetworkPriority.normal);
         this.debugReceive(deviceAddress, response);
      }
   }

   public BBacnetObjectIdentifier createObject(BBacnetAddress deviceAddress, int objectType, Array<PropertyValue> listOfInitialValues) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("BacnetClientLayer.createObject(objectType: " + BBacnetObjectType.tag(objectType) + ')');
         }

         CreateObjectRequest request = new CreateObjectRequest(objectType, listOfInitialValues);
         return this.sendCreateObjectRequest(deviceAddress, request);
      }
   }

   public BBacnetObjectIdentifier createObject(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId, Array<PropertyValue> listOfInitialValues) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("BacnetClientLayer.createObject(objectId: " + objectId + ')');
         }

         CreateObjectRequest request = new CreateObjectRequest(objectId, listOfInitialValues);
         return this.sendCreateObjectRequest(deviceAddress, request);
      }
   }

   private BBacnetObjectIdentifier sendCreateObjectRequest(BBacnetAddress deviceAddress, CreateObjectRequest request) throws BacnetException {
      this.debugSend(deviceAddress, request);
      BacnetServicePrimitive response = this.transport().sendConfirmedRequestComplex(request, deviceAddress, BNetworkPriority.normal);
      this.debugReceive(deviceAddress, response);
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("  response = " + response);
      }

      return ((CreateObjectAck)response).getObjectId();
   }

   public void deleteObject(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("BacnetClientLayer.deleteObject(" + objectId + ")");
         }

         DeleteObjectRequest request = new DeleteObjectRequest(objectId);
         this.debugSend(deviceAddress, request);
         BacnetServicePrimitive response = this.transport().sendConfirmedRequestSimple(request, deviceAddress, BNetworkPriority.normal);
         this.debugReceive(deviceAddress, response);
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(response == null ? "  >>>> FAILED!" : "  ---- OK!");
         }
      }
   }

   public byte[] readProperty(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId, int propertyId) throws BacnetException {
      return this.readProperty(deviceAddress, objectId, propertyId, -1);
   }

   public byte[] readProperty(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               "BacnetClientLayer.readProperty("
                  + deviceAddress
                  + ';'
                  + objectId
                  + ';'
                  + BBacnetPropertyIdentifier.tag(propertyId)
                  + ';'
                  + propertyArrayIndex
                  + ')'
            );
         }

         ReadPropertyRequest request = new ReadPropertyRequest(objectId, propertyId, propertyArrayIndex);
         this.debugSend(deviceAddress, request);
         BacnetServicePrimitive response = this.transport().sendConfirmedRequestComplex(request, deviceAddress, BNetworkPriority.normal);
         this.debugReceive(deviceAddress, response);
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(deviceAddress + " readProperty response = " + response);
         }

         return ((ReadPropertyAck)response).getEncodedValue();
      }
   }

   public Vector readPropertyMultiple(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId, Vector propRefs) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("BacnetClientLayer.readPropertyMultiple() addr=" + deviceAddress);
         }

         ReadPropertyMultipleRequest request = new ReadPropertyMultipleRequest(new NReadAccessSpec(objectId, propRefs));
         this.debugSend(deviceAddress, request);
         ReadPropertyMultipleAck response = (ReadPropertyMultipleAck)this.transport()
            .sendConfirmedRequestComplex(request, deviceAddress, BNetworkPriority.normal);
         this.debugReceive(deviceAddress, response);
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(deviceAddress + " readPropertyMultiple response = " + response);
         }

         NReadAccessResult result = (NReadAccessResult)response.getListOfReadAccessResults().elementAt(0);
         return result.getListOfResults();
      }
   }

   public Vector readPropertyMultiple(BBacnetAddress deviceAddress, Vector readAccessSpecs) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         ReadPropertyMultipleRequest request = new ReadPropertyMultipleRequest(readAccessSpecs);
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("BacnetClientLayer.readPropertyMultiple() addr=" + deviceAddress);
         }

         this.debugSend(deviceAddress, request);
         BacnetServicePrimitive response = this.transport().sendConfirmedRequestComplex(request, deviceAddress, BNetworkPriority.normal);
         this.debugReceive(deviceAddress, response);
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(deviceAddress + " readPropertyMultiple response = " + response);
         }

         ReadPropertyMultipleAck ack = (ReadPropertyMultipleAck)response;
         return ack.getListOfReadAccessResults();
      }
   }

   public Iterator readPropertyMultiple(BBacnetAddress deviceAddress, Array readAccessSpecs) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         ReadPropertyMultipleRequest request = new ReadPropertyMultipleRequest(readAccessSpecs);
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(deviceAddress + " BacnetClientLayer.readPropertyMultiple() request = " + request);
         }

         this.debugSend(deviceAddress, request);
         BacnetServicePrimitive response = this.transport().sendConfirmedRequestComplex(request, deviceAddress, BNetworkPriority.normal);
         this.debugReceive(deviceAddress, response);
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(deviceAddress + " readPropertyMultiple response = " + response);
         }

         return ((ReadPropertyMultipleAck)response).getResults();
      }
   }

   public ReadRangeAck readRange(
      BBacnetAddress deviceAddress,
      BBacnetObjectIdentifier objectId,
      int propertyId,
      int propertyArrayIndex,
      int rangeType,
      long referenceIndex,
      BBacnetDateTime referenceTime,
      int count
   ) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               "BacnetClientLayer.readRange("
                  + objectId
                  + ";"
                  + propertyId
                  + ";"
                  + propertyArrayIndex
                  + ";"
                  + rangeType
                  + ";"
                  + referenceIndex
                  + ";"
                  + referenceTime
                  + ";"
                  + count
                  + ")"
            );
         }

         ReadRangeRequest request = new ReadRangeRequest(objectId, propertyId, propertyArrayIndex, rangeType, referenceIndex, referenceTime, count);
         this.debugSend(deviceAddress, request);
         BacnetServicePrimitive response = this.transport().sendConfirmedRequestComplex(request, deviceAddress, BNetworkPriority.normal);
         this.debugReceive(deviceAddress, response);
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(deviceAddress + " readRange response = " + response);
         }

         return (ReadRangeAck)response;
      }
   }

   public void writeProperty(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex, byte[] encodedValue) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         if (propertyId == 87) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine("Priority array write converted to present-value@" + propertyArrayIndex);
            }

            this.writeProperty(deviceAddress, objectId, 85, -1, encodedValue, propertyArrayIndex);
         } else {
            this.writeProperty(deviceAddress, objectId, propertyId, propertyArrayIndex, encodedValue, -1);
         }
      }
   }

   public void writeProperty(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId, int propertyId, byte[] encodedValue) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         this.writeProperty(deviceAddress, objectId, propertyId, -1, encodedValue, -1);
      }
   }

   public void writeProperty(
      BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex, byte[] encodedValue, int priorityLevel
   ) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         this.verifyUnicastAddress(deviceAddress);
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               "BacnetClientLayer.writeProperty("
                  + objectId
                  + ";"
                  + propertyId
                  + "["
                  + propertyArrayIndex
                  + "]@"
                  + priorityLevel
                  + "): encodedValue="
                  + ByteArrayUtil.toHexString(encodedValue)
            );
         }

         WritePropertyRequest request = new WritePropertyRequest(objectId, propertyId, propertyArrayIndex, encodedValue, priorityLevel);
         this.debugSend(deviceAddress, request);
         BacnetServicePrimitive response = this.transport().sendConfirmedRequestSimple(request, deviceAddress, BNetworkPriority.normal);
         this.debugReceive(deviceAddress, response);
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(response == null ? deviceAddress + "writeProperty response  >>>> FAILED!" : deviceAddress + "writeProperty response ---- OK!");
         }
      }
   }

   public void writePropertyMultiple(BBacnetAddress deviceAddress, Vector writeAccessSpecs) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         this.verifyUnicastAddress(deviceAddress);
         logger.fine("BacnetClientLayer.writePropertyMultiple");
         WritePropertyMultipleRequest request = new WritePropertyMultipleRequest(writeAccessSpecs);
         this.debugSend(deviceAddress, request);
         BacnetServicePrimitive response = this.transport().sendConfirmedRequestSimple(request, deviceAddress, BNetworkPriority.normal);
         this.debugReceive(deviceAddress, response);
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               response == null ? deviceAddress + "writePropertyMultiple response  >>>> FAILED!" : deviceAddress + "writePropertyMultiple response ---- OK!"
            );
         }
      }
   }

   public void deviceCommunicationControl(
      BBacnetAddress deviceAddress, BBacnetCommControl enableDisable, BRelTime duration, String password, BCharacterSetEncoding encoding
   ) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("BacnetClientLayer.deviceCommunicationControl(" + enableDisable + "; " + duration + "; " + password + "; " + encoding + ")");
         }

         DeviceCommunicationControlRequest request = new DeviceCommunicationControlRequest(duration, enableDisable, password, encoding);
         this.debugSend(deviceAddress, request);
         BacnetServicePrimitive response = this.transport().sendConfirmedRequestSimple(request, deviceAddress, BNetworkPriority.normal);
         this.debugReceive(deviceAddress, response);
      }
   }

   public byte[] confirmedPrivateTransfer(BBacnetAddress deviceAddress, int vendorId, int serviceNumber, byte[] serviceParameters) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("BacnetClientLayer.confirmedPrivateTransfer(" + vendorId + ";" + serviceNumber + ":" + ByteArrayUtil.toHexString(serviceParameters));
         }

         ConfirmedPrivateTransferRequest request = new ConfirmedPrivateTransferRequest(vendorId, serviceNumber, serviceParameters);
         this.debugSend(deviceAddress, request);
         BacnetServicePrimitive response = this.transport().sendConfirmedRequestComplex(request, deviceAddress, BNetworkPriority.normal);
         this.debugReceive(deviceAddress, response);
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("  response = " + response);
         }

         return ((ConfirmedPrivateTransferAck)response).getResultBlock();
      }
   }

   public void reinitializeDevice(
      BBacnetAddress deviceAddress, BBacnetReinitializedDeviceState reinitializedStateOfDevice, String password, BCharacterSetEncoding encoding
   ) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("BacnetClientLayer.reinitializeDevice(" + reinitializedStateOfDevice + "; " + password + "; " + encoding + ")");
         }

         ReinitializeDeviceRequest request = new ReinitializeDeviceRequest(reinitializedStateOfDevice, password, encoding);
         this.debugSend(deviceAddress, request);
         BacnetServicePrimitive response = this.transport().sendConfirmedRequestSimple(request, deviceAddress, BNetworkPriority.normal);
         this.debugReceive(deviceAddress, response);
      }
   }

   public void unconfirmedCovNotification(BBacnetAddress address, CovNotificationParameters cnp) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         UnconfirmedCovNotificationRequest ucnr = new UnconfirmedCovNotificationRequest(cnp);
         this.debugSend(address, ucnr);
         this.transport().sendUnconfirmedRequest(ucnr, address, BNetworkPriority.normal);
      }
   }

   public void unconfirmedEventNotification(BBacnetAddress address, EventNotificationParameters enp) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("BacnetClientLayer.unconfirmedEventNotification(" + address + ");enp=" + enp);
         }

         UnconfirmedEventNotificationRequest uenr = new UnconfirmedEventNotificationRequest(enp);
         BNetworkPriority networkPriority = BNetworkPriority.normal;
         int eventPriority = enp.getPriority();
         if (eventPriority < 63) {
            networkPriority = BNetworkPriority.lifeSafety;
         } else if (eventPriority < 127) {
            networkPriority = BNetworkPriority.criticalEquipment;
         } else if (eventPriority < 191) {
            networkPriority = BNetworkPriority.urgent;
         }

         this.debugSend(address, uenr);
         this.transport().sendUnconfirmedRequest(uenr, address, networkPriority);
      }
   }

   public void unconfirmedPrivateTransfer(BBacnetAddress deviceAddress, int vendorId, int serviceNumber, byte[] serviceParameters) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("BacnetClientLayer.unconfirmedPrivateTransfer(" + vendorId + ";" + serviceNumber + ":" + ByteArrayUtil.toHexString(serviceParameters));
         }

         UnconfirmedPrivateTransferRequest request = new UnconfirmedPrivateTransferRequest(vendorId, serviceNumber, serviceParameters);
         this.debugSend(deviceAddress, request);
         this.transport().sendUnconfirmedRequest(request, deviceAddress, BNetworkPriority.normal);
      }
   }

   public void timeSynch(BBacnetRecipient recipient) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("BacnetClientLayer.timeSynch(" + recipient + ")");
         }

         TimeSynchronizationRequest request = new TimeSynchronizationRequest(Clock.time(10));
         BBacnetAddress deviceAddress = null;
         if (recipient.isDevice()) {
            deviceAddress = DeviceRegistry.getDeviceAddress(recipient.getDevice());
         } else {
            deviceAddress = recipient.getAddress();
         }

         this.debugSend(deviceAddress, request);
         this.transport().sendUnconfirmedRequest(request, deviceAddress, BNetworkPriority.normal);
      }
   }

   public void whoHas(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         WhoHasRequest request = new WhoHasRequest(objectId);
         this.debugSend(deviceAddress, request);
         this.transport().sendUnconfirmedRequest(request, deviceAddress, BNetworkPriority.normal);
      }
   }

   public void whoHas(BBacnetAddress deviceAddress, BBacnetObjectIdentifier objectId, int lowLimit, int highLimit) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         WhoHasRequest request = new WhoHasRequest(lowLimit, highLimit, objectId);
         this.debugSend(deviceAddress, request);
         this.transport().sendUnconfirmedRequest(request, deviceAddress, BNetworkPriority.normal);
      }
   }

   public void whoHas(BBacnetAddress deviceAddress, String objectName, BCharacterSetEncoding charset) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("BacnetClientLayer.whoHas(" + objectName + ") to " + deviceAddress);
         }

         WhoHasRequest request = new WhoHasRequest(objectName, charset);
         this.debugSend(deviceAddress, request);
         this.transport().sendUnconfirmedRequest(request, deviceAddress, BNetworkPriority.normal);
      }
   }

   public void whoHas(BBacnetAddress deviceAddress, String objectName, BCharacterSetEncoding charset, int lowLimit, int highLimit) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         WhoHasRequest request = new WhoHasRequest(lowLimit, highLimit, objectName, charset);
         this.debugSend(deviceAddress, request);
         this.transport().sendUnconfirmedRequest(request, deviceAddress, BNetworkPriority.normal);
      }
   }

   public void whoIs(BBacnetAddress deviceAddress) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("BacnetClientLayer.whoIs()");
         }

         WhoIsRequest request = new WhoIsRequest();
         this.debugSend(deviceAddress, request);
         this.transport().sendUnconfirmedRequest(request, deviceAddress, BNetworkPriority.normal);
      }
   }

   public void whoIs(BBacnetAddress deviceAddress, int lowLimit, int highLimit) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("BacnetClientLayer.whoIs(" + lowLimit + " - " + highLimit + ")");
         }

         WhoIsRequest request = new WhoIsRequest(lowLimit, highLimit);
         this.debugSend(deviceAddress, request);
         this.transport().sendUnconfirmedRequest(request, deviceAddress, BNetworkPriority.normal);
      }
   }

   public void utcTimeSynch(BBacnetRecipient recipient) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("BacnetClientLayer.utcTimeSynch(" + recipient + ")");
         }

         BAbsTime t = BAbsTime.make();
         BRelTime off = BRelTime.make(t.getTimeZoneOffset());
         UtcTimeSynchronizationRequest request = new UtcTimeSynchronizationRequest(t.subtract(off));
         BBacnetAddress deviceAddress = null;
         if (recipient.isDevice()) {
            deviceAddress = DeviceRegistry.getDeviceAddress(recipient.getDevice());
         } else {
            deviceAddress = recipient.getAddress();
         }

         this.debugSend(deviceAddress, request);
         this.transport().sendUnconfirmedRequest(request, deviceAddress, BNetworkPriority.normal);
      }
   }

   protected void verifyUnicastAddress(BBacnetAddress address) throws BacnetException {
      boolean local = address.equals(
         BBacnetAddress.LOCAL_BROADCAST_ADDRESS.getNetworkNumber(), BBacnetAddress.LOCAL_BROADCAST_ADDRESS.getMacAddress().getBytes()
      );
      boolean global = address.equals(
         BBacnetAddress.GLOBAL_BROADCAST_ADDRESS.getNetworkNumber(), BBacnetAddress.GLOBAL_BROADCAST_ADDRESS.getMacAddress().getBytes()
      );
      if (local || global) {
         throw new BacnetStackException("Cannot send writePropertyRequest to a broadcast address");
      }
   }

   public void deviceRestartNotification(BBacnetRecipient recipient) throws BacnetException {
      if (!this.stack().isCommInitiationEnabled()) {
         throw new BacnetStackException("Stack Disabled");
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("BacnetClientLayer.utcTimeSynch(" + recipient + ")");
         }

         BLocalBacnetDevice device = BBacnetNetwork.localDevice();
         long subscriberProcessId = 0L;
         BBacnetObjectIdentifier initiatingDeviceId = device.getObjectId();
         long timeRemaining = 0L;
         PropertyValue[] listOfValues = this.buildRestartPropertyValues(device);
         if (listOfValues != null) {
            CovNotificationParameters cnp = new CovNotificationParameters(
               subscriberProcessId, initiatingDeviceId, initiatingDeviceId, timeRemaining, listOfValues
            );
            this.unconfirmedCovNotification(recipient.getAddress(), cnp);
         }
      }
   }

   private PropertyValue[] buildRestartPropertyValues(BLocalBacnetDevice d) {
      try {
         PropertyValue systemStatus = this.getProperty(d, 112);
         PropertyValue timeOfDeviceRestart = this.getProperty(d, 203);
         PropertyValue lastRestartReason = this.getProperty(d, 196);
         return new PropertyValue[]{systemStatus, timeOfDeviceRestart, lastRestartReason};
      } catch (RejectException var5) {
         return null;
      }
   }

   private PropertyValue getProperty(BIBacnetExportObject object, int pid) throws RejectException {
      return new NBacnetPropertyValue(object.readProperty(new NBacnetPropertyReference(pid)));
   }

   private BBacnetTransportLayer transport() {
      return ((BBacnetStack)this.getParent()).getTransport();
   }

   public final BBacnetStack stack() {
      return (BBacnetStack)this.getParent();
   }

   public void addDebugListener(AppDebugListener listener) {
      this.debugListeners.add(listener);
   }

   public void removeDebugListener(AppDebugListener listener) {
      this.debugListeners.remove(listener);
   }

   private void debugSend(BBacnetAddress address, BacnetServicePrimitive request) {
      int size = this.debugListeners.size();

      for (int i = 0; i < size; i++) {
         this.debugListeners.get(i).send(address, request);
      }
   }

   private void debugReceive(BBacnetAddress address, BacnetServicePrimitive response) {
      int size = this.debugListeners.size();

      for (int i = 0; i < size; i++) {
         this.debugListeners.get(i).receive(address, response);
      }
   }
}
