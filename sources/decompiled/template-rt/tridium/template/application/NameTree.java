package com.tridium.template.application;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

public class NameTree {
   static String MACRO_PREFIX = "$";
   private final Map<String, NameTree> children = new HashMap<>();

   public NameTree() {
   }

   public NameTree(NameTree other) {
      this();

      for (Entry<String, NameTree> entry : other.children.entrySet()) {
         this.children.put(entry.getKey(), entry.getValue() == null ? null : new NameTree(entry.getValue()));
      }
   }

   public boolean isEmpty() {
      for (NameTree child : this.children.values()) {
         if (child == null || !child.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public void add(String leafName) {
      this.children.putIfAbsent(leafName, null);
   }

   public NameTree get(String branchName) {
      NameTree result = this.children.getOrDefault(branchName, null);
      if (result == null) {
         result = new NameTree();
         this.children.put(branchName, result);
      }

      return result;
   }

   public boolean has(String name) {
      return this.children.containsKey(name);
   }

   public boolean hasBranch(String branchName) {
      return this.children.getOrDefault(branchName, null) != null;
   }

   public boolean hasLeaf(String leafName) {
      return this.children.containsKey(leafName) && this.children.get(leafName) == null;
   }

   public NameTree fetch(String branchName) {
      return this.children.getOrDefault(branchName, null);
   }

   public Set<String> getNames() {
      return this.children.keySet();
   }

   public Set<String> getAllNames() {
      Set<String> result = new TreeSet<>();
      this.gatherAllNames(result);
      return result;
   }

   public void replaceMacro(String macroName, String newName) {
      String macroKey = MACRO_PREFIX + macroName;
      if (this.hasLeaf(macroKey)) {
         if (!this.has(newName)) {
            this.add(newName);
         }

         this.children.remove(macroKey);
      }

      for (NameTree childTree : this.children.values()) {
         if (childTree != null) {
            childTree.replaceMacro(macroName, newName);
         }
      }
   }

   public void removeMacro(String macroName) {
      String macroKey = MACRO_PREFIX + macroName;
      this.children.remove(macroKey);

      for (NameTree childTree : this.children.values()) {
         if (childTree != null) {
            childTree.removeMacro(macroName);
         }
      }
   }

   private void gatherAllNames(Set<String> result) {
      result.addAll(this.children.keySet());

      for (NameTree childTree : this.children.values()) {
         if (childTree != null) {
            childTree.gatherAllNames(result);
         }
      }
   }
}
