package javax.baja.bacnet.datatypes;

import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.datatypes.BAddArrayElementAction;
import com.tridium.bacnet.datatypes.BRemoveArrayElementAction;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetObject;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.util.GrandchildChangedContext;
import javax.baja.bacnet.virtual.BBacnetVirtualProperty;
import javax.baja.bacnet.virtual.BacnetVirtualUtil;
import javax.baja.category.BCategoryMask;
import javax.baja.naming.BOrd;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraTopic;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.BPermissions;
import javax.baja.spy.SpyWriter;
import javax.baja.sync.Transaction;
import javax.baja.sys.Action;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDouble;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnum;
import javax.baja.sys.BFloat;
import javax.baja.sys.BInteger;
import javax.baja.sys.BObject;
import javax.baja.sys.BSimple;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.BasicContext;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Subscriber;
import javax.baja.sys.Sys;
import javax.baja.sys.Topic;
import javax.baja.sys.Type;
import javax.baja.util.BTypeSpec;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "arrayTypeSpec",
      type = "BTypeSpec",
      defaultValue = "BTypeSpec.DEFAULT",
      flags = 4
   ), @NiagaraProperty(
      name = "size",
      type = "int",
      defaultValue = "0",
      flags = 4
   ), @NiagaraProperty(
      name = "fixedSize",
      type = "boolean",
      defaultValue = "false",
      flags = 4
   )})
@NiagaraActions({@NiagaraAction(
      name = "addElement",
      parameterType = "BValue",
      defaultValue = "BBacnetUnsigned.DEFAULT",
      flags = 4
   ), @NiagaraAction(
      name = "removeElement",
      parameterType = "BInteger",
      defaultValue = "BInteger.make(0)",
      flags = 4
   )})
@NiagaraTopic(
   name = "arrayPropertyChanged"
)
public class BBacnetArray extends BComponent implements BIBacnetDataType {
   public static final Property arrayTypeSpec = newProperty(4, BTypeSpec.DEFAULT, null);
   public static final Property size = newProperty(4, 0, null);
   public static final Property fixedSize = newProperty(4, false, null);
   public static final Action addElement = newAction(4, BBacnetUnsigned.DEFAULT, null);
   public static final Action removeElement = newAction(4, BInteger.make(0), null);
   public static final Topic arrayPropertyChanged = newTopic(0, null);
   public static final Type TYPE = Sys.loadType(BBacnetArray.class);
   private int asnType;
   private boolean config;
   private static final Logger loggerBacnetClient = Logger.getLogger("bacnet.client");
   private static final Logger loggerBacnet = Logger.getLogger("bacnet");
   private static final Logger loggerBacnetDebug = Logger.getLogger("bacnet.debug");
   public static final String ELEMENT_0 = "element0";

   public BTypeSpec getArrayTypeSpec() {
      return (BTypeSpec)this.get(arrayTypeSpec);
   }

   public void setArrayTypeSpec(BTypeSpec v) {
      this.set(arrayTypeSpec, v, null);
   }

   public int getSize() {
      return this.getInt(size);
   }

   public void setSize(int v) {
      this.setInt(size, v, null);
   }

   public boolean getFixedSize() {
      return this.getBoolean(fixedSize);
   }

   public void setFixedSize(boolean v) {
      this.setBoolean(fixedSize, v, null);
   }

   public void addElement(BValue parameter) {
      this.invoke(addElement, parameter, null);
   }

   public void removeElement(BInteger parameter) {
      this.invoke(removeElement, parameter, null);
   }

   public void fireArrayPropertyChanged(BValue event) {
      this.fire(arrayPropertyChanged, event, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetArray() {
   }

   public BBacnetArray(Type arrayType) {
      this.setArrayTypeSpec(BTypeSpec.make(arrayType));
   }

   public BBacnetArray(Type arrayType, int fixedSize) {
      this.setArrayTypeSpec(BTypeSpec.make(arrayType));
      this.setFixedSize(true);
      this.setSize(fixedSize);
      this.checkSize(true, null);
   }

   public final void started() {
      if (this.getParent() instanceof BBacnetObject) {
         this.config = true;
      }

      this.checkSize(true, null);
      if (!this.getFixedSize()) {
         if (this.get("addArrayElement") == null) {
            BAddArrayElementAction addElement = new BAddArrayElementAction();
            addElement.setParameterTypeSpec(this.getArrayTypeSpec());
            this.add("addArrayElement", addElement, 2);
         }

         if (this.get("removeArrayElement") == null) {
            BRemoveArrayElementAction removeElement = new BRemoveArrayElementAction();
            removeElement.setParameterTypeSpec(BDynamicEnum.TYPE.getTypeSpec());
            this.add("removeArrayElement", removeElement, 2);
         }
      }
   }

   public final void changed(Property p, Context cx) {
      if (this.isRunning()) {
         if (p.equals(size)) {
            Property e0 = this.getProperty("element0");
            if (e0 != null) {
               this.setInt(e0, this.getSize(), cx);
            }

            if (cx != noWrite) {
               this.checkSize(true, cx);
            }
         } else if (!p.equals(fixedSize) && !p.equals(arrayTypeSpec) && !p.getName().equals("element0") && p.getName().startsWith("element") && cx != noWrite) {
            if (this.config) {
               try {
                  ((BBacnetObject)this.getParent()).writeProperty(this.getPropertyInParent(), this.index(p), AsnUtil.toAsn(this.asnType(), this.get(p)));
               } catch (BacnetException var4) {
                  loggerBacnetClient.warning(
                     "Unable to write array element " + this.index(p) + " in property " + this.getPropertyInParent() + " of " + this.getParent() + ":" + var4
                  );
               }
            } else if (BacnetVirtualUtil.isVirtual(this)) {
               this.getParent()
                  .asComponent()
                  .changed(this.getPropertyInParent(), new GrandchildChangedContext(this.index(p), AsnUtil.toAsn(this.asnType(), this.get(p))));
            }
         }
      }
   }

   public final void subscribed() {
      if (this.config && !this.getParent().asComponent().isSubscribed()) {
         this.getParent().asComponent().subscribed();
      }

      BBacnetVirtualProperty vp = BacnetVirtualUtil.getVirtualProperty(this);
      if (vp != null) {
         vp.childSubscribed(this);
      }
   }

   public void unsubscribed() {
      if (this.config && !this.getParent().asComponent().isSubscribed()) {
         this.getParent().asComponent().unsubscribed();
      }

      BBacnetVirtualProperty vp = BacnetVirtualUtil.getVirtualProperty(this);
      if (vp != null) {
         vp.childUnsubscribed(this);
      }
   }

   public boolean isChildLegal(BComponent child) {
      return !this.isRunning() ? true : child.getType().is(this.getArrayTypeSpec().getTypeInfo());
   }

   public BCategoryMask getAppliedCategoryMask() {
      return BacnetVirtualUtil.isVirtual(this) ? this.getParent().asComponent().getAppliedCategoryMask() : super.getAppliedCategoryMask();
   }

   public BCategoryMask getCategoryMask() {
      return BacnetVirtualUtil.isVirtual(this) ? this.getParent().asComponent().getCategoryMask() : super.getCategoryMask();
   }

   public BPermissions getPermissions(Context cx) {
      return BacnetVirtualUtil.isVirtual(this) ? this.getParent().asComponent().getPermissions(cx) : super.getPermissions(cx);
   }

   public final void doAddElement(BValue arrayElement) {
      if (this.getFixedSize()) {
         loggerBacnetDebug.severe(this + ".doAddElement:Fixed size array; can't add element!");
      } else {
         int size = this.getSize();
         if (arrayElement.getType() == this.getArrayType()) {
            this.add(this.name(size), arrayElement);
            this.setSize(++size);
            BComplex parent = this.getParent();
            if (parent instanceof BComponent) {
               ((BComponent)parent).changed(this.getPropertyInParent(), null);
            }
         } else if (arrayElement instanceof BOrd) {
            this.add(this.name(size), arrayElement);
            this.setSize(++size);
         } else {
            loggerBacnetDebug.severe(this + ".doAddElement:Wrong element type: this is an array of " + this.getArrayType().getTypeName());
         }
      }
   }

   public final void doRemoveElement(BInteger index) {
      if (this.getFixedSize()) {
         loggerBacnetDebug.severe(this + ".doRemoveElement:Fixed size array; can't remove element!");
      } else {
         this.remove(this.name(index.getInt()));
         this.reIndex();
         this.setSize(this.getSize() - 1);
         this.getParent().asComponent().changed(this.getPropertyInParent(), null);
      }
   }

   public String toString(Context cx) {
      this.loadSlots();
      if (cx != null && cx instanceof BasicContext) {
         return "BacnetARRAY[" + this.getSize() + "] of " + this.getArrayTypeSpec();
      } else {
         StringBuilder sb = new StringBuilder("{");
         int len = this.getSize();

         for (int i = 1; i <= len; i++) {
            sb.append(this.getElement(i)).append(',');
         }

         if (sb.length() == 1) {
            return "{}";
         } else {
            sb.setCharAt(sb.length() - 1, '}');
            return sb.toString();
         }
      }
   }

   public final BValue getElement(int index) {
      return index == 0 ? this.get(size) : this.get(this.name(index - 1));
   }

   public final void setElement(int index, BValue value) {
      if (value.getType() == this.getArrayType() && index > 0 && index <= this.getSize()) {
         this.set(this.name(index - 1), value);
      }
   }

   public static int index(String propName) {
      propName = SlotPath.unescape(propName);
      int sc = propName.indexOf(";");
      return sc < 0 ? Integer.parseInt(propName.substring(7)) : Integer.parseInt(propName.substring(7, sc));
   }

   private Type getArrayType() {
      try {
         return this.getArrayTypeSpec().getResolvedType();
      } catch (Exception var2) {
         loggerBacnet.log(Level.SEVERE, "Exception occurred in getArrayType", (Throwable)var2);
         return null;
      }
   }

   private int asnType() {
      Type t = this.getArrayType();
      if (t == BBacnetNull.TYPE) {
         this.asnType = 0;
      } else if (t == BBoolean.TYPE) {
         this.asnType = 1;
      } else if (t == BBacnetUnsigned.TYPE) {
         this.asnType = 2;
      } else if (t == BInteger.TYPE) {
         this.asnType = 3;
      } else if (t == BFloat.TYPE) {
         this.asnType = 4;
      } else if (t == BDouble.TYPE) {
         this.asnType = 5;
      } else if (t == BBacnetOctetString.TYPE) {
         this.asnType = 6;
      } else if (t == BString.TYPE) {
         this.asnType = 7;
      } else if (t == BBacnetBitString.TYPE) {
         this.asnType = 8;
      } else if (BEnum.class.isAssignableFrom(t.getTypeClass())) {
         this.asnType = 9;
      } else if (t == BBacnetDate.TYPE) {
         this.asnType = 10;
      } else if (t == BBacnetTime.TYPE) {
         this.asnType = 11;
      } else if (t == BBacnetObjectIdentifier.TYPE) {
         this.asnType = 12;
      } else {
         this.asnType = -1;
      }

      return this.asnType;
   }

   private synchronized void checkSize(boolean trim, Context cx) {
      SlotCursor<Property> c = this.getProperties();
      int count = 0;

      while (c.next(this.getArrayType().getTypeClass())) {
         if (c.property().isDynamic()) {
            count++;
         }
      }

      if (trim) {
         this.trimToSize(count, cx);
      } else {
         this.setInt(size, count, cx);
         Property p = this.getProperty("element0");
         if (p != null) {
            this.setInt(p, count, cx);
         }
      }
   }

   private void trimToSize(int count, Context cx) {
      int siz = this.getSize();
      if (siz < count) {
         for (int i = siz; i < count; i++) {
            this.remove(this.name(i), cx);
         }
      } else if (siz > count) {
         for (int i = count; i < siz; i++) {
            this.add(this.name(i), (BValue)this.getArrayType().getInstance(), cx);
         }
      }
   }

   private String name(int ndx) {
      return "element" + (ndx + 1);
   }

   private int index(Property p) {
      return Integer.parseInt(p.getName().substring(7));
   }

   private static void copyFrom(BComponent src, BComponent dst, Context cx) {
      Context txn = Transaction.start(dst, cx);
      Property[] props = dst.getPropertiesArray();
      int i = props.length;

      while (--i >= 0) {
         if (props[i].isDynamic()) {
            dst.remove(props[i], cx);
         }
      }

      SlotCursor<Property> c = src.getProperties();

      while (c.next()) {
         Property p = c.property();
         BValue o = c.get();
         if (o instanceof BComplex) {
            o = o.newCopy(true);
         }

         if (dst.get(p.getName()) != null) {
            dst.set(p, o, cx);
         } else {
            dst.asComponent().add(p.getName(), o, cx);
         }
      }

      try {
         Transaction.end(dst, txn);
      } catch (Exception var10) {
         throw new BajaRuntimeException(var10);
      }
   }

   private void reIndex() {
      if (!this.getFixedSize()) {
         int siz = this.getSize();

         for (int ndx = 1; ndx < siz; ndx++) {
            BValue elem = this.getElement(ndx);
            if (elem == null) {
               int i = ndx;

               while (elem == null && i < siz) {
                  elem = this.getElement(++i);
               }

               this.add(this.name(ndx - 1), elem);
               this.remove(this.name(i - 1));
            }
         }
      }
   }

   @Override
   public final void writeAsn(AsnOutput out) {
      synchronized (out) {
         SlotCursor<Property> c = this.getProperties();

         while (c.next()) {
            try {
               BObject arrayElement = c.get();
               if (arrayElement instanceof BOrd) {
                  arrayElement = ((BOrd)arrayElement).get(this);
               }

               if (arrayElement != null && arrayElement.getType() == this.getArrayType()) {
                  switch (this.asnType()) {
                     case 0:
                        out.writeNull();
                        break;
                     case 1:
                        out.writeBoolean((BBoolean)c.get());
                        break;
                     case 2:
                        out.writeUnsigned((BBacnetUnsigned)c.get());
                        break;
                     case 3:
                        out.writeSignedInteger((BInteger)c.get());
                        break;
                     case 4:
                        out.writeReal((BFloat)c.get());
                        break;
                     case 5:
                        out.writeDouble((BDouble)c.get());
                        break;
                     case 6:
                        out.writeOctetString((BBacnetOctetString)c.get());
                        break;
                     case 7:
                        out.writeCharacterString((BString)c.get());
                        break;
                     case 8:
                        out.writeBitString((BBacnetBitString)c.get());
                        break;
                     case 9:
                        out.writeEnumerated((BEnum)c.get());
                        break;
                     case 10:
                        out.writeDate((BBacnetDate)c.get());
                        break;
                     case 11:
                        out.writeTime((BBacnetTime)c.get());
                        break;
                     case 12:
                        out.writeObjectIdentifier((BBacnetObjectIdentifier)c.get());
                        break;
                     default:
                        ((BIBacnetDataType)c.get()).writeAsn(out);
                  }
               }
            } catch (Exception var6) {
               loggerBacnet.log(Level.WARNING, this.getName() + ":" + this + ": writeAsn: Exception!", (Throwable)var6);
            }
         }
      }
   }

   @Override
   public final void readAsn(AsnInput in) throws AsnException {
      Subscriber[] subs = this.getSubscribers();
      if (subs == null || subs.length == 0) {
         BComponent[] ckids = this.getChildComponents();
         boolean keepSub = false;

         for (int i = 0; i < ckids.length; i++) {
            if (ckids[i].isSubscribed()) {
               keepSub = true;
               break;
            }
         }

         if (!keepSub) {
            Property p = this.getProperty("element0");
            if (p != null) {
               this.remove(p);
            }
         }
      }

      ArrayList<BValue> v = new ArrayList<>();
      synchronized (in) {
         while (in.peekTag() != -1) {
            BValue arrayElement;
            switch (this.asnType()) {
               case 0:
                  arrayElement = in.readNull();
                  break;
               case 1:
                  arrayElement = BBoolean.make(in.readBoolean());
                  break;
               case 2:
                  arrayElement = in.readUnsigned();
                  break;
               case 3:
                  arrayElement = BInteger.make(in.readSignedInteger());
                  break;
               case 4:
                  arrayElement = BFloat.make(in.readReal());
                  break;
               case 5:
                  arrayElement = BDouble.make(in.readDouble());
                  break;
               case 6:
                  arrayElement = BBacnetOctetString.make(in.readOctetString());
                  break;
               case 7:
                  arrayElement = BString.make(in.readCharacterString());
                  break;
               case 8:
                  arrayElement = in.readBitString();
                  break;
               case 9:
                  BEnum d = (BEnum)this.getArrayType().getInstance();
                  arrayElement = d.getRange().get(in.readEnumerated());
                  break;
               case 10:
                  arrayElement = in.readDate();
                  break;
               case 11:
                  arrayElement = in.readTime();
                  break;
               case 12:
                  arrayElement = in.readObjectIdentifier();
                  break;
               default:
                  arrayElement = (BValue)this.getArrayType().getInstance();
                  ((BIBacnetDataType)arrayElement).readAsn(in);
            }

            if (arrayElement != null) {
               v.add(arrayElement);
            }
         }
      }

      int ndx = 0;

      for (BValue val : v) {
         String name = this.name(ndx++);
         Property p = this.getProperty(name);
         if (p != null) {
            if (p.getType().is(BSimple.TYPE)) {
               this.set(p, val, noWrite);
            } else if (p.getType().is(BComponent.TYPE)) {
               copyFrom(val.asComponent(), this.get(p).asComponent(), noWrite);
            } else {
               ((BComplex)this.get(p)).copyFrom(val.asComplex(), noWrite);
            }
         } else {
            this.add(name, val, noWrite);
         }
      }

      for (Property p = this.getProperty(this.name(ndx++)); p != null; p = this.getProperty(this.name(ndx++))) {
         this.remove(p, noWrite);
      }

      this.checkSize(false, noWrite);
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetArray", 2);
      out.prop("asnType", this.asnType);
      out.prop("config", this.config);
      out.prop("virtual", BacnetVirtualUtil.isVirtual(this));
      out.endProps();
   }
}
