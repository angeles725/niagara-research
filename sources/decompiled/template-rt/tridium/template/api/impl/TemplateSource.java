package com.tridium.template.api.impl;

import com.tridium.template.api.TemplateScope;
import com.tridium.template.api.TemplateSourceType;
import com.tridium.template.api.TemplateType;
import com.tridium.template.api.TemplateValueSource;
import com.tridium.template.api.TemplateValueType;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import javax.baja.file.FilePath;
import javax.baja.naming.BOrd;

public abstract class TemplateSource implements AutoCloseable {
   private boolean useMinorVersionOnDeployment = true;
   protected static final Logger log = Logger.getLogger("template");

   @Override
   public void close() {
   }

   public void setUseMinorVersionOnDeployment(boolean useMinorVersionOnDeployment) {
      this.useMinorVersionOnDeployment = useMinorVersionOnDeployment;
   }

   public boolean getUseMinorVersionOnDeployment() {
      return this.useMinorVersionOnDeployment;
   }

   public void save(OutputStream out) throws IOException {
      throw new UnsupportedOperationException("TemplateSource.save not yet implemented!");
   }

   public BOrd save(FilePath filePath) throws IOException {
      throw new UnsupportedOperationException("TemplateSource.save not yet implemented!");
   }

   public BOrd save() throws IOException {
      throw new UnsupportedOperationException("TemplateSource.save not yet implemented!");
   }

   public TemplateSourceType getSourceType() {
      throw new UnsupportedOperationException("TemplateSource.getSourceType not yet implemented!");
   }

   public TemplateType getTemplateType() {
      throw new UnsupportedOperationException("TemplateSource.getTemplateType not yet implemented!");
   }

   public TemplateScope getTemplateScope() {
      throw new UnsupportedOperationException("TemplateSource.getTemplateScope not yet implemented!");
   }

   public String getTitle() {
      throw new UnsupportedOperationException("TemplateSource.getTitle not yet implemented!");
   }

   public String getVendor() {
      return null;
   }

   public String getFileName() {
      return this.getTitle() + "." + this.getFileExtension();
   }

   public String getVersion() {
      throw new UnsupportedOperationException("TemplateSource.getVersion not yet implemented!");
   }

   public String getUid() {
      throw new UnsupportedOperationException("TemplateSource.getUid not yet implemented!");
   }

   public String getDescription() {
      return null;
   }

   public String getInfo() {
      return null;
   }

   public String getBaseName() {
      throw new UnsupportedOperationException("TemplateSource.getBaseName not yet implemented!");
   }

   public IntStream getPropertyKeyStream() {
      throw new UnsupportedOperationException("TemplateSource.propertyKeyStream not yet implemented!");
   }

   public IntStream getElementKeyStream(int propertyKey) {
      throw new UnsupportedOperationException("TemplateSource.elementKeyStream not yet implemented!");
   }

   public IntStream getOptionalComponentKeyStream() {
      return IntStream.empty();
   }

   public String getOptionalComponentPath(int optionalComponentKey) {
      throw new UnsupportedOperationException("TemplateSource.getOptionalComponentPath not yet implemented!");
   }

   public String getOptionalComponentNType(int optionalComponentKey) {
      throw new UnsupportedOperationException("TemplateSource.getOptionalComponentSlotType not yet implemented!");
   }

   public boolean isOptionalInstalled(int optionalComponentKey) {
      throw new UnsupportedOperationException("TemplateSource.isOptionalInstalled not yet implemented!");
   }

   public String getPropertyName(int propertyKey) {
      throw new UnsupportedOperationException("TemplateSource.getPropertyName not yet implemented!");
   }

   public String getPropertyPath(int propertyKey) {
      throw new UnsupportedOperationException("TemplateSource.getPropertyPath not yet implemented!");
   }

   public String getPropertyUserTip(int propertyKey) {
      throw new UnsupportedOperationException("TemplateSource.getPropertyUserTip not yet implemented!");
   }

   public String getElementName(int propertyKey, int elementKey) {
      return null;
   }

   public TemplateValueType getValueType(int propertyKey, int elementKey) {
      return TemplateValueType.UNKNOWN;
   }

   public boolean isValueNullable(int propertyKey, int elementKey) {
      return false;
   }

   public String getValueNType(int propertyKey, int elementKey) {
      throw new UnsupportedOperationException("TemplateSource.getValueNType not yet implemented!");
   }

   public boolean hasLocalValue(int propertyKey) {
      return false;
   }

   public boolean isValueMissing(int propertyKey, TemplateValueSource valueSource) {
      return true;
   }

   public boolean isValueNull(int propertyKey, int elementKey, TemplateValueSource valueSource) {
      throw new UnsupportedOperationException("TemplateSource.isValueNull not yet implemented!");
   }

   public double getNumericValue(int propertyKey, int elementKey, TemplateValueSource valueSource) {
      throw new UnsupportedOperationException("TemplateSource.getNumericElementValue not yet implemented!");
   }

   public long getIntegerValue(int propertyKey, int elementKey, TemplateValueSource valueSource) {
      throw new UnsupportedOperationException("TemplateSource.getIntegerElementValue not yet implemented!");
   }

   public boolean getBooleanValue(int propertyKey, int elementKey, TemplateValueSource valueSource) {
      throw new UnsupportedOperationException("TemplateSource.getBooleanElementValue not yet implemented!");
   }

   public String getStringValue(int propertyKey, int elementKey, TemplateValueSource valueSource) {
      throw new UnsupportedOperationException("TemplateSource.getStringElementValue not yet implemented!");
   }

   public Map<Integer, String> getDefinedEnumValues(int propertyKey, int elementKey, TemplateValueSource valueSource) {
      throw new UnsupportedOperationException("TemplateSource.getDefinedEnumElementValues not yet implemented!");
   }

   public boolean hasComponent(String componentPath) {
      throw new UnsupportedOperationException("TemplateSource.hasComponent not yet implemented!");
   }

   protected String getFileExtension() {
      return "ntpl";
   }
}
