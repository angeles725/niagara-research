package com.tridium.modbusCore;

public interface ModbusErrorCodes {
   int E_INVALID_FUNCTION_CODE = 100;
   int E_COMMUNICATIONS_ERROR = 101;
   int REGISTER_NOT_POLLED_BY_DEVICE = 102;
   int DATA_NOT_AVAILABLE = 103;
   int MODBUS_TCP_COULD_NOT_CONNECT = 104;
}
