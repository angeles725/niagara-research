package javax.baja.lonworks.proxy;

import javax.baja.agent.AgentList;
import javax.baja.driver.point.BPointDeviceExt;
import javax.baja.driver.point.BPointFolder;
import javax.baja.lonworks.BLonDevice;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BLonPointDeviceExt extends BPointDeviceExt {
   public static final Type TYPE = Sys.loadType(BLonPointDeviceExt.class);

   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BLonDevice;
   }

   public boolean isChildLegal(BComponent child) {
      return !child.getType().is(BPointFolder.TYPE) || child.getType().is(BLonPointFolder.TYPE);
   }

   public Type getDeviceType() {
      return BLonDevice.TYPE;
   }

   public Type getPointFolderType() {
      return BLonPointFolder.TYPE;
   }

   public Type getProxyExtType() {
      return BLonProxyExt.TYPE;
   }

   public AgentList getAgents(Context cx) {
      AgentList agents = super.getAgents(cx);
      agents.remove("driver:PointManager");
      return agents;
   }
}
