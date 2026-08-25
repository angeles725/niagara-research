package com.tridium.nre.firewall;

import java.util.Objects;

public final class RedirectRule extends FirewallRule {
   private int publicServerPort;
   private int localServerPort;
   private IpProtocol ipProtocol;
   private String adapter;
   private boolean bindToLoopback = false;

   public RedirectRule(int publicServerPort, int localServerPort, IpProtocol ipProtocol, String adapter) {
      this.ruleType = FirewallRule.RuleType.REDIRECT_RULE;
      this.publicServerPort = publicServerPort;
      this.localServerPort = localServerPort;
      this.ipProtocol = ipProtocol;
      this.adapter = adapter;
   }

   public RedirectRule(int publicServerPort, int localServerPort, IpProtocol ipProtocol, String adapter, boolean bindToLoopback) {
      this(publicServerPort, localServerPort, ipProtocol, adapter);
      this.bindToLoopback = bindToLoopback;
   }

   public int getPublicServerPort() {
      return this.publicServerPort;
   }

   public int getLocalServerPort() {
      return this.localServerPort;
   }

   public IpProtocol getIpProtocol() {
      return this.ipProtocol;
   }

   public String getAdapter() {
      return this.adapter;
   }

   public boolean getBindToLoopback() {
      return this.bindToLoopback;
   }

   @Override
   public boolean equivalentTo(FirewallRule rule) {
      if (rule == null) {
         return false;
      }

      if (rule.getRuleType() == FirewallRule.RuleType.REDIRECT_RULE) {
         RedirectRule rrule = (RedirectRule)rule;
         if (this.publicServerPort == rrule.publicServerPort
            && this.localServerPort == rrule.localServerPort
            && this.adapter.equals(rrule.adapter)
            && this.ipProtocol == rrule.ipProtocol) {
            return true;
         }
      }

      return false;
   }

   @Override
   public boolean equals(Object o) {
      if (o instanceof RedirectRule) {
         RedirectRule rule = (RedirectRule)o;
         if (rule.publicServerPort == this.publicServerPort) {
            return true;
         }
      }

      return false;
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(this.publicServerPort);
   }

   @Override
   public String toString() {
      return String.format("%s:%d->%d %s on %s", this.ruleType.name(), this.publicServerPort, this.localServerPort, this.ipProtocol.name(), this.adapter)
         .toLowerCase();
   }
}
