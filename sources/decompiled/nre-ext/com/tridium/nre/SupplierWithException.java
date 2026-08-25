package com.tridium.nre;

import javax.baja.nre.function.SupplierCanThrowException;

@FunctionalInterface
@Deprecated
public interface SupplierWithException<T, E extends Exception> extends SupplierCanThrowException<T, E> {
}
