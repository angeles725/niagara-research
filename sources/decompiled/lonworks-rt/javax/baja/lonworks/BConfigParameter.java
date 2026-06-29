package javax.baja.lonworks;

import com.tridium.lonworks.util.NmUtil;
import com.tridium.lonworks.util.selfdoc.SelfDocUtil;
import java.util.logging.Level;
import javax.baja.lonworks.datatypes.BConfigProps;
import javax.baja.lonworks.datatypes.BModifyFlags;
import javax.baja.lonworks.enums.BLonConfigScope;
import javax.baja.lonworks.enums.BLonNodeState;
import javax.baja.lonworks.londata.BLonData;
import javax.baja.lonworks.util.LonFile;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BIcon;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "configProps",
   type = "BConfigProps",
   defaultValue = "new BConfigProps()"
)
public class BConfigParameter extends BLonComponent {
   public static final Property configProps = newProperty(0, new BConfigProps(), null);
   public static final Type TYPE = Sys.loadType(BConfigParameter.class);
   private static final BIcon icon = BIcon.make("module://lonworks/com/tridium/lonworks/ui/icons/cp.png");

   public BConfigProps getConfigProps() {
      return (BConfigProps)this.get(configProps);
   }

   public void setConfigProps(BConfigProps v) {
      this.set(configProps, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public BConfigParameter() {
   }

   public BConfigParameter(BLonData data, int offset, int length, BModifyFlags modifyFlag, BLonConfigScope scope, String select) {
      BConfigProps cfgProps = this.getConfigProps();
      cfgProps.setOffset(offset);
      cfgProps.setLength(length);
      cfgProps.setModifyFlag(modifyFlag);
      cfgProps.setScope(scope);
      cfgProps.setSelect(select);
      this.setData(data);
   }

   @Override
   public boolean isConfigParameter() {
      return true;
   }

   @Override
   public boolean isForeignPersistent() {
      return true;
   }

   @Override
   public boolean isWriteable() {
      BModifyFlags mf = this.getConfigProps().getModifyFlag();
      return !mf.isMfgOnly() && !mf.isConst();
   }

   @Override
   protected void dataChanged(Context context) {
      if (!BLonNetwork.lonNoWrite.equals(context)) {
         if (this.isRunning()) {
            this.doForceWrite();
         }
      }
   }

   @Override
   public void doForceWrite() {
      BConfigProps configProps = this.getConfigProps();
      if (configProps.getModifyFlag().isMfgOnly()) {
         throw new BajaRuntimeException("Can not write mfgOnly cp " + this.getDisplayName(null));
      } else if (configProps.getModifyFlag().isConst()) {
         throw new BajaRuntimeException("Can not write constant cp " + this.getDisplayName(null));
      } else {
         BLonDevice dev = this.lonDevice();
         dev.checkState();
         boolean downloading = dev.isDownLoadInProgress();

         try {
            LonFile f = dev.getReadWriteFile();
            if (f == null) {
               throw new LonException("Error writing " + this.getDisplayName(null) + ": could not access file");
            } else {
               int[] sels = null;
               boolean[] objDis = null;
               boolean onlineReq = false;
               boolean failedObjDevOff = false;
               if (!downloading) {
                  dev.beginConfigWrite();
                  if (configProps.getModifyFlag().isDisabled() && configProps.getScope() == BLonConfigScope.object) {
                     sels = SelfDocUtil.selectToIntArray(configProps.getSelect());
                     objDis = new boolean[sels.length];

                     try {
                        dev.disableObjectsForWrite(sels, objDis);
                     } catch (Throwable var12) {
                        this.lonNetwork().log().log(Level.WARNING, "Unable to disable object " + configProps.getSelect(), var12);
                        failedObjDevOff = true;
                     }
                  }

                  if (configProps.getModifyFlag().isOffline() || failedObjDevOff) {
                     try {
                        NmUtil.setDeviceState(dev, BLonNodeState.configOffline);
                     } catch (LonException var11) {
                        System.out.println(var11);
                     }

                     onlineReq = true;
                  }
               }

               f.write(this.getData().toNetBytes(), configProps.getOffset());
               if (!downloading) {
                  f.flush();
                  if (onlineReq) {
                     try {
                        NmUtil.setDeviceState(dev, BLonNodeState.configOnline);
                     } catch (LonException var10) {
                        System.out.println(var10);
                     }
                  }

                  if (sels != null) {
                     dev.enableObjectsAfterWrite(sels, objDis);
                  }

                  if (configProps.getModifyFlag().isReset()) {
                     dev.doReset();
                  }

                  dev.endConfigWrite();
               }

               this.getData().writeOk();
            }
         } catch (Throwable var13) {
            this.getData().writeFail(var13.toString());
            String errMsg = "Unable to write " + this.debugName();
            this.lonNetwork().log().log(Level.SEVERE, errMsg, var13);
            throw new BajaRuntimeException(errMsg + " " + var13.getMessage(), var13);
         }
      }
   }

   @Override
   public void doForceRead() {
      BLonDevice dev = this.lonDevice();
      dev.checkState();
      BConfigProps configProps = this.getConfigProps();
      LonFile f = configProps.getModifyFlag().isConst() ? dev.getReadOnlyFile() : dev.getReadWriteFile();
      if (f == null) {
         throw new RuntimeException("Error reading " + this.getDisplayName(null) + ": could not access file");
      } else {
         try {
            this.getData().fromNetBytes(f.read(configProps.getOffset(), configProps.getLength()));
            this.getData().readOk();
         } catch (Throwable var6) {
            this.getData().readFail(var6.toString());
            String errMsg = "Unable to read " + this.debugName();
            this.lonNetwork().log().log(Level.SEVERE, errMsg, var6);
            throw new BajaRuntimeException(errMsg + " " + var6.getMessage(), var6);
         }
      }
   }

   @Override
   protected void lonComponentSubscribed() {
      BBoolean bb = (BBoolean)this.getPropertyInParent().getFacets().get("deviceSpecific");
      if (bb != null && bb.getBoolean()) {
         this.forceRead();
      }
   }

   public BIcon getIcon() {
      return icon;
   }
}
