package com.tridium.json.pretty;

import com.tridium.json.JSONException;
import com.tridium.json.JSONObject;
import com.tridium.json.JSONStringer;
import com.tridium.json.JSONWriter;
import java.io.IOException;

public class PrettyJSONStringer extends JSONStringer {
   private static final int MAX_DEPTH = 200;
   private final int indentFactor;
   private boolean comma;
   private final JSONObject[] stack;
   private int top;

   public PrettyJSONStringer(int indentFactor) {
      this.indentFactor = indentFactor;
      this.comma = false;
      this.stack = new JSONObject[200];
      this.top = 0;
   }

   private JSONWriter append(String string) throws JSONException {
      if (string == null) {
         throw new JSONException("Null pointer");
      }

      if (this.mode != 'i' && this.mode != 'o' && this.mode != 'a') {
         throw new JSONException("Value out of sequence.");
      }

      try {
         if (this.mode == 'a') {
            if (this.comma) {
               this.writer.append(',');
            }

            this.writer.append('\n');
            this.indent();
         }

         this.writer.append(string);
      } catch (IOException e) {
         throw new JSONException(e);
      }

      if (this.mode == 'o') {
         this.mode = 'k';
      }

      this.comma = true;
      return this;
   }

   private void indent() throws IOException {
      int indent = this.top * this.indentFactor;

      for (int i = 0; i < indent; i++) {
         this.writer.append(' ');
      }
   }

   @Override
   public JSONWriter array() throws JSONException {
      if (this.mode != 'i' && this.mode != 'o' && this.mode != 'a') {
         throw new JSONException("Misplaced array.");
      }

      this.append("[");
      this.push(null);
      this.comma = false;
      return this;
   }

   private JSONWriter end(char m, char c) throws JSONException {
      if (this.mode != m) {
         throw new JSONException(m == 'a' ? "Misplaced endArray." : "Misplaced endObject.");
      }

      this.pop(m);

      try {
         if (this.comma) {
            this.writer.append('\n');
            this.indent();
         }

         this.writer.append(c);
      } catch (IOException e) {
         throw new JSONException(e);
      }

      this.comma = true;
      return this;
   }

   @Override
   public JSONWriter endArray() throws JSONException {
      return this.end('a', ']');
   }

   @Override
   public JSONWriter endObject() throws JSONException {
      return this.end('k', '}');
   }

   @Override
   public JSONWriter key(String string) throws JSONException {
      if (string == null) {
         throw new JSONException("Null key.");
      }

      if (this.mode == 'k') {
         try {
            JSONObject topObject = this.stack[this.top - 1];
            if (topObject.has(string)) {
               throw new JSONException("Duplicate key \"" + string + '"');
            }

            topObject.put(string, true);
            if (this.comma) {
               this.writer.append(',');
            }

            this.writer.append('\n');
            this.indent();
            this.writer.append(JSONObject.quote(string));
            this.writer.append(':');
            this.writer.append(' ');
            this.comma = false;
            this.mode = 'o';
            return this;
         } catch (IOException e) {
            throw new JSONException(e);
         }
      } else {
         throw new JSONException("Misplaced key.");
      }
   }

   @Override
   public JSONWriter object() throws JSONException {
      if (this.mode == 'i') {
         this.mode = 'o';
      }

      if (this.mode != 'o' && this.mode != 'a') {
         throw new JSONException("Misplaced object.");
      }

      this.append("{");
      this.push(new JSONObject());
      this.comma = false;
      return this;
   }

   private void pop(char c) throws JSONException {
      if (this.top <= 0) {
         throw new JSONException("Nesting error.");
      }

      char m = (char)(this.stack[this.top - 1] == null ? 97 : 107);
      if (m != c) {
         throw new JSONException("Nesting error.");
      }

      this.top--;
      this.mode = (char)(this.top == 0 ? 100 : (this.stack[this.top - 1] == null ? 97 : 107));
   }

   private void push(JSONObject jo) throws JSONException {
      if (this.top >= 200) {
         throw new JSONException("Nesting too deep.");
      }

      this.stack[this.top] = jo;
      this.mode = (char)(jo == null ? 97 : 107);
      this.top++;
   }

   @Override
   public JSONWriter value(boolean b) throws JSONException {
      return this.append(b ? "true" : "false");
   }

   @Override
   public JSONWriter value(double d) throws JSONException {
      return this.value(Double.valueOf(d));
   }

   @Override
   public JSONWriter value(long l) throws JSONException {
      return this.append(Long.toString(l));
   }

   @Override
   public JSONWriter value(Object object) throws JSONException {
      return this.append(valueToString(object));
   }
}
