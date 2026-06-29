package javax.baja.bacnet.enums;

import javax.baja.bacnet.BacnetConst;
import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.InvalidEnumException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("other"), @Range("authenticationFailed"), @Range("configurationInProgress"), @Range("deviceBusy"), @Range("dynamicCreationNotSupported"), @Range("fileAccessDenied"), @Range("incompatibleSecurityLevels"), @Range("inconsistentParameters"), @Range("inconsistentSelectionCriterion"), @Range("invalidDataType"), @Range("invalidFileAccessMethod"), @Range("invalidFileStartPosition"), @Range("invalidOperatorName"), @Range("invalidParameterDataType"), @Range("invalidTimeStamp"), @Range("keyGenerationError"), @Range("missingRequiredParameter"), @Range("noObjectsOfSpecifiedType"), @Range("noSpaceForObject"), @Range("noSpaceToAddListElement"), @Range("noSpaceToWriteProperty"), @Range("noVtSessionsAvailable"), @Range("propertyIsNotA_List"), @Range("objectDeletionNotPermitted"), @Range("objectIdentifierAlreadyExists"), @Range("operationalProblem"), @Range("passwordFailure"), @Range("readAccessDenied"), @Range("securityNotSupported"), @Range("serviceRequestDenied"), @Range("timeout"), @Range("unknownObject"), @Range("unknownProperty"), @Range("removed"), @Range("unknownVtClass"), @Range("unknownVtSession"), @Range("unsupportedObjectType"), @Range("valueOutOfRange"), @Range("vtSessionAlreadyClosed"), @Range("vtSessionTerminationFailure"), @Range("writeAccessDenied"), @Range("characterSetNotSupported"), @Range("invalidArrayIndex"), @Range("covSubscriptionFailed"), @Range("notCovProperty"), @Range("optionalFunctionalityNotSupported"), @Range("invalidConfigurationData"), @Range("datatypeNotSupported"), @Range("duplicateName"), @Range("duplicateObjectId"), @Range("propertyIsNotAnArray"), @Range("abortBufferOverflow"), @Range("abortInvalidApduInThisState"), @Range("abortPreemptedByHigherPriorityTask"), @Range("abortSegmentationNotSupported"), @Range("abortProprietary"), @Range("abortOther"), @Range("invalidTag"), @Range("networkDown"), @Range("rejectBufferOverflow"), @Range("rejectInconsistentParameters"), @Range("rejectInvalidParameterDataType"), @Range("rejectInvalidTag"), @Range("rejectMissingRequiredParameter"), @Range("rejectParameterOutOfRange"), @Range("rejectTooManyArguments"), @Range("rejectUndefinedEnumeration"), @Range("rejectUnrecognizedService"), @Range("rejectProprietary"), @Range("rejectOther"), @Range("unknownDevice"), @Range("unknownRoute"), @Range("valueNotInitialized"), @Range("invalidEventState"), @Range("noAlarmConfigured"), @Range("logBufferFull"), @Range("loggedValuePurged"), @Range("noPropertySpecified"), @Range("notConfiguredForTriggeredLogging"), @Range("unknownSubscription"), @Range("parameterOutOfRange"), @Range("listElementNotFound"), @Range("busy"), @Range("communicationDisabled"), @Range("success"), @Range("accessDenied"), @Range("badDestinationAddress"), @Range("badDestinationDeviceId"), @Range("badSignature"), @Range("badSourceAddress"), @Range("badTimestamp"), @Range("cannotUseKey"), @Range("cannotVerifyMessageId"), @Range("correctKeyRevision"), @Range("destinationDeviceIdRequired"), @Range("duplicateMessage"), @Range("encryptionNotConfigured"), @Range("encryptionRequired"), @Range("incorrectKey"), @Range("invalidKeyData"), @Range("keyUpdateInProgress"), @Range("malformedMessage"), @Range("notKeyServer"), @Range("securityNotConfigured"), @Range("sourceSecurityRequired"), @Range("tooManyKeys"), @Range("unknownAuthenticationType"), @Range("unknownKey"), @Range("unknownKeyRevision"), @Range("unknownSourceMessage"), @Range("notRouterToDnet"), @Range("routerBusy"), @Range("unknownNetworkMessage"), @Range("messageTooLong"), @Range("securityError"), @Range("addressingError"), @Range("writeBdtFailed"), @Range("readBdtFailed"), @Range("registerForeignDeviceFailed"), @Range("readFdtFailed"), @Range("deleteFdtEntryFailed"), @Range("distributeBroadcastFailed"), @Range("unknownFileSize"), @Range("abortApduTooLong"), @Range("abortApplicationExceededReplyTime"), @Range("abortOutOfResources"), @Range("abortTsmTimeout"), @Range("abortWindowSizeOutOfRange"), @Range("fileFull"), @Range("inconsistentConfiguration"), @Range("inconsistentObjectType"), @Range("internalError"), @Range("notConfigured"), @Range("outOfMemory"), @Range("valueTooLong"), @Range("abortInsufficientSecurity"), @Range("abortSecurityError"), @Range("duplicateEntry"), @Range("invalidValueInThisState"), @Range("invalidOperationInThisState"), @Range("listItemNotNumbered"), @Range("listItemNotTimestamped"), @Range("invalidDataEncoding"), @Range("bvlcFunctionUnknown"), @Range("bvlcProprietaryFunctionUnknown"), @Range("headerEncodingError"), @Range("headerNotUnderstood"), @Range("messageIncomplete"), @Range("notA_BacnetScHub"), @Range("payloadExpected"), @Range("unexpectedData"), @Range("nodeDuplicateVmac"), @Range("httpUnexpectedResponseCode"), @Range("httpNoUpgrade"), @Range("httpResourceNotLocal"), @Range("httpProxyAuthenticationFailed"), @Range("httpResponseTimeout"), @Range("httpResponseSyntaxError"), @Range("httpResponseValueError"), @Range("httpResponseMissingHeader"), @Range("httpWebsocketHeaderError"), @Range("httpUpgradeRequired"), @Range("httpUpgradeError"), @Range("httpTemporaryUnavailable"), @Range("httpNotA_Server"), @Range("httpError"), @Range("websocketSchemeNotSupported"), @Range("websocketUnknownControlMessage"), @Range("websocketCloseError"), @Range("websocketClosedByPeer"), @Range("websocketEndpointLeaves"), @Range("websocketProtocolError"), @Range("websocketDataNotAccepted"), @Range("websocketClosedAbnormally"), @Range("websocketDataInconsistent"), @Range("websocketDataAgainstPolicy"), @Range("websocketFrameTooLong"), @Range("websocketExtensionMissing"), @Range("websocketRequestUnavailable"), @Range("websocketError"), @Range("tlsClientCertificateError"), @Range("tlsServerCertificateError"), @Range("tlsClientAuthenticationFailed"), @Range("tlsServerAuthenticationFailed"), @Range("tlsClientCertificateExpired"), @Range("tlsServerCertificateExpired"), @Range("tlsClientCertificateRevoked"), @Range("tlsServerCertificateRevoked"), @Range("tlsError"), @Range("dnsUnavailable"), @Range("dnsNameResolutionFailed"), @Range("dnsResolverFailure"), @Range("dnsError"), @Range("tcpConnectTimeout"), @Range("tcpConnectionRefused"), @Range("tcpClosedByLocal"), @Range("tcpClosedOther"), @Range("tcpError"), @Range("ipAddressNotReachable"), @Range("ipError"), @Range(
      value = "nullValueEvent",
      ordinal = 355
   )}
)
public final class BBacnetErrorCode extends BFrozenEnum implements BacnetConst {
   public static final int OTHER = 0;
   public static final int AUTHENTICATION_FAILED = 1;
   public static final int CONFIGURATION_IN_PROGRESS = 2;
   public static final int DEVICE_BUSY = 3;
   public static final int DYNAMIC_CREATION_NOT_SUPPORTED = 4;
   public static final int FILE_ACCESS_DENIED = 5;
   public static final int INCOMPATIBLE_SECURITY_LEVELS = 6;
   public static final int INCONSISTENT_PARAMETERS = 7;
   public static final int INCONSISTENT_SELECTION_CRITERION = 8;
   public static final int INVALID_DATA_TYPE = 9;
   public static final int INVALID_FILE_ACCESS_METHOD = 10;
   public static final int INVALID_FILE_START_POSITION = 11;
   public static final int INVALID_OPERATOR_NAME = 12;
   public static final int INVALID_PARAMETER_DATA_TYPE = 13;
   public static final int INVALID_TIME_STAMP = 14;
   public static final int KEY_GENERATION_ERROR = 15;
   public static final int MISSING_REQUIRED_PARAMETER = 16;
   public static final int NO_OBJECTS_OF_SPECIFIED_TYPE = 17;
   public static final int NO_SPACE_FOR_OBJECT = 18;
   public static final int NO_SPACE_TO_ADD_LIST_ELEMENT = 19;
   public static final int NO_SPACE_TO_WRITE_PROPERTY = 20;
   public static final int NO_VT_SESSIONS_AVAILABLE = 21;
   public static final int PROPERTY_IS_NOT_A_LIST = 22;
   public static final int OBJECT_DELETION_NOT_PERMITTED = 23;
   public static final int OBJECT_IDENTIFIER_ALREADY_EXISTS = 24;
   public static final int OPERATIONAL_PROBLEM = 25;
   public static final int PASSWORD_FAILURE = 26;
   public static final int READ_ACCESS_DENIED = 27;
   public static final int SECURITY_NOT_SUPPORTED = 28;
   public static final int SERVICE_REQUEST_DENIED = 29;
   public static final int TIMEOUT = 30;
   public static final int UNKNOWN_OBJECT = 31;
   public static final int UNKNOWN_PROPERTY = 32;
   public static final int REMOVED = 33;
   public static final int UNKNOWN_VT_CLASS = 34;
   public static final int UNKNOWN_VT_SESSION = 35;
   public static final int UNSUPPORTED_OBJECT_TYPE = 36;
   public static final int VALUE_OUT_OF_RANGE = 37;
   public static final int VT_SESSION_ALREADY_CLOSED = 38;
   public static final int VT_SESSION_TERMINATION_FAILURE = 39;
   public static final int WRITE_ACCESS_DENIED = 40;
   public static final int CHARACTER_SET_NOT_SUPPORTED = 41;
   public static final int INVALID_ARRAY_INDEX = 42;
   public static final int COV_SUBSCRIPTION_FAILED = 43;
   public static final int NOT_COV_PROPERTY = 44;
   public static final int OPTIONAL_FUNCTIONALITY_NOT_SUPPORTED = 45;
   public static final int INVALID_CONFIGURATION_DATA = 46;
   public static final int DATATYPE_NOT_SUPPORTED = 47;
   public static final int DUPLICATE_NAME = 48;
   public static final int DUPLICATE_OBJECT_ID = 49;
   public static final int PROPERTY_IS_NOT_AN_ARRAY = 50;
   public static final int ABORT_BUFFER_OVERFLOW = 51;
   public static final int ABORT_INVALID_APDU_IN_THIS_STATE = 52;
   public static final int ABORT_PREEMPTED_BY_HIGHER_PRIORITY_TASK = 53;
   public static final int ABORT_SEGMENTATION_NOT_SUPPORTED = 54;
   public static final int ABORT_PROPRIETARY = 55;
   public static final int ABORT_OTHER = 56;
   public static final int INVALID_TAG = 57;
   public static final int NETWORK_DOWN = 58;
   public static final int REJECT_BUFFER_OVERFLOW = 59;
   public static final int REJECT_INCONSISTENT_PARAMETERS = 60;
   public static final int REJECT_INVALID_PARAMETER_DATA_TYPE = 61;
   public static final int REJECT_INVALID_TAG = 62;
   public static final int REJECT_MISSING_REQUIRED_PARAMETER = 63;
   public static final int REJECT_PARAMETER_OUT_OF_RANGE = 64;
   public static final int REJECT_TOO_MANY_ARGUMENTS = 65;
   public static final int REJECT_UNDEFINED_ENUMERATION = 66;
   public static final int REJECT_UNRECOGNIZED_SERVICE = 67;
   public static final int REJECT_PROPRIETARY = 68;
   public static final int REJECT_OTHER = 69;
   public static final int UNKNOWN_DEVICE = 70;
   public static final int UNKNOWN_ROUTE = 71;
   public static final int VALUE_NOT_INITIALIZED = 72;
   public static final int INVALID_EVENT_STATE = 73;
   public static final int NO_ALARM_CONFIGURED = 74;
   public static final int LOG_BUFFER_FULL = 75;
   public static final int LOGGED_VALUE_PURGED = 76;
   public static final int NO_PROPERTY_SPECIFIED = 77;
   public static final int NOT_CONFIGURED_FOR_TRIGGERED_LOGGING = 78;
   public static final int UNKNOWN_SUBSCRIPTION = 79;
   public static final int PARAMETER_OUT_OF_RANGE = 80;
   public static final int LIST_ELEMENT_NOT_FOUND = 81;
   public static final int BUSY = 82;
   public static final int COMMUNICATION_DISABLED = 83;
   public static final int SUCCESS = 84;
   public static final int ACCESS_DENIED = 85;
   public static final int BAD_DESTINATION_ADDRESS = 86;
   public static final int BAD_DESTINATION_DEVICE_ID = 87;
   public static final int BAD_SIGNATURE = 88;
   public static final int BAD_SOURCE_ADDRESS = 89;
   public static final int BAD_TIMESTAMP = 90;
   public static final int CANNOT_USE_KEY = 91;
   public static final int CANNOT_VERIFY_MESSAGE_ID = 92;
   public static final int CORRECT_KEY_REVISION = 93;
   public static final int DESTINATION_DEVICE_ID_REQUIRED = 94;
   public static final int DUPLICATE_MESSAGE = 95;
   public static final int ENCRYPTION_NOT_CONFIGURED = 96;
   public static final int ENCRYPTION_REQUIRED = 97;
   public static final int INCORRECT_KEY = 98;
   public static final int INVALID_KEY_DATA = 99;
   public static final int KEY_UPDATE_IN_PROGRESS = 100;
   public static final int MALFORMED_MESSAGE = 101;
   public static final int NOT_KEY_SERVER = 102;
   public static final int SECURITY_NOT_CONFIGURED = 103;
   public static final int SOURCE_SECURITY_REQUIRED = 104;
   public static final int TOO_MANY_KEYS = 105;
   public static final int UNKNOWN_AUTHENTICATION_TYPE = 106;
   public static final int UNKNOWN_KEY = 107;
   public static final int UNKNOWN_KEY_REVISION = 108;
   public static final int UNKNOWN_SOURCE_MESSAGE = 109;
   public static final int NOT_ROUTER_TO_DNET = 110;
   public static final int ROUTER_BUSY = 111;
   public static final int UNKNOWN_NETWORK_MESSAGE = 112;
   public static final int MESSAGE_TOO_LONG = 113;
   public static final int SECURITY_ERROR = 114;
   public static final int ADDRESSING_ERROR = 115;
   public static final int WRITE_BDT_FAILED = 116;
   public static final int READ_BDT_FAILED = 117;
   public static final int REGISTER_FOREIGN_DEVICE_FAILED = 118;
   public static final int READ_FDT_FAILED = 119;
   public static final int DELETE_FDT_ENTRY_FAILED = 120;
   public static final int DISTRIBUTE_BROADCAST_FAILED = 121;
   public static final int UNKNOWN_FILE_SIZE = 122;
   public static final int ABORT_APDU_TOO_LONG = 123;
   public static final int ABORT_APPLICATION_EXCEEDED_REPLY_TIME = 124;
   public static final int ABORT_OUT_OF_RESOURCES = 125;
   public static final int ABORT_TSM_TIMEOUT = 126;
   public static final int ABORT_WINDOW_SIZE_OUT_OF_RANGE = 127;
   public static final int FILE_FULL = 128;
   public static final int INCONSISTENT_CONFIGURATION = 129;
   public static final int INCONSISTENT_OBJECT_TYPE = 130;
   public static final int INTERNAL_ERROR = 131;
   public static final int NOT_CONFIGURED = 132;
   public static final int OUT_OF_MEMORY = 133;
   public static final int VALUE_TOO_LONG = 134;
   public static final int ABORT_INSUFFICIENT_SECURITY = 135;
   public static final int ABORT_SECURITY_ERROR = 136;
   public static final int DUPLICATE_ENTRY = 137;
   public static final int INVALID_VALUE_IN_THIS_STATE = 138;
   public static final int INVALID_OPERATION_IN_THIS_STATE = 139;
   public static final int LIST_ITEM_NOT_NUMBERED = 140;
   public static final int LIST_ITEM_NOT_TIMESTAMPED = 141;
   public static final int INVALID_DATA_ENCODING = 142;
   public static final int BVLC_FUNCTION_UNKNOWN = 143;
   public static final int BVLC_PROPRIETARY_FUNCTION_UNKNOWN = 144;
   public static final int HEADER_ENCODING_ERROR = 145;
   public static final int HEADER_NOT_UNDERSTOOD = 146;
   public static final int MESSAGE_INCOMPLETE = 147;
   public static final int NOT_A_BACNET_SC_HUB = 148;
   public static final int PAYLOAD_EXPECTED = 149;
   public static final int UNEXPECTED_DATA = 150;
   public static final int NODE_DUPLICATE_VMAC = 151;
   public static final int HTTP_UNEXPECTED_RESPONSE_CODE = 152;
   public static final int HTTP_NO_UPGRADE = 153;
   public static final int HTTP_RESOURCE_NOT_LOCAL = 154;
   public static final int HTTP_PROXY_AUTHENTICATION_FAILED = 155;
   public static final int HTTP_RESPONSE_TIMEOUT = 156;
   public static final int HTTP_RESPONSE_SYNTAX_ERROR = 157;
   public static final int HTTP_RESPONSE_VALUE_ERROR = 158;
   public static final int HTTP_RESPONSE_MISSING_HEADER = 159;
   public static final int HTTP_WEBSOCKET_HEADER_ERROR = 160;
   public static final int HTTP_UPGRADE_REQUIRED = 161;
   public static final int HTTP_UPGRADE_ERROR = 162;
   public static final int HTTP_TEMPORARY_UNAVAILABLE = 163;
   public static final int HTTP_NOT_A_SERVER = 164;
   public static final int HTTP_ERROR = 165;
   public static final int WEBSOCKET_SCHEME_NOT_SUPPORTED = 166;
   public static final int WEBSOCKET_UNKNOWN_CONTROL_MESSAGE = 167;
   public static final int WEBSOCKET_CLOSE_ERROR = 168;
   public static final int WEBSOCKET_CLOSED_BY_PEER = 169;
   public static final int WEBSOCKET_ENDPOINT_LEAVES = 170;
   public static final int WEBSOCKET_PROTOCOL_ERROR = 171;
   public static final int WEBSOCKET_DATA_NOT_ACCEPTED = 172;
   public static final int WEBSOCKET_CLOSED_ABNORMALLY = 173;
   public static final int WEBSOCKET_DATA_INCONSISTENT = 174;
   public static final int WEBSOCKET_DATA_AGAINST_POLICY = 175;
   public static final int WEBSOCKET_FRAME_TOO_LONG = 176;
   public static final int WEBSOCKET_EXTENSION_MISSING = 177;
   public static final int WEBSOCKET_REQUEST_UNAVAILABLE = 178;
   public static final int WEBSOCKET_ERROR = 179;
   public static final int TLS_CLIENT_CERTIFICATE_ERROR = 180;
   public static final int TLS_SERVER_CERTIFICATE_ERROR = 181;
   public static final int TLS_CLIENT_AUTHENTICATION_FAILED = 182;
   public static final int TLS_SERVER_AUTHENTICATION_FAILED = 183;
   public static final int TLS_CLIENT_CERTIFICATE_EXPIRED = 184;
   public static final int TLS_SERVER_CERTIFICATE_EXPIRED = 185;
   public static final int TLS_CLIENT_CERTIFICATE_REVOKED = 186;
   public static final int TLS_SERVER_CERTIFICATE_REVOKED = 187;
   public static final int TLS_ERROR = 188;
   public static final int DNS_UNAVAILABLE = 189;
   public static final int DNS_NAME_RESOLUTION_FAILED = 190;
   public static final int DNS_RESOLVER_FAILURE = 191;
   public static final int DNS_ERROR = 192;
   public static final int TCP_CONNECT_TIMEOUT = 193;
   public static final int TCP_CONNECTION_REFUSED = 194;
   public static final int TCP_CLOSED_BY_LOCAL = 195;
   public static final int TCP_CLOSED_OTHER = 196;
   public static final int TCP_ERROR = 197;
   public static final int IP_ADDRESS_NOT_REACHABLE = 198;
   public static final int IP_ERROR = 199;
   public static final int NULL_VALUE_EVENT = 355;
   public static final BBacnetErrorCode other = new BBacnetErrorCode(0);
   public static final BBacnetErrorCode authenticationFailed = new BBacnetErrorCode(1);
   public static final BBacnetErrorCode configurationInProgress = new BBacnetErrorCode(2);
   public static final BBacnetErrorCode deviceBusy = new BBacnetErrorCode(3);
   public static final BBacnetErrorCode dynamicCreationNotSupported = new BBacnetErrorCode(4);
   public static final BBacnetErrorCode fileAccessDenied = new BBacnetErrorCode(5);
   public static final BBacnetErrorCode incompatibleSecurityLevels = new BBacnetErrorCode(6);
   public static final BBacnetErrorCode inconsistentParameters = new BBacnetErrorCode(7);
   public static final BBacnetErrorCode inconsistentSelectionCriterion = new BBacnetErrorCode(8);
   public static final BBacnetErrorCode invalidDataType = new BBacnetErrorCode(9);
   public static final BBacnetErrorCode invalidFileAccessMethod = new BBacnetErrorCode(10);
   public static final BBacnetErrorCode invalidFileStartPosition = new BBacnetErrorCode(11);
   public static final BBacnetErrorCode invalidOperatorName = new BBacnetErrorCode(12);
   public static final BBacnetErrorCode invalidParameterDataType = new BBacnetErrorCode(13);
   public static final BBacnetErrorCode invalidTimeStamp = new BBacnetErrorCode(14);
   public static final BBacnetErrorCode keyGenerationError = new BBacnetErrorCode(15);
   public static final BBacnetErrorCode missingRequiredParameter = new BBacnetErrorCode(16);
   public static final BBacnetErrorCode noObjectsOfSpecifiedType = new BBacnetErrorCode(17);
   public static final BBacnetErrorCode noSpaceForObject = new BBacnetErrorCode(18);
   public static final BBacnetErrorCode noSpaceToAddListElement = new BBacnetErrorCode(19);
   public static final BBacnetErrorCode noSpaceToWriteProperty = new BBacnetErrorCode(20);
   public static final BBacnetErrorCode noVtSessionsAvailable = new BBacnetErrorCode(21);
   public static final BBacnetErrorCode propertyIsNotA_List = new BBacnetErrorCode(22);
   public static final BBacnetErrorCode objectDeletionNotPermitted = new BBacnetErrorCode(23);
   public static final BBacnetErrorCode objectIdentifierAlreadyExists = new BBacnetErrorCode(24);
   public static final BBacnetErrorCode operationalProblem = new BBacnetErrorCode(25);
   public static final BBacnetErrorCode passwordFailure = new BBacnetErrorCode(26);
   public static final BBacnetErrorCode readAccessDenied = new BBacnetErrorCode(27);
   public static final BBacnetErrorCode securityNotSupported = new BBacnetErrorCode(28);
   public static final BBacnetErrorCode serviceRequestDenied = new BBacnetErrorCode(29);
   public static final BBacnetErrorCode timeout = new BBacnetErrorCode(30);
   public static final BBacnetErrorCode unknownObject = new BBacnetErrorCode(31);
   public static final BBacnetErrorCode unknownProperty = new BBacnetErrorCode(32);
   public static final BBacnetErrorCode removed = new BBacnetErrorCode(33);
   public static final BBacnetErrorCode unknownVtClass = new BBacnetErrorCode(34);
   public static final BBacnetErrorCode unknownVtSession = new BBacnetErrorCode(35);
   public static final BBacnetErrorCode unsupportedObjectType = new BBacnetErrorCode(36);
   public static final BBacnetErrorCode valueOutOfRange = new BBacnetErrorCode(37);
   public static final BBacnetErrorCode vtSessionAlreadyClosed = new BBacnetErrorCode(38);
   public static final BBacnetErrorCode vtSessionTerminationFailure = new BBacnetErrorCode(39);
   public static final BBacnetErrorCode writeAccessDenied = new BBacnetErrorCode(40);
   public static final BBacnetErrorCode characterSetNotSupported = new BBacnetErrorCode(41);
   public static final BBacnetErrorCode invalidArrayIndex = new BBacnetErrorCode(42);
   public static final BBacnetErrorCode covSubscriptionFailed = new BBacnetErrorCode(43);
   public static final BBacnetErrorCode notCovProperty = new BBacnetErrorCode(44);
   public static final BBacnetErrorCode optionalFunctionalityNotSupported = new BBacnetErrorCode(45);
   public static final BBacnetErrorCode invalidConfigurationData = new BBacnetErrorCode(46);
   public static final BBacnetErrorCode datatypeNotSupported = new BBacnetErrorCode(47);
   public static final BBacnetErrorCode duplicateName = new BBacnetErrorCode(48);
   public static final BBacnetErrorCode duplicateObjectId = new BBacnetErrorCode(49);
   public static final BBacnetErrorCode propertyIsNotAnArray = new BBacnetErrorCode(50);
   public static final BBacnetErrorCode abortBufferOverflow = new BBacnetErrorCode(51);
   public static final BBacnetErrorCode abortInvalidApduInThisState = new BBacnetErrorCode(52);
   public static final BBacnetErrorCode abortPreemptedByHigherPriorityTask = new BBacnetErrorCode(53);
   public static final BBacnetErrorCode abortSegmentationNotSupported = new BBacnetErrorCode(54);
   public static final BBacnetErrorCode abortProprietary = new BBacnetErrorCode(55);
   public static final BBacnetErrorCode abortOther = new BBacnetErrorCode(56);
   public static final BBacnetErrorCode invalidTag = new BBacnetErrorCode(57);
   public static final BBacnetErrorCode networkDown = new BBacnetErrorCode(58);
   public static final BBacnetErrorCode rejectBufferOverflow = new BBacnetErrorCode(59);
   public static final BBacnetErrorCode rejectInconsistentParameters = new BBacnetErrorCode(60);
   public static final BBacnetErrorCode rejectInvalidParameterDataType = new BBacnetErrorCode(61);
   public static final BBacnetErrorCode rejectInvalidTag = new BBacnetErrorCode(62);
   public static final BBacnetErrorCode rejectMissingRequiredParameter = new BBacnetErrorCode(63);
   public static final BBacnetErrorCode rejectParameterOutOfRange = new BBacnetErrorCode(64);
   public static final BBacnetErrorCode rejectTooManyArguments = new BBacnetErrorCode(65);
   public static final BBacnetErrorCode rejectUndefinedEnumeration = new BBacnetErrorCode(66);
   public static final BBacnetErrorCode rejectUnrecognizedService = new BBacnetErrorCode(67);
   public static final BBacnetErrorCode rejectProprietary = new BBacnetErrorCode(68);
   public static final BBacnetErrorCode rejectOther = new BBacnetErrorCode(69);
   public static final BBacnetErrorCode unknownDevice = new BBacnetErrorCode(70);
   public static final BBacnetErrorCode unknownRoute = new BBacnetErrorCode(71);
   public static final BBacnetErrorCode valueNotInitialized = new BBacnetErrorCode(72);
   public static final BBacnetErrorCode invalidEventState = new BBacnetErrorCode(73);
   public static final BBacnetErrorCode noAlarmConfigured = new BBacnetErrorCode(74);
   public static final BBacnetErrorCode logBufferFull = new BBacnetErrorCode(75);
   public static final BBacnetErrorCode loggedValuePurged = new BBacnetErrorCode(76);
   public static final BBacnetErrorCode noPropertySpecified = new BBacnetErrorCode(77);
   public static final BBacnetErrorCode notConfiguredForTriggeredLogging = new BBacnetErrorCode(78);
   public static final BBacnetErrorCode unknownSubscription = new BBacnetErrorCode(79);
   public static final BBacnetErrorCode parameterOutOfRange = new BBacnetErrorCode(80);
   public static final BBacnetErrorCode listElementNotFound = new BBacnetErrorCode(81);
   public static final BBacnetErrorCode busy = new BBacnetErrorCode(82);
   public static final BBacnetErrorCode communicationDisabled = new BBacnetErrorCode(83);
   public static final BBacnetErrorCode success = new BBacnetErrorCode(84);
   public static final BBacnetErrorCode accessDenied = new BBacnetErrorCode(85);
   public static final BBacnetErrorCode badDestinationAddress = new BBacnetErrorCode(86);
   public static final BBacnetErrorCode badDestinationDeviceId = new BBacnetErrorCode(87);
   public static final BBacnetErrorCode badSignature = new BBacnetErrorCode(88);
   public static final BBacnetErrorCode badSourceAddress = new BBacnetErrorCode(89);
   public static final BBacnetErrorCode badTimestamp = new BBacnetErrorCode(90);
   public static final BBacnetErrorCode cannotUseKey = new BBacnetErrorCode(91);
   public static final BBacnetErrorCode cannotVerifyMessageId = new BBacnetErrorCode(92);
   public static final BBacnetErrorCode correctKeyRevision = new BBacnetErrorCode(93);
   public static final BBacnetErrorCode destinationDeviceIdRequired = new BBacnetErrorCode(94);
   public static final BBacnetErrorCode duplicateMessage = new BBacnetErrorCode(95);
   public static final BBacnetErrorCode encryptionNotConfigured = new BBacnetErrorCode(96);
   public static final BBacnetErrorCode encryptionRequired = new BBacnetErrorCode(97);
   public static final BBacnetErrorCode incorrectKey = new BBacnetErrorCode(98);
   public static final BBacnetErrorCode invalidKeyData = new BBacnetErrorCode(99);
   public static final BBacnetErrorCode keyUpdateInProgress = new BBacnetErrorCode(100);
   public static final BBacnetErrorCode malformedMessage = new BBacnetErrorCode(101);
   public static final BBacnetErrorCode notKeyServer = new BBacnetErrorCode(102);
   public static final BBacnetErrorCode securityNotConfigured = new BBacnetErrorCode(103);
   public static final BBacnetErrorCode sourceSecurityRequired = new BBacnetErrorCode(104);
   public static final BBacnetErrorCode tooManyKeys = new BBacnetErrorCode(105);
   public static final BBacnetErrorCode unknownAuthenticationType = new BBacnetErrorCode(106);
   public static final BBacnetErrorCode unknownKey = new BBacnetErrorCode(107);
   public static final BBacnetErrorCode unknownKeyRevision = new BBacnetErrorCode(108);
   public static final BBacnetErrorCode unknownSourceMessage = new BBacnetErrorCode(109);
   public static final BBacnetErrorCode notRouterToDnet = new BBacnetErrorCode(110);
   public static final BBacnetErrorCode routerBusy = new BBacnetErrorCode(111);
   public static final BBacnetErrorCode unknownNetworkMessage = new BBacnetErrorCode(112);
   public static final BBacnetErrorCode messageTooLong = new BBacnetErrorCode(113);
   public static final BBacnetErrorCode securityError = new BBacnetErrorCode(114);
   public static final BBacnetErrorCode addressingError = new BBacnetErrorCode(115);
   public static final BBacnetErrorCode writeBdtFailed = new BBacnetErrorCode(116);
   public static final BBacnetErrorCode readBdtFailed = new BBacnetErrorCode(117);
   public static final BBacnetErrorCode registerForeignDeviceFailed = new BBacnetErrorCode(118);
   public static final BBacnetErrorCode readFdtFailed = new BBacnetErrorCode(119);
   public static final BBacnetErrorCode deleteFdtEntryFailed = new BBacnetErrorCode(120);
   public static final BBacnetErrorCode distributeBroadcastFailed = new BBacnetErrorCode(121);
   public static final BBacnetErrorCode unknownFileSize = new BBacnetErrorCode(122);
   public static final BBacnetErrorCode abortApduTooLong = new BBacnetErrorCode(123);
   public static final BBacnetErrorCode abortApplicationExceededReplyTime = new BBacnetErrorCode(124);
   public static final BBacnetErrorCode abortOutOfResources = new BBacnetErrorCode(125);
   public static final BBacnetErrorCode abortTsmTimeout = new BBacnetErrorCode(126);
   public static final BBacnetErrorCode abortWindowSizeOutOfRange = new BBacnetErrorCode(127);
   public static final BBacnetErrorCode fileFull = new BBacnetErrorCode(128);
   public static final BBacnetErrorCode inconsistentConfiguration = new BBacnetErrorCode(129);
   public static final BBacnetErrorCode inconsistentObjectType = new BBacnetErrorCode(130);
   public static final BBacnetErrorCode internalError = new BBacnetErrorCode(131);
   public static final BBacnetErrorCode notConfigured = new BBacnetErrorCode(132);
   public static final BBacnetErrorCode outOfMemory = new BBacnetErrorCode(133);
   public static final BBacnetErrorCode valueTooLong = new BBacnetErrorCode(134);
   public static final BBacnetErrorCode abortInsufficientSecurity = new BBacnetErrorCode(135);
   public static final BBacnetErrorCode abortSecurityError = new BBacnetErrorCode(136);
   public static final BBacnetErrorCode duplicateEntry = new BBacnetErrorCode(137);
   public static final BBacnetErrorCode invalidValueInThisState = new BBacnetErrorCode(138);
   public static final BBacnetErrorCode invalidOperationInThisState = new BBacnetErrorCode(139);
   public static final BBacnetErrorCode listItemNotNumbered = new BBacnetErrorCode(140);
   public static final BBacnetErrorCode listItemNotTimestamped = new BBacnetErrorCode(141);
   public static final BBacnetErrorCode invalidDataEncoding = new BBacnetErrorCode(142);
   public static final BBacnetErrorCode bvlcFunctionUnknown = new BBacnetErrorCode(143);
   public static final BBacnetErrorCode bvlcProprietaryFunctionUnknown = new BBacnetErrorCode(144);
   public static final BBacnetErrorCode headerEncodingError = new BBacnetErrorCode(145);
   public static final BBacnetErrorCode headerNotUnderstood = new BBacnetErrorCode(146);
   public static final BBacnetErrorCode messageIncomplete = new BBacnetErrorCode(147);
   public static final BBacnetErrorCode notA_BacnetScHub = new BBacnetErrorCode(148);
   public static final BBacnetErrorCode payloadExpected = new BBacnetErrorCode(149);
   public static final BBacnetErrorCode unexpectedData = new BBacnetErrorCode(150);
   public static final BBacnetErrorCode nodeDuplicateVmac = new BBacnetErrorCode(151);
   public static final BBacnetErrorCode httpUnexpectedResponseCode = new BBacnetErrorCode(152);
   public static final BBacnetErrorCode httpNoUpgrade = new BBacnetErrorCode(153);
   public static final BBacnetErrorCode httpResourceNotLocal = new BBacnetErrorCode(154);
   public static final BBacnetErrorCode httpProxyAuthenticationFailed = new BBacnetErrorCode(155);
   public static final BBacnetErrorCode httpResponseTimeout = new BBacnetErrorCode(156);
   public static final BBacnetErrorCode httpResponseSyntaxError = new BBacnetErrorCode(157);
   public static final BBacnetErrorCode httpResponseValueError = new BBacnetErrorCode(158);
   public static final BBacnetErrorCode httpResponseMissingHeader = new BBacnetErrorCode(159);
   public static final BBacnetErrorCode httpWebsocketHeaderError = new BBacnetErrorCode(160);
   public static final BBacnetErrorCode httpUpgradeRequired = new BBacnetErrorCode(161);
   public static final BBacnetErrorCode httpUpgradeError = new BBacnetErrorCode(162);
   public static final BBacnetErrorCode httpTemporaryUnavailable = new BBacnetErrorCode(163);
   public static final BBacnetErrorCode httpNotA_Server = new BBacnetErrorCode(164);
   public static final BBacnetErrorCode httpError = new BBacnetErrorCode(165);
   public static final BBacnetErrorCode websocketSchemeNotSupported = new BBacnetErrorCode(166);
   public static final BBacnetErrorCode websocketUnknownControlMessage = new BBacnetErrorCode(167);
   public static final BBacnetErrorCode websocketCloseError = new BBacnetErrorCode(168);
   public static final BBacnetErrorCode websocketClosedByPeer = new BBacnetErrorCode(169);
   public static final BBacnetErrorCode websocketEndpointLeaves = new BBacnetErrorCode(170);
   public static final BBacnetErrorCode websocketProtocolError = new BBacnetErrorCode(171);
   public static final BBacnetErrorCode websocketDataNotAccepted = new BBacnetErrorCode(172);
   public static final BBacnetErrorCode websocketClosedAbnormally = new BBacnetErrorCode(173);
   public static final BBacnetErrorCode websocketDataInconsistent = new BBacnetErrorCode(174);
   public static final BBacnetErrorCode websocketDataAgainstPolicy = new BBacnetErrorCode(175);
   public static final BBacnetErrorCode websocketFrameTooLong = new BBacnetErrorCode(176);
   public static final BBacnetErrorCode websocketExtensionMissing = new BBacnetErrorCode(177);
   public static final BBacnetErrorCode websocketRequestUnavailable = new BBacnetErrorCode(178);
   public static final BBacnetErrorCode websocketError = new BBacnetErrorCode(179);
   public static final BBacnetErrorCode tlsClientCertificateError = new BBacnetErrorCode(180);
   public static final BBacnetErrorCode tlsServerCertificateError = new BBacnetErrorCode(181);
   public static final BBacnetErrorCode tlsClientAuthenticationFailed = new BBacnetErrorCode(182);
   public static final BBacnetErrorCode tlsServerAuthenticationFailed = new BBacnetErrorCode(183);
   public static final BBacnetErrorCode tlsClientCertificateExpired = new BBacnetErrorCode(184);
   public static final BBacnetErrorCode tlsServerCertificateExpired = new BBacnetErrorCode(185);
   public static final BBacnetErrorCode tlsClientCertificateRevoked = new BBacnetErrorCode(186);
   public static final BBacnetErrorCode tlsServerCertificateRevoked = new BBacnetErrorCode(187);
   public static final BBacnetErrorCode tlsError = new BBacnetErrorCode(188);
   public static final BBacnetErrorCode dnsUnavailable = new BBacnetErrorCode(189);
   public static final BBacnetErrorCode dnsNameResolutionFailed = new BBacnetErrorCode(190);
   public static final BBacnetErrorCode dnsResolverFailure = new BBacnetErrorCode(191);
   public static final BBacnetErrorCode dnsError = new BBacnetErrorCode(192);
   public static final BBacnetErrorCode tcpConnectTimeout = new BBacnetErrorCode(193);
   public static final BBacnetErrorCode tcpConnectionRefused = new BBacnetErrorCode(194);
   public static final BBacnetErrorCode tcpClosedByLocal = new BBacnetErrorCode(195);
   public static final BBacnetErrorCode tcpClosedOther = new BBacnetErrorCode(196);
   public static final BBacnetErrorCode tcpError = new BBacnetErrorCode(197);
   public static final BBacnetErrorCode ipAddressNotReachable = new BBacnetErrorCode(198);
   public static final BBacnetErrorCode ipError = new BBacnetErrorCode(199);
   public static final BBacnetErrorCode nullValueEvent = new BBacnetErrorCode(355);
   public static final BBacnetErrorCode DEFAULT = other;
   public static final Type TYPE = Sys.loadType(BBacnetErrorCode.class);
   public static final int MAX_ASHRAE_ID = 136;
   public static final int MAX_RESERVED_ID = 255;
   public static final int MAX_ID = 65535;
   public static final int TARGET_NOT_CONFIGURED = 1000;
   public static final int INVALID_TARGET_TYPE = 1001;
   public static final String TARGET_NOT_CONFIGURED_TAG = "targetNotConfigured";
   public static final String INVALID_TARGET_TYPE_TAG = "invalidTargetType";
   public static final int[] NIAGARA_CODES = new int[]{1000, 1001};
   public static final String[] NIAGARA_TAGS = new String[]{"targetNotConfigured", "invalidTargetType"};
   public static final BEnumRange NIAGARA_ERROR_CODES_RANGE = BEnumRange.make(TYPE, NIAGARA_CODES, NIAGARA_TAGS);

   public static BBacnetErrorCode make(int ordinal) {
      return (BBacnetErrorCode)other.getRange().get(ordinal, false);
   }

   public static BBacnetErrorCode make(String tag) {
      return (BBacnetErrorCode)other.getRange().get(tag);
   }

   private BBacnetErrorCode(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public static String tag(int id) {
      return tag(id, null);
   }

   public static String tag(int id, BExtensibleEnumList list) {
      if (DEFAULT.getRange().isOrdinal(id)) {
         return DEFAULT.getRange().getTag(id);
      } else if (isAshrae(id)) {
         return ASHRAE_PREFIX + id;
      } else if (isProprietary(id)) {
         return list != null && list.getErrorCodeRange().isOrdinal(id) ? list.getErrorCodeRange().getTag(id) : PROPRIETARY_PREFIX + id;
      } else {
         throw new InvalidEnumException(id);
      }
   }

   public static int ordinal(String tag) {
      return ordinal(tag, null);
   }

   public static int ordinal(String tag, BExtensibleEnumList list) {
      if (DEFAULT.getRange().isTag(tag)) {
         return DEFAULT.getRange().tagToOrdinal(tag);
      } else if (tag.startsWith(ASHRAE_PREFIX)) {
         return Integer.parseInt(tag.substring(ASHRAE_PREFIX_LENGTH));
      } else if (list != null && list.getErrorCodeRange().isTag(tag)) {
         return list.getErrorCodeRange().tagToOrdinal(tag);
      } else if (tag.startsWith(PROPRIETARY_PREFIX)) {
         return Integer.parseInt(tag.substring(PROPRIETARY_PREFIX_LENGTH));
      } else {
         throw new InvalidEnumException(tag);
      }
   }

   public static boolean isProprietary(int id) {
      return id > 255 && id <= 65535;
   }

   public static boolean isAshrae(int id) {
      return id > 136 && id <= 255;
   }

   public static boolean isValid(int id) {
      return id <= 65535;
   }

   public static boolean isFixed(int id) {
      return id <= 136;
   }
}
