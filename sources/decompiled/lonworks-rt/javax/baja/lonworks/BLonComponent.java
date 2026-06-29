package javax.baja.lonworks;

import com.tridium.lonworks.Lon;
import javax.baja.lonworks.londata.BLonData;
import javax.baja.lonworks.londata.BLonPrimitive;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.BValue;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Context;
import javax.baja.sys.LocalizableRuntimeException;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.IFuture;
import javax.baja.util.Invocation;

@NiagaraType
@NiagaraActions({@NiagaraAction(
      name = "forceRead",
      flags = 16
   ), @NiagaraAction(
      name = "forceWrite",
      flags = 16
   ), @NiagaraAction(
      name = "update",
      parameterType = "BLonData",
      defaultValue = "new BLonData()",
      flags = 4
   )})
public abstract class BLonComponent extends BLonData {
   public static final Action forceRead = newAction(16, null);
   public static final Action forceWrite = newAction(16, null);
   public static final Action update = newAction(4, new BLonData(), null);
   public static final Type TYPE = Sys.loadType(BLonComponent.class);
   private BLonDevice dev = null;
   private BLonNetwork network = null;
   protected int subscribeCount = 0;
   public boolean illegalLength = false;

   public void forceRead() {
      this.invoke(forceRead, null, null);
   }

   public void forceWrite() {
      this.invoke(forceWrite, null, null);
   }

   public void update(BLonData parameter) {
      this.invoke(update, parameter, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BLonDevice || parent instanceof BLonObject;
   }

   public BLonDevice getDevice() {
      if (this.dev == null) {
         for (BComplex p = this; this.dev == null && p != null; p = p.getParent()) {
            if (p.getType().is(BLonDevice.TYPE)) {
               this.dev = (BLonDevice)p;
            }
         }
      }

      return this.dev;
   }

   private BLonNetwork getNetwork() {
      if (this.network != null) {
         return this.network;
      } else {
         BComplex p = this.getParent();

         while (!(p instanceof BLonNetwork)) {
            p = p.getParent();
         }

         this.network = (BLonNetwork)p;
         return this.network;
      }
   }

   public final void started() throws Exception {
      super.started();
      this.dev = null;
      this.network = null;
      if (!this.isWriteable()) {
         this.setFlags(forceWrite, this.getFlags(forceWrite) | 4);
      }

      if (this.isForeignPersistent()) {
         this.readOk();
      }

      if ((this.isNetworkVariable() || this.isNetworkConfig()) && this.getByteLength() > Lon.maxNvLength()) {
         this.getDevice().log().severe(this.getDisplayName(null) + " disabled because length " + this.getByteLength() + " > " + Lon.maxNvLength() + " bytes");
         this.illegalLength = true;
      }

      this.lonComponentStarted();
   }

   public final void stopped() throws Exception {
      super.stopped();
      this.lonComponentStopped();
      this.dev = null;
      this.network = null;
   }

   public void lonComponentStarted() {
   }

   public void lonComponentStopped() {
   }

   public boolean isNavChild() {
      return Lon.lcInNavTree();
   }

   public BLonData getData() {
      BObject dat = this.get("data");
      return (BLonData)(dat != null && dat instanceof BLonData ? (BLonData)dat : this);
   }

   public void setData(BLonData v) {
      String typName = v.getType().getTypeName();
      if (!typName.equals(BLonData.TYPE.getTypeName())) {
         if (this.get("data") != null) {
            this.set("data", v);
         } else {
            this.add("data", v);
         }
      } else {
         Property[] a = this.getPropertiesArray();

         for (int i = 0; i < a.length; i++) {
            if (isDataProp(a[i])) {
               this.remove(a[i]);
            }
         }

         a = v.getPropertiesArray();

         for (int ix = 0; ix < a.length; ix++) {
            Property prop = a[ix];
            if (isDataProp(prop)) {
               this.add(prop.getName(), v.get(prop), v.getFlags(prop), prop.getFacets(), null);
            }
         }
      }
   }

   public final BLonData copyData() {
      return (BLonData)this.getData().newCopy(true);
   }

   public final void updateData(BLonData data, boolean write) {
      if (this.isRunning()) {
         this.doUpdate(data);
         if (write) {
            this.doForceWrite();
         }
      } else {
         this.update(data);
         if (write) {
            this.forceWrite();
         }
      }
   }

   public final void doUpdate(BLonData dat) {
      BLonData myData = (BLonData)this.get("data");
      if (myData == null) {
         myData = this;
      }

      this.copyData(dat, myData);
      this.dataChanged(BLonNetwork.lonNoWrite);
   }

   private void copyData(BLonData src, BLonData dest) {
      Property[] psrc = src.getPropertiesArray();
      Property[] pdest = dest.getPropertiesArray();
      if (psrc.length != pdest.length) {
         throw new BajaRuntimeException("Unmatched Type in doUpdate " + this.getDisplayName(null));
      } else {
         for (int i = 0; i < psrc.length; i++) {
            if (psrc[i].getType() != pdest[i].getType()) {
               throw new BajaRuntimeException("Unmatched Type in doUpdate " + this.getDisplayName(null));
            }

            if (psrc[i].getType().is(BLonPrimitive.TYPE)) {
               dest.set(pdest[i], src.get(psrc[i]), BLonNetwork.lonNoPropagateNoWrite);
            } else if (psrc[i].getType().is(BLonData.TYPE)) {
               this.copyData((BLonData)src.get(psrc[i]), (BLonData)dest.get(pdest[i]));
            }
         }
      }
   }

   public IFuture post(Action action, BValue arg, Context cx) {
      if (action.equals(forceRead)) {
         return this.postForceRead(cx);
      } else {
         return action.equals(forceWrite) ? this.postForceWrite(cx) : super.post(action, arg, cx);
      }
   }

   protected IFuture postForceRead(Context cx) {
      return this.getNetwork().postAsync(new Invocation(this, forceRead, null, cx));
   }

   protected IFuture postForceWrite(Context cx) {
      if (!this.isWriteable()) {
         throw new LocalizableRuntimeException("lonworks", "lonComponent.notWritable");
      } else {
         return this.getNetwork().postWrite(new Invocation(this, forceWrite, null, cx));
      }
   }

   public abstract void doForceWrite();

   public abstract void doForceRead();

   public boolean isForeignPersistent() {
      return false;
   }

   public boolean isWriteable() {
      return true;
   }

   public BLonDevice lonDevice() {
      return this.getDevice();
   }

   public BLonNetwork lonNetwork() {
      return this.getNetwork();
   }

   public boolean isNetworkVariable() {
      return false;
   }

   public boolean isNetworkConfig() {
      return false;
   }

   public boolean isConfigParameter() {
      return false;
   }

   public boolean isLocalNv() {
      return false;
   }

   public boolean isLocalNci() {
      return false;
   }

   @Override
   public void subscribed() {
      this.readSubscribed();
   }

   @Override
   public void unsubscribed() {
      this.readUnsubscribed();
   }

   @Override
   public void readSubscribed() {
      this.subscribeCount++;
      if (this.subscribeCount == 1) {
         this.lonComponentSubscribed();
      }
   }

   public String debugName() {
      return this.getParent().getDisplayName(null) + ":" + this.getDisplayName(null) + " ";
   }

   @Override
   public void readUnsubscribed() {
      this.subscribeCount--;
      if (this.subscribeCount == 0) {
         this.lonComponentUnsubscribed();
      }
   }

   protected void lonComponentSubscribed() {
   }

   protected void lonComponentUnsubscribed() {
   }
}
