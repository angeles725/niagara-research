package javax.baja.bacnet.virtual;

import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NBacnetPropertyReference;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.stack.server.BBacnetExportTable;
import java.util.logging.Level;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetArray;
import javax.baja.bacnet.datatypes.BBacnetListOf;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetPropertyReference;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.export.BIBacnetExportObject;
import javax.baja.bacnet.export.BLocalBacnetDevice;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.util.PropertyInfo;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BTypeSpec;
import javax.baja.virtual.BVirtualComponent;

@NiagaraType
@NiagaraProperty(
   name = "pollRate",
   type = "BRelTime",
   defaultValue = "BRelTime.makeSeconds(5)"
)
public class BLocalBacnetVirtualGateway extends BBacnetVirtualGateway {
   public static final Property pollRate = newProperty(0, BRelTime.makeSeconds(5), null);
   public static final Type TYPE = Sys.loadType(BLocalBacnetVirtualGateway.class);
   private static final BBacnetPropertyReference[] ALL = new BBacnetPropertyReference[]{new BBacnetPropertyReference(8)};
   private LocalBacnetVirtualPoll localPoll;

   public BRelTime getPollRate() {
      return (BRelTime)this.get(pollRate);
   }

   public void setPollRate(BRelTime v) {
      this.set(pollRate, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public void started() throws Exception {
      super.started();
      this.localPoll = new LocalBacnetVirtualPoll(this);
   }

   @Override
   protected BBacnetVirtualObject makeBacnetVirtualObject(String virtualPathName) {
      return new BLocalBacnetVirtualObject(this, virtualPathName);
   }

   protected BLocalBacnetVirtualObject makeLocalBacnetVirtualObject(BIBacnetExportObject export) {
      return new BLocalBacnetVirtualObject(export);
   }

   @Override
   protected BBacnetVirtualProperty makeBacnetVirtualProperty(int propertyId, BValue value, String readFault, boolean useFacets) {
      return new BLocalBacnetVirtualProperty(propertyId, value, readFault, useFacets);
   }

   @Override
   protected Property addVirtualSlot(BVirtualComponent parent, String virtualPathName) {
      try {
         if (!(parent instanceof BLocalBacnetVirtualProperty)) {
            if (parent instanceof BLocalBacnetVirtualObject) {
               BLocalBacnetVirtualObject object = (BLocalBacnetVirtualObject)parent;
               int scndx = virtualPathName.indexOf(";");
               String propertyName = scndx > 0 ? virtualPathName.substring(0, scndx) : virtualPathName;
               String virtualPropertyName = SlotPath.escape(virtualPathName);
               int propertyId = BBacnetPropertyIdentifier.ordinal(propertyName);
               PropertyInfo pi = this.localDevice().getPropertyInfo(object.getExport().getObjectId().getObjectType(), propertyId);
               boolean useFacets = false;
               BTypeSpec spec = null;
               BValue v;
               if (pi != null) {
                  try {
                     if (pi.isArray()) {
                        spec = BTypeSpec.make(pi.getType());
                        Type t = spec.getResolvedType();
                        int size = pi.getSize();
                        if (size > 0) {
                           v = new BBacnetArray(t, size);
                        } else {
                           v = new BBacnetArray(t);
                        }
                     } else if (pi.isList()) {
                        spec = BTypeSpec.make(pi.getType());
                        Type t = spec.getResolvedType();
                        v = new BBacnetListOf(t);
                     } else {
                        spec = BTypeSpec.make(pi.getType());
                        v = (BValue)spec.getInstance();
                     }

                     if (!pi.getFacetControl().equals("no")) {
                        useFacets = true;
                     }
                  } catch (Exception var14) {
                     String s = "Unknown Type:" + spec + " for " + BBacnetObjectType.tag(object.getObjectId().getObjectType()) + ":" + propertyName;
                     log.severe("addVirtualSlot:" + s);
                     if (log.isLoggable(Level.FINE)) {
                        log.log(Level.FINE, "Exception occurred in addVirtualSlot", (Throwable)var14);
                     }

                     v = BString.make(s);
                  }
               } else {
                  v = BString.make("???");
               }

               return parent.add(virtualPropertyName, this.makeBacnetVirtualProperty(propertyId, v, null, useFacets), 2);
            }

            return parent.add(SlotPath.escape(virtualPathName), this.makeBacnetVirtualObject(virtualPathName), 2);
         }
      } catch (Exception var15) {
         if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "Exception occurred in addVirtualSlot", (Throwable)var15);
         }
      }

      return null;
   }

   @Override
   protected void loadObjects(BVirtualComponent parent) {
      BBacnetExportTable exports = (BBacnetExportTable)this.localDevice().getExportTable();
      BBacnetObjectIdentifier[] ids = exports.getObjectIds();

      for (int i = 0; i < ids.length; i++) {
         BBacnetObjectIdentifier id = ids[i];
         String name = SlotPath.escape(id.toString(nameContext));
         Property p = parent.getProperty(name);
         if (p == null) {
            BIBacnetExportObject o = this.localDevice().lookupBacnetObject(id);
            if (o != null) {
               parent.add(SlotPath.escape(name), this.makeLocalBacnetVirtualObject(o), 2);
            }
         }
      }
   }

   @Override
   protected void loadProperties(BBacnetVirtualObject object) {
      BLocalBacnetVirtualObject local = (BLocalBacnetVirtualObject)object;

      try {
         PropertyValue[] pvs = local.getExport().readPropertyMultiple(ALL);

         for (int i = 0; i < pvs.length; i++) {
            try {
               PropertyValue pv = pvs[i];
               int propertyId = pv.getPropertyId();
               String propertyName = BBacnetPropertyIdentifier.tag(propertyId);
               String virtualPropertyName = SlotPath.escape(propertyName);
               Property p = local.getProperty(virtualPropertyName);
               if (p == null) {
                  PropertyInfo pi = this.localDevice().getPropertyInfo(local.getExport().getObjectId().getObjectType(), pv.getPropertyId());
                  String readFault = null;
                  boolean useFacets = false;
                  BTypeSpec spec = null;
                  if (pi != null) {
                     try {
                        if (pi.isArray()) {
                           spec = BTypeSpec.make(pi.getType());
                           Type t = spec.getResolvedType();
                           int size = pi.getSize();
                           if (size > 0) {
                              new BBacnetArray(t, size);
                           } else {
                              new BBacnetArray(t);
                           }
                        } else if (pi.isList()) {
                           spec = BTypeSpec.make(pi.getType());
                           Type t = spec.getResolvedType();
                           new BBacnetListOf(t);
                        } else {
                           spec = BTypeSpec.make(pi.getType());
                           BValue v = (BValue)spec.getInstance();
                        }

                        if (!pi.getFacetControl().equals("no")) {
                           useFacets = true;
                        }
                     } catch (Exception var17) {
                        String s = "Unknown Type:" + spec + " for " + BBacnetObjectType.tag(object.getObjectId().getObjectType()) + ":" + propertyName;
                        log.info("addVirtualSlot:" + s);
                        if (log.isLoggable(Level.FINE)) {
                           log.log(Level.FINE, "Exception occurred in loadProperties", (Throwable)var17);
                        }

                        BValue v = BString.make(s);
                     }
                  } else {
                     BValue v = BString.make("");
                  }

                  Object var22;
                  if (!pv.isError()) {
                     var22 = AsnUtil.asnToValue(pi, pv.getPropertyValue());
                  } else {
                     readFault = NErrorType.toString(pv.getErrorClass(), pv.getErrorCode());
                     var22 = BString.make("???");
                  }

                  object.add(virtualPropertyName, this.makeBacnetVirtualProperty(propertyId, (BValue)var22, readFault, useFacets), 2, null);
               }
            } catch (Exception var18) {
               if (log.isLoggable(Level.FINE)) {
                  log.log(Level.FINE, "Exception occurred in loadProperties", (Throwable)var18);
               }
            }
         }
      } catch (Exception var19) {
         if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "Exception occurred in loadProperties", (Throwable)var19);
         }
      }
   }

   @Override
   protected int readArraySize(BBacnetVirtualProperty vp) {
      try {
         PropertyValue propVal = this.localDevice().readProperty(new NBacnetPropertyReference(vp.getPropertyId(), 0));
         return AsnUtil.fromAsnInteger(propVal.getPropertyValue());
      } catch (BacnetException var3) {
         if (log.isLoggable(Level.FINE)) {
            log.log(
               Level.FINE,
               "Exception reading array size in BacnetVirtualProperty "
                  + vp.object().getObjectId()
                  + ":"
                  + vp.getPropertyId()
                  + "["
                  + vp.debugString(null)
                  + "]",
               (Throwable)var3
            );
         }

         return 0;
      }
   }

   public String toString(Context cx) {
      return "LocalBacnetVirtualGateway";
   }

   public LocalBacnetVirtualPoll getLocalPoll() {
      return this.localPoll;
   }

   BLocalBacnetDevice localDevice() {
      return (BLocalBacnetDevice)this.getParent();
   }

   @Override
   BBacnetNetwork network() {
      return (BBacnetNetwork)this.getParent().getParent();
   }

   @Override
   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      this.localPoll.spy(out);
   }
}
