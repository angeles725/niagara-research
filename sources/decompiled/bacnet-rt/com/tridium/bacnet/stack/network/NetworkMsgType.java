package com.tridium.bacnet.stack.network;

public interface NetworkMsgType {
   int WHO_IS_ROUTER_TO_NETWORK = 0;
   int I_AM_ROUTER_TO_NETWORK = 1;
   int I_COULD_BE_ROUTER_TO_NETWORK = 2;
   int REJECT_MESSAGE_TO_NETWORK = 3;
   int ROUTER_BUSY_TO_NETWORK = 4;
   int ROUTER_AVAILABLE_TO_NETWORK = 5;
   int INITIALIZE_ROUTING_TABLE = 6;
   int INITIALIZE_ROUTING_TABLE_ACK = 7;
   int ESTABLISH_CONNECTION_TO_NETWORK = 8;
   int DISCONNECT_CONNECTION_TO_NETWORK = 9;
   int CHALLENGE_REQUEST = 10;
   int SECURITY_PAYLOAD = 11;
   int SECURITY_RESPONSE = 12;
   int REQUEST_KEY_UPDATE = 13;
   int UPDATE_KEY_SET = 14;
   int UPDATE_DISTRIBUTION_KEY = 15;
   int REQUEST_MASTER_KEY = 16;
   int SET_MASTER_KEY = 17;
   int WHAT_IS_NETWORK_NUMBER = 18;
   int NETWORK_NUMBER_IS = 19;
   int FIRST_ASHRAE_RESERVED_NET_MSG_TYPE = 20;
   int LAST_ASHRAE_RESERVED_NET_MSG_TYPE = 127;
   String[] TAGS = new String[]{
      "Who-Is-Router-To-Network",
      "I-Am-Router-To-Network",
      "I-Could-Be-Router-To-Network",
      "Reject-Message-To-Network",
      "Router-Busy-To-Network",
      "Router-Available-To-Network",
      "Initialize-Routing-Table",
      "Initialize-Routing-Table-Ack",
      "Establish-Connection-To-Network",
      "Disconnect-Connection-To-Network",
      "Challenge-Request",
      "Security-Payload",
      "Security-Response",
      "Request-Key-Update",
      "Update-Key-Set",
      "Update-Distribution-Key",
      "Request-Master-Key",
      "Set-Master-Key",
      "What-Is-Network-Number",
      "Network-Number-Is"
   };
   int VENDOR_PROPRIETARY_BIT = 128;
}
