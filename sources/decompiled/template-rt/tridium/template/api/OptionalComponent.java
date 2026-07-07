package com.tridium.template.api;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class OptionalComponent {
   private final NiagaraTemplate template;
   private final int componentKey;

   public NiagaraTemplate template() {
      return this.template;
   }

   public String getPath() {
      return this.template.getOptionalComponentPath(this.componentKey);
   }

   public String getNType() {
      return this.template.getOptionalComponentNType(this.componentKey);
   }

   public List<TemplateProperty> properties() {
      return this.template.getOptionalComponentPropertyKeyStream(this.componentKey).mapToObj(this.template::getProperty).collect(Collectors.toList());
   }

   public List<TemplateElement> propertyElements() {
      return this.template
         .getOptionalComponentPropertyElementKeyStream(this.componentKey)
         .map(pek -> this.template.getElement((Integer)pek.getFirst(), (Integer)pek.getSecond()))
         .collect(Collectors.toList());
   }

   public boolean isInstalled() {
      return this.template.isOptionalInstalled(this.componentKey);
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof OptionalComponent)) {
         return false;
      } else {
         OptionalComponent other = (OptionalComponent)obj;
         return this.template == other.template && this.componentKey == other.componentKey;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.template, this.componentKey);
   }

   OptionalComponent(NiagaraTemplate template, int componentKey) {
      this.template = Objects.requireNonNull(template);
      this.componentKey = componentKey;
   }
}
