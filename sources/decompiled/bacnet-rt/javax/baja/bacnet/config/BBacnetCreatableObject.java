package javax.baja.bacnet.config;

import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.NBacnetPropertyValue;
import com.tridium.bacnet.stack.BBacnetStack;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BBacnetObject;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetArray;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetDestination;
import javax.baja.bacnet.datatypes.BBacnetDeviceObjectPropertyReference;
import javax.baja.bacnet.datatypes.BBacnetEventParameter;
import javax.baja.bacnet.datatypes.BBacnetListOf;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetPropertyValue;
import javax.baja.bacnet.datatypes.BBacnetUnsigned;
import javax.baja.bacnet.enums.BBacnetEventType;
import javax.baja.bacnet.enums.BBacnetNotifyType;
import javax.baja.bacnet.io.ErrorException;
import javax.baja.bacnet.io.IllegalActionInitiationError;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.status.BStatus;
import javax.baja.sys.Action;
import javax.baja.sys.BEnum;
import javax.baja.sys.BValue;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "description",
   type = "String",
   defaultValue = "",
   facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.DESCRIPTION, BacnetConst.ASN_CHARACTER_STRING)")}
)
@NiagaraAction(
   name = "createObject"
)
public abstract class BBacnetCreatableObject extends BBacnetObject {
   public static final Property description = newProperty(0, "", makeFacets(28, 7));
   public static final Action createObject = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BBacnetCreatableObject.class);

   public String getDescription() {
      return this.getString(description);
   }

   public void setDescription(String v) {
      this.setString(description, v, null);
   }

   public void createObject() {
      this.invoke(createObject, null, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void started() throws Exception {
      super.started();
      if (BBacnetNetwork.bacnet().getType() == BBacnetNetwork.TYPE) {
         int hideFlag = this.getFlags(createObject) | 4;
         this.setFlags(createObject, hideFlag);
      }
   }

   private BBacnetObjectIdentifier sendCreateObjectRequest(Array<PropertyValue> listOfInitialValues) throws BacnetException {
      BBacnetStack stack = (BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm();
      BBacnetObjectIdentifier objectId = this.getObjectId();
      BBacnetDevice device = (BBacnetDevice)this.getParent().getParent();
      return objectId.getInstanceNumber() < 0
         ? stack.getClient().createObject(device.getAddress(), objectId.getObjectType(), listOfInitialValues)
         : stack.getClient().createObject(device.getAddress(), objectId, listOfInitialValues);
   }

   protected void addDescription(Array<PropertyValue> listOfInitialValues) {
      String desc = this.getDescription();
      if (desc != null && desc.length() != 0) {
         this.addProperty(28, description, listOfInitialValues);
      }
   }

   protected void addObjectName(Array<PropertyValue> listOfInitialValues) {
      String name = this.getObjectName();
      if (name != null && name.length() != 0) {
         this.addProperty(77, objectName, listOfInitialValues);
      }
   }

   protected void addEventParameter(BBacnetEventParameter eventParameter, Array<PropertyValue> listOfInitialValues) {
      if (eventParameter.getChoice() != 20) {
         AsnOutputStream asnOut = new AsnOutputStream();
         eventParameter.writeAsn(asnOut);
         PropertyValue eventParametersPV = new NBacnetPropertyValue(83, asnOut.toByteArray());
         listOfInitialValues.add(eventParametersPV);
      }
   }

   protected void addEventEnable(BBacnetBitString eventEnable, Array<PropertyValue> listOfInitialValues) {
      PropertyValue eventEnablePV = new BBacnetPropertyValue(35, eventEnable);
      listOfInitialValues.add(eventEnablePV);
   }

   protected void addNotificationClass(BBacnetUnsigned notificationClass, Array<PropertyValue> listOfInitialValues) {
      long nc = notificationClass.getInt();
      if (nc >= 0L && nc < 4194303L) {
         PropertyValue notificationClassPV = new BBacnetPropertyValue(17, notificationClass);
         listOfInitialValues.add(notificationClassPV);
      }
   }

   protected void addLogDeviceObjectPropertyReference(BBacnetDeviceObjectPropertyReference objectPropertyReference, Array<PropertyValue> listOfInitialValues) {
      if (objectPropertyReference.getObjectId().getInstanceNumber() != -1) {
         AsnOutputStream asnOut = new AsnOutputStream();
         objectPropertyReference.writeAsn(asnOut);
         PropertyValue objectPropertyReferencePV = new NBacnetPropertyValue(132, asnOut.toByteArray());
         listOfInitialValues.add(objectPropertyReferencePV);
      }
   }

   protected void addObjectPropertyReference(BBacnetDeviceObjectPropertyReference objectPropertyReference, Array<PropertyValue> listOfInitialValues) {
      if (objectPropertyReference.getObjectId().getInstanceNumber() != -1) {
         AsnOutputStream asnOut = new AsnOutputStream();
         objectPropertyReference.writeAsn(asnOut);
         PropertyValue objectPropertyReferencePV = new NBacnetPropertyValue(78, asnOut.toByteArray());
         listOfInitialValues.add(objectPropertyReferencePV);
      }
   }

   protected void addEventType(BEnum evType, Array<PropertyValue> listOfInitialValues) {
      BBacnetEventType eventType = BBacnetEventType.make(evType.getOrdinal());
      if (eventType != BBacnetEventType.none) {
         PropertyValue eventTypePV = new BBacnetPropertyValue(37, eventType);
         listOfInitialValues.add(eventTypePV);
      }
   }

   protected void addNotifyType(BBacnetNotifyType notifyType, Array<PropertyValue> listOfInitialValues) {
      PropertyValue notifyTypePV = new BBacnetPropertyValue(72, notifyType);
      listOfInitialValues.add(notifyTypePV);
   }

   protected void addRecipientist(Property recipientList, Array<PropertyValue> listOfInitialValues) {
      if (this.shouldAddListProperty(recipientList, BBacnetDestination.class)) {
         this.addProperty(102, recipientList, listOfInitialValues);
      }
   }

   protected void addListOfObjectPropertyReferences(Property listOfObjectPropertyReferences, Array<PropertyValue> listOfInitialValues) {
      if (this.shouldAddListProperty(listOfObjectPropertyReferences, BBacnetDeviceObjectPropertyReference.class)) {
         this.addProperty(54, listOfObjectPropertyReferences, listOfInitialValues);
      }
   }

   protected void addPriorityForWriting(BBacnetUnsigned priorityForWriting, Array<PropertyValue> listOfInitialValues) {
      if (priorityForWriting != null) {
         long pow = priorityForWriting.getInt();
         if (pow > 0L && pow != 4194303L) {
            PropertyValue priorityForWritingPV = new BBacnetPropertyValue(88, priorityForWriting);
            listOfInitialValues.add(priorityForWritingPV);
         }
      }
   }

   protected void addScheduleDefault(Property scheduleDefault, Array<PropertyValue> listOfInitialValue) {
      this.addProperty(174, scheduleDefault, listOfInitialValue);
   }

   public void doCreateObject() {
      if (BBacnetNetwork.bacnet().getType() == BBacnetNetwork.TYPE) {
         throw new IllegalActionInitiationError("bacnet", "illegal.create.object.initiation", new Object[]{"Bacnet"});
      } else {
         Array<PropertyValue> listOfInitialValues = new Array(PropertyValue.class);
         this.addInitialValues(listOfInitialValues);

         try {
            BBacnetObjectIdentifier objOid = this.sendCreateObjectRequest(listOfInitialValues);
            this.setObjectId(objOid);
            this.doUpload(null, null);
            this.setFaultCause("");
         } catch (Exception var4) {
            if (var4 instanceof ErrorException) {
               ErrorException errorException = (ErrorException)var4;
               this.setStatus(BStatus.fault);
               this.setFaultCause(errorException.toString());
               log.severe("Could not send the create object request :: " + errorException.toString());
            } else {
               log.severe("Could not send the create object request.");
            }
         }
      }
   }

   protected void addInitialValues(Array<PropertyValue> listOfInitialValues) {
      this.addObjectName(listOfInitialValues);
      this.addDescription(listOfInitialValues);
      this.addObjectInitialValues(listOfInitialValues);
   }

   protected void addPriority(Property priority, Array<PropertyValue> listOfInitialValues) {
      BValue value = this.get(priority);
      if (value != null && value instanceof BBacnetArray) {
         BBacnetArray priorityArray = (BBacnetArray)value;
         if (priorityArray != null && priorityArray.getSize() > 0) {
            this.addProperty(86, priority, listOfInitialValues);
         }
      }
   }

   private <T> boolean shouldAddListProperty(Property property, Class<T> childClass) {
      if (property != null) {
         BValue value = this.get(property);
         if (value instanceof BBacnetListOf) {
            T[] destinations = (T[])((BBacnetListOf)value).getChildren(childClass);
            if (destinations != null && destinations.length > 0) {
               return true;
            }
         }
      }

      return false;
   }

   protected void addAckRequired(Property property, Array<PropertyValue> listOfInitialValues) {
      this.addProperty(1, property, listOfInitialValues);
   }

   private void addProperty(int propertyIdentifer, Property property, Array<PropertyValue> listOfInitialValues) {
      BBacnetObject.BacnetPropertyData d = this.getPropertyData(property);
      listOfInitialValues.add(new NBacnetPropertyValue(propertyIdentifer, this.toEncodedValue(d, property)));
   }

   protected abstract void addObjectInitialValues(Array<PropertyValue> var1);
}
