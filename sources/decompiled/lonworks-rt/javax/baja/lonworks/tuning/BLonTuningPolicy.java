package javax.baja.lonworks.tuning;

import javax.baja.driver.point.BTuningPolicy;
import javax.baja.driver.util.BPollFrequency;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BInteger;
import javax.baja.sys.BRelTime;
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
      name = "writeDelay",
      type = "int",
      defaultValue = "0",
      facets = {@Facet("BFacets.make(BFacets.MIN,BInteger.make(0),BFacets.MAX,BInteger.make(1000),BFacets.UNITS,UnitDatabase.getUnit(\"millisecond\"))")}
   )})
public class BLonTuningPolicy extends BTuningPolicy {
   public static final Property pollFrequency = newProperty(0, BPollFrequency.normal, null);
   public static final Property writeDelay = newProperty(
      0, 0, BFacets.make("min", BInteger.make(0), "max", BInteger.make(1000), "units", UnitDatabase.getUnit("millisecond"))
   );
   public static final Type TYPE = Sys.loadType(BLonTuningPolicy.class);

   public BPollFrequency getPollFrequency() {
      return (BPollFrequency)this.get(pollFrequency);
   }

   public void setPollFrequency(BPollFrequency v) {
      this.set(pollFrequency, v, null);
   }

   public int getWriteDelay() {
      return this.getInt(writeDelay);
   }

   public void setWriteDelay(int v) {
      this.setInt(writeDelay, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BLonTuningPolicy() {
   }

   public BLonTuningPolicy(BRelTime minWriteTime, BRelTime maxWriteTime, boolean writeOnStart, boolean writeOnUp, boolean writeOnEnabled, BRelTime staleTime) {
      this.setMinWriteTime(minWriteTime);
      this.setMaxWriteTime(maxWriteTime);
      this.setWriteOnStart(writeOnStart);
      this.setWriteOnUp(writeOnUp);
      this.setWriteOnEnabled(writeOnEnabled);
      this.setStaleTime(staleTime);
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BLonTuningPolicyMap;
   }
}
