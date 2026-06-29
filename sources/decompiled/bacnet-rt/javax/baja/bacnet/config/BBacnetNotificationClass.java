package javax.baja.bacnet.config;

import com.tridium.bacnet.datatypes.BNcRecipientList;
import javax.baja.bacnet.datatypes.BBacnetArray;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetDestination;
import javax.baja.bacnet.datatypes.BBacnetListOf;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetRecipient;
import javax.baja.bacnet.datatypes.BBacnetUnsigned;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.driver.loadable.BUploadParameters;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.sys.Action;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.NOTIFICATION_CLASS)",
      flags = 8,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_IDENTIFIER, ASN_OBJECT_IDENTIFIER)")},
      override = true
   ), @NiagaraProperty(
      name = "objectType",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetObjectType.NOTIFICATION_CLASS, BEnumRange.make(BBacnetObjectType.TYPE))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_TYPE, ASN_ENUMERATED)")},
      override = true
   ), @NiagaraProperty(
      name = "notificationClass",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.DEFAULT",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.NOTIFICATION_CLASS, ASN_UNSIGNED)")}
   ), @NiagaraProperty(
      name = "priority",
      type = "BBacnetArray",
      defaultValue = "new BBacnetArray(BBacnetUnsigned.TYPE, 3)",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.PRIORITY, ASN_BACNET_ARRAY)")}
   ), @NiagaraProperty(
      name = "ackRequired",
      type = "BBacnetBitString",
      defaultValue = "BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength(\"BacnetEventTransitionBits\"))",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.ACK_REQUIRED, ASN_BIT_STRING, BacnetBitStringUtil.BACNET_EVENT_TRANSITION_BITS_MAP)")}
   ), @NiagaraProperty(
      name = "recipientList",
      type = "BBacnetListOf",
      defaultValue = "new BNcRecipientList(BBacnetDestination.TYPE)",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.RECIPIENT_LIST, ASN_BACNET_LIST)")}
   )})
@NiagaraActions({@NiagaraAction(
      name = "addDestination",
      parameterType = "BBacnetDestination",
      defaultValue = "new BBacnetDestination()"
   ), @NiagaraAction(
      name = "removeDestination",
      parameterType = "BBacnetDestination",
      defaultValue = "new BBacnetDestination()"
   ), @NiagaraAction(
      name = "removeRecipient",
      parameterType = "BBacnetRecipient",
      defaultValue = "new BBacnetRecipient()"
   )})
public class BBacnetNotificationClass extends BBacnetCreatableObject {
   public static final Property objectId = newProperty(8, BBacnetObjectIdentifier.make(15), makeFacets(75, 12));
   public static final Property objectType = newProperty(1, BDynamicEnum.make(15, BEnumRange.make(BBacnetObjectType.TYPE)), makeFacets(79, 9));
   public static final Property notificationClass = newProperty(1, BBacnetUnsigned.DEFAULT, makeFacets(17, 2));
   public static final Property priority = newProperty(1, new BBacnetArray(BBacnetUnsigned.TYPE, 3), makeFacets(86, -2));
   public static final Property ackRequired = newProperty(
      0,
      BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength("BacnetEventTransitionBits")),
      makeFacets(1, 8, BacnetBitStringUtil.BACNET_EVENT_TRANSITION_BITS_MAP)
   );
   public static final Property recipientList = newProperty(0, new BNcRecipientList(BBacnetDestination.TYPE), makeFacets(102, -3));
   public static final Action addDestination = newAction(0, new BBacnetDestination(), null);
   public static final Action removeDestination = newAction(0, new BBacnetDestination(), null);
   public static final Action removeRecipient = newAction(0, new BBacnetRecipient(), null);
   public static final Type TYPE = Sys.loadType(BBacnetNotificationClass.class);

   public BBacnetUnsigned getNotificationClass() {
      return (BBacnetUnsigned)this.get(notificationClass);
   }

   public void setNotificationClass(BBacnetUnsigned v) {
      this.set(notificationClass, v, null);
   }

   public BBacnetArray getPriority() {
      return (BBacnetArray)this.get(priority);
   }

   public void setPriority(BBacnetArray v) {
      this.set(priority, v, null);
   }

   public BBacnetBitString getAckRequired() {
      return (BBacnetBitString)this.get(ackRequired);
   }

   public void setAckRequired(BBacnetBitString v) {
      this.set(ackRequired, v, null);
   }

   public BBacnetListOf getRecipientList() {
      return (BBacnetListOf)this.get(recipientList);
   }

   public void setRecipientList(BBacnetListOf v) {
      this.set(recipientList, v, null);
   }

   public void addDestination(BBacnetDestination parameter) {
      this.invoke(addDestination, parameter, null);
   }

   public void removeDestination(BBacnetDestination parameter) {
      this.invoke(removeDestination, parameter, null);
   }

   public void removeRecipient(BBacnetRecipient parameter) {
      this.invoke(removeRecipient, parameter, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public void doAddDestination(BBacnetDestination dest) {
      this.getRecipientList().addListElement(dest, null);
   }

   public void doRemoveDestination(BBacnetDestination dest) {
      this.getRecipientList().removeListElement(dest, null);
   }

   public void doRemoveRecipient(BBacnetRecipient recip) {
      this.network().postAsync(new BBacnetNotificationClass.NCRemoveRecipientRequest(recip));
      this.upload(new BUploadParameters());
   }

   @Override
   protected void addObjectInitialValues(Array<PropertyValue> listOfInitialValues) {
      this.addPriority(priority, listOfInitialValues);
      this.addAckRequired(ackRequired, listOfInitialValues);
      this.addRecipientist(recipientList, listOfInitialValues);
   }

   class NCRemoveRecipientRequest implements Runnable {
      public BBacnetRecipient recip;

      NCRemoveRecipientRequest(BBacnetRecipient recip) {
         this.recip = recip;
      }

      @Override
      public void run() {
         SlotCursor<Property> sc = BBacnetNotificationClass.this.getRecipientList().getProperties();

         while (sc.next(BBacnetDestination.class)) {
            BBacnetDestination d = (BBacnetDestination)sc.get();
            if (d.getRecipient().equivalent(this.recip)) {
               BBacnetNotificationClass.this.getRecipientList().removeListElement(d, null);
            }
         }
      }
   }
}
