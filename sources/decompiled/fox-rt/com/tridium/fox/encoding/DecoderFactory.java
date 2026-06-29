package com.tridium.fox.encoding;

import com.tridium.fox.message.FoxMessage;
import com.tridium.fox.message.FoxObject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.baja.sys.Context;

public class DecoderFactory {
   private static final Map<String, DecoderFactory.Provider> providers = new HashMap<>();

   public static Object decode(FoxMessage msg, String key, Object arg) throws Exception {
      return decode(msg, key, arg, null);
   }

   public static Object decode(FoxMessage msg, String key, Object arg, Context cx) throws Exception {
      FoxObject fo = (FoxObject)msg.get(key);
      return decode(fo.encoding, fo.data, arg, cx);
   }

   public static Object decode(FoxObject fo, Object arg) throws Exception {
      return decode(fo.encoding, fo.data, arg);
   }

   public static Object decode(String name, byte[] buf, Object arg) throws Exception {
      return decode(name, buf, arg, null);
   }

   public static Object decode(String name, byte[] buf, Object arg, Context cx) throws Exception {
      DecoderFactory.Provider provider = providers.get(name);
      if (provider != null) {
         return provider.decode(buf, arg, cx);
      } else {
         throw new IOException("Unknown object encoding: " + name);
      }
   }

   public static void register(String name, DecoderFactory.Provider provider) {
      providers.put(name, provider);
   }

   public static void unregister(String name) {
      providers.remove(name);
   }

   static {
      register("bog", new BogCodec.Provider());
   }

   public interface Provider {
      default Object decode(byte[] buf, Object arg) throws Exception {
         return this.decode(buf, arg, null);
      }

      Object decode(byte[] var1, Object var2, Context var3) throws Exception;
   }
}
