package com.tridium.nre.firewall;

import com.tridium.nre.firewall.pf.InvalidRuleException;

public class NullFirewallProcessor extends FirewallProcessor {
   @Override
   public String getDescription() {
      return "A no-op firewall that is used on platforms that have their own managed firewall. (Like Windows)";
   }

   @Override
   public FirewallRule validateRule(FirewallRule rule) throws InvalidRuleException {
      switch (rule.getRuleType()) {
         case REDIRECT_RULE:
            RedirectRule oldRdr = (RedirectRule)rule;
            return new RedirectRule(oldRdr.getPublicServerPort(), oldRdr.getPublicServerPort(), oldRdr.getIpProtocol(), oldRdr.getAdapter());
         default:
            throw new InvalidRuleException("unrecognized rule");
      }
   }

   @Override
   public void processRules() {
   }
}
