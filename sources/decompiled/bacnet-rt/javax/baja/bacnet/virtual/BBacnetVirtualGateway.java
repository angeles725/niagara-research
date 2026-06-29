package javax.baja.bacnet.virtual;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NBacnetPropertyReference;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.asn.NReadPropertyResult;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.config.BBacnetDeviceObject;
import javax.baja.bacnet.datatypes.BBacnetArray;
import javax.baja.bacnet.datatypes.BBacnetListOf;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.util.PropertyInfo;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.space.BComponentSpace;
import javax.baja.space.LoadCallbacks;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.BComponent;
import javax.baja.sys.BInteger;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BTypeSpec;
import javax.baja.virtual.BVirtualComponent;
import javax.baja.virtual.BVirtualComponentSpace;
import javax.baja.virtual.BVirtualGateway;

@NiagaraType
public class BBacnetVirtualGateway extends BVirtualGateway implements BacnetConst {
   public static final Type TYPE = Sys.loadType(BBacnetVirtualGateway.class);
   protected static final Logger log = Logger.getLogger("bacnet.virtual");
   private boolean auditWrites = false;
   private Object SLOT_LOCK = new Object();

   public Type getType() {
      return TYPE;
   }

   @Deprecated
   protected BBacnetVirtualComponent makeBacnetVirtualComponent(String virtualPathName) {
      return new BBacnetVirtualComponent(virtualPathName);
   }

   protected BBacnetVirtualObject makeBacnetVirtualObject(String virtualPathName) {
      return new BBacnetVirtualObject(virtualPathName);
   }

   protected BBacnetVirtualProperty makeBacnetVirtualProperty(int propertyId, BValue value, String readFault, boolean useFacets) {
      return new BBacnetVirtualProperty(propertyId, value, readFault, useFacets);
   }

   protected boolean getAuditWrites() {
      return this.auditWrites;
   }

   protected void setAuditWrites(boolean aw) {
      this.auditWrites = aw;
   }

   protected BVirtualComponentSpace makeVirtualSpace() {
      BVirtualComponentSpace vspc = super.makeVirtualSpace();
      vspc.setLoadCallbacks(new BBacnetVirtualGateway.MyLoadCallbacks());
      return vspc;
   }

   public Slot loadVirtualSlot(BVirtualComponent parent, String virtualPathName) {
      if (parent == null) {
         return null;
      } else {
         Slot result = null;
         if (virtualPathName != null) {
            synchronized (this.SLOT_LOCK) {
               String virtualSlotName = SlotPath.escape(virtualPathName);
               result = parent.getSlot(virtualSlotName);
               if (result == null) {
                  result = this.addVirtualSlot(parent, virtualPathName);
                  if (result != null) {
                     String nameAssigned = result.getName();
                     if (!nameAssigned.equals(virtualSlotName)) {
                        log.warning("Name of virtual slot added is inconsistent: \"" + nameAssigned + "\" was expected to be \"" + virtualSlotName + "\"");
                     }
                  }
               } else if (parent instanceof BBacnetVirtualProperty) {
                  BBacnetVirtualProperty vp = (BBacnetVirtualProperty)parent;
                  if (result.isProperty()) {
                     BValue v = parent.get(result.asProperty());
                     if (v instanceof BBacnetArray) {
                        BBacnetArray a = (BBacnetArray)v;
                        if (!a.getFixedSize()) {
                           int size = this.readArraySize(vp);
                           a.setSize(size);
                        }
                     }
                  }
               }
            }
         }

         return result;
      }
   }

   protected Property addVirtualSlot(BVirtualComponent parent, String virtualPathName) {
      try {
         if (!(parent instanceof BBacnetVirtualProperty)) {
            if (parent instanceof BBacnetVirtualObject) {
               BBacnetVirtualObject o = (BBacnetVirtualObject)parent;
               int scndx = virtualPathName.indexOf(";");
               String propertyName = scndx > 0 ? virtualPathName.substring(0, scndx) : virtualPathName;
               String virtualPropertyName = SlotPath.escape(virtualPathName);
               int propertyId = BBacnetPropertyIdentifier.ordinal(propertyName);
               PropertyInfo pi = this.device().getPropertyInfo(o.getObjectId().getObjectType(), propertyId);
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
                     } else if (pi.getType() != null) {
                        spec = BTypeSpec.make(pi.getType());
                        v = (BValue)spec.getInstance();
                     } else {
                        v = BString.make("--");
                     }

                     if (!pi.getFacetControl().equals("no")) {
                        useFacets = true;
                     }
                  } catch (Exception var14) {
                     String s = "Unknown Type:" + spec + " for " + BBacnetObjectType.tag(o.getObjectId().getObjectType()) + ":" + propertyName;
                     log.info("addVirtualSlot:" + s);
                     if (log.isLoggable(Level.FINE)) {
                        log.log(Level.FINE, "Exception occurred in addVirtualSlot: ", (Throwable)var14);
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
            log.log(Level.FINE, "Exception occurred in addVirtualSlot: ", (Throwable)var15);
         }
      }

      return null;
   }

   public void loadVirtualSlots(BVirtualComponent parent) {
      if (this.device() != null && this.device().isOperational()) {
         if (!(parent instanceof BBacnetVirtualProperty)) {
            if (parent instanceof BBacnetVirtualObject) {
               this.loadProperties((BBacnetVirtualObject)parent);
            } else {
               this.loadObjects(parent);
            }
         }
      }
   }

   public void updateStatus() {
      BComponentSpace space = this.getVirtualSpace();
      if (space != null) {
         BComponent root = space.getRootComponent();
         if (root != null) {
            SlotCursor<Property> sc = root.getProperties();

            while (sc.next(BBacnetVirtualObject.class)) {
               ((BBacnetVirtualObject)sc.get()).updateStatus();
            }
         }
      }
   }

   BBacnetDevice device() {
      return (BBacnetDevice)this.getParent();
   }

   BBacnetNetwork network() {
      return (BBacnetNetwork)this.device().getNetwork();
   }

   protected void loadObjects(BVirtualComponent parent) {
      try {
         BBacnetDeviceObject deviceObject = this.device().getConfig().getDeviceObject();
         deviceObject.readProperty(BBacnetDeviceObject.objectList);
         BBacnetObjectIdentifier[] objectList = (BBacnetObjectIdentifier[])deviceObject.getObjectList().getChildren(BBacnetObjectIdentifier.class);

         for (int i = 0; i < objectList.length; i++) {
            BBacnetObjectIdentifier objectId = objectList[i];
            String virtualPathName = objectId.toString(nameContext);
            String virtualObjectName = SlotPath.escape(virtualPathName);
            Property p = parent.getProperty(virtualObjectName);
            if (p == null) {
               p = parent.add(virtualObjectName, this.makeBacnetVirtualObject(virtualPathName), 2);
            }
         }
      } catch (Exception var9) {
         log.info("Unable to loadObjects in BacnetVirtualGateway!");
         if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "Exception occurred in loadObjects: ", (Throwable)var9);
         }
      }
   }

   protected void loadProperties(BBacnetVirtualObject parent) {
      boolean propertiesLoaded = false;
      AsnInputStream asnIn = new AsnInputStream();

      try {
         if (this.device().isServiceSupported("readPropertyMultiple")) {
            Vector refs = new Vector();
            refs.add(new NBacnetPropertyReference(8));

            for (NReadPropertyResult rpr : this.network().getBacnetComm().readPropertyMultiple(this.device().getAddress(), parent.getObjectId(), refs)) {
               int propertyId = rpr.getPropertyId();
               String propertyName = BBacnetPropertyIdentifier.tag(propertyId);
               String virtualPropertyName = SlotPath.escape(propertyName);
               Property p = parent.getProperty(virtualPropertyName);
               if (p == null) {
                  PropertyInfo pi = this.device().getPropertyInfo(parent.getObjectId().getObjectType(), propertyId);
                  String readFault = null;
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
                        } else if (pi.getType() != null) {
                           spec = BTypeSpec.make(pi.getType());
                           v = (BValue)spec.getInstance();
                        } else {
                           v = BString.make("--");
                        }

                        if (!pi.getFacetControl().equals("no")) {
                           useFacets = true;
                        }
                     } catch (Exception var22) {
                        String s = "Unknown Type:" + spec + " for " + BBacnetObjectType.tag(parent.getObjectId().getObjectType()) + ":" + propertyName;
                        log.info("addVirtualSlot:" + s);
                        if (log.isLoggable(Level.FINE)) {
                           log.log(Level.FINE, "Exception occurred in loadProperties: ", (Throwable)var22);
                        }

                        v = BString.make(s);
                     }
                  } else {
                     v = BString.make("");
                  }

                  if (!rpr.isError()) {
                     asnIn.setBuffer(rpr.getPropertyValue());
                     v = BacnetVirtualUtil.readValue(asnIn, v);
                  } else {
                     readFault = NErrorType.toString(rpr.getErrorClass(), rpr.getErrorCode());
                     v = BString.make("???");
                  }

                  parent.add(virtualPropertyName, this.makeBacnetVirtualProperty(propertyId, v, readFault, useFacets), 2, null);
               }
            }

            propertiesLoaded = true;
         }
      } catch (Exception var23) {
         log.info("Unable to loadProperties using RPM in BacnetVirtualGateway for " + parent + ":" + var23);
         if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "Exception occurred in loadProperties: ", (Throwable)var23);
         }
      }

      if (!propertiesLoaded) {
         try {
            int[] propertyIds = this.device().getRequiredProperties(parent.getObjectId());

            for (int i = 0; i < propertyIds.length; i++) {
               int propertyId = propertyIds[i];
               String propertyName = BBacnetPropertyIdentifier.tag(propertyId);
               String virtualPropertyName = SlotPath.escape(propertyName);
               Property p = parent.getProperty(virtualPropertyName);
               if (p == null) {
                  String readFaultx = null;
                  PropertyInfo pix = this.device().getPropertyInfo(parent.getObjectId().getObjectType(), propertyId);
                  boolean useFacetsx = false;
                  BTypeSpec specx = null;
                  BValue vx;
                  if (pix != null) {
                     try {
                        if (pix.isArray()) {
                           specx = BTypeSpec.make(pix.getType());
                           Type t = specx.getResolvedType();
                           int size = pix.getSize();
                           if (size > 0) {
                              vx = new BBacnetArray(t, size);
                           } else {
                              vx = new BBacnetArray(t);
                           }
                        } else if (pix.isList()) {
                           specx = BTypeSpec.make(pix.getType());
                           Type t = specx.getResolvedType();
                           vx = new BBacnetListOf(t);
                        } else if (pix.getType() != null) {
                           specx = BTypeSpec.make(pix.getType());
                           vx = (BValue)specx.getInstance();
                        } else {
                           vx = BString.make("--");
                        }

                        if (!pix.getFacetControl().equals("no")) {
                           useFacetsx = true;
                        }
                     } catch (Exception var20) {
                        String s = "Unknown Type:" + specx + " for " + BBacnetObjectType.tag(parent.getObjectId().getObjectType()) + ":" + propertyName;
                        log.info("addVirtualSlot:" + s);
                        if (log.isLoggable(Level.FINE)) {
                           log.log(Level.FINE, "Exception occurred in loadProperties: ", (Throwable)var20);
                        }

                        vx = BString.make(s);
                     }
                  } else {
                     vx = BString.make("");
                  }

                  try {
                     byte[] encodedValue = this.network().getBacnetComm().readProperty(this.device().getAddress(), parent.getObjectId(), propertyId);
                     asnIn.setBuffer(encodedValue);
                     vx = BacnetVirtualUtil.readValue(asnIn, vx);
                  } catch (BacnetException var19) {
                     readFaultx = var19.toString();
                     vx = BString.make("???");
                  }

                  parent.add(virtualPropertyName, this.makeBacnetVirtualProperty(propertyId, vx, readFaultx, useFacetsx), 2, null);
               }
            }
         } catch (Exception var21) {
            log.info("Unable to loadProperties using RP in BacnetVirtualGateway for " + parent + ":" + var21);
            if (log.isLoggable(Level.FINE)) {
               log.log(Level.FINE, "Exception occurred in loadProperties: ", (Throwable)var21);
            }
         }
      }
   }

   protected int readArraySize(BBacnetVirtualProperty vp) {
      try {
         byte[] encodedValue = this.network().getBacnetComm().readProperty(this.device().getAddress(), vp.object().getObjectId(), vp.getPropertyId(), 0);
         return AsnUtil.fromAsnInteger(encodedValue);
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

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.prop("auditWrites", this.auditWrites);
      out.endProps();
   }

   static class MyLoadCallbacks extends LoadCallbacks {
      public void loadSlots(BComponent c) {
         BComponentSpace space = c.getComponentSpace();
         if (space instanceof BVirtualComponentSpace) {
            if (c instanceof BVirtualComponent) {
               BVirtualGateway vGate = ((BVirtualComponentSpace)space).getVirtualGateway();
               if (vGate != null) {
                  vGate.loadVirtualSlots((BVirtualComponent)c);
               }
            } else if (c instanceof BBacnetArray) {
               BBacnetArray a = (BBacnetArray)c;
               Property e0 = a.getProperty("element0");
               if (e0 == null) {
                  a.add("element0", BInteger.make(-1), 2);
               }
            }
         }
      }
   }
}
