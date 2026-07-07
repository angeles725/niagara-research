package com.tridium.template.api;

import com.tridium.template.api.impl.NiagaraTemplateUtils;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TemplateProperty {
   private final NiagaraTemplate template;
   private final int propertyKey;

   public NiagaraTemplate template() {
      return this.template;
   }

   public String getUserTip() {
      return NiagaraTemplateUtils.replaceNull(this.template.getPropertyUserTip(this.propertyKey));
   }

   public boolean hasLocalValue() {
      return this.template.hasLocalValue(this.propertyKey);
   }

   public List<TemplateElement> elements() {
      return this.template
         .getElementKeyStream(this.propertyKey)
         .boxed()
         .map(elementKey -> this.template.getElement(this.propertyKey, elementKey))
         .collect(Collectors.toList());
   }

   public TemplateElement getElement(String elementName) {
      return this.template.getElement(this.propertyKey, elementName);
   }

   public String getName() {
      return this.template.getPropertyName(this.propertyKey);
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof TemplateProperty)) {
         return false;
      } else {
         TemplateProperty other = (TemplateProperty)obj;
         return this.template == other.template && this.propertyKey == other.propertyKey;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.template, this.propertyKey);
   }

   TemplateProperty(NiagaraTemplate template, int propertyKey) {
      this.template = Objects.requireNonNull(template);
      this.propertyKey = propertyKey;
   }
}
