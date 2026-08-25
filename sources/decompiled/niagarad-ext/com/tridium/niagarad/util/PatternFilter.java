package com.tridium.niagarad.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import javax.baja.nre.util.Array;

public class PatternFilter {
   private PatternFilter.State start;
   private PatternFilter.State accept;
   private String pattern;

   public static PatternFilter[] parseList(String listOfPatterns, String separators) {
      ArrayList<PatternFilter> list = new ArrayList<>();
      StringTokenizer st = new StringTokenizer(listOfPatterns, separators);

      while (st.hasMoreTokens()) {
         list.add(new PatternFilter(st.nextToken()));
      }

      return list.toArray(new PatternFilter[0]);
   }

   public static PatternFilter[] parseList(String listOfPatterns) {
      return parseList(listOfPatterns, " \t,;");
   }

   public PatternFilter(String pattern) {
      this.pattern = pattern;
      this.parse();
   }

   public String getPattern() {
      return this.pattern;
   }

   public String getDescription() {
      return this.pattern;
   }

   public boolean accept(File file) {
      return this.accept(file.getName());
   }

   public boolean accept(Object object) {
      return this.accept(object.toString());
   }

   public boolean accept(String string) {
      Array<PatternFilter.State> curStates = new Array(PatternFilter.State.class);
      curStates.push(this.start);
      char[] characterBuffer = string.toCharArray();

      for (char character : characterBuffer) {
         Array<PatternFilter.State> nextStates = new Array(PatternFilter.State.class);

         while (!curStates.isEmpty()) {
            nextStates.addAll(((PatternFilter.State)curStates.pop()).transition(character));
         }

         if (nextStates.isEmpty()) {
            return false;
         }

         curStates = nextStates;
      }

      return curStates.contains(this.accept);
   }

   public boolean hasWildChars() {
      return this.pattern.indexOf(42) != -1 || this.pattern.indexOf(63) != -1;
   }

   private void parse() {
      PatternFilter.State cur = this.start = new PatternFilter.State();
      char[] buf = this.pattern.toCharArray();

      for (int i = 0; i < buf.length; i++) {
         char c = buf[i];
         if (c == '*') {
            if (i <= 0 || buf[i - 1] != '*') {
               cur.addTransition(PatternFilter.Condition.any(), cur);
            }
         } else if (c == '?') {
            cur = cur.addTransition(PatternFilter.Condition.any(), new PatternFilter.State());
         } else {
            cur = cur.addTransition(PatternFilter.Condition.exact(c), new PatternFilter.State());
         }
      }

      this.accept = cur;
   }

   private abstract static class Condition {
      private Condition() {
      }

      public abstract boolean match(char var1);

      public static PatternFilter.Condition exact(final char c) {
         return new PatternFilter.Condition() {
            @Override
            public boolean match(char test) {
               return c == test;
            }
         };
      }

      public static PatternFilter.Condition any() {
         return new PatternFilter.Condition() {
            @Override
            public boolean match(char test) {
               return true;
            }
         };
      }
   }

   private class State {
      private Map<PatternFilter.Condition, PatternFilter.State> transitions = new HashMap<>();

      private State() {
      }

      public PatternFilter.State addTransition(PatternFilter.Condition c, PatternFilter.State next) {
         this.transitions.put(c, next);
         return next;
      }

      public PatternFilter.State[] transition(char c) {
         Array<PatternFilter.State> outStates = new Array(PatternFilter.State.class);
         this.transitions.keySet().stream().filter(cond -> cond.match(c)).forEach(cond -> outStates.add(this.transitions.get(cond)));
         return (PatternFilter.State[])outStates.trim();
      }
   }
}
