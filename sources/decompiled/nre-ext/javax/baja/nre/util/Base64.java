package javax.baja.nre.util;

import java.io.ByteArrayOutputStream;

@Deprecated
public class Base64 {
   private static final char[] encodeTable = new char[]{
      'A',
      'B',
      'C',
      'D',
      'E',
      'F',
      'G',
      'H',
      'I',
      'J',
      'K',
      'L',
      'M',
      'N',
      'O',
      'P',
      'Q',
      'R',
      'S',
      'T',
      'U',
      'V',
      'W',
      'X',
      'Y',
      'Z',
      'a',
      'b',
      'c',
      'd',
      'e',
      'f',
      'g',
      'h',
      'i',
      'j',
      'k',
      'l',
      'm',
      'n',
      'o',
      'p',
      'q',
      'r',
      's',
      't',
      'u',
      'v',
      'w',
      'x',
      'y',
      'z',
      '0',
      '1',
      '2',
      '3',
      '4',
      '5',
      '6',
      '7',
      '8',
      '9',
      '+',
      '/'
   };
   private static final char PAD = '=';
   public static final int HASHLEN = 16;
   public static final int HASHHEXLEN = 32;

   public static String encode(String s) {
      return encode(s.getBytes());
   }

   public static String encode(byte[] buf) {
      return encode(buf, -1);
   }

   public static String encode(byte[] buf, int linelen) {
      int pos = 0;
      StringBuilder sbuf = new StringBuilder((int)(buf.length * 1.33));
      int bytesRemaining = buf.length;

      int index;
      for (index = 0; bytesRemaining >= 3; index += 3) {
         int i0 = (255 & buf[index]) >> 2;
         int i1 = ((255 & buf[index] & 3) << 4) + ((255 & buf[index + 1]) >> 4);
         int i2 = ((15 & buf[index + 1]) << 2) + ((255 & buf[index + 2]) >> 6);
         int i3 = 255 & buf[index + 2] & 63;
         pos = append(sbuf, encodeTable[i0], linelen, pos);
         pos = append(sbuf, encodeTable[i1], linelen, pos);
         pos = append(sbuf, encodeTable[i2], linelen, pos);
         pos = append(sbuf, encodeTable[i3], linelen, pos);
         bytesRemaining -= 3;
      }

      if (bytesRemaining > 0) {
         byte[] remainder = new byte[3];

         for (int i = 0; i < bytesRemaining; i++) {
            remainder[i] = buf[index + i];
         }

         for (int i = bytesRemaining; i < 3; i++) {
            remainder[i] = 0;
         }

         int lastOut = bytesRemaining == 1 ? 2 : 3;
         int i0 = (255 & remainder[0]) >> 2;
         int i1 = ((255 & remainder[0] & 3) << 4) + ((255 & remainder[1]) >> 4);
         int i2 = ((15 & remainder[1]) << 2) + ((255 & remainder[2]) >> 6);
         pos = append(sbuf, encodeTable[i0], linelen, pos);
         if (lastOut > 1) {
            pos = append(sbuf, encodeTable[i1], linelen, pos);
         }

         if (lastOut > 2) {
            pos = append(sbuf, encodeTable[i2], linelen, pos);
         }

         int padCount = 4 - lastOut;

         for (int i = 0; i < padCount; i++) {
            pos = append(sbuf, '=', linelen, pos);
         }
      }

      return sbuf.toString();
   }

   private static final int append(StringBuilder sbuf, char ch, int linelen, int pos) {
      if (linelen != -1) {
         if (pos == linelen) {
            sbuf.append('\n');
            pos = 0;
         }

         pos++;
      }

      sbuf.append(ch);
      return pos;
   }

   public static byte[] decode(String src) {
      int bits = src.length() * 6;
      ByteArrayOutputStream res = new ByteArrayOutputStream(bits / 8);
      int index = 0;

      for (int bytesRemaining = src.length(); bytesRemaining >= 4; index += 4) {
         int val0 = getVal(src.charAt(index));

         while (val0 == -2 && bytesRemaining > 0) {
            index++;
            if (--bytesRemaining > 0) {
               val0 = getVal(src.charAt(index));
            }
         }

         if (bytesRemaining == 0) {
            throw new IllegalStateException("Unexpected end of input.");
         }

         int val1 = getVal(src.charAt(index + 1));

         while (val1 == -2 && bytesRemaining > 0) {
            index++;
            if (--bytesRemaining > 0) {
               val1 = getVal(src.charAt(index + 1));
            }
         }

         if (bytesRemaining == 0) {
            throw new IllegalStateException("Unexpected end of input.");
         }

         int val2 = getVal(src.charAt(index + 2));

         while (val2 == -2 && bytesRemaining > 0) {
            index++;
            if (--bytesRemaining > 0) {
               val2 = getVal(src.charAt(index + 2));
            }
         }

         if (bytesRemaining == 0) {
            throw new IllegalStateException("Unexpected end of input.");
         }

         int val3 = getVal(src.charAt(index + 3));

         while (val3 == -2 && bytesRemaining > 0) {
            index++;
            if (--bytesRemaining > 0) {
               val3 = getVal(src.charAt(index + 3));
            }
         }

         if (bytesRemaining == 0) {
            throw new IllegalStateException("Unexpected end of input.");
         }

         int group = 0;
         int padCount = 0;
         if (val0 != -1) {
            group |= val0 << 18;
         } else {
            padCount++;
         }

         if (val1 != -1) {
            group |= val1 << 12;
         } else {
            padCount++;
         }

         if (val2 != -1) {
            group |= val2 << 6;
         } else {
            padCount++;
         }

         if (val3 != -1) {
            group |= val3;
         } else {
            padCount++;
         }

         res.write((group & 0xFF0000) >> 16);
         if (val2 != -1) {
            res.write((group & 0xFF00) >> 8);
            if (val3 != -1) {
               res.write(group & 0xFF);
            }
         }

         if (padCount > 0) {
            bytesRemaining = 0;
         } else {
            bytesRemaining -= 4;
         }
      }

      return res.toByteArray();
   }

   public static String decodeToString(String s) {
      return new String(decode(s));
   }

   private static int getVal(char ch) {
      if (ch == '=') {
         return -1;
      } else {
         int val = ch;
         if (val >= 65 && val <= 90) {
            return val - 65;
         } else if (val >= 97 && val <= 122) {
            return val - 71;
         } else if (val >= 48 && val <= 57) {
            return val + 4;
         } else if (val == 43) {
            return 62;
         } else {
            return val == 47 ? 63 : -2;
         }
      }
   }
}
