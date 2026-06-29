package com.tridium.bacnet.asn;

import javax.baja.bacnet.BacnetConst;

public interface AsnConst extends BacnetConst {
   int TAG_CLASS_MASK = 8;
   int APPLICATION_TAG = 0;
   int CONTEXT_TAG = 8;
   int NOT_APPLICATION_TAG = -1;
   int TAG_NUMBER_MASK = 240;
   int TAG_NUMBER_SHIFT = 4;
   int EXTENDED_TAG_NUMBER = 240;
   int TAG_NUMBER_RESERVED = 255;
   int LVT_MASK = 7;
   int LVT_EXTENDED_LENGTH = 5;
   int LENGTH_FLAG_254_TO_65535 = 254;
   int LENGTH_FLAG_GREATER_THAN_65535 = 255;
   int OPENING_TAG = 6;
   int CLOSING_TAG = 7;
   String E_BACNET_ASN_INVALID_TAG = "Invalid tag: ";
   String E_BACNET_ASN_INVALID_BOOLEAN_VALUE = "Invalid boolean value";
   String E_BACNET_ASN_INTEGER_OVERFLOW = "Integer overflow";
   String E_BACNET_ASN_INVALID_REAL_VALUE = "Invalid real value";
   String E_BACNET_ASN_INVALID_DOUBLE_VALUE = "Invalid double value";
   String E_BACNET_ASN_UNSUPPORTED_CHARACTER_SET = "Unsupported char set";
   String E_BACNET_ASN_EXPECTED_OPENING_TAG = "Expected opening tag";
   String E_BACNET_ASN_NO_CLOSING_TAG = "No closing tag";
   String E_BACNET_ASN_INVALID_TAG_CLASS = "Invalid tag class";
   String E_BACNET_ASN_INVALID_TAG_NUMBER = "Invalid tag number";
   String E_BACNET_ASN_INVALID_LENGTH_TAG = "Invalid length tag";
   String E_BACNET_ASN_INCONSISTENT_LENGTH = "Inconsistent length";
   String E_BACNET_ASN_CONVERSION_FAILED = "Asn conversion failed";
   String E_BACNET_ASN_INVALID_BIT_STRING_LENGTH = "Invalid bit string length";
   String E_BACNET_ASN_INVALID_BINARY_PV_VALUE = "Invalid BacnetBinaryPv value";
   String E_BACNET_ASN_FROM_ASN_DEPRECATED = "deprecated: use fromAsnBytes() instead!";
   String E_BACNET_ASN_INVALID_PARAMETER = "Invalid parameter ";
   int STATUS_FLAGS_LENGTH = 4;
   int SIGN_EXTEND = -256;
}
