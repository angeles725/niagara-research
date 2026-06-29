package javax.baja.bacnet.point;

import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.driver.point.BTuningPolicy;
import javax.baja.driver.util.BPollFrequency;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.units.UnitDatabase;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "pollFrequency",
      type = "BPollFrequency",
      defaultValue = "BPollFrequency.normal"
   ), @NiagaraProperty(
      name = "useCov",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "useConfirmedCov",
      type = "boolean",
      defaultValue = "true"
   ), @NiagaraProperty(
      name = "covSubscriptionLifetime",
      type = "int",
      defaultValue = "15",
      facets = {@Facet("BFacets.makeInt(UnitDatabase.getUnit(\"minute\"))")}
   ), @NiagaraProperty(
      name = "useCovProperty",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "useConfirmedCovProperty",
      type = "boolean",
      defaultValue = "true"
   ), @NiagaraProperty(
      name = "covPropertyIncrement",
      type = "double",
      defaultValue = "1.0"
   ), @NiagaraProperty(
      name = "covPropertySubscriptionLifetime",
      type = "int",
      defaultValue = "15",
      facets = {@Facet("BFacets.makeInt(UnitDatabase.getUnit(\"minute\"))")}
   ), @NiagaraProperty(
      name = "acceptUnsolicitedCov",
      type = "boolean",
      defaultValue = "false"
   )})
public class BBacnetTuningPolicy extends BTuningPolicy {
   public static final Property pollFrequency = newProperty(0, BPollFrequency.normal, null);
   public static final Property useCov = newProperty(0, false, null);
   public static final Property useConfirmedCov = newProperty(0, true, null);
   public static final Property covSubscriptionLifetime = newProperty(0, 15, BFacets.makeInt(UnitDatabase.getUnit("minute")));
   public static final Property useCovProperty = newProperty(0, false, null);
   public static final Property useConfirmedCovProperty = newProperty(0, true, null);
   public static final Property covPropertyIncrement = newProperty(0, 1.0, null);
   public static final Property covPropertySubscriptionLifetime = newProperty(0, 15, BFacets.makeInt(UnitDatabase.getUnit("minute")));
   public static final Property acceptUnsolicitedCov = newProperty(0, false, null);
   public static final Type TYPE = Sys.loadType(BBacnetTuningPolicy.class);

   public BPollFrequency getPollFrequency() {
      return (BPollFrequency)this.get(pollFrequency);
   }

   public void setPollFrequency(BPollFrequency v) {
      this.set(pollFrequency, v, null);
   }

   public boolean getUseCov() {
      return this.getBoolean(useCov);
   }

   public void setUseCov(boolean v) {
      this.setBoolean(useCov, v, null);
   }

   public boolean getUseConfirmedCov() {
      return this.getBoolean(useConfirmedCov);
   }

   public void setUseConfirmedCov(boolean v) {
      this.setBoolean(useConfirmedCov, v, null);
   }

   public int getCovSubscriptionLifetime() {
      return this.getInt(covSubscriptionLifetime);
   }

   public void setCovSubscriptionLifetime(int v) {
      this.setInt(covSubscriptionLifetime, v, null);
   }

   public boolean getUseCovProperty() {
      return this.getBoolean(useCovProperty);
   }

   public void setUseCovProperty(boolean v) {
      this.setBoolean(useCovProperty, v, null);
   }

   public boolean getUseConfirmedCovProperty() {
      return this.getBoolean(useConfirmedCovProperty);
   }

   public void setUseConfirmedCovProperty(boolean v) {
      this.setBoolean(useConfirmedCovProperty, v, null);
   }

   public double getCovPropertyIncrement() {
      return this.getDouble(covPropertyIncrement);
   }

   public void setCovPropertyIncrement(double v) {
      this.setDouble(covPropertyIncrement, v, null);
   }

   public int getCovPropertySubscriptionLifetime() {
      return this.getInt(covPropertySubscriptionLifetime);
   }

   public void setCovPropertySubscriptionLifetime(int v) {
      this.setInt(covPropertySubscriptionLifetime, v, null);
   }

   public boolean getAcceptUnsolicitedCov() {
      return this.getBoolean(acceptUnsolicitedCov);
   }

   public void setAcceptUnsolicitedCov(boolean v) {
      this.setBoolean(acceptUnsolicitedCov, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BBacnetTuningPolicyMap;
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning()) {
         if (p.equals(useCov) || p.equals(useCovProperty)) {
            ((BBacnetNetwork)((BBacnetTuningPolicyMap)this.getParent()).getNetwork()).tuningChanged(this, cx);
         }
      }
   }
}
