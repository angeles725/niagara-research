package com.tridium.fox.sys.broker;

import com.tridium.fox.sys.BFoxSession;
import com.tridium.space.BGateway;
import com.tridium.space.BIGatewaySpace;
import java.util.HashMap;
import javax.baja.naming.BHost;
import javax.baja.naming.BISession;
import javax.baja.naming.BOrd;
import javax.baja.nav.BINavNode;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.sys.BComponent;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.LexiconText;

@NiagaraType
public class BFoxGatewaySpace extends BFoxComponentSpace implements BIGatewaySpace {
   public static final Type TYPE = Sys.loadType(BFoxGatewaySpace.class);
   protected final BGateway gateway;
   private boolean touchEnabled = false;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BFoxGatewaySpace() {
      super("foxgatewayspace", null, BOrd.NULL);
      this.gateway = null;
   }

   public BFoxGatewaySpace(BGateway gateway, LexiconText lexText) {
      super(gateway.getGatewaySchemeId(), lexText, gateway.getGatewaySchemeOrd());
      this.gateway = gateway;
   }

   public BGateway getGateway() {
      return this.gateway;
   }

   public BFoxSession getFoxSession() {
      return ((BFoxComponentSpace)this.gateway.getSpace()).channel.getFoxSession();
   }

   public BComponent getRootComponent() {
      BComponent c = super.getRootComponent();
      if (c == null) {
         try {
            BFoxSession foxSession = this.getFoxSession();
            this.init(foxSession);
         } catch (Exception var3) {
            var3.printStackTrace();
         }
      }

      return super.getRootComponent();
   }

   public void setTouchEnabled(boolean enabled) {
      this.touchEnabled = enabled;
   }

   public BHost getHost() {
      return this.gateway.getSpace().getHost();
   }

   public BISession getSession() {
      return this.gateway.getSpace().getSession();
   }

   public BOrd getOrdInSession() {
      return BOrd.make(this.gateway.getOrdInSession(), this.gateway.getGatewaySchemeOrd());
   }

   public BOrd getAbsoluteOrd() {
      return BOrd.make(this.gateway.getAbsoluteOrd(), this.gateway.getGatewaySchemeOrd());
   }

   public BOrd getOrdInHost() {
      return BOrd.make(this.gateway.getOrdInHost(), this.gateway.getGatewaySchemeOrd());
   }

   public BOrd getNavOrd() {
      return this.gateway.getNavOrd();
   }

   public BINavNode getNavParent() {
      return this.gateway;
   }

   public boolean hasNavChildren() {
      return this.getRootComponent() != null;
   }

   public BINavNode getNavChild(String navName) {
      return this.getRootComponent().getNavChild(navName);
   }

   public BINavNode resolveNavChild(String navName) {
      return this.getRootComponent().resolveNavChild(navName);
   }

   public BINavNode[] getNavChildren() {
      return this.getRootComponent().getNavChildren();
   }

   @Override
   public Object fw(int x, Object a, Object b, Object c, Object d) {
      if (x == 110 && this.touchEnabled) {
         this.touch((BOrd[])a);
      }

      return super.fw(x, a, b, c, d);
   }

   protected void touch(BOrd[] paths) {
      try {
         Array<String> list = new Array(String.class);
         HashMap<String, String> dups = new HashMap<>();

         for (int i = 0; i < paths.length; i++) {
            String x = paths[i].encodeToString();
            if (dups.get(x) == null) {
               dups.put(x, x);
               list.add(x);
            }
         }

         if (list.size() != 0) {
            this.channel().touch((String[])list.trim());
         }
      } catch (Throwable var6) {
         log.severe("touch(): " + var6);
         throw BFoxSession.toException(var6);
      }
   }
}
