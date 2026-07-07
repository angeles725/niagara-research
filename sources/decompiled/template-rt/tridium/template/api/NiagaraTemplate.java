package com.tridium.template.api;

import com.tridium.nre.util.tuple.Pair;
import com.tridium.template.api.impl.FileTemplateSource;
import com.tridium.template.api.impl.NewTemplateSource;
import com.tridium.template.api.impl.NiagaraTemplateUtils;
import com.tridium.template.api.impl.TemplateSource;
import com.tridium.template.api.impl.TemplateSourceFactory;
import com.tridium.template.file.BNtplFile;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.baja.file.BIDirectory;
import javax.baja.file.FilePath;
import javax.baja.naming.BOrd;
import javax.baja.sys.BComponent;
import javax.baja.sys.BStation;
import javax.baja.sys.Sys;

public final class NiagaraTemplate implements AutoCloseable {
   private final TemplateSource source;

   private NiagaraTemplate(TemplateSource source) {
      Objects.requireNonNull(source);
      this.source = source;
   }

   public static NiagaraTemplate createFrom(BComponent sourceComponent) {
      Objects.requireNonNull(sourceComponent);
      NewTemplateSource source = TemplateSourceFactory.create(sourceComponent);
      return source == null ? null : new NiagaraTemplate(source);
   }

   public static NiagaraTemplate createStationTemplateFrom(BStation sourceStation) {
      Objects.requireNonNull(sourceStation);
      NewTemplateSource source = TemplateSourceFactory.create(sourceStation, false);
      return source == null ? null : new NiagaraTemplate(source);
   }

   public static NiagaraTemplate createStationTemplateFrom(BStation sourceStation, BIDirectory stationHomeDir) {
      Objects.requireNonNull(sourceStation);
      NewTemplateSource source = TemplateSourceFactory.create(sourceStation, stationHomeDir);
      return source == null ? null : new NiagaraTemplate(source);
   }

   public static NiagaraTemplate createStationTemplateFrom(BStation sourceStation, BIDirectory stationHomeDir, BIDirectory stationProtectedHomeDir) {
      Objects.requireNonNull(sourceStation);
      NewTemplateSource source = TemplateSourceFactory.create(sourceStation, stationHomeDir, stationProtectedHomeDir, false);
      return source == null ? null : new NiagaraTemplate(source);
   }

   public static NiagaraTemplate createApplicationFrom(BStation sourceStation) {
      Objects.requireNonNull(sourceStation);
      NewTemplateSource source = TemplateSourceFactory.create(sourceStation, true);
      return source == null ? null : new NiagaraTemplate(source);
   }

   public static NiagaraTemplate createApplicationFrom(BStation sourceStation, BIDirectory stationHomeDir) {
      Objects.requireNonNull(sourceStation);
      NewTemplateSource source = TemplateSourceFactory.create(sourceStation, stationHomeDir, true);
      return source == null ? null : new NiagaraTemplate(source);
   }

   public static NiagaraTemplate createApplicationFrom(BStation sourceStation, BIDirectory stationHomeDir, BIDirectory stationProtectedHomeDir) {
      Objects.requireNonNull(sourceStation);
      NewTemplateSource source = TemplateSourceFactory.create(sourceStation, stationHomeDir, stationProtectedHomeDir, true);
      return source == null ? null : new NiagaraTemplate(source);
   }

   public static NiagaraTemplate createFromLocalStation() {
      return createStationTemplateFrom(Objects.requireNonNull(Sys.getStation()));
   }

   public static NiagaraTemplate createApplicationFromLocalStation() {
      return createApplicationFrom(Objects.requireNonNull(Sys.getStation()));
   }

   public static NiagaraTemplate open(BComponent deployedTemplate) {
      Objects.requireNonNull(deployedTemplate);
      TemplateSource source = TemplateSourceFactory.open(deployedTemplate);
      return source == null ? null : new NiagaraTemplate(source);
   }

   public static NiagaraTemplate load(BNtplFile templateFile) {
      Objects.requireNonNull(templateFile);
      TemplateSource source = FileTemplateSource.load(templateFile);
      return new NiagaraTemplate(source);
   }

   @Override
   public void close() {
      this.source.close();
   }

   public void setUseMinorVersionOnDeployment(boolean useMinorVersionOnDeployment) {
      this.source.setUseMinorVersionOnDeployment(useMinorVersionOnDeployment);
   }

   public boolean getUseMinorVersionOnDeployment() {
      return this.source.getUseMinorVersionOnDeployment();
   }

   public void save(OutputStream out) throws IOException {
      this.source.save(out);
   }

   public BOrd save(FilePath filePath) throws IOException {
      return this.source.save(filePath);
   }

   public BOrd save() throws IOException {
      return this.source.save();
   }

   public TemplateType getTemplateType() {
      return NiagaraTemplateUtils.replaceNull(TemplateType.class, this.source.getTemplateType());
   }

   public TemplateScope getTemplateScope() {
      return NiagaraTemplateUtils.replaceNull(TemplateScope.class, this.source.getTemplateScope());
   }

   public TemplateSourceType getSourceType() {
      return NiagaraTemplateUtils.replaceNull(TemplateSourceType.class, this.source.getSourceType());
   }

   public String getTitle() {
      return NiagaraTemplateUtils.replaceNull(this.source.getTitle());
   }

   public String getVendor() {
      return NiagaraTemplateUtils.replaceNull(this.source.getVendor());
   }

   public String getFileName() {
      return NiagaraTemplateUtils.replaceNull(this.source.getFileName());
   }

   public String getVersion() {
      return NiagaraTemplateUtils.replaceNull(this.source.getVersion());
   }

   public String getUid() {
      return NiagaraTemplateUtils.replaceNull(this.source.getUid());
   }

   public String getDescription() {
      return NiagaraTemplateUtils.replaceNull(this.source.getDescription());
   }

   public String getInfo() {
      return NiagaraTemplateUtils.replaceNull(this.source.getInfo());
   }

   public List<TemplateProperty> properties() {
      return this.source.getPropertyKeyStream().mapToObj(this::getProperty).collect(Collectors.toList());
   }

   public String getBaseName() {
      return NiagaraTemplateUtils.replaceNull(this.source.getBaseName());
   }

   public List<TemplateProperty> requiredProperties() {
      return this.source.getPropertyKeyStream().filter(this::isARequiredProperty).mapToObj(this::getProperty).collect(Collectors.toList());
   }

   public List<OptionalComponent> optionalComponents() {
      return this.source.getOptionalComponentKeyStream().mapToObj(this::getOptionalComponent).collect(Collectors.toList());
   }

   public List<TemplateElement> propertyElements() {
      return this.propertyElementKeyStream().map(k -> this.getElement((Integer)k.getFirst(), (Integer)k.getSecond())).collect(Collectors.toList());
   }

   public TemplateProperty getProperty(final String propertyName) {
      Optional<Integer> propertyKey = this.source.getPropertyKeyStream().boxed().filter(new Predicate<Integer>() {
         public boolean test(Integer propertyKey) {
            return Objects.equals(NiagaraTemplate.this.source.getPropertyName(propertyKey), propertyName);
         }
      }).findFirst();
      return propertyKey.<TemplateProperty>map(this::getProperty).orElse(null);
   }

   public List<TemplateElement> requiredPropertyElements() {
      return this.source
         .getPropertyKeyStream()
         .filter(this::isARequiredProperty)
         .boxed()
         .flatMap(pk -> this.source.getElementKeyStream(pk).mapToObj(ek -> new Pair(pk, ek)))
         .map(k -> this.getElement((Integer)k.getFirst(), (Integer)k.getSecond()))
         .collect(Collectors.toList());
   }

   public boolean hasSensitiveDataConfig() {
      return this.propertyElementKeyStream()
         .anyMatch(k -> this.getValueType((Integer)k.getFirst(), (Integer)k.getSecond(), TemplateValueSource.PRESENT_VALUE) == TemplateValueType.PASSWORD);
   }

   public boolean hasComponent(String path) {
      return this.source.hasComponent(path);
   }

   public boolean isCompatibleWith(NiagaraTemplate otherTemplate) {
      Map<String, TemplateProperty> propertyMap = otherTemplate.properties()
         .stream()
         .collect(Collectors.toMap(TemplateProperty::getName, prop -> (TemplateProperty)prop));

      for (TemplateProperty property : this.properties()) {
         if (!propertyMap.containsKey(property.getName())) {
            return false;
         }

         List<TemplateElement> theirElements = propertyMap.get(property.getName()).elements();
         List<TemplateElement> ourElements = property.elements();
         if (theirElements.size() != ourElements.size()) {
            return false;
         }

         for (int i = 0; i < ourElements.size(); i++) {
            if (!Objects.equals(ourElements.get(i).getName(), theirElements.get(i).getName())
               || !ourElements.get(i).getValueType().equals(theirElements.get(i).getValueType())) {
               return false;
            }
         }
      }

      return true;
   }

   OptionalComponent getOptionalComponent(int optionalComponentKey) {
      return new OptionalComponent(this, optionalComponentKey);
   }

   TemplateProperty getProperty(int propertyKey) {
      return new TemplateProperty(this, propertyKey);
   }

   TemplateElement getElement(int propertyKey, int elementKey) {
      return new TemplateElement(this, propertyKey, elementKey);
   }

   TemplateValue getElementValue(int propertyKey, int elementKey, TemplateValueSource valueSource) {
      if (valueSource == TemplateValueSource.PRESENT_VALUE) {
         valueSource = this.source.hasLocalValue(propertyKey) ? TemplateValueSource.LOCAL_VALUE : TemplateValueSource.DEFAULT_VALUE;
      }

      return new TemplateValue(this, propertyKey, elementKey, valueSource);
   }

   TemplateElement getElement(int propertyKey, String elementName) {
      Optional<Integer> foundKey = this.source
         .getElementKeyStream(propertyKey)
         .boxed()
         .filter(elementKey -> Objects.equals(this.source.getElementName(propertyKey, elementKey), elementName))
         .findFirst();
      return foundKey.<TemplateElement>map(elementKey -> this.getElement(propertyKey, elementKey)).orElse(null);
   }

   String getPropertyName(int propertyKey) {
      return this.source.getPropertyName(propertyKey);
   }

   String getPropertyPath(int propertyKey) {
      return this.source.getPropertyPath(propertyKey);
   }

   String getOptionalComponentPath(int optionalComponentKey) {
      return NiagaraTemplateUtils.replaceNull(this.source.getOptionalComponentPath(optionalComponentKey));
   }

   IntStream getOptionalComponentPropertyKeyStream(int optionalComponentKey) {
      String path = this.source.getOptionalComponentPath(optionalComponentKey) + '/';
      return this.source.getPropertyKeyStream().filter(propertyKey -> this.getPropertyPath(propertyKey).startsWith(path));
   }

   String getOptionalComponentNType(int optionalComponentKey) {
      return this.source.getOptionalComponentNType(optionalComponentKey);
   }

   boolean isOptionalInstalled(int optionalComponentKey) {
      return this.source.isOptionalInstalled(optionalComponentKey);
   }

   String getElementName(int propertyKey, int elementKey) {
      return this.source.getElementName(propertyKey, elementKey);
   }

   boolean hasLocalValue(int propertyKey) {
      return this.source.hasLocalValue(propertyKey);
   }

   boolean isValueMissing(int propertyKey, int elementKey, TemplateValueSource valueSource) {
      return this.source.isValueMissing(propertyKey, valueSource);
   }

   TemplateValueType getValueType(int propertyKey, int elementKey, TemplateValueSource valueSource) {
      return this.source.getValueType(propertyKey, elementKey);
   }

   boolean isValueNullable(int propertyKey, int elementKey) {
      return this.source.isValueNullable(propertyKey, elementKey);
   }

   boolean isValueNull(int propertyKey, int elementKey, TemplateValueSource valueSource) {
      return this.source.isValueNull(propertyKey, elementKey, valueSource);
   }

   double getNumericValue(int propertyKey, int elementKey, TemplateValueSource valueSource) {
      return this.source.getNumericValue(propertyKey, elementKey, valueSource);
   }

   long getIntegerValue(int propertyKey, int elementKey, TemplateValueSource valueSource) {
      return this.source.getIntegerValue(propertyKey, elementKey, valueSource);
   }

   boolean getBooleanValue(int propertyKey, int elementKey, TemplateValueSource valueSource) {
      return this.source.getBooleanValue(propertyKey, elementKey, valueSource);
   }

   String getStringValue(int propertyKey, int elementKey, TemplateValueSource valueSource) {
      return this.source.getStringValue(propertyKey, elementKey, valueSource);
   }

   Map<Integer, String> getDefinedEnumValues(int propertyKey, int elementKey, TemplateValueSource valueSource) {
      return this.source.getDefinedEnumValues(propertyKey, elementKey, valueSource);
   }

   String getValueNType(int propertyKey, int elementKey, TemplateValueSource valueSource) {
      return this.source.getValueNType(propertyKey, elementKey);
   }

   String getPropertyUserTip(int propertyKey) {
      return this.source.getPropertyUserTip(propertyKey);
   }

   IntStream getElementKeyStream(int propertyKey) {
      return this.source.getElementKeyStream(propertyKey);
   }

   Stream<Pair<Integer, Integer>> getOptionalComponentPropertyElementKeyStream(int optionalComponentKey) {
      return this.getOptionalComponentPropertyKeyStream(optionalComponentKey)
         .boxed()
         .flatMap(pk -> this.source.getElementKeyStream(pk).mapToObj(ek -> new Pair(pk, ek)));
   }

   private Stream<Pair<Integer, Integer>> propertyElementKeyStream() {
      return this.source.getPropertyKeyStream().boxed().flatMap(pk -> this.source.getElementKeyStream(pk).mapToObj(ek -> new Pair(pk, ek)));
   }

   private boolean isARequiredProperty(int propertyKey) {
      String propertyPath = this.source.getPropertyPath(propertyKey);
      return this.source
         .getOptionalComponentKeyStream()
         .noneMatch(optionalComponentKey -> propertyPath.startsWith(this.getOptionalComponentPath(optionalComponentKey) + '/'));
   }
}
