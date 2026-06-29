package javax.baja.bacnet.virtual;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetDate;
import javax.baja.bacnet.datatypes.BBacnetNull;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetOctetString;
import javax.baja.bacnet.datatypes.BBacnetTime;
import javax.baja.bacnet.datatypes.BBacnetUnsigned;
import javax.baja.bacnet.datatypes.BIBacnetDataType;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.naming.BOrd;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.space.BComponentSpace;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDouble;
import javax.baja.sys.BEnum;
import javax.baja.sys.BFacets;
import javax.baja.sys.BFloat;
import javax.baja.sys.BInteger;
import javax.baja.sys.BObject;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BTypeSpec;
import javax.baja.virtual.BVirtualComponent;
import javax.baja.virtual.BVirtualComponentSpace;

@NiagaraType
@Deprecated
@NiagaraProperties({@NiagaraProperty(
      name = "arrayTypeSpec",
      type = "BTypeSpec",
      defaultValue = "BTypeSpec.DEFAULT",
      flags = 4
   ), @NiagaraProperty(
      name = "size",
      type = "int",
      defaultValue = "-1",
      flags = 4
   )})
public class BBacnetVirtualArray extends BVirtualComponent implements BacnetConst, BIBacnetDataType {
   public static final Property arrayTypeSpec = newProperty(4, BTypeSpec.DEFAULT, null);
   public static final Property size = newProperty(4, -1, null);
   public static final Type TYPE = Sys.loadType(BBacnetVirtualArray.class);
   private int asnType;
   boolean elementsLoaded = false;
   private static final Logger logger = Logger.getLogger("bacnet");

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

   public Type getType() {
      return TYPE;
   }

   public BBacnetVirtualArray() {
   }

   public BBacnetVirtualArray(Type arrayType) {
      this.setArrayTypeSpec(BTypeSpec.make(arrayType));
   }

   public BBacnetVirtualArray(Type arrayType, int fixedSize) {
      this.setArrayTypeSpec(BTypeSpec.make(arrayType));
      this.setSize(fixedSize);
   }

   public void added(Property p, Context cx) {
      super.added(p, cx);
      if (cx != noWrite) {
         BComponentSpace space = this.getComponentSpace();
         if (space instanceof BVirtualComponentSpace) {
            int ndx = index(p.getName());
            this.getParent().asComponent().added(this.getPropertyInParent(), BFacets.make("index", BInteger.make(ndx)));
         }
      }
   }

   public void removed(Property p, BValue oldValue, Context cx) {
      super.removed(p, oldValue, cx);
      if (cx != noWrite) {
         BComponentSpace space = this.getComponentSpace();
         if (space instanceof BVirtualComponentSpace) {
            int ndx = index(p.getName());
            this.getParent().asComponent().removed(this.getPropertyInParent(), oldValue, BFacets.make("index", BInteger.make(ndx)));
         }
      }
   }

   public final void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning()) {
         if (!p.equals(size) && !p.equals(arrayTypeSpec)) {
            BComponentSpace space = this.getComponentSpace();
            if (space instanceof BVirtualComponentSpace) {
               if (cx != noWrite) {
                  this.getParent().asComponent().changed(this.getPropertyInParent(), BFacets.make("index", BInteger.make(this.index(p))));
               } else {
                  this.getParent().asComponent().changed(this.getPropertyInParent(), cx);
               }
            }
         }
      }
   }

   public final void subscribed() {
      BComponentSpace space = this.getComponentSpace();
      if (space instanceof BVirtualComponentSpace) {
         BComponent c = this.getParent().asComponent();
         if (!c.isSubscribed()) {
            c.subscribed();
         }
      }
   }

   public void unsubscribed() {
      BComponentSpace space = this.getComponentSpace();
      if (space instanceof BVirtualComponentSpace) {
         BComponent c = this.getParent().asComponent();
         if (!c.isSubscribed()) {
            c.unsubscribed();
         }
      }
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BBacnetVirtualComponent;
   }

   public String toString(Context cx) {
      int size = this.getSize();
      String sz = String.valueOf(size);
      if (size < 0) {
         sz = "N";
      }

      return "BacnetVirtualARRAY[" + sz + "] of " + this.getArrayTypeSpec();
   }

   public final BValue getElement(int index) {
      return index == 0 ? this.get(size) : this.get(this.name(index - 1));
   }

   public final void setElement(int index, BValue value) {
      if (value.getType() == this.getArrayType() && index > 0) {
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
         logger.log(Level.SEVERE, "BacnetVirtualArray:Unable to get resolved Type for " + this.getArrayTypeSpec(), (Throwable)var2);
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

   private String name(int ndx) {
      return "element" + (ndx + 1);
   }

   private int index(Property p) {
      return index(p.getName());
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
               logger.log(Level.INFO, this.getName() + ":" + this + ": writeAsn: Exception!", (Throwable)var6);
            }
         }
      }
   }

   @Override
   public final void readAsn(AsnInput in) throws AsnException {
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
            this.set(p, val, noWrite);
         } else {
            this.add(name, val, noWrite);
         }
      }

      for (Property p = this.getProperty(this.name(ndx++)); p != null; p = this.getProperty(this.name(ndx++))) {
         this.remove(p, noWrite);
      }
   }

   public void readAsn(AsnInput in, int index) throws AsnException {
      synchronized (in) {
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

         Property p = this.getProperty("element" + index);
         if (p != null && arrayElement != null) {
            this.set(p, arrayElement, noWrite);
         }
      }
   }

   public final void writeAsn(AsnOutput out, int index) {
      synchronized (out) {
         try {
            BValue arrayElement = this.getElement(index);
            if (arrayElement != null && arrayElement.getType() == this.getArrayType()) {
               switch (this.asnType()) {
                  case 0:
                     out.writeNull();
                     break;
                  case 1:
                     out.writeBoolean((BBoolean)arrayElement);
                     break;
                  case 2:
                     out.writeUnsigned((BBacnetUnsigned)arrayElement);
                     break;
                  case 3:
                     out.writeSignedInteger((BInteger)arrayElement);
                     break;
                  case 4:
                     out.writeReal((BFloat)arrayElement);
                     break;
                  case 5:
                     out.writeDouble((BDouble)arrayElement);
                     break;
                  case 6:
                     out.writeOctetString((BBacnetOctetString)arrayElement);
                     break;
                  case 7:
                     out.writeCharacterString((BString)arrayElement);
                     break;
                  case 8:
                     out.writeBitString((BBacnetBitString)arrayElement);
                     break;
                  case 9:
                     out.writeEnumerated((BEnum)arrayElement);
                     break;
                  case 10:
                     out.writeDate((BBacnetDate)arrayElement);
                     break;
                  case 11:
                     out.writeTime((BBacnetTime)arrayElement);
                     break;
                  case 12:
                     out.writeObjectIdentifier((BBacnetObjectIdentifier)arrayElement);
                     break;
                  default:
                     ((BIBacnetDataType)arrayElement).writeAsn(out);
               }
            }
         } catch (Exception var6) {
            logger.log(Level.INFO, this.getName() + ":" + this + ": writeAsn: Exception!", (Throwable)var6);
         }
      }
   }
}
