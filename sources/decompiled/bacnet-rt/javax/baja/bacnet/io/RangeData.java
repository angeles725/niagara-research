package javax.baja.bacnet.io;

import javax.baja.bacnet.datatypes.BBacnetBitString;

public interface RangeData extends PropertyReference {
   int RESULT_FLAGS = 3;
   int ITEM_COUNT = 4;
   int ITEM_DATA_NO_SEQ_NUM = 5;
   int FIRST_SEQUENCE_NUMBER = 6;
   int ITEM_DATA = 7;

   BBacnetBitString getResultFlags();

   boolean includesFirstItem();

   boolean includesLastItem();

   boolean isMoreItems();

   long getItemCount();

   long getFirstSequenceNumber();

   byte[] getItemData();

   ErrorType getError();

   int getErrorClass();

   int getErrorCode();

   boolean isError();
}
