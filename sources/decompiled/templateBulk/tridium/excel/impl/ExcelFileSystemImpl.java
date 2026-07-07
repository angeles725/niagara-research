package com.tridium.excel.impl;

import com.tridium.excel.ExcelFileSystem;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

public class ExcelFileSystemImpl implements ExcelFileSystem {
   final POIFSFileSystem fileSystem;

   ExcelFileSystemImpl(POIFSFileSystem fileSystem) {
      this.fileSystem = fileSystem;
   }

   public void writeFilesystem(OutputStream stream) throws IOException {
      this.fileSystem.writeFilesystem(stream);
   }

   @Override
   public String toString() {
      return this.fileSystem.toString();
   }
}
