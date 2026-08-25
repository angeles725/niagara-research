package com.tridium.nre.security;

import java.util.function.Supplier;

public interface ISecretBytesSupplier extends Supplier<SecretBytes>, AutoCloseable {
   @Override
   void close();

   ISecretBytesSupplier newCopy();

   static ISecretBytesSupplier wrap(final SecretBytes key) {
      return new ISecretBytesSupplier() {
         @Override
         public void close() {
            key.close();
         }

         public SecretBytes get() {
            return key;
         }

         @Override
         public ISecretBytesSupplier newCopy() {
            return ISecretBytesSupplier.wrap(key.newCopy());
         }
      };
   }
}
