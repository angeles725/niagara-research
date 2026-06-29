package com.tridium.lonworks.netmgmt;

public interface NetMgmtConst {
   int MAX_NUM_GROUPS = 256;
   int MAX_CRITICAL_TARGETS = 5;
   int MAX_CONNECTIONS = 12287;
   boolean USE_AUTHENTICATION = true;
   boolean NO_AUTHENTICATION = false;
   int SERVICE_PIN_TIMEOUT_MINUTE = 5;
   int SERVICE_PIN_TIMEOUT_SECONDS = 300;
   int SERVICE_PIN_TIMEOUT_MILLI = 300000;
   boolean NEAR_SIDE = false;
   boolean FAR_SIDE = true;
   int UNKNOWN_DESCRIPTOR = 0;
   int STANDARD_DESCRIPTOR = 1;
   int RELIABLE_DESCRIPTOR = 2;
   int CRITICAL_DESCRIPTOR = 3;
   int AUTHENTICATED_DESCRIPTOR = 4;
}
