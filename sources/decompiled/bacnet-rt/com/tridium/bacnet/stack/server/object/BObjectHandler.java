package com.tridium.bacnet.stack.server.object;

import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.services.BacnetReject;
import com.tridium.bacnet.services.BacnetServicePrimitive;
import com.tridium.bacnet.services.confirmed.CreateObjectAck;
import com.tridium.bacnet.services.confirmed.CreateObjectRequest;
import com.tridium.bacnet.services.confirmed.DeleteObjectAck;
import com.tridium.bacnet.services.confirmed.DeleteObjectRequest;
import com.tridium.bacnet.services.error.CreateObjectError;
import com.tridium.bacnet.services.error.DeleteObjectError;
import com.tridium.bacnet.stack.server.ServiceHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetConfirmedServiceChoice;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetErrorClass;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.export.BIBacnetExportObject;
import javax.baja.bacnet.export.BLocalBacnetDevice;
import javax.baja.bacnet.export.BacnetDescriptorUtil;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.sys.BComponent;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BFolder;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "createEnabled",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "deleteEnabled",
      type = "boolean",
      defaultValue = "false"
   )})
public class BObjectHandler extends BComponent implements ServiceHandler, BacnetConfirmedServiceChoice {
   public static final Property createEnabled = newProperty(0, false, null);
   public static final Property deleteEnabled = newProperty(0, false, null);
   public static final Type TYPE = Sys.loadType(BObjectHandler.class);
   private static final int UNKNOWN_OBJECT_TYPE = -1;
   private static final Logger logger = Logger.getLogger("bacnet.server.object.handler");

   public boolean getCreateEnabled() {
      return this.getBoolean(createEnabled);
   }

   public void setCreateEnabled(boolean v) {
      this.setBoolean(createEnabled, v, null);
   }

   public boolean getDeleteEnabled() {
      return this.getBoolean(deleteEnabled);
   }

   public void setDeleteEnabled(boolean v) {
      this.setBoolean(deleteEnabled, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void started() throws Exception {
      super.started();
      boolean[] servicesSupported = BBacnetNetwork.localDevice().getProtocolServicesSupported().getBits();
      boolean createEnabled = this.getCreateEnabled();
      if (servicesSupported[10] != createEnabled) {
         setServicesSupportedBit(10, createEnabled);
      }

      boolean deleteEnabled = this.getDeleteEnabled();
      if (servicesSupported[11] != deleteEnabled) {
         setServicesSupportedBit(11, deleteEnabled);
      }
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning()) {
         if (p.equals(createEnabled)) {
            setServicesSupportedBit(10, this.getCreateEnabled());
         } else if (p.equals(deleteEnabled)) {
            setServicesSupportedBit(11, this.getDeleteEnabled());
         }
      }
   }

   private static void setServicesSupportedBit(int bit, boolean newState) {
      BLocalBacnetDevice localDevice = BBacnetNetwork.localDevice();
      BBacnetBitString newBitString = BBacnetBitString.make(localDevice.getProtocolServicesSupported(), bit, newState);
      localDevice.setProtocolServicesSupported(newBitString);
   }

   @Override
   public BacnetServicePrimitive receiveRequest(int serviceChoice, BacnetServicePrimitive request, BBacnetAddress sourceAddress) {
      switch (serviceChoice) {
         case 10:
            if (request instanceof CreateObjectRequest) {
               return this.processCreateObjectRequest(sourceAddress, (CreateObjectRequest)request);
            }
            break;
         case 11:
            if (request instanceof DeleteObjectRequest) {
               return this.processDeleteObjectRequest(sourceAddress, (DeleteObjectRequest)request);
            }
      }

      return null;
   }

   private BacnetServicePrimitive processCreateObjectRequest(BBacnetAddress address, CreateObjectRequest request) {
      int objectType = request.getObjectType();
      BBacnetObjectIdentifier oid = request.getObjectId();
      if (objectType == -1) {
         if (oid == null) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine("CreateObject request specifies UNKNOWN_OBJECT_TYPE and does not contain an objectId");
            }

            return error(BBacnetErrorClass.object, BBacnetErrorCode.valueOutOfRange, 0);
         }

         objectType = oid.getObjectType();
      }

      if (!isDescriptorSupportedForType(objectType)) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("CreateObject request specifies unknown objectType " + BBacnetObjectType.tag(objectType) + "; objectId: " + oid);
         }

         return error(BBacnetErrorClass.object, BBacnetErrorCode.unsupportedObjectType, 0);
      } else if (!isObjectCreationSupportedForType(objectType)) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("CreateObject request specifies unsupported objectType " + BBacnetObjectType.tag(objectType) + "; objectId: " + oid);
         }

         return error(BBacnetErrorClass.object, BBacnetErrorCode.dynamicCreationNotSupported, 0);
      } else if (!this.getCreateEnabled()) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("CreateObject request rejected because object creation is not enabled; objectId: " + oid);
         }

         return error(BBacnetErrorClass.object, BBacnetErrorCode.dynamicCreationNotSupported, 0);
      } else {
         if (BacnetDescriptorUtil.exportTable().get("dynamicObjects") == null) {
            BacnetDescriptorUtil.exportTable().add("dynamicObjects", new BFolder());
         }

         if (!this.checkForObjectSpace(objectType)) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine("CreateObject request rejected because there is no space for objectType " + BBacnetObjectType.tag(objectType) + "; objectId: " + oid);
            }

            return error(BBacnetErrorClass.object, BBacnetErrorCode.noSpaceForObject, 0);
         } else {
            if (oid == null && objectType > -1) {
               oid = BacnetDescriptorUtil.nextObjectIdentifier(objectType);
            }

            BBacnetObjectCreator objectCreator = this.getCreator(oid.getObjectType());
            if (objectCreator == null) {
               if (logger.isLoggable(Level.FINE)) {
                  logger.fine(
                     "CreateObject request rejected because there is no objectCreator for objectType "
                        + BBacnetObjectType.tag(objectType)
                        + "; objectId: "
                        + oid
                  );
               }

               return error(BBacnetErrorClass.object, BBacnetErrorCode.dynamicCreationNotSupported, 0);
            } else if (this.objectIdExists(oid)) {
               if (logger.isLoggable(Level.FINE)) {
                  logger.fine("CreateObject request rejected because there oid " + oid + " already exists");
               }

               return error(BBacnetErrorClass.object, BBacnetErrorCode.objectIdentifierAlreadyExists, 0);
            } else {
               return this.createObject(objectCreator, oid, request.getListOfInitialValues());
            }
         }
      }
   }

   private BacnetServicePrimitive createObject(BBacnetObjectCreator oc, BBacnetObjectIdentifier oid, Array<PropertyValue> initialValues) {
      if (oid.getInstanceNumber() > 4194302) {
         return new BacnetReject(6);
      } else {
         BIBacnetExportObject expDesc = null;

         try {
            if (oid.getObjectType() == 17) {
               expDesc = ((BScheduleCreator)oc).createObject(oid, initialValues);
               if (logger.isLoggable(Level.FINE)) {
                  logger.fine("Dynamic Schedule creation - Creating schedule " + expDesc.getType());
               }
            } else {
               expDesc = oc.createObject(oid);
            }
         } catch (Exception var15) {
            if (logger.isLoggable(Level.FINE)) {
               logger.log(Level.FINE, "Exception creating object for object identifier " + oid, (Throwable)var15);
            }

            return new BacnetReject(7);
         }

         ErrorType errorType = oc.exportObject(BacnetDescriptorUtil.exportTable(), expDesc);
         if (errorType != null) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine("Error exporting object for object identifier " + oid);
            }

            return error(errorType, 0);
         } else {
            if (initialValues != null) {
               for (int i = 0; i < initialValues.size(); i++) {
                  int elemNum = i + 1;
                  PropertyValue pv = (PropertyValue)initialValues.get(i);
                  if (!oc.checkProperties(pv.getPropertyId())) {
                     if (logger.isLoggable(Level.FINE)) {
                        logger.fine(
                           "Attempting to create object for object id "
                              + oid
                              + "; initial value property "
                              + BBacnetPropertyIdentifier.tag(pv.getPropertyId())
                              + " is unknown for objects of the type being created."
                        );
                     }

                     this.getDeletor(oid.getObjectType()).deleteObject(oid, BacnetDescriptorUtil.exportTable());
                     return error(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty, elemNum);
                  }

                  if (!oc.isInitialValueSupported(pv.getPropertyId())) {
                     if (logger.isLoggable(Level.FINE)) {
                        logger.fine(
                           "Attempting to create object for object id "
                              + oid
                              + "; initialization during the CreateObject service for property "
                              + BBacnetPropertyIdentifier.tag(pv.getPropertyId())
                              + " is not supported."
                        );
                     }

                     this.getDeletor(oid.getObjectType()).deleteObject(oid, BacnetDescriptorUtil.exportTable());
                     return error(BBacnetErrorClass.property, BBacnetErrorCode.writeAccessDenied, elemNum);
                  }

                  BBacnetErrorCode errorCode = null;
                  BacnetException exception = null;
                  BBacnetErrorClass errorClass = BBacnetErrorClass.object;

                  try {
                     errorType = oc.writeInitialValue(expDesc, pv);
                     if (errorType != null) {
                        if (logger.isLoggable(Level.FINE)) {
                           logger.fine(
                              "Attempting to create object for object id "
                                 + oid
                                 + "; error writing initial value for property "
                                 + BBacnetPropertyIdentifier.tag(pv.getPropertyId())
                                 + '.'
                           );
                        }

                        this.getDeletor(oid.getObjectType()).deleteObject(oid, BacnetDescriptorUtil.exportTable());
                        return error(errorType, elemNum);
                     }
                  } catch (AsnException var13) {
                     errorCode = BBacnetErrorCode.invalidDataType;
                     exception = var13;
                     errorClass = BBacnetErrorClass.property;
                  } catch (BacnetException var14) {
                     errorCode = BBacnetErrorCode.inconsistentParameters;
                     exception = var14;
                  }

                  if (errorCode != null) {
                     logger.log(
                        Level.WARNING,
                        "Attempting to create object for object id "
                           + oid
                           + "; exception thrown while writing initial value for property "
                           + BBacnetPropertyIdentifier.tag(pv.getPropertyId())
                           + '.',
                        (Throwable)exception
                     );
                     this.getDeletor(oid.getObjectType()).deleteObject(oid, BacnetDescriptorUtil.exportTable());
                     return error(errorClass, errorCode, elemNum);
                  }
               }

               oc.postProcess(expDesc);
            }

            return new CreateObjectAck(oid);
         }
      }
   }

   private BacnetServicePrimitive processDeleteObjectRequest(BBacnetAddress address, DeleteObjectRequest request) {
      ErrorType err = null;
      if (!this.getDeleteEnabled() || request.getObjectId().getObjectType() == 8) {
         return error(new NErrorType(1, 23));
      } else if (!this.objectIdExists(request.getObjectId())) {
         return error(new NErrorType(1, 31));
      } else if (this.getDeletor(request.getObjectId().getObjectType()) != null) {
         err = this.getDeletor(request.getObjectId().getObjectType()).deleteObject(request.getObjectId(), BacnetDescriptorUtil.exportTable());
         return (BacnetServicePrimitive)(err != null ? error(err) : new DeleteObjectAck(request.getObjectId()));
      } else {
         return error(new NErrorType(1, 23));
      }
   }

   private boolean objectIdExists(BBacnetObjectIdentifier oid) {
      return BacnetDescriptorUtil.exportTable().byObjectId(oid) != null;
   }

   private BBacnetObjectCreator getCreator(int objectType) {
      BBacnetObjectCreator[] creators = (BBacnetObjectCreator[])this.getChildren(BBacnetObjectCreator.class);
      BBacnetObjectCreator boc = null;

      for (BBacnetObjectCreator oc : creators) {
         if (oc.isObjectTypeSupported(objectType)) {
            boc = oc;
         }
      }

      if (boc == null) {
         boc = this.addObjectCreator(objectType);
      }

      return boc;
   }

   private BBacnetObjectDeletor getDeletor(int objectType) {
      BBacnetObjectDeletor[] deletors = (BBacnetObjectDeletor[])this.getChildren(BBacnetObjectDeletor.class);
      BBacnetObjectDeletor bod = null;

      for (BBacnetObjectDeletor od : deletors) {
         if (od.isObjectTypeSupported(objectType)) {
            bod = od;
         }
      }

      if (bod == null) {
         bod = this.addObjectDeletor(objectType);
      }

      return bod;
   }

   private BBacnetObjectCreator addObjectCreator(int objectType) {
      BBacnetObjectCreator oc = null;
      String key = "";
      switch (objectType) {
         case 6:
            oc = new BCalendarCreator();
            key = "CalendarCreator";
         case 7:
         case 8:
         case 10:
         case 11:
         case 12:
         case 13:
         case 14:
         case 16:
         case 18:
         case 19:
         default:
            break;
         case 9:
            oc = new BEventEnrollmentCreator();
            key = "EventEnrollmentCreator";
            break;
         case 15:
            oc = new BNotificationClassCreator();
            key = "NotificationClassCreator";
            break;
         case 17:
            oc = new BScheduleCreator();
            key = "ScheduleCreator";
            break;
         case 20:
            oc = new BTrendLogCreator();
            key = "TrendLogCreator";
      }

      if (oc != null) {
         this.add(key, oc, 5);
      }

      return oc;
   }

   private BBacnetObjectDeletor addObjectDeletor(int objectType) {
      switch (objectType) {
         case 6:
            BCalendarDeletor cd = new BCalendarDeletor();
            this.add("CalendarDeletor", cd, 5);
            return cd;
         case 7:
         case 8:
         case 10:
         case 11:
         case 12:
         case 13:
         case 14:
         case 16:
         case 18:
         case 19:
         default:
            return null;
         case 9:
            BEventEnrollmentDeletor ed = new BEventEnrollmentDeletor();
            this.add("EventEnrollmentDeletor", ed, 5);
            return ed;
         case 15:
            BNotificationClassDeletor nd = new BNotificationClassDeletor();
            this.add("NotificationClassDeletor", nd, 5);
            return nd;
         case 17:
            BScheduleDeletor sd = new BScheduleDeletor();
            this.add("ScheduleDeletor", sd, 5);
            return sd;
         case 20:
            BTrendLogDeletor td = new BTrendLogDeletor();
            this.add("TrendLogDeletor", td, 5);
            return td;
      }
   }

   private boolean checkForObjectSpace(int objecttype) {
      int licenseLimit = 0;
      Boolean result = true;
      return result;
   }

   private static boolean isObjectCreationSupportedForType(int objectType) {
      switch (objectType) {
         case 6:
         case 9:
         case 15:
         case 17:
         case 20:
            return true;
         case 7:
         case 8:
         case 10:
         case 11:
         case 12:
         case 13:
         case 14:
         case 16:
         case 18:
         case 19:
         default:
            return false;
      }
   }

   private static boolean isDescriptorSupportedForType(int objectType) {
      switch (objectType) {
         case 0:
         case 1:
         case 2:
         case 3:
         case 4:
         case 5:
         case 6:
         case 8:
         case 9:
         case 10:
         case 12:
         case 13:
         case 14:
         case 15:
         case 17:
         case 19:
         case 20:
         case 29:
         case 40:
         case 45:
         case 46:
         case 48:
            return true;
         case 7:
         case 11:
         case 16:
         case 18:
         case 21:
         case 22:
         case 23:
         case 24:
         case 25:
         case 26:
         case 27:
         case 28:
         case 30:
         case 31:
         case 32:
         case 33:
         case 34:
         case 35:
         case 36:
         case 37:
         case 38:
         case 39:
         case 41:
         case 42:
         case 43:
         case 44:
         case 47:
         default:
            return false;
      }
   }

   protected static BacnetServicePrimitive error(BBacnetErrorClass errClass, BBacnetErrorCode errCode, int elementNumber) {
      return new CreateObjectError(new NErrorType(errClass.getOrdinal(), errCode.getOrdinal()), elementNumber);
   }

   protected static BacnetServicePrimitive error(ErrorType errorType, int elementNumber) {
      return new CreateObjectError(new NErrorType(errorType.getErrorClass(), errorType.getErrorCode()), elementNumber);
   }

   protected static BacnetServicePrimitive error(ErrorType errorType) {
      return new DeleteObjectError(new NErrorType(errorType.getErrorClass(), errorType.getErrorCode()));
   }
}
