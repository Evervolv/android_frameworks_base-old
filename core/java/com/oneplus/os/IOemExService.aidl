/********************************************************************************
 ** Copyright (C), 2008-2015, OE obile Comm Corp., Ltd
 ** VENDOR_EDIT, All rights reserved.
 **
 ** File: - IOemExService.aidl
 ** Description:
 **     oem ex service, three pointers move shot screen
 **
 ** Version: 1.0
 ** Date: 2015-04-13
 ** Author: liuhuisheng@Framework
 **
 ** ------------------------------- Revision History: ----------------------------
 ** <author>                        <data>       <version>   <desc>
 ** ------------------------------------------------------------------------------
 ** liuhuisheng@Framework          2015-04-13   1.0         Create this moudle
 ********************************************************************************/

package com.oneplus.os;

import com.oneplus.os.IThreeKeyPolicy;

/** @hide */
interface IOemExService {
    void disableDefaultThreeKey();
    void enalbeDefaultThreeKey();
    void addThreeKeyPolicy(IThreeKeyPolicy policy);
    void removeThreeKeyPolicy(IThreeKeyPolicy policy);
    void resetThreeKey();
    int getThreeKeyStatus();
}
