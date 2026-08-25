package com.tridium.json.quick;

import com.tridium.json.JSONArray;
import com.tridium.json.JSONException;
import com.tridium.json.JSONObject;
import com.tridium.json.JSONWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.security.AccessController;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.function.ConsumerCanThrowException;

public class QuickJSONWriter extends JSONWriter {
   boolean comma = false;
   private static final Logger LOGGER = Logger.getLogger("niagara.quickJSONWriter");
   private static boolean DISABLED = AccessController.doPrivileged(() -> Boolean.getBoolean("niagara.quickJSONWriter.disable"));
   private static boolean lastLogged = DISABLED || LOGGER.isLoggable(Level.FINER);

   public static JSONWriter make(Appendable w) {
      return isEnabled() ? new QuickJSONWriter(w) : new JSONWriter(w);
   }

   private static boolean isEnabled() {
      if (!DISABLED && !LOGGER.isLoggable(Level.FINER)) {
         if (!lastLogged && LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("QuickJSONWriter is currently enabled");
            lastLogged = true;
         }

         return true;
      } else {
         if (lastLogged && LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("QuickJSONWriter is currently disabled");
            lastLogged = false;
         }

         return false;
      }
   }

   public static JSONWriter inMemory() {
      return stringWriter();
   }

   public static String toJSONString(ConsumerCanThrowException<JSONWriter, ? extends Exception> c) {
      return stringWriterFromConsumer(c).toString();
   }

   public static JSONObject toJSONObject(ConsumerCanThrowException<JSONWriter, ? extends Exception> c) {
      return new JSONObject(toJSONString(c));
   }

   public static JSONArray toJSONArray(ConsumerCanThrowException<JSONWriter, ? extends Exception> c) {
      return new JSONArray(toJSONString(c));
   }

   protected QuickJSONWriter(Appendable w) {
      super(w);
   }

   private QuickJSONWriter append(String s) {
      try {
         if (this.comma) {
            this.writer.append(',');
         }

         this.writer.append(s);
         this.comma = true;
         return this;
      } catch (IOException e) {
         throw new JSONException(e);
      }
   }

   public QuickJSONWriter array() {
      this.append("[");
      this.comma = false;
      return this;
   }

   public QuickJSONWriter endArray() {
      this.comma = false;
      this.append("]");
      this.comma = true;
      return this;
   }

   public QuickJSONWriter endObject() {
      this.comma = false;
      this.append("}");
      this.comma = true;
      return this;
   }

   public QuickJSONWriter key(String s) {
      if (s == null) {
         throw new JSONException("Null key.");
      }

      this.append(JSONObject.quote(s) + ':');
      this.comma = false;
      return this;
   }

   public QuickJSONWriter object() {
      this.append("{");
      this.comma = false;
      return this;
   }

   public QuickJSONWriter value(boolean b) {
      return this.append(b ? "true" : "false");
   }

   public QuickJSONWriter value(double d) {
      return this.value(Double.valueOf(d));
   }

   public QuickJSONWriter value(long l) {
      return this.append(Long.toString(l));
   }

   public QuickJSONWriter value(Object o) {
      return this.append(JSONObject.valueToString(o));
   }

   private static JSONWriter stringWriter() {
      return isEnabled() ? new QuickJSONWriter.QuickStringJSONWriter() : new QuickJSONWriter.RegularStringJSONWriter();
   }

   private static JSONWriter stringWriterFromConsumer(ConsumerCanThrowException<JSONWriter, ? extends Exception> c) {
      JSONWriter w = stringWriter();

      try {
         c.accept(w);
         return w;
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   private static class QuickStringJSONWriter extends QuickJSONWriter {
      private QuickStringJSONWriter() {
         super(new StringWriter());
      }

      @Override
      public String toString() {
         return this.writer.toString();
      }
   }

   private static class RegularStringJSONWriter extends JSONWriter {
      private RegularStringJSONWriter() {
         super(new StringWriter());
      }

      @Override
      public String toString() {
         return this.writer.toString();
      }
   }
}
