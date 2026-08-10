package com.tridium.ndriver.ui;

import com.tridium.ndriver.discover.BINDiscoveryGroup;
import com.tridium.ndriver.discover.BINDiscoveryIcon;
import com.tridium.ndriver.discover.BINDiscoveryLeaf;
import com.tridium.ndriver.discover.BINDiscoveryObject;
import com.tridium.ndriver.discover.BNDiscoveryJob;
import com.tridium.ndriver.discover.BNDiscoveryLeaf;
import com.tridium.ndriver.ui.device.BNDeviceManager;
import com.tridium.ndriver.ui.point.BNPointManager;
import javax.baja.control.BBooleanPoint;
import javax.baja.control.BBooleanWritable;
import javax.baja.control.BEnumPoint;
import javax.baja.control.BEnumWritable;
import javax.baja.control.BNumericPoint;
import javax.baja.control.BNumericWritable;
import javax.baja.control.BStringPoint;
import javax.baja.control.BStringWritable;
import javax.baja.gx.BImage;
import javax.baja.job.BJob;
import javax.baja.naming.SlotPath;
import javax.baja.nre.util.Array;
import javax.baja.registry.TypeInfo;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BComponentEvent;
import javax.baja.sys.BIcon;
import javax.baja.sys.BValue;
import javax.baja.sys.Subscriber;
import javax.baja.sys.Type;
import javax.baja.workbench.mgr.BAbstractManager;
import javax.baja.workbench.mgr.MgrColumn;
import javax.baja.workbench.mgr.MgrEditRow;
import javax.baja.workbench.mgr.MgrLearn;
import javax.baja.workbench.mgr.MgrTypeInfo;

public class NMgrLearn extends MgrLearn {
   Subscriber discoveryFolderSub = new Subscriber() {
      public void event(BComponentEvent event) {
         if (event.getValue() instanceof BNDiscoveryLeaf) {
            if (event.getId() == 1) {
               if (NMgrLearn.this.hasRoot(event.getValue())) {
                  return;
               }

               NMgrLearn.this.updateRoots(((BNDiscoveryJob)NMgrLearn.this.getJob()).getRootDiscoveryObjects());
            } else if (event.getId() == 2) {
               if (!NMgrLearn.this.hasRoot(event.getValue())) {
                  return;
               }

               NMgrLearn.this.updateRoots(((BNDiscoveryJob)NMgrLearn.this.getJob()).getRootDiscoveryObjects());
            }
         }
      }
   };
   public static final BIcon deviceIcon = BIcon.make("module://icons/x16/device.png");
   public static final BIcon pointGroupIcon = BIcon.make("module://icons/x16/pointFolder.png");
   public static final BIcon deviceGroupIcon = BIcon.make("module://icons/x16/deviceFolder.png");
   public static final BIcon numericElementIcon = BIcon.make("module://icons/x16/control/numericPoint.png");
   public static final BIcon booleanElementIcon = BIcon.make("module://icons/x16/control/booleanPoint.png");
   public static final BIcon enumElementIcon = BIcon.make("module://icons/x16/control/enumPoint.png");
   public static final BIcon stringElementIcon = BIcon.make("module://icons/x16/control/stringPoint.png");
   public static final BIcon numericElementIconRo = BIcon.make("module://icons/x16/statusNumeric.png");
   public static final BIcon booleanElementIconRo = BIcon.make("module://icons/x16/statusBoolean.png");
   public static final BIcon enumElementIconRo = BIcon.make("module://icons/x16/statusEnum.png");
   public static final BIcon stringElementIconRo = BIcon.make("module://icons/x16/statusString.png");

   public NMgrLearn(BAbstractManager manager) {
      super(manager);
   }

   public void setJob(BJob job) {
      super.setJob(job);
      if (job instanceof BNDiscoveryJob) {
         BNDiscoveryJob dJob = (BNDiscoveryJob)job;
         this.discoveryFolderSub.subscribe(dJob.discoveryFolder());
         this.updateRoots(dJob.getRootDiscoveryObjects());
      }
   }

   protected MgrColumn[] makeColumns() {
      Array<MgrColumn> al = new Array(MgrColumn.class);
      NMgrColumnUtil.getColumnsFor((BComplex)NMgrUtil.getDiscoveryLeafInstance(this.getManager()), al);
      return (MgrColumn[])al.trim();
   }

   public void toRow(Object discovery, MgrEditRow row) {
      if (discovery instanceof BINDiscoveryLeaf) {
         BINDiscoveryLeaf leaf = (BINDiscoveryLeaf)discovery;
         leaf.updateTarget(row.getTarget());
         leaf.defaultTargetUpdate(row.getTarget());

         try {
            row.loadCells();
         } catch (Exception var5) {
         }

         if (leaf.getDiscoveryName() != null) {
            row.setDefaultName(SlotPath.escape(leaf.getDiscoveryName()));
         }
      }
   }

   public void jobComplete(BJob job) {
      if (job instanceof BNDiscoveryJob) {
         BNDiscoveryJob djob = (BNDiscoveryJob)job;
         this.updateRoots(djob.getRootDiscoveryObjects());
         this.discoveryFolderSub.unsubscribe(djob.discoveryFolder());
      }
   }

   public boolean isDepthExpandable(int depth) {
      return true;
   }

   public boolean hasChildren(Object discovery) {
      return discovery instanceof BINDiscoveryGroup;
   }

   public boolean isExisting(Object discovery, BComponent component) {
      return discovery != null && component != null && discovery instanceof BINDiscoveryLeaf ? ((BINDiscoveryLeaf)discovery).isExisting(component) : false;
   }

   public Object[] getChildren(Object discovery) {
      if (!(discovery instanceof BINDiscoveryGroup)) {
         return null;
      } else {
         BINDiscoveryGroup discoveryGroup = (BINDiscoveryGroup)discovery;
         Object[] discoveryChildren = discoveryGroup.getChildren(BINDiscoveryObject.class);

         for (int i = 0; i < discoveryChildren.length; i++) {
            if (discoveryChildren[i] instanceof BComponent) {
               this.getManager().registerForComponentEvents((BComponent)discoveryChildren[i]);
            }
         }

         return discoveryChildren;
      }
   }

   public MgrTypeInfo[] toTypes(Object discovery) {
      if (discovery instanceof BINDiscoveryLeaf) {
         BINDiscoveryLeaf discoveryLeaf = (BINDiscoveryLeaf)discovery;
         TypeInfo[] validDbTypes = discoveryLeaf.getValidDatabaseTypes();
         if (validDbTypes != null && validDbTypes.length > 0) {
            return makeArray(validDbTypes);
         }
      }

      return null;
   }

   private static MgrTypeInfo[] makeArray(TypeInfo[] typeInfo) {
      MgrTypeInfo[] r = new MgrTypeInfo[typeInfo.length];

      for (int i = 0; i < r.length; i++) {
         r[i] = MgrTypeInfo.make(typeInfo[i]);
      }

      return r;
   }

   public boolean isGroup(Object discovery) {
      return discovery instanceof BINDiscoveryGroup;
   }

   public BImage getIcon(Object discovery) {
      BIcon icon = null;
      if (discovery instanceof BINDiscoveryIcon) {
         icon = ((BINDiscoveryIcon)discovery).getDiscoveryIcon();
      } else if (this.getManager() instanceof BNDeviceManager) {
         icon = this.getDeviceIconDefault(discovery);
      } else if (this.getManager() instanceof BNPointManager) {
         icon = this.getPointIconDefault(discovery);
      }

      return icon != null ? BImage.make(icon) : null;
   }

   private BIcon getPointIconDefault(Object discovery) {
      if (discovery instanceof BINDiscoveryGroup) {
         return pointGroupIcon;
      } else {
         return discovery instanceof BINDiscoveryLeaf ? this.getPointIconDefault((BINDiscoveryLeaf)discovery) : stringElementIconRo;
      }
   }

   private BIcon getPointIconDefault(BINDiscoveryLeaf discovery) {
      TypeInfo[] databaseTypes = discovery.getValidDatabaseTypes();
      if (databaseTypes != null && databaseTypes.length != 0) {
         Type defaultType = databaseTypes[0].getTypeSpec().getResolvedType();
         if (defaultType == null) {
            return null;
         } else if (defaultType.is(BNumericWritable.TYPE)) {
            return numericElementIcon;
         } else if (defaultType.is(BNumericPoint.TYPE)) {
            return numericElementIconRo;
         } else if (defaultType.is(BBooleanWritable.TYPE)) {
            return booleanElementIcon;
         } else if (defaultType.is(BBooleanPoint.TYPE)) {
            return booleanElementIconRo;
         } else if (defaultType.is(BEnumWritable.TYPE)) {
            return enumElementIcon;
         } else if (defaultType.is(BEnumPoint.TYPE)) {
            return enumElementIconRo;
         } else if (defaultType.is(BStringWritable.TYPE)) {
            return stringElementIcon;
         } else {
            return defaultType.is(BStringPoint.TYPE) ? stringElementIconRo : null;
         }
      } else {
         return null;
      }
   }

   private BIcon getDeviceIconDefault(Object discovery) {
      return discovery instanceof BINDiscoveryGroup ? deviceGroupIcon : deviceIcon;
   }

   private boolean hasRoot(BValue value) {
      for (int i = 0; i < this.getRootCount(); i++) {
         if (value.equivalent(this.getRoot(i))) {
            return true;
         }
      }

      return false;
   }
}
