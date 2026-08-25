package com.tridium.nre.util;

public interface IElement {
   String name();

   String get(String var1);

   String get(String var1, String var2);

   int geti(String var1);

   int geti(String var1, int var2);

   double getd(String var1);

   double getd(String var1, double var2);

   float getf(String var1);

   float getf(String var1, float var2);

   long getl(String var1);

   long getl(String var1, long var2);

   int attrSize();

   String attrName(int var1);

   String attrValue(int var1);

   IElement copy();
}
