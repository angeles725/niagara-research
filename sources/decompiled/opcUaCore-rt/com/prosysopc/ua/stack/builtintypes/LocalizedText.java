package com.prosysopc.ua.stack.builtintypes;

import com.prosysopc.ua.stack.core.Identifiers;
import com.prosysopc.ua.stack.utils.StringUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LocalizedText {
   public static final Pattern LOCALE_PATTERN = Pattern.compile("^(([a-z]{2})(-([A-Z]{2,3}){1})?)?$");
   public static final Locale NO_LOCALE = Locale.ROOT;
   @Deprecated
   public static final Locale NULL_LOCALE = NO_LOCALE;
   public static final LocalizedText[] EMPTY_ARRAY = new LocalizedText[0];
   @Deprecated
   public static final NodeId ID = Identifiers.LocalizedText;
   public static final LocalizedText EMPTY = new LocalizedText("", NULL_LOCALE);
   @Deprecated
   public static final LocalizedText NULL = EMPTY;
   public static final LocalizedText EMPTY_EN = english("");
   private final LocalizedText.a delegate;

   public static LocalizedText.Builder builder() {
      return new LocalizedText.Builder();
   }

   public static LocalizedText english(String var0) {
      return new LocalizedText(var0, "en");
   }

   public static LocalizedText merge(LocalizedText var0, LocalizedText var1) {
      boolean var2 = true;
      if (var0 == null) {
         var0 = EMPTY;
      }

      LocalizedText.Builder var3 = var0.toBuilder();
      if (var2) {
         var3.cnD();
      }

      var3.setTexts(var1);
      if (var2) {
         var3.cnD();
      }

      return var3.build();
   }

   public static Locale toLocale(String var0) {
      if (var0 == null) {
         return Locale.ROOT;
      } else {
         Matcher var1 = LOCALE_PATTERN.matcher(var0);
         if (!var1.matches()) {
            return NO_LOCALE;
         } else {
            String var2 = var1.group(2);
            String var3 = var1.group(4);
            if (var2 == null) {
               var2 = "";
            }

            if (var3 == null) {
               var3 = "";
            }

            return new Locale(var2, var3);
         }
      }
   }

   public static String toLocaleId(Locale var0) {
      return var0 == null ? "" : var0.getLanguage() + (!var0.getCountry().equals("") ? "-" + var0.getCountry() : "");
   }

   public LocalizedText(String var1) {
      this(var1, NO_LOCALE);
   }

   public LocalizedText(String var1, Locale var2) {
      this(var1, var2 == null ? null : toLocaleId(var2));
   }

   public LocalizedText(String var1, String var2) {
      this(new LocalizedText.c(var2, var1));
   }

   private LocalizedText(LocalizedText.a var1) {
      if (var1 == null) {
         throw new IllegalStateException("Internal delegate was null");
      } else {
         this.delegate = var1;
      }
   }

   public LocalizedText asSingleLocale(List<Locale> var1) {
      return this.delegate instanceof LocalizedText.c ? this : new LocalizedText(this.delegate.h(var1));
   }

   public LocalizedText asSingleLocale(Locale var1) {
      return var1 == null ? this.asSingleLocale(Collections.emptyList()) : this.asSingleLocale(Arrays.asList(var1));
   }

   @Override
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (!(var1 instanceof LocalizedText)) {
         return false;
      } else {
         LocalizedText var2 = (LocalizedText)var1;
         return Objects.equals(this.delegate, var2.delegate);
      }
   }

   public Locale getLocale() {
      return toLocale(this.getLocaleId());
   }

   public String getLocaleId() {
      return this.delegate.getLocaleId();
   }

   public String getText() {
      return this.delegate.getText();
   }

   public String getText(List<Locale> var1) {
      return this.asSingleLocale(var1).getText();
   }

   public String getText(Locale var1) {
      return this.getText(Arrays.asList(var1));
   }

   public Map<String, String> getTexts() {
      return Collections.unmodifiableMap(this.delegate.getData());
   }

   @Override
   public int hashCode() {
      return this.delegate.hashCode();
   }

   public boolean hasText(Locale var1) {
      String var2 = toLocaleId(var1);
      return this.hasText(var2);
   }

   public boolean hasText(String var1) {
      return this.delegate.hasText(var1);
   }

   public LocalizedText.Builder toBuilder() {
      LocalizedText.Builder var1 = builder();
      this.getTexts().forEach((var1x, var2) -> var1.setText(var2, var1x));
      return var1;
   }

   @Override
   public String toString() {
      return this.delegate.toString();
   }

   public static final class Builder {
      private final LinkedHashMap<String, String> data = new LinkedHashMap<>();

      public LocalizedText build() {
         this.data
            .forEach(
               (var1x, var2x) -> {
                  if (var1x == null || var2x == null) {
                     throw new IllegalStateException(
                        "LocalizedText.Builder doesn't support null values nor null keys, use empty String instead, data was: " + this.data
                     );
                  }
               }
            );
         if (this.data.size() <= 1) {
            LocalizedText var2 = this.data
               .entrySet()
               .stream()
               .findFirst()
               .map(var0 -> new LocalizedText(var0.getValue(), var0.getKey()))
               .orElse(LocalizedText.EMPTY);
            return LocalizedText.EMPTY.equals(var2) ? LocalizedText.EMPTY : var2;
         } else {
            LinkedHashMap var1 = new LinkedHashMap<>(this.data);
            return new LocalizedText(new LocalizedText.b(var1));
         }
      }

      public Map<String, String> getTexts() {
         return this.data;
      }

      public LocalizedText.Builder removeAll() {
         this.data.clear();
         return this;
      }

      public LocalizedText.Builder removeText(Locale var1) {
         if (var1 == null) {
            var1 = Locale.ROOT;
         }

         this.data.remove(LocalizedText.toLocaleId(var1));
         return this;
      }

      public LocalizedText.Builder removeText(String var1) {
         if (var1 == null) {
            var1 = "";
         }

         this.data.remove(var1);
         return this;
      }

      public LocalizedText.Builder setDefaultText(String var1) {
         this.implSet(var1, "");
         return this;
      }

      public LocalizedText.Builder setText(String var1, Locale var2) {
         this.implSet(var1, LocalizedText.toLocaleId(var2));
         return this;
      }

      public LocalizedText.Builder setText(String var1, String var2) {
         this.implSet(var1, var2);
         return this;
      }

      public LocalizedText.Builder setTexts(LocalizedText var1) {
         if (var1 == null) {
            var1 = LocalizedText.EMPTY;
         }

         this.data.putAll(var1.getTexts());
         return this;
      }

      private void implSet(String var1, String var2) {
         if (var2 == null) {
            var2 = "";
         }

         if (var1 == null) {
            var1 = "";
         }

         this.data.put(var2, var1);
      }

      LocalizedText.Builder cnD() {
         if ("".equals(this.data.get(""))) {
            this.data.clear();
            return this;
         } else {
            Iterator var1 = this.data.entrySet().iterator();

            while (var1.hasNext()) {
               Entry var2 = (Entry)var1.next();
               if ("".equals(var2.getValue())) {
                  var1.remove();
               }
            }

            return this;
         }
      }
   }

   private abstract static class a {
      private a() {
      }

      public abstract Map<String, String> getData();

      public abstract String getLocaleId();

      public abstract String getText();

      public abstract boolean hasText(String var1);

      public abstract LocalizedText.c h(List<Locale> var1);
   }

   private static class b extends LocalizedText.a {
      private final LinkedHashMap<String, String> data;

      private b(LinkedHashMap<String, String> var1) {
         this.data = var1;
      }

      @Override
      public boolean equals(Object var1) {
         if (this == var1) {
            return true;
         } else if (!(var1 instanceof LocalizedText.b)) {
            return false;
         } else {
            LocalizedText.b var2 = (LocalizedText.b)var1;
            return Objects.equals(this.data, var2.data);
         }
      }

      @Override
      public Map<String, String> getData() {
         return this.data;
      }

      @Override
      public String getLocaleId() {
         return this.data.containsKey("") ? "" : this.data.entrySet().stream().findFirst().map(var0 -> var0.getKey()).orElse(null);
      }

      @Override
      public String getText() {
         return this.data.containsKey("") ? this.data.get("") : this.data.entrySet().stream().findFirst().map(var0 -> var0.getValue()).orElse(null);
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.data);
      }

      @Override
      public boolean hasText(String var1) {
         return !StringUtils.isNullOrEmpty(var1) ? this.data.containsKey(var1) : this.data.containsKey(null) || this.data.containsKey("");
      }

      @Override
      public LocalizedText.c h(List<Locale> var1) {
         if (var1 == null) {
            var1 = Collections.emptyList();
         }

         for (Locale var3 : var1) {
            String var4 = var3 == null ? "" : LocalizedText.toLocaleId(var3);
            if (this.data.containsKey(var4)) {
               return new LocalizedText.c(var4, this.data.get(var4));
            }
         }

         for (Locale var9 : var1) {
            String var10 = var9 == null ? "" : var9.getLanguage();

            for (Entry var6 : this.data.entrySet()) {
               String var7 = var6.getKey() == null ? "" : LocalizedText.toLocale((String)var6.getKey()).getLanguage();
               if (Objects.equals(var10, var7)) {
                  return new LocalizedText.c(var6.getKey() == null ? "" : (String)var6.getKey(), (String)var6.getValue());
               }
            }
         }

         return new LocalizedText.c(this.getLocaleId(), this.getText());
      }

      @Override
      public String toString() {
         return "[texts=" + this.data + "]";
      }
   }

   private static class c extends LocalizedText.a {
      private final String rW;
      private final String text;

      private c(String var1, String var2) {
         this.rW = var1 == null ? "" : var1;
         this.text = var2 == null ? "" : var2;
      }

      @Override
      public boolean equals(Object var1) {
         if (this == var1) {
            return true;
         } else if (!(var1 instanceof LocalizedText.c)) {
            return false;
         } else {
            LocalizedText.c var2 = (LocalizedText.c)var1;
            return Objects.equals(this.rW, var2.rW) && Objects.equals(this.text, var2.text);
         }
      }

      @Override
      public Map<String, String> getData() {
         HashMap var1 = new HashMap();
         var1.put(this.rW, this.text);
         return var1;
      }

      @Override
      public String getLocaleId() {
         return this.rW;
      }

      @Override
      public String getText() {
         return this.text;
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.rW, this.text);
      }

      @Override
      public boolean hasText(String var1) {
         return StringUtils.isNullOrEmpty(var1) ? StringUtils.isNullOrEmpty(var1) : var1.equals(this.rW);
      }

      @Override
      public LocalizedText.c h(List<Locale> var1) {
         return this;
      }

      @Override
      public String toString() {
         String var1 = this.getLocaleId();
         if (var1 != null && !var1.isEmpty()) {
            return "(" + this.getLocaleId() + ") " + this.getText();
         } else {
            return this.text == null ? "" : this.text;
         }
      }
   }
}
