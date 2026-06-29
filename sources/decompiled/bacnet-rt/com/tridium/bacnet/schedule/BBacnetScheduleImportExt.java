package com.tridium.bacnet.schedule;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.driver.schedule.BScheduleImportExt;
import javax.baja.driver.util.BDescriptorState;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.schedule.BAbstractSchedule;
import javax.baja.sys.Action;
import javax.baja.sys.BFacets;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.IFuture;
import javax.baja.util.Invocation;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "supervisorId",
      type = "String",
      defaultValue = "",
      flags = 5,
      override = true
   ), @NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.SCHEDULE)",
      facets = {@Facet("ScheduleSupport0.SCHEDULE_CALENDAR_OBJECT_ID_FACETS")}
   ), @NiagaraProperty(
      name = "priorityForWriting",
      type = "int",
      defaultValue = "16",
      flags = 1,
      facets = {@Facet("BFacets.makeInt(1,16)")}
   )})
public class BBacnetScheduleImportExt extends BScheduleImportExt {
   public static final Property supervisorId = newProperty(5, "", null);
   public static final Property objectId = newProperty(0, BBacnetObjectIdentifier.make(17), ScheduleSupport0.SCHEDULE_CALENDAR_OBJECT_ID_FACETS);
   public static final Property priorityForWriting = newProperty(1, 16, BFacets.makeInt(1, 16));
   public static final Type TYPE = Sys.loadType(BBacnetScheduleImportExt.class);
   public static final Logger logger = Logger.getLogger("bacnet.schedule");

   public BBacnetObjectIdentifier getObjectId() {
      return (BBacnetObjectIdentifier)this.get(objectId);
   }

   public void setObjectId(BBacnetObjectIdentifier v) {
      this.set(objectId, v, null);
   }

   public int getPriorityForWriting() {
      return this.getInt(priorityForWriting);
   }

   public void setPriorityForWriting(int v) {
      this.setInt(priorityForWriting, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public String toString(Context cx) {
      String parentName = this.getParent() != null ? this.getParent().getName() : "Unparented";
      return parentName + "-ImportExt [" + this.getObjectId().toString(cx) + "]";
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (cx != Context.decoding && this.isRunning()) {
         if (p == objectId) {
            this.execute();
         }
      }
   }

   protected IFuture postExecute(Action action, BValue arg, Context cx) {
      return BBacnetNetwork.bacnet().postAsync(new Invocation(this, action, arg, cx));
   }

   public void doExecute() {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("doExecute on " + this);
      }

      BBacnetScheduleDeviceExt deviceExt = (BBacnetScheduleDeviceExt)this.getDeviceExt();
      this.executeInProgress();

      try {
         BAbstractSchedule ret = deviceExt.readRemote(this);
         if (ret != null) {
            this.importSupervisor(ret);
         }

         this.executeOk();
      } catch (BacnetException var7) {
         logger.log(
            Level.SEVERE,
            "BacnetException reading supervisor schedule data for " + this.getSubordinate().getName() + " from " + this.getObjectId() + ": " + var7,
            (Throwable)var7
         );
         this.executeFail(var7);
      } catch (Exception var8) {
         logger.log(
            Level.SEVERE,
            "Exception reading supervisor schedule data for " + this.getSubordinate().getName() + " from " + this.getObjectId() + ": " + var8,
            (Throwable)var8
         );
         this.executeFail(var8);
      } finally {
         this.setState(BDescriptorState.idle);
      }
   }
}
