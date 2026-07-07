package com.tridium.template.api.impl;

import com.tridium.nre.util.tuple.Pair;
import com.tridium.template.api.TemplateScope;
import com.tridium.template.api.TemplateSourceType;
import com.tridium.template.api.TemplateType;
import com.tridium.template.api.TemplateValueSource;
import com.tridium.template.api.TemplateValueType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.baja.sys.BValue;

public class CombinedTemplateSource extends TemplateSource {
   private final TemplateSourceWithValue primary;
   private final TemplateSourceWithValue secondary;
   private List<Pair<Integer, Integer>> propertyKeys = null;

   public CombinedTemplateSource(TemplateSourceWithValue primarySource, TemplateSourceWithValue secondarySource) {
      assert primarySource.getValueSource() == TemplateValueSource.LOCAL_VALUE;

      assert secondarySource.getValueSource() == TemplateValueSource.DEFAULT_VALUE;

      this.primary = primarySource;
      this.secondary = secondarySource;
   }

   @Override
   public void close() {
      this.primary.close();
      this.secondary.close();
      super.close();
   }

   @Override
   public TemplateSourceType getSourceType() {
      return this.primary.getSourceType();
   }

   @Override
   public TemplateType getTemplateType() {
      return this.primary.getTemplateType();
   }

   @Override
   public TemplateScope getTemplateScope() {
      return this.primary.getTemplateScope();
   }

   @Override
   public String getTitle() {
      return this.primary.getTitle();
   }

   @Override
   public String getVendor() {
      return this.primary.getVendor();
   }

   @Override
   public String getVersion() {
      return this.primary.getVersion();
   }

   @Override
   public String getUid() {
      return this.primary.getUid();
   }

   @Override
   public String getDescription() {
      return this.primary.getDescription();
   }

   @Override
   public String getInfo() {
      return this.primary.getInfo();
   }

   @Override
   public String getBaseName() {
      return this.primary.getBaseName();
   }

   @Override
   public IntStream getOptionalComponentKeyStream() {
      return this.secondary.getOptionalComponentKeyStream();
   }

   @Override
   public String getOptionalComponentPath(int optionalComponentKey) {
      return this.secondary.getOptionalComponentPath(optionalComponentKey);
   }

   @Override
   public String getOptionalComponentNType(int optionalComponentKey) {
      return this.secondary.getOptionalComponentNType(optionalComponentKey);
   }

   @Override
   public boolean isOptionalInstalled(int optionalComponentKey) {
      return this.primary.hasComponent(this.getOptionalComponentPath(optionalComponentKey));
   }

   @Override
   public boolean hasComponent(String componentPath) {
      return this.primary.hasComponent(componentPath);
   }

   @Override
   public IntStream getPropertyKeyStream() {
      return IntStream.range(0, this.getPropertyCount());
   }

   @Override
   public String getPropertyPath(int propertyKey) {
      return this.hasPrimaryPropertyKey(propertyKey)
         ? this.primary.getPropertyPath(this.getPrimaryPropertyKey(propertyKey))
         : this.secondary.getPropertyPath(this.getSecondaryPropertyKey(propertyKey));
   }

   @Override
   public String getPropertyName(int propertyKey) {
      return this.hasPrimaryPropertyKey(propertyKey)
         ? this.primary.getPropertyName(this.getPrimaryPropertyKey(propertyKey))
         : this.secondary.getPropertyName(this.getSecondaryPropertyKey(propertyKey));
   }

   @Override
   public String getPropertyUserTip(int propertyKey) {
      return this.hasPrimaryPropertyKey(propertyKey)
         ? this.primary.getPropertyUserTip(this.getPrimaryPropertyKey(propertyKey))
         : this.secondary.getPropertyUserTip(this.getSecondaryPropertyKey(propertyKey));
   }

   @Override
   public IntStream getElementKeyStream(int propertyKey) {
      return this.hasPrimaryPropertyKey(propertyKey)
         ? this.primary.getElementKeyStream(this.getPrimaryPropertyKey(propertyKey))
         : this.secondary.getElementKeyStream(this.getSecondaryPropertyKey(propertyKey));
   }

   @Override
   public String getElementName(int propertyKey, int elementKey) {
      return this.hasPrimaryPropertyKey(propertyKey)
         ? this.primary.getElementName(this.getPrimaryPropertyKey(propertyKey), elementKey)
         : this.secondary.getElementName(this.getSecondaryPropertyKey(propertyKey), elementKey);
   }

   @Override
   public TemplateValueType getValueType(int propertyKey, int elementKey) {
      return this.hasPrimaryPropertyKey(propertyKey)
         ? this.primary.getValueType(this.getPrimaryPropertyKey(propertyKey), elementKey)
         : this.secondary.getValueType(this.getSecondaryPropertyKey(propertyKey), elementKey);
   }

   @Override
   public boolean isValueNullable(int propertyKey, int elementKey) {
      return this.hasPrimaryPropertyKey(propertyKey)
         ? this.primary.isValueNullable(this.getPrimaryPropertyKey(propertyKey), elementKey)
         : this.secondary.isValueNullable(this.getSecondaryPropertyKey(propertyKey), elementKey);
   }

   @Override
   public String getValueNType(int propertyKey, int elementKey) {
      return this.hasPrimaryPropertyKey(propertyKey)
         ? this.primary.getValueNType(this.getPrimaryPropertyKey(propertyKey), elementKey)
         : this.secondary.getValueNType(this.getSecondaryPropertyKey(propertyKey), elementKey);
   }

   @Override
   public boolean hasLocalValue(int propertyKey) {
      return this.whichSource(propertyKey, TemplateValueSource.PRESENT_VALUE) == CombinedTemplateSource.PropertySource.PRIMARY;
   }

   @Override
   public boolean isValueMissing(int propertyKey, TemplateValueSource valueSource) {
      switch (this.whichSource(propertyKey, valueSource)) {
         case DONT_CARE:
            return this.primary.isValueMissing(this.getPrimaryPropertyKey(propertyKey), valueSource)
               && this.secondary.isValueMissing(this.getSecondaryPropertyKey(propertyKey), valueSource);
         case PRIMARY:
            return this.primary.isValueMissing(this.getPrimaryPropertyKey(propertyKey), valueSource);
         case SECONDARY:
            return this.secondary.isValueMissing(this.getSecondaryPropertyKey(propertyKey), valueSource);
         default:
            return true;
      }
   }

   @Override
   public boolean isValueNull(int propertyKey, int elementKey, TemplateValueSource valueSource) {
      switch (this.whichSource(propertyKey, valueSource)) {
         case PRIMARY:
            return this.primary.isValueNull(this.getPrimaryPropertyKey(propertyKey), elementKey, valueSource);
         case SECONDARY:
            return this.secondary.isValueNull(this.getSecondaryPropertyKey(propertyKey), elementKey, valueSource);
         default:
            throw new IllegalStateException("missing " + valueSource);
      }
   }

   @Override
   public double getNumericValue(int propertyKey, int elementKey, TemplateValueSource valueSource) {
      switch (this.whichSource(propertyKey, valueSource)) {
         case PRIMARY:
            return this.primary.getNumericValue(this.getPrimaryPropertyKey(propertyKey), elementKey, valueSource);
         case SECONDARY:
            return this.secondary.getNumericValue(this.getSecondaryPropertyKey(propertyKey), elementKey, valueSource);
         default:
            throw new IllegalStateException("missing " + valueSource);
      }
   }

   @Override
   public long getIntegerValue(int propertyKey, int elementKey, TemplateValueSource valueSource) {
      switch (this.whichSource(propertyKey, valueSource)) {
         case PRIMARY:
            return this.primary.getIntegerValue(this.getPrimaryPropertyKey(propertyKey), elementKey, valueSource);
         case SECONDARY:
            return this.secondary.getIntegerValue(this.getSecondaryPropertyKey(propertyKey), elementKey, valueSource);
         default:
            throw new IllegalStateException("missing " + valueSource);
      }
   }

   @Override
   public boolean getBooleanValue(int propertyKey, int elementKey, TemplateValueSource valueSource) {
      switch (this.whichSource(propertyKey, valueSource)) {
         case PRIMARY:
            return this.primary.getBooleanValue(this.getPrimaryPropertyKey(propertyKey), elementKey, valueSource);
         case SECONDARY:
            return this.secondary.getBooleanValue(this.getSecondaryPropertyKey(propertyKey), elementKey, valueSource);
         default:
            throw new IllegalStateException("missing " + valueSource);
      }
   }

   @Override
   public String getStringValue(int propertyKey, int elementKey, TemplateValueSource valueSource) {
      switch (this.whichSource(propertyKey, valueSource)) {
         case PRIMARY:
            return this.primary.getStringValue(this.getPrimaryPropertyKey(propertyKey), elementKey, valueSource);
         case SECONDARY:
            return this.secondary.getStringValue(this.getSecondaryPropertyKey(propertyKey), elementKey, valueSource);
         default:
            throw new IllegalStateException("missing " + valueSource);
      }
   }

   @Override
   public Map<Integer, String> getDefinedEnumValues(int propertyKey, int elementKey, TemplateValueSource valueSource) {
      switch (this.whichSource(propertyKey, valueSource)) {
         case PRIMARY:
            return this.primary.getDefinedEnumValues(this.getPrimaryPropertyKey(propertyKey), elementKey, valueSource);
         case SECONDARY:
            return this.secondary.getDefinedEnumValues(this.getSecondaryPropertyKey(propertyKey), elementKey, valueSource);
         default:
            throw new IllegalStateException("missing " + valueSource);
      }
   }

   private int getPropertyCount() {
      this.loadPropertyKeys();
      return this.propertyKeys.size();
   }

   private boolean hasPrimaryPropertyKey(int propertyKey) {
      this.loadPropertyKeys();
      return this.propertyKeys.get(propertyKey).getFirst() != null;
   }

   private boolean hasSecondaryPropertyKey(int propertyKey) {
      this.loadPropertyKeys();
      return this.propertyKeys.get(propertyKey).getSecond() != null;
   }

   private boolean hasBothPropertyKeys(int propertyKey) {
      return this.hasPrimaryPropertyKey(propertyKey) && this.hasSecondaryPropertyKey(propertyKey);
   }

   private int getPrimaryPropertyKey(int propertyKey) {
      this.loadPropertyKeys();
      return (Integer)this.propertyKeys.get(propertyKey).getFirst();
   }

   private int getSecondaryPropertyKey(int propertyKey) {
      this.loadPropertyKeys();
      return (Integer)this.propertyKeys.get(propertyKey).getSecond();
   }

   private void loadPropertyKeys() {
      if (this.propertyKeys == null) {
         this.propertyKeys = new ArrayList<>();
         List<Pair<Integer, String>> primaryPropertyNames = this.primary
            .getPropertyKeyStream()
            .mapToObj(key -> new Pair(key, this.primary.getPropertyName(key)))
            .collect(Collectors.toList());
         List<Pair<Integer, String>> secondaryPropertyNames = this.secondary
            .getPropertyKeyStream()
            .mapToObj(key -> new Pair(key, this.secondary.getPropertyName(key)))
            .collect(Collectors.toList());
         Set<String> processedProperties = new HashSet<>();

         for (Pair<Integer, String> primaryKeyAndName : primaryPropertyNames) {
            int primaryKey = (Integer)primaryKeyAndName.getFirst();
            String primaryName = (String)primaryKeyAndName.getSecond();
            if (!processedProperties.contains(primaryName)) {
               processedProperties.add(primaryName);
               Integer secondaryKey = null;

               for (Pair<Integer, String> secondaryKeyAndName : secondaryPropertyNames) {
                  if (primaryName.equals(secondaryKeyAndName.getSecond())) {
                     secondaryKey = (Integer)secondaryKeyAndName.getFirst();
                     break;
                  }
               }

               this.propertyKeys.add(new Pair(primaryKey, secondaryKey));
            }
         }

         for (int secondaryIndex = 0; secondaryIndex < secondaryPropertyNames.size(); secondaryIndex++) {
            String secondaryName = (String)secondaryPropertyNames.get(secondaryIndex).getSecond();
            if (!processedProperties.contains(secondaryName)) {
               processedProperties.add(secondaryName);
               int secondaryKey = (Integer)secondaryPropertyNames.get(secondaryIndex).getFirst();
               Pair<Integer, Integer> propertyKeyPair = new Pair(null, secondaryKey);
               int propertyIndex = 0;
               if (secondaryIndex > 0) {
                  for (int predecessorSecondaryKey = (Integer)secondaryPropertyNames.get(secondaryIndex - 1).getFirst();
                     propertyIndex < this.propertyKeys.size();
                     propertyIndex++
                  ) {
                     Integer otherSecondaryKey = (Integer)this.propertyKeys.get(propertyIndex).getSecond();
                     if (otherSecondaryKey != null && otherSecondaryKey == predecessorSecondaryKey) {
                        propertyIndex++;
                        break;
                     }
                  }
               }

               this.propertyKeys.add(propertyIndex, propertyKeyPair);
            }
         }
      }
   }

   private CombinedTemplateSource.PropertySource whichSource(int propertyKey, TemplateValueSource requestedSource) {
      switch (requestedSource) {
         case NON_VALUE:
            return CombinedTemplateSource.PropertySource.DONT_CARE;
         case LOCAL_VALUE:
            if (this.hasBothPropertyKeys(propertyKey)) {
               BValue primaryValue = this.primary.getPropertyValue(this.getPrimaryPropertyKey(propertyKey));
               BValue secondaryValue = this.secondary.getPropertyValue(this.getSecondaryPropertyKey(propertyKey));
               return primaryValue.equivalent(secondaryValue) ? CombinedTemplateSource.PropertySource.MISSING : CombinedTemplateSource.PropertySource.PRIMARY;
            }

            return this.hasPrimaryPropertyKey(propertyKey) ? CombinedTemplateSource.PropertySource.PRIMARY : CombinedTemplateSource.PropertySource.MISSING;
         case DEFAULT_VALUE:
            if (this.hasBothPropertyKeys(propertyKey)) {
               BValue primaryValue = this.primary.getPropertyValue(this.getPrimaryPropertyKey(propertyKey));
               BValue secondaryValue = this.secondary.getPropertyValue(this.getSecondaryPropertyKey(propertyKey));
               return secondaryValue.getType().equals(primaryValue.getType())
                  ? CombinedTemplateSource.PropertySource.SECONDARY
                  : CombinedTemplateSource.PropertySource.MISSING;
            }

            return this.hasSecondaryPropertyKey(propertyKey) ? CombinedTemplateSource.PropertySource.SECONDARY : CombinedTemplateSource.PropertySource.MISSING;
         case PRESENT_VALUE:
            if (this.hasBothPropertyKeys(propertyKey)) {
               BValue primaryValue = this.primary.getPropertyValue(this.getPrimaryPropertyKey(propertyKey));
               BValue secondaryValue = this.secondary.getPropertyValue(this.getSecondaryPropertyKey(propertyKey));
               return primaryValue.equivalent(secondaryValue) ? CombinedTemplateSource.PropertySource.SECONDARY : CombinedTemplateSource.PropertySource.PRIMARY;
            }

            return this.hasPrimaryPropertyKey(propertyKey) ? CombinedTemplateSource.PropertySource.PRIMARY : CombinedTemplateSource.PropertySource.SECONDARY;
         default:
            return CombinedTemplateSource.PropertySource.MISSING;
      }
   }

   private static enum PropertySource {
      DONT_CARE,
      PRIMARY,
      SECONDARY,
      MISSING;
   }
}
