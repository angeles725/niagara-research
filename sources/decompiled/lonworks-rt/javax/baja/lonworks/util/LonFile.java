package javax.baja.lonworks.util;

import com.tridium.lonworks.device.DeviceFacets;
import com.tridium.lonworks.file.LonFileReadWrite;
import com.tridium.lonworks.file.LonFileTransfer;
import com.tridium.lonworks.file.NoDirectoryException;
import javax.baja.lonworks.BINetworkVariable;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BNetworkVariable;
import javax.baja.lonworks.LonException;

public abstract class LonFile {
   public static final boolean ALLOW_RANDOM_ACCESS = true;
   public static final boolean NO_RANDOM_ACCESS = false;
   public static final boolean CREATE_FILE = true;
   public static final boolean ACCESS_FILE = false;
   public static final int CONFIG_TEMPLATE_FILE = 0;
   public static final int READ_WRITE_CONFIG_FILE = 1;
   public static final int READ_ONLY_CONFIG_FILE = 2;
   public static final int CONFIG_PARAM_TEMPLATE_FILE = 2;
   public static final int CONFIG_PARAM_VALUE_FILE = 1;

   public static LonFile createFile(BLonDevice dev) throws LonException {
      LonFile file = null;
      BINetworkVariable[] nvs = dev.getNetworkVariables();
      BNetworkVariable dirNv;
      if ((dirNv = findNvByObjectAndType(dev, nvs, 114)) != null) {
         file = new LonFileReadWrite(dev, dirNv);
      } else {
         BNetworkVariable reqNv;
         BNetworkVariable statNv;
         if ((reqNv = findNvByObjectAndType(dev, nvs, 73)) == null || (statNv = findNvByObjectAndType(dev, nvs, 74)) == null) {
            return null;
         }

         BNetworkVariable posNv = findNvByObjectAndType(dev, nvs, 90);

         try {
            file = new LonFileTransfer(dev, reqNv, statNv, posNv);
         } catch (NoDirectoryException var8) {
            dev.log().warning(dev.getDisplayName(null) + " has file snvts but reports no file directory");
            return null;
         }
      }

      return file;
   }

   private static BNetworkVariable findNvByObjectAndType(BLonDevice dev, BINetworkVariable[] nvs, int snvtType) {
      int nodObjNdx = DeviceFacets.getNodeObjectIndex(dev);

      for (int i = 0; i < nvs.length; i++) {
         if (nvs[i] != null && nvs[i].isNetworkVariable()) {
            BNetworkVariable nv = (BNetworkVariable)nvs[i];
            if (nv.getNvProps().getObjectIndex() == nodObjNdx && nv.getSnvtType() == snvtType) {
               return nv;
            }
         }
      }

      return null;
   }

   public abstract LonFile copy();

   public abstract void open(int var1, boolean var2, boolean var3) throws LonException;

   public abstract byte[] read(int var1, int var2) throws LonException;

   public abstract byte[] read(int var1) throws LonException;

   public abstract byte[] read() throws LonException;

   public abstract void write(byte[] var1, int var2) throws LonException;

   public abstract void write(byte[] var1) throws LonException;

   public abstract void close() throws LonException;

   public abstract void flush() throws LonException;

   public abstract String getDirectoryString() throws LonException;

   public abstract int findFileNum(int var1);

   public abstract int findFileNum(int var1, int var2);

   public abstract boolean isOpen();

   public boolean supportsRandomAccess() {
      return true;
   }
}
