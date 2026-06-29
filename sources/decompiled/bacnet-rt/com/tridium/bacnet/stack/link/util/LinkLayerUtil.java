package com.tridium.bacnet.stack.link.util;

import com.tridium.bacnet.stack.link.ip.BacnetNetworkAdapter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;
import javax.baja.naming.SlotPath;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnumRange;

public final class LinkLayerUtil {
   public static final boolean NUMBER_DUPLICATES = true;
   private static final int NONE_IDX = -1;
   private static final int FIRST_IDX = 0;
   private static List<Pattern> includedPatterns;
   private static List<Pattern> excludedPatterns;

   private LinkLayerUtil() {
   }

   public static BDynamicEnum select(int ord, BEnumRange range) {
      return BDynamicEnum.make(ord, range);
   }

   public static Collection<BacnetNetworkAdapter> filterAdapters(Collection<BacnetNetworkAdapter> adapters) {
      List<Pattern> includedPatterns = getIncludedPatterns();
      List<Pattern> excludedPatterns = getExcludedPatterns();
      List<BacnetNetworkAdapter> filtered = new ArrayList<>(adapters.size());

      for (BacnetNetworkAdapter adapter : adapters) {
         String identifier = adapter.getIdentifier().trim();
         if ((includedPatterns.isEmpty() || matchesAny(identifier, includedPatterns)) && !matchesAny(identifier, excludedPatterns)) {
            filtered.add(adapter);
         }
      }

      return filtered;
   }

   private static boolean matchesAny(String identifier, List<Pattern> patterns) {
      for (Pattern pattern : patterns) {
         if (!pattern.pattern().isEmpty() && pattern.matcher(identifier).matches()) {
            return true;
         }
      }

      return false;
   }

   private static List<Pattern> getIncludedPatterns() {
      if (includedPatterns == null) {
         String patterns = AccessController.doPrivileged((PrivilegedAction<String>)(() -> System.getProperty("niagara.bacnet.included.ip.adapters", "")))
            .trim();
         includedPatterns = Collections.unmodifiableList(makePatternList(patterns));
      }

      return includedPatterns;
   }

   private static List<Pattern> getExcludedPatterns() {
      if (excludedPatterns == null) {
         String patterns = AccessController.doPrivileged((PrivilegedAction<String>)(() -> System.getProperty("niagara.bacnet.excluded.ip.adapters", "")))
            .trim();
         excludedPatterns = Collections.unmodifiableList(makePatternList(patterns));
      }

      return excludedPatterns;
   }

   private static List<Pattern> makePatternList(String patternStrings) {
      ArrayList<Pattern> patterns = new ArrayList<>();

      for (String patternString : patternStrings.split(";")) {
         String trimmed = patternString.trim();
         if (!trimmed.isEmpty()) {
            patterns.add(Pattern.compile(trimmed));
         }
      }

      patterns.trimToSize();
      return patterns;
   }

   public static BEnumRange makeIdRange(Collection<BacnetNetworkAdapter> adapters, String none) {
      Vector<String> ids = new Vector<>();

      for (BacnetNetworkAdapter a : adapters) {
         ids.add(a.getIdentifier());
      }

      return makeEnumRange(ids, false, none);
   }

   public static BEnumRange makeIpRange(Collection<BacnetNetworkAdapter> adapters, String none) {
      Vector<String> ips = new Vector<>();

      for (BacnetNetworkAdapter netIf : adapters) {
         ips.add(netIf.getAddress());
      }

      return makeEnumRange(ips, false, none);
   }

   public static BEnumRange makeDescRange(Collection<BacnetNetworkAdapter> adapters, String none) {
      Vector<String> desc = new Vector<>();

      for (BacnetNetworkAdapter a : adapters) {
         desc.add(a.getDescription());
      }

      return makeEnumRange(desc, true, none);
   }

   public static BEnumRange makeEnumRange(Vector<String> v, boolean fixDuplicates, String none) {
      int size = v.size() + 1;
      String[] arr = new String[size];
      arr[0] = none;
      if (!v.isEmpty()) {
         for (int i = 1; i < size; i++) {
            arr[i] = v.get(i - 1);
         }

         if (fixDuplicates) {
            fixDuplicates(arr);
         }

         for (int i = 1; i < size; i++) {
            arr[i] = SlotPath.escape(arr[i]);
         }
      }

      return BEnumRange.make(ords(arr), arr);
   }

   public static int ordinal(String tag, BEnumRange range, String none) {
      int ordinal = 0;
      if (!tag.equals(none) && range.isTag(tag)) {
         ordinal = range.tagToOrdinal(tag);
      } else {
         ordinal = range.getOrdinals().length == 2 ? 0 : -1;
      }

      return ordinal;
   }

   private static void fixDuplicates(String[] arr) {
      HashMap<String, LinkLayerUtil.AdapterNode> names = new HashMap<>(arr.length);

      for (int i = 1; i < arr.length; i++) {
         LinkLayerUtil.AdapterNode node = names.get(arr[i]);
         if (node == null) {
            names.put(arr[i], new LinkLayerUtil.AdapterNode(arr[i]));
         } else {
            node.increment();
         }
      }

      for (int ix = arr.length - 1; ix > 0; ix--) {
         LinkLayerUtil.AdapterNode node = names.get(arr[ix]);
         arr[ix] = node.toString();
         node.decrement();
      }
   }

   private static int[] ords(String[] values) {
      int[] ords = new int[values.length];

      for (int i = 0; i < ords.length; i++) {
         ords[i] = i - 1;
      }

      return ords;
   }

   public static String addressToString(byte[] address) {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < address.length; i++) {
         sb.append(address[i] & 255).append(".");
      }

      sb.deleteCharAt(sb.length() - 1);
      return sb.toString();
   }

   private static class AdapterNode {
      public final String value;
      public int count = 1;
      public boolean hasDuplicates = false;

      public AdapterNode(String value) {
         this.value = value;
      }

      public void increment() {
         this.hasDuplicates = true;
         this.count++;
      }

      public void decrement() {
         this.count--;
      }

      @Override
      public int hashCode() {
         return this.value.hashCode();
      }

      @Override
      public boolean equals(Object other) {
         if (other instanceof LinkLayerUtil.AdapterNode) {
            LinkLayerUtil.AdapterNode node = (LinkLayerUtil.AdapterNode)other;
            return this.value.equals(node.value);
         } else {
            return false;
         }
      }

      @Override
      public String toString() {
         return this.hasDuplicates ? this.value + " #" + this.count : this.value;
      }
   }
}
