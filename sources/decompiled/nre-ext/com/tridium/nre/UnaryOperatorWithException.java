package com.tridium.nre;

import javax.baja.nre.function.UnaryOperatorCanThrowException;

@FunctionalInterface
@Deprecated
public interface UnaryOperatorWithException<T, E extends Throwable> extends UnaryOperatorCanThrowException<T, E> {
   static <T> UnaryOperatorWithException<T, RuntimeException> identity() {
      return o -> o;
   }
}
