package com.tridium.lonworks.util.xif;

import java.io.LineNumberReader;

public class WrappingPrinter {
   private LineNumberReader reader;
   private int lineSize;
   private int lineIndent = 5;

   public WrappingPrinter() {
   }

   public WrappingPrinter(LineNumberReader reader) {
      this.reader = reader;
   }

   public WrappingPrinter(LineNumberReader reader, int lineSize, int lineIndent) {
      this.reader = reader;
      this.lineSize = lineSize;
      this.lineIndent = lineIndent;
   }

   public void setReader(LineNumberReader reader) {
      this.reader = reader;
   }

   public void setSize(int lineSize) {
      this.lineSize = lineSize;
   }

   public void setLength(int lineSize) {
      this.lineSize = lineSize;
   }

   public void setIndent(int lineIndent) {
      this.lineIndent = lineIndent;
   }

   public void print(String message) {
      print(message, this.lineSize, this.lineIndent);
   }

   public static void print(String message, int len, int indent) {
      if (len == 0) {
         System.out.println(message);
      } else {
         while (message.length() > len) {
            String print = message.substring(0, len - 1);
            int index = print.lastIndexOf(" ");
            if (index > indent) {
               message = message.substring(index + 1);
               print = print.substring(0, index);
            } else {
               message = message.substring(len - 1);
            }

            System.out.println(print);

            for (int i = 0; i < indent; i++) {
               message = " " + message;
            }
         }

         System.out.println(message);
      }
   }

   public void printErrorMessage(String message) {
      if (this.reader != null) {
         print("ERROR ln " + this.reader.getLineNumber() + ": " + message, this.lineSize, this.lineIndent);
      } else {
         print("ERROR: " + message, this.lineSize, this.lineIndent);
      }
   }

   public void printErrorMessage(String message, int size, int indent) {
      if (this.reader != null) {
         print("ERROR ln " + this.reader.getLineNumber() + ": " + message, size, indent);
      } else {
         print("ERROR: " + message, size, indent);
      }
   }
}
