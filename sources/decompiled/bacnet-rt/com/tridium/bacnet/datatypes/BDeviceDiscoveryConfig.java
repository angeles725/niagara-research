package com.tridium.bacnet.datatypes;

import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BFacets;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.units.UnitDatabase;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "deviceLowLimit",
      type = "int",
      defaultValue = "BBacnetObjectIdentifier.MIN_INSTANCE_NUMBER"
   ), @NiagaraProperty(
      name = "deviceHighLimit",
      type = "int",
      defaultValue = "BBacnetObjectIdentifier.MAX_INSTANCE_NUMBER"
   ), @NiagaraProperty(
      name = "networks",
      type = "BDiscoveryNetworks",
      defaultValue = "BDiscoveryNetworks.DEFAULT"
   ), @NiagaraProperty(
      name = "waitResponseTime",
      type = "int",
      defaultValue = "10",
      facets = {@Facet("BFacets.makeInt(UnitDatabase.getUnit(\"second\"))")}
   )})
public class BDeviceDiscoveryConfig extends BRequestConfig {
   public static final Property deviceLowLimit = newProperty(0, 0, null);
   public static final Property deviceHighLimit = newProperty(0, 4194302, null);
   public static final Property networks = newProperty(0, BDiscoveryNetworks.DEFAULT, null);
   public static final Property waitResponseTime = newProperty(0, 10, BFacets.makeInt(UnitDatabase.getUnit("second")));
   public static final Type TYPE = Sys.loadType(BDeviceDiscoveryConfig.class);

   public int getDeviceLowLimit() {
      return this.getInt(deviceLowLimit);
   }

   public void setDeviceLowLimit(int v) {
      this.setInt(deviceLowLimit, v, null);
   }

   public int getDeviceHighLimit() {
      return this.getInt(deviceHighLimit);
   }

   public void setDeviceHighLimit(int v) {
      this.setInt(deviceHighLimit, v, null);
   }

   public BDiscoveryNetworks getNetworks() {
      return (BDiscoveryNetworks)this.get(networks);
   }

   public void setNetworks(BDiscoveryNetworks v) {
      this.set(networks, v, null);
   }

   public int getWaitResponseTime() {
      return this.getInt(waitResponseTime);
   }

   public void setWaitResponseTime(int v) {
      this.setInt(waitResponseTime, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public boolean isDefaultRange() {
      return this.getDeviceLowLimit() == 0 && this.getDeviceHighLimit() == 4194302;
   }
}
