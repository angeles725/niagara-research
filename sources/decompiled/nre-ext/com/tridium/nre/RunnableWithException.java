package com.tridium.nre;

import javax.baja.nre.function.RunnableCanThrowException;

@FunctionalInterface
@Deprecated
public interface RunnableWithException<E extends Exception> extends RunnableCanThrowException<E> {
}
