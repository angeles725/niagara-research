package com.tridium.nre.firewall;

import com.tridium.nre.firewall.pf.InvalidRuleException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class FirewallProcessor {
   protected final List<FirewallRule> ruleList = Collections.synchronizedList(new ArrayList<>());
   protected Logger logger = Logger.getLogger("firewall");

   public FirewallRule addRule(FirewallRule rule) throws InvalidRuleException {
      this.ruleList.remove(rule);
      if (this.logger.isLoggable(Level.FINE)) {
         this.logger.fine("adding rule " + rule.toString());
      }

      this.ruleList.add(rule);
      if (this.logger.isLoggable(Level.FINEST)) {
         this.dumpRuleList();
      }

      return rule;
   }

   public void removeRule(FirewallRule rule) {
      if (this.logger.isLoggable(Level.FINE)) {
         this.logger.fine("removing rule " + rule.toString());
      }

      this.ruleList.remove(rule);
      if (this.logger.isLoggable(Level.FINEST)) {
         this.dumpRuleList();
      }
   }

   public FirewallRule[] getRulesList() {
      return this.ruleList.toArray(new FirewallRule[0]);
   }

   public void dumpRuleList() {
      Iterator<FirewallRule> iterator = this.ruleList.iterator();
      if (this.logger.isLoggable(Level.FINEST)) {
         this.logger.finest("Rule List:");

         while (iterator.hasNext()) {
            FirewallRule rule = iterator.next();
            this.logger.finest("  -> " + rule);
         }
      }
   }

   public String getFirewallName() {
      return this.getClass().getSimpleName();
   }

   public abstract String getDescription();

   public abstract FirewallRule validateRule(FirewallRule var1) throws InvalidRuleException;

   public abstract void processRules();
}
