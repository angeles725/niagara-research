package com.tridium.fox.sys.broker;

import com.tridium.fox.sys.BFoxChannelRegistry;
import com.tridium.fox.sys.BFoxSession;
import com.tridium.fox.sys.BIFoxProxySpace;
import com.tridium.sys.schema.ComponentSlotMap;
import com.tridium.sys.transfer.DeleteOp;
import com.tridium.sys.transfer.RemoteTransferSpace;
import com.tridium.sys.transfer.TransferResult;
import com.tridium.sys.transfer.TransferStrategy;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.naming.BOrd;
import javax.baja.naming.SlotPath;
import javax.baja.naming.UnresolvedException;
import javax.baja.net.NotConnectedException;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.space.BComponentSpace;
import javax.baja.space.LoadCallbacks;
import javax.baja.space.SubscribeCallbacks;
import javax.baja.spy.SpyWriter;
import javax.baja.sync.BProxyComponentSpace;
import javax.baja.sync.SyncBuffer;
import javax.baja.sync.SyncOp;
import javax.baja.sync.Transaction;
import javax.baja.sync.TrapToSyncBuffer;
import javax.baja.sys.Action;
import javax.baja.sys.ActionInvokeException;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BValue;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.CopyHints;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.LexiconText;
import javax.baja.virtual.BVirtualComponent;
import javax.baja.virtual.VirtualPath;

@NiagaraType
public abstract class BFoxComponentSpace extends BProxyComponentSpace implements RemoteTransferSpace, BIFoxProxySpace {
   public static final Type TYPE = Sys.loadType(BFoxComponentSpace.class);
   public static final Logger log = Logger.getLogger("FoxSpace");
   BBrokerChannel channel;
   HashMap<BComponent, BAbsTime> subscribed = new HashMap<>();
   boolean spaceReadonly = false;
   long defaultLeaseTime = 60000L;

   public Type getType() {
      return TYPE;
   }

   public BFoxComponentSpace(String name, LexiconText lexText, BOrd ordInSession) {
      super(name, lexText, ordInSession);
      this.setLoadCallbacks(new BFoxComponentSpace.FoxLoadCallbacks());
      this.setTrapCallbacks(new BFoxComponentSpace.FoxTrapCallbacks());
      this.setSubscribeCallbacks(new BFoxComponentSpace.FoxSubscribeCallbacks());
   }

   @Override
   public void init(BFoxSession foxSession) throws Exception {
      BFoxChannelRegistry channels = foxSession.getConnection().getChannels();
      this.channel = channels.getSysChannel().makeBrokerChannel(this);
   }

   @Override
   public void cleanup(BFoxSession foxSession) throws Exception {
      BFoxChannelRegistry channels = foxSession.getConnection().getChannels();
      this.channel.cleanupClient();
      channels.remove(this.channel.getPropertyInParent());
      this.channel = null;
   }

   public boolean isSpaceReadonly() {
      return this.spaceReadonly;
   }

   public long getDefaultLeaseTime() {
      return this.defaultLeaseTime;
   }

   protected BComponent loadByHandle(Object handle) {
      SlotPath path = this.handleToSlotPath(handle);
      if (path == null) {
         throw new UnresolvedException(String.valueOf(handle));
      } else {
         try {
            return (BComponent)BOrd.make(path).get(this);
         } catch (Throwable var4) {
            log.log(Level.SEVERE, "loadByHandle(" + handle + "): " + var4);
            throw BFoxSession.toException(var4);
         }
      }
   }

   public SlotPath handleToSlotPath(Object handle) {
      SlotPath path = super.handleToSlotPath(handle);
      if (path != null) {
         return path;
      } else {
         try {
            return this.channel().handleToPath(new Object[]{handle})[0];
         } catch (Throwable var4) {
            log.log(Level.SEVERE, "handleToSlotPath(" + handle + "): " + var4);
            throw BFoxSession.toException(var4);
         }
      }
   }

   public SlotPath[] handlesToSlotPaths(Object[] handles) {
      boolean allFound = true;
      SlotPath[] paths = new SlotPath[handles.length];

      for (int i = 0; i < paths.length; i++) {
         paths[i] = super.handleToSlotPath(handles[i]);
         if (paths[i] == null) {
            allFound = false;
            break;
         }
      }

      if (allFound) {
         return paths;
      } else {
         try {
            return this.channel().handleToPath(handles);
         } catch (Throwable var5) {
            log.log(Level.SEVERE, "handlesToSlotPaths(" + handles.length + "): " + var5);
            throw BFoxSession.toException(var5);
         }
      }
   }

   public Object fw(int x, Object a, Object b, Object c, Object d) {
      switch (x) {
         case 103:
            return this.generateHandles((Integer)a);
         case 104:
            this.ensureLoaded((SlotPath[])a);
            return null;
         case 105:
         case 108:
         default:
            return super.fw(x, a, b, c, d);
         case 106:
            return this.delete((DeleteOp)a, false);
         case 107:
            return this.delete((DeleteOp)a, true);
         case 109:
            return this.rpc((BComponent)a, (String)b, (BValue)c);
      }
   }

   private Object[] generateHandles(int count) {
      try {
         return this.channel().generateHandles(count);
      } catch (Throwable var3) {
         log.log(Level.SEVERE, "generateHandles(): " + var3);
         throw BFoxSession.toException(var3);
      }
   }

   private DeleteOp delete(DeleteOp op, boolean undo) {
      try {
         op = this.channel().delete(op, undo);
         this.sync();
         return op;
      } catch (Throwable var4) {
         log.log(Level.SEVERE, "deleteOp(): " + var4);
         throw BFoxSession.toException(var4);
      }
   }

   private BValue rpc(BComponent t, String key, BValue arg) {
      try {
         return this.channel().rpc(t, key, arg);
      } catch (Throwable var5) {
         log.log(Level.SEVERE, "rpc(): " + var5);
         throw BFoxSession.toException(var5);
      }
   }

   public final BBrokerChannel channel() throws Exception {
      if (this.channel == null) {
         throw new NotConnectedException();
      } else {
         return this.channel;
      }
   }

   public void ensureLoaded(SlotPath[] paths) {
      try {
         Array<String> list = new Array(String.class);
         HashMap<String, String> dups = new HashMap<>();
         StringBuilder buf = new StringBuilder();

         for (int i = 0; i < paths.length; i++) {
            SlotPath path = paths[i];
            boolean isVirtual = path instanceof VirtualPath;
            buf.setLength(0);
            if (isVirtual) {
               buf.append("virtual:");
            } else {
               buf.append("slot:");
            }

            BValue val = this.getRootComponent();

            for (int j = 0; j < path.depth(); j++) {
               String name = path.nameAt(j);
               buf.append('/').append(name);
               if (isVirtual) {
                  name = SlotPath.escape(name);
               }

               if (val instanceof BComplex) {
                  Slot prop = ((BComplex)val).getSlot(name);
                  if (prop instanceof Property) {
                     val = ((BComplex)val).get((Property)prop);
                  } else if (prop == null) {
                     val = null;
                  }

                  if (val instanceof BComponent) {
                     ComponentSlotMap slotMap = (ComponentSlotMap)val.fw(1);
                     if (!slotMap.isBrokerPropsLoaded() && !(val instanceof BVirtualComponent)) {
                        val = null;
                     }
                  }
               }

               if (val == null) {
                  String x = buf.toString();
                  if (dups.get(x) == null) {
                     dups.put(x, x);
                     list.add(x);
                  }
               }
            }
         }

         if (list.size() != 0) {
            this.channel().ensureLoaded((String[])list.trim(), 0);
         }
      } catch (Throwable var13) {
         log.log(Level.SEVERE, "ensureLoaded(): " + var13);
         throw BFoxSession.toException(var13);
      }
   }

   public Transaction newTransaction(Context context) {
      return new Transaction(this, this.getEncodingContext(context)) {
         public void commit(Context cx) throws Exception {
            BFoxComponentSpace.this.channel().syncToMaster(this);
         }
      };
   }

   public Context getEncodingContext(Context base) {
      try {
         return this.channel().makeEncodeContext(base, false);
      } catch (RuntimeException var3) {
         throw var3;
      } catch (Exception var4) {
         throw new BajaRuntimeException(var4);
      }
   }

   public void sync() throws Exception {
      this.channel().syncFromMaster();
   }

   public boolean fireDirectCallbacks() {
      return false;
   }

   public TransferResult transfer(TransferStrategy strategy) throws Exception {
      try {
         return this.channel().transfer(strategy);
      } catch (Exception var3) {
         throw BFoxSession.toException(var3);
      }
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps("Subscribed");

      for (BComponent c : this.subscribed.keySet()) {
         BAbsTime t = this.subscribed.get(c);
         out.tr(c.toPathString(), t);
      }

      out.endProps();
   }

   class AutoCommitSyncBuffer extends SyncBuffer {
      AutoCommitSyncBuffer(BComponentSpace space) {
         super(space, false);
      }

      public void add(SyncOp op) {
         super.add(op);

         try {
            BFoxComponentSpace.this.channel().syncToMaster(this);
         } catch (Throwable var3) {
            BFoxComponentSpace.log.log(Level.SEVERE, "AutoCommitSyncBuffer: " + var3);
            throw BFoxSession.toException(var3);
         }
      }
   }

   class FoxLoadCallbacks extends LoadCallbacks {
      public boolean isLazyLoad() {
         return true;
      }

      public void loadSlots(BComponent c) {
         try {
            BFoxComponentSpace.this.channel().load(c, 0);
         } catch (Throwable var3) {
            BFoxComponentSpace.log.log(Level.SEVERE, "loadSlots(" + c.toPathString() + "): " + var3);
            throw BFoxSession.toException(var3);
         }
      }

      public BValue[] newCopy(BValue[] v, CopyHints hints) {
         try {
            return BFoxComponentSpace.this.channel().copy(v, hints);
         } catch (Throwable var4) {
            BFoxComponentSpace.log.log(Level.SEVERE, "newCopy():" + var4);
            throw BFoxSession.toException(var4);
         }
      }
   }

   class FoxSubscribeCallbacks extends SubscribeCallbacks {
      public void update(BComponent c, int depth) {
         try {
            BFoxComponentSpace.this.channel().load(c, depth);
         } catch (Throwable var4) {
            BFoxComponentSpace.log.log(Level.SEVERE, "update(" + c.toPathString() + "): " + var4);
            throw BFoxSession.toException(var4);
         }
      }

      public void subscribe(BComponent[] c, int depth) {
         if (depth == 0) {
            synchronized (BFoxComponentSpace.this.subscribed) {
               Array<BComponent> needed = new Array(BComponent.class);

               for (int i = 0; i < c.length; i++) {
                  if (BFoxComponentSpace.this.subscribed.get(c[i]) == null) {
                     needed.add(c[i]);
                  }
               }

               if (needed.size() == 0) {
                  return;
               }

               c = (BComponent[])needed.trim();
            }
         }

         try {
            BFoxComponentSpace.this.channel().subscribe(c, depth);
         } catch (Throwable var8) {
            BFoxComponentSpace.log.log(Level.SEVERE, "subscribe(" + c[0].toPathString() + "): " + var8);
            throw BFoxSession.toException(var8);
         }

         synchronized (BFoxComponentSpace.this.subscribed) {
            for (int ix = 0; ix < c.length; ix++) {
               this.addToSubscribed(c[ix], depth);
            }
         }
      }

      void addToSubscribed(BComponent c, int depth) {
         BFoxComponentSpace.this.subscribed.put(c, Clock.time());
         if (depth > 0) {
            BComponent[] kids = c.getChildComponents();

            for (int i = 0; i < kids.length; i++) {
               this.addToSubscribed(kids[i], depth - 1);
            }
         }
      }

      public void unsubscribe(BComponent[] c) {
         synchronized (BFoxComponentSpace.this.subscribed) {
            for (int i = 0; i < c.length; i++) {
               BFoxComponentSpace.this.subscribed.remove(c[i]);
            }
         }

         try {
            BFoxComponentSpace.this.channel().unsubscribe(c);
         } catch (NotConnectedException var5) {
         } catch (Exception var6) {
            BFoxComponentSpace.log.log(Level.SEVERE, "Cannot unsubscribe: " + c[0].toPathString(), (Throwable)var6);
         }
      }
   }

   class FoxTrapCallbacks extends TrapToSyncBuffer {
      public SyncBuffer getBuffer() {
         return BFoxComponentSpace.this.new AutoCommitSyncBuffer(BFoxComponentSpace.this);
      }

      public BValue invoke(BComponent c, Action action, BValue arg, Context context) {
         try {
            return BFoxComponentSpace.this.channel().invoke(c, action, arg);
         } catch (Exception var6) {
            BFoxComponentSpace.log.log(Level.SEVERE, "Cannot invoke: " + c.toPathString() + "." + action);
            throw new ActionInvokeException(var6);
         }
      }
   }
}
