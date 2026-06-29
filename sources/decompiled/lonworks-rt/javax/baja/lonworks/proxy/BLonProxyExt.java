package javax.baja.lonworks.proxy;

import com.tridium.lonworks.util.NmUtil;
import javax.baja.driver.point.BProxyExt;
import javax.baja.driver.point.BReadWriteMode;
import javax.baja.driver.point.BTuningPolicy;
import javax.baja.lonworks.BLonComponent;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.lonworks.BNetworkVariable;
import javax.baja.lonworks.enums.BLonLinkType;
import javax.baja.lonworks.londata.BLonData;
import javax.baja.lonworks.londata.BLonPrimitive;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdTarget;
import javax.baja.naming.UnresolvedException;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.spy.SpyWriter;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.Context;
import javax.baja.sys.Flags;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.Queue;
import javax.baja.util.Worker;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "targetComp",
      type = "String",
      defaultValue = "value",
      flags = 8
   ), @NiagaraProperty(
      name = "targetName",
      type = "String",
      defaultValue = "value",
      flags = 8
   ), @NiagaraProperty(
      name = "linkType",
      type = "BLonLinkType",
      defaultValue = "BLonLinkType.standard"
   ), @NiagaraProperty(
      name = "priority",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "tuningPolicyName",
      type = "String",
      defaultValue = "defaultProxyPolicy",
      flags = 5,
      override = true
   ), @NiagaraProperty(
      name = "status",
      type = "BStatus",
      defaultValue = "BStatus.ok",
      flags = 3,
      override = true
   ), @NiagaraProperty(
      name = "deviceFacets",
      type = "BFacets",
      defaultValue = "BFacets.NULL",
      flags = 1,
      override = true
   )})
public abstract class BLonProxyExt extends BProxyExt {
   public static final Property targetComp = newProperty(8, "value", null);
   public static final Property targetName = newProperty(8, "value", null);
   public static final Property linkType = newProperty(0, BLonLinkType.standard, null);
   public static final Property priority = newProperty(0, false, null);
   public static final Property tuningPolicyName = newProperty(5, "defaultProxyPolicy", null);
   public static final Property status = newProperty(3, BStatus.ok, null);
   public static final Property deviceFacets = newProperty(1, BFacets.NULL, null);
   public static final Type TYPE = Sys.loadType(BLonProxyExt.class);
   Worker wrkr = null;
   BLonProxyExt.ProxyRequest req = null;
   private static final String RESOLVE_FAULT_MSG = "Can not resolve target: ";
   protected BLonData datPnt = null;
   protected Property targetProp = null;
   private boolean isDataPointWriteable = true;
   private int dataPntMismatchCount = 0;
   private boolean dataPntSubscribed = false;

   public String getTargetComp() {
      return this.getString(targetComp);
   }

   public void setTargetComp(String v) {
      this.setString(targetComp, v, null);
   }

   public String getTargetName() {
      return this.getString(targetName);
   }

   public void setTargetName(String v) {
      this.setString(targetName, v, null);
   }

   public BLonLinkType getLinkType() {
      return (BLonLinkType)this.get(linkType);
   }

   public void setLinkType(BLonLinkType v) {
      this.set(linkType, v, null);
   }

   public boolean getPriority() {
      return this.getBoolean(priority);
   }

   public void setPriority(boolean v) {
      this.setBoolean(priority, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BLonProxyExt() {
   }

   public BLonProxyExt(OrdTarget ordTgt) {
      BComponent comp = ordTgt.getComponent();
      BLonDevice dev = this.getLonDevice(comp);
      String tgtCmp = comp.getSlotPath().toString().substring(dev.getSlotPath().toString().length() + 1);
      this.setTargetComp(tgtCmp);
      this.setTargetName(ordTgt.getSlotInComponent().getName());
   }

   private BLonDevice getLonDevice(BComponent c) {
      while (!(c instanceof BLonDevice)) {
         c = (BComponent)c.getParent();
      }

      return (BLonDevice)c;
   }

   public Type getDeviceExtType() {
      return BLonPointDeviceExt.TYPE;
   }

   public BReadWriteMode getMode() {
      return this.isDataPointWriteable ? BReadWriteMode.writeonly : BReadWriteMode.readonly;
   }

   public final void readSubscribed(Context cx) throws Exception {
      BLonData dataPoint = this.getDataPoint();
      if (dataPoint != null && !this.dataPntSubscribed) {
         dataPoint.readSubscribed();
         this.dataPntSubscribed = true;
      }
   }

   public final void readUnsubscribed(Context cx) throws Exception {
      BLonData dataPoint = this.datPnt;
      if (dataPoint != null && this.dataPntSubscribed) {
         dataPoint.readUnsubscribed();
         this.dataPntSubscribed = false;
      }
   }

   public boolean write(Context cx) throws Exception {
      if (this.isDataPointWriteable && !this.isUnoperational()) {
         Worker w = this.getProxyQueue();
         if (w != null && w.isRunning()) {
            try {
               ((Queue)w.getTodo()).enqueue(this.getRequest(cx));
            } catch (Throwable var4) {
               this.doProxyWrite(cx);
            }

            return false;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public final void forceUpdate() {
      this.doProxyWrite(BLonNetwork.lonNoWrite);
   }

   private void doProxyWrite(Context cx) {
      if (!this.isUnoperational()) {
         try {
            BStatusValue value = this.getWriteValue();
            if (value.getStatus().isNull()) {
               return;
            }

            BLonData dataPoint = this.getDataPoint();
            if (dataPoint == null) {
               return;
            }

            BLonPrimitive prim = this.getPrimitiveValue(value);
            if (prim == null) {
               this.writeFail("Unable to convert status value to lonPrimitive.");
               return;
            }

            boolean stateWrite = cx == BTuningPolicy.maxWriteTimeContext
               || cx == BTuningPolicy.writeOnStartContext
               || cx == BTuningPolicy.writeOnUpContext
               || cx == BTuningPolicy.writeOnEnabledContext;
            if (!stateWrite) {
               dataPoint.set(this.targetProp, prim, cx);
            }
         } catch (Throwable var6) {
            this.writeFail(var6.toString());
         }
      }
   }

   private Worker getProxyQueue() {
      if (this.wrkr == null) {
         this.wrkr = ((BLonNetwork)((BLonDevice)this.getDevice()).getNetwork()).getProxyQueue();
      }

      return this.wrkr;
   }

   private Runnable getRequest(Context cx) {
      if (this.req == null) {
         this.req = new BLonProxyExt.ProxyRequest();
      }

      this.req.setContext(cx);
      return this.req;
   }

   public final void started() throws Exception {
      super.started();
      this.setStale(false, null);
      this.registerProxy(this.getDataPoint());
      this.checkIfWriteable();
      this.extStarted();
      if (this.getLinkType() == BLonLinkType.unknown) {
         BLonComponent lc = this.getLonComponent();
         if (lc != null && lc.isNetworkVariable()) {
            this.setLinkType(NmUtil.getLinkType(((BNetworkVariable)lc).getNvConfigData()));
         }
      }
   }

   protected void extStarted() {
      this.setReadValue(this.getStatusValue());
   }

   public final void stopped() throws Exception {
      try {
         this.unregisterProxy();
      } catch (Throwable var2) {
      }

      super.stopped();
   }

   private void registerProxy(BLonData dataPoint) {
      if (dataPoint != null) {
         if (this.isSubscribedDesired() && !this.dataPntSubscribed) {
            dataPoint.readSubscribed();
            this.dataPntSubscribed = true;
         }

         dataPoint.registerProxyExt(this);
      }
   }

   private void unregisterProxy() {
      BLonData dataPoint = this.datPnt;
      if (dataPoint != null) {
         if (this.dataPntSubscribed) {
            dataPoint.readUnsubscribed();
            this.dataPntSubscribed = false;
         }

         dataPoint.unregisterProxyExt(this);
      }
   }

   public boolean isReadWriteProxy() {
      return false;
   }

   public void changed(Property prop, Context context) {
      try {
         if (this.isRunning()) {
            if (prop == targetName || prop == targetComp) {
               this.renew();
            }

            return;
         }
      } finally {
         super.changed(prop, context);
      }
   }

   public BLonComponent getLonComponent() {
      BLonData dataPoint = this.datPnt;
      return dataPoint == null ? null : dataPoint.getLonComponent();
   }

   public void renew() {
      this.renew(false);
   }

   public void renew(boolean updateFacets) {
      this.unregisterProxy();
      this.datPnt = null;
      if (updateFacets) {
         this.deviceFacetsChanged();
      }

      this.registerProxy(this.getDataPoint());
   }

   protected void deviceFacetsChanged() {
   }

   public BLonData getDataPoint() {
      BLonData orig = this.datPnt;
      this.resolveProxyLink();
      if (orig != null && this.datPnt != null && orig != this.datPnt) {
         System.out
            .println(
               NmUtil.timeStamp()
                  + " resolveProxyLink found new object orig="
                  + orig.getHandle()
                  + " isMounted="
                  + orig.isMounted()
                  + " new="
                  + this.datPnt.getHandle()
                  + " isMounted="
                  + this.datPnt.isMounted()
            );
         ((BLonNetwork)this.getLonDevice(this).getNetwork()).dataPntMismatchCount++;
         this.dataPntMismatchCount++;
         this.registerProxy(this.datPnt);
      }

      return this.datPnt;
   }

   private void resolveProxyLink() {
      BComponent refPoint = null;

      try {
         BLonDevice dev = this.getLonDevice(this);
         BOrd tgtOrd = BOrd.make("slot:" + this.getTargetComp());
         refPoint = (BComponent)tgtOrd.get(dev);
         this.targetProp = refPoint.getProperty(this.getTargetName());
      } catch (Throwable var4) {
         refPoint = null;
      }

      if (refPoint != null && this.targetProp != null) {
         if (this.getStatus().isFault() && this.getFaultCause().indexOf("Can not resolve target: ") >= 0) {
            this.setFaultCause("");
            this.setStatus(BStatus.ok);
            this.getTuning().writeDesired();
         }

         this.datPnt = (BLonData)refPoint;
      } else {
         if (this.datPnt != null) {
            System.out
               .println(
                  "\nunable to re-resolve proxy reference for "
                     + this.getDevice().getDisplayName(null)
                     + ":"
                     + this.getParent().getDisplayName(null)
                     + ":"
                     + this.datPnt.getDisplayName(null)
               );
            System.out.println("getTargetComp()=" + this.getTargetComp() + " getTargetName()=" + this.getTargetName());
            System.out.println("previous datPnt.isMounted()=" + this.datPnt.isMounted() + " handle=" + this.datPnt.getHandle());
            System.out.println();
         }

         this.datPnt = null;
         this.setFaultCause("Can not resolve target: " + this.toString(null));
         this.setStatus(BStatus.fault);
         this.executePoint();
         throw new UnresolvedException("Cannot resolve proxyLink in " + this.getDevice().getDisplayName(null) + ":" + this.getParent().getDisplayName(null));
      }
   }

   protected abstract BLonPrimitive getPrimitiveValue(BStatusValue var1);

   public final BStatusValue getStatusValue() {
      BLonPrimitive target = this.getTarget();
      return target == null ? null : this.getStatusValue(target);
   }

   protected final BLonPrimitive getTarget() {
      BLonData dataPoint = this.getDataPoint();
      if (dataPoint != null && this.targetProp != null) {
         try {
            return (BLonPrimitive)dataPoint.get(this.targetProp);
         } catch (Exception var4) {
            System.out.println("ERROR: no property " + this.targetProp.getName() + " in " + dataPoint.getDisplayName(null));
            return null;
         }
      } else {
         return null;
      }
   }

   public abstract BStatusValue getStatusValue(BLonPrimitive var1);

   public String toString(Context c) {
      return this.getTargetComp() + "/" + this.getTargetName();
   }

   private void checkIfWriteable() {
      BLonComponent lc = this.getLonComponent();
      this.isDataPointWriteable = this.getParentPoint().isWritablePoint() && lc.isWriteable();
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      BLonData dataPoint = this.datPnt;
      out.startProps();
      out.trTitle("LonProxyExt", 2);
      BLonComponent lc = this.getLonComponent();
      out.prop("lonComponent", lc == null ? "null" : lc.getName());
      if (dataPoint == null) {
         out.prop("dataPoint", "null");
      } else {
         out.prop("dataPoint", dataPoint.getName());
         out.prop("dataPoint.handle", dataPoint.getHandle());
         out.prop("dataPoint.isMounted", dataPoint.isMounted());
         String name = dataPoint.getName();
         this.spySlot_(out, name, dataPoint.getSlotPath().getBody(), this.getFlags(this.targetProp), dataPoint);
      }

      out.prop("targetProp", this.targetProp == null ? "null" : this.targetProp.getName());
      out.prop("isDataPointWriteable", this.isDataPointWriteable);
      if (dataPoint != null && this.targetProp != null) {
         out.prop("primitive val", dataPoint.get(this.targetProp).toString(null));
      }

      out.prop("dataPntMismatchCount", this.dataPntMismatchCount);
      out.prop("dataPointSubscribed", this.dataPntSubscribed);
      out.endProps();
   }

   private void spySlot_(SpyWriter out, String name, String slotPath, int flags, Object value) {
      out.w("<tr><td align='left' nowrap='true'><b>").a("/nav/localhost/station" + slotPath, name).w("</b>");
      if (flags == 0) {
         out.w("{0}");
      } else {
         out.w(" {").w(Flags.encodeToString(flags)).w("}");
      }

      out.w("</td><td align='left' nowrap='true'>").w(value).w("</td></tr>\n");
   }

   private class ProxyRequest implements Runnable {
      Context cx = null;

      private ProxyRequest() {
      }

      @Override
      public void run() {
         BLonProxyExt.this.doProxyWrite(this.cx);
      }

      public void setContext(Context cx) {
         this.cx = cx;
      }
   }
}
