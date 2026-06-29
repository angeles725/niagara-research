package com.tridium.bacnet.stack;

public interface BacnetStackErrorCodes {
   String E_BACNET_STACK_LINK_ETHERNET_DRIVER_FAILED = "Ethernet driver failed to open";
   String E_BACNET_STACK_NETWORK_INVALID_MESSAGE = "Invalid Network Layer Message";
   String E_BACNET_STACK_NETWORK_UNSUPPORTED_MESSAGE = "Unsupported Network Layer Message";
   String E_BACNET_STACK_NETWORK_UNSUPPORTED_PROTOCOL_VERSION = "Unsupported Protocol Version";
   String E_BACNET_STACK_TRANSPORT_INVALID_APDU_TYPE = "Invalid APDU type";
   String E_BACNET_STACK_TRANSPORT_INVALID_APDU_LENGTH = "Invalid APDU length";
   String E_BACNET_STACK_TRANSPORT_LOCKUP_DETECTED = "lockup: invoke ID ";
   String E_BACNET_STACK_TRANSPORT_TRANSACTION_TIMEOUT = "timeout: invoke ID ";
   String E_BACNET_STACK_TRANSPORT_TRANSACTION_TIMEOUT_INTERRUPTION = "interrupted: invoke ID ";
   String E_BACNET_STACK_TRANSPORT_INVALID_RESPONSE_TYPE = "Invalid Response Type";
   String E_BACNET_STACK_TRANSPORT_UNKNOWN_PDU_TYPE = "Unknown PDU type received";
   String E_BACNET_STACK_TRANSPORT_QUEUE_FULL = "Transport Queue Overflow";
   String E_BACNET_STACK_TRANSPORT_INVALID_INVOKE_ID = "No Invoke IDs available";
   String E_BACNET_STACK_TRANSPORT_UNRESOLVED_ADDRESS = "Unresolved device address";
   String E_BACNET_STACK_TRANSPORT_TRANSACTION_ABANDONED = "abandoned: invoke ID ";
   String E_BACNET_STACK_CANNOT_SEND = "Cannot send packet: invoke ID ";
   String E_BACNET_STACK_CLIENT_STACK_DISABLED = "Stack Disabled";
   String E_BACNET_STACK_CLIENT_BROADCAST_WRITE_DISABLED = "Cannot send writePropertyRequest to a broadcast address";
}
