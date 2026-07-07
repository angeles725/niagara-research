package com.tridium.template.api;

import com.tridium.template.api.impl.NiagaraTemplateUtils;
import java.util.Map;
import java.util.Objects;

public class TemplateValue {
   private final NiagaraTemplate template;
   private final int propertyKey;
   private final int elementKey;
   private final TemplateValueSource valueSource;

   public NiagaraTemplate getTemplate() {
      return this.template;
   }

   public TemplateProperty getProperty() {
      return this.template.getProperty(this.propertyKey);
   }

   public TemplateElement getElement() {
      return this.template.getElement(this.propertyKey, this.elementKey);
   }

   public TemplateValueSource getValueSource() {
      return this.valueSource;
   }

   public boolean isDefaultValue() {
      return this.valueSource == TemplateValueSource.DEFAULT_VALUE;
   }

   public boolean isMissing() {
      return this.template.isValueMissing(this.propertyKey, this.elementKey, this.valueSource);
   }

   public TemplateValueType getType() {
      return NiagaraTemplateUtils.replaceNull(TemplateValueType.class, this.template.getValueType(this.propertyKey, this.elementKey, this.valueSource));
   }

   public String getNType() {
      return NiagaraTemplateUtils.replaceNull(this.template.getValueNType(this.propertyKey, this.elementKey, this.valueSource));
   }

   public boolean isNull() {
      return this.template.isValueNull(this.propertyKey, this.elementKey, this.valueSource);
   }

   public double getNumericValue() {
      return this.template.getNumericValue(this.propertyKey, this.elementKey, this.valueSource);
   }

   public long getIntegerValue() {
      return this.template.getIntegerValue(this.propertyKey, this.elementKey, this.valueSource);
   }

   public boolean getBooleanValue() {
      return this.template.getBooleanValue(this.propertyKey, this.elementKey, this.valueSource);
   }

   public String getStringValue() {
      return NiagaraTemplateUtils.replaceNull(this.template.getStringValue(this.propertyKey, this.elementKey, this.valueSource));
   }

   public Map<Integer, String> getDefinedEnumValues() {
      return this.template.getDefinedEnumValues(this.propertyKey, this.elementKey, this.valueSource);
   }

   public boolean isEqualValue(TemplateValue other) {
      if (this.getType() != other.getType()) {
         return false;
      } else {
         switch (this.getType()) {
            case STRING:
               return Objects.equals(this.getStringValue(), other.getStringValue());
            case BOOLEAN:
               return this.getBooleanValue() == other.getBooleanValue();
            case INTEGER:
            case ENUM:
               return this.getIntegerValue() == other.getIntegerValue();
            case NUMERIC:
               return this.getNumericValue() == other.getNumericValue();
            default:
               return false;
         }
      }
   }

   TemplateValue(NiagaraTemplate template, int propertyKey, int elementKey, TemplateValueSource valueSource) {
      this.template = Objects.requireNonNull(template);
      this.propertyKey = propertyKey;
      this.elementKey = elementKey;
      this.valueSource = valueSource;
   }
}
