package com.tridium.bacnet.stack.server;

import com.tridium.bacnet.asn.NBacnetPropertyReference;
import com.tridium.bacnet.asn.NBacnetPropertyValue;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.asn.NReadAccessResult;
import com.tridium.bacnet.asn.NReadAccessSpec;
import com.tridium.bacnet.asn.NReadPropertyResult;
import com.tridium.bacnet.asn.NWriteAccessSpec;
import com.tridium.bacnet.services.BacnetAbort;
import com.tridium.bacnet.services.BacnetReject;
import com.tridium.bacnet.services.BacnetServicePrimitive;
import com.tridium.bacnet.services.BacnetSimpleAck;
import com.tridium.bacnet.services.confirmed.AddListElementRequest;
import com.tridium.bacnet.services.confirmed.ReadPropertyAck;
import com.tridium.bacnet.services.confirmed.ReadPropertyMultipleAck;
import com.tridium.bacnet.services.confirmed.ReadPropertyMultipleRequest;
import com.tridium.bacnet.services.confirmed.ReadPropertyRequest;
import com.tridium.bacnet.services.confirmed.ReadRangeRequest;
import com.tridium.bacnet.services.confirmed.RemoveListElementRequest;
import com.tridium.bacnet.services.confirmed.WritePropertyMultipleRequest;
import com.tridium.bacnet.services.confirmed.WritePropertyRequest;
import com.tridium.bacnet.services.error.NChangeListError;
import com.tridium.bacnet.services.error.SimpleError;
import com.tridium.bacnet.services.error.WritePropertyMultipleError;
import com.tridium.bacnet.stack.transport.BBacnetTransportLayer;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetConfirmedServiceChoice;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetObjectPropertyReference;
import javax.baja.bacnet.enums.BBacnetBackupState;
import javax.baja.bacnet.export.BIBacnetExportObject;
import javax.baja.bacnet.export.BLocalBacnetDevice;
import javax.baja.bacnet.io.AbortException;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.ChangeListError;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.PropertyReference;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.io.RangeData;
import javax.baja.bacnet.io.RejectException;

public class PropertyHandler implements ServiceHandler, BacnetConfirmedServiceChoice {
   private static final Logger logger = Logger.getLogger("bacnet.server");
   private BBacnetServerLayer server;
   private BBacnetTransportLayer transportLayer;

   PropertyHandler(BBacnetServerLayer server) {
      this.server = server;
   }

   @Override
   public BacnetServicePrimitive receiveRequest(int serviceChoice, BacnetServicePrimitive request, BBacnetAddress sourceAddress) {
      switch (serviceChoice) {
         case 8:
            return this.processAddListElementRequest((AddListElementRequest)request);
         case 9:
            return this.processRemoveListElementRequest((RemoveListElementRequest)request);
         case 10:
         case 11:
         case 13:
         case 17:
         case 18:
         case 19:
         case 20:
         case 21:
         case 22:
         case 23:
         case 24:
         case 25:
         default:
            logger.info("PropertyHandler.receiveRequest:Unknown request! " + request);
            return new BacnetReject(9);
         case 12:
            return this.processReadPropertyRequest((ReadPropertyRequest)request);
         case 14:
            return this.processReadPropertyMultipleRequest((ReadPropertyMultipleRequest)request);
         case 15:
            return this.processWritePropertyRequest((WritePropertyRequest)request);
         case 16:
            return this.processWritePropertyMultipleRequest((WritePropertyMultipleRequest)request);
         case 26:
            return this.processReadRangeRequest((ReadRangeRequest)request);
      }
   }

   public void setTransportLayer(BBacnetTransportLayer transportLayer) {
      this.transportLayer = transportLayer;
   }

   @Override
   public BBacnetTransportLayer getTransportLayer() {
      return this.transportLayer;
   }

   private BacnetServicePrimitive processReadPropertyRequest(ReadPropertyRequest request) {
      BLocalBacnetDevice local = BBacnetNetwork.localDevice();
      BBacnetObjectIdentifier objectId = request.getObjectId();
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("PropertyHandler.ReadPropertyRequest received: " + request);
      }

      BIBacnetExportObject object = local.lookupBacnetObject(objectId);
      if (object == null) {
         if (objectId.getObjectType() != 8 || objectId.getInstanceNumber() != 4194303) {
            return new SimpleError(12, new NErrorType(1, 31));
         }

         object = local;
         objectId = local.getObjectId();
      }

      if (objectId.equals(this.server.getBackupRestoreFileId()) && local.getBackupAndRestoreState() != BBacnetBackupState.idle) {
         this.server.updateLastBackupRestoreCommTime();
      }

      PropertyValue result;
      try {
         result = object.readProperty(new NBacnetPropertyReference(request.getPropertyId(), request.getPropertyArrayIndex()));
      } catch (RejectException var7) {
         return new BacnetReject(var7.getRejectReason());
      }

      return (BacnetServicePrimitive)(result.isError()
         ? new SimpleError(12, new NErrorType(result.getErrorClass(), result.getErrorCode()))
         : new ReadPropertyAck(objectId, request.getPropertyId(), request.getPropertyArrayIndex(), result.getPropertyValue()));
   }

   private BacnetServicePrimitive processReadPropertyMultipleRequest(ReadPropertyMultipleRequest request) {
      BLocalBacnetDevice local = BBacnetNetwork.localDevice();
      ReadPropertyMultipleAck response = new ReadPropertyMultipleAck();
      Iterator<NReadAccessSpec> readAccessSpecs = request.getReadAccessSpecs();
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("PropertyHandler.ReadPropertyMultipleRequest received: " + request);
      }

      boolean foundAtLeastOneObject = false;
      boolean foundAtLeastOneProperty = false;

      try {
         while (readAccessSpecs.hasNext()) {
            NReadAccessSpec ras = readAccessSpecs.next();
            BBacnetObjectIdentifier objectId = ras.getObjectId();
            BIBacnetExportObject object = local.lookupBacnetObject(objectId);
            if (object == null && objectId.getObjectType() == 8 && objectId.getInstanceNumber() == 4194303) {
               object = local;
               objectId = local.getObjectId();
            }

            NReadAccessResult readAccessResult = new NReadAccessResult(objectId);
            if (object == null) {
               Iterator propertyReferences = ras.getPropertyReferences();

               while (propertyReferences.hasNext()) {
                  NBacnetPropertyReference propRef = (NBacnetPropertyReference)propertyReferences.next();
                  readAccessResult.addResult(new NReadPropertyResult(propRef.getPropertyId(), propRef.getPropertyArrayIndex(), new NErrorType(1, 31)));
               }
            } else {
               foundAtLeastOneObject = true;
               if (objectId.equals(this.server.getBackupRestoreFileId())) {
                  this.server.updateLastBackupRestoreCommTime();
               }

               PropertyReference[] refs = ras.getListOfPropertyReferences();
               PropertyValue[] readResults = object.readPropertyMultiple(refs);

               for (int i = 0; i < readResults.length; i++) {
                  if (!foundAtLeastOneProperty && !readResults[i].isError()) {
                     foundAtLeastOneProperty = true;
                  }

                  readAccessResult.addResult(readResults[i]);
               }
            }

            response.addReadAccessResult(readAccessResult);
         }
      } catch (RejectException var14) {
         return new BacnetReject(var14.getRejectReason());
      }

      return (BacnetServicePrimitive)(foundAtLeastOneObject ? response : new SimpleError(14, new NErrorType(1, 31)));
   }

   private BacnetServicePrimitive processReadRangeRequest(ReadRangeRequest request) {
      BLocalBacnetDevice local = BBacnetNetwork.localDevice();
      BBacnetObjectIdentifier objectId = request.getObjectId();
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("PropertyHandler.ReadRangeRequest received: " + request);
      }

      BIBacnetExportObject object = local.lookupBacnetObject(objectId);
      if (object == null) {
         return new SimpleError(26, new NErrorType(1, 31));
      } else {
         RangeData result;
         try {
            result = object.readRange(request);
         } catch (RejectException var7) {
            return new BacnetReject(var7.getRejectReason());
         }

         return (BacnetServicePrimitive)(result.isError() ? new SimpleError(26, (NErrorType)result.getError()) : (BacnetServicePrimitive)result);
      }
   }

   private BacnetServicePrimitive processWritePropertyRequest(WritePropertyRequest request) {
      BLocalBacnetDevice local = BBacnetNetwork.localDevice();
      BBacnetObjectIdentifier objectId = request.getObjectId();
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("PropertyHandler.WritePropertyRequest received: " + request);
      }

      BIBacnetExportObject object = local.lookupBacnetObject(objectId);
      if (object == null) {
         return new SimpleError(15, new NErrorType(1, 31));
      } else {
         try {
            if (objectId.equals(this.server.getBackupRestoreFileId()) && local.getBackupAndRestoreState() != BBacnetBackupState.idle) {
               this.server.updateLastBackupRestoreCommTime();
            }

            object.setTransportLayer(this.getTransportLayer());
            ErrorType result = object.writeProperty(
               new NBacnetPropertyValue(request.getPropertyId(), request.getPropertyArrayIndex(), request.getEncodedValue(), request.getPriority())
            );
            return (BacnetServicePrimitive)(result == null ? new BacnetSimpleAck(15) : new SimpleError(15, (NErrorType)result));
         } catch (RejectException var7) {
            return new BacnetReject(var7.getRejectReason());
         } catch (AbortException var8) {
            return new BacnetAbort(var8.getAbortReason());
         } catch (AsnException var9) {
            return new BacnetReject(4);
         } catch (BacnetException var10) {
            return new SimpleError(15, new NErrorType(1, 0));
         } catch (Exception var11) {
            logger.log(Level.INFO, "Exception in processWritePropertyRequest:" + var11, (Throwable)var11);
            return new SimpleError(15, new NErrorType(5, 0));
         }
      }
   }

   private BacnetServicePrimitive processWritePropertyMultipleRequest(WritePropertyMultipleRequest request) {
      BLocalBacnetDevice local = BBacnetNetwork.localDevice();
      Iterator<NWriteAccessSpec> writeAccessSpecs = request.getWriteAccessSpecs();
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("PropertyHandler.WritePropertyMultipleRequest received: " + request);
      }

      BBacnetObjectIdentifier objectId = null;
      NBacnetPropertyValue propertyValue = null;

      try {
         while (writeAccessSpecs.hasNext()) {
            NWriteAccessSpec was = writeAccessSpecs.next();
            objectId = was.getObjectId();
            BIBacnetExportObject object = local.lookupBacnetObject(objectId);
            Iterator propertyValues = was.getPropertyValues();

            while (propertyValues.hasNext()) {
               propertyValue = (NBacnetPropertyValue)propertyValues.next();
               if (object == null) {
                  return new WritePropertyMultipleError(
                     new NErrorType(1, 31), new BBacnetObjectPropertyReference(objectId, propertyValue.getPropertyId(), propertyValue.getPropertyArrayIndex())
                  );
               }

               if (objectId.equals(this.server.getBackupRestoreFileId())) {
                  this.server.updateLastBackupRestoreCommTime();
               }

               ErrorType error = object.writeProperty(propertyValue);
               if (error != null) {
                  return new WritePropertyMultipleError(
                     (NErrorType)error, new BBacnetObjectPropertyReference(objectId, propertyValue.getPropertyId(), propertyValue.getPropertyArrayIndex())
                  );
               }
            }
         }

         return new BacnetSimpleAck(16);
      } catch (RejectException var10) {
         return new BacnetReject(var10.getRejectReason());
      } catch (AbortException var11) {
         return new BacnetAbort(var11.getAbortReason());
      } catch (AsnException var12) {
         return new BacnetReject(4);
      } catch (BacnetException var13) {
         if (objectId == null) {
            objectId = BBacnetObjectIdentifier.DEFAULT;
         }

         if (propertyValue == null) {
            propertyValue = new NBacnetPropertyValue();
         }

         return new WritePropertyMultipleError(
            new NErrorType(1, 0), new BBacnetObjectPropertyReference(objectId, propertyValue.getPropertyId(), propertyValue.getPropertyArrayIndex())
         );
      } catch (Exception var14) {
         logger.log(Level.INFO, "Exception in processWritePropertyMultipleRequest:" + var14, (Throwable)var14);
         if (objectId == null) {
            objectId = BBacnetObjectIdentifier.DEFAULT;
         }

         if (propertyValue == null) {
            propertyValue = new NBacnetPropertyValue();
         }

         return new WritePropertyMultipleError(
            new NErrorType(5, 0), new BBacnetObjectPropertyReference(objectId, propertyValue.getPropertyId(), propertyValue.getPropertyArrayIndex())
         );
      }
   }

   private BacnetServicePrimitive processAddListElementRequest(AddListElementRequest request) {
      BLocalBacnetDevice local = BBacnetNetwork.localDevice();
      BBacnetObjectIdentifier objectId = request.getObjectId();
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("PropertyHandler.AddListElementRequest received: " + request);
      }

      BIBacnetExportObject object = local.lookupBacnetObject(objectId);
      if (object == null) {
         return new NChangeListError(8, new NErrorType(1, 31), 1L);
      } else {
         try {
            ChangeListError result = object.addListElements(
               new NBacnetPropertyValue(request.getPropertyId(), request.getPropertyArrayIndex(), request.getListOfElements())
            );
            return (BacnetServicePrimitive)(result == null ? new BacnetSimpleAck(8) : (NChangeListError)result);
         } catch (RejectException var7) {
            return new BacnetReject(var7.getRejectReason());
         } catch (AbortException var8) {
            return new BacnetAbort(var8.getAbortReason());
         } catch (AsnException var9) {
            return new BacnetReject(4);
         } catch (BacnetException var10) {
            return new NChangeListError(8, new NErrorType(1, 0), 1L);
         } catch (Exception var11) {
            logger.log(Level.INFO, "Exception in addListElementRequest:" + var11, (Throwable)var11);
            return new NChangeListError(8, new NErrorType(5, 0), 1L);
         }
      }
   }

   private BacnetServicePrimitive processRemoveListElementRequest(RemoveListElementRequest request) {
      BLocalBacnetDevice local = BBacnetNetwork.localDevice();
      BBacnetObjectIdentifier objectId = request.getObjectId();
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("PropertyHandler.RemoveListElementRequest received: " + request);
      }

      BIBacnetExportObject object = local.lookupBacnetObject(objectId);
      if (object == null) {
         return new NChangeListError(9, new NErrorType(1, 31), 1L);
      } else {
         try {
            ChangeListError result = object.removeListElements(
               new NBacnetPropertyValue(request.getPropertyId(), request.getPropertyArrayIndex(), request.getListOfElements())
            );
            return (BacnetServicePrimitive)(result == null ? new BacnetSimpleAck(9) : (NChangeListError)result);
         } catch (RejectException var7) {
            return new BacnetReject(var7.getRejectReason());
         } catch (AbortException var8) {
            return new BacnetAbort(var8.getAbortReason());
         } catch (AsnException var9) {
            return new BacnetReject(4);
         } catch (BacnetException var10) {
            return new NChangeListError(9, new NErrorType(1, 0), 1L);
         } catch (Exception var11) {
            logger.log(Level.INFO, "Exception in removeListElementRequest:" + var11, (Throwable)var11);
            return new NChangeListError(9, new NErrorType(5, 0), 1L);
         }
      }
   }
}
