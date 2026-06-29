package com.tridium.fox.sys;

import javax.baja.sys.BajaRuntimeException;

public class MismatchedStationNamesException extends BajaRuntimeException {
   public MismatchedStationNamesException(String expected, String actual) {
      super(expected + " != " + actual);
   }

   public String toString() {
      return "Mismatched station names: " + this.getMessage();
   }
}
