package com.tridium.bacnet.util.metrics.latency;

import com.tridium.bacnet.util.point.BPooledWorkerPoint;
import java.util.concurrent.atomic.LongAdder;
import javax.baja.bacnet.device.LatencyRecorder;
import javax.baja.bacnet.device.LatencyRecorderAware;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusValue;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.units.BUnit;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "facets",
      type = "BFacets",
      defaultValue = "BFacets.makeNumeric(BUnit.getUnit(\"millisecond\"), 1)",
      override = true
   ), @NiagaraProperty(
      name = "min",
      type = "long",
      defaultValue = "0"
   ), @NiagaraProperty(
      name = "max",
      type = "long",
      defaultValue = "0"
   ), @NiagaraProperty(
      name = "count",
      type = "long",
      defaultValue = "0"
   ), @NiagaraProperty(
      name = "alpha",
      type = "double",
      defaultValue = "0.5"
   )})
@NiagaraAction(
   name = "reset"
)
public class BAverageLatency extends BPooledWorkerPoint implements LatencyRecorder {
   public static final Property facets = newProperty(0, BFacets.makeNumeric(BUnit.getUnit("millisecond"), 1), null);
   public static final Property min = newProperty(0, 0, null);
   public static final Property max = newProperty(0, 0, null);
   public static final Property count = newProperty(0, 0, null);
   public static final Property alpha = newProperty(0, 0.5, null);
   public static final Action reset = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BAverageLatency.class);
   private long minValue = Long.MAX_VALUE;
   private long maxValue = Long.MIN_VALUE;
   private volatile double averageLatency = 0.0;
   private volatile double alphaVal = 0.0;
   private LongAdder numberOfEvents = new LongAdder();

   public long getMin() {
      return this.getLong(min);
   }

   public void setMin(long v) {
      this.setLong(min, v, null);
   }

   public long getMax() {
      return this.getLong(max);
   }

   public void setMax(long v) {
      this.setLong(max, v, null);
   }

   public long getCount() {
      return this.getLong(count);
   }

   public void setCount(long v) {
      this.setLong(count, v, null);
   }

   public double getAlpha() {
      return this.getDouble(alpha);
   }

   public void setAlpha(double v) {
      this.setDouble(alpha, v, null);
   }

   public void reset() {
      this.invoke(reset, null, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void started() throws Exception {
      super.started();
      LatencyRecorderAware lr = this.getRecorder();
      if (lr != null) {
         lr.addLatencyRecorder(this);
      }

      this.alphaVal = this.getAlpha();
   }

   @Override
   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (p != null && p.equals(alpha)) {
         this.alphaVal = this.getAlpha();
      }
   }

   @Override
   public void stopped() throws Exception {
      super.stopped();
      LatencyRecorderAware lr = this.getRecorder();
      if (lr != null) {
         lr.addLatencyRecorder(this);
      }
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof LatencyRecorderAware;
   }

   protected LatencyRecorderAware getRecorder() {
      return (LatencyRecorderAware)this.getParent();
   }

   public boolean isRecordingLatency() {
      return this.getEnabled();
   }

   public void recordLatency(long ms) {
      this.minValue = Math.min(ms, this.minValue);
      this.maxValue = Math.max(ms, this.maxValue);
      if (this.numberOfEvents.longValue() == 0L) {
         this.averageLatency = ms;
      } else {
         this.averageLatency = ms * this.alphaVal + (1.0 - this.alphaVal) * this.averageLatency;
      }

      this.numberOfEvents.increment();
   }

   public void onExecute(BStatusValue o, Context cx) {
      BStatusNumeric out = (BStatusNumeric)o;
      long events = this.numberOfEvents.longValue();
      if (events > 0L) {
         out.setValue(this.averageLatency);
         this.setCount(events);
         if (this.maxValue != Long.MIN_VALUE && this.getMax() != this.maxValue) {
            this.setMax(this.maxValue);
         }

         if (this.minValue != Long.MAX_VALUE && this.getMin() != this.minValue) {
            this.setMin(this.minValue);
         }
      }
   }

   public void doReset() {
      this.averageLatency = 0.0;
      this.numberOfEvents.reset();
      this.minValue = Long.MAX_VALUE;
      this.maxValue = Long.MIN_VALUE;
   }
}
