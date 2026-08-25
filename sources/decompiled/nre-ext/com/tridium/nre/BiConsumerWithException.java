package com.tridium.nre;

import javax.baja.nre.function.BiConsumerCanThrowException;

@FunctionalInterface
@Deprecated
public interface BiConsumerWithException<T, U, E extends Exception> extends BiConsumerCanThrowException<T, U, E> {
}
