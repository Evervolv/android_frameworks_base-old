package com.oneplus.os;

/**
 * @hide
 * @author clee
 * licx oem_service
 */
interface IOemService {

    /******************************************************
     *
     * This part is for writing raw partition
     *
     ******************************************************/
    String readRawPartition(int offset, int size);
    int writeRawPartition(String content);
    /******************************************************
     *
     * This part is for writing critical data
     *
     ******************************************************/
    String readCriticalData(int id, int size);
    int writeCriticalData(int id, String content);
}
