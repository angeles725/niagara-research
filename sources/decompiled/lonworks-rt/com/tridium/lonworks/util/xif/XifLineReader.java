package com.tridium.lonworks.util.xif;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

public class XifLineReader extends LineNumberReader {
   public XifLineReader(String filename) throws FileNotFoundException {
      super(new FileReader(filename));
   }

   public XifLineReader(InputStream in) {
      super(new InputStreamReader(in));
   }

   public String readXifLine() throws IOException {
      return this.readXifLine(true);
   }

   public String readXifLine(boolean skipBlanks) throws IOException {
      String line;
      do {
         line = super.readLine();
         if (line == null) {
            throw new EOFException();
         }

         line = line.trim();
      } while (skipBlanks && line.equals("") || line.startsWith("#"));

      return line;
   }
}
