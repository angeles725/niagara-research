package com.tridium.nre.di;

public class SingletonSupplier<T> extends BaseSupplier<T> {
   private T instance = (T)null;
   private int recursive = 0;

   public SingletonSupplier(Class<T> type, Class<? extends T> clazz) {
      super(type, clazz);
   }

   public SingletonSupplier(Class<T> type, Class<? extends T> clazz, IConfiguration config) {
      super(type, clazz, config);
   }

   @Override
   public T get(NreInstantiator instantiator) throws NreInstantiationException {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         NreInstancePermission instancePermission = new NreInstancePermission(this.type);
         sm.checkPermission(instancePermission);
      }

      synchronized (this) {
         if (this.instance == null) {
            try {
               if (this.recursive > 0) {
                  throw new NreInstantiationException("recursive object creation is not supported");
               }

               this.recursive++;
               T tinstance = this.createNewInstance(instantiator);
               this.evaluateConfiguration(tinstance);
               this.instance = tinstance;
            } catch (NreInstantiationException nie) {
               throw nie;
            } catch (Exception e) {
               throw new NreInstantiationException("unable to create instance", e);
            } finally {
               this.recursive--;
            }
         }

         return this.instance;
      }
   }
}
