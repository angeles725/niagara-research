package com.tridium.template.api;

import java.util.Objects;

public class TemplateElement {
   private final NiagaraTemplate template;
   private final int propertyKey;
   private final int elementKey;
   private static final String NAME_SEPARATOR = ".";

   public NiagaraTemplate template() {
      return this.template;
   }

   public TemplateProperty property() {
      return this.template.getProperty(this.propertyKey);
   }

   public String getName() {
      return this.template.getElementName(this.propertyKey, this.elementKey);
   }

   public String getFullName() {
      String propertyName = this.template.getPropertyName(this.propertyKey);
      String elementName = this.template.getElementName(this.propertyKey, this.elementKey);
      return elementName == null ? propertyName : propertyName + "." + elementName;
   }

   public TemplateValueType getValueType() {
      return this.template.getValueType(this.propertyKey, this.elementKey, TemplateValueSource.NON_VALUE);
   }

   public boolean isValueNullable() {
      return this.template.isValueNullable(this.propertyKey, this.elementKey);
   }

   public TemplateValue presentValue() {
      return this.template.getElementValue(this.propertyKey, this.elementKey, TemplateValueSource.PRESENT_VALUE);
   }

   public TemplateValue localValue() {
      return this.template.getElementValue(this.propertyKey, this.elementKey, TemplateValueSource.LOCAL_VALUE);
   }

   public TemplateValue defaultValue() {
      return this.template.getElementValue(this.propertyKey, this.elementKey, TemplateValueSource.DEFAULT_VALUE);
   }

   TemplateElement(NiagaraTemplate template, int propertyKey, int elementKey) {
      this.template = Objects.requireNonNull(template);
      this.propertyKey = propertyKey;
      this.elementKey = elementKey;
   }
}
