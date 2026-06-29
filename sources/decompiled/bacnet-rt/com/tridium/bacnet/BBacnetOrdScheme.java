package com.tridium.bacnet;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BIBacnetObjectContainer;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.export.BIBacnetExportObject;
import javax.baja.bacnet.export.BLocalBacnetDevice;
import javax.baja.naming.BOrd;
import javax.baja.naming.BOrdScheme;
import javax.baja.naming.OrdQuery;
import javax.baja.naming.OrdTarget;
import javax.baja.naming.SyntaxException;
import javax.baja.naming.UnresolvedException;
import javax.baja.nre.annotations.NiagaraSingleton;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.schedule.BScheduleReference;
import javax.baja.sys.BObject;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType(
   ordScheme = "bac"
)
@NiagaraSingleton
public class BBacnetOrdScheme extends BOrdScheme {
   public static final BBacnetOrdScheme INSTANCE = new BBacnetOrdScheme();
   public static final Type TYPE = Sys.loadType(BBacnetOrdScheme.class);
   private static final Logger logger = Logger.getLogger("bacnet");

   public Type getType() {
      return TYPE;
   }

   private BBacnetOrdScheme() {
      super("bac");
   }

   public OrdQuery parse(String queryBody) {
      return new BacnetQuery(queryBody);
   }

   private BBacnetNetwork findNetwork(BObject o) {
      try {
         BBacnetNetwork net = BBacnetNetwork.bacnet();
         if (net != null) {
            return net;
         } else {
            BOrd serviceOrd = BOrd.make("service:bacnet:BacnetNetwork");
            return (BBacnetNetwork)serviceOrd.get(o);
         }
      } catch (Exception var4) {
         logger.log(Level.SEVERE, "Error finding network", (Throwable)var4);
         return null;
      }
   }

   public OrdTarget resolve(OrdTarget base, OrdQuery query) throws SyntaxException, UnresolvedException {
      try {
         BacnetQuery q = (BacnetQuery)query;
         BObject baseObject = base.get();
         BBacnetNetwork net = this.findNetwork(baseObject);
         String d = q.getDevice();
         String o = q.getObject();
         String p = q.getProperty();
         String n = q.getIndex();
         BBacnetObjectIdentifier deviceId = BBacnetObjectIdentifier.DEFAULT;
         BBacnetObjectIdentifier objectId = BBacnetObjectIdentifier.DEFAULT;
         int propertyId = -1;
         int index = -1;
         deviceId = (BBacnetObjectIdentifier)deviceId.decodeFromString(d);
         if (!q.hasObject()) {
            BLocalBacnetDevice local = (BLocalBacnetDevice)net.getLocalDevice().loadSlots();
            if (deviceId.equals(local.getObjectId())) {
               return new OrdTarget(base, local);
            }

            BOrd ord = net.lookupDeviceOrdById(deviceId);
            BBacnetDevice dev = (BBacnetDevice)ord.get(net);
            return new OrdTarget(base, dev);
         }

         objectId = (BBacnetObjectIdentifier)objectId.decodeFromString(o);
         if (q.hasProperty()) {
            propertyId = Integer.parseInt(p);
         }

         if (q.hasIndex()) {
            index = Integer.parseInt(n);
         }

         if (baseObject instanceof BIBacnetObjectContainer) {
            BIBacnetObjectContainer oc = (BIBacnetObjectContainer)baseObject;
            BObject object = oc.lookupBacnetObject(objectId, propertyId, index, q.getDomain());
            return new OrdTarget(base, object);
         }

         if (net != null) {
            if (deviceId.equals(net.getLocalDevice().getObjectId())) {
               BObject object = net.getLocalDevice().lookupBacnetObject(objectId, propertyId, index, q.getDomain());
               if (baseObject instanceof BScheduleReference) {
                  return new OrdTarget(base, ((BIBacnetExportObject)object).getObject());
               }

               return new OrdTarget(base, object);
            }

            BOrd ord = net.lookupDeviceOrdById(deviceId);
            BBacnetDevice device = (BBacnetDevice)ord.get(net);
            if (device != null) {
               BObject object = device.lookupBacnetObject(objectId, propertyId, index, q.getDomain());
               return new OrdTarget(base, object);
            }

            throw new UnresolvedException("" + baseObject + " [" + baseObject.getType() + "] is not a valid base for resolving a Bacnet ord query!");
         }
      } catch (Exception var17) {
         throw new UnresolvedException("Unable to resolve Bacnet ord query:" + query + " against base:" + base, var17);
      }

      throw new UnresolvedException("Unable to resolve Bacnet ord query:" + query + " against base:" + base);
   }
}
