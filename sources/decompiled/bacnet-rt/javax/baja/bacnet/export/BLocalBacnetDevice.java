package javax.baja.bacnet.export;

import com.tridium.bacnet.BacnetVendorUtil;
import com.tridium.bacnet.ObjectTypeList;
import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.asn.NReadPropertyResult;
import com.tridium.bacnet.services.BacnetConfirmedRequest;
import com.tridium.bacnet.services.confirmed.ReadRangeAck;
import com.tridium.bacnet.stack.BBacnetStack;
import com.tridium.bacnet.stack.DeviceRegistry;
import com.tridium.bacnet.stack.client.BBacnetClientLayer;
import com.tridium.bacnet.stack.link.mstp.BBacnetMstpLinkLayer;
import com.tridium.bacnet.stack.link.sc.BScLinkLayer;
import com.tridium.bacnet.stack.network.BBacnetNetworkLayer;
import com.tridium.bacnet.stack.network.BNetworkPort;
import com.tridium.bacnet.stack.server.BBacnetExportTable;
import com.tridium.bacnet.stack.server.LocalBacnetCovPropPoll;
import com.tridium.bacnet.stack.transport.BBacnetTransportLayer;
import com.tridium.sys.Nre;
import com.tridium.sys.station.Station;
import com.tridium.sys.station.Station.SaveListener;
import com.tridium.util.ComponentTreeCursor;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BIBacnetObjectContainer;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetAddressBinding;
import javax.baja.bacnet.datatypes.BBacnetArray;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetCovSubscription;
import javax.baja.bacnet.datatypes.BBacnetDateTime;
import javax.baja.bacnet.datatypes.BBacnetListOf;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetRecipient;
import javax.baja.bacnet.datatypes.BBacnetTimeStamp;
import javax.baja.bacnet.datatypes.BBacnetUnsigned;
import javax.baja.bacnet.datatypes.BIBacnetDataType;
import javax.baja.bacnet.enums.BBacnetBackupState;
import javax.baja.bacnet.enums.BBacnetDeviceStatus;
import javax.baja.bacnet.enums.BBacnetErrorClass;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.enums.BBacnetRestartReason;
import javax.baja.bacnet.enums.BBacnetSegmentation;
import javax.baja.bacnet.enums.BCharacterSetEncoding;
import javax.baja.bacnet.enums.BExtensibleEnumList;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.ChangeListError;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.PropertyReference;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.io.RangeData;
import javax.baja.bacnet.io.RangeReference;
import javax.baja.bacnet.io.RejectException;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.bacnet.util.PropertyInfo;
import javax.baja.bacnet.virtual.BLocalBacnetVirtualGateway;
import javax.baja.file.BFileSystem;
import javax.baja.file.BIFile;
import javax.baja.file.FilePath;
import javax.baja.naming.BOrd;
import javax.baja.naming.UnresolvedException;
import javax.baja.nav.BINavNode;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.security.PermissionException;
import javax.baja.spy.SpyWriter;
import javax.baja.status.BStatus;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BEnum;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIcon;
import javax.baja.sys.BObject;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.BasicContext;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Flags;
import javax.baja.sys.LocalizableRuntimeException;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.sys.Clock.Ticket;
import javax.baja.timezone.BTimeZone;
import javax.baja.units.BUnit;
import javax.baja.user.BUser;
import javax.baja.user.BUserService;
import javax.baja.util.BUuid;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "status",
      type = "BStatus",
      defaultValue = "BStatus.ok",
      flags = 67
   ), @NiagaraProperty(
      name = "faultCause",
      type = "String",
      defaultValue = "",
      flags = 3
   ), @NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.DEVICE)",
      facets = {@Facet("BBacnetObjectType.getObjectIdFacets(BBacnetObjectType.DEVICE)")}
   ), @NiagaraProperty(
      name = "systemStatus",
      type = "BBacnetDeviceStatus",
      defaultValue = "BBacnetDeviceStatus.operational",
      flags = 3
   ), @NiagaraProperty(
      name = "vendorName",
      type = "String",
      defaultValue = "Tridium",
      flags = 1
   ), @NiagaraProperty(
      name = "vendorId",
      type = "int",
      defaultValue = "36",
      flags = 1
   ), @NiagaraProperty(
      name = "modelName",
      type = "String",
      defaultValue = "Niagara4 Station",
      flags = 1
   ), @NiagaraProperty(
      name = "firmwareRevision",
      type = "String",
      defaultValue = "unknown",
      flags = 1
   ), @NiagaraProperty(
      name = "applicationSoftwareVersion",
      type = "String",
      defaultValue = "unknown",
      flags = 1
   ), @NiagaraProperty(
      name = "location",
      type = "String",
      defaultValue = "unknown"
   ), @NiagaraProperty(
      name = "description",
      type = "String",
      defaultValue = "Local BACnet Device object"
   ), @NiagaraProperty(
      name = "deviceUuid",
      type = "BUuid",
      defaultValue = "BUuid.DEFAULT",
      flags = 65,
      facets = {@Facet(
         name = "BFacets.SECURITY",
         value = "true"
      )}
   ), @NiagaraProperty(
      name = "protocolVersion",
      type = "int",
      defaultValue = "1",
      flags = 1
   ), @NiagaraProperty(
      name = "protocolRevision",
      type = "int",
      defaultValue = "14",
      flags = 1
   ), @NiagaraProperty(
      name = "protocolConformanceClass",
      type = "int",
      defaultValue = "3",
      flags = 5
   ), @NiagaraProperty(
      name = "protocolServicesSupported",
      type = "BBacnetBitString",
      defaultValue = "SERVER_SERVICES_SUPPORTED",
      flags = 1,
      facets = {@Facet("BacnetBitStringUtil.BACNET_SERVICES_SUPPORTED_FACETS")}
   ), @NiagaraProperty(
      name = "protocolObjectTypesSupported",
      type = "BBacnetBitString",
      defaultValue = "SERVER_OBJECT_TYPES_SUPPORTED",
      flags = 1,
      facets = {@Facet("BacnetBitStringUtil.BACNET_OBJECT_TYPES_SUPPORTED_FACETS")}
   ), @NiagaraProperty(
      name = "maxAPDULengthAccepted",
      type = "int",
      defaultValue = "ConfirmedRequestPdu.MAX_APDU_LENGTH_UP_TO_1476_OCTETS",
      flags = 1
   ), @NiagaraProperty(
      name = "segmentationSupported",
      type = "BBacnetSegmentation",
      defaultValue = "BBacnetSegmentation.segmentedBoth",
      flags = 1
   ), @NiagaraProperty(
      name = "maxSegmentsAccepted",
      type = "int",
      defaultValue = "ConfirmedRequestPdu.MAX_SEGS_GREATER_THAN_64",
      flags = 1
   ), @NiagaraProperty(
      name = "apduSegmentTimeout",
      type = "int",
      defaultValue = "2000",
      facets = {@Facet(
         name = "BFacets.UNITS",
         value = "BUnit.getUnit(\"millisecond\")"
      ), @Facet(
         name = "BFacets.MIN",
         value = "0"
      )}
   ), @NiagaraProperty(
      name = "apduTimeout",
      type = "int",
      defaultValue = "3000",
      facets = {@Facet(
         name = "BFacets.UNITS",
         value = "BUnit.getUnit(\"millisecond\")"
      ), @Facet(
         name = "BFacets.MIN",
         value = "0"
      )}
   ), @NiagaraProperty(
      name = "numberOfApduRetries",
      type = "int",
      defaultValue = "3"
   ), @NiagaraProperty(
      name = "deviceAddressBinding",
      type = "BBacnetListOf",
      defaultValue = "new BBacnetListOf(BBacnetAddressBinding.TYPE)",
      flags = 7
   ), @NiagaraProperty(
      name = "databaseRevision",
      type = "int",
      defaultValue = "1",
      flags = 1
   ), @NiagaraProperty(
      name = "configurationFiles",
      type = "BBacnetArray",
      defaultValue = "new BBacnetArray(BBacnetObjectIdentifier.TYPE)",
      flags = 5
   ), @NiagaraProperty(
      name = "lastRestoreTime",
      type = "BBacnetTimeStamp",
      defaultValue = "new BBacnetTimeStamp(new BBacnetDateTime())",
      flags = 1
   ), @NiagaraProperty(
      name = "backupFailureTimeout",
      type = "BRelTime",
      defaultValue = "BRelTime.makeSeconds(180)",
      facets = {@Facet(
         name = "BFacets.SHOW_MILLISECONDS",
         value = "false"
      ), @Facet(
         name = "BFacets.MIN",
         value = "BRelTime.DEFAULT"
      )}
   ), @NiagaraProperty(
      name = "backupPreparationTime",
      type = "BRelTime",
      defaultValue = "BRelTime.make(60000)",
      flags = 1,
      facets = {@Facet(
         name = "BFacets.SHOW_MILLISECONDS",
         value = "true"
      ), @Facet(
         name = "BFacets.MIN",
         value = "BRelTime.DEFAULT"
      )}
   ), @NiagaraProperty(
      name = "restorePreparationTime",
      type = "BRelTime",
      defaultValue = "BRelTime.make(60000)",
      flags = 1,
      facets = {@Facet(
         name = "BFacets.SHOW_MILLISECONDS",
         value = "true"
      ), @Facet(
         name = "BFacets.MIN",
         value = "BRelTime.DEFAULT"
      )}
   ), @NiagaraProperty(
      name = "restoreCompletionTime",
      type = "BRelTime",
      defaultValue = "BRelTime.make(180000)",
      flags = 1,
      facets = {@Facet(
         name = "BFacets.SHOW_MILLISECONDS",
         value = "true"
      ), @Facet(
         name = "BFacets.MIN",
         value = "BRelTime.DEFAULT"
      )}
   ), @NiagaraProperty(
      name = "backupAndRestoreState",
      type = "BBacnetBackupState",
      defaultValue = "BBacnetBackupState.DEFAULT",
      flags = 3
   ), @NiagaraProperty(
      name = "activeCovSubscriptions",
      type = "BBacnetListOf",
      defaultValue = "new BBacnetListOf(BBacnetCovSubscription.TYPE)",
      flags = 7
   ), @NiagaraProperty(
      name = "characterSet",
      type = "BCharacterSetEncoding",
      defaultValue = "BCharacterSetEncoding.iso10646_UTF8"
   ), @NiagaraProperty(
      name = "enumerationList",
      type = "BExtensibleEnumList",
      defaultValue = "new BExtensibleEnumList()"
   ), @NiagaraProperty(
      name = "exportTable",
      type = "BComponent",
      defaultValue = "new BBacnetExportTable()"
   ), @NiagaraProperty(
      name = "virtual",
      type = "BLocalBacnetVirtualGateway",
      defaultValue = "new BLocalBacnetVirtualGateway()",
      flags = 4
   ), @NiagaraProperty(
      name = "covPropertyPollRate",
      type = "BRelTime",
      defaultValue = "BRelTime.makeSeconds(5)",
      flags = 4
   ), @NiagaraProperty(
      name = "timeSynchronizationRecipients",
      type = "BBacnetListOf",
      defaultValue = "new BBacnetListOf(BBacnetRecipient.TYPE)"
   ), @NiagaraProperty(
      name = "timeSynchronizationInterval",
      type = "BRelTime",
      defaultValue = "BRelTime.make(86400000)",
      facets = {@Facet(
         name = "BFacets.SHOW_SECONDS",
         value = "false"
      ), @Facet(
         name = "BFacets.MIN",
         value = "BRelTime.DEFAULT"
      )}
   ), @NiagaraProperty(
      name = "alignIntervals",
      type = "boolean",
      defaultValue = "true"
   ), @NiagaraProperty(
      name = "intervalOffset",
      type = "int",
      defaultValue = "0",
      facets = {@Facet(
         name = "BFacets.UNITS",
         value = "BUnit.getUnit(\"minute\")"
      ), @Facet(
         name = "BFacets.MIN",
         value = "0"
      ), @Facet(
         name = "BFacets.MAX",
         value = "1440"
      )}
   ), @NiagaraProperty(
      name = "utcTimeSynchronizationRecipients",
      type = "BBacnetListOf",
      defaultValue = "new BBacnetListOf(BBacnetRecipient.TYPE)"
   ), @NiagaraProperty(
      name = "lastRestartReason",
      type = "BBacnetRestartReason",
      defaultValue = "BBacnetRestartReason.unknown"
   ), @NiagaraProperty(
      name = "timeOfDeviceRestart",
      type = "BBacnetTimeStamp",
      defaultValue = "new BBacnetTimeStamp(BAbsTime.make())",
      flags = 2
   ), @NiagaraProperty(
      name = "restartNotificationRecipients",
      type = "BBacnetListOf",
      defaultValue = "new BBacnetListOf(BBacnetRecipient.TYPE)"
   )})
@NiagaraActions({@NiagaraAction(
      name = "sendIAm"
   ), @NiagaraAction(
      name = "setBackupMode",
      parameterType = "BBoolean",
      defaultValue = "BBoolean.FALSE",
      flags = 4
   ), @NiagaraAction(
      name = "setRestoreMode",
      parameterType = "BBoolean",
      defaultValue = "BBoolean.FALSE",
      flags = 4
   ), @NiagaraAction(
      name = "println",
      parameterType = "BString",
      defaultValue = "BString.make(\"\")"
   ), @NiagaraAction(
      name = "sendTimeSynch",
      flags = 4
   ), @NiagaraAction(
      name = "checkDuplicates",
      flags = 4
   ), @NiagaraAction(
      name = "sendRestartNotifications",
      flags = 4
   ), @NiagaraAction(
      name = "changeDeviceUuid",
      parameterType = "BUuid",
      defaultValue = "BUuid.NULL",
      flags = 128
   )})
public class BLocalBacnetDevice extends BComponent implements BIBacnetExportObject, BIBacnetObjectContainer, BacnetPropertyListProvider {
   private static final BBacnetBitString SERVER_OBJECT_TYPES_SUPPORTED = BBacnetBitString.make(
      new boolean[]{
         true,
         true,
         true,
         true,
         true,
         true,
         true,
         false,
         true,
         true,
         true,
         false,
         true,
         true,
         true,
         true,
         false,
         true,
         false,
         true,
         true,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         true,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         false,
         true,
         false,
         false,
         false,
         false,
         true,
         true,
         false,
         true,
         false,
         false,
         false,
         false,
         false,
         false
      }
   );
   private static final BBacnetBitString SERVER_SERVICES_SUPPORTED = BBacnetBitString.make(
      new boolean[]{
         true,
         true,
         true,
         true,
         true,
         true,
         true,
         true,
         true,
         true,
         true,
         true,
         true,
         false,
         true,
         true,
         true,
         true,
         true,
         false,
         true,
         false,
         false,
         false,
         false,
         false,
         true,
         true,
         true,
         true,
         true,
         false,
         true,
         true,
         true,
         true,
         true,
         false,
         true,
         true,
         false
      }
   );
   public static final Property status = newProperty(67, BStatus.ok, null);
   public static final Property faultCause = newProperty(3, "", null);
   public static final Property objectId = newProperty(0, BBacnetObjectIdentifier.make(8), BBacnetObjectType.getObjectIdFacets(8));
   public static final Property systemStatus = newProperty(3, BBacnetDeviceStatus.operational, null);
   public static final Property vendorName = newProperty(1, "Tridium", null);
   public static final Property vendorId = newProperty(1, 36, null);
   public static final Property modelName = newProperty(1, "Niagara4 Station", null);
   public static final Property firmwareRevision = newProperty(1, "unknown", null);
   public static final Property applicationSoftwareVersion = newProperty(1, "unknown", null);
   public static final Property location = newProperty(0, "unknown", null);
   public static final Property description = newProperty(0, "Local BACnet Device object", null);
   public static final Property deviceUuid = newProperty(65, BUuid.DEFAULT, BFacets.make("security", true));
   public static final Property protocolVersion = newProperty(1, 1, null);
   public static final Property protocolRevision = newProperty(1, 14, null);
   public static final Property protocolConformanceClass = newProperty(5, 3, null);
   public static final Property protocolServicesSupported = newProperty(1, SERVER_SERVICES_SUPPORTED, BacnetBitStringUtil.BACNET_SERVICES_SUPPORTED_FACETS);
   public static final Property protocolObjectTypesSupported = newProperty(
      1, SERVER_OBJECT_TYPES_SUPPORTED, BacnetBitStringUtil.BACNET_OBJECT_TYPES_SUPPORTED_FACETS
   );
   public static final Property maxAPDULengthAccepted = newProperty(1, 1476, null);
   public static final Property segmentationSupported = newProperty(1, BBacnetSegmentation.segmentedBoth, null);
   public static final Property maxSegmentsAccepted = newProperty(1, 255, null);
   public static final Property apduSegmentTimeout = newProperty(
      0, 2000, BFacets.make(BFacets.make("units", BUnit.getUnit("millisecond")), BFacets.make("min", 0))
   );
   public static final Property apduTimeout = newProperty(0, 3000, BFacets.make(BFacets.make("units", BUnit.getUnit("millisecond")), BFacets.make("min", 0)));
   public static final Property numberOfApduRetries = newProperty(0, 3, null);
   public static final Property deviceAddressBinding = newProperty(7, new BBacnetListOf(BBacnetAddressBinding.TYPE), null);
   public static final Property databaseRevision = newProperty(1, 1, null);
   public static final Property configurationFiles = newProperty(5, new BBacnetArray(BBacnetObjectIdentifier.TYPE), null);
   public static final Property lastRestoreTime = newProperty(1, new BBacnetTimeStamp(new BBacnetDateTime()), null);
   public static final Property backupFailureTimeout = newProperty(
      0, BRelTime.makeSeconds(180), BFacets.make(BFacets.make("showMilliseconds", false), BFacets.make("min", BRelTime.DEFAULT))
   );
   public static final Property backupPreparationTime = newProperty(
      1, BRelTime.make(60000L), BFacets.make(BFacets.make("showMilliseconds", true), BFacets.make("min", BRelTime.DEFAULT))
   );
   public static final Property restorePreparationTime = newProperty(
      1, BRelTime.make(60000L), BFacets.make(BFacets.make("showMilliseconds", true), BFacets.make("min", BRelTime.DEFAULT))
   );
   public static final Property restoreCompletionTime = newProperty(
      1, BRelTime.make(180000L), BFacets.make(BFacets.make("showMilliseconds", true), BFacets.make("min", BRelTime.DEFAULT))
   );
   public static final Property backupAndRestoreState = newProperty(3, BBacnetBackupState.DEFAULT, null);
   public static final Property activeCovSubscriptions = newProperty(7, new BBacnetListOf(BBacnetCovSubscription.TYPE), null);
   public static final Property characterSet = newProperty(0, BCharacterSetEncoding.iso10646_UTF8, null);
   public static final Property enumerationList = newProperty(0, new BExtensibleEnumList(), null);
   public static final Property exportTable = newProperty(0, new BBacnetExportTable(), null);
   public static final Property virtual = newProperty(4, new BLocalBacnetVirtualGateway(), null);
   public static final Property covPropertyPollRate = newProperty(4, BRelTime.makeSeconds(5), null);
   public static final Property timeSynchronizationRecipients = newProperty(0, new BBacnetListOf(BBacnetRecipient.TYPE), null);
   public static final Property timeSynchronizationInterval = newProperty(
      0, BRelTime.make(86400000L), BFacets.make(BFacets.make("showSeconds", false), BFacets.make("min", BRelTime.DEFAULT))
   );
   public static final Property alignIntervals = newProperty(0, true, null);
   public static final Property intervalOffset = newProperty(
      0, 0, BFacets.make(BFacets.make(BFacets.make("units", BUnit.getUnit("minute")), BFacets.make("min", 0)), BFacets.make("max", 1440))
   );
   public static final Property utcTimeSynchronizationRecipients = newProperty(0, new BBacnetListOf(BBacnetRecipient.TYPE), null);
   public static final Property lastRestartReason = newProperty(0, BBacnetRestartReason.unknown, null);
   public static final Property timeOfDeviceRestart = newProperty(2, new BBacnetTimeStamp(BAbsTime.make()), null);
   public static final Property restartNotificationRecipients = newProperty(0, new BBacnetListOf(BBacnetRecipient.TYPE), null);
   public static final Action sendIAm = newAction(0, null);
   public static final Action setBackupMode = newAction(4, BBoolean.FALSE, null);
   public static final Action setRestoreMode = newAction(4, BBoolean.FALSE, null);
   public static final Action println = newAction(0, BString.make(""), null);
   public static final Action sendTimeSynch = newAction(4, null);
   public static final Action checkDuplicates = newAction(4, null);
   public static final Action sendRestartNotifications = newAction(4, null);
   public static final Action changeDeviceUuid = newAction(128, BUuid.NULL, null);
   public static final Type TYPE = Sys.loadType(BLocalBacnetDevice.class);
   private boolean fatalFault = false;
   private volatile boolean brandPropertiesRead = false;
   private static final BIcon icon = BIcon.std("deviceLocal.png");
   private static final Lexicon lex = Lexicon.make("bacnet");
   private static final int[] REQUIRED_PROPS = new int[]{75, 77, 79, 112, 121, 120, 70, 44, 12, 98, 139, 97, 96, 76, 62, 107, 11, 73, 30, 155};
   public static final String LAST_RESTORE_TIME_FILENAME = "~backups/lastRestoreTime";
   public static final String OBJECT_NAME_OVERRIDE_SLOTNAME = "objectName";
   private static final Logger log = Logger.getLogger("bacnet.server");
   private static final Logger loggerBacnetTransport = Logger.getLogger("bacnet.transport");
   private static final AsnInputStream asnIn = new AsnInputStream();
   private static final AsnOutputStream asnOut = new AsnOutputStream();
   private BacnetCovSubscriber covSubscriber = new BacnetCovSubscriber();
   private final BIBacnetExportObject.ObjectSubscriber objectSubscriber = new BIBacnetExportObject.ObjectSubscriber();
   static BasicContext bacnetContext;
   private int maxWaitTime = 0;
   private String objectName = "";
   private BBacnetDeviceStatus preBackupRestoreStatus = BBacnetDeviceStatus.operational;
   private final LocalBacnetCovPropPoll covPropPoller = new LocalBacnetCovPropPoll(this);
   private Ticket tsTicket = null;
   private final Object TIME_SYNC_LOCK = new Object();
   private BAbsTime lastTSTime = null;
   private static final boolean allowObjectIdWrite = false;
   private static final BRelTime CHECK_DUP_DELAY = BRelTime.makeSeconds(5);
   private Ticket checkDupTicket = null;
   private final Object CHECK_DUP_LOCK = new Object();
   private static final String SERIAL_NUMBER = Nre.getHostId();
   private final SaveListener saveListener = new SaveListener() {
      public void stationSave() {
         BLocalBacnetDevice.this.setDatabaseRevision(BLocalBacnetDevice.this.getDatabaseRevision() + 1);
      }

      public void stationSaveOk() {
      }

      public void stationSaveFail(String cause) {
      }

      @Override
      public String toString() {
         return "Local BACnet Device " + BLocalBacnetDevice.this.getNavOrd();
      }
   };

   @Override
   public BStatus getStatus() {
      return (BStatus)this.get(status);
   }

   public void setStatus(BStatus v) {
      this.set(status, v, null);
   }

   public String getFaultCause() {
      return this.getString(faultCause);
   }

   public void setFaultCause(String v) {
      this.setString(faultCause, v, null);
   }

   @Override
   public BBacnetObjectIdentifier getObjectId() {
      return (BBacnetObjectIdentifier)this.get(objectId);
   }

   @Override
   public void setObjectId(BBacnetObjectIdentifier v) {
      this.set(objectId, v, null);
   }

   public BBacnetDeviceStatus getSystemStatus() {
      return (BBacnetDeviceStatus)this.get(systemStatus);
   }

   public void setSystemStatus(BBacnetDeviceStatus v) {
      this.set(systemStatus, v, null);
   }

   public String getVendorName() {
      return this.getString(vendorName);
   }

   public void setVendorName(String v) {
      this.setString(vendorName, v, null);
   }

   public int getVendorId() {
      return this.getInt(vendorId);
   }

   public void setVendorId(int v) {
      this.setInt(vendorId, v, null);
   }

   public String getModelName() {
      return this.getString(modelName);
   }

   public void setModelName(String v) {
      this.setString(modelName, v, null);
   }

   public String getFirmwareRevision() {
      return this.getString(firmwareRevision);
   }

   public void setFirmwareRevision(String v) {
      this.setString(firmwareRevision, v, null);
   }

   public String getApplicationSoftwareVersion() {
      return this.getString(applicationSoftwareVersion);
   }

   public void setApplicationSoftwareVersion(String v) {
      this.setString(applicationSoftwareVersion, v, null);
   }

   public String getLocation() {
      return this.getString(location);
   }

   public void setLocation(String v) {
      this.setString(location, v, null);
   }

   public String getDescription() {
      return this.getString(description);
   }

   public void setDescription(String v) {
      this.setString(description, v, null);
   }

   public BUuid getDeviceUuid() {
      return (BUuid)this.get(deviceUuid);
   }

   public void setDeviceUuid(BUuid v) {
      this.set(deviceUuid, v, null);
   }

   public int getProtocolVersion() {
      return this.getInt(protocolVersion);
   }

   public void setProtocolVersion(int v) {
      this.setInt(protocolVersion, v, null);
   }

   public int getProtocolRevision() {
      return this.getInt(protocolRevision);
   }

   public void setProtocolRevision(int v) {
      this.setInt(protocolRevision, v, null);
   }

   public int getProtocolConformanceClass() {
      return this.getInt(protocolConformanceClass);
   }

   public void setProtocolConformanceClass(int v) {
      this.setInt(protocolConformanceClass, v, null);
   }

   public BBacnetBitString getProtocolServicesSupported() {
      return (BBacnetBitString)this.get(protocolServicesSupported);
   }

   public void setProtocolServicesSupported(BBacnetBitString v) {
      this.set(protocolServicesSupported, v, null);
   }

   public BBacnetBitString getProtocolObjectTypesSupported() {
      return (BBacnetBitString)this.get(protocolObjectTypesSupported);
   }

   public void setProtocolObjectTypesSupported(BBacnetBitString v) {
      this.set(protocolObjectTypesSupported, v, null);
   }

   public int getMaxAPDULengthAccepted() {
      return this.getInt(maxAPDULengthAccepted);
   }

   public void setMaxAPDULengthAccepted(int v) {
      this.setInt(maxAPDULengthAccepted, v, null);
   }

   public BBacnetSegmentation getSegmentationSupported() {
      return (BBacnetSegmentation)this.get(segmentationSupported);
   }

   public void setSegmentationSupported(BBacnetSegmentation v) {
      this.set(segmentationSupported, v, null);
   }

   public int getMaxSegmentsAccepted() {
      return this.getInt(maxSegmentsAccepted);
   }

   public void setMaxSegmentsAccepted(int v) {
      this.setInt(maxSegmentsAccepted, v, null);
   }

   public int getApduSegmentTimeout() {
      return this.getInt(apduSegmentTimeout);
   }

   public void setApduSegmentTimeout(int v) {
      this.setInt(apduSegmentTimeout, v, null);
   }

   public int getApduTimeout() {
      return this.getInt(apduTimeout);
   }

   public void setApduTimeout(int v) {
      this.setInt(apduTimeout, v, null);
   }

   public int getNumberOfApduRetries() {
      return this.getInt(numberOfApduRetries);
   }

   public void setNumberOfApduRetries(int v) {
      this.setInt(numberOfApduRetries, v, null);
   }

   public BBacnetListOf getDeviceAddressBinding() {
      return (BBacnetListOf)this.get(deviceAddressBinding);
   }

   public void setDeviceAddressBinding(BBacnetListOf v) {
      this.set(deviceAddressBinding, v, null);
   }

   public int getDatabaseRevision() {
      return this.getInt(databaseRevision);
   }

   public void setDatabaseRevision(int v) {
      this.setInt(databaseRevision, v, null);
   }

   public BBacnetArray getConfigurationFiles() {
      return (BBacnetArray)this.get(configurationFiles);
   }

   public void setConfigurationFiles(BBacnetArray v) {
      this.set(configurationFiles, v, null);
   }

   public BBacnetTimeStamp getLastRestoreTime() {
      return (BBacnetTimeStamp)this.get(lastRestoreTime);
   }

   public void setLastRestoreTime(BBacnetTimeStamp v) {
      this.set(lastRestoreTime, v, null);
   }

   public BRelTime getBackupFailureTimeout() {
      return (BRelTime)this.get(backupFailureTimeout);
   }

   public void setBackupFailureTimeout(BRelTime v) {
      this.set(backupFailureTimeout, v, null);
   }

   public BRelTime getBackupPreparationTime() {
      return (BRelTime)this.get(backupPreparationTime);
   }

   public void setBackupPreparationTime(BRelTime v) {
      this.set(backupPreparationTime, v, null);
   }

   public BRelTime getRestorePreparationTime() {
      return (BRelTime)this.get(restorePreparationTime);
   }

   public void setRestorePreparationTime(BRelTime v) {
      this.set(restorePreparationTime, v, null);
   }

   public BRelTime getRestoreCompletionTime() {
      return (BRelTime)this.get(restoreCompletionTime);
   }

   public void setRestoreCompletionTime(BRelTime v) {
      this.set(restoreCompletionTime, v, null);
   }

   public BBacnetBackupState getBackupAndRestoreState() {
      return (BBacnetBackupState)this.get(backupAndRestoreState);
   }

   public void setBackupAndRestoreState(BBacnetBackupState v) {
      this.set(backupAndRestoreState, v, null);
   }

   public BBacnetListOf getActiveCovSubscriptions() {
      return (BBacnetListOf)this.get(activeCovSubscriptions);
   }

   public void setActiveCovSubscriptions(BBacnetListOf v) {
      this.set(activeCovSubscriptions, v, null);
   }

   public BCharacterSetEncoding getCharacterSet() {
      return (BCharacterSetEncoding)this.get(characterSet);
   }

   public void setCharacterSet(BCharacterSetEncoding v) {
      this.set(characterSet, v, null);
   }

   public BExtensibleEnumList getEnumerationList() {
      return (BExtensibleEnumList)this.get(enumerationList);
   }

   public void setEnumerationList(BExtensibleEnumList v) {
      this.set(enumerationList, v, null);
   }

   public BComponent getExportTable() {
      return (BComponent)this.get(exportTable);
   }

   public void setExportTable(BComponent v) {
      this.set(exportTable, v, null);
   }

   public BLocalBacnetVirtualGateway getVirtual() {
      return (BLocalBacnetVirtualGateway)this.get(virtual);
   }

   public void setVirtual(BLocalBacnetVirtualGateway v) {
      this.set(virtual, v, null);
   }

   public BRelTime getCovPropertyPollRate() {
      return (BRelTime)this.get(covPropertyPollRate);
   }

   public void setCovPropertyPollRate(BRelTime v) {
      this.set(covPropertyPollRate, v, null);
   }

   public BBacnetListOf getTimeSynchronizationRecipients() {
      return (BBacnetListOf)this.get(timeSynchronizationRecipients);
   }

   public void setTimeSynchronizationRecipients(BBacnetListOf v) {
      this.set(timeSynchronizationRecipients, v, null);
   }

   public BRelTime getTimeSynchronizationInterval() {
      return (BRelTime)this.get(timeSynchronizationInterval);
   }

   public void setTimeSynchronizationInterval(BRelTime v) {
      this.set(timeSynchronizationInterval, v, null);
   }

   public boolean getAlignIntervals() {
      return this.getBoolean(alignIntervals);
   }

   public void setAlignIntervals(boolean v) {
      this.setBoolean(alignIntervals, v, null);
   }

   public int getIntervalOffset() {
      return this.getInt(intervalOffset);
   }

   public void setIntervalOffset(int v) {
      this.setInt(intervalOffset, v, null);
   }

   public BBacnetListOf getUtcTimeSynchronizationRecipients() {
      return (BBacnetListOf)this.get(utcTimeSynchronizationRecipients);
   }

   public void setUtcTimeSynchronizationRecipients(BBacnetListOf v) {
      this.set(utcTimeSynchronizationRecipients, v, null);
   }

   public BBacnetRestartReason getLastRestartReason() {
      return (BBacnetRestartReason)this.get(lastRestartReason);
   }

   public void setLastRestartReason(BBacnetRestartReason v) {
      this.set(lastRestartReason, v, null);
   }

   public BBacnetTimeStamp getTimeOfDeviceRestart() {
      return (BBacnetTimeStamp)this.get(timeOfDeviceRestart);
   }

   public void setTimeOfDeviceRestart(BBacnetTimeStamp v) {
      this.set(timeOfDeviceRestart, v, null);
   }

   public BBacnetListOf getRestartNotificationRecipients() {
      return (BBacnetListOf)this.get(restartNotificationRecipients);
   }

   public void setRestartNotificationRecipients(BBacnetListOf v) {
      this.set(restartNotificationRecipients, v, null);
   }

   public void sendIAm() {
      this.invoke(sendIAm, null, null);
   }

   public void setBackupMode(BBoolean parameter) {
      this.invoke(setBackupMode, parameter, null);
   }

   public void setRestoreMode(BBoolean parameter) {
      this.invoke(setRestoreMode, parameter, null);
   }

   public void println(BString parameter) {
      this.invoke(println, parameter, null);
   }

   public void sendTimeSynch() {
      this.invoke(sendTimeSynch, null, null);
   }

   public void checkDuplicates() {
      this.invoke(checkDuplicates, null, null);
   }

   public void sendRestartNotifications() {
      this.invoke(sendRestartNotifications, null, null);
   }

   public void changeDeviceUuid(BUuid parameter) {
      this.invoke(changeDeviceUuid, parameter, null);
   }

   public Type getType() {
      return TYPE;
   }

   public String toString(Context c) {
      return "Local Bacnet Device [" + this.getObjectId() + "]";
   }

   public void started() throws Exception {
      super.started();
      this.checkFatalFault();
      Type type = BBacnetNetwork.bacnet().getType();
      this.setFirmwareRevision(type.getVendorVersion().toString());
      this.setApplicationSoftwareVersion(type.getVendor() + " " + type.getVendorVersion());
      this.objectName = Sys.getStation().getStationName();
      if (!Sys.isStationStarted()) {
         this.incrementDatabaseRevision();
      }

      if (BUuid.DEFAULT.equals(this.getDeviceUuid())) {
         this.setDeviceUuid(BUuid.make());
      }

      this.setInt(protocolRevision, 16);
      this.checkConfiguration();
      this.network().postAsync(new Runnable() {
         @Override
         public void run() {
            BLocalBacnetDevice.this.readBrandProperties();
         }
      });
      this.linkTo("sendIAmLink", this, objectId, sendIAm);
      this.setFlags(this.getSlot("sendIAmLink"), this.getFlags(this.getSlot("sendIAmLink")) | 4);
      Station.addSaveListener(this.saveListener);

      try {
         getBacnetContext();
      } catch (Exception var3) {
      }

      this.maxWaitTime = this.getApduTimeout() * (this.getNumberOfApduRetries() + 1);
      boolean[] bits = this.getProtocolObjectTypesSupported().getBits();
      if (bits.length < 56) {
         bits = Arrays.copyOf(bits, 56);
         bits[55] = false;
         this.setProtocolObjectTypesSupported(BBacnetBitString.make(bits));
      }
   }

   public void descendantsStarted() {
      BFileSystem fs = BFileSystem.INSTANCE;
      FilePath path = new FilePath("~backups/lastRestoreTime");
      AccessController.doPrivileged((PrivilegedAction<Void>)(() -> {
         BIFile lastRestoreFile = fs.findFile(path);
         if (lastRestoreFile != null) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(lastRestoreFile.getInputStream(), StandardCharsets.UTF_8))) {
               String s = br.readLine();
               BAbsTime t = BAbsTime.make(s);
               BBacnetTimeStamp ts = new BBacnetTimeStamp(t);
               this.setLastRestoreTime(ts);
               s = br.readLine();
               int dbRev = Integer.parseInt(s);
               this.setDatabaseRevision(dbRev + 1);
            } catch (IOException var22) {
               log.log(Level.SEVERE, "IOException occurred reading last restore time file in descendantsStarted", (Throwable)var22);
            }

            try {
               lastRestoreFile.delete();
            } catch (IOException var18) {
               log.log(Level.SEVERE, "IOException occurred deleting last restore time file in descendantsStarted", (Throwable)var18);
            }
         }

         return null;
      }));
   }

   public void stopped() {
      Station.removeSaveListener(this.saveListener);
      this.covSubscriber.unsubscribeAll();
      this.covSubscriber = null;
      if (this.tsTicket != null) {
         this.tsTicket.cancel();
      }

      this.tsTicket = null;
      this.lastTSTime = null;
   }

   public void added(Property p, Context cx) {
      super.added(p, cx);
      if (this.isRunning()) {
         if ("objectName".equals(p.getName())) {
            log.fine("LocalBacnetDevice: added new property slot \"objectName\" to override the Object_Name property");
            this.incrementDatabaseRevision();
         }
      }
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning()) {
         if ("objectName".equals(p.getName())) {
            this.incrementDatabaseRevision();
         } else if (p.equals(objectId)) {
            this.checkConfiguration();
            if (this.getStatus().isOk()) {
               BBacnetNetwork.localDevice().incrementDatabaseRevision();
            }
         } else if (p.equals(apduTimeout) || p.equals(numberOfApduRetries)) {
            this.maxWaitTime = this.getApduTimeout() * (this.getNumberOfApduRetries() + 1);
            BBacnetTransportLayer transport = ((BBacnetStack)((BBacnetNetwork)this.getParent()).getBacnetComm()).getTransport();
            long lockup = transport.getLockupThreshold().getMillis();
            if (this.maxWaitTime > lockup) {
               loggerBacnetTransport.info("Reconfiguring Transport layer lockup threshold...");
               transport.set(BBacnetTransportLayer.lockupThreshold, BRelTime.make(this.maxWaitTime), BacnetConst.fallback);
            }
         }

         if (p.equals(timeSynchronizationInterval) || p.equals(alignIntervals) || p.equals(intervalOffset)) {
            this.scheduleTimeSynch();
         }

         if (p.equals(timeSynchronizationRecipients)) {
            this.checkRecipients(p);
         } else if (p.equals(utcTimeSynchronizationRecipients)) {
            this.checkRecipients(p);
         }
      }
   }

   public void removed(Property p, BValue oldValue, Context cx) {
      super.removed(p, oldValue, cx);
      if (this.isRunning()) {
         if ("objectName".equals(p.getName())) {
            log.fine("LocalBacnetDevice: removed the property slot \"objectName\"");
            this.incrementDatabaseRevision();
         }
      }
   }

   public void atSteadyState() throws Exception {
      super.atSteadyState();
      this.checkRecipients(timeSynchronizationRecipients);
      this.checkRecipients(utcTimeSynchronizationRecipients);
      this.sendTimeSynch();
      this.scheduleTimeSynch();
      this.setTimeOfDeviceRestart(new BBacnetTimeStamp(BAbsTime.make()));
      this.sendRestartNotifications();
   }

   public void clockChanged(BRelTime shift) throws Exception {
      this.sendTimeSynch();
      this.scheduleTimeSynch();
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BBacnetNetwork;
   }

   public BINavNode[] getNavChildren() {
      BINavNode[] kids = super.getNavChildren();
      Array<BINavNode> acc = new Array(BINavNode.class);

      for (int i = 0; i < kids.length; i++) {
         BComponent kid = (BComponent)kids[i];
         if (!kid.getType().is(BBacnetListOf.TYPE) && !kid.getType().is(BBacnetArray.TYPE)) {
            acc.add(kid);
         }
      }

      return (BINavNode[])acc.trim();
   }

   public UUID getUuid() {
      BUuid bUuid = this.getDeviceUuid();
      long msb = bUuid.getMostSignificant();
      long lsb = bUuid.getLeastSignificant();
      return new UUID(msb, lsb);
   }

   @Override
   public final BObject getObject() {
      return this;
   }

   @Override
   public final BOrd getObjectOrd() {
      return this.getOrdInSession();
   }

   @Override
   public final void setObjectOrd(BOrd objectOrd, Context cx) {
      throw new UnsupportedOperationException(lex.getText("UnsupportedOperationException.localDevice.setObjectOrd"));
   }

   @Override
   public String getObjectName() {
      BValue objName = this.get("objectName");
      if (objName instanceof BString) {
         String name = ((BString)objName).getString();
         if (name.length() > 0) {
            return name;
         }
      }

      return this.objectName + "_" + this.getObjectId().getInstanceNumber();
   }

   @Override
   public void setObjectName(String objectName) {
      throw new UnsupportedOperationException(lex.getText("UnsupportedOperationException.localDevice.setObjectName"));
   }

   @Override
   public void checkConfiguration() {
      if (this.isFatalFault()) {
         this.setStatus(BStatus.makeFault(this.getStatus(), true));
      } else {
         boolean configOk = true;
         if (!this.getObjectId().isValid()) {
            this.setFaultCause("Invalid Object ID");
            configOk = false;
         }

         if (Sys.getStation() != null) {
            BIBacnetExportObject o = BBacnetNetwork.localDevice().lookupBacnetObject(this.getObjectName());
            if (o != this) {
               this.setFaultCause("Duplicate Object_Name");
               configOk = false;
            }
         }

         if (configOk) {
            this.setFaultCause("");
         }

         this.setStatus(BStatus.makeFault(this.getStatus(), !configOk));
      }
   }

   public String export(BIBacnetExportObject object) {
      return this.exports().export(object);
   }

   public String exportByOrd(BIBacnetExportObject object) {
      return this.exports().exportByOrd(object);
   }

   public void unexport(BBacnetObjectIdentifier objectId, String objectName, BIBacnetExportObject object) {
      this.exports().unexport(objectId, objectName, object);
      if (this.isRunning()) {
         this.checkDuplicates(object);
      }
   }

   private void checkDuplicates(BIBacnetExportObject exclude) {
      synchronized (this.CHECK_DUP_LOCK) {
         if (this.checkDupTicket != null) {
            this.checkDupTicket.cancel();
         }

         this.checkDupTicket = Clock.schedule(this, CHECK_DUP_DELAY, checkDuplicates, null);
      }
   }

   public void doCheckDuplicates() {
      ComponentTreeCursor c = new ComponentTreeCursor(this.exports(), null);
      BIBacnetExportObject e = null;

      while (c.next(BIBacnetExportObject.class)) {
         e = (BIBacnetExportObject)c.get();
         if (e.getStatus().isFault()) {
            e.checkConfiguration();
         }
      }
   }

   private BBacnetExportTable exports() {
      return (BBacnetExportTable)this.getExportTable();
   }

   public final BIBacnetExportObject lookupBacnetObject(BBacnetObjectIdentifier objectId) {
      return (BIBacnetExportObject)(this.getObjectId().equals(objectId) ? this : this.exports().byObjectId(objectId));
   }

   public final BIBacnetExportObject lookupBacnetObject(String objectName) {
      return (BIBacnetExportObject)(this.getObjectName().equals(objectName) ? this : this.exports().byObjectName(objectName));
   }

   public final BBacnetObjectIdentifier lookupBacnetObjectId(BOrd objectOrd) {
      return this.getObjectOrd().equals(objectOrd) ? this.getObjectId() : this.exports().byObjectOrd(objectOrd);
   }

   public final int getNextInstance(int objectType) {
      return this.exports().getNextInstance(objectType);
   }

   public final void incrementDatabaseRevision() {
      this.setDatabaseRevision(this.getDatabaseRevision() + 1);
   }

   @Override
   public final BObject lookupBacnetObject(BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex, String domain) {
      return (BObject)this.lookupBacnetObject(objectId);
   }

   public void doSendIAm() {
      ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm()).getServer().iAm();
   }

   public void doSetBackupMode(BBoolean backupMode) {
      if (this.getSystemStatus().getOrdinal() == 3) {
         throw new IllegalStateException("Cannot modify backup mode while restore is in progress");
      } else {
         if (backupMode.getBoolean()) {
            log.info("Entering Backup Mode...");
            this.preBackupRestoreStatus = this.getSystemStatus();
            this.setBackupAndRestoreState(BBacnetBackupState.preparingForBackup);
            this.setSystemStatus(BBacnetDeviceStatus.backupInProgress);
         } else {
            log.info("Exiting Backup Mode...");
            this.setBackupAndRestoreState(BBacnetBackupState.idle);
            this.setSystemStatus(this.preBackupRestoreStatus);
            ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm()).getServer().cleanupBackupMode();
         }
      }
   }

   public void doSetRestoreMode(BBoolean restoreMode) {
      if (this.getSystemStatus().getOrdinal() == 5) {
         throw new IllegalStateException("Cannot modify restore mode while backup is in progress");
      } else {
         if (restoreMode.getBoolean()) {
            log.info("Entering Restore Mode...");
            this.preBackupRestoreStatus = this.getSystemStatus();
            this.setBackupAndRestoreState(BBacnetBackupState.preparingForRestore);
            this.setSystemStatus(BBacnetDeviceStatus.downloadInProgress);
         } else {
            log.info("Exiting Restore Mode...");
            this.setBackupAndRestoreState(BBacnetBackupState.idle);
            this.setSystemStatus(this.preBackupRestoreStatus);
            ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm()).getServer().cleanupBackupMode();
         }
      }
   }

   public void doPrintln(BString arg) {
      System.out.println("\n\n********\n" + arg + "\n********\n");
   }

   public final void doSendTimeSynch() {
      this.lastTSTime = BAbsTime.make();
      if (log.isLoggable(Level.FINE)) {
         log.fine("Sending Automatic Time Synch...");
      }

      try {
         BBacnetClientLayer client = ((BBacnetStack)this.network().getBacnetComm()).getClient();
         SlotCursor<Property> c = this.getTimeSynchronizationRecipients().getProperties();
         BBacnetRecipient r = null;

         while (c.next(BBacnetRecipient.class)) {
            r = (BBacnetRecipient)c.get();
            if (!r.isDevice() || r.getDevice().isValid() && DeviceRegistry.getDeviceAddress(r.getDevice()) != null) {
               client.timeSynch(r);
            }
         }

         c = this.getUtcTimeSynchronizationRecipients().getProperties();

         while (c.next(BBacnetRecipient.class)) {
            r = (BBacnetRecipient)c.get();
            if (!r.isDevice() || r.getDevice().isValid() && DeviceRegistry.getDeviceAddress(r.getDevice()) != null) {
               client.utcTimeSynch(r);
            }
         }
      } catch (BacnetException var4) {
         log.log(Level.WARNING, "BacnetException sending time synch {" + var4 + "}", (Throwable)var4);
      }
   }

   public void doSendRestartNotifications() {
      SlotCursor<Property> c = this.getRestartNotificationRecipients().getProperties();
      BBacnetClientLayer client = ((BBacnetStack)this.network().getBacnetComm()).getClient();

      while (c.next(BBacnetRecipient.class)) {
         BBacnetRecipient r = (BBacnetRecipient)c.get();
         if (!r.isDevice() || r.getDevice().isValid() && DeviceRegistry.getDeviceAddress(r.getDevice()) != null) {
            try {
               client.deviceRestartNotification(r);
            } catch (BacnetException var5) {
               log.log(Level.WARNING, "BacnetException sending restart notification {" + var5 + "}", (Throwable)var5);
            }
         }
      }
   }

   public void doChangeDeviceUuid(BUuid uuid) {
      if (BUuid.NULL.equals(uuid)) {
         throw new LocalizableRuntimeException("bacnet", "localBacnetDevice.changeDeviceUuid.newValueNull");
      } else if (hasEnabledScPort()) {
         throw new LocalizableRuntimeException("bacnet", "localBacnetDevice.changeDeviceUuid.scPortEnabled");
      } else {
         this.setDeviceUuid(uuid);
      }
   }

   private static boolean hasEnabledScPort() {
      BBacnetStack comm = (BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm();
      BNetworkPort[] ports = (BNetworkPort[])comm.getNetwork().getChildren(BNetworkPort.class);

      for (BNetworkPort port : ports) {
         if (port.getLink() instanceof BScLinkLayer && port.getEnabled()) {
            return true;
         }
      }

      return false;
   }

   public final void addAddressBinding(BBacnetDevice device) {
      BBacnetAddress address = (BBacnetAddress)device.getAddress().newCopy();
      BBacnetNetworkLayer net = ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm()).getNetwork();
      if (net.isDirectlyConnectedNetwork(address.getNetworkNumber())) {
         address.setNetworkNumber(0);
      }

      BBacnetAddressBinding b = this.bindingById(device.getObjectId());
      if (b == null) {
         b = new BBacnetAddressBinding(device.getObjectId(), address);
         this.getDeviceAddressBinding().addListElement(b, null);
      } else {
         b.getDeviceAddress().copyFrom(address);
      }

      Flags.setAllReadonly(this.getDeviceAddressBinding(), true, null);
   }

   public final void removeAddressBinding(BBacnetDevice device) {
      BBacnetAddressBinding b = this.bindingById(device.getObjectId());
      if (b != null) {
         this.getDeviceAddressBinding().removeListElement(b, null);
      }
   }

   public final void updateAddressBinding(BBacnetObjectIdentifier oldId, BBacnetObjectIdentifier newId) {
      BBacnetAddressBinding b = this.bindingById(oldId);
      if (b != null) {
         b.setDeviceObjectId(newId);
      }
   }

   public final void updateAddressBinding(BBacnetAddress oldAddress, BBacnetAddress newAddress) {
      BBacnetAddress address = (BBacnetAddress)oldAddress.newCopy();
      BBacnetNetworkLayer net = ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm()).getNetwork();
      if (net.isDirectlyConnectedNetwork(address.getNetworkNumber())) {
         address.setNetworkNumber(0);
      }

      BBacnetAddressBinding b = this.bindingByAddress(address);
      if (b != null) {
         b.getDeviceAddress().copyFrom(newAddress);
      }
   }

   private BBacnetAddressBinding bindingById(BBacnetObjectIdentifier id) {
      SlotCursor<Property> sc = this.getDeviceAddressBinding().getProperties();

      while (sc.next(BBacnetAddressBinding.class)) {
         BBacnetAddressBinding b = (BBacnetAddressBinding)sc.get();
         if (b.getDeviceObjectId().equals(id)) {
            return b;
         }
      }

      return null;
   }

   private BBacnetAddressBinding bindingByAddress(BBacnetAddress address) {
      if (address == null) {
         return null;
      } else {
         byte[] mac = address.getMacAddress().getBytes();
         SlotCursor<Property> sc = this.getDeviceAddressBinding().getProperties();

         while (sc.next(BBacnetAddressBinding.class)) {
            BBacnetAddressBinding b = (BBacnetAddressBinding)sc.get();
            if (b.getDeviceAddress().macEquals(mac)) {
               return b;
            }
         }

         return null;
      }
   }

   @Override
   public final PropertyValue readProperty(PropertyReference ref) throws RejectException {
      return this.readProperty(ref.getPropertyId(), ref.getPropertyArrayIndex());
   }

   @Override
   public final PropertyValue[] readPropertyMultiple(PropertyReference[] refs) throws RejectException {
      ArrayList<PropertyValue> results = new ArrayList<>(refs.length);

      for (int i = 0; i < refs.length; i++) {
         switch (refs[i].getPropertyId()) {
            case 8:
               int[] props = REQUIRED_PROPS;
               int j = 0;

               for (; j < props.length; j++) {
                  if (checkPropertyForReadMultiple(props[j])) {
                     results.add(this.readProperty(props[j]));
                  }
               }

               props = this.getOptionalProps();

               for (int jx = 0; jx < props.length; jx++) {
                  results.add(this.readProperty(props[jx]));
               }
               break;
            case 80:
               int[] props = this.getOptionalProps();

               for (int jx = 0; jx < props.length; jx++) {
                  results.add(this.readProperty(props[jx]));
               }
               break;
            case 105:
               int[] props = REQUIRED_PROPS;

               for (int j = 0; j < props.length; j++) {
                  if (checkPropertyForReadMultiple(props[j])) {
                     results.add(this.readProperty(props[j]));
                  }
               }
               break;
            default:
               results.add(this.readProperty(refs[i].getPropertyId(), refs[i].getPropertyArrayIndex()));
         }
      }

      return results.toArray(new PropertyValue[0]);
   }

   private static boolean checkPropertyForReadMultiple(int property_id) {
      return property_id != 371;
   }

   @Override
   public final RangeData readRange(RangeReference rangeReference) throws RejectException {
      int propertyId = rangeReference.getPropertyId();
      if (!this.hasProperty(propertyId)) {
         return new ReadRangeAck(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty);
      } else if (propertyId != 30 && propertyId != 152 && propertyId != 202 && propertyId != 116 && propertyId != 206) {
         return new ReadRangeAck(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
      } else if (rangeReference.getPropertyArrayIndex() != -1) {
         return new ReadRangeAck(BBacnetErrorClass.property, BBacnetErrorCode.propertyIsNotAnArray);
      } else {
         switch (propertyId) {
            case 30:
               BBacnetAddressBinding[] addrList = (BBacnetAddressBinding[])this.getDeviceAddressBinding().getChildren(BBacnetAddressBinding.class);
               return this.readRange(rangeReference, addrList, 16);
            case 116:
               SlotCursor<Property> tsC = this.getTimeSynchronizationRecipients().getProperties();
               ArrayList<BBacnetRecipient> timeSyncOrdList = new ArrayList<>();
               int k = 0;

               try {
                  while (tsC.next(BBacnetRecipient.class)) {
                     timeSyncOrdList.add((BBacnetRecipient)tsC.get());
                  }

                  BBacnetRecipient[] timeSyncList = new BBacnetRecipient[timeSyncOrdList.size()];

                  for (k = 0; k < timeSyncOrdList.size(); k++) {
                     timeSyncList[k] = timeSyncOrdList.get(k);
                  }

                  return this.readRange(rangeReference, timeSyncList, 44);
               } catch (Exception var20) {
                  log.warning("Exception building TIME_SYNCHRONIZATION_RECIPIENTS[" + k + "] for ReadRange: " + var20);
                  return new ReadRangeAck(0, 25);
               }
            case 152:
               BOrd[] covOrdList = (BOrd[])this.getActiveCovSubscriptions().getChildren(BOrd.class);
               BBacnetCovSubscription[] covList = new BBacnetCovSubscription[covOrdList.length];
               int j = 0;

               try {
                  for (j = 0; j < covOrdList.length; j++) {
                     covList[j] = (BBacnetCovSubscription)covOrdList[j].get(this);
                  }

                  return this.readRange(rangeReference, covList, 44);
               } catch (Exception var19) {
                  log.warning("Exception building Active_COV_Subscriptions[" + j + "] for ReadRange: " + var19);
                  return new ReadRangeAck(0, 25);
               }
            case 202:
               SlotCursor<Property> rnC = this.getRestartNotificationRecipients().getProperties();
               ArrayList<BBacnetRecipient> rstPropsList = new ArrayList<>();
               int m = 0;

               try {
                  while (rnC.next(BBacnetRecipient.class)) {
                     rstPropsList.add((BBacnetRecipient)rnC.get());
                  }

                  BBacnetRecipient[] rstNotiList = new BBacnetRecipient[rstPropsList.size()];

                  for (m = 0; m < rstPropsList.size(); m++) {
                     rstNotiList[m] = rstPropsList.get(m);
                  }

                  return this.readRange(rangeReference, rstNotiList, 44);
               } catch (Exception var18) {
                  log.warning("Exception building RESTART_NOTIFICATION_RECIPIENTS[" + m + "] for ReadRange: " + var18);
                  return new ReadRangeAck(0, 25);
               }
            case 206:
               SlotCursor<Property> utsC = this.getUtcTimeSynchronizationRecipients().getProperties();
               ArrayList<BBacnetRecipient> utcTimeSyncOrdList = new ArrayList<>();
               int l = 0;

               try {
                  while (utsC.next(BBacnetRecipient.class)) {
                     utcTimeSyncOrdList.add((BBacnetRecipient)utsC.get());
                  }

                  BBacnetRecipient[] utcTimeSyncList = new BBacnetRecipient[utcTimeSyncOrdList.size()];

                  for (l = 0; l < utcTimeSyncOrdList.size(); l++) {
                     utcTimeSyncList[l] = utcTimeSyncOrdList.get(l);
                  }

                  return this.readRange(rangeReference, utcTimeSyncList, 44);
               } catch (Exception var17) {
                  log.warning("Exception building UTC_TIME_SYNCHRONIZATION_RECIPIENTS[" + l + "] for ReadRange: " + var17);
                  return new ReadRangeAck(0, 25);
               }
            default:
               return new ReadRangeAck(BBacnetErrorClass.property, BBacnetErrorCode.propertyIsNotA_List);
         }
      }
   }

   private boolean hasProperty(int propertyId) {
      for (int id : REQUIRED_PROPS) {
         if (id == propertyId) {
            return true;
         }
      }

      for (int idx : this.getOptionalProps()) {
         if (idx == propertyId) {
            return true;
         }
      }

      return propertyId == 371;
   }

   @Override
   public final ErrorType writeProperty(PropertyValue val) throws BacnetException {
      return this.writeProperty(val.getPropertyId(), val.getPropertyArrayIndex(), val.getPropertyValue(), val.getPriority());
   }

   @Override
   public final ChangeListError addListElements(PropertyValue val) throws BacnetException {
      return this.addListElements(val.getPropertyId(), val.getPropertyArrayIndex(), val.getPropertyValue());
   }

   @Override
   public final ChangeListError removeListElements(PropertyValue val) throws BacnetException {
      return this.removeListElements(val.getPropertyId(), val.getPropertyArrayIndex(), val.getPropertyValue());
   }

   public int getDeviceTimeout() {
      return this.maxWaitTime;
   }

   boolean isArray(int propertyId) {
      if (propertyId == 76) {
         return true;
      } else {
         return propertyId == 154 ? true : propertyId == 371;
      }
   }

   private PropertyValue readProperty(int pId) {
      return this.readProperty(pId, -1);
   }

   protected PropertyValue readProperty(int pId, int ndx) {
      if (ndx >= 0 && !this.isArray(pId)) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(2, 50));
      } else {
         synchronized (asnOut) {
            asnOut.reset();

            try {
               switch (pId) {
                  case 10:
                     asnOut.writeUnsignedInteger(this.getApduSegmentTimeout());
                     break;
                  case 11:
                     asnOut.writeUnsignedInteger(this.getApduTimeout());
                     break;
                  case 12:
                     asnOut.writeCharacterString(this.getApplicationSoftwareVersion(), this.getCharacterSet());
                     break;
                  case 24:
                     asnOut.writeBoolean(BAbsTime.make().inDaylightTime());
                     break;
                  case 28:
                     asnOut.writeCharacterString(this.getDescription(), this.getCharacterSet());
                     break;
                  case 30:
                     this.getDeviceAddressBinding().writeAsn(asnOut);
                     break;
                  case 44:
                     asnOut.writeCharacterString(this.getFirmwareRevision(), this.getCharacterSet());
                     break;
                  case 56:
                     BAbsTime t0 = BAbsTime.make();
                     int wd = t0.getWeekday().getOrdinal();
                     if (wd == 0) {
                        wd = 7;
                     }

                     asnOut.writeDate(t0.getYear() - 1900, t0.getMonth().getOrdinal() + 1, t0.getDay(), wd);
                     break;
                  case 57:
                     BAbsTime t = BAbsTime.make();
                     asnOut.writeTime(t.getHour(), t.getMinute(), t.getSecond(), t.getMillisecond() / 10);
                     break;
                  case 58:
                     asnOut.writeCharacterString(this.getLocation(), this.getCharacterSet());
                     break;
                  case 62:
                     asnOut.writeUnsignedInteger(this.getMaxAPDULengthAccepted());
                     break;
                  case 63:
                     BBacnetNetworkLayer net = ((BBacnetStack)((BBacnetNetwork)this.getParent()).getBacnetComm()).getNetwork();
                     SlotCursor<Property> sc = net.loadSlots().getProperties();

                     while (sc.next(BNetworkPort.class)) {
                        if (((BNetworkPort)sc.get()).getLink() instanceof BBacnetMstpLinkLayer) {
                           asnOut.writeUnsignedInteger(((BBacnetMstpLinkLayer)((BNetworkPort)sc.get()).getLink()).getMaxInfoFrames());
                           break;
                        }
                     }

                     if (asnOut.size() == 0) {
                        asnOut.writeUnsignedInteger(1L);
                     }
                     break;
                  case 64:
                     BBacnetNetworkLayer net = ((BBacnetStack)((BBacnetNetwork)this.getParent()).getBacnetComm()).getNetwork();
                     SlotCursor<Property> sc = net.loadSlots().getProperties();

                     while (sc.next(BNetworkPort.class)) {
                        if (((BNetworkPort)sc.get()).getLink() instanceof BBacnetMstpLinkLayer) {
                           asnOut.writeUnsignedInteger(((BBacnetMstpLinkLayer)((BNetworkPort)sc.get()).getLink()).getMaxMaster());
                           break;
                        }
                     }

                     if (asnOut.size() == 0) {
                        asnOut.writeUnsignedInteger(127L);
                     }
                     break;
                  case 70:
                     asnOut.writeCharacterString(this.getModelName(), this.getCharacterSet());
                     break;
                  case 73:
                     asnOut.writeUnsignedInteger(this.getNumberOfApduRetries());
                     break;
                  case 75:
                     asnOut.writeObjectIdentifier(this.getObjectId());
                     break;
                  case 76:
                     if (ndx == 0) {
                        int size = this.exports().getSize();
                        asnOut.writeUnsignedInteger(size + 1);
                     } else if (ndx == -1) {
                        asnOut.writeObjectIdentifier(this.getObjectId());
                        this.exports().writeObjectIds(asnOut);
                     } else if (ndx == 1) {
                        asnOut.writeObjectIdentifier(this.getObjectId());
                     } else {
                        asnOut.writeObjectIdentifier(this.exports().getEntry(ndx - 2));
                     }
                     break;
                  case 77:
                     asnOut.writeCharacterString(this.getObjectName(), this.getCharacterSet());
                     break;
                  case 79:
                     asnOut.writeEnumerated(this.getObjectId().getObjectType());
                     break;
                  case 96:
                     asnOut.writeBitString(this.getProtocolObjectTypesSupported());
                     break;
                  case 97:
                     asnOut.writeBitString(this.getProtocolServicesSupported());
                     break;
                  case 98:
                     asnOut.writeUnsignedInteger(this.getProtocolVersion());
                     break;
                  case 107:
                     asnOut.writeEnumerated(this.getSegmentationSupported().getOrdinal());
                     break;
                  case 112:
                     asnOut.writeEnumerated(this.getSystemStatus().getOrdinal());
                     break;
                  case 116:
                     this.getTimeSynchronizationRecipients().writeAsn(asnOut);
                     break;
                  case 119:
                     int niagaraMillis = BTimeZone.getLocal().getUtcOffset();
                     int off = (int)(-niagaraMillis / 60000L);
                     asnOut.writeSignedInteger(off);
                     break;
                  case 120:
                     asnOut.writeUnsignedInteger(this.getVendorId());
                     break;
                  case 121:
                     asnOut.writeCharacterString(this.getVendorName(), this.getCharacterSet());
                     break;
                  case 139:
                     asnOut.writeUnsignedInteger(this.getProtocolRevision());
                     break;
                  case 152:
                     this.getActiveCovSubscriptions().writeAsn(asnOut);
                     break;
                  case 153:
                     asnOut.writeUnsignedInteger(this.getBackupFailureTimeout().getMillis() / 1000L);
                     break;
                  case 154:
                     if (ndx == 0) {
                        int size = this.getConfigurationFiles().getSize();
                        asnOut.writeUnsignedInteger(size);
                     } else if (ndx == -1) {
                        this.getConfigurationFiles().writeAsn(asnOut);
                     } else {
                        BBacnetObjectIdentifier id = (BBacnetObjectIdentifier)this.getConfigurationFiles().getElement(ndx);
                        if (id == null) {
                           return new NReadPropertyResult(pId, ndx, new NErrorType(2, 42));
                        }

                        asnOut.writeObjectIdentifier(id);
                     }
                     break;
                  case 155:
                     asnOut.writeUnsignedInteger(this.getDatabaseRevision());
                     break;
                  case 157:
                     this.getLastRestoreTime().writeAsn(asnOut);
                     break;
                  case 167:
                     int num = this.getMaxSegmentsAccepted();
                     if (num < 0) {
                        num = 100;
                     }

                     asnOut.writeUnsignedInteger(num);
                     break;
                  case 193:
                     asnOut.writeBoolean(this.getAlignIntervals());
                     break;
                  case 195:
                     asnOut.writeUnsignedInteger(this.getIntervalOffset());
                     break;
                  case 196:
                     asnOut.writeEnumerated(this.getLastRestartReason().getOrdinal());
                     break;
                  case 202:
                     this.getRestartNotificationRecipients().writeAsn(asnOut);
                     break;
                  case 203:
                     this.getTimeOfDeviceRestart().writeAsn(asnOut);
                     break;
                  case 204:
                     asnOut.writeUnsignedInteger(this.getTimeSynchronizationInterval().getMillis() / 60000L);
                     break;
                  case 206:
                     this.getUtcTimeSynchronizationRecipients().writeAsn(asnOut);
                     break;
                  case 338:
                     asnOut.writeEnumerated(this.getBackupAndRestoreState().getOrdinal());
                     break;
                  case 339:
                     asnOut.writeUnsignedInteger(this.getBackupPreparationTime().getSeconds());
                     break;
                  case 340:
                     asnOut.writeUnsignedInteger(this.getRestoreCompletionTime().getSeconds());
                     break;
                  case 341:
                     asnOut.writeUnsignedInteger(this.getRestorePreparationTime().getSeconds());
                     break;
                  case 371:
                     return this.readPropertyList(ndx);
                  case 372:
                     asnOut.writeCharacterString(SERIAL_NUMBER, this.getCharacterSet());
                     break;
                  case 507:
                  default:
                     return new NReadPropertyResult(pId, ndx, new NErrorType(2, 32));
               }
            } catch (IndexOutOfBoundsException var14) {
               return new NReadPropertyResult(pId, ndx, new NErrorType(2, 42));
            }

            return new NReadPropertyResult(pId, ndx, asnOut.toByteArray());
         }
      }
   }

   protected RangeData readRange(RangeReference ref, Object[] list, int maxEncodedSize) {
      int rangeType = ref.getRangeType();
      int len = list.length;
      boolean[] rflags = new boolean[]{false, false, false};
      int maxDataLength = -1;
      if (ref instanceof BacnetConfirmedRequest) {
         maxDataLength = ((BacnetConfirmedRequest)ref).getMaxDataLength() - 23 + 3 + 5;
      }

      if (rangeType == 3) {
         int refNdx = (int)ref.getReferenceIndex();
         int count = ref.getCount();
         if (refNdx <= len && refNdx >= 1) {
            Array<Object> a = new Array(Object.class);
            int itemsFound = 0;
            if (count > 0) {
               for (int i = refNdx - 1; i < len && itemsFound < count; i++) {
                  a.add(list[i]);
                  itemsFound++;
               }

               if (refNdx == 1) {
                  rflags[0] = true;
               }

               if (refNdx + count - 1 >= len) {
                  rflags[1] = true;
               }
            } else {
               if (count >= 0) {
                  return new ReadRangeAck(5, 7);
               }

               count = -count;

               for (int i = refNdx - 1; i >= 0 && itemsFound < count; i--) {
                  a.add(list[i]);
                  itemsFound++;
               }

               a = a.reverse();
               if (refNdx - count <= 0) {
                  rflags[0] = true;
               }

               if (refNdx == len) {
                  rflags[1] = true;
               }
            }

            Iterator<Object> it = a.iterator();
            int itemCount = 0;
            synchronized (asnOut) {
               asnOut.reset();
               if (maxDataLength > 0) {
                  while (it.hasNext()) {
                     if (maxDataLength - asnOut.size() < maxEncodedSize) {
                        rflags[1] = false;
                        break;
                     }

                     ((BIBacnetDataType)it.next()).writeAsn(asnOut);
                     itemCount++;
                  }
               } else {
                  itemCount = itemsFound;

                  while (it.hasNext()) {
                     ((BIBacnetDataType)it.next()).writeAsn(asnOut);
                  }
               }

               if (itemCount < itemsFound) {
                  rflags[2] = true;
               }

               return new ReadRangeAck(this.getObjectId(), ref.getPropertyId(), -1, BBacnetBitString.make(rflags), itemCount, asnOut.toByteArray());
            }
         } else {
            return new ReadRangeAck(this.getObjectId(), ref.getPropertyId(), -1, BBacnetBitString.emptyBitString(3), 0L, new byte[0]);
         }
      } else if (rangeType == -1) {
         rflags[0] = false;
         int itemCount = 0;
         synchronized (asnOut) {
            asnOut.reset();
            if (maxDataLength > 0) {
               for (int i = 0; i < len; i++) {
                  ((BIBacnetDataType)list[i]).writeAsn(asnOut);
                  itemCount++;
                  if (maxDataLength - asnOut.size() < maxEncodedSize) {
                     break;
                  }
               }

               if (itemCount > 0) {
                  rflags[0] = true;
               }

               if (itemCount > 0 && itemCount == len) {
                  rflags[1] = true;
               }
            } else {
               itemCount = len;

               for (int ix = 0; ix < len; ix++) {
                  ((BIBacnetDataType)list[ix]).writeAsn(asnOut);
               }

               if (len > 0) {
                  rflags[0] = true;
               }

               if (len > 0 && len == len) {
                  rflags[1] = true;
               }
            }

            if (itemCount < len) {
               rflags[2] = true;
            }

            return new ReadRangeAck(this.getObjectId(), ref.getPropertyId(), -1, BBacnetBitString.make(rflags), itemCount, asnOut.toByteArray());
         }
      } else if (rangeType == 6) {
         return new ReadRangeAck(BBacnetErrorClass.property, BBacnetErrorCode.listItemNotNumbered);
      } else {
         return rangeType == 7
            ? new ReadRangeAck(BBacnetErrorClass.property, BBacnetErrorCode.listItemNotTimestamped)
            : new ReadRangeAck(BBacnetErrorClass.services, BBacnetErrorCode.parameterOutOfRange);
      }
   }

   protected ErrorType writeProperty(int pId, int ndx, byte[] val, int pri) throws BacnetException {
      if (ndx >= 0 && !this.isArray(pId)) {
         return new NErrorType(2, 50);
      } else {
         try {
            synchronized (asnIn) {
               long lval = 0L;
               switch (pId) {
                  case 28:
                     this.setString(description, AsnUtil.fromAsnCharacterString(val), getBacnetContext());
                     return null;
                  case 58:
                     this.setString(location, AsnUtil.fromAsnCharacterString(val), getBacnetContext());
                     return null;
                  case 75:
                     return new NErrorType(2, 40);
                  case 116:
                     asnIn.setBuffer(val);
                     BBacnetListOf tsRecips = (BBacnetListOf)this.getTimeSynchronizationRecipients().newCopy();
                     tsRecips.readAsn(asnIn);
                     this.set(timeSynchronizationRecipients, tsRecips, getBacnetContext());
                     return null;
                  case 153:
                     lval = AsnUtil.fromAsnUnsignedInteger(val);
                     if (lval > 65535L) {
                        return new NErrorType(2, 37);
                     }

                     this.set(backupFailureTimeout, BRelTime.make(1000L * lval), getBacnetContext());
                     return null;
                  case 193:
                     boolean align = AsnUtil.fromAsnBoolean(val);
                     this.setBoolean(alignIntervals, align, getBacnetContext());
                     return null;
                  case 195:
                     long offset = AsnUtil.fromAsnUnsignedInteger(val);
                     BFacets f = this.getSlotFacets(intervalOffset);
                     if (offset <= f.geti("max", 1440) && offset >= f.geti("min", 0)) {
                        this.setInt(intervalOffset, (int)offset, getBacnetContext());
                        return null;
                     }

                     return new NErrorType(2, 37);
                  case 202:
                     asnIn.setBuffer(val);
                     BBacnetListOf rsRecips = (BBacnetListOf)this.getRestartNotificationRecipients().newCopy();
                     rsRecips.readAsn(asnIn);
                     this.set(restartNotificationRecipients, rsRecips, getBacnetContext());
                     return null;
                  case 204:
                     BBacnetUnsigned unsigned = AsnUtil.fromAsnUnsigned(val);
                     long timeSynchIntervalMinutes = unsigned.getUnsigned();
                     this.set(timeSynchronizationInterval, BRelTime.make(timeSynchIntervalMinutes * 60000L), getBacnetContext());
                     return null;
                  case 206:
                     asnIn.setBuffer(val);
                     BBacnetListOf utcTsRecips = (BBacnetListOf)this.getUtcTimeSynchronizationRecipients().newCopy();
                     utcTsRecips.readAsn(asnIn);
                     this.set(utcTimeSynchronizationRecipients, utcTsRecips, getBacnetContext());
                     return null;
                  case 371:
                     return new NErrorType(2, 40);
                  default:
                     for (int i = 0; i < REQUIRED_PROPS.length; i++) {
                        if (pId == REQUIRED_PROPS[i]) {
                           return new NErrorType(2, 40);
                        }
                     }

                     int[] props = this.getOptionalProps();

                     for (int ix = 0; ix < props.length; ix++) {
                        if (pId == props[ix]) {
                           return new NErrorType(2, 40);
                        }
                     }

                     return new NErrorType(2, 32);
               }
            }
         } catch (AsnException var22) {
            log.warning("AsnException writing property " + pId + " in object " + this.getObjectId() + ": " + var22);
            return new NErrorType(2, 9);
         } catch (PermissionException var23) {
            log.warning("PermissionException writing property " + pId + " in object " + this.getObjectId() + ": " + var23);
            return new NErrorType(2, 40);
         }
      }
   }

   protected ChangeListError addListElements(int pId, int ndx, byte[] val) throws BacnetException {
      if (!this.hasProperty(pId)) {
         return BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty);
      } else if (pId != 30 && pId != 152 && pId != 202 && pId != 116 && pId != 206) {
         return BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
      } else if (ndx != -1) {
         return BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.property, BBacnetErrorCode.propertyIsNotAnArray);
      } else {
         synchronized (asnIn) {
            switch (pId) {
               case 30:
               case 152:
                  return BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.property, BBacnetErrorCode.writeAccessDenied);
               case 116:
                  return this.getTimeSynchronizationRecipients().addElements(val, getBacnetContext());
               case 202:
                  return this.getRestartNotificationRecipients().addElements(val, getBacnetContext());
               case 206:
                  return this.getUtcTimeSynchronizationRecipients().addElements(val, getBacnetContext());
               default:
                  return BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty);
            }
         }
      }
   }

   protected ChangeListError removeListElements(int pId, int ndx, byte[] val) throws BacnetException {
      if (!this.hasProperty(pId)) {
         return BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty);
      } else if (pId != 30 && pId != 152 && pId != 202 && pId != 116 && pId != 206) {
         return BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
      } else if (ndx != -1) {
         return BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.property, BBacnetErrorCode.propertyIsNotAnArray);
      } else {
         synchronized (asnIn) {
            switch (pId) {
               case 30:
               case 152:
                  return BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.property, BBacnetErrorCode.writeAccessDenied);
               case 116:
                  return this.getTimeSynchronizationRecipients().removeElements(val, getBacnetContext());
               case 202:
                  return this.getRestartNotificationRecipients().removeElements(val, getBacnetContext());
               case 206:
                  return this.getUtcTimeSynchronizationRecipients().removeElements(val, getBacnetContext());
               default:
                  return BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty);
            }
         }
      }
   }

   private int[] getOptionalProps() {
      Vector<BBacnetPropertyIdentifier> v = new Vector<>();
      v.add(BBacnetPropertyIdentifier.location);
      v.add(BBacnetPropertyIdentifier.description);
      v.add(BBacnetPropertyIdentifier.maxSegmentsAccepted);
      v.add(BBacnetPropertyIdentifier.localTime);
      v.add(BBacnetPropertyIdentifier.localDate);
      v.add(BBacnetPropertyIdentifier.utcOffset);
      v.add(BBacnetPropertyIdentifier.serialNumber);
      v.add(BBacnetPropertyIdentifier.daylightSavingsStatus);
      v.add(BBacnetPropertyIdentifier.apduSegmentTimeout);
      v.add(BBacnetPropertyIdentifier.timeSynchronizationRecipients);
      v.add(BBacnetPropertyIdentifier.maxMaster);
      v.add(BBacnetPropertyIdentifier.maxInfoFrames);
      v.add(BBacnetPropertyIdentifier.utcTimeSynchronizationRecipients);
      v.add(BBacnetPropertyIdentifier.timeSynchronizationInterval);
      v.add(BBacnetPropertyIdentifier.alignIntervals);
      v.add(BBacnetPropertyIdentifier.intervalOffset);
      v.add(BBacnetPropertyIdentifier.lastRestartReason);
      v.add(BBacnetPropertyIdentifier.timeOfDeviceRestart);
      this.addOptionalProps(v);
      int[] optionalProps = new int[v.size()];

      for (int i = 0; i < optionalProps.length; i++) {
         optionalProps[i] = ((BEnum)v.elementAt(i)).getOrdinal();
      }

      return optionalProps;
   }

   protected void addOptionalProps(Vector<BBacnetPropertyIdentifier> v) {
      v.add(BBacnetPropertyIdentifier.activeCovSubscriptions);
      v.add(BBacnetPropertyIdentifier.restartNotificationRecipients);
      v.add(BBacnetPropertyIdentifier.configurationFiles);
      v.add(BBacnetPropertyIdentifier.lastRestoreTime);
      v.add(BBacnetPropertyIdentifier.backupFailureTimeout);
      v.add(BBacnetPropertyIdentifier.backupPreparationTime);
      v.add(BBacnetPropertyIdentifier.restorePreparationTime);
      v.add(BBacnetPropertyIdentifier.restoreCompletionTime);
      v.add(BBacnetPropertyIdentifier.backupAndRestoreState);
   }

   private BBacnetNetwork network() {
      return (BBacnetNetwork)this.getParent();
   }

   public void subscribeCov(BIBacnetCovSource export, BComponent src, Property p) {
      BBacnetCovSubscription cov = (BBacnetCovSubscription)((BComplex)export).get(p);
      if (cov.isCovProperty()) {
         this.covPropPoller.subscribe(cov);
      } else {
         this.covSubscriber.subscribe(export, src);
      }

      BOrd covOrd = BOrd.make(((BComponent)export).getSlotPathOrd().toString() + "/" + p.getName());
      Property sub = this.getActiveCovSubscriptions().addListElement(covOrd, null);
      this.getActiveCovSubscriptions().setFlags(sub, 1);
   }

   public void unsubscribeCov(BIBacnetCovSource export, BComponent src, Property p) {
      BBacnetCovSubscription cov = (BBacnetCovSubscription)((BComplex)export).get(p);
      if (cov.isCovProperty()) {
         this.covPropPoller.unsubscribe(cov);
      } else {
         Object[] children = ((BComponent)export).getChildren(BBacnetCovSubscription.class);
         if (children.length <= 0) {
            if (log.isLoggable(Level.FINE)) {
               log.fine("Removing cov subscription on " + export);
            }

            this.covSubscriber.unsubscribe(export, src);
         }
      }

      BOrd covOrd = BOrd.make(((BComponent)export).getSlotPathOrd().toString() + "/" + p.getName());
      this.getActiveCovSubscriptions().removeListElement(covOrd, null);
   }

   public void subscribe(BIBacnetExportObject export, Object src) {
      if (src instanceof BComponent) {
         this.objectSubscriber.subscribe(export, (BComponent)src);
      }
   }

   public void unsubscribe(BIBacnetExportObject export, Object src) {
      if (src instanceof BComponent) {
         this.objectSubscriber.unsubscribe(export, (BComponent)src);
      }
   }

   public static Context getBacnetContext() {
      if (bacnetContext == null) {
         try {
            BUserService us = (BUserService)Sys.getService(BUserService.TYPE);
            BUser bacnetUser = us.getUser("BACnet");
            if (bacnetUser == null) {
               bacnetUser = (BUser)us.get(us.add("BACnet", new BUser()));
            }

            if (!bacnetUser.getEnabled()) {
               throw new PermissionException("BACnet User not enabled");
            }

            bacnetContext = new BasicContext(bacnetUser);
         } catch (Exception var2) {
            log.log(Level.SEVERE, "Unable to retrieve BACnet user context", (Throwable)var2);
            throw new PermissionException("Error retrieving BACnet user context");
         }
      }

      return bacnetContext;
   }

   private void scheduleTimeSynch() {
      synchronized (this.TIME_SYNC_LOCK) {
         if (this.tsTicket != null) {
            this.tsTicket.cancel();
         }

         BRelTime interval = this.getTimeSynchronizationInterval();
         long imillis = interval.getMillis();
         if (imillis == 0L) {
            this.tsTicket = null;
         } else {
            BAbsTime now = BAbsTime.now();
            BAbsTime start = null;
            long nowMillis = now.getMillis();
            if (imillis > 0L) {
               if (this.getAlignIntervals()) {
                  if (3600000L % imillis == 0L) {
                     BAbsTime startOfHour = BAbsTime.make(now.getYear(), now.getMonth(), now.getDay(), now.getHour(), 0);
                     long startOfHourMillis = startOfHour.getMillis();
                     start = getNextInterval(startOfHourMillis, imillis, this.getIntervalOffset(), nowMillis);
                  } else if (86400000L % imillis == 0L) {
                     BAbsTime startOfDay = BAbsTime.make(now.getYear(), now.getMonth(), now.getDay(), 0, 0);
                     long startOfDayMillis = startOfDay.getMillis();
                     start = getNextInterval(startOfDayMillis, imillis, this.getIntervalOffset(), nowMillis);
                  } else {
                     start = this.lastTSTime.add(interval);
                     if (start.isBefore(now)) {
                        now.add(interval);
                     }
                  }
               } else {
                  start = this.lastTSTime.add(interval);
                  if (start.isBefore(now)) {
                     now.add(interval);
                  }
               }

               if (log.isLoggable(Level.FINE)) {
                  StringBuilder sb = new StringBuilder("BACnet Time Synchronization: every ");
                  sb.append(interval.toString(BFacets.make("showSeconds", false)))
                     .append(", beginning at ")
                     .append(start.toString(BFacets.make("showSeconds", false)))
                     .append(this.getAlignIntervals() ? ": aligned" : ": unaligned");
                  if (this.getAlignIntervals()) {
                     sb.append(", offset:").append(this.getIntervalOffset()).append(" min");
                  }

                  log.fine(sb.toString());
               }

               this.tsTicket = Clock.schedulePeriodically(this, start, interval, sendTimeSynch, null);
            } else {
               log.fine("BACnet Time Synchronization disabled");
            }
         }
      }
   }

   private static BAbsTime getNextInterval(long start, long interval, int offset, long now) {
      long offsetInterval = offset * 60000L % interval;
      long next = start + offsetInterval;

      do {
         next += interval;
      } while (next < now);

      return BAbsTime.make(next);
   }

   private void checkRecipients(Property p) {
      SlotCursor<Property> c = ((BComplex)this.get(p)).getProperties();
      BBacnetRecipient r = null;

      while (c.next(BBacnetRecipient.class)) {
         r = (BBacnetRecipient)c.get();
         if (r.isDevice()) {
            BBacnetObjectIdentifier deviceId = r.getDevice();
            if (deviceId.isValid() && DeviceRegistry.getDeviceAddress(deviceId) == null) {
               try {
                  ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm())
                     .getClient()
                     .whoIs(BBacnetAddress.GLOBAL_BROADCAST_ADDRESS, deviceId.getInstanceNumber(), deviceId.getInstanceNumber());
               } catch (BacnetException var6) {
                  log.log(Level.SEVERE, "Unable to determine address for Bacnet Time Synch Recipient " + deviceId, (Throwable)var6);
               }
            }
         }
      }
   }

   public void updateSystemStatus(BBacnetDeviceStatus newStatus) {
      this.preBackupRestoreStatus = this.getSystemStatus();
      this.setSystemStatus(newStatus);
   }

   public void restoreSystemStatus() {
      this.setSystemStatus(this.preBackupRestoreStatus);
   }

   public PropertyInfo getPropertyInfo(int objectType, int propId) {
      PropertyInfo propInfo = ObjectTypeList.getInstance().getPropertyInfo(objectType, propId);
      if (propInfo == null) {
         propInfo = new PropertyInfo(BBacnetPropertyIdentifier.tag(propId), propId, -6);
      }

      return propInfo;
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("LocalBacnetDevice", 2);
      out.prop("fatalFault", this.fatalFault);
      out.prop("objectName", this.getObjectName());
      out.prop("preBackupRestoreStatus", this.preBackupRestoreStatus);
      out.prop("tsTicket", this.tsTicket);
      out.prop("lastTSTime", this.lastTSTime);
      out.trTitle("DeviceRegistry", 2);
      javax.baja.nre.util.LongHashMap.Iterator it = DeviceRegistry.addressIterator();
      int i = 0;

      while (it.hasNext()) {
         out.prop("  " + i++, it.next());
      }

      out.prop("bacnetContext", bacnetContext);
      out.prop("COV subscription count", this.covSubscriber.getSubscriptionCount());
      this.covPropPoller.spy(out);
      out.endProps();
   }

   @Override
   public final boolean isFatalFault() {
      return this.fatalFault;
   }

   private void checkFatalFault() {
      if (!this.fatalFault) {
         if (this.network().isFatalFault()) {
            this.fatalFault = true;
            this.setFaultCause("Network fault: " + this.network().getFaultCause());
         } else {
            this.setFaultCause("");
         }
      }
   }

   private void readBrandProperties() {
      if (!this.brandPropertiesRead) {
         AccessController.doPrivileged((PrivilegedAction<Void>)(() -> {
            InputStream is = null;

            try {
               BOrd ord = BOrd.make("file:!etc/brand.properties");
               BIFile brandFile = (BIFile)ord.resolve().get();
               is = brandFile.getInputStream();
               BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

               for (String line = br.readLine(); line != null; line = br.readLine()) {
                  try {
                     line = line.trim();
                     if (line.startsWith("bacnetVendorId=")) {
                        int vid = 36;

                        try {
                           vid = Integer.parseInt(line.substring(15));
                        } catch (Exception var20) {
                        }

                        this.setVendorId(vid);
                        this.setVendorName(BacnetVendorUtil.getVendorName(vid));
                     } else if (line.startsWith("modelName=")) {
                        String mn = line.substring(10);
                        this.objectName = mn;
                        this.setModelName(mn);
                     } else if (line.startsWith("applicationSoftwareVersion=")) {
                        String nAppSwVer = this.getType().getVendor() + " " + this.getType().getVendorVersion();
                        this.setApplicationSoftwareVersion(line.substring(27) + " - BACnet: " + nAppSwVer);
                     }
                  } catch (Exception var21) {
                     log.warning("Error parsing BACnet device branding information line: " + line + " (" + var21 + ")");
                  }
               }
            } catch (UnresolvedException var22) {
            } catch (Exception var23) {
               log.log(Level.SEVERE, "Error reading BACnet device branding information", (Throwable)var23);
            } finally {
               try {
                  if (is != null) {
                     is.close();
                  }
               } catch (Exception var19) {
               }

               this.brandPropertiesRead = true;
            }

            return null;
         }));
      }
   }

   public BIcon getIcon() {
      return icon;
   }

   @Override
   public int[] getPropertyList() {
      return BacnetPropertyList.makePropertyList(REQUIRED_PROPS, this.getOptionalProps());
   }
}
