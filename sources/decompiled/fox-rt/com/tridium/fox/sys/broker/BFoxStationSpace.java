package com.tridium.fox.sys.broker;

import com.tridium.fox.sys.BIFoxGatewayProxyFactory;
import com.tridium.fox.sys.BIFoxProxySpace;
import com.tridium.space.BGateway;
import com.tridium.sys.schema.ComponentSlotMap;
import javax.baja.agent.AgentFilter;
import javax.baja.agent.AgentList;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.space.BSpace;
import javax.baja.sys.BComponent;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.LexiconText;
import javax.baja.virtual.BVirtualGateway;

@NiagaraType
public class BFoxStationSpace extends BFoxComponentSpace {
   public static final Type TYPE = Sys.loadType(BFoxStationSpace.class);

   @Override
   public Type getType() {
      return TYPE;
   }

   public BFoxStationSpace() {
      super("station", LexiconText.make("baja", "nav.station"), BOrd.make("station:"));
   }

   public Type[] getEnabledMixIns() {
      return this.channel.getConnection().getChannels().getSysChannel().getStationMixIns();
   }

   @Override
   public Object fw(int x, Object a, Object b, Object c, Object d) {
      switch (x) {
         case 101:
            super.fw(x, a, b, c, d);
            this.fwMount((ComponentSlotMap)a);
            return null;
         default:
            return super.fw(x, a, b, c, d);
      }
   }

   private void fwMount(ComponentSlotMap slotMap) {
      BComponent comp = (BComponent)slotMap.getInstance();
      if (comp instanceof BVirtualGateway) {
         BVirtualGateway gateway = (BVirtualGateway)comp;
         gateway.fw(108, new BFoxVirtualSpace(gateway), null, null, null);
      } else if (comp instanceof BGateway) {
         BSpace foxSpace = null;
         BGateway gateway = (BGateway)comp;
         AgentList agents = gateway.getAgents();
         agents = agents.filter(AgentFilter.is(BIFoxGatewayProxyFactory.TYPE));
         if (agents.size() == 0) {
            throw new IllegalStateException("Could not find BIFoxGatewayProxyFactory for " + comp.getType());
         }

         foxSpace = ((BIFoxGatewayProxyFactory)agents.getDefault().getInstance()).makeFoxGatewayProxySpace(gateway);
         if (!(foxSpace instanceof BIFoxProxySpace)) {
            throw new IllegalStateException("Fox gateway proxy space must implement BIFoxProxySpace");
         }

         gateway.fw(108, foxSpace, null, null, null);
      }
   }
}
