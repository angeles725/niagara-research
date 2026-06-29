package com.tridium.lonworks.util.xif;

import com.tridium.lonworks.xml.XElementQualifier;
import com.tridium.lonworks.xml.XNetVariable;
import java.io.PrintStream;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

public class ElementParser {
   private int byt = 0;
   private int bit = 0;
   private PrintStream out;

   public ElementParser(PrintStream out) {
      this.out = out;
   }

   public void parse(String line, XNetVariable netVariable, int ndx) {
      String token = "";
      StringTokenizer st = new StringTokenizer(line);

      try {
         int type = Integer.parseInt(token = st.nextToken());
         int offset = Integer.parseInt(token = st.nextToken());
         int size = Integer.parseInt(token = st.nextToken());
         int signedFlag = Integer.parseInt(token = st.nextToken());
         int arraySize = Integer.parseInt(token = st.nextToken());
         if (arraySize == 0) {
            arraySize = 1;
         }

         for (int i = 0; i < arraySize; i++) {
            XElementQualifier eq = new XElementQualifier();
            switch (type) {
               case 0:
                  eq.setElementType("c8");
                  this.addBytes(1);
                  break;
               case 1:
                  eq.setElementType(signedFlag == 0 ? "u8" : "s8");
                  this.addBytes(1);
                  break;
               case 2:
                  eq.setElementType(signedFlag == 0 ? "u16" : "s16");
                  this.addBytes(2);
                  break;
               case 3:
                  eq.setElementType("ub");
                  if (offset < this.bit) {
                     this.addBytes(1);
                     this.bit = offset;
                  }

                  if (offset > this.bit) {
                     this.bit = offset;
                  }

                  eq.setByteOffset(this.byt);
                  eq.setBitOffset(this.bit);
                  eq.setLength(size);
                  this.addBits(size);
                  break;
               default:
                  eq.setElementType("na");
                  this.addBytes(size);
                  eq.setLength(size);
            }

            eq.name = "element" + ndx + (arraySize > 1 ? "_" + i : "");
            netVariable.addAttribute(eq.name, eq);
         }
      } catch (NumberFormatException var13) {
         this.out.println("\"" + token + "\" is not a number\n" + line + "\n" + netVariable);
      } catch (NoSuchElementException var14) {
         this.out.println("missing fields in an element definition");
      } catch (Throwable var15) {
         var15.printStackTrace();
      }
   }

   private void addBytes(int amount) {
      this.byt += amount;
      this.bit = 0;
   }

   private void addBits(int amount) {
      this.byt = this.byt + (this.bit + amount) / 8;
      this.bit = (this.bit + amount) % 8;
   }
}
