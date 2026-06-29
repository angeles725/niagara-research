package com.tridium.lonworks.util.selfdoc;

public class LonMarkObjectUtil {
   public static String getObjectName(int type) {
      switch (type) {
         case 0:
            return "Node";
         case 1:
            return "OpenLoopSensor";
         case 2:
            return "ClosedLoopSensor";
         case 3:
            return "OpenLoopActuator";
         case 4:
            return "ClosedLoopActuator";
         case 5:
            return "Controller";
         case 520:
            return "AnalogInput";
         case 521:
            return "AnalogOutput";
         case 1010:
            return "LightSensor";
         case 1040:
            return "HvacTempSensor";
         case 1050:
            return "RelativeHumiditySensor";
         case 1060:
            return "OccupancySensor";
         case 1070:
            return "Co2Sensor";
         case 3040:
            return "LampActuator";
         case 3050:
            return "ConstantLight";
         case 3071:
            return "Occupancy";
         case 3200:
            return "Switch";
         case 3250:
            return "ScenePanel";
         case 3251:
            return "Scene";
         case 3252:
            return "PartitionWall";
         case 3300:
            return "RealTimeKeeper";
         case 3301:
            return "RealTimeBasedScheduler";
         case 6010:
            return "VariableSpeedMotorDrive";
         case 8010:
            return "VAV";
         case 8020:
            return "FanCoil";
         case 8030:
            return "RoofTopUnit";
         case 8040:
            return "Chiller";
         case 8051:
            return "HeatPump";
         case 8060:
            return "Thermostat";
         case 8070:
            return "Chilled Ceiling";
         case 8080:
            return "UnitVentilator";
         case 8090:
            return "SccCommandModule";
         case 8110:
            return "DamperActuator";
         case 8120:
            return "Pump";
         case 8301:
            return "Boiler";
         case 8310:
            return "Boiler";
         case 8500:
            return "SpaceComfort";
         case 8501:
            return "SccFanCoil";
         case 8502:
            return "SccVAV";
         case 8503:
            return "SccHeatPump";
         case 8504:
            return "SccRooftop";
         case 8505:
            return "SccUnitVentilator";
         case 8506:
            return "SccChilledCeiling";
         case 8507:
            return "SccRadiator";
         case 8508:
            return "SccAHU";
         case 8509:
            return "SccSelfContained";
         case 8610:
            return "DischargeAir";
         case 11002:
            return "SmokeFireInitiatorIntel";
         default:
            System.out.println("No name for LonMark profile type " + type);
            return "Type" + Integer.toString(type);
      }
   }
}
