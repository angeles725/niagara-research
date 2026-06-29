package javax.baja.bacnet.io;

import javax.baja.bacnet.datatypes.BBacnetDateTime;

public interface RangeReference extends PropertyReference {
   int BY_POSITION = 3;
   int BY_TIME_DEPRECATED = 4;
   int TIME_RANGE_DEPRECATED = 5;
   int BY_SEQUENCE_NUMBER = 6;
   int BY_TIME = 7;

   int getRangeType();

   long getReferenceIndex();

   BBacnetDateTime getReferenceTime();

   int getCount();
}
