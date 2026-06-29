package com.tridium.bacnet.stack.server;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.BacnetNotificationParameters;
import com.tridium.bacnet.asn.EventNotificationParameters;
import com.tridium.bacnet.asn.NAlarmSummary;
import com.tridium.bacnet.asn.NEnrollmentSummary;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.asn.NEventSummary;
import com.tridium.bacnet.datatypes.BPriorityFilter;
import com.tridium.bacnet.enums.BAcknowledgmentFilter;
import com.tridium.bacnet.enums.BEventStateFilter;
import com.tridium.bacnet.history.BBacnetHistoryDeviceExt;
import com.tridium.bacnet.history.BBacnetHistoryImport;
import com.tridium.bacnet.services.BacnetServicePrimitive;
import com.tridium.bacnet.services.BacnetSimpleAck;
import com.tridium.bacnet.services.confirmed.AcknowledgeAlarmRequest;
import com.tridium.bacnet.services.confirmed.ConfirmedEventNotificationRequest;
import com.tridium.bacnet.services.confirmed.GetAlarmSummaryAck;
import com.tridium.bacnet.services.confirmed.GetAlarmSummaryRequest;
import com.tridium.bacnet.services.confirmed.GetEnrollmentSummaryAck;
import com.tridium.bacnet.services.confirmed.GetEnrollmentSummaryRequest;
import com.tridium.bacnet.services.confirmed.GetEventInformationAck;
import com.tridium.bacnet.services.confirmed.GetEventInformationRequest;
import com.tridium.bacnet.services.error.SimpleError;
import com.tridium.bacnet.services.unconfirmed.UnconfirmedEventNotificationRequest;
import com.tridium.bacnet.stack.BBacnetStack;
import com.tridium.bacnet.stack.DeviceRegistry;
import com.tridium.bacnet.stack.client.AckAlarmRequest;
import com.tridium.bacnet.stack.network.BBacnetNetworkLayer;
import com.tridium.bacnet.stack.network.BNetworkPort;
import com.tridium.bacnet.stack.transport.BBacnetTransportLayer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.alarm.AlarmDbConnection;
import javax.baja.alarm.BAckState;
import javax.baja.alarm.BAlarmDatabase;
import javax.baja.alarm.BAlarmRecord;
import javax.baja.alarm.BAlarmService;
import javax.baja.alarm.BAlarmTransitionBits;
import javax.baja.alarm.BIAlarmSource;
import javax.baja.alarm.BSourceState;
import javax.baja.alarm.ext.BAlarmSourceExt;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetAlarmConst;
import javax.baja.bacnet.BacnetConfirmedServiceChoice;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.BacnetUnconfirmedServiceChoice;
import javax.baja.bacnet.alarm.BBacnetAlarmDeviceExt;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetDestination;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetRecipient;
import javax.baja.bacnet.datatypes.BBacnetRecipientProcess;
import javax.baja.bacnet.datatypes.BBacnetTimeStamp;
import javax.baja.bacnet.enums.BBacnetEventState;
import javax.baja.bacnet.enums.BBacnetNotifyType;
import javax.baja.bacnet.export.BBacnetEventSource;
import javax.baja.bacnet.export.BBacnetNotificationClassDescriptor;
import javax.baja.bacnet.export.BIBacnetExportObject;
import javax.baja.bacnet.export.BLocalBacnetDevice;
import javax.baja.bacnet.io.BBacnetComm;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.EventNotificationListener;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.control.BControlPoint;
import javax.baja.data.BIDataValue;
import javax.baja.driver.alarm.BAlarmDeviceExt;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BEnum;
import javax.baja.sys.BFacets;
import javax.baja.sys.BInteger;
import javax.baja.sys.BObject;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Cursor;
import javax.baja.sys.NoSuchSlotException;
import javax.baja.sys.Property;
import javax.baja.sys.ServiceNotFoundException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BFormat;
import javax.baja.util.BUuid;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "silenceSupported",
      type = "boolean",
      defaultValue = "false",
      flags = 1
   ), @NiagaraProperty(
      name = "eventSummaryAlarmClass",
      type = "String",
      defaultValue = "defaultAlarmClass",
      facets = {@Facet(
         name = "BFacets.FIELD_EDITOR",
         value = "\"alarm:AlarmClassFE\""
      ), @Facet(
         name = "BFacets.UX_FIELD_EDITOR",
         value = "\"alarm:AlarmClassEditor\""
      )}
   ), @NiagaraProperty(
      name = "eventSummaryProcessId",
      type = "int",
      defaultValue = "0",
      facets = {@Facet("BFacets.makeInt(0,Integer.MAX_VALUE)")}
   ), @NiagaraProperty(
      name = "toOffnormalBuffer",
      type = "BHashedEventBuffer",
      defaultValue = "new BHashedEventBuffer()",
      flags = 4
   ), @NiagaraProperty(
      name = "toFaultBuffer",
      type = "BHashedEventBuffer",
      defaultValue = "new BHashedEventBuffer()",
      flags = 4
   ), @NiagaraProperty(
      name = "toNormalBuffer",
      type = "BHashedEventBuffer",
      defaultValue = "new BHashedEventBuffer()",
      flags = 4
   ), @NiagaraProperty(
      name = "eventSummaries",
      type = "BComponent",
      defaultValue = "new BComponent()",
      flags = 4
   ), @NiagaraProperty(
      name = "alarmSourceName",
      type = "BFormat",
      defaultValue = "BFormat.make(\"%name%\")"
   )})
@NiagaraActions({@NiagaraAction(
      name = "ackAlarm",
      parameterType = "BAlarmRecord",
      defaultValue = "new BAlarmRecord()",
      returnType = "BBoolean",
      flags = 5
   ), @NiagaraAction(
      name = "dumpBuffers",
      flags = 4
   ), @NiagaraAction(
      name = "clearBuffers"
   )})
public class BEventHandler
   extends BComponent
   implements ServiceHandler,
   BIAlarmSource,
   BacnetConfirmedServiceChoice,
   BacnetUnconfirmedServiceChoice,
   BacnetAlarmConst,
   BacnetConst {
   public static final Property silenceSupported = newProperty(1, false, null);
   public static final Property eventSummaryAlarmClass = newProperty(
      0, "defaultAlarmClass", BFacets.make(BFacets.make("fieldEditor", "alarm:AlarmClassFE"), BFacets.make("uxFieldEditor", "alarm:AlarmClassEditor"))
   );
   public static final Property eventSummaryProcessId = newProperty(0, 0, BFacets.makeInt(0, Integer.MAX_VALUE));
   public static final Property toOffnormalBuffer = newProperty(4, new BHashedEventBuffer(), null);
   public static final Property toFaultBuffer = newProperty(4, new BHashedEventBuffer(), null);
   public static final Property toNormalBuffer = newProperty(4, new BHashedEventBuffer(), null);
   public static final Property eventSummaries = newProperty(4, new BComponent(), null);
   public static final Property alarmSourceName = newProperty(0, BFormat.make("%name%"), null);
   public static final Action ackAlarm = newAction(5, new BAlarmRecord(), null);
   public static final Action dumpBuffers = newAction(4, null);
   public static final Action clearBuffers = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BEventHandler.class);
   private BBacnetTransportLayer transportLayer;
   private static final Logger logger = Logger.getLogger("bacnet.server");
   private ArrayList<EventNotificationListener> confirmedEventListeners = new ArrayList<>();
   private ArrayList<EventNotificationListener> unconfirmedEventListeners = new ArrayList<>();

   public boolean getSilenceSupported() {
      return this.getBoolean(silenceSupported);
   }

   public void setSilenceSupported(boolean v) {
      this.setBoolean(silenceSupported, v, null);
   }

   public String getEventSummaryAlarmClass() {
      return this.getString(eventSummaryAlarmClass);
   }

   public void setEventSummaryAlarmClass(String v) {
      this.setString(eventSummaryAlarmClass, v, null);
   }

   public int getEventSummaryProcessId() {
      return this.getInt(eventSummaryProcessId);
   }

   public void setEventSummaryProcessId(int v) {
      this.setInt(eventSummaryProcessId, v, null);
   }

   public BHashedEventBuffer getToOffnormalBuffer() {
      return (BHashedEventBuffer)this.get(toOffnormalBuffer);
   }

   public void setToOffnormalBuffer(BHashedEventBuffer v) {
      this.set(toOffnormalBuffer, v, null);
   }

   public BHashedEventBuffer getToFaultBuffer() {
      return (BHashedEventBuffer)this.get(toFaultBuffer);
   }

   public void setToFaultBuffer(BHashedEventBuffer v) {
      this.set(toFaultBuffer, v, null);
   }

   public BHashedEventBuffer getToNormalBuffer() {
      return (BHashedEventBuffer)this.get(toNormalBuffer);
   }

   public void setToNormalBuffer(BHashedEventBuffer v) {
      this.set(toNormalBuffer, v, null);
   }

   public BComponent getEventSummaries() {
      return (BComponent)this.get(eventSummaries);
   }

   public void setEventSummaries(BComponent v) {
      this.set(eventSummaries, v, null);
   }

   public BFormat getAlarmSourceName() {
      return (BFormat)this.get(alarmSourceName);
   }

   public void setAlarmSourceName(BFormat v) {
      this.set(alarmSourceName, v, null);
   }

   public BBoolean ackAlarm(BAlarmRecord parameter) {
      return (BBoolean)this.invoke(ackAlarm, parameter, null);
   }

   public void dumpBuffers() {
      this.invoke(dumpBuffers, null, null);
   }

   public void clearBuffers() {
      this.invoke(clearBuffers, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBoolean doAckAlarm(BAlarmRecord ackRequest) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("BEventHandler.doAckAlarm():" + ackRequest);
      }

      if (this.checkRecordByTimestamp(ackRequest)) {
         AckAlarmRequest ackReq = new AckAlarmRequest(ackRequest);
         BBacnetNetwork.bacnet().postAsync(ackReq);
         return BBoolean.TRUE;
      } else {
         logger.fine("Stale ack received from Alarm Service!");

         try {
            BAlarmService as = (BAlarmService)Sys.getService(BAlarmService.TYPE);
            ackRequest.ackAlarm();
            ackRequest.setAckState(BAckState.acked);
            as.routeAlarm(ackRequest);
         } catch (ServiceNotFoundException var3) {
            logger.log(Level.SEVERE, "BEventHandler.doAckAlarm:Unable to find Alarm Service!", (Throwable)var3);
         }

         return BBoolean.FALSE;
      }
   }

   public void doDumpBuffers() {
   }

   public void doClearBuffers() {
      this.getToOffnormalBuffer().removeAll();
      this.getToFaultBuffer().removeAll();
      this.getToNormalBuffer().removeAll();
      this.getEventSummaries().removeAll();
   }

   @Override
   public BacnetServicePrimitive receiveRequest(int serviceChoice, BacnetServicePrimitive request, BBacnetAddress sourceAddress) {
      BacnetServicePrimitive response = null;

      try {
         if (request.getServiceType() == 0) {
            switch (serviceChoice) {
               case 0:
                  response = this.processAcknowledgeAlarmRequest((AcknowledgeAlarmRequest)request, sourceAddress);
                  break;
               case 2:
                  response = this.processConfirmedEventNotificationRequest((ConfirmedEventNotificationRequest)request);
                  this.routeToConfirmedListeners((ConfirmedEventNotificationRequest)request, sourceAddress);
                  break;
               case 3:
                  response = this.processGetAlarmSummaryRequest((GetAlarmSummaryRequest)request);
                  break;
               case 4:
                  response = this.processGetEnrollmentSummaryRequest((GetEnrollmentSummaryRequest)request);
                  break;
               case 29:
                  response = this.processGetEventInformationRequest((GetEventInformationRequest)request);
                  break;
               default:
                  logger.info("BEventHandler.receiveRequest:Unknown request! " + request);
            }
         } else {
            switch (serviceChoice) {
               case 3:
                  this.processUnconfirmedEventNotificationRequest((UnconfirmedEventNotificationRequest)request);
                  this.routeToUnconfirmedListeners((UnconfirmedEventNotificationRequest)request, sourceAddress);
                  break;
               default:
                  logger.info("BEventHandler.receiveRequest:Unknown request! " + request);
            }
         }
      } catch (Exception var6) {
         logger.log(Level.SEVERE, "Server error in event handler: " + var6 + " processing request " + request + " from " + sourceAddress, (Throwable)var6);
      }

      return response;
   }

   protected BacnetServicePrimitive processAcknowledgeAlarmRequest(AcknowledgeAlarmRequest request, BBacnetAddress sourceAddress) {
      boolean logTrace = logger.isLoggable(Level.FINE);
      if (logTrace) {
         logger.fine("BEventHandler: AcknowledgeAlarmRequest received: " + request);
      }

      BLocalBacnetDevice localDevice = BBacnetNetwork.localDevice();
      ErrorType err = null;

      try {
         BAlarmService as = (BAlarmService)Sys.getService(BAlarmService.TYPE);
         BBacnetObjectIdentifier deviceId = localDevice.getObjectId();
         BBacnetObjectIdentifier objectId = request.getEventObjectId();
         BIBacnetExportObject export = localDevice.lookupBacnetObject(objectId);
         if (export == null) {
            return new SimpleError(0, new NErrorType(1, 31));
         } else if (!(export instanceof BBacnetEventSource)) {
            return new SimpleError(0, new NErrorType(1, 74));
         } else {
            BEnum stateAcked = request.getEventStateAcknowledged();
            BAlarmRecord record = this.getRecordFromEventBuffer(stateAcked.getOrdinal(), deviceId, objectId, 0L, false);
            if (logTrace) {
               logger.fine("\nstateAcked:" + stateAcked + ", ordinal=" + stateAcked.getOrdinal());
            }

            err = this.validateAcknowledgement(request, record, as, sourceAddress, (BBacnetEventSource)export);
            if (err != null) {
               return new SimpleError(0, err);
            } else {
               if (logTrace) {
                  logger.fine(
                     "\n\nacknowledgement validated (but not yet acked!) record="
                        + record
                        + "\nalarmData="
                        + record.getAlarmData()
                        + "\nuuid:"
                        + record.getUuid()
                        + "\n"
                  );
               }

               record.setUser(request.getAcknowledgementSource());
               record.setAckState(BAckState.ackPending);
               int acksReq = 0;
               BInteger acksReqFacet = (BInteger)record.getAlarmFacet("bacnetAcksRequired");
               if (acksReqFacet != null) {
                  acksReq = acksReqFacet.getInt();
               }

               if (BBacnetEventState.isOffnormal(stateAcked)) {
                  record.addAlarmFacet("offnormalAcked", BBoolean.TRUE);
                  acksReq &= -5;
               } else if (BBacnetEventState.isFault(stateAcked)) {
                  acksReq &= -3;
               } else if (BBacnetEventState.isNormal(stateAcked)) {
                  acksReq &= -2;
               }

               if (logTrace) {
                  logger.fine("bacnetAcksRequired=" + acksReq);
               }

               record.addAlarmFacet("bacnetAcksRequired", BInteger.make(acksReq));
               record.addAlarmFacet("stateAcked", BInteger.make(stateAcked.getOrdinal()));
               if (acksReq == 0) {
                  if (logTrace) {
                     logger.fine(
                        "BEventHandler.processAckAlarmReq: no more acks req'd - ackAlarm(): user=" + record.getUser() + " stateAcked=" + stateAcked + "..."
                     );
                  }

                  as.ackAlarm(record);
               } else {
                  if (logTrace) {
                     logger.fine(
                        "\nBEventHandler.processAckAlarmReq: more acks req'd - routeAlarm(): user="
                           + record.getUser()
                           + " stateAcked="
                           + stateAcked
                           + "; adjusting ackedTransitions..."
                     );
                  }

                  BObject alarmSource = record.getSource().get(0).get(this);
                  BAlarmSourceExt ext = (BAlarmSourceExt)alarmSource;
                  int ibits = ext.getAckedTransitions().getBits();
                  ibits |= getAlarmTransitionBit(request.getEventStateAcknowledged());
                  ext.setAckedTransitions(BAlarmTransitionBits.make(ibits));
                  AlarmDbConnection conn = as.getAlarmDb().getDbConnection(null);
                  Throwable var18 = null;

                  try {
                     conn.update(record);
                  } catch (Throwable var29) {
                     var18 = var29;
                     throw var29;
                  } finally {
                     if (conn != null) {
                        if (var18 != null) {
                           try {
                              conn.close();
                           } catch (Throwable var28) {
                              var18.addSuppressed(var28);
                           }
                        } else {
                           conn.close();
                        }
                     }
                  }

                  as.routeAlarm(record);
               }

               BAlarmRecord[] others = this.getOtherUnackeds(record);

               for (int i = 0; i < others.length; i++) {
                  this.ackNiagara(others[i], stateAcked);
                  as.routeAlarm(others[i]);
               }

               return new BacnetSimpleAck(0);
            }
         }
      } catch (ServiceNotFoundException var31) {
         logger.log(Level.SEVERE, "BEventHandler.processAcknowledgeAlarmRequest:Unable to find Alarm Service!", (Throwable)var31);
         return new SimpleError(0, new NErrorType(3, 0));
      } catch (Exception var32) {
         logger.log(Level.SEVERE, "BEventHandler.processAcknowledgeAlarmRequest:Exception occurred!", (Throwable)var32);
         return new SimpleError(0, new NErrorType(3, 0));
      }
   }

   protected BacnetServicePrimitive processConfirmedEventNotificationRequest(ConfirmedEventNotificationRequest request) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("BEventHandler: ConfirmedEventNotificationRequest received: " + request);
      }

      NErrorType err = null;

      try {
         err = this.processEvent(request.getEventNotificationParameters(), true);
      } catch (ServiceNotFoundException var4) {
         logger.severe("Can't find Alarm Service!");
         err = new NErrorType(0, 25);
      } catch (Exception var5) {
         logger.log(Level.SEVERE, "Error processing BACnet Event Notification:" + var5, (Throwable)var5);
         err = new NErrorType(0, 25);
      }

      return (BacnetServicePrimitive)(err == null ? new BacnetSimpleAck(2) : new SimpleError(2, err));
   }

   protected void processUnconfirmedEventNotificationRequest(UnconfirmedEventNotificationRequest request) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("BEventHandler: UnconfirmedEventNotificationRequest received: " + request);
      }

      try {
         this.processEvent(request.getEventNotificationParameters(), false);
      } catch (ServiceNotFoundException var3) {
         logger.severe("Can't find Alarm Service!");
      } catch (Exception var4) {
         logger.log(Level.SEVERE, "Error processing BACnet Event Notification:" + var4, (Throwable)var4);
      }
   }

   protected BacnetServicePrimitive processGetAlarmSummaryRequest(GetAlarmSummaryRequest request) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("BEventHandler: GetAlarmSummaryRequest received: " + request);
      }

      BLocalBacnetDevice local = BBacnetNetwork.localDevice();
      Vector<NAlarmSummary> listOfAlarmSummaries = new Vector<>();
      BBacnetObjectIdentifier[] ids = (BBacnetObjectIdentifier[])this.getEventSummaries().getChildren(BBacnetObjectIdentifier.class);
      int len = ids.length;
      int ndx = 0;

      for (int i = ndx; i < len; i++) {
         BBacnetObjectIdentifier id = ids[i];
         BBacnetEventSource src = (BBacnetEventSource)local.lookupBacnetObject(id);
         if (src != null && src.getEventDetectionEnable()) {
            if (src.getNotifyType() == BBacnetNotifyType.alarm) {
               BEnum eventState = src.getEventState();
               if (eventState != null && eventState.getOrdinal() != 0) {
                  NAlarmSummary asum = new NAlarmSummary(id, eventState, src.getAckedTransitions());
                  listOfAlarmSummaries.add(asum);
               }
            }
         } else {
            this.getEventSummaries().remove(id.toString(nameContext));
         }
      }

      return new GetAlarmSummaryAck(listOfAlarmSummaries);
   }

   protected BacnetServicePrimitive processGetEnrollmentSummaryRequest(GetEnrollmentSummaryRequest request) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("BEventHandler: GetEnrollmentSummaryRequest received: " + request);
      }

      BBacnetExportTable exports = (BBacnetExportTable)BBacnetNetwork.localDevice().getExportTable();
      BIBacnetExportObject[] entries = exports.getExportedObjects(BBacnetEventSource.TYPE);
      BBacnetEventSource eventSource = null;
      Vector<NEnrollmentSummary> v = new Vector<>();
      BAcknowledgmentFilter ackFilter = request.getAcknowledgmentFilter();
      BBacnetRecipientProcess enrollmentFilter = request.getEnrollmentFilter();
      BEventStateFilter eventStateFilter = request.getEventStateFilter();
      BEnum eventTypeFilter = request.getEventTypeFilter();
      BPriorityFilter priorityFilter = request.getPriorityFilter();
      long notificationClassFilter = request.getNotificationClassFilter();
      if (entries != null) {
         for (int i = 0; i < entries.length; i++) {
            eventSource = (BBacnetEventSource)entries[i];
            if (eventSource != null
               && eventSource.getEventDetectionEnable()
               && eventSource.isEventInitiationEnabled()
               && ackFilter.filter(eventSource.getAckedTransitions())) {
               if (request.isEnrollmentFilter()) {
                  BBacnetNotificationClassDescriptor nc = eventSource.getNotificationClass();
                  BBacnetDestination[] recips = nc.getRecipientList();
                  boolean include = false;

                  for (int j = 0; j < recips.length; j++) {
                     if (recips[j].getProcessIdentifier().equals(enrollmentFilter.getProcessIdentifier())
                        && recips[j].getRecipient().equivalent(enrollmentFilter.getRecipient())) {
                        include = true;
                        break;
                     }
                  }

                  if (!include) {
                     continue;
                  }
               }

               if ((!request.isEventStateFilter() || eventStateFilter.filter(eventSource.getEventState()))
                  && (!request.isEventTypeFilter() || eventTypeFilter.getOrdinal() == eventSource.getEventType().getOrdinal())) {
                  int transition = getNiagaraSourceState(eventSource.getEventState().getOrdinal());
                  int priority = getEventPriority(transition, eventSource);
                  if ((!request.isPriorityFilter() || priorityFilter.filter(priority))
                     && (!request.isNotificationClassFilter() || eventSource.getNotificationClass().getNotificationClass() == notificationClassFilter)) {
                     v.add(
                        new NEnrollmentSummary(
                           eventSource.getObjectId(),
                           eventSource.getEventType(),
                           eventSource.getEventState(),
                           priority,
                           request.isNotificationClassFilter() ? eventSource.getNotificationClass().getNotificationClass() : -1L
                        )
                     );
                  }
               }
            }
         }
      }

      return new GetEnrollmentSummaryAck(v);
   }

   protected BacnetServicePrimitive processGetEventInformationRequest(GetEventInformationRequest request) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("BEventHandler: GetEventInformationRequest received: " + request);
      }

      int maxResponseLength = request.getMaxDataLength();
      maxResponseLength -= 4;
      int lastObjectId = -1;
      BBacnetObjectIdentifier lastReceivedObjectId = request.getLastReceivedObjectId();
      if (lastReceivedObjectId != null) {
         lastObjectId = lastReceivedObjectId.hashCode();
      }

      Vector<NEventSummary> listOfEventSummaries = new Vector<>();
      int responseDataSize = 0;
      boolean moreEvents = false;
      BLocalBacnetDevice local = BBacnetNetwork.localDevice();
      BBacnetObjectIdentifier[] ids = (BBacnetObjectIdentifier[])this.getEventSummaries().getChildren(BBacnetObjectIdentifier.class);
      int len = ids.length;
      int ndx = 0;
      if (lastReceivedObjectId != null) {
         while (ndx < len && ids[ndx++].hashCode() != lastObjectId) {
         }
      }

      for (int i = ndx; i < len; i++) {
         BBacnetObjectIdentifier id = ids[i];
         BBacnetEventSource src = (BBacnetEventSource)local.lookupBacnetObject(id);
         boolean toBeRemoved = src == null
            || src.getEventState() == null
            || !src.getEventDetectionEnable()
            || src.getEventState() == BBacnetEventState.normal && src.getAckedTransitions().equals(BBacnetBitString.make(new boolean[]{true, true, true}));
         if (toBeRemoved) {
            this.getEventSummaries().remove(id.toString(nameContext));
         } else {
            NEventSummary esum = new NEventSummary(
               id,
               src.getEventState(),
               src.getAckedTransitions(),
               src.getEventTimeStamps(),
               src.getNotifyType(),
               src.getEventEnable(),
               src.getEventPriorities()
            );
            responseDataSize += esum.getEncodedSize();
            if (maxResponseLength > 0 && responseDataSize > maxResponseLength) {
               moreEvents = true;
               break;
            }

            listOfEventSummaries.add(esum);
         }
      }

      return new GetEventInformationAck(listOfEventSummaries, moreEvents);
   }

   public void putRecordToEventBuffer(
      int eventStateOrdinal, BBacnetObjectIdentifier deviceId, BBacnetObjectIdentifier objectId, long processId, BAlarmRecord alarmRecord
   ) {
      BString msgText = BString.make(alarmRecord.getFormattedAlarmDataValue("msgText", facetsContext));
      switch (eventStateOrdinal) {
         case 0:
            alarmRecord.addAlarmFacet("toNormalMsgText", msgText);
            break;
         case 1:
            alarmRecord.addAlarmFacet("toFaultMsgText", msgText);
            break;
         case 2:
         case 3:
         case 4:
         case 5:
         default:
            alarmRecord.addAlarmFacet("toOffNormalMsgText", msgText);
      }

      BHashedEventBuffer buffer = this.getEventBuffer(eventStateOrdinal);
      buffer.putRecord(deviceId, objectId, processId, alarmRecord);
   }

   public void removeAllRecordFromEventBuffer(BBacnetObjectIdentifier deviceId, BBacnetObjectIdentifier objectId, long processId) {
      BHashedEventBuffer buffer = this.getEventBuffer(1);
      buffer.removeRecord(deviceId, objectId, processId);
      buffer = this.getEventBuffer(2);
      buffer.removeRecord(deviceId, objectId, processId);
      buffer = this.getEventBuffer(0);
      buffer.removeRecord(deviceId, objectId, processId);
   }

   public BAlarmRecord getRecordFromEventBuffer(
      int eventStateOrdinal, BBacnetObjectIdentifier deviceId, BBacnetObjectIdentifier objectId, long processId, boolean removeFromBuffer
   ) {
      return this.getRecordFromEventBuffer(eventStateOrdinal, deviceId, objectId, processId, null, removeFromBuffer);
   }

   public BAlarmRecord getRecordFromEventBuffer(
      int eventStateOrdinal, BBacnetObjectIdentifier deviceId, BBacnetObjectIdentifier objectId, long processId, BUuid uuid, boolean removeFromBuffer
   ) {
      BHashedEventBuffer buffer = this.getEventBuffer(eventStateOrdinal);
      return buffer.getRecord(deviceId, objectId, processId, uuid, removeFromBuffer);
   }

   public boolean checkRecordByTimestamp(BAlarmRecord ackRequest) {
      BHashedEventBuffer buffer = this.getEventBuffer(ackRequest.getSourceState());
      return buffer.checkRecord(ackRequest);
   }

   public void addEventSummary(BBacnetObjectIdentifier objectId) {
      String name = objectId.toString(nameContext);
      if (this.getEventSummaries().get(name) == null) {
         this.getEventSummaries().add(name, objectId, 1);
      }
   }

   public void removeEventSummary(BBacnetObjectIdentifier objectId) {
      try {
         this.getEventSummaries().remove(objectId.toString(nameContext));
      } catch (NoSuchSlotException var3) {
      }
   }

   public void processEventSummary(NEventSummary eventSummary, BBacnetObjectIdentifier deviceId) {
      try {
         BBacnetNetwork bacnet = BBacnetNetwork.bacnet();
         BBacnetDevice device = bacnet.doLookupDeviceById(deviceId);
         BAlarmService as = (BAlarmService)Sys.getService(BAlarmService.TYPE);
         BBacnetObjectIdentifier objectId = eventSummary.getObjectId();
         BComponent localSource = this.findLocalAlarmSource(device, eventSummary);
         int eventStateOrdinal = eventSummary.getEventState().getOrdinal();
         BBacnetBitString ackedTransistions = eventSummary.getAcknowledgedTransitions();

         for (int i = 0; i < 3; i++) {
            long processId = this.getProcessIdForEvent(deviceId, eventSummary);
            int transitionEventState = this.getEventStateFromTransition(i);
            BAlarmRecord dbRecord = null;
            if (transitionEventState == 0) {
               dbRecord = this.getRecordFromEventBuffer(1, deviceId, objectId, processId, false);
               if (dbRecord == null) {
                  dbRecord = this.getRecordFromEventBuffer(2, deviceId, objectId, processId, false);
               }
            } else {
               dbRecord = this.getRecordFromEventBuffer(transitionEventState, deviceId, objectId, processId, false);
            }

            if (this.shouldBuildAlarm(eventSummary, i, dbRecord)) {
               BFacets alarmData = this.buildAlarmData(eventSummary, deviceId, i);
               BAlarmRecord record = null;
               if (transitionEventState == 0 && dbRecord != null && !dbRecord.isNormal()) {
                  record = dbRecord;
                  BEnum fromState = dbRecord.getSourceState();
                  BBacnetTimeStamp time = eventSummary.getEventTimeStamps()[i];
                  dbRecord.addAlarmFacet("fromState", BString.make(fromState.getTag()));
                  dbRecord.addAlarmFacet("msgText", BString.make("NORMAL"));
                  dbRecord.setNormalTime(time.toBAbsTime());
               } else {
                  record = new BAlarmRecord(device.getAlarms(), device.getAlarms().getAlarmClass(), alarmData);
               }

               BSourceState alarmState;
               switch (eventStateOrdinal) {
                  case 0:
                  default:
                     alarmState = BSourceState.normal;
                     break;
                  case 1:
                     alarmState = BSourceState.fault;
                     break;
                  case 2:
                  case 3:
                  case 4:
                  case 5:
                     alarmState = BSourceState.offnormal;
               }

               record.setSourceState(alarmState);
               record.addAlarmFacet("toState", BString.make(alarmState.getTag()));
               BBacnetTimeStamp time = eventSummary.getEventTimeStamps()[i];
               record.setTimestamp(time.toBAbsTime());
               record.setPriority(eventSummary.getEventPriorities()[i]);
               if (!ackedTransistions.getBit(i)) {
                  record.addAlarmFacet("bacnetAcksRequired", BString.make(alarmState.getTag() + "@" + time.toString(facetsContext)));
               }

               if (transitionEventState == 0 && dbRecord != null) {
                  record.addAlarmFacet("sourceName", BString.make(dbRecord.getSource().encodeToString()));
               } else {
                  this.addLocalSource(localSource, record);
               }

               if (logger.isLoggable(Level.FINE)) {
                  logger.fine("Routing alarm from GetEventInfo:" + record);
               }

               this.putRecordToEventBuffer(eventStateOrdinal, deviceId, objectId, processId, record);
               as.routeAlarm(record);
            }
         }
      } catch (ServiceNotFoundException var20) {
         logger.log(Level.SEVERE, "BEventHandler.processEventSummary:Unable to find Alarm Service!", (Throwable)var20);
      }
   }

   public void setTransportLayer(BBacnetTransportLayer transportLayer) {
      this.transportLayer = transportLayer;
   }

   @Override
   public BBacnetTransportLayer getTransportLayer() {
      return this.transportLayer;
   }

   protected NErrorType processEvent(EventNotificationParameters enp, boolean confirmed) {
      BAlarmService as = (BAlarmService)Sys.getService(BAlarmService.TYPE);
      BAlarmDatabase db = as.getAlarmDb();
      BAlarmRecord record = null;
      BBacnetObjectIdentifier deviceId = enp.getInitiatingDeviceId();
      BBacnetObjectIdentifier objectId = enp.getEventObjectId();
      long processId = enp.getProcessId();
      BBacnetNetwork bacnet = BBacnetNetwork.bacnet();
      BBacnetDevice device = bacnet.doLookupDeviceById(deviceId);
      if (device == null) {
         logger.info("Bacnet alarm received from unknown device!  Ignoring...");
         return new NErrorType(1, 31);
      } else {
         device.getAlarms().setLastReceivedTime(BAbsTime.make());
         BControlPoint point;
         if (objectId.getObjectType() == 9) {
            BControlPoint[] eventEnrollmentPoints = device.getPoints().findPoints(objectId);
            point = eventEnrollmentPoints.length > 0 ? eventEnrollmentPoints[0] : null;
         } else {
            point = (BControlPoint)device.lookupBacnetObject(objectId, 85, -1, "point");
         }

         BBacnetNotifyType notifyType = enp.getNotifyType();
         if (notifyType.getOrdinal() == 2) {
            record = this.processAckNotification(enp, device.getAlarms(), db);
         } else {
            record = this.processEventNotification(enp, device.getAlarms(), point);
            record.addAlarmFacet("confirmed", BBoolean.make(confirmed));
            this.putRecordToEventBuffer(enp.getToState().getOrdinal(), deviceId, objectId, processId, record);
         }

         if (record != null) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine("processEvent...record:" + record + "\n data=" + record.getAlarmData());
            }

            this.addSourceAndRoute(record, point, device.getAlarms());
            this.checkForBufferReady(enp.getEventType().getOrdinal(), device, objectId);
            return null;
         } else {
            logger.info("Unable to generate or match alarm for routing: " + enp + "\n toState=" + enp.getToState() + ", notifyType=" + notifyType);
            return new NErrorType(5, 0);
         }
      }
   }

   protected BAlarmRecord processAckNotification(EventNotificationParameters params, BAlarmDeviceExt alarmDeviceExt, BAlarmDatabase db) {
      BAlarmRecord record = this.getRecordFromEventBuffer(
         params.getToState().getOrdinal(), params.getInitiatingDeviceId(), params.getEventObjectId(), params.getProcessId(), false
      );
      if (record != null) {
         this.ackNiagara(record, params.getToState());
      }

      BAlarmRecord[] otherUnacked = this.getOtherUnackeds(record);

      for (int i = 0; i < otherUnacked.length; i++) {
         this.ackNiagara(otherUnacked[i], params.getToState());
         alarmDeviceExt.routeAlarm(otherUnacked[i]);
      }

      return record;
   }

   protected BAlarmRecord processEventNotification(EventNotificationParameters enp, BAlarmDeviceExt alarmDeviceExt, BControlPoint point) {
      BAlarmRecord record = null;
      BEnum toState = enp.getToState();
      if (BBacnetEventState.isNormal(toState)) {
         BEnum fromState = enp.getFromState();
         if (BBacnetEventState.isFault(fromState) || BBacnetEventState.isOffnormal(fromState)) {
            record = this.getRecordFromEventBuffer(fromState.getOrdinal(), enp.getInitiatingDeviceId(), enp.getEventObjectId(), enp.getProcessId(), false);
         }
      }

      if (record == null) {
         if (point != null) {
            record = new BAlarmRecord(point.getProxyExt(), alarmDeviceExt.getAlarmClass(), this.buildAlarmData(enp), BUuid.make());
         } else {
            record = new BAlarmRecord(alarmDeviceExt, alarmDeviceExt.getAlarmClass(), this.buildAlarmData(enp), BUuid.make());
         }

         record.setTimestamp(enp.getTimeStamp().toBAbsTime());
         record.setAckRequired(enp.getAckRequired());
      } else {
         if (BBacnetEventState.isNormal(toState)) {
            BAlarmRecord[] otherAlarms = this.getOtherNonNormals(record);
            this.updateAlarmsToNormal(otherAlarms, enp, point, alarmDeviceExt);
         }

         this.updateAlarm(record, enp);
      }

      this.setBacnetData(record, enp);
      return record;
   }

   protected long getProcessIdForEvent(BBacnetObjectIdentifier initiatingDeviceId, NEventSummary eventSummary) {
      long processId = 0L;

      try {
         BBacnetAddress addr = DeviceRegistry.getDeviceAddress(initiatingDeviceId);
         int nc = AsnUtil.fromAsnUnsignedInt(this.comm().readProperty(addr, eventSummary.getObjectId(), 17));
         byte[] b = this.comm().readProperty(addr, BBacnetObjectIdentifier.make(15, nc), 102);
         AsnInputStream in = new AsnInputStream(b);
         BBacnetDestination dest = new BBacnetDestination();

         while (in.available() > 0) {
            dest.readAsn(in);
            BBacnetRecipient r = dest.getRecipient();
            if (r.isAddress()) {
               int destNet = r.getAddress().getNetworkNumber();
               BBacnetNetworkLayer net = ((BBacnetStack)this.comm()).getNetwork();
               BNetworkPort port = net.getPortByNetwork(destNet);
               if (port != null) {
                  byte[] mac = port.getLink().getMacAddress();
                  if (r.getAddress().equals(destNet, mac)) {
                     processId = dest.getProcessIdentifier().getLong();
                     break;
                  }
               }
            } else if (r.getDevice().equals(BBacnetNetwork.localDevice().getObjectId())) {
               processId = dest.getProcessIdentifier().getLong();
               break;
            }
         }
      } catch (Exception var15) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(
               Level.FINE, "Cannot determine processId for Event Summary:" + eventSummary + ":" + var15 + ";\n -- Using local processId...", (Throwable)var15
            );
         }
      }

      return processId;
   }

   protected BComponent findLocalAlarmSource(BBacnetDevice device, NEventSummary eventSummary) {
      return (BControlPoint)device.lookupBacnetObject(eventSummary.getObjectId(), 85, -1, "point");
   }

   protected boolean shouldBuildAlarm(NEventSummary eventSummary, int eventTransitionIndex, BAlarmRecord dbRecord) {
      boolean enabled = eventSummary.getEventEnable().getBit(eventTransitionIndex);
      boolean acked = eventSummary.getAcknowledgedTransitions().getBit(eventTransitionIndex);
      if (enabled && !acked) {
         BBacnetTimeStamp esumTS = eventSummary.getEventTimeStamps()[eventTransitionIndex];
         if (this.isNull(esumTS)) {
            return false;
         } else if (dbRecord == null) {
            return true;
         } else {
            BAbsTime recordTS = dbRecord.getTimestamp();
            if (eventTransitionIndex == 2) {
               recordTS = dbRecord.getNormalTime();
            }

            return recordTS.equals(BAbsTime.DEFAULT) ? true : esumTS.toBAbsTime().isAfter(recordTS);
         }
      } else {
         return false;
      }
   }

   private boolean isNull(BBacnetTimeStamp ts) {
      switch (ts.getChoice()) {
         case 0:
            return ts.getTime().isAnyUnspecified();
         case 2:
            return ts.getDateTime().isAnyUnspecified();
         default:
            return false;
      }
   }

   private int getEventStateFromTransition(int eventTransition) {
      switch (eventTransition) {
         case 1:
            return 1;
         case 2:
            return 0;
         default:
            return 2;
      }
   }

   protected void addLocalSource(BComponent localSource, BAlarmRecord record) {
      if (localSource != null) {
         record.addAlarmFacet("sourceName", this.makeAlarmSourceName(localSource));
      }
   }

   protected final ErrorType validateAcknowledgement(
      AcknowledgeAlarmRequest ack, BAlarmRecord rec, BAlarmService as, BBacnetAddress sourceAddress, BBacnetEventSource export
   ) {
      BEnum stateAckedEn = ack.getEventStateAcknowledged();
      int stateAcked = stateAckedEn.getOrdinal();
      if (rec == null) {
         return new NErrorType(5, 73);
      } else {
         if (BBacnetEventState.isOffnormal(stateAckedEn) && stateAcked != 2) {
            int offnormalToState = 2;
            BInteger offTof = (BInteger)rec.getAlarmFacet("offnormalToState");
            if (offTof != null) {
               offnormalToState = offTof.getInt();
            }

            if (stateAcked != offnormalToState) {
               return new NErrorType(5, 73);
            }
         }

         return !timesMatch(ack.getTimeStamp(), getTimestamp(rec, stateAckedEn)) ? new NErrorType(5, 14) : null;
      }
   }

   protected static final BAbsTime getTimestamp(BAlarmRecord rec, BEnum stateAcked) {
      return BBacnetEventState.isNormal(stateAcked) ? rec.getNormalTime() : rec.getTimestamp();
   }

   protected static final boolean timesMatch(BBacnetTimeStamp ts, BAbsTime t) {
      if (ts.getChoice() != 2) {
         return false;
      } else {
         BAbsTime tst = ts.toBAbsTime();
         int scale = 10;
         long m1 = tst.getMillis() / scale;
         long m2 = t.getMillis() / scale;
         return m1 == m2;
      }
   }

   protected void checkForBufferReady(int eventType, BBacnetDevice device, BBacnetObjectIdentifier objectId) {
      if (eventType == 10) {
         BBacnetHistoryImport[] pts = ((BBacnetHistoryDeviceExt)device.getTrendLogs()).findImportDescriptors(objectId);
         if (pts != null) {
            for (int j = 0; j < pts.length; j++) {
               pts[j].setBufferReady(true);
               pts[j].execute();
               pts[j].setBufferReady(false);
            }
         }
      }
   }

   protected void updateAlarm(BAlarmRecord record, EventNotificationParameters enp) {
      String tsString = enp.getTimeStamp().toString(facetsContext);
      BEnum toState = enp.getToState();
      BEnum fromState = enp.getFromState();
      record.addAlarmFacet("toState", BString.make(toState.getTag()));
      record.addAlarmFacet("fromState", BString.make(fromState.getTag()));
      record.addAlarmFacet("BacnetTimestamp", BString.make(tsString));
      String s = enp.getMessageText();
      if (s != null && s.length() > 0) {
         record.addAlarmFacet("msgText", BString.make(s));
      }

      if (enp.getAckRequired()) {
         BString acksReq = (BString)record.getAlarmData().getFacet("bacnetAcksRequired");
         if (acksReq == null) {
            acksReq = BString.make(toState.getTag() + "@" + tsString);
         } else {
            String acksReqStr = acksReq.getString();
            StringTokenizer st = new StringTokenizer(acksReqStr, ";");
            StringBuilder newAcksReqStr = new StringBuilder();
            boolean found = false;

            while (st.hasMoreTokens()) {
               String state = st.nextToken();
               if (state.startsWith(toState.getTag())) {
                  state = state.substring(0, state.indexOf("@") + 1) + tsString;
                  found = true;
               }

               newAcksReqStr.append(state).append(';');
            }

            if (!found) {
               acksReq = BString.make(newAcksReqStr.append(toState.getTag()).append('@').append(tsString).toString());
            } else {
               acksReq = BString.make(newAcksReqStr.substring(0, newAcksReqStr.length() - 1));
            }
         }

         record.addAlarmFacet("bacnetAcksRequired", acksReq);
      } else if (enp.getToState() != BBacnetEventState.normal || record.getAckState() != BAckState.unacked) {
         record.setAckRequired(false);
      }
   }

   protected void setBacnetData(BAlarmRecord record, EventNotificationParameters enp) {
      BEnum toState = enp.getToState();
      switch (toState.getOrdinal()) {
         case 0:
         default:
            record.setNormalTime(enp.getTimeStamp().toBAbsTime());
            record.setSourceState(BSourceState.normal);
            break;
         case 1:
            record.setSourceState(BSourceState.fault);
            break;
         case 2:
         case 3:
         case 4:
         case 5:
            record.setSourceState(BSourceState.offnormal);
      }

      record.setPriority(enp.getPriority());
      if (enp.getAckRequired()) {
         record.setAckState(BAckState.unacked);
      }
   }

   private void ackNiagara(BAlarmRecord record, BEnum toState) {
      record.ackAlarm();
      record.setAckState(BAckState.acked);
      BObject acksReqFacet = record.getAlarmData().getFacet("bacnetAcksRequired");
      if (acksReqFacet != null) {
         if (acksReqFacet instanceof BString) {
            String acksReq = ((BString)acksReqFacet).getString();
            int start = acksReq.indexOf(toState.getTag());
            if (start >= 0) {
               int end = acksReq.indexOf(";", start);
               if (end < 0) {
                  end = acksReq.length();
               }

               acksReq = new StringBuilder(acksReq).delete(start, end).toString();
               if (acksReq.startsWith(";")) {
                  acksReq = acksReq.substring(1);
               }

               record.addAlarmFacet("bacnetAcksRequired", BString.make(acksReq));
            }
         } else if (acksReqFacet instanceof BInteger) {
            int acksReq = ((BInteger)acksReqFacet).getInt();
            if (BBacnetEventState.isOffnormal(toState)) {
               acksReq &= -5;
            }

            if (BBacnetEventState.isFault(toState)) {
               acksReq &= -3;
            }

            if (BBacnetEventState.isNormal(toState)) {
               acksReq &= -2;
            }

            record.addAlarmFacet("bacnetAcksRequired", BInteger.make(acksReq));
         }
      }
   }

   private BBacnetComm comm() {
      return BBacnetNetwork.bacnet().getBacnetComm();
   }

   static void dumpAlarm(BAlarmRecord r, boolean showFacets) {
      if (r != null) {
         System.out.println("\n" + r);
         if (showFacets) {
            System.out.println("      Facets:" + r.getAlarmData());
         }
      }
   }

   protected static int getAlarmTransitionBit(BEnum eventStateAcknowledged) {
      switch (eventStateAcknowledged.getOrdinal()) {
         case 0:
            return 4;
         case 1:
            return 2;
         case 2:
         case 3:
         case 4:
         case 5:
         default:
            return 1;
      }
   }

   private BAlarmRecord[] getOtherUnackeds(BAlarmRecord record) {
      Array<BAlarmRecord> a = new Array(BAlarmRecord.class);

      try {
         BAlarmService as = (BAlarmService)Sys.getService(BAlarmService.TYPE);
         BAlarmDatabase alarmDb = as.getAlarmDb();
         BObject source = record.getSource().get(0).get(this);
         if (!(source instanceof BBacnetAlarmDeviceExt)) {
            AlarmDbConnection conn = alarmDb.getDbConnection(null);
            Throwable var7 = null;

            try {
               Cursor<BAlarmRecord> c = conn.getAlarmsForSource(record.getSource());
               Throwable var9 = null;

               try {
                  BUuid uuid = record.getUuid();

                  while (c.next()) {
                     BAlarmRecord rec = (BAlarmRecord)((BAlarmRecord)c.get()).asComplex().newCopy();
                     if (!uuid.equals(rec.getUuid()) && rec.getAckState() != BAckState.acked && rec.getAlarmTransition().equals(record.getAlarmTransition())) {
                        a.add(rec);
                     }
                  }
               } catch (Throwable var35) {
                  var9 = var35;
                  throw var35;
               } finally {
                  if (c != null) {
                     if (var9 != null) {
                        try {
                           c.close();
                        } catch (Throwable var34) {
                           var9.addSuppressed(var34);
                        }
                     } else {
                        c.close();
                     }
                  }
               }
            } catch (Throwable var37) {
               var7 = var37;
               throw var37;
            } finally {
               if (conn != null) {
                  if (var7 != null) {
                     try {
                        conn.close();
                     } catch (Throwable var33) {
                        var7.addSuppressed(var33);
                     }
                  } else {
                     conn.close();
                  }
               }
            }
         }
      } catch (Exception var39) {
         logger.log(Level.SEVERE, "Exception occurred in getOtherUnackeds", (Throwable)var39);
      }

      return (BAlarmRecord[])a.trim();
   }

   private BAlarmRecord[] getOtherNonNormals(BAlarmRecord record) {
      Array<BAlarmRecord> a = new Array(BAlarmRecord.class);

      try {
         BAlarmService as = (BAlarmService)Sys.getService(BAlarmService.TYPE);
         BAlarmDatabase alarmDb = as.getAlarmDb();
         AlarmDbConnection conn = alarmDb.getDbConnection(null);
         Throwable var6 = null;

         try {
            Cursor<BAlarmRecord> c = conn.getAlarmsForSource(record.getSource());
            Throwable var8 = null;

            try {
               BUuid uuid = record.getUuid();

               while (c.next()) {
                  BAlarmRecord rec = (BAlarmRecord)((BAlarmRecord)c.get()).asComplex().newCopy();
                  if (!uuid.equals(rec.getUuid()) && !rec.isNormal()) {
                     a.add(rec);
                  }
               }
            } catch (Throwable var34) {
               var8 = var34;
               throw var34;
            } finally {
               if (c != null) {
                  if (var8 != null) {
                     try {
                        c.close();
                     } catch (Throwable var33) {
                        var8.addSuppressed(var33);
                     }
                  } else {
                     c.close();
                  }
               }
            }
         } catch (Throwable var36) {
            var6 = var36;
            throw var36;
         } finally {
            if (conn != null) {
               if (var6 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var32) {
                     var6.addSuppressed(var32);
                  }
               } else {
                  conn.close();
               }
            }
         }
      } catch (Exception var38) {
         logger.log(Level.SEVERE, "Exception occurred in getOtherNonNormals", (Throwable)var38);
      }

      return (BAlarmRecord[])a.trim();
   }

   private void updateAlarmsToNormal(BAlarmRecord[] records, EventNotificationParameters enp, BControlPoint point, BAlarmDeviceExt alarmDeviceExt) {
      for (int i = 0; i < records.length; i++) {
         this.updateAlarm(records[i], enp);
         this.setBacnetData(records[i], enp);
         this.addSourceAndRoute(records[i], point, alarmDeviceExt);
      }
   }

   private void addSourceAndRoute(BAlarmRecord record, BControlPoint point, BAlarmDeviceExt alarmDeviceExt) {
      if (point != null) {
         record.addAlarmFacet("sourceName", this.makeAlarmSourceName(point));
      }

      alarmDeviceExt.routeAlarm(record);
   }

   private BString makeAlarmSourceName(BComponent source) {
      if (source instanceof BControlPoint) {
         BValue proxyExtFormat = ((BControlPoint)source).getProxyExt().get("alarmSourceName");
         if (proxyExtFormat instanceof BFormat) {
            return BString.make(((BFormat)proxyExtFormat).format(source));
         }
      }

      return BString.make(this.getAlarmSourceName().format(source));
   }

   protected static final int getNiagaraSourceState(int toState) {
      switch (toState) {
         case 0:
            return 0;
         case 1:
            return 2;
         default:
            return 1;
      }
   }

   protected static final int getEventPriority(int transition, BBacnetEventSource eventSource) {
      int[] eventPriorities = eventSource.getEventPriorities();
      if (transition == 0) {
         return eventPriorities[2];
      } else {
         return transition == 2 ? eventPriorities[1] : eventPriorities[0];
      }
   }

   private BHashedEventBuffer getEventBuffer(int eventStateOrdinal) {
      switch (eventStateOrdinal) {
         case 0:
            return this.getToNormalBuffer();
         case 1:
            return this.getToFaultBuffer();
         default:
            return this.getToOffnormalBuffer();
      }
   }

   private BHashedEventBuffer getEventBuffer(BSourceState sourceState) {
      switch (sourceState.getOrdinal()) {
         case 0:
            return this.getToNormalBuffer();
         case 1:
            return this.getToOffnormalBuffer();
         case 2:
            return this.getToFaultBuffer();
         default:
            logger.severe("Unknown source state! " + sourceState);
            throw new IllegalArgumentException("Invalid source state:" + sourceState);
      }
   }

   private BFacets buildAlarmData(EventNotificationParameters enp) {
      HashMap<String, BIDataValue> map = new HashMap<>();
      map.put("processId", BString.make(Long.toString(enp.getProcessId())));
      map.put("deviceId", BString.make(enp.getInitiatingDeviceId().toString(facetsContext)));
      map.put("objectId", BString.make(enp.getEventObjectId().toString(facetsContext)));
      map.put("NC", BString.make(Long.toString(enp.getNotificationClass())));
      map.put("eventType", BString.make(enp.getEventType().getTag()));
      map.put("notifyType", BString.make(enp.getNotifyType().getTag()));
      map.put("toState", BString.make(enp.getToState().getTag()));
      map.put("BacnetTimestamp", BString.make(enp.getTimeStamp().toString(facetsContext)));
      map.put("priority", BInteger.make(enp.getPriority()));
      if (enp.getAckRequired()) {
         map.put("bacnetAcksRequired", BString.make(enp.getToState().getTag() + "@" + enp.getTimeStamp().toString(facetsContext)));
      }

      String s = enp.getMessageText();
      if (s != null && s.length() > 0) {
         map.put("msgText", BString.make(s));
      }

      BEnum fromState = enp.getFromState();
      if (fromState != null) {
         map.put("fromState", BString.make(fromState.getTag()));
      }

      BacnetNotificationParameters eventValues = enp.getEventValues();
      if (eventValues != null) {
         eventValues.addAlarmData(map);
      }

      return BFacets.make(map);
   }

   private BFacets buildAlarmData(NEventSummary esum, BBacnetObjectIdentifier deviceId, int transition) {
      HashMap<String, BIDataValue> map = new HashMap<>();
      map.put("processId", BString.make(String.valueOf(this.getEventSummaryProcessId())));
      map.put("deviceId", BString.make(deviceId.toString(facetsContext)));
      map.put("objectId", BString.make(esum.getObjectId().toString(facetsContext)));
      map.put("toState", BString.make(esum.getEventState().getTag()));
      map.put("BacnetTimestamp", BString.make(esum.getEventTimeStamps()[transition].toString(facetsContext)));
      map.put("notifyType", BString.make(esum.getNotifyType().getTag()));
      map.put("ackedTransitions", BString.make(esum.getAcknowledgedTransitions().toString(BacnetBitStringUtil.BacnetEventTransitionBits)));
      BBacnetTimeStamp[] eventTimeStamps = esum.getEventTimeStamps();
      map.put(
         "eventTimeStamps",
         BString.make(
            eventTimeStamps[0].toString(facetsContext) + "," + eventTimeStamps[1].toString(facetsContext) + "," + eventTimeStamps[2].toString(facetsContext)
         )
      );
      map.put("eventEnable", BString.make(esum.getEventEnable().toString(BacnetBitStringUtil.BacnetEventTransitionBits)));
      int[] eventPriorities = esum.getEventPriorities();
      map.put("eventPriorities", BString.make("" + eventPriorities[0] + "," + eventPriorities[1] + "," + eventPriorities[2]));
      return BFacets.make(map);
   }

   void addListener(EventNotificationListener listener, int serviceIndex) {
      switch (serviceIndex) {
         case 2:
            this.confirmedEventListeners.add(listener);
            break;
         case 29:
            this.unconfirmedEventListeners.add(listener);
      }
   }

   void removeListener(EventNotificationListener listener, int serviceIndex) {
      try {
         switch (serviceIndex) {
            case 2:
               this.confirmedEventListeners.remove(listener);
               break;
            case 29:
               this.unconfirmedEventListeners.remove(listener);
         }
      } catch (Exception var4) {
      }
   }

   private void routeToConfirmedListeners(ConfirmedEventNotificationRequest request, BBacnetAddress sourceAddress) {
      int len = this.confirmedEventListeners.size();
      if (len != 0) {
         EventNotificationParameters enp = request.getEventNotificationParameters();

         for (int i = 0; i < len; i++) {
            this.confirmedEventListeners
               .get(i)
               .receiveConfirmedEventNotification(
                  sourceAddress,
                  enp.getProcessId(),
                  enp.getInitiatingDeviceId(),
                  enp.getEventObjectId(),
                  enp.getTimeStamp(),
                  enp.getNotificationClass(),
                  enp.getPriority(),
                  enp.getEventType(),
                  enp.getMessageText(),
                  enp.getNotifyType(),
                  enp.getAckRequired(),
                  enp.getFromState(),
                  enp.getToState(),
                  enp.getRawEventValues(),
                  enp.getEncoding()
               );
         }
      }
   }

   private void routeToUnconfirmedListeners(UnconfirmedEventNotificationRequest request, BBacnetAddress sourceAddress) {
      int len = this.unconfirmedEventListeners.size();
      if (len != 0) {
         EventNotificationParameters enp = request.getEventNotificationParameters();

         for (int i = 0; i < len; i++) {
            this.unconfirmedEventListeners
               .get(i)
               .receiveUnconfirmedEventNotification(
                  sourceAddress,
                  enp.getProcessId(),
                  enp.getInitiatingDeviceId(),
                  enp.getEventObjectId(),
                  enp.getTimeStamp(),
                  enp.getNotificationClass(),
                  enp.getPriority(),
                  enp.getEventType(),
                  enp.getMessageText(),
                  enp.getNotifyType(),
                  enp.getAckRequired(),
                  enp.getFromState(),
                  enp.getToState(),
                  enp.getRawEventValues(),
                  enp.getEncoding()
               );
         }
      }
   }
}
