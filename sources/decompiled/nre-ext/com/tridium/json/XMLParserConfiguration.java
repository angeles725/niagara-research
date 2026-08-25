package com.tridium.json;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class XMLParserConfiguration extends ParserConfiguration {
   public static final XMLParserConfiguration ORIGINAL = new XMLParserConfiguration();
   public static final XMLParserConfiguration KEEP_STRINGS = new XMLParserConfiguration().withKeepStrings(true);
   private String cDataTagName;
   private boolean convertNilAttributeToNull;
   private Map<String, XMLXsiTypeConverter<?>> xsiTypeMap;
   private Set<String> forceList;

   public XMLParserConfiguration() {
      this.cDataTagName = "content";
      this.convertNilAttributeToNull = false;
      this.xsiTypeMap = Collections.emptyMap();
      this.forceList = Collections.emptySet();
   }

   @Deprecated
   public XMLParserConfiguration(boolean keepStrings) {
      this(keepStrings, "content", false);
   }

   @Deprecated
   public XMLParserConfiguration(String cDataTagName) {
      this(false, cDataTagName, false);
   }

   @Deprecated
   public XMLParserConfiguration(boolean keepStrings, String cDataTagName) {
      super(keepStrings, 512);
      this.cDataTagName = cDataTagName;
      this.convertNilAttributeToNull = false;
   }

   @Deprecated
   public XMLParserConfiguration(boolean keepStrings, String cDataTagName, boolean convertNilAttributeToNull) {
      super(keepStrings, 512);
      this.cDataTagName = cDataTagName;
      this.convertNilAttributeToNull = convertNilAttributeToNull;
   }

   private XMLParserConfiguration(
      boolean keepStrings,
      String cDataTagName,
      boolean convertNilAttributeToNull,
      Map<String, XMLXsiTypeConverter<?>> xsiTypeMap,
      Set<String> forceList,
      int maxNestingDepth
   ) {
      super(keepStrings, maxNestingDepth);
      this.cDataTagName = cDataTagName;
      this.convertNilAttributeToNull = convertNilAttributeToNull;
      this.xsiTypeMap = Collections.unmodifiableMap(xsiTypeMap);
      this.forceList = Collections.unmodifiableSet(forceList);
   }

   protected XMLParserConfiguration clone() {
      return new XMLParserConfiguration(
         this.keepStrings, this.cDataTagName, this.convertNilAttributeToNull, this.xsiTypeMap, this.forceList, this.maxNestingDepth
      );
   }

   public XMLParserConfiguration withKeepStrings(boolean newVal) {
      return super.withKeepStrings(newVal);
   }

   public String getcDataTagName() {
      return this.cDataTagName;
   }

   public XMLParserConfiguration withcDataTagName(String newVal) {
      XMLParserConfiguration newConfig = this.clone();
      newConfig.cDataTagName = newVal;
      return newConfig;
   }

   public boolean isConvertNilAttributeToNull() {
      return this.convertNilAttributeToNull;
   }

   public XMLParserConfiguration withConvertNilAttributeToNull(boolean newVal) {
      XMLParserConfiguration newConfig = this.clone();
      newConfig.convertNilAttributeToNull = newVal;
      return newConfig;
   }

   public Map<String, XMLXsiTypeConverter<?>> getXsiTypeMap() {
      return this.xsiTypeMap;
   }

   public XMLParserConfiguration withXsiTypeMap(Map<String, XMLXsiTypeConverter<?>> xsiTypeMap) {
      XMLParserConfiguration newConfig = this.clone();
      Map<String, XMLXsiTypeConverter<?>> cloneXsiTypeMap = new HashMap<>(xsiTypeMap);
      newConfig.xsiTypeMap = Collections.unmodifiableMap(cloneXsiTypeMap);
      return newConfig;
   }

   public Set<String> getForceList() {
      return this.forceList;
   }

   public XMLParserConfiguration withForceList(Set<String> forceList) {
      XMLParserConfiguration newConfig = this.clone();
      Set<String> cloneForceList = new HashSet<>(forceList);
      newConfig.forceList = Collections.unmodifiableSet(cloneForceList);
      return newConfig;
   }

   public XMLParserConfiguration withMaxNestingDepth(int maxNestingDepth) {
      return super.withMaxNestingDepth(maxNestingDepth);
   }
}
