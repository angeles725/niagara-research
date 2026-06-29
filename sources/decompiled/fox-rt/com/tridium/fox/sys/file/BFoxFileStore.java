package com.tridium.fox.sys.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.baja.file.BAbstractFileStore;
import javax.baja.file.BIFile;
import javax.baja.file.FilePath;
import javax.baja.io.BajaIOException;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.BPermissions;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BFoxFileStore extends BAbstractFileStore {
   public static final Type TYPE = Sys.loadType(BFoxFileStore.class);
   BAbsTime modified;
   boolean isDirectory;
   boolean isReadonly;
   long size;
   int hashCode;
   BPermissions permissions;
   long crc = -1L;

   public Type getType() {
      return TYPE;
   }

   public BFoxFileStore(BFoxFileSpace space, FilePath path) {
      super(space, path);
   }

   public boolean isDirectory() {
      return this.isDirectory;
   }

   public boolean isReadonly() {
      return this.isReadonly;
   }

   public long getSize() {
      return this.size;
   }

   public BAbsTime getLastModified() {
      return this.modified;
   }

   protected boolean doSetLastModified(BAbsTime absTime) throws IOException {
      try {
         if (this.channel().setLastModified(this, absTime)) {
            this.modified = absTime;
            return true;
         } else {
            return false;
         }
      } catch (Exception var3) {
         throw new IOException("Cannot update setLastModified: " + this.getFilePath());
      }
   }

   public BPermissions getPermissions(BIFile file, Context cx) {
      return this.permissions;
   }

   public int hashCode() {
      return this.hashCode;
   }

   public boolean equals(Object object) {
      if (!(object instanceof BFoxFileStore)) {
         return false;
      } else {
         BFoxFileStore x = (BFoxFileStore)object;
         return this.getFileSpace() == x.getFileSpace() && this.getFilePath().equals(x.getFilePath());
      }
   }

   public InputStream getInputStream() throws IOException {
      try {
         return this.channel().read(this);
      } catch (IOException var2) {
         throw var2;
      } catch (Exception var3) {
         throw new BajaIOException(var3);
      }
   }

   public OutputStream getOutputStream() throws IOException {
      try {
         return this.channel().write(this);
      } catch (IOException var2) {
         throw var2;
      } catch (Exception var3) {
         throw new BajaIOException(var3);
      }
   }

   public long getCrc() throws IOException {
      if (this.crc == -1L && !this.isDirectory()) {
         try {
            this.crc = this.channel().getCrc((BFoxFileSpace)this.getFileSpace(), this.getFilePath());
         } catch (Exception var2) {
            throw new IOException("Cannot get CRC: " + this.getFilePath());
         }
      }

      return this.crc;
   }

   public BFileChannel channel() {
      return ((BFoxFileSpace)this.getFileSpace()).channel();
   }
}
