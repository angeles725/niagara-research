package com.tridium.nre.security;

import java.security.AccessController;
import javax.baja.xml.XElem;
import javax.baja.xml.XException;

public class PasswordStrength {
   public static final int DEFAULT_MINIMUM_LENGTH = AccessController.doPrivileged(
      () -> SecurityInitializer.getInstance().getSecurityInfoProvider().getDefaultMinimumPasswordLength()
   );
   public static final int MINIMUM_ALLOWED_LENGTH = AccessController.doPrivileged(
      () -> SecurityInitializer.getInstance().isFips() ? SecurityInitializer.getInstance().getSecurityInfoProvider().getDefaultMinimumPasswordLength() : 0
   );
   public static final int DEFAULT_MAXIMUM_LENGTH = AccessController.doPrivileged(
      () -> SecurityInitializer.getInstance().getSecurityInfoProvider().getDefaultMaximumPasswordLength()
   );
   private static final String DEFAULT_MAX_VALUE_STRING = String.valueOf(DEFAULT_MAXIMUM_LENGTH);
   public static final PasswordStrength DEFAULT = new PasswordStrength(DEFAULT_MINIMUM_LENGTH, 1, 1, 1, 0, DEFAULT_MAXIMUM_LENGTH);
   public static final PasswordStrength FIPS_1 = new PasswordStrength(14, 1, 1, 1, 0, DEFAULT_MAXIMUM_LENGTH);
   private final int minimumLength;
   private final int minimumLowerCase;
   private final int minimumUpperCase;
   private final int minimumDigits;
   private final int minimumSpecial;
   private final int maximumLength;

   public PasswordStrength(int minimumLength, int minimumLowerCase, int minimumUpperCase, int minimumDigits, int minimumSpecial) {
      this(minimumLength, minimumLowerCase, minimumUpperCase, minimumDigits, minimumSpecial, DEFAULT_MAXIMUM_LENGTH);
   }

   public PasswordStrength(int minimumLength, int minimumLowerCase, int minimumUpperCase, int minimumDigits, int minimumSpecial, int maximumLength) {
      if (minimumLength < 0) {
         throw new IllegalArgumentException("Minimum password length cannot be less than 0");
      }

      if (minimumLowerCase < 0) {
         throw new IllegalArgumentException("Minimum lowercase password characters cannot be less than 0");
      }

      if (minimumUpperCase < 0) {
         throw new IllegalArgumentException("Minimum uppercase password characters cannot be less than 0");
      }

      if (minimumDigits < 0) {
         throw new IllegalArgumentException("Minimum password digits cannot be less than 0");
      }

      if (minimumSpecial < 0) {
         throw new IllegalArgumentException("Minimum special password characters cannot be less than 0");
      }

      if (maximumLength < 1) {
         throw new IllegalArgumentException("Maximum password length cannot be less than 1");
      }

      if (minimumLength > maximumLength) {
         throw new IllegalArgumentException("Minimum password length exceeds maximum password length");
      }

      if (minimumLowerCase + minimumUpperCase + minimumDigits + minimumSpecial > maximumLength) {
         throw new IllegalArgumentException(
            "Maximum password length cannot be less than the combined minimum lowercase, uppercase, digit, and special characters"
         );
      }

      this.minimumLength = minimumLength;
      this.minimumLowerCase = minimumLowerCase;
      this.minimumUpperCase = minimumUpperCase;
      this.minimumDigits = minimumDigits;
      this.minimumSpecial = minimumSpecial;
      this.maximumLength = maximumLength;
   }

   public static PasswordStrength makeFromXElem(XElem elem) {
      try {
         return new PasswordStrength(
            Integer.parseInt(elem.get("minimumLength")),
            Integer.parseInt(elem.get("minimumLowerCase")),
            Integer.parseInt(elem.get("minimumUpperCase")),
            Integer.parseInt(elem.get("minimumDigits")),
            Integer.parseInt(elem.get("minimumSpecial")),
            Integer.parseInt(elem.get("maximumLength", DEFAULT_MAX_VALUE_STRING))
         );
      } catch (XException e) {
         throw new IllegalArgumentException("Malformed PasswordStrength XElem", e);
      }
   }

   public int getMinimumLength() {
      return this.minimumLength;
   }

   public int getMinimumLowerCase() {
      return this.minimumLowerCase;
   }

   public int getMinimumUpperCase() {
      return this.minimumUpperCase;
   }

   public int getMinimumDigits() {
      return this.minimumDigits;
   }

   public int getMinimumSpecial() {
      return this.minimumSpecial;
   }

   public int getMaximumLength() {
      return this.maximumLength;
   }
}
