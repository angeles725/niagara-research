package com.tridium.bacnet.datatypes;

import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetObject;
import javax.baja.bacnet.datatypes.BBacnetDestination;
import javax.baja.bacnet.datatypes.BBacnetListOf;
import javax.baja.driver.loadable.BUploadParameters;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BObject;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BNcRecipientList extends BBacnetListOf {
   public static final Type TYPE = Sys.loadType(BNcRecipientList.class);
   protected boolean addActions = false;
   private boolean config;
   private static final Logger logger = Logger.getLogger("bacnet");

   @Override
   public Type getType() {
      return TYPE;
   }

   public BNcRecipientList() {
   }

   public BNcRecipientList(Type listType) {
      super(listType);
   }

   @Override
   public final void started() {
      super.started();
      if (this.getParent() instanceof BBacnetObject) {
         this.config = true;
      }
   }

   @Override
   public Property addListElement(BValue listElement, Context cx) {
      if (this.config) {
         BBacnetObject o = (BBacnetObject)this.getParent();
         if (o.getObjectId().getInstanceNumber() >= 0) {
            o.postAsync(new BNcRecipientList.RecipientListManipulation(o, this.getPropertyInParent(), listElement, true));
            o.upload(new BUploadParameters());
            return null;
         }
      }

      if (listElement.getType().is(this.getListType())) {
         return !this.contains(listElement) ? this.add(null, listElement, cx) : null;
      } else if (listElement instanceof BOrd) {
         return this.add(null, listElement, 2, cx);
      } else {
         log.severe(this + ".addListElement:Wrong element type: this is a list of " + this.getListType().getTypeName());
         return null;
      }
   }

   @Override
   public void removeListElement(BValue listElement, Context cx) {
      if (this.config) {
         BBacnetObject o = (BBacnetObject)this.getParent();
         o.postAsync(new BNcRecipientList.RecipientListManipulation(o, this.getPropertyInParent(), listElement, false));
         o.upload(new BUploadParameters());
      } else {
         SlotCursor<Property> c = this.getProperties();

         while (c.next()) {
            if (c.get().equivalent(listElement)) {
               this.remove(c.property(), cx);
               return;
            }
         }
      }
   }

   static class RecipientListManipulation extends ListManipulation {
      RecipientListManipulation(BBacnetObject o, Property p, BValue v, boolean add) {
         super(o, p, v, add);
      }

      @Override
      protected boolean checkEquals(BObject o, BValue v) {
         return o.getType().is(BBacnetDestination.TYPE) && v.getType().is(BBacnetDestination.TYPE)
            ? ((BBacnetDestination)o).destinationEquals((BBacnetDestination)v)
            : false;
      }
   }
}
