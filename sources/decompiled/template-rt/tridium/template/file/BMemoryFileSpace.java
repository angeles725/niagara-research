package com.tridium.template.file;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;
import javax.baja.file.BDataFile;
import javax.baja.file.BDirectory;
import javax.baja.file.BFileSpace;
import javax.baja.file.BIFile;
import javax.baja.file.BIFileStore;
import javax.baja.file.BMemoryFileStore;
import javax.baja.file.FilePath;
import javax.baja.naming.BHost;
import javax.baja.naming.BISession;
import javax.baja.naming.BLocalHost;
import javax.baja.naming.BOrd;
import javax.baja.naming.UnresolvedException;
import javax.baja.nre.annotations.NiagaraSingleton;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.registry.TypeInfo;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraSingleton
public class BMemoryFileSpace extends BFileSpace {
   public static final BMemoryFileSpace INSTANCE = new BMemoryFileSpace();
   public static final Type TYPE = Sys.loadType(BMemoryFileSpace.class);
   private static HashMap<String, BMemoryFileStore> cache = new HashMap<>();
   int handle = 1;
   private static final BMemoryFileSpace.MemoryMap map = new BMemoryFileSpace.MemoryMap();
   private static final HashMap<String, String> aliasMap = new HashMap<>();
   static final Logger log = Logger.getLogger("sys.file");
   static ThreadLocal<Context> threadLocalContext = new ThreadLocal<>();
   static BIFile MEM_NO_FILE = new BDataFile(new BMemoryFileStore(INSTANCE, new FilePath("")));
   BOrd ABS_ORD = BOrd.make("memory:");
   private static final BIFile[] EMPTY = new BIFile[0];
   private BOrd ordInHost;

   public Type getType() {
      return TYPE;
   }

   public BMemoryFileStore makeMemoryStore(FilePath filePath) {
      return this.makeMemoryStore(filePath, null);
   }

   public BMemoryFileStore makeMemoryStore(FilePath filePath, FilePath aliasFilePath) {
      return this.makeMemoryStore(new MemoryPath(filePath), aliasFilePath == null ? null : new MemoryPath(aliasFilePath));
   }

   public BMemoryFileStore makeMemoryStore(MemoryPath memoryPath) {
      return this.makeMemoryStore(memoryPath, null);
   }

   public BMemoryFileStore makeMemoryStore(MemoryPath memoryPath, MemoryPath aliasMemoryPath) {
      return this.makeMemoryStore(memoryPath.getBody(), aliasMemoryPath == null ? null : aliasMemoryPath.getBody());
   }

   public BMemoryFileStore makeMemoryStore(String filePath) {
      return this.makeMemoryStore(filePath, null);
   }

   public BMemoryFileStore makeMemoryStore(String filePath, String aliasPath) {
      MemoryPath path = MemoryPath.make(filePath);
      BMemoryFileStore fs = new BMemoryFileStore(this, path);
      String pathKey = path.getBody();
      map.put(pathKey, fs);
      if (aliasPath != null) {
         MemoryPath alias = MemoryPath.make(aliasPath);
         String aliasKey = alias.getBody();
         if (!aliasKey.equals(pathKey)) {
            aliasMap.put(aliasKey, pathKey);
         }
      }

      return fs;
   }

   private String getNextHandle() {
      return Integer.toString(this.handle++, 16);
   }

   public void delete(FilePath fp, Context cx) throws IOException {
      if (fp instanceof MemoryPath) {
         String key = fp.getBody();
         map.remove(key);

         boolean aliasRemoved;
         do {
            aliasRemoved = aliasMap.values().remove(key);
         } while (aliasRemoved);
      }
   }

   public BIFile findFile(FilePath path) {
      if (!(path instanceof MemoryPath)) {
         return MEM_NO_FILE;
      } else {
         BMemoryFileStore store = (BMemoryFileStore)this.findStore(path);
         if (store == null) {
            return MEM_NO_FILE;
         } else {
            String extension = store.getExtension();
            if (extension != null && !extension.isEmpty()) {
               TypeInfo type = Sys.getRegistry().getFileTypeForExtension(extension);

               try {
                  BIFile file = (BIFile)type.getInstance();
                  file.setStore(store);
                  return file;
               } catch (Exception var6) {
                  return MEM_NO_FILE;
               }
            } else {
               return new BDataFile(store);
            }
         }
      }
   }

   private BMemoryFileSpace() {
      super("memory");
   }

   public BOrd getAbsoluteOrd() {
      return this.ABS_ORD;
   }

   public BDirectory makeDir(FilePath path, Context cx) throws IOException {
      return null;
   }

   public BIFile makeFile(FilePath path, Context cx) throws IOException {
      return null;
   }

   public void move(FilePath from, FilePath to, Context cx) throws IOException {
   }

   public BIFile[] listFiles() {
      return null;
   }

   public FilePath localFileToPath(File file) {
      try {
         String path = file.getCanonicalPath();
         path = path.replace('\\', '/');
         if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
         }

         if (!path.startsWith("/")) {
            path = "/" + path;
         }

         return new FilePath(path);
      } catch (Exception var3) {
         throw new UnresolvedException("Cannot map to path: " + file, var3);
      }
   }

   public BIFileStore findStore(FilePath path) {
      String key = path.getBody();
      BIFileStore result = (BIFileStore)map.get(key);
      if (result == null) {
         result = (BIFileStore)map.get(aliasMap.get(key));
      }

      return result;
   }

   public BIFile getChild(BIFile dir, String name) {
      return null;
   }

   public BIFile[] getChildren(BIFile dir) {
      return EMPTY;
   }

   public boolean hasNavChildren() {
      return true;
   }

   public BOrd getOrdInHost() {
      return this.ABS_ORD;
   }

   public BOrd getOrdInSession() {
      return this.ABS_ORD;
   }

   public BHost getHost() {
      return BLocalHost.INSTANCE;
   }

   public BISession getSession() {
      return BLocalHost.INSTANCE;
   }

   public void spy(SpyWriter out) throws Exception {
      try {
         threadLocalContext.set(out.getContext());
         super.spy(out);
         out.startProps("BMemoryFileSpace");
         out.prop("ordInHost", this.ordInHost);
         out.endProps();
         out.startTable(true);
         out.trTitle("cache", 4);
         out.w("<tr>").th("key").th("name").th("modified").w("</tr>\n");
         Object[] keys = cache.keySet().toArray();

         for (Object key : keys) {
            BMemoryFileStore fs = cache.get(key);
            out.tr(key, fs.getFileName(), fs.getLastModified());
         }

         out.endTable();
      } finally {
         threadLocalContext.set(null);
      }
   }

   static {
      BLocalHost.INSTANCE.addNavChild(INSTANCE);
   }

   private static final class MemoryMap extends HashMap<String, BMemoryFileStore> {
      private MemoryMap() {
      }
   }
}
