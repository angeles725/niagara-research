package javax.baja.bacnet.datatypes;

import com.tridium.bacnet.BacUtil;
import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.datatypes.BAddListElementAction;
import com.tridium.bacnet.datatypes.BRemoveListElementAction;
import com.tridium.bacnet.datatypes.ListManipulation;
import com.tridium.bacnet.services.error.NChangeListError;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetObject;
import javax.baja.bacnet.export.BIBacnetExportObject;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.io.ChangeListError;
import javax.baja.bacnet.virtual.BBacnetVirtualProperty;
import javax.baja.bacnet.virtual.BacnetVirtualUtil;
import javax.baja.category.BCategoryMask;
import javax.baja.driver.loadable.BUploadParameters;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraTopic;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.BPermissions;
import javax.baja.security.PermissionException;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.BAction;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDouble;
import javax.baja.sys.BEnum;
import javax.baja.sys.BFloat;
import javax.baja.sys.BInteger;
import javax.baja.sys.BObject;
import javax.baja.sys.BSimple;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.BasicContext;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Topic;
import javax.baja.sys.Type;
import javax.baja.util.BTypeSpec;

@NiagaraType
@NiagaraProperty(
   name = "listTypeSpec",
   type = "BTypeSpec",
   defaultValue = "BTypeSpec.DEFAULT",
   flags = 4
)
@NiagaraTopic(
   name = "listPropertyChanged"
)
public class BBacnetListOf extends BComponent implements BIBacnetDataType {
   public static final Property listTypeSpec = newProperty(4, BTypeSpec.DEFAULT, null);
   public static final Topic listPropertyChanged = newTopic(0, null);
   public static final Type TYPE = Sys.loadType(BBacnetListOf.class);
   protected boolean addActions = true;
   private int asnType;
   private boolean config;
   private boolean export;
   protected static final Logger log = Logger.getLogger("bacnet");

   public BTypeSpec getListTypeSpec() {
      return (BTypeSpec)this.get(listTypeSpec);
   }

   public void setListTypeSpec(BTypeSpec v) {
      this.set(listTypeSpec, v, null);
   }

   public void fireListPropertyChanged(BValue event) {
      this.fire(listPropertyChanged, event, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetListOf() {
   }

   public BBacnetListOf(Type listType) {
      this.setListTypeSpec(BTypeSpec.make(listType));
   }

   public void started() {
      if (this.getParent() instanceof BBacnetObject) {
         this.config = true;
      }

      if (this.getParent() instanceof BIBacnetExportObject) {
         this.export = true;
      }

      if (this.addActions) {
         if (this.get("addElement") == null) {
            BAddListElementAction addElement = new BAddListElementAction();
            addElement.setParameterTypeSpec(this.getListTypeSpec());
            this.add("addElement", addElement, 2, noWrite);
         }

         if (this.get("removeElement") == null) {
            BRemoveListElementAction removeElement = new BRemoveListElementAction();
            removeElement.setParameterTypeSpec(this.getListTypeSpec());
            this.add("removeElement", removeElement, 2, noWrite);
         }
      }
   }

   public final void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning()) {
         if (this.config || this.export || BacnetVirtualUtil.isVirtual(this)) {
            this.getParent().asComponent().changed(this.getPropertyInParent(), cx);
         }
      }
   }

   public final void added(Property p, Context cx) {
      super.added(p, cx);
      if (cx != noWrite) {
         if (this.export) {
            this.getParent().asComponent().changed(this.getPropertyInParent(), cx);
         }
      }
   }

   public final void removed(Property p, BValue old, Context cx) {
      super.removed(p, old, cx);
      if (cx != noWrite) {
         if (this.export) {
            this.getParent().asComponent().changed(this.getPropertyInParent(), cx);
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

   public final void unsubscribed() {
      if (this.config && !this.getParent().asComponent().isSubscribed()) {
         this.getParent().asComponent().unsubscribed();
      }

      BBacnetVirtualProperty vp = BacnetVirtualUtil.getVirtualProperty(this);
      if (vp != null) {
         vp.childUnsubscribed(this);
      }
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

   public final ChangeListError addElements(byte[] encodedElements, Context cx) {
      ArrayList<BValue> v = new ArrayList<>();
      int ffen = 1;

      try {
         AsnInputStream in = new AsnInputStream(encodedElements);
         int tag = in.peekTag();
         Type t = this.getListType();
         if (t.is(BIBacnetDataType.TYPE)) {
            while (tag != -1) {
               BObject o = t.getInstance();
               ((BIBacnetDataType)o).readAsn(in);
               v.add((BValue)o);
               ffen++;
               tag = in.peekTag();
            }
         } else {
            while (tag != -1) {
               BSimple s = in.readAsn();
               if (t.is(BEnum.TYPE)) {
                  s = ((BEnum)t.getInstance()).getRange().get(((BInteger)s).getInt());
               }

               v.add(s);
               ffen++;
               tag = in.peekTag();
            }
         }
      } catch (AsnException var10) {
         return new NChangeListError(8, new NErrorType(2, 9), ffen);
      }

      try {
         for (int var11 = 0; var11 < v.size(); var11++) {
            this.addListElement(v.get(var11), cx);
         }

         return null;
      } catch (PermissionException var9) {
         return new NChangeListError(8, new NErrorType(2, 40), 0L);
      }
   }

   public final ChangeListError removeElements(byte[] encodedElements, Context cx) {
      ArrayList<BValue> v = new ArrayList<>();
      int ffen = 1;

      try {
         AsnInputStream in = new AsnInputStream(encodedElements);
         int tag = in.peekTag();
         Type t = this.getListType();
         if (t.is(BIBacnetDataType.TYPE)) {
            while (tag != -1) {
               BObject o = t.getInstance();
               ((BIBacnetDataType)o).readAsn(in);
               v.add((BValue)o);
               ffen++;
               tag = in.peekTag();
            }
         } else {
            while (tag != -1) {
               BSimple s = in.readAsn();
               if (t.is(BEnum.TYPE)) {
                  s = ((BEnum)t.getInstance()).getRange().get(((BInteger)s).getInt());
               }

               v.add(s);
               ffen++;
               tag = in.peekTag();
            }
         }
      } catch (AsnException var10) {
         return new NChangeListError(9, new NErrorType(2, 9), ffen);
      }

      try {
         for (int var11 = 1; var11 <= v.size(); var11++) {
            if (!this.contains(v.get(var11 - 1))) {
               return new NChangeListError(9, new NErrorType(5, 81), var11);
            }
         }

         for (int var12 = 0; var12 < v.size(); var12++) {
            this.removeListElement(v.get(var12), cx);
         }

         return null;
      } catch (PermissionException var9) {
         return new NChangeListError(9, new NErrorType(2, 40), 0L);
      }
   }

   public final boolean contains(BValue element) {
      SlotCursor<Property> c = this.getProperties();

      while (c.next()) {
         if (c.get().equivalent(element)) {
            return true;
         }
      }

      return false;
   }

   public final Type getListType() {
      try {
         return this.getListTypeSpec().getResolvedType();
      } catch (Exception var2) {
         log.info("Exception resolving list type for " + this.getName() + ":" + var2.getMessage());
         return BBacnetNull.TYPE;
      }
   }

   public String toString(Context cx) {
      if (cx != null && cx instanceof BasicContext) {
         return "List of " + this.getListTypeSpec();
      } else {
         this.loadSlots();
         StringBuilder sb = new StringBuilder("{");
         SlotCursor<Property> sc = this.getProperties();

         while (sc.next()) {
            if (((Property)sc.slot()).isProperty()
               && (sc.property().getType().getTypeSpec().equals(this.getListTypeSpec()) || sc.property().getType().is(BOrd.TYPE))) {
               sb.append(sc.get()).append(',');
            }
         }

         if (sb.length() == 1) {
            return "{}";
         } else {
            sb.setCharAt(sb.length() - 1, '}');
            return sb.toString();
         }
      }
   }

   @Override
   public final void writeAsn(AsnOutput out) {
      synchronized (out) {
         SlotCursor<Property> c = this.getProperties();
         c.next();

         while (c.next()) {
            try {
               BObject listElement = c.get();
               if (!(listElement instanceof BAction)) {
                  if (listElement instanceof BOrd) {
                     listElement = ((BOrd)listElement).get(this);
                  }

                  if (listElement != null && listElement.getType() == this.getListType()) {
                     switch (this.asnType()) {
                        case 0:
                           out.writeNull();
                           break;
                        case 1:
                           out.writeBoolean((BBoolean)listElement);
                           break;
                        case 2:
                           out.writeUnsigned((BBacnetUnsigned)listElement);
                           break;
                        case 3:
                           out.writeSignedInteger((BInteger)listElement);
                           break;
                        case 4:
                           out.writeReal((BFloat)listElement);
                           break;
                        case 5:
                           out.writeDouble((BDouble)listElement);
                           break;
                        case 6:
                           out.writeOctetString((BBacnetOctetString)listElement);
                           break;
                        case 7:
                           out.writeCharacterString((BString)listElement);
                           break;
                        case 8:
                           out.writeBitString((BBacnetBitString)listElement);
                           break;
                        case 9:
                           out.writeEnumerated((BEnum)listElement);
                           break;
                        case 10:
                           out.writeDate((BBacnetDate)listElement);
                           break;
                        case 11:
                           out.writeTime((BBacnetTime)listElement);
                           break;
                        case 12:
                           out.writeObjectIdentifier((BBacnetObjectIdentifier)listElement);
                           break;
                        default:
                           ((BIBacnetDataType)listElement).writeAsn(out);
                           if (log.isLoggable(Level.FINE)) {
                              log.fine(
                                 this.getName()
                                    + ": writeAsn: constructed data type: listElem="
                                    + listElement
                                    + " t="
                                    + listElement.getType()
                                    + ", list type="
                                    + this.getListType()
                              );
                           }
                     }
                  } else {
                     log.warning(this.getName() + ": writeAsn: listElem is null or type mismatch!");
                  }
               }
            } catch (Exception var6) {
               log.warning(this.getName() + ": writeAsn: Exception! " + var6.getMessage());
               throw new IllegalStateException(var6);
            }
         }
      }
   }

   @Override
   public final void readAsn(AsnInput in) throws AsnException {
      ArrayList<BValue> v = new ArrayList<>();
      synchronized (in) {
         while (in.peekTag() != -1) {
            BValue listElement = null;
            switch (this.asnType()) {
               case 0:
                  listElement = in.readNull();
                  break;
               case 1:
                  listElement = BBoolean.make(in.readBoolean());
                  break;
               case 2:
                  listElement = in.readUnsigned();
                  break;
               case 3:
                  listElement = BInteger.make(in.readSignedInteger());
                  break;
               case 4:
                  listElement = BFloat.make(in.readReal());
                  break;
               case 5:
                  listElement = BDouble.make(in.readDouble());
                  break;
               case 6:
                  listElement = BBacnetOctetString.make(in.readOctetString());
                  break;
               case 7:
                  listElement = BString.make(in.readCharacterString());
                  break;
               case 8:
                  listElement = in.readBitString();
                  break;
               case 9:
                  BEnum d = (BEnum)this.getListType().getInstance();
                  listElement = d.getRange().get(in.readEnumerated());
                  break;
               case 10:
                  listElement = in.readDate();
                  break;
               case 11:
                  listElement = in.readTime();
                  break;
               case 12:
                  listElement = in.readObjectIdentifier();
                  break;
               default:
                  listElement = (BValue)this.getListType().getInstance();
                  ((BIBacnetDataType)listElement).readAsn(in);
            }

            if (listElement != null) {
               if (!listElement.getType().getTypeName().equals(this.getListTypeSpec().getTypeName())) {
                  throw new AsnException(
                     "Invalid data type for list element: expected=" + this.getListTypeSpec().getTypeName() + " actual=" + listElement.getType().getTypeName()
                  );
               }

               v.add(listElement);
            }
         }
      }

      int ndx = 0;

      for (BValue val : v) {
         String name = this.name(ndx++);
         BacUtil.setOrAdd(this, name, val, noWrite);
      }

      for (Property p = this.getProperty(this.name(ndx++)); p != null; p = this.getProperty(this.name(ndx++))) {
         this.remove(p, noWrite);
      }
   }

   public Property addListElement(BValue listElement, Context cx) {
      if (this.config) {
         BBacnetObject o = (BBacnetObject)this.getParent();
         o.postAsync(new ListManipulation(o, this.getPropertyInParent(), listElement, true));
         if (o.getObjectId().getInstanceNumber() == -1) {
            BacUtil.setOrAdd(this, listElement.getClass().getSimpleName() + "?", listElement, noWrite);
            return null;
         } else {
            o.upload(new BUploadParameters());
            return null;
         }
      } else if (listElement.getType().is(this.getListType())) {
         return !this.contains(listElement) ? this.add(null, listElement, cx) : null;
      } else if (listElement instanceof BOrd) {
         return this.add(null, listElement, 2, cx);
      } else {
         log.severe(this + ".addListElement:Wrong element type: this is a list of " + this.getListType().getTypeName());
         return null;
      }
   }

   public void removeListElement(BValue listElement, Context cx) {
      if (this.config) {
         BBacnetObject o = (BBacnetObject)this.getParent();
         if (o.getObjectId().getInstanceNumber() != -1) {
            o.postAsync(new ListManipulation(o, this.getPropertyInParent(), listElement, false));
            o.upload(new BUploadParameters());
            return;
         }
      }

      SlotCursor<Property> c = this.getProperties();

      while (c.next()) {
         if (c.get().equivalent(listElement)) {
            this.remove(c.property(), cx);
            return;
         }
      }
   }

   private int asnType() {
      Type t = this.getListType();
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
      String s = this.getListTypeSpec().getTypeName();
      return ndx == 0 ? s : s + ndx;
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetListOf", 2);
      out.prop("asnType", this.asnType);
      out.prop("config", this.config);
      out.prop("export", this.export);
      out.prop("virtual", BacnetVirtualUtil.isVirtual(this));
      out.endProps();
   }
}
