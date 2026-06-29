package javax.baja.lonworks;

import com.tridium.lonworks.Lon;
import com.tridium.lonworks.util.NmUtil;
import com.tridium.lonworks.util.selfdoc.SelfDocUtil;
import java.util.logging.Level;
import javax.baja.lonworks.datatypes.BModifyFlags;
import javax.baja.lonworks.datatypes.BNcProps;
import javax.baja.lonworks.datatypes.BNvConfigData;
import javax.baja.lonworks.enums.BLonConfigScope;
import javax.baja.lonworks.enums.BLonNodeState;
import javax.baja.lonworks.londata.BLonData;
import javax.baja.lonworks.util.SnvtUtil;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraTopic;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BIcon;
import javax.baja.sys.BValue;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Topic;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "ncProps",
      type = "BNcProps",
      defaultValue = "new BNcProps()"
   ), @NiagaraProperty(
      name = "nvConfigData",
      type = "BNvConfigData",
      defaultValue = "new BNvConfigData()"
   )})
@NiagaraTopic(
   name = "receivedUpdate"
)
public class BNetworkConfig extends BLonComponent implements BINetworkVariable {
   public static final Property ncProps = newProperty(0, new BNcProps(), null);
   public static final Property nvConfigData = newProperty(0, new BNvConfigData(), null);
   public static final Topic receivedUpdate = newTopic(0, null);
   public static final Type TYPE = Sys.loadType(BNetworkConfig.class);
   private static final BIcon icon = BIcon.make("module://lonworks/com/tridium/lonworks/ui/icons/nci.png");

   public BNcProps getNcProps() {
      return (BNcProps)this.get(ncProps);
   }

   public void setNcProps(BNcProps v) {
      this.set(ncProps, v, null);
   }

   @Override
   public BNvConfigData getNvConfigData() {
      return (BNvConfigData)this.get(nvConfigData);
   }

   @Override
   public void setNvConfigData(BNvConfigData v) {
      this.set(nvConfigData, v, null);
   }

   public void fireReceivedUpdate(BValue event) {
      this.fire(receivedUpdate, event, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public BNetworkConfig() {
   }

   public BNetworkConfig(int nvIndex, int snvtType, int configIndex, BModifyFlags modifyFlag, BLonConfigScope scope, String select, float[] init) {
      this(nvIndex, snvtType, configIndex, modifyFlag, scope, select);
      this.initDataElements(init);
   }

   public BNetworkConfig(int nvIndex, int snvtType, int configIndex, BModifyFlags modifyFlag, BLonConfigScope scope, String select) {
      BNcProps ncProps = this.getNcProps();
      ncProps.setNvIndex(nvIndex);
      ncProps.setSnvtType(snvtType);
      ncProps.setConfigIndex(configIndex);
      ncProps.setModifyFlag(modifyFlag);
      ncProps.setScope(scope);
      ncProps.setSelect(select);
      this.setData(SnvtUtil.getLonData(snvtType));
   }

   public BNetworkConfig(int nvIndex, BLonData data, int configIndex, BModifyFlags modifyFlag, BLonConfigScope scope, String select) {
      BNcProps ncProps = this.getNcProps();
      ncProps.setNvIndex(nvIndex);
      ncProps.setConfigIndex(configIndex);
      ncProps.setModifyFlag(modifyFlag);
      ncProps.setScope(scope);
      ncProps.setSelect(select);
      this.setData(data);
   }

   @Override
   public boolean isNetworkConfig() {
      return true;
   }

   @Override
   public int getNvIndex() {
      return this.getNcProps().getNvIndex();
   }

   @Override
   public void setNvIndex(int nvIndex) {
      this.getNcProps().setNvIndex(nvIndex);
   }

   @Override
   public int getSnvtType() {
      return this.getNcProps().getSnvtType();
   }

   @Override
   public void setUnbound() {
      this.getNcProps().setUnbound();
      this.getNvConfigData().setUnbound(this.getNvIndex());
   }

   @Override
   public void receiveUpdate(byte[] nvData) {
      try {
         this.getData().fromNetBytes(nvData);
         this.getData().readOk();
         this.fireReceivedUpdate(null);
      } catch (Throwable var3) {
         this.getData().readFail(var3.toString());
         this.lonNetwork()
            .log()
            .log(Level.SEVERE, "Could not decode nv update data " + this.getParent().getDisplayName(null) + ":" + this.getDisplayName(null), var3);
      }
   }

   @Override
   public final void lonComponentStarted() {
      if (this.getNvConfigData().getSelector() == -1) {
         this.getNvConfigData().setUnbound(this.getNcProps().getNvIndex());
      }
   }

   @Override
   public boolean isForeignPersistent() {
      return true;
   }

   @Override
   public boolean isWriteable() {
      return !this.getNcProps().getModifyFlag().isMfgOnly();
   }

   @Override
   protected void dataChanged(Context context) {
      if (!BLonNetwork.lonNoWrite.equals(context)) {
         if (this.isRunning()) {
            this.forceWrite();
         }
      }
   }

   @Override
   public void doForceRead() {
      this.lonDevice().checkState();
      if (this.illegalLength) {
         throw new BajaRuntimeException(this.getDisplayName(null) + " data length > maxNvLength of " + Lon.maxNvLength() + " bytes");
      } else {
         BNcProps ncProps = this.getNcProps();

         try {
            if (Lon.d()) {
               byte[] nvData = NmUtil.fetchNv(this.lonDevice(), ncProps.getNvIndex());
               this.getData().fromNetBytes(nvData);
            }

            this.getData().readOk();
         } catch (Throwable var4) {
            this.getData().readFail(var4.toString());
            String errMsg = "Unable to read " + this.debugName();
            this.lonNetwork().log().log(Level.SEVERE, errMsg, var4);
            throw new BajaRuntimeException(errMsg + " " + var4.getMessage(), var4);
         }
      }
   }

   @Override
   public void doForceWrite() {
      if (Lon.d()) {
         BNvConfigData configData = this.getNvConfigData();
         if (configData.isInput()) {
            BNcProps ncProps = this.getNcProps();
            if (ncProps.getModifyFlag().isMfgOnly()) {
               throw new BajaRuntimeException("Can not write mfgOnly nci " + this.getDisplayName(null));
            } else {
               BLonDevice dev = this.lonDevice();
               dev.checkState();
               if (this.illegalLength) {
                  throw new BajaRuntimeException(this.getDisplayName(null) + " data length > maxNvLength of " + Lon.maxNvLength() + " bytes");
               } else {
                  boolean downloading = dev.isDownLoadInProgress();

                  try {
                     int[] sels = null;
                     boolean[] objDis = null;
                     boolean onlineReq = false;
                     if (!downloading) {
                        dev.beginConfigWrite();
                        if (ncProps.getModifyFlag().isOffline()) {
                           try {
                              NmUtil.setDeviceState(dev, BLonNodeState.configOffline);
                           } catch (LonException var10) {
                              System.out.println(var10);
                           }

                           onlineReq = true;
                        }

                        if (ncProps.getModifyFlag().isDisabled() && ncProps.getScope() == BLonConfigScope.object) {
                           sels = SelfDocUtil.selectToIntArray(ncProps.getSelect());
                           objDis = new boolean[sels.length];
                           dev.disableObjectsForWrite(sels, objDis);
                        }
                     }

                     NmUtil.setNvValue(this.lonDevice(), configData, this.getData().toNetBytes());
                     if (!downloading) {
                        if (onlineReq) {
                           try {
                              NmUtil.setDeviceState(dev, BLonNodeState.configOnline);
                           } catch (LonException var9) {
                              System.out.println(var9);
                           }
                        }

                        if (sels != null) {
                           dev.enableObjectsAfterWrite(sels, objDis);
                        }

                        if (ncProps.getModifyFlag().isReset()) {
                           dev.doReset();
                        }

                        dev.endConfigWrite();
                     }

                     this.getData().writeOk();
                  } catch (Throwable var11) {
                     this.getData().writeFail(var11.toString());
                     String errMsg = "Unable to write " + this.debugName();
                     this.lonNetwork().log().log(Level.SEVERE, errMsg, var11);
                     throw new BajaRuntimeException(errMsg + " " + var11.getMessage(), var11);
                  }
               }
            }
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
