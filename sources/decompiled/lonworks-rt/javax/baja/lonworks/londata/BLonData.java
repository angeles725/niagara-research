package javax.baja.lonworks.londata;

import java.io.IOException;
import java.util.Vector;
import java.util.logging.Logger;
import javax.baja.control.BControlPoint;
import javax.baja.lonworks.BLonComponent;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;
import javax.baja.lonworks.proxy.BLonProxyExt;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDouble;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.BObject;
import javax.baja.sys.BString;
import javax.baja.sys.BVector;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.BasicContext;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.units.BUnit;

@NiagaraType
public class BLonData extends BVector {
   public static final Type TYPE = Sys.loadType(BLonData.class);
   private Vector<BLonProxyExt> proxyExt = null;
   private int byteLength = -1;

   public BLonData() {
   }

   public BLonData(BLonPrimitive prim, BLonElementQualifiers elemQual, BUnit units) {
      BFacets f = LonFacetsUtil.makeFacets(elemQual, units);
      this.add("value", prim, 0, f, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void changed(Property prop, Context context) {
      super.changed(prop, context);
      if (isDataProp(prop)) {
         this.dataChanged(context);
      }
   }

   public void added(Property prop, Context context) {
      super.added(prop, context);
      if (prop.getType().is(BLonEnum.TYPE)) {
         try {
            BEnum e = ((BLonEnum)this.get(prop)).getEnum();
            if (e instanceof BDynamicEnum) {
               BFacets f = this.getSlotFacets(prop);
               BEnumRange er = e.getRange();
               boolean validRng = er.getOrdinals().length > 0;
               BString fr = (BString)f.get("rng", null);
               if (!validRng && fr != null) {
                  BDynamicEnum de = BDynamicEnum.make(e.getEnum().getOrdinal(), (BEnumRange)BEnumRange.DEFAULT.decodeFromString(fr.toString()));
                  this.set(prop, BLonEnum.make(de));
               }

               if (validRng && fr == null) {
                  String s = er.encodeToString();
                  f = BFacets.make(f, "rng", BString.make(s));
                  this.setFacets(prop, f);
               }
            }
         } catch (IOException var9) {
            var9.printStackTrace();
         }
      }
   }

   public static boolean isDataProp(Property prop) {
      return prop.getType().is(BLonPrimitive.TYPE) || prop.getType().is(TYPE);
   }

   protected void dataChanged(Context context) {
      BComponent parent = (BComponent)this.getParent();
      if (parent != null) {
         if (parent.getType().is(TYPE)) {
            ((BLonData)parent).dataChanged(context);
         }
      }
   }

   public void writeOk() {
      if (this.proxyExt != null) {
         for (int i = 0; i < this.proxyExt.size(); i++) {
            BLonProxyExt pext = this.proxyExt.elementAt(i);
            BStatusValue sval = pext.getStatusValue();
            if (sval != null) {
               pext.writeOk(sval);
            }
         }
      }
   }

   public void writeFail(String err) {
      if (this.proxyExt != null) {
         for (int i = 0; i < this.proxyExt.size(); i++) {
            BLonProxyExt pext = this.proxyExt.elementAt(i);
            pext.writeFail(err);
         }
      }
   }

   public void readFail(String err) {
      if (this.proxyExt != null) {
         for (int i = 0; i < this.proxyExt.size(); i++) {
            BLonProxyExt pext = this.proxyExt.elementAt(i);
            pext.readFail(err);
         }
      }
   }

   public void readOk() {
      if (this.proxyExt != null) {
         for (int i = 0; i < this.proxyExt.size(); i++) {
            BLonProxyExt pext = this.proxyExt.elementAt(i);
            BStatusValue sval = pext.getStatusValue();
            if (sval != null) {
               pext.readOk(sval);
            }
         }
      }
   }

   public void markStale(boolean s, Context cx) {
      if (this.proxyExt != null) {
         for (int i = 0; i < this.proxyExt.size(); i++) {
            this.proxyExt.elementAt(i).setStale(true, cx);
         }
      }
   }

   public void registerProxyExt(BLonProxyExt c) {
      this.getLC().getData().doRegisterProxyExt(c);
   }

   private void doRegisterProxyExt(BLonProxyExt c) {
      if (this.proxyExt == null) {
         this.proxyExt = new Vector<>();
      }

      if (!this.proxyExt.contains(c)) {
         this.proxyExt.addElement(c);
      }
   }

   public void unregisterProxyExt(BLonProxyExt c) {
      this.getLC().getData().doUnregisterProxyExt(c);
   }

   private BLonComponent getLC() {
      BComplex p = this;

      while (!p.getType().is(BLonComponent.TYPE)) {
         p = p.getParent();
      }

      return (BLonComponent)p;
   }

   private void doUnregisterProxyExt(BLonProxyExt c) {
      if (this.proxyExt != null) {
         this.proxyExt.removeElement(c);
      }
   }

   public void removeProxies(Context c) {
      if (this.proxyExt != null) {
         while (!this.proxyExt.isEmpty()) {
            BLonProxyExt pe = this.proxyExt.firstElement();
            this.proxyExt.removeElementAt(0);
            BComponent o = (BComponent)pe.getParent();
            ((BComponent)o.getParent()).remove(o.getPropertyInParent(), c);
         }
      }
   }

   public boolean hasProxies() {
      return this.proxyExt != null && this.proxyExt.size() > 0;
   }

   public boolean hasWriteProxies() {
      if (this.proxyExt != null) {
         for (int i = 0; i < this.proxyExt.size(); i++) {
            BLonProxyExt lp = this.proxyExt.elementAt(i);
            if (!lp.isUnoperational()) {
               BControlPoint cp = (BControlPoint)lp.getParent();
               if (cp.isWritablePoint()) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public BControlPoint[] getProxies(boolean writeable) {
      if (this.proxyExt == null) {
         return null;
      } else {
         Array<BControlPoint> a = new Array(BControlPoint.class);

         for (int i = 0; i < this.proxyExt.size(); i++) {
            BLonProxyExt lp = this.proxyExt.elementAt(i);
            BControlPoint cp = (BControlPoint)lp.getParent();
            if (cp.isWritablePoint() || writeable) {
               a.add(cp);
            }
         }

         return (BControlPoint[])a.trim();
      }
   }

   public final boolean forceProxyUpdates() {
      boolean updates = false;
      if (this.proxyExt != null) {
         for (int i = 0; i < this.proxyExt.size(); i++) {
            BLonProxyExt lp = this.proxyExt.elementAt(i);
            BControlPoint cp = (BControlPoint)lp.getParent();
            if (cp.isWritablePoint()) {
               lp.forceUpdate();
               updates = true;
            }
         }
      }

      return updates;
   }

   @Deprecated
   public boolean canCopyFrom(BLonData ld) {
      return this.hasEquivalentElements(ld);
   }

   public boolean hasEquivalentElements(BLonData ld) {
      if (this.isUnion() != ld.isUnion()) {
         return false;
      } else {
         SlotCursor<Property> c = this.getProperties();
         SlotCursor<Property> c2 = ld.getProperties();

         while (c.nextObject()) {
            if (!c2.nextObject()) {
               return false;
            }

            Property p = c.property();
            Property p2 = c2.property();
            if (p.getType() != p2.getType()) {
               return false;
            }

            if (p.getType().is(TYPE)) {
               if (!((BLonData)c.get()).hasEquivalentElements((BLonData)c2.get())) {
                  return false;
               }
            } else if (p.getType().is(BLonPrimitive.TYPE)) {
               if (!p.getName().equals(p2.getName())) {
                  return false;
               }

               BLonElementQualifiers e = LonFacetsUtil.getQualifiers(p.getFacets());
               BLonElementQualifiers e2 = LonFacetsUtil.getQualifiers(p2.getFacets());
               if (!e.canCopyFrom(e2)) {
                  return false;
               }

               if (p.getType().is(BLonEnum.TYPE)) {
                  BEnum en = ((BLonEnum)c.get()).getEnum();
                  BEnum en2 = ((BLonEnum)c2.get()).getEnum();
                  if (!en.getRange().equals(en2.getRange())) {
                     return false;
                  }
               }
            }
         }

         return !c2.nextObject();
      }
   }

   public void subscribed() {
      this.readSubscribed();
   }

   public void unsubscribed() {
      this.readUnsubscribed();
   }

   public void readSubscribed() {
      BComplex p = this.getParent();
      if (p instanceof BLonComponent) {
         ((BLonComponent)p).readSubscribed();
      } else {
         ((BLonData)p).readSubscribed();
      }
   }

   public void readUnsubscribed() {
      BComplex p = this.getParent();
      if (p instanceof BLonComponent) {
         ((BLonComponent)p).readUnsubscribed();
      } else {
         ((BLonData)p).readUnsubscribed();
      }
   }

   public BLonComponent getLonComponent() {
      BComplex p = this;

      while (!(p instanceof BLonComponent)) {
         p = p.getParent();
      }

      return (BLonComponent)p;
   }

   public void initDataElements(float[] d) {
      this.initDataElements(d, 0);
   }

   private int initDataElements(float[] d, int n) {
      int cnt = n;
      SlotCursor<Property> c = this.getProperties();

      while (c.nextObject()) {
         BObject obj = c.get();
         Type typ = obj.getType();
         if (typ.is(BLonPrimitive.TYPE)) {
            if (cnt >= d.length) {
               throw new BajaRuntimeException("Not enough init elements for " + this.getLonComponent().getDisplayName(null));
            }

            BLonElementQualifiers e = LonFacetsUtil.getQualifiers(c.property().getFacets());
            BLonPrimitive newValue = ((BLonPrimitive)obj).makeFromDouble(d[cnt++], e);
            this.set(c.property(), newValue, BLonNetwork.lonNoWrite);
         } else if (typ.is(TYPE)) {
            cnt = this.initDataElements(d, cnt);
         }
      }

      return cnt;
   }

   public BLonPrimitive[] getPrimitives() {
      Array<BLonPrimitive> a = new Array(BLonPrimitive.class);
      this.getPrimitives(this, a);
      return (BLonPrimitive[])a.trim();
   }

   void getPrimitives(BLonData ldat, Array<BLonPrimitive> a) {
      SlotCursor<Property> c = ldat.getProperties();

      while (c.nextObject()) {
         BObject obj = c.get();
         Type typ = obj.getType();
         if (typ.is(BLonPrimitive.TYPE)) {
            a.add((BLonPrimitive)obj);
         } else if (typ.is(TYPE)) {
            this.getPrimitives((BLonData)obj, a);
         }
      }
   }

   public int getByteLength() {
      if (this.byteLength == -1) {
         this.byteLength = this.toNetBytes().length;
      }

      return this.byteLength;
   }

   public byte[] toNetBytes() {
      LonOutputStream out = new LonOutputStream();
      this.toOutputStream(out);
      return out.toByteArray();
   }

   protected void toOutputStream(LonOutputStream out) {
      Property[] pa = this.getActiveProps();

      for (int i = 0; i < pa.length; i++) {
         BObject obj = this.get(pa[i]);
         Type typ = obj.getType();
         if (typ.is(BLonPrimitive.TYPE)) {
            this.primitiveToOutputStream(pa[i], out);
         } else if (typ.is(TYPE)) {
            int origMark = out.setBitFieldMark();
            ((BLonData)obj).toOutputStream(out);
            out.resetBitFieldMark(origMark);
         }
      }
   }

   protected void primitiveToOutputStream(Property prop, LonOutputStream out) {
      BLonElementQualifiers e = LonFacetsUtil.getQualifiers(prop.getFacets());
      int pos = e.getByteOffset();
      if (pos > 0) {
         out.setPosition(pos);
      }

      ((BLonPrimitive)this.get(prop)).toOutputStream(out, e);
   }

   public void fromNetBytes(byte[] netBytes) {
      this.fromInputStream(new LonInputStream(netBytes));
   }

   protected void fromInputStream(LonInputStream in) {
      SlotCursor<Property> c = this.getProperties();

      while (c.nextObject()) {
         BObject obj = c.get();
         Type typ = obj.getType();
         if (typ.is(BLonPrimitive.TYPE)) {
            this.primitiveFromInputStream(c.property(), in);
         } else if (typ.is(TYPE)) {
            int origMark = in.setBitFieldMark();
            ((BLonData)obj).fromInputStream(in);
            in.resetBitFieldMark(origMark);
         }
      }
   }

   Property[] getActiveProps() {
      return this.getPropertiesArray();
   }

   protected void primitiveFromInputStream(Property prop, LonInputStream in) {
      BLonElementQualifiers e = LonFacetsUtil.getQualifiers(prop.getFacets());

      BLonPrimitive newValue;
      try {
         int pos = e.getByteOffset();
         if (pos > 0) {
            in.reset(pos);
         }

         newValue = ((BLonPrimitive)this.get(prop)).fromInputStream(in, e);
      } catch (Throwable var6) {
         Logger.getLogger("lonworks").severe("Unable to decode netBytes for " + prop.getName() + ": " + var6.getMessage());
         return;
      }

      this.set(prop, newValue, BLonNetwork.lonNoWrite);
   }

   public String toString(Context cx) {
      StringBuilder sb = new StringBuilder();
      int cnt = 0;
      Property[] pa = new Property[0];

      try {
         pa = this.getActiveProps();
      } catch (NullPointerException var12) {
      }

      for (int i = 0; i < pa.length; i++) {
         BObject o = this.get(pa[i]);
         Type typ = o.getType();
         if (typ.is(BLonPrimitive.TYPE)) {
            if (cnt > 3) {
               sb.append(", ...");
               break;
            }

            if (cnt > 0) {
               sb.append(", ");
            }

            BLonPrimitive lp = (BLonPrimitive)o;
            if (lp.isNumeric()) {
               double dval = lp.getDataAsDouble();
               BasicContext bcx = new BasicContext(cx, pa[i].getFacets());
               sb.append(BDouble.make(dval).toString(bcx));
            } else {
               sb.append(lp.toString(cx));
            }

            cnt++;
         }
      }

      return sb.toString();
   }

   public String toDebugString() {
      StringBuilder sb = new StringBuilder();

      for (SlotCursor<Property> c = this.getProperties(); c.nextObject(); sb.append("\n")) {
         sb.append(c.property().getName()).append(":").append(c.get().toDebugString()).append("\n");
         BFacets f = c.property().getFacets();

         try {
            if (f != null) {
               sb.append(f.encodeToString()).append("\n");
            }
         } catch (Throwable var5) {
         }
      }

      return sb.toString();
   }

   public void setLonInt(String propName, int val, Context cx) {
      Property prop = this.getProperty(propName);
      BLonPrimitive lonPrim = (BLonPrimitive)this.get(prop);
      BLonElementQualifiers e = LonFacetsUtil.getQualifiers(prop.getFacets());
      if (lonPrim.getType().is(BLonInteger.TYPE)) {
         this.set(prop, BLonInteger.make(val), cx);
      } else {
         this.set(prop, lonPrim.makeFromDouble(val, e), cx);
      }
   }

   public void setLonBoolean(String propName, boolean val, Context cx) {
      Property prop = this.getProperty(propName);
      BLonPrimitive lonPrim = (BLonPrimitive)this.get(prop);
      if (lonPrim.getType().is(BLonBoolean.TYPE)) {
         this.set(prop, BLonBoolean.make(val), cx);
      } else {
         this.set(prop, lonPrim.makeFromBoolean(val), cx);
      }
   }

   public void setLonFloat(String propName, float val, Context cx) {
      Property prop = this.getProperty(propName);
      BLonPrimitive lonPrim = (BLonPrimitive)this.get(prop);
      BLonElementQualifiers e = LonFacetsUtil.getQualifiers(prop.getFacets());
      if (lonPrim.getType().is(BLonFloat.TYPE)) {
         this.set(prop, BLonFloat.make(val), cx);
      } else {
         this.set(prop, lonPrim.makeFromDouble(val, e), cx);
      }
   }

   public void setLonString(String propName, String val, Context cx) {
      Property prop = this.getProperty(propName);
      BLonPrimitive lonPrim = (BLonPrimitive)this.get(prop);
      if (lonPrim.getType().is(BLonString.TYPE)) {
         this.set(prop, BLonString.make(val), cx);
      } else {
         this.set(prop, lonPrim.makeFromString(val), cx);
      }
   }

   public void setLonEnum(String propName, String val, Context cx) {
      Property prop = this.getProperty(propName);
      BLonPrimitive lonPrim = (BLonPrimitive)this.get(prop);
      this.set(prop, lonPrim.makeFromString(val), cx);
   }

   public void setLonEnum(String propName, BEnum val, Context cx) {
      Property prop = this.getProperty(propName);
      BLonPrimitive lonPrim = (BLonPrimitive)this.get(prop);
      this.set(prop, lonPrim.makeFromEnum(val), cx);
   }

   public int getLonInt(String propName) {
      BLonPrimitive lonPrim = (BLonPrimitive)this.get(this.getProperty(propName));
      return lonPrim.getType().is(BLonInteger.TYPE) ? ((BLonInteger)lonPrim).getInt() : (int)lonPrim.getDataAsDouble();
   }

   public boolean getLonBoolean(String propName) {
      BLonPrimitive lonPrim = (BLonPrimitive)this.get(this.getProperty(propName));
      return lonPrim.getType().is(BLonBoolean.TYPE) ? ((BLonBoolean)lonPrim).getBoolean() : lonPrim.getDataAsBoolean();
   }

   public float getLonFloat(String propName) {
      BLonPrimitive lonPrim = (BLonPrimitive)this.get(this.getProperty(propName));
      return lonPrim.getType().is(BLonFloat.TYPE) ? ((BLonFloat)lonPrim).getFloat() : (float)lonPrim.getDataAsDouble();
   }

   public String getLonString(String propName) {
      BLonPrimitive lonPrim = (BLonPrimitive)this.get(this.getProperty(propName));
      return lonPrim.getType().is(BLonString.TYPE) ? ((BLonString)lonPrim).getString() : lonPrim.getDataAsString();
   }

   public BEnum getLonEnum(String propName, BEnum en) {
      BLonPrimitive lonPrim = (BLonPrimitive)this.get(this.getProperty(propName));
      return lonPrim.getDataAsEnum(en);
   }

   public boolean isUnion() {
      return false;
   }
}
