package com.tridium.fox.sys.broker;

import com.tridium.fox.sys.BFoxSession;
import com.tridium.sys.schema.ComponentSlotMap;
import java.util.HashMap;
import java.util.logging.Level;
import javax.baja.naming.BHost;
import javax.baja.naming.BISession;
import javax.baja.naming.BOrd;
import javax.baja.nav.BINavNode;
import javax.baja.nav.BNavRoot;
import javax.baja.nav.NavEvent;
import javax.baja.nav.NavListener;
import javax.baja.net.NotConnectedException;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.sys.BComponent;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.LexiconText;
import javax.baja.virtual.BVirtualComponent;
import javax.baja.virtual.BVirtualGateway;

@NiagaraType
public class BFoxVirtualSpace extends BFoxComponentSpace implements NavListener {
   public static final Type TYPE = Sys.loadType(BFoxVirtualSpace.class);
   BVirtualGateway gateway;
   private volatile boolean unloaded = true;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BFoxVirtualSpace(BVirtualGateway gateway) {
      super("virtual", LexiconText.make("baja", "nav.virtual"), BOrd.make("virtual:"));
      this.setLoadCallbacks(new BFoxVirtualSpace.FoxVirtualLoadCallbacks());
      this.setSubscribeCallbacks(new BFoxVirtualSpace.FoxVirtualSubscribeCallbacks());
      this.gateway = gateway;
   }

   public BFoxVirtualSpace() {
      this(null);
   }

   public BVirtualGateway getVirtualGateway() {
      return this.gateway;
   }

   public BFoxSession getFoxSession() {
      return ((BFoxComponentSpace)this.gateway.getSpace()).channel.getFoxSession();
   }

   public BComponent getRootComponent() {
      BComponent c = super.getRootComponent();
      if (c == null || this.unloaded) {
         try {
            BFoxSession foxSession = this.getFoxSession();
            this.init(foxSession);
            BNavRoot.INSTANCE.addNavListener(this);
         } catch (Exception var6) {
            var6.printStackTrace();
         } finally {
            this.unloaded = false;
         }
      }

      return super.getRootComponent();
   }

   public void setRootComponent(BComponent root) {
      BComponent existingRoot = super.getRootComponent();
      if (existingRoot == null) {
         super.setRootComponent(root);
      } else {
         ((ComponentSlotMap)existingRoot.fw(1)).setHandle(root.getHandle());
      }
   }

   @Override
   public void cleanup(BFoxSession foxSession) throws Exception {
      super.cleanup(foxSession);
      BNavRoot.INSTANCE.removeNavListener(this);
      this.unloaded = true;
   }

   public void childUnparented(BComponent c, Property property, BValue oldChild, Context context) {
      super.childUnparented(c, property, oldChild, context);
      if (c instanceof BVirtualComponent) {
         ((ComponentSlotMap)c.fw(1)).setBrokerPropsLoaded(false);
      }
   }

   public BHost getHost() {
      return this.gateway.getSpace().getHost();
   }

   public BISession getSession() {
      return this.gateway.getSpace().getSession();
   }

   public BOrd getOrdInSession() {
      return BOrd.make(this.gateway.getOrdInSession(), "virtual:");
   }

   public BOrd getAbsoluteOrd() {
      return BOrd.make(this.gateway.getAbsoluteOrd(), "virtual:");
   }

   public BOrd getOrdInHost() {
      return BOrd.make(this.gateway.getOrdInHost(), "virtual:");
   }

   public final BOrd getNavOrd() {
      return this.gateway.getNavOrd();
   }

   public final BINavNode getNavParent() {
      return this.gateway;
   }

   public boolean hasNavChildren() {
      return true;
   }

   public BINavNode getNavChild(String navName) {
      this.getRootComponent();
      BINavNode child = super.getNavChild(navName);
      if (child == null) {
         this.checkUnloadSpace();
      }

      return child;
   }

   public BINavNode resolveNavChild(String navName) {
      this.getRootComponent();
      BINavNode child = super.resolveNavChild(navName);
      if (child == null) {
         this.checkUnloadSpace();
      }

      return child;
   }

   public BINavNode[] getNavChildren() {
      this.getRootComponent();
      BINavNode[] children = super.getNavChildren();
      if (children == null || children.length == 0) {
         this.checkUnloadSpace();
      }

      return children;
   }

   public void navEvent(NavEvent event) {
      BComponent root = super.getRootComponent();
      if (root != null && event.getId() == 2 && event.getParent() == root) {
         this.checkUnloadSpace();
      }
   }

   private void checkUnloadSpace() {
      if (!this.unloaded) {
         try {
            BComponent root = super.getRootComponent();
            if (root.isSubscribed()) {
               return;
            }

            boolean noDynamicChildren = true;
            SlotCursor<Property> props = root.getProperties();

            while (props.next(BComponent.class)) {
               if (props.property().isDynamic()) {
                  noDynamicChildren = false;
                  break;
               }
            }

            if (noDynamicChildren) {
               this.channel().unloadVirtualBroker();
            }
         } catch (Throwable var4) {
            String path = this.gateway != null ? this.gateway.toPathString() : "a null virtual gateway";
            log.log(Level.WARNING, "Could not unload fox virtual space for " + path, var4);
         }
      }
   }

   @Override
   public Object fw(int x, Object a, Object b, Object c, Object d) {
      switch (x) {
         case 110:
            this.touch((BOrd[])a);
            break;
         case 117:
            if (super.getRootComponent() == a) {
               this.getRootComponent();
            }
            break;
         case 118:
            if (super.getRootComponent() == a) {
               this.checkUnloadSpace();
            }
      }

      return super.fw(x, a, b, c, d);
   }

   void touch(BOrd[] paths) {
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

         if (list.size() == 0) {
            return;
         }

         this.channel().touch((String[])list.trim());
      } catch (NotConnectedException var6) {
      } catch (Throwable var7) {
         log.severe("touch(): " + var7);
         throw BFoxSession.toException(var7);
      }
   }

   class FoxVirtualLoadCallbacks extends BFoxComponentSpace.FoxLoadCallbacks {
      public Slot loadSlot(BComponent c, String slotName) {
         try {
            Slot slot = c.getSlot(slotName);
            if (slot != null) {
               if (!slot.isProperty()) {
                  return slot;
               }

               BValue value = c.get(slot.asProperty());
               if (!value.isComponent()) {
                  return slot;
               }

               if (value.asComponent().getComponentSpace() != null) {
                  return slot;
               }
            }
         } catch (Throwable var6) {
         }

         try {
            return BFoxVirtualSpace.this.channel().loadSlot(c, slotName, 0);
         } catch (Throwable var5) {
            BFoxComponentSpace.log.severe("loadSlot(" + c.toPathString() + ", " + slotName + "): " + var5);
            throw BFoxSession.toException(var5);
         }
      }
   }

   class FoxVirtualSubscribeCallbacks extends BFoxComponentSpace.FoxSubscribeCallbacks {
      @Override
      public void update(BComponent c, int depth) {
         try {
            BFoxVirtualSpace.this.channel().load(c, depth, true);
         } catch (Throwable var4) {
            BFoxComponentSpace.log.severe("update(" + c.toPathString() + "): " + var4);
            throw BFoxSession.toException(var4);
         }
      }
   }
}
