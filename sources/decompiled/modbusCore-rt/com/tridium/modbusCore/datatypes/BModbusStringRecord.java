package com.tridium.modbusCore.datatypes;

import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraTopic;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BBlob;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BFacets;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Topic;
import javax.baja.sys.Type;
import javax.baja.util.IFuture;
import javax.baja.util.Invocation;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "writeOnInputChange",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "padding",
      type = "boolean",
      defaultValue = "false",
      facets = {@Facet("BFacets.make(BFacets.TRUE_TEXT, BString.make(\"Pad with nulls\"), BFacets.FALSE_TEXT, BString.make(\"Pad with spaces\"))")}
   ), @NiagaraProperty(
      name = "input",
      type = "String",
      defaultValue = "",
      flags = 8,
      facets = {@Facet("BFacets.make(BFacets.MULTI_LINE, BBoolean.TRUE)")}
   ), @NiagaraProperty(
      name = "output",
      type = "String",
      defaultValue = "",
      flags = 267,
      facets = {@Facet("BFacets.make(BFacets.MULTI_LINE, BBoolean.TRUE)")}
   )})
@NiagaraAction(
   name = "write",
   flags = 24
)
@NiagaraTopic(
   name = "writeSuccessful",
   flags = 8
)
public abstract class BModbusStringRecord extends BModbusFileRecord {
   public static final Property writeOnInputChange = newProperty(0, false, null);
   public static final Property padding = newProperty(
      0, false, BFacets.make("trueText", BString.make("Pad with nulls"), "falseText", BString.make("Pad with spaces"))
   );
   public static final Property input = newProperty(8, "", BFacets.make("multiLine", BBoolean.TRUE));
   public static final Property output = newProperty(267, "", BFacets.make("multiLine", BBoolean.TRUE));
   public static final Action write = newAction(24, null);
   public static final Topic writeSuccessful = newTopic(8, null);
   public static final Type TYPE = Sys.loadType(BModbusStringRecord.class);

   public boolean getWriteOnInputChange() {
      return this.getBoolean(writeOnInputChange);
   }

   public void setWriteOnInputChange(boolean v) {
      this.setBoolean(writeOnInputChange, v, null);
   }

   public boolean getPadding() {
      return this.getBoolean(padding);
   }

   public void setPadding(boolean v) {
      this.setBoolean(padding, v, null);
   }

   public String getInput() {
      return this.getString(input);
   }

   public void setInput(String v) {
      this.setString(input, v, null);
   }

   public String getOutput() {
      return this.getString(output);
   }

   public void setOutput(String v) {
      this.setString(output, v, null);
   }

   public void write() {
      this.invoke(write, null, null);
   }

   public void fireWriteSuccessful(BValue event) {
      this.fire(writeSuccessful, event, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public void changed(Property property, Context context) {
      super.changed(property, context);
      if (this.isRunning()) {
         if (property.equals(input) && this.getWriteOnInputChange()) {
            this.write();
         }
      }
   }

   public IFuture post(Action action, BValue arg, Context cx) {
      return action.equals(write) ? this.getNetwork().postAsync(new Invocation(this, action, arg, cx)) : super.post(action, arg, cx);
   }

   public abstract void doWrite();

   protected void setOutputBytes(byte[] fileData) {
      if (fileData == null) {
         this.setData(BBlob.DEFAULT);
         this.setOutput("");
      } else {
         this.setData(BBlob.make(fileData));
         this.setOutput(new String(fileData).trim());
      }
   }

   protected byte[] getInputBytes(int recLength, String padding) {
      return getStringBytes(this.getInput(), recLength, padding);
   }

   protected static byte[] getStringBytes(String str, int recLength, String padding) {
      byte[] result = new byte[recLength * 2];
      int inputLength = 0;
      if (str != null) {
         byte[] inputBytes = str.getBytes();
         inputLength = inputBytes.length;
         if (inputLength >= result.length) {
            System.arraycopy(inputBytes, 0, result, 0, result.length);
            return result;
         }

         System.arraycopy(inputBytes, 0, result, 0, inputLength);
      }

      byte[] padBytes = null;
      if (padding == null) {
         padBytes = new byte[]{0};
      } else {
         padBytes = padding.getBytes();
      }

      if (padBytes != null && padBytes.length > 0) {
         int index = inputLength;

         while (index < result.length) {
            for (int i = 0; i < padBytes.length; i++) {
               result[index] = padBytes[i];
               if (++index >= result.length) {
                  break;
               }
            }
         }
      }

      return result;
   }

   protected static byte[] padBytes(byte[] inBytes, int recLength, String padding) {
      byte[] result = new byte[recLength * 2];
      int inputLength = 0;
      if (inBytes != null) {
         inputLength = inBytes.length;
         if (inputLength >= result.length) {
            System.arraycopy(inBytes, 0, result, 0, result.length);
            return result;
         }

         System.arraycopy(inBytes, 0, result, 0, inputLength);
      }

      byte[] padBytes = null;
      if (padding == null) {
         padBytes = new byte[]{0};
      } else {
         padBytes = padding.getBytes();
      }

      if (padBytes != null && padBytes.length > 0) {
         int index = inputLength;

         while (index < result.length) {
            for (int i = 0; i < padBytes.length; i++) {
               result[index] = padBytes[i];
               if (++index >= result.length) {
                  break;
               }
            }
         }
      }

      return result;
   }
}
