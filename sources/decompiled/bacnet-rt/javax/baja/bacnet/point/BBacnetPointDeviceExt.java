package javax.baja.bacnet.point;

import com.tridium.bacnet.job.BBacnetDiscoverPointsJob;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.baja.agent.AgentList;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BIBacnetObjectContainer;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.control.BControlPoint;
import javax.baja.driver.point.BPointDeviceExt;
import javax.baja.naming.BOrd;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.nre.util.LongHashMap;
import javax.baja.nre.util.LongHashMap.Iterator;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraAction(
   name = "submitPointDiscoveryJob",
   returnType = "BOrd",
   flags = 4
)
public class BBacnetPointDeviceExt extends BPointDeviceExt implements BIBacnetObjectContainer {
   public static final Action submitPointDiscoveryJob = newAction(4, null);
   public static final Type TYPE = Sys.loadType(BBacnetPointDeviceExt.class);
   private LongHashMap byPointHash = new LongHashMap();
   private Hashtable<BBacnetObjectIdentifier, Array<BControlPoint>> byObjectId = new Hashtable<>();

   public BOrd submitPointDiscoveryJob() {
      return (BOrd)this.invoke(submitPointDiscoveryJob, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public Type getDeviceType() {
      return BBacnetDevice.TYPE;
   }

   public Type getProxyExtType() {
      return BBacnetProxyExt.TYPE;
   }

   public Type getPointFolderType() {
      return BBacnetPointFolder.TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BBacnetDevice;
   }

   public BOrd doSubmitPointDiscoveryJob(Context cx) {
      return this.device().isFatalFault() ? null : new BBacnetDiscoverPointsJob(this).submit(cx);
   }

   @Override
   public BObject lookupBacnetObject(BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex, String domain) {
      return this.findPoint(objectId, propertyId, propertyArrayIndex);
   }

   void registerPoint(BBacnetProxyExt pExt) {
      synchronized (this.byPointHash) {
         this.byPointHash.put(hash(pExt), pExt.getParentPoint());
         Array<BControlPoint> a = this.byObjectId.get(pExt.getObjectId());
         if (a == null) {
            a = new Array(BControlPoint.class);
         }

         a.add(pExt.getParentPoint());
         this.byObjectId.put(pExt.getObjectId(), a);
      }
   }

   void unregisterPoint(BBacnetProxyExt pExt) {
      BControlPoint pt = pExt.getParentPoint();
      synchronized (this.byPointHash) {
         Iterator it = this.byPointHash.iterator();

         while (it.hasNext()) {
            BControlPoint p = (BControlPoint)it.next();
            if (p.equals(pt)) {
               this.byPointHash.remove(it.key());
            }
         }

         Array<BControlPoint> a = this.byObjectId.get(pExt.getObjectId());
         if (a != null) {
            a.remove(pExt.getParentPoint());
            if (a.size() == 0) {
               this.byObjectId.values().remove(a);
            }
         }
      }
   }

   void reregisterPoint(BBacnetProxyExt pExt) {
      synchronized (this.byPointHash) {
         this.unregisterPoint(pExt);
         this.registerPoint(pExt);
      }
   }

   private static long hash(BBacnetProxyExt pExt) {
      return hash(pExt.getObjectId().hashCode(), pExt.getPropertyId().getOrdinal(), pExt.getPropertyArrayIndex());
   }

   private static long hash(int objectId, int propertyId, int propertyArrayIndex) {
      return (long)objectId << 32 | (propertyId & 65535) << 16 | propertyArrayIndex & 65535;
   }

   public final BBacnetDevice device() {
      return (BBacnetDevice)this.getDevice();
   }

   public final BControlPoint findPoint(BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex) {
      return (BControlPoint)this.byPointHash.get(hash(objectId.hashCode(), propertyId, propertyArrayIndex));
   }

   public final BControlPoint[] findPoints(BBacnetObjectIdentifier objectId) {
      Array<BControlPoint> a = this.byObjectId.get(objectId);
      return a != null ? (BControlPoint[])a.trim() : new BControlPoint[0];
   }

   public final BControlPoint[] findPoints(BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex) {
      Array<BControlPoint> a = this.byObjectId.get(objectId);
      if (a != null) {
         int size = a.size();
         if (size > 0) {
            if (size == 1 && pointMatches((BControlPoint)a.get(0), propertyId, propertyArrayIndex)) {
               return (BControlPoint[])a.trim();
            }

            Array<BControlPoint> filtered = new Array(BControlPoint.class);

            for (int i = 0; i < size; i++) {
               BControlPoint point = (BControlPoint)a.get(i);
               if (pointMatches(point, propertyId, propertyArrayIndex)) {
                  filtered.add(point);
               }
            }

            return (BControlPoint[])filtered.trim();
         }
      }

      return new BControlPoint[0];
   }

   private static boolean pointMatches(BControlPoint point, int propertyId, int propertyArrayIndex) {
      if (!(point.getProxyExt() instanceof BBacnetProxyExt)) {
         return false;
      } else {
         BBacnetProxyExt proxy = (BBacnetProxyExt)point.getProxyExt();
         return proxy.getPropertyId().getOrdinal() == propertyId && proxy.getPropertyArrayIndex() == propertyArrayIndex;
      }
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetPointDeviceExt", 2);
      out.trTitle("byPointHash:" + this.byPointHash.size(), 2);
      Iterator it = this.byPointHash.iterator();

      while (it.hasNext()) {
         BControlPoint pt = (BControlPoint)it.next();
         out.prop("  " + Long.toHexString(it.key()), SlotPath.unescape(pt.getName()) + ":" + pt.getHandleOrd());
      }

      out.trTitle("byObjectId:" + this.byObjectId.size(), 2);
      Enumeration<BBacnetObjectIdentifier> e = this.byObjectId.keys();

      while (e.hasMoreElements()) {
         BBacnetObjectIdentifier k = e.nextElement();
         out.prop("  " + k, this.byObjectId.get(k));
      }

      out.endProps();
   }

   public AgentList getAgents(Context cx) {
      AgentList agents = super.getAgents(cx);
      agents.remove("driver:PointManager");
      agents.toBottom("bacnetEDE:EdeBacnetPointManager");
      return agents;
   }
}
