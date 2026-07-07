package com.tridium.template.api.impl;

import com.tridium.template.api.TemplateValueSource;
import com.tridium.template.api.TemplateValueType;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;
import javax.baja.security.BPassword;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BInteger;
import javax.baja.sys.BLong;
import javax.baja.sys.BNumber;
import javax.baja.sys.BSimple;
import javax.baja.sys.BString;
import javax.baja.sys.BStruct;
import javax.baja.sys.BValue;

public abstract class TemplateSourceWithValue extends TemplateSource {
   protected BValue getPropertyValue(int propertyKey) {
      throw new UnsupportedOperationException("TemplateSourceWithValue.getPropertyValue not yet implemented!");
   }

   protected abstract TemplateValueSource getValueSource();

   @Override
   public IntStream getElementKeyStream(int propertyKey) {
      return IntStream.range(0, getElementCountForPropertyValue(this.getPropertyValue(propertyKey)));
   }

   @Override
   public String getElementName(int propertyKey, int elementKey) {
      BValue propertyValue = this.getPropertyValue(propertyKey);
      return isPropertyPrimitive(propertyValue) ? null : ((BStruct)propertyValue).getPropertiesArray()[elementKey].getName();
   }

   @Override
   public TemplateValueType getValueType(int propertyKey, int elementKey) {
      BValue elementValue = this.getPropertyElementValue(propertyKey, elementKey);
      if (elementValue instanceof BInteger || elementValue instanceof BLong) {
         return TemplateValueType.INTEGER;
      } else if (elementValue instanceof BNumber) {
         return TemplateValueType.NUMERIC;
      } else if (elementValue instanceof BBoolean) {
         return TemplateValueType.BOOLEAN;
      } else if (elementValue instanceof BEnum) {
         return TemplateValueType.ENUM;
      } else {
         return elementValue instanceof BPassword ? TemplateValueType.PASSWORD : TemplateValueType.STRING;
      }
   }

   @Override
   public boolean isValueNullable(int propertyKey, int elementKey) {
      BValue elementValue = this.getPropertyElementValueWithStatus(propertyKey, elementKey);
      return elementValue instanceof BStatusValue;
   }

   @Override
   public String getValueNType(int propertyKey, int elementKey) {
      return this.getPropertyElementValueWithStatus(propertyKey, elementKey).getType().toString();
   }

   @Override
   public boolean hasLocalValue(int propertyKey) {
      return this.getValueSource() == TemplateValueSource.LOCAL_VALUE;
   }

   @Override
   public boolean isValueMissing(int propertyKey, TemplateValueSource valueSource) {
      return this.clarifyValueSource(valueSource) != this.getValueSource();
   }

   @Override
   public boolean isValueNull(int propertyKey, int elementKey, TemplateValueSource valueSource) {
      this.checkValueSource(valueSource);
      BValue elementValue = this.getPropertyElementValueWithStatus(propertyKey, elementKey);
      return elementValue instanceof BStatusValue ? ((BStatusValue)elementValue).getStatus().isNull() : false;
   }

   @Override
   public double getNumericValue(int propertyKey, int elementKey, TemplateValueSource valueSource) throws NumberFormatException {
      this.checkValueSource(valueSource);
      BValue elementValue = this.getPropertyElementValue(propertyKey, elementKey);
      if (elementValue instanceof BNumber) {
         return ((BNumber)elementValue).getDouble();
      } else if (elementValue instanceof BEnum) {
         return ((BEnum)elementValue).getOrdinal();
      } else if (elementValue instanceof BString) {
         return Double.parseDouble(elementValue.toString(null));
      } else {
         throw new IllegalStateException("not convertible to number " + elementValue.toString(null));
      }
   }

   @Override
   public long getIntegerValue(int propertyKey, int elementKey, TemplateValueSource valueSource) throws NumberFormatException {
      this.checkValueSource(valueSource);
      BValue elementValue = this.getPropertyElementValue(propertyKey, elementKey);
      if (elementValue instanceof BNumber) {
         return ((BNumber)elementValue).getLong();
      } else if (elementValue instanceof BEnum) {
         return ((BEnum)elementValue).getOrdinal();
      } else if (elementValue instanceof BString) {
         return Long.parseLong(elementValue.toString(null));
      } else {
         throw new IllegalStateException("not convertible to integer " + elementValue.toString(null));
      }
   }

   @Override
   public boolean getBooleanValue(int propertyKey, int elementKey, TemplateValueSource valueSource) throws IllegalArgumentException {
      this.checkValueSource(valueSource);
      BValue elementValue = this.getPropertyElementValue(propertyKey, elementKey);
      if (elementValue instanceof BBoolean) {
         return ((BBoolean)elementValue).getBoolean();
      } else if (elementValue instanceof BString) {
         return BBoolean.decode(elementValue.toString(null));
      } else {
         throw new IllegalStateException("not convertible to boolean " + elementValue.toString(null));
      }
   }

   @Override
   public String getStringValue(int propertyKey, int elementKey, TemplateValueSource valueSource) {
      this.checkValueSource(valueSource);
      BValue elementValue = this.getPropertyElementValue(propertyKey, elementKey);
      String result;
      if (elementValue instanceof BEnum) {
         result = ((BEnum)elementValue).getTag();
      } else {
         if (elementValue instanceof BPassword) {
            throw new IllegalStateException("can't read password value");
         }

         if (elementValue instanceof BSimple) {
            try {
               result = ((BSimple)elementValue).encodeToString();
            } catch (IOException var7) {
               result = elementValue.toString(null);
            }
         } else {
            result = elementValue.toString(null);
         }
      }

      return result;
   }

   @Override
   public Map<Integer, String> getDefinedEnumValues(int propertyKey, int elementKey, TemplateValueSource valueSource) {
      this.checkValueSource(valueSource);
      BValue elementValue = this.getPropertyElementValue(propertyKey, elementKey);
      if (!(elementValue instanceof BEnum)) {
         throw new IllegalStateException("enum expected " + elementValue.toString(null));
      } else {
         BEnum enumValue = (BEnum)elementValue;
         BEnumRange enumRange = enumValue.getRange();
         if (enumRange.isNull()) {
            return Collections.emptyMap();
         } else {
            Map<Integer, String> values = new TreeMap<>();

            for (int ordinal : enumRange.getOrdinals()) {
               values.put(ordinal, enumRange.getTag(ordinal));
            }

            return Collections.unmodifiableMap(values);
         }
      }
   }

   private BValue getPropertyElementValue(int propertyKey, int elementKey) {
      return getPropertyElementValue(this.getPropertyValue(propertyKey), elementKey);
   }

   private BValue getPropertyElementValueWithStatus(int propertyKey, int elementKey) {
      return getPropertyElementValueWithStatus(this.getPropertyValue(propertyKey), elementKey);
   }

   private TemplateValueSource clarifyValueSource(TemplateValueSource valueSource) {
      return valueSource != TemplateValueSource.PRESENT_VALUE && valueSource != TemplateValueSource.NON_VALUE ? valueSource : this.getValueSource();
   }

   private void checkValueSource(TemplateValueSource valueSource) {
      if (this.clarifyValueSource(valueSource) != this.getValueSource()) {
         throw new IllegalStateException("missing " + valueSource.toString());
      }
   }

   private static BValue getPropertyElementValue(BValue propertyValue, int elementKey) {
      BValue elementValue = getPropertyElementValueWithStatus(propertyValue, elementKey);
      return elementValue instanceof BStatusValue ? ((BStatusValue)elementValue).getValueValue() : elementValue;
   }

   private static BValue getPropertyElementValueWithStatus(BValue propertyValue, int elementKey) {
      if (isPropertyPrimitive(propertyValue)) {
         return propertyValue;
      } else {
         BStruct structValue = (BStruct)propertyValue;
         return structValue.get(structValue.getPropertiesArray()[elementKey]);
      }
   }

   private static int getElementCountForPropertyValue(BValue propertyValue) {
      return isPropertyPrimitive(propertyValue) ? 1 : ((BStruct)propertyValue).getPropertyCount();
   }

   private static boolean isPropertyPrimitive(BValue propertyValue) {
      return propertyValue instanceof BStatusValue ? true : !(propertyValue instanceof BStruct);
   }
}
