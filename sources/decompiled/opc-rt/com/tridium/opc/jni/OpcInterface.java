package com.tridium.opc.jni;

import java.lang.reflect.InvocationTargetException;

public class OpcInterface {
   String iid;
   Class<?> javaImpl;

   public OpcInterface(String iid, Class<?> javaImpl) {
      this.iid = iid;
      this.javaImpl = javaImpl;
   }

   public ComObjectClient newInstance() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
      return (ComObjectClient)this.javaImpl.getDeclaredConstructor().newInstance();
   }

   @Override
   public String toString() {
      return this.iid + this.javaImpl.getName();
   }
}
