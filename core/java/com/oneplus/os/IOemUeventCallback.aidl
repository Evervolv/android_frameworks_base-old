/********************************************************************************
 ** Copyright (C), 2008-2015, OEM mobile Comm Corp., Ltd
 ** VENDOR_EDIT, All rights reserved.
 **
 ** File: - IOemUeventCallback.aidl
 ** Description:
 **     oem ex service, register oemExUeventCallBack
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


/** @hide */
interface IOemUeventCallback {
void onInputEvent( String message );

}
