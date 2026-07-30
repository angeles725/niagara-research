package com.tridium.basicdriver.util;

import com.tridium.basicdriver.BBasicNetwork;
import com.tridium.basicdriver.point.BBasicProxyExt;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;
import javax.baja.driver.util.BIPollable;
import javax.baja.driver.util.BPollFrequency;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BStruct;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public abstract class BBasicPollGroup extends BStruct implements BIBasicPollable {
   public static final Type TYPE = Sys.loadType(BBasicPollGroup.class);
   static HashMap<Integer, Hashtable<Object, BBasicPollGroup>> typesToCodes = new HashMap<>();
   Object code;
   Vector<BBasicProxyExt> mySubscribedProxies;
   Vector<BBasicProxyExt> myProxies;
   boolean subscribed = false;

   public Type getType() {
      return TYPE;
   }

   public BBasicPollGroup() {
      this.code = null;
      this.mySubscribedProxies = new Vector<>(8);
      this.myProxies = new Vector<>(8);
      this.subscribed = false;
   }

   public Object getCode() {
      return this.code;
   }

   protected final BBasicProxyExt[] getProxyExts() {
      BBasicProxyExt[] pexts = new BBasicProxyExt[this.myProxies.size()];
      this.myProxies.copyInto(pexts);
      return pexts;
   }

   protected final BBasicProxyExt[] getSubscribedProxyExts() {
      BBasicProxyExt[] pexts = new BBasicProxyExt[this.mySubscribedProxies.size()];
      this.mySubscribedProxies.copyInto(pexts);
      return pexts;
   }

   public void registerProxy(BBasicProxyExt proxy) {
      if (!this.myProxies.contains(proxy)) {
         this.myProxies.add(proxy);
      }
   }

   public void unregisterProxy(BBasicProxyExt proxy) {
      try {
         this.readUnsubscribed(proxy);
      } catch (Exception var6) {
      }

      this.myProxies.remove(proxy);
      Type pgType = proxy.getPollGroupType();
      Object pgCode = proxy.getPollGroupCode();
      Hashtable<Object, BBasicPollGroup> codesToGroups = typesToCodes.get(pgType.getId());
      if (codesToGroups != null) {
         BBasicPollGroup pg = codesToGroups.get(pgCode);
         if (pg != null) {
            codesToGroups.remove(pgCode);
         }

         if (codesToGroups.size() < 1) {
            typesToCodes.remove(pgType.getId());
         }
      }
   }

   public void readSubscribed(BBasicProxyExt proxy) throws Exception {
      if (!this.mySubscribedProxies.contains(proxy)) {
         this.mySubscribedProxies.add(proxy);
      }

      if (!this.subscribed) {
         ((BBasicNetwork)proxy.getNetwork()).getPollScheduler().subscribe(this);
         this.subscribed = true;
      }
   }

   public void readUnsubscribed(BBasicProxyExt proxy) throws Exception {
      this.mySubscribedProxies.remove(proxy);
      if (this.mySubscribedProxies.size() == 0) {
         ((BBasicNetwork)proxy.getNetwork()).getPollScheduler().unsubscribe(this);
         this.subscribed = false;
      }
   }

   @Override
   public abstract void poll();

   public BPollFrequency getPollFrequency() {
      BPollFrequency fastestYet = BPollFrequency.slow;

      for (BBasicProxyExt proxy : this.mySubscribedProxies) {
         if (proxy instanceof BIPollable) {
            BPollFrequency fr = ((BIPollable)proxy).getPollFrequency();
            if (fr.compareTo(fastestYet) < 0) {
               fastestYet = fr;
            }
         }
      }

      return fastestYet;
   }

   public static BBasicPollGroup getPollGroup(BBasicProxyExt proxy) {
      Type pgType = proxy.getPollGroupType();
      Object pgCode = proxy.getPollGroupCode();
      Hashtable<Object, BBasicPollGroup> codesToGroups = typesToCodes.get(pgType.getId());
      if (codesToGroups == null) {
         codesToGroups = new Hashtable<>();
         typesToCodes.put(pgType.getId(), codesToGroups);
      }

      BBasicPollGroup pg = codesToGroups.get(pgCode);
      if (pg == null) {
         pg = (BBasicPollGroup)pgType.getInstance();
         pg.code = pgCode;
         codesToGroups.put(pgCode, pg);
      }

      return pg;
   }
}
