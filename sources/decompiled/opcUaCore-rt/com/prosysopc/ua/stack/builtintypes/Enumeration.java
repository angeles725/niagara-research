package com.prosysopc.ua.stack.builtintypes;

import com.prosysopc.ua.typedictionary.EnumerationSpecification;

public interface Enumeration {
   int getValue();

   EnumerationSpecification specification();

   Enumeration.Builder toBuilder();

   public interface Builder {
      Enumeration build();

      Enumeration.Builder setValue(int var1);
   }
}
