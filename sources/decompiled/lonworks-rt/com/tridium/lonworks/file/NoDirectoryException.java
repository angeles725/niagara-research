package com.tridium.lonworks.file;

import javax.baja.lonworks.LonException;

public class NoDirectoryException extends LonException {
   public NoDirectoryException() {
      super("No file directory", null);
   }
}
