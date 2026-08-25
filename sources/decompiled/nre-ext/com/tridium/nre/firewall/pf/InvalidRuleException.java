package com.tridium.nre.firewall.pf;

public class InvalidRuleException extends Exception {
   public InvalidRuleException(String message) {
      super(message);
   }
}
