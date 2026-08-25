package com.tridium.nre.firewall;

public abstract class FirewallRule {
   protected FirewallRule.RuleType ruleType = null;

   public FirewallRule.RuleType getRuleType() {
      return this.ruleType;
   }

   public abstract boolean equivalentTo(FirewallRule var1);

   @Override
   public abstract boolean equals(Object var1);

   @Override
   public abstract String toString();

   public enum RuleType {
      REDIRECT_RULE;
   }
}
