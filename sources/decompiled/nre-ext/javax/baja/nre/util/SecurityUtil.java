package javax.baja.nre.util;

import java.io.File;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Date;

public final class SecurityUtil {
   private static final int BLOCK_SIZE = 64;
   private static final String STRING_BLOCK = "        ";
   private static final byte[] BYTE_BLOCK = new byte[]{32, 32, 32, 32, 32, 32, 32, 32};
   private static final char[] CHAR_BLOCK = new char[]{' ', ' ', ' ', ' ', ' ', ' ', ' ', ' '};
   private static final char[] HEX_CHARS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
   private static final String UNKNOWN_SESSION_ID = "unknown-session";
   private static final byte[] SALT_BYTES = new byte[16];

   private SecurityUtil() {
   }

   public static boolean equals(String a, String b) {
      if (a == b) {
         return true;
      }

      if (a != null && b != null) {
         int match = a.length() == b.length() ? 0 : 1;
         a = a.length() == 0 ? "        " : a;
         b = b.length() == 0 ? "        " : b;
         int length = (Math.max(a.length(), b.length()) / 64 + 1) * 64;

         for (int i = 0; i < length; i++) {
            match |= a.charAt(i % a.length()) ^ b.charAt(i % b.length());
         }

         return match == 0;
      } else {
         return false;
      }
   }

   public static boolean equals(char[] a, char[] b) {
      if (a == b) {
         return true;
      }

      if (a != null && b != null) {
         int match = a.length == b.length ? 0 : 1;
         a = a.length == 0 ? CHAR_BLOCK : a;
         b = b.length == 0 ? CHAR_BLOCK : b;
         int length = (Math.max(a.length, b.length) / 64 + 1) * 64;

         for (int i = 0; i < length; i++) {
            match |= a[i % a.length] ^ b[i % b.length];
         }

         return match == 0;
      } else {
         return false;
      }
   }

   public static boolean equals(byte[] a, byte[] b) {
      if (a == b) {
         return true;
      }

      if (a != null && b != null) {
         int match = a.length == b.length ? 0 : 1;
         a = a.length == 0 ? BYTE_BLOCK : a;
         b = b.length == 0 ? BYTE_BLOCK : b;
         int length = (Math.max(a.length, b.length) / 64 + 1) * 64;

         for (int i = 0; i < length; i++) {
            match |= a[i % a.length] ^ b[i % b.length];
         }

         return match == 0;
      } else {
         return false;
      }
   }

   public static void zeroByteArray(byte[] bytes) {
      if (bytes != null) {
         Arrays.fill(bytes, (byte)0);
      }
   }

   public static void zeroCharArray(char[] chars) {
      if (chars != null) {
         Arrays.fill(chars, '\u0000');
      }
   }

   public static char[] toHexChars(byte[] bytes) {
      CharBuffer buf = CharBuffer.allocate(bytes.length * 2);

      for (byte aByte : bytes) {
         int val = aByte & 255;
         int high = val % 16 & 0xFF;
         val /= 16;
         int low = val % 16 & 0xFF;
         buf.put(HEX_CHARS[low]);
         buf.put(HEX_CHARS[high]);
      }

      return Arrays.copyOf(buf.array(), buf.position());
   }

   public static byte[] toBytesFromUTF8Chars(char[] chars) {
      CharBuffer charBuffer = CharBuffer.wrap(chars);
      java.nio.ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
      byte[] bytes = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
      Arrays.fill(charBuffer.array(), '\u0000');
      Arrays.fill(byteBuffer.array(), (byte)0);
      return bytes;
   }

   public static char[] toUTF8CharsFromBytes(byte[] bytes) {
      java.nio.ByteBuffer byteBuffer = java.nio.ByteBuffer.wrap(bytes);
      CharBuffer charBuffer = Charset.forName("UTF-8").decode(byteBuffer);
      char[] chars = Arrays.copyOfRange(charBuffer.array(), charBuffer.position(), charBuffer.limit());
      Arrays.fill(charBuffer.array(), '\u0000');
      Arrays.fill(byteBuffer.array(), (byte)0);
      return chars;
   }

   public static byte[] fromHexChars(char[] chars) {
      java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocate(chars.length / 2);

      for (char aChar : chars) {
         int x = aChar - '0';
         if (x >= 0 && x <= 9) {
            buf.put((byte)x);
         } else {
            x = aChar - 'A' + 10;
            if (x >= 10 && x <= 15) {
               buf.put((byte)x);
            } else {
               x = aChar - 'a' + 10;
               if (x < 10 || x > 15) {
                  throw new IllegalArgumentException("'" + aChar + "'");
               }

               buf.put((byte)x);
            }
         }
      }

      return Arrays.copyOf(buf.array(), buf.position());
   }

   public static File resolveChrootPath(File chrootFile, File relativePathFile) {
      Path chroot = Paths.get(chrootFile.toString());
      Path relativePath = Paths.get(relativePathFile.toString());
      Path full = chroot.resolve(relativePath).normalize();
      if (full.startsWith(chroot)) {
         return full.toFile();
      } else {
         throw new IllegalArgumentException("invalid path resolution: " + relativePathFile);
      }
   }

   public static File resolveChrootPath(File chrootFile, String relativePathFile) {
      Path chroot = Paths.get(chrootFile.toString());
      Path relativePath = Paths.get(relativePathFile);
      Path full = chroot.resolve(relativePath).normalize();
      if (full.startsWith(chroot)) {
         return full.toFile();
      } else {
         throw new IllegalArgumentException("invalid path resolution: " + relativePathFile);
      }
   }

   public static File resolveChrootPath(String chrootFile, String relativePathFile) {
      Path chroot = Paths.get(chrootFile);
      Path relativePath = Paths.get(relativePathFile);
      Path full = chroot.resolve(relativePath).normalize();
      if (full.startsWith(chroot)) {
         return full.toFile();
      } else {
         throw new IllegalArgumentException("invalid path resolution: " + relativePathFile);
      }
   }

   public static String calculateSessionIdHash(String sessionId) {
      if (sessionId == null) {
         return "unknown-session";
      }

      try {
         MessageDigest digest = MessageDigest.getInstance("SHA-256");
         digest.update(SALT_BYTES);
         byte[] hash = digest.digest(sessionId.getBytes(StandardCharsets.UTF_8));
         return TextUtil.bytesToHexString(hash);
      } catch (NoSuchAlgorithmException nsae) {
         System.err.println("WARNING [" + new Date() + "][nre] failed to calculate session id hash");
         nsae.printStackTrace();
         return "unknown-session";
      }
   }

   static {
      new SecureRandom().nextBytes(SALT_BYTES);
   }
}
