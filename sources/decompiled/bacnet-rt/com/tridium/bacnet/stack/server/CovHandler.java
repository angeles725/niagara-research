package com.tridium.bacnet.stack.server;

import com.tridium.bacnet.BacUtil;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.CovNotificationParameters;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.services.BacnetReject;
import com.tridium.bacnet.services.BacnetServicePrimitive;
import com.tridium.bacnet.services.BacnetSimpleAck;
import com.tridium.bacnet.services.confirmed.ConfirmedCovNotificationRequest;
import com.tridium.bacnet.services.confirmed.SubscribeCovPropertyRequest;
import com.tridium.bacnet.services.confirmed.SubscribeCovRequest;
import com.tridium.bacnet.services.error.SimpleError;
import com.tridium.bacnet.services.unconfirmed.UnconfirmedCovNotificationRequest;
import com.tridium.bacnet.stack.BBacnetStack;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetConfirmedServiceChoice;
import javax.baja.bacnet.BacnetUnconfirmedServiceChoice;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetCovSubscription;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetTimeStamp;
import javax.baja.bacnet.enums.BBacnetRestartReason;
import javax.baja.bacnet.export.BIBacnetCovSource;
import javax.baja.bacnet.export.BIBacnetExportObject;
import javax.baja.bacnet.export.BLocalBacnetDevice;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.PropertyReference;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.point.BBacnetPointDeviceExt;
import javax.baja.bacnet.point.BBacnetProxyExt;
import javax.baja.bacnet.point.PointCmd;
import javax.baja.control.BControlPoint;
import javax.baja.status.BStatus;
import javax.baja.sys.BDouble;
import javax.baja.sys.BValue;

public class CovHandler implements ServiceHandler, BacnetConfirmedServiceChoice, BacnetUnconfirmedServiceChoice {
   private static BBacnetNetwork network;
   private static final Logger logger = Logger.getLogger("bacnet.server");

   @Override
   public BacnetServicePrimitive receiveRequest(int serviceChoice, BacnetServicePrimitive request, BBacnetAddress sourceAddress) {
      switch (serviceChoice) {
         case 1:
            return this.processConfirmedCovNotificationRequest((ConfirmedCovNotificationRequest)request);
         case 2:
            this.processUnconfirmedCovNotificationRequest((UnconfirmedCovNotificationRequest)request);
            return null;
         case 5:
            return this.processSubscribeCovRequest((SubscribeCovRequest)request, sourceAddress);
         case 28:
            return this.processSubscribeCovPropertyRequest((SubscribeCovPropertyRequest)request, sourceAddress);
         default:
            logger.info("CovHandler.receiveRequest:Unknown request! " + request);
            return new BacnetReject(9);
      }
   }

   private BacnetServicePrimitive processSubscribeCovRequest(SubscribeCovRequest request, BBacnetAddress subscriberAddress) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("CovHandler.SubscribeCovRequest received: " + request);
      }

      BLocalBacnetDevice local = BBacnetNetwork.localDevice();
      BBacnetObjectIdentifier objectId = request.getMonitoredObjectId();
      long processId = request.getSubscriberProcessId();
      long lifetime = request.getLifetime();
      BIBacnetExportObject export = local.lookupBacnetObject(objectId);
      BacnetServicePrimitive validationError = this.covRequestValidation(export, objectId, lifetime, 5);
      if (validationError != null) {
         return validationError;
      } else {
         BIBacnetCovSource point = (BIBacnetCovSource)export;
         BBacnetCovSubscription sub = point.findCovSubscription(subscriberAddress, processId, objectId);
         if (request.isCancellation()) {
            if (sub != null) {
               point.removeCovSubscription(sub);
            }

            return new BacnetSimpleAck(5);
         } else {
            if (sub != null) {
               sub.setIssueConfirmedNotifications(request.getIssueConfirmedNotifications());
               point.startCovTimer(sub, lifetime);
            } else {
               sub = new BBacnetCovSubscription(subscriberAddress, processId, objectId, request.getIssueConfirmedNotifications());
               sub.setLastValue(((BControlPoint)point.getObject()).getOutStatusValue());
               point.startCovTimer(sub, lifetime);
               point.addCovSubscription(sub);
            }

            return new BacnetSimpleAck(5);
         }
      }
   }

   private BacnetServicePrimitive covRequestValidation(BIBacnetExportObject export, BBacnetObjectIdentifier objectId, long lifetime, int requestType) {
      if (export == null) {
         return new SimpleError(requestType, new NErrorType(1, 31));
      } else if (export.getObject() == null) {
         return new SimpleError(requestType, new NErrorType(1, 1000));
      } else if (lifetime > 28800L) {
         logger.info("Lifetime is more that that allowed value of 28800 seconds :: " + lifetime);
         return new SimpleError(requestType, new NErrorType(5, 37));
      } else if (!(export instanceof BIBacnetCovSource)) {
         logger.info("Attempt to subscribe-" + (requestType == 28 ? "Cov" : "CovP") + " for non-Cov object " + objectId);
         return new SimpleError(requestType, new NErrorType(1, 45));
      } else {
         return null;
      }
   }

   private BacnetServicePrimitive covpRequestValidation(BIBacnetExportObject export, BBacnetObjectIdentifier objectId, long lifetime, int propId) {
      BacnetServicePrimitive validationError = this.covRequestValidation(export, objectId, lifetime, 28);
      if (validationError != null) {
         return validationError;
      } else {
         switch (propId) {
            case 28:
            case 75:
            case 77:
            case 79:
            case 371:
               return new SimpleError(28, new NErrorType(2, 44));
            default:
               boolean presentInPropertyList = Arrays.stream(export.getPropertyList()).anyMatch(i -> i == propId);
               return !presentInPropertyList ? new SimpleError(28, new NErrorType(2, 32)) : null;
         }
      }
   }

   private BacnetServicePrimitive processSubscribeCovPropertyRequest(SubscribeCovPropertyRequest request, BBacnetAddress subscriberAddress) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("CovHandler.SubscribeCovPropertyRequest received: " + request);
      }

      BLocalBacnetDevice local = BBacnetNetwork.localDevice();
      BBacnetObjectIdentifier objectId = request.getMonitoredObjectId();
      long processId = request.getSubscriberProcessId();
      long lifetime = request.getLifetime();
      PropertyReference pref = request.getMonitoredPropertyId();
      BIBacnetExportObject export = local.lookupBacnetObject(objectId);
      int propertyId = request.getMonitoredPropertyId().getPropertyId();
      BacnetServicePrimitive validationError = this.covpRequestValidation(export, objectId, lifetime, propertyId);
      if (validationError != null) {
         return validationError;
      } else {
         BIBacnetCovSource point = (BIBacnetCovSource)export;
         BBacnetCovSubscription sub = point.findCovPropertySubscription(
            subscriberAddress, processId, objectId, pref.getPropertyId(), pref.getPropertyArrayIndex()
         );
         if (request.isCancellation()) {
            if (sub != null) {
               point.removeCovSubscription(sub);
            }

            return new BacnetSimpleAck(28);
         } else {
            if (sub != null) {
               sub.setIssueConfirmedNotifications(request.getIssueConfirmedNotifications());
               if (lifetime > 0L) {
                  point.startCovTimer(sub, lifetime);
                  BDouble covIncrement = request.getCovIncrement();
                  if (covIncrement != null) {
                     sub.setCovIncrement(covIncrement.getFloat());
                  }
               }
            } else {
               sub = new BBacnetCovSubscription(
                  subscriberAddress, processId, objectId, pref, request.getIssueConfirmedNotifications(), request.getCovIncrement()
               );
               sub.setCovProperty(true);
               sub.setLastPropValue(point.getCurrentCovValue(sub));
               if (lifetime > 0L) {
                  point.startCovTimer(sub, lifetime);
               }

               point.addCovSubscription(sub);
            }

            return new BacnetSimpleAck(28);
         }
      }
   }

   private BacnetServicePrimitive processConfirmedCovNotificationRequest(ConfirmedCovNotificationRequest request) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("CovHandler.ConfirmedCovNotificationRequest received: " + request);
      }

      this.processCov(request.getCovNotificationParameters());
      return new BacnetSimpleAck(1);
   }

   private void processUnconfirmedCovNotificationRequest(UnconfirmedCovNotificationRequest request) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("CovHandler.UnconfirmedCovNotificationRequest received: " + request);
      }

      this.processCov(request.getCovNotificationParameters());
   }

   private void processCov(CovNotificationParameters cnp) {
      BBacnetDevice device = BBacnetNetwork.bacnet().doLookupDeviceById(cnp.getInitiatingDeviceId());
      if (device == null) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("Cov Notification from unmapped device:" + cnp.getInitiatingDeviceId());
         }
      } else {
         BBacnetObjectIdentifier objectId = cnp.getMonitoredObjectId();
         PropertyValue[] propVals = cnp.getListOfValues();
         BBacnetServerLayer serverLayer = ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm()).getServer();
         if (serverLayer.getUpdateStatusOnCov() && !device.isDown()) {
            device.pingOk();
         }

         BStatus status = null;

         for (int i = 0; i < propVals.length; i++) {
            try {
               if (propVals[i].getPropertyId() == 111) {
                  status = AsnUtil.asnStatusFlagsToBStatus(propVals[i].getPropertyValue());
               } else if (propVals[i].getPropertyId() == 196) {
                  BBacnetRestartReason reason = (BBacnetRestartReason)AsnUtil.fromAsnEnumerated(BBacnetRestartReason.DEFAULT, propVals[i].getPropertyValue());
                  updateDevice(device, "lastRestartReason", reason);
               } else if (propVals[i].getPropertyId() == 203) {
                  BBacnetTimeStamp timeStamp = new BBacnetTimeStamp();
                  AsnUtil.fromAsn(propVals[i].getPropertyValue(), timeStamp);
                  updateDevice(device, "timeOfDeviceRestart", timeStamp);
               } else {
                  updatePointValue(device, objectId, propVals[i]);
               }
            } catch (AsnException var9) {
               if (logger.isLoggable(Level.FINE)) {
                  logger.log(Level.FINE, "Error parsing property: " + var9.getMessage(), (Throwable)var9);
               }
            }
         }

         updateStatusFlags(device, objectId, status);
      }
   }

   private static void updatePointValue(BBacnetDevice device, BBacnetObjectIdentifier objectId, PropertyValue propVal) {
      BBacnetPointDeviceExt ext = device.getPoints();
      BControlPoint[] points = ext.findPoints(objectId, propVal.getPropertyId(), propVal.getPropertyArrayIndex());

      for (int i = 0; i < points.length; i++) {
         BBacnetProxyExt proxyExt = (BBacnetProxyExt)points[i].getProxyExt();
         if (!proxyExt.isPolled() || proxyExt.getAcceptUnsolicitedCov()) {
            proxyExt.fromEncodedValue(propVal.getPropertyValue(), null, BBacnetProxyExt.covContext);
         }
      }
   }

   private static void updateStatusFlags(BBacnetDevice device, BBacnetObjectIdentifier objectId, BStatus status) {
      BControlPoint[] pts = device.getPoints().findPoints(objectId);

      for (int j = 0; j < pts.length; j++) {
         BBacnetProxyExt ext = (BBacnetProxyExt)pts[j].getProxyExt();
         if (ext.useStatusFlags()) {
            if (status == null) {
               BBacnetNetwork.bacnet().postAsync(new PointCmd(268435456, ext));
            } else {
               ext.fromEncodedValue(null, status, BBacnetProxyExt.covContext);
            }
         }
      }
   }

   private static void updateDevice(BBacnetDevice device, String name, BValue value) {
      BacUtil.setOrAdd(device.getConfig().getDeviceObject(), name, value, 1, null, null);
   }

   private static boolean isSupported(int propertyId) {
      int[] propertyIds = new int[]{355, 354, 356, 357, 168, 113, 17, 35, 0, 72, 353, 45, 59, 25, 52};
      return Arrays.stream(propertyIds).filter(x -> x == propertyId).findFirst() == null;
   }

   private static BBacnetNetwork bacnet() {
      if (network == null) {
         network = BBacnetNetwork.bacnet();
      }

      return network;
   }
}
