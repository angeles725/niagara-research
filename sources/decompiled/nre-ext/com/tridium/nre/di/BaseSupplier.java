package com.tridium.nre.di;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class BaseSupplier<T> implements TypeSupplier<T> {
   protected Class<T> type;
   protected Class<? extends T> clazz;
   protected Object config;

   public BaseSupplier(Class<T> type, Class<? extends T> clazz) {
      this.type = type;
      this.clazz = clazz;
      this.config = null;
   }

   public BaseSupplier(Class<T> type, Class<? extends T> clazz, Object config) {
      this.type = type;
      this.clazz = clazz;
      this.config = config;
   }

   protected T createNewInstance(NreInstantiator instantiator) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
      if (this.clazz.isAnnotationPresent(RequiresInstantiator.class)) {
         Constructor<? extends T> constructor = this.clazz.getDeclaredConstructor(NreInstantiator.class);
         constructor.setAccessible(true);
         return (T)constructor.newInstance(instantiator);
      } else {
         Constructor<? extends T> constructor = this.clazz.getDeclaredConstructor();
         constructor.setAccessible(true);
         return (T)constructor.newInstance();
      }
   }

   protected void evaluateConfiguration(T instance) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
      if (this.clazz.isAnnotationPresent(RequiresConfiguration.class)) {
         RequiresConfiguration configAnnotation = this.clazz.getAnnotation(RequiresConfiguration.class);
         Method method = this.clazz.getDeclaredMethod(configAnnotation.method(), configAnnotation.argument());
         method.setAccessible(true);
         if (this.config == null) {
            throw new IllegalArgumentException("configuration can't be null");
         }

         if (!configAnnotation.argument().isAssignableFrom(this.config.getClass())) {
            throw new IllegalArgumentException("configuration doesn't match the required type");
         }

         method.invoke(instance, this.config);
      }
   }

   @Override
   public Class<T> getType() {
      return this.type;
   }
}
