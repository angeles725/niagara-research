package com.tridium.nre;

import javax.baja.nre.function.ConsumerCanThrowException;

@FunctionalInterface
@Deprecated
public interface ConsumerWithException<T, E extends Exception> extends ConsumerCanThrowException<T, E> {
}
