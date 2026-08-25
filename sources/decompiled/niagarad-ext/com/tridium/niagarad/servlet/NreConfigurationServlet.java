package com.tridium.niagarad.servlet;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.http.Http;
import com.tridium.niagarad.log.ErrorHandler;
import com.tridium.niagarad.log.MessageBundle;
import com.tridium.niagarad.util.CsrfTokenUtil;
import com.tridium.niagarad.util.DaemonAuthUtil;
import com.tridium.niagarad.util.KeyedList;
import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.platform.NREMemoryPool;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.xml.XWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class NreConfigurationServlet extends DaemonServlet {
   private static final int _HEAP_SPACE = 0;
   private static final int _CODE_CACHE = 1;
   private static final int _META_SPACE = 2;
   private static final int _RAM_DISK = 3;
   private static final int _SYSTEM_RESERVE = 4;
   private volatile boolean defaultSettings = true;
   private final IPlatformProvider platformProvider;
   private int totalNREMemorySize = 0;
   private Logger filter;
   private static final String HEAP_SPACE_SIZE_FILEPATH = NiagaraDaemon.NIAGARA_USER_HOME_DAEMON_PATH + File.separator + "nreHeapSpaceSize";
   private static final String META_SPACE_SIZE_FILEPATH = NiagaraDaemon.NIAGARA_USER_HOME_DAEMON_PATH + File.separator + "nreMetaSpaceSize";
   private static final String RAM_DISK_SIZE_FILEPATH = NiagaraDaemon.NIAGARA_USER_HOME_DAEMON_PATH + File.separator + "nreRamDiskSize";
   private static final String CODE_CACHE_SIZE_FILEPATH = NiagaraDaemon.NIAGARA_USER_HOME_DAEMON_PATH + File.separator + "nreCodeCacheSize";
   private static final String SYSTEM_RESERVE_SIZE_FILEPATH = NiagaraDaemon.NIAGARA_USER_HOME_DAEMON_PATH + File.separator + "nreSystemReserveSize";

   public NreConfigurationServlet(IPlatformProvider platformProvider) {
      super("nreconfig");
      this.platformProvider = platformProvider;
   }

   @Override
   public synchronized boolean doStart() {
      this.filter = Logger.getLogger("nreconfig");
      File heapFile = new File(HEAP_SPACE_SIZE_FILEPATH);
      File metaFile = new File(META_SPACE_SIZE_FILEPATH);
      File ramFile = new File(RAM_DISK_SIZE_FILEPATH);
      File codeFile = new File(CODE_CACHE_SIZE_FILEPATH);
      File systemFile = new File(SYSTEM_RESERVE_SIZE_FILEPATH);
      NREMemoryPool heapSpacePool = this.getMemoryPool(0, false);
      NREMemoryPool metaSpacePool = this.getMemoryPool(2, false);
      NREMemoryPool ramDiskPool = this.getMemoryPool(3, false);
      NREMemoryPool codeCachePool = this.getMemoryPool(1, false);
      NREMemoryPool systemReservePool = this.getMemoryPool(4, false);
      this.totalNREMemorySize = heapSpacePool.getDefaultSize()
         + metaSpacePool.getDefaultSize()
         + ramDiskPool.getDefaultSize()
         + codeCachePool.getDefaultSize()
         + systemReservePool.getDefaultSize();
      boolean validConfigurationFound = false;
      if (heapFile.exists() && metaFile.exists() && ramFile.exists() && codeFile.exists() && systemFile.exists()) {
         try {
            int currentHeapSpaceValue = readFileValue(heapFile);
            int currentMetaSpaceValue = readFileValue(metaFile);
            int currentRamDiskValue = readFileValue(ramFile);
            int currentCodeCacheValue = readFileValue(codeFile);
            int currentSystemReserveValue = readFileValue(systemFile);
            int totalPoolSizes = currentHeapSpaceValue + currentMetaSpaceValue + currentRamDiskValue + currentCodeCacheValue + currentSystemReserveValue;
            if (totalPoolSizes > this.getTotalNREMemoryPool()) {
               this.filter.severe("invalid nre memory configuration values, " + totalPoolSizes + " > " + this.getTotalNREMemoryPool());
            } else if (this.validateRestrictions(null, heapSpacePool, currentHeapSpaceValue, "heapSpacePool")
               && this.validateRestrictions(null, metaSpacePool, currentMetaSpaceValue, "metaSpacePool")
               && this.validateRestrictions(null, ramDiskPool, currentRamDiskValue, "ramDiskPool")
               && this.validateRestrictions(null, codeCachePool, currentCodeCacheValue, "codeCachePool")
               && this.validateRestrictions(null, systemReservePool, currentSystemReserveValue, "systemReservePool")) {
               validConfigurationFound = true;
            } else {
               this.filter.severe("invalid nre memory configuration, resetting to defaults");
            }
         } catch (Exception e) {
            this.filter.severe("error reading nre memory configuration (" + e + "), resetting to defaults");
         }
      } else if (heapFile.exists() || metaFile.exists() || ramFile.exists() || codeFile.exists() || systemFile.exists()) {
         this.filter.severe("partial nre memory configuration detected, resetting to defaults");
      }

      if (validConfigurationFound) {
         this.defaultSettings = false;
      } else {
         this.deleteFile(heapFile);
         this.deleteFile(metaFile);
         this.deleteFile(ramFile);
         this.deleteFile(codeFile);
         this.deleteFile(systemFile);
         this.defaultSettings = true;
      }

      return true;
   }

   @Override
   public boolean useDefaultAuthentication() {
      return false;
   }

   @Override
   public boolean authenticate(HttpServletRequest req, HttpServletResponse resp) {
      boolean requireAdmin = false;
      String queryString = req.getQueryString();
      if (queryString != null) {
         KeyedList query = Http.getGetForm(queryString);
         requireAdmin = query.containsKey("update");
      }

      return requireAdmin
         ? DaemonAuthUtil.authAdmin(this.getServer().getAuthenticator(), req, resp)
         : DaemonAuthUtil.authUser(this.getServer().getAuthenticator(), req, resp);
   }

   @Override
   public synchronized int doGet(HttpServletRequest request, ErrorHandler handler, KeyedList query, XWriter content) {
      if (query == null || !query.containsKey("update")) {
         return this.sendSettings(content);
      } else if (!DebugServlet.debugEnabled && !CsrfTokenUtil.verifyCsrfToken(request, query.get("csrfToken", null))) {
         MessageBundle msg = new MessageBundle("invalid CSRF token in request");
         handler.error(msg);
         this.filter.severe("invalid CSRF token in request");
         return 403;
      } else {
         return this.doUpdate(handler, query);
      }
   }

   private int doUpdate(ErrorHandler handler, KeyedList query) {
      NreConfigurationServlet.HasAnyHasAll propertyChecker = new NreConfigurationServlet.HasAnyHasAll();
      if (query.containsKey("reset")) {
         return this.resetDefaults(handler);
      }

      checkProperty(query, "heapSpaceSize", propertyChecker);
      checkProperty(query, "metaSpaceSize", propertyChecker);
      checkProperty(query, "ramDiskSize", propertyChecker);
      checkProperty(query, "codeCacheSize", propertyChecker);
      checkProperty(query, "systemSize", propertyChecker);
      if (propertyChecker.hasAny && !propertyChecker.hasAll) {
         MessageBundle msg = new MessageBundle("platDaemon", "NreConfigServlet.missingArg", "NreConfigServlet: All parameters must be provided");
         handler.error(msg);
         this.filter.severe("all nreconfig parameters must be provided in update");
         return 400;
      }

      String queryValue = null;

      int heapSpaceSizeUpdate;
      int metaSpaceSizeUpdate;
      int ramDiskSizeUpdate;
      int codeCacheSizeUpdate;
      int systemReserveSizeUpdate;
      try {
         queryValue = query.get("heapSpaceSize", "0");
         heapSpaceSizeUpdate = Integer.parseInt(queryValue);
         queryValue = query.get("metaSpaceSize", "0");
         metaSpaceSizeUpdate = Integer.parseInt(queryValue);
         queryValue = query.get("ramDiskSize", "0");
         ramDiskSizeUpdate = Integer.parseInt(queryValue);
         queryValue = query.get("codeCacheSize", "0");
         codeCacheSizeUpdate = Integer.parseInt(queryValue);
         queryValue = query.get("systemSize", "0");
         systemReserveSizeUpdate = Integer.parseInt(queryValue);
      } catch (Exception e) {
         MessageBundle msg = new MessageBundle("Invalid memory value \"" + queryValue + "\" specified");
         handler.error(msg);
         this.filter.severe("invalid memory value \"" + queryValue + "\" specified");
         return 400;
      }

      NREMemoryPool heapSpacePool = this.getMemoryPool(0, true);
      NREMemoryPool metaSpacePool = this.getMemoryPool(2, true);
      NREMemoryPool ramDiskPool = this.getMemoryPool(3, true);
      NREMemoryPool codeCachePool = this.getMemoryPool(1, true);
      NREMemoryPool systemReservePool = this.getMemoryPool(4, true);
      if (this.validateRestrictions(handler, heapSpacePool, heapSpaceSizeUpdate, "heapSpacePool")
         && this.validateRestrictions(handler, metaSpacePool, metaSpaceSizeUpdate, "metaSpacePool")
         && this.validateRestrictions(handler, ramDiskPool, ramDiskSizeUpdate, "ramDiskPool")
         && this.validateRestrictions(handler, codeCachePool, codeCacheSizeUpdate, "codeCachePool")
         && this.validateRestrictions(handler, systemReservePool, systemReserveSizeUpdate, "systemReservePool")) {
         int totalPoolSizes = heapSpaceSizeUpdate + metaSpaceSizeUpdate + ramDiskSizeUpdate + codeCacheSizeUpdate;
         if (totalPoolSizes > this.getTotalNREMemoryPool()) {
            MessageBundle msg = new MessageBundle("Invalid memory values specified, " + totalPoolSizes + " > " + this.getTotalNREMemoryPool());
            handler.error(msg);
            this.filter.severe("invalid memory values specified, " + totalPoolSizes + " > " + this.getTotalNREMemoryPool());
            return 400;
         }

         File heapFile = new File(HEAP_SPACE_SIZE_FILEPATH);
         File metaFile = new File(META_SPACE_SIZE_FILEPATH);
         File ramDiskFile = new File(RAM_DISK_SIZE_FILEPATH);
         File codeCacheFile = new File(CODE_CACHE_SIZE_FILEPATH);
         File systemFile = new File(SYSTEM_RESERVE_SIZE_FILEPATH);

         try {
            writeFileValue(heapFile, heapSpaceSizeUpdate);
            writeFileValue(metaFile, metaSpaceSizeUpdate);
            writeFileValue(ramDiskFile, ramDiskSizeUpdate);
            writeFileValue(codeCacheFile, codeCacheSizeUpdate);
            writeFileValue(systemFile, systemReserveSizeUpdate);
         } catch (Exception e) {
            MessageBundle msg = new MessageBundle("Error writing nre memory configuration to file (" + e + ")");
            handler.error(msg);
            this.filter.log(Level.SEVERE, "error writing nre memory configuration to file (" + e + ")", e);
            this.deleteFile(heapFile);
            this.deleteFile(metaFile);
            this.deleteFile(ramDiskFile);
            this.deleteFile(codeCacheFile);
            this.deleteFile(systemFile);
            return 500;
         }

         this.defaultSettings = false;
         this.filter
            .info(
               "nre memory pool successfully updated, heap = "
                  + heapSpaceSizeUpdate
                  + " meta = "
                  + metaSpaceSizeUpdate
                  + " ram = "
                  + ramDiskSizeUpdate
                  + " code = "
                  + codeCacheSizeUpdate
                  + " system = "
                  + systemReserveSizeUpdate
            );
         return 200;
      } else {
         return 400;
      }
   }

   private int sendSettings(XWriter content) {
      content.w("<nreConfig").w(' ').attr("version", "3").w(' ').attr("totalMemory", String.valueOf(this.getTotalNREMemoryPool())).w(">").nl();
      printMemoryPoolXML(content, "heapSpace", this.getMemoryPool(0, true));
      printMemoryPoolXML(content, "metaSpace", this.getMemoryPool(2, true));
      printMemoryPoolXML(content, "ramDisk", this.getMemoryPool(3, true));
      printMemoryPoolXML(content, "codeCache", this.getMemoryPool(1, true));
      printMemoryPoolXML(content, "system", this.getMemoryPool(4, true));
      content.w("</nreConfig>").nl();
      return 200;
   }

   private static void printMemoryPoolXML(XWriter content, String elementName, NREMemoryPool memoryPool) {
      content.w("  <" + elementName)
         .w(' ')
         .attr("default", String.valueOf(memoryPool.getDefaultSize()))
         .w(' ')
         .attr("current", String.valueOf(memoryPool.getCurrentSize()))
         .w(' ')
         .attr("minimum", String.valueOf(memoryPool.getMinimumSize()))
         .w(' ')
         .attr("maximum", String.valueOf(memoryPool.getMaximumSize()))
         .w("/>")
         .nl();
   }

   public synchronized boolean printSettings(StringBuilder builder) {
      NREMemoryPool heapSpaceMemory = this.getMemoryPool(0, true);
      NREMemoryPool metaSpaceMemory = this.getMemoryPool(2, true);
      NREMemoryPool ramDiskMemory = this.getMemoryPool(3, true);
      NREMemoryPool codeCacheMemory = this.getMemoryPool(1, true);
      NREMemoryPool systemReserveMemory = this.getMemoryPool(4, true);
      builder.append("Heap->").append(heapSpaceMemory.getCurrentSize()).append("[").append(heapSpaceMemory.getDefaultSize()).append("] ");
      builder.append("Meta->").append(metaSpaceMemory.getCurrentSize()).append("[").append(metaSpaceMemory.getDefaultSize()).append("] ");
      builder.append("Ram Disk->").append(ramDiskMemory.getCurrentSize()).append("[").append(ramDiskMemory.getDefaultSize()).append("] ");
      builder.append("Code Cache->").append(codeCacheMemory.getCurrentSize()).append("[").append(codeCacheMemory.getDefaultSize()).append("] ");
      builder.append("System->").append(systemReserveMemory.getCurrentSize()).append("[").append(systemReserveMemory.getDefaultSize()).append("]");
      return this.defaultSettings;
   }

   private int resetDefaults(ErrorHandler handler) {
      File heapFile = new File(HEAP_SPACE_SIZE_FILEPATH);
      File metaFile = new File(META_SPACE_SIZE_FILEPATH);
      File ramDiskFile = new File(RAM_DISK_SIZE_FILEPATH);
      File codeCacheFile = new File(CODE_CACHE_SIZE_FILEPATH);
      File systemReserveFile = new File(SYSTEM_RESERVE_SIZE_FILEPATH);
      boolean success = this.deleteFile(heapFile);
      success = this.deleteFile(metaFile) && success;
      success = this.deleteFile(ramDiskFile) && success;
      success = this.deleteFile(codeCacheFile) && success;
      success = this.deleteFile(systemReserveFile) && success;
      if (!success) {
         MessageBundle msg = new MessageBundle("Failed to reset nre memory pool");
         handler.error(msg);
         this.filter.severe("failed to reset nre memory pool");
         return 500;
      } else {
         this.defaultSettings = true;
         this.filter.info("nre memory pool successfully reset");
         return 200;
      }
   }

   private int getTotalNREMemoryPool() {
      return this.totalNREMemorySize;
   }

   private NREMemoryPool getMemoryPool(int requestedPool, boolean calculateCurrent) {
      NREMemoryPool poolSizes;
      String currentSizePath;
      switch (requestedPool) {
         case 0:
            poolSizes = this.platformProvider.getNREHeapMemoryPool();
            currentSizePath = HEAP_SPACE_SIZE_FILEPATH;
            break;
         case 1:
            poolSizes = this.platformProvider.getNRECodeCacheMemoryPool();
            currentSizePath = CODE_CACHE_SIZE_FILEPATH;
            break;
         case 2:
            poolSizes = this.platformProvider.getNREMetaSpaceMemoryPool();
            currentSizePath = META_SPACE_SIZE_FILEPATH;
            break;
         case 3:
            poolSizes = this.platformProvider.getNRERamDiskMemoryPool();
            currentSizePath = RAM_DISK_SIZE_FILEPATH;
            break;
         case 4:
            poolSizes = this.platformProvider.getNRESystemReserveMemoryPool();
            currentSizePath = SYSTEM_RESERVE_SIZE_FILEPATH;
            break;
         default:
            throw new UnsupportedOperationException("Unknown pool requested");
      }

      if (calculateCurrent) {
         int currentValue = poolSizes.getDefaultSize();
         File currentFile = new File(currentSizePath);

         try {
            currentValue = readFileValue(currentFile);
         } catch (Exception var8) {
         }

         poolSizes.setCurrentSize(currentValue);
      }

      return poolSizes;
   }

   private static void checkProperty(KeyedList query, String propName, NreConfigurationServlet.HasAnyHasAll reference) {
      if (query == null) {
         reference.hasAll = false;
      } else {
         reference.hasAny = reference.hasAny || query.containsKey(propName);
         reference.hasAll = reference.hasAll && query.containsKey(propName);
      }
   }

   private boolean validateRestrictions(ErrorHandler handler, NREMemoryPool pool, int newValue, String poolName) {
      if (newValue < pool.getMinimumSize()) {
         MessageBundle msg = new MessageBundle("Invalid memory value for " + poolName + ": " + newValue + " < " + pool.getMinimumSize());
         if (handler != null) {
            handler.error(msg);
         }

         this.filter.severe("invalid nre memory value for " + poolName + ": " + newValue + " < " + pool.getMinimumSize());
         return false;
      } else if (newValue > pool.getMaximumSize()) {
         MessageBundle msg = new MessageBundle("Invalid memory value for " + poolName + ": " + newValue + " > " + pool.getMaximumSize());
         if (handler != null) {
            handler.error(msg);
         }

         this.filter.severe("invalid nre memory value for " + poolName + ": " + newValue + " > " + pool.getMaximumSize());
         return false;
      } else {
         return true;
      }
   }

   private boolean deleteFile(File fileToDelete) {
      boolean deleted = true;
      Exception deleteException = null;

      try {
         if (fileToDelete.exists() && !fileToDelete.delete()) {
            deleted = false;
         }
      } catch (Exception e) {
         deleteException = e;
         deleted = false;
      }

      if (!deleted) {
         this.filter.warning("failed to delete file: " + fileToDelete.getName() + (deleteException != null ? "(" + deleteException + ")" : ""));
      }

      return deleted;
   }

   private static int readFileValue(File fileToRead) throws Exception {
      try (BufferedReader fileReader = new BufferedReader(new FileReader(fileToRead))) {
         return Integer.parseInt(fileReader.readLine());
      }
   }

   private static void writeFileValue(File fileToWrite, int value) throws Exception {
      try (BufferedWriter fileWriter = new BufferedWriter(new FileWriter(fileToWrite))) {
         fileWriter.write(String.valueOf(value));
         fileWriter.flush();
      }
   }

   private static class HasAnyHasAll {
      public boolean hasAny = false;
      public boolean hasAll = true;

      private HasAnyHasAll() {
      }
   }
}
