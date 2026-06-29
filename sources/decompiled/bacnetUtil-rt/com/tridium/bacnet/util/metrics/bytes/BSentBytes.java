package com.tridium.bacnet.util.metrics.bytes;

import com.tridium.bacnet.stack.network.NetworkPdu;
import com.tridium.bacnet.stack.network.wiretap.OutgoingWiretap;
import com.tridium.bacnet.stack.network.wiretap.WiretapAware;
import com.tridium.bacnet.util.point.BPooledWorkerPoint;
import com.tridium.bacnet.util.point.EventsPerSecond;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.LongAdder;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.units.BUnit;

@NiagaraType
@NiagaraProperty(
   name = "facets",
   type = "BFacets",
   defaultValue = "BFacets.makeNumeric(BUnit.getUnit(\"bytes per second\"), 1)",
   override = true
)
public class BSentBytes extends BPooledWorkerPoint implements OutgoingWiretap, EventsPerSecond {
   public static final Property facets = newProperty(0, BFacets.makeNumeric(BUnit.getUnit("bytes per second"), 1), null);
   public static final Type TYPE = Sys.loadType(BSentBytes.class);
   private LongAdder sent = new LongAdder();
   private long lastUpdate = System.currentTimeMillis();

   @Override
   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof WiretapAware;
   }

   public void onExecute(BStatusValue o, Context cx) {
      long t1 = this.lastUpdate;
      long bytesSent = this.sent.longValue();
      this.sent.reset();
      this.lastUpdate = System.currentTimeMillis();
      BStatusNumeric out = (BStatusNumeric)o;
      out.setValue(this.calculateEventsPerSecond(bytesSent, t1, this.lastUpdate));
   }

   public void sendRequest(byte[] destAddress, NetworkPdu npdu) {
      this.run(() -> {
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         npdu.writeNetworkBytes(baos);
         this.sent.add(baos.size());
      });
   }

   public long getSentBytes() {
      return this.sent.longValue();
   }
}
