package com.tridium.bacnet.schedule;

import javax.baja.bacnet.datatypes.BBacnetAny;
import javax.baja.bacnet.datatypes.BBacnetArray;
import javax.baja.bacnet.datatypes.BBacnetDailySchedule;
import javax.baja.bacnet.datatypes.BBacnetListOf;
import javax.baja.bacnet.datatypes.BBacnetObjectPropertyReference;
import javax.baja.bacnet.datatypes.BBacnetSpecialEvent;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "dataType",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "supervisorOrd",
      type = "BOrd",
      defaultValue = "BOrd.DEFAULT",
      facets = {@Facet(
         name = "BFacets.TARGET_TYPE",
         value = "\"schedule:AbstractSchedule\""
      )}
   ), @NiagaraProperty(
      name = "listOfObjectPropertyRefs",
      type = "BBacnetListOf",
      defaultValue = "new BBacnetListOf(BBacnetObjectPropertyReference.TYPE)"
   ), @NiagaraProperty(
      name = "scheduleDefault",
      type = "BBacnetAny",
      defaultValue = "new BBacnetAny()"
   ), @NiagaraProperty(
      name = "hasWeeklySchedule",
      type = "boolean",
      defaultValue = "true"
   ), @NiagaraProperty(
      name = "weeklySchedule",
      type = "BBacnetArray",
      defaultValue = "new BBacnetArray(BBacnetDailySchedule.TYPE, 7)"
   ), @NiagaraProperty(
      name = "hasExceptionSchedule",
      type = "boolean",
      defaultValue = "true"
   ), @NiagaraProperty(
      name = "exceptionSchedule",
      type = "BBacnetArray",
      defaultValue = "new BBacnetArray(BBacnetSpecialEvent.TYPE)"
   ), @NiagaraProperty(
      name = "typeChanged",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "superChanged",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "oprChanged",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "schedDefChanged",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "weeklyChanged",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "exceptionChanged",
      type = "boolean",
      defaultValue = "false"
   )})
public class BBacnetChangeTypeParm extends BComponent {
   public static final Property dataType = newProperty(0, "", null);
   public static final Property supervisorOrd = newProperty(0, BOrd.DEFAULT, BFacets.make("targetType", "schedule:AbstractSchedule"));
   public static final Property listOfObjectPropertyRefs = newProperty(0, new BBacnetListOf(BBacnetObjectPropertyReference.TYPE), null);
   public static final Property scheduleDefault = newProperty(0, new BBacnetAny(), null);
   public static final Property hasWeeklySchedule = newProperty(0, true, null);
   public static final Property weeklySchedule = newProperty(0, new BBacnetArray(BBacnetDailySchedule.TYPE, 7), null);
   public static final Property hasExceptionSchedule = newProperty(0, true, null);
   public static final Property exceptionSchedule = newProperty(0, new BBacnetArray(BBacnetSpecialEvent.TYPE), null);
   public static final Property typeChanged = newProperty(0, false, null);
   public static final Property superChanged = newProperty(0, false, null);
   public static final Property oprChanged = newProperty(0, false, null);
   public static final Property schedDefChanged = newProperty(0, false, null);
   public static final Property weeklyChanged = newProperty(0, false, null);
   public static final Property exceptionChanged = newProperty(0, false, null);
   public static final Type TYPE = Sys.loadType(BBacnetChangeTypeParm.class);

   public String getDataType() {
      return this.getString(dataType);
   }

   public void setDataType(String v) {
      this.setString(dataType, v, null);
   }

   public BOrd getSupervisorOrd() {
      return (BOrd)this.get(supervisorOrd);
   }

   public void setSupervisorOrd(BOrd v) {
      this.set(supervisorOrd, v, null);
   }

   public BBacnetListOf getListOfObjectPropertyRefs() {
      return (BBacnetListOf)this.get(listOfObjectPropertyRefs);
   }

   public void setListOfObjectPropertyRefs(BBacnetListOf v) {
      this.set(listOfObjectPropertyRefs, v, null);
   }

   public BBacnetAny getScheduleDefault() {
      return (BBacnetAny)this.get(scheduleDefault);
   }

   public void setScheduleDefault(BBacnetAny v) {
      this.set(scheduleDefault, v, null);
   }

   public boolean getHasWeeklySchedule() {
      return this.getBoolean(hasWeeklySchedule);
   }

   public void setHasWeeklySchedule(boolean v) {
      this.setBoolean(hasWeeklySchedule, v, null);
   }

   public BBacnetArray getWeeklySchedule() {
      return (BBacnetArray)this.get(weeklySchedule);
   }

   public void setWeeklySchedule(BBacnetArray v) {
      this.set(weeklySchedule, v, null);
   }

   public boolean getHasExceptionSchedule() {
      return this.getBoolean(hasExceptionSchedule);
   }

   public void setHasExceptionSchedule(boolean v) {
      this.setBoolean(hasExceptionSchedule, v, null);
   }

   public BBacnetArray getExceptionSchedule() {
      return (BBacnetArray)this.get(exceptionSchedule);
   }

   public void setExceptionSchedule(BBacnetArray v) {
      this.set(exceptionSchedule, v, null);
   }

   public boolean getTypeChanged() {
      return this.getBoolean(typeChanged);
   }

   public void setTypeChanged(boolean v) {
      this.setBoolean(typeChanged, v, null);
   }

   public boolean getSuperChanged() {
      return this.getBoolean(superChanged);
   }

   public void setSuperChanged(boolean v) {
      this.setBoolean(superChanged, v, null);
   }

   public boolean getOprChanged() {
      return this.getBoolean(oprChanged);
   }

   public void setOprChanged(boolean v) {
      this.setBoolean(oprChanged, v, null);
   }

   public boolean getSchedDefChanged() {
      return this.getBoolean(schedDefChanged);
   }

   public void setSchedDefChanged(boolean v) {
      this.setBoolean(schedDefChanged, v, null);
   }

   public boolean getWeeklyChanged() {
      return this.getBoolean(weeklyChanged);
   }

   public void setWeeklyChanged(boolean v) {
      this.setBoolean(weeklyChanged, v, null);
   }

   public boolean getExceptionChanged() {
      return this.getBoolean(exceptionChanged);
   }

   public void setExceptionChanged(boolean v) {
      this.setBoolean(exceptionChanged, v, null);
   }

   public Type getType() {
      return TYPE;
   }
}
