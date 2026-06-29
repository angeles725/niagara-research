package javax.baja.bacnet.point;

import java.util.ArrayList;
import javax.baja.agent.AgentList;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BIBacnetObjectContainer;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.control.BControlPoint;
import javax.baja.driver.point.BPointFolder;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BBacnetPointFolder extends BPointFolder implements BIBacnetObjectContainer {
   public static final Type TYPE = Sys.loadType(BBacnetPointFolder.class);

   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BBacnetPointDeviceExt || parent instanceof BBacnetPointFolder;
   }

   @Override
   public BObject lookupBacnetObject(BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex, String domain) {
      return this.findPoint(objectId, propertyId, propertyArrayIndex);
   }

   public final BBacnetDevice device() {
      return (BBacnetDevice)this.getDevice();
   }

   public final BControlPoint findPoint(BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex) {
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BControlPoint.class)) {
         BControlPoint pt = (BControlPoint)c.get();
         Type type = pt.getProxyExt().getType();
         if (type.is(BBacnetProxyExt.TYPE)) {
            BBacnetProxyExt ext = (BBacnetProxyExt)pt.getProxyExt();
            if (ext.getObjectId().equals(objectId) && ext.getPropertyId().getOrdinal() == propertyId && ext.getPropertyArrayIndex() == propertyArrayIndex) {
               return pt;
            }
         }
      }

      return null;
   }

   public final BControlPoint[] findPoints(BBacnetObjectIdentifier objectId) {
      SlotCursor<Property> c = this.getProperties();
      ArrayList<BControlPoint> a = new ArrayList<>();

      while (c.next(BControlPoint.class)) {
         BControlPoint pt = (BControlPoint)c.get();
         Type type = pt.getProxyExt().getType();
         if (type == BBacnetNumericProxyExt.TYPE
            || type == BBacnetBooleanProxyExt.TYPE
            || type == BBacnetEnumProxyExt.TYPE
            || type == BBacnetStringProxyExt.TYPE) {
            BBacnetProxyExt ext = (BBacnetProxyExt)pt.getProxyExt();
            if (ext.getObjectId().equals(objectId)) {
               a.add(pt);
            }
         }
      }

      return a.toArray(new BControlPoint[0]);
   }

   public AgentList getAgents(Context cx) {
      AgentList agents = super.getAgents(cx);
      agents.remove("driver:PointManager");
      agents.toBottom("bacnetEDE:EdeBacnetPointManager");
      return agents;
   }

   public String toString(Context cx) {
      return "BacnetPointFolder:" + this.getName();
   }
}
