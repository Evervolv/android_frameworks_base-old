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

import com.oneplus.os.IOemExInputCallBack;
import com.oneplus.os.IOemUeventCallback;
import com.oneplus.os.IThreeKeyPolicy;

/** @hide */
interface IOemExService {

//#ifdef VENDOR_EDIT
//hovanchen, 2016/10/17, Add for Install customization applicaiton while inserting India SIM cards.
void startApkInstall();
//#endif /* VENDOR_EDIT */

boolean registerInputEvent(IOemExInputCallBack callBack , int keycode);

void unregisterInputEvent(IOemExInputCallBack callBack);

void pauseExInputEvent();

void resumeExInputEvent();

boolean startUevent ( String patch, IOemUeventCallback callBack );

boolean stopUevent ( IOemUeventCallback callBack);

boolean setInteractive ( boolean interactive ,long delayMillis );

boolean setSystemProperties ( String key ,String value); 

boolean setKeyMode ( int keyMode );

 boolean setHomeUpLock ( );

 void setGammaData(int val);

 void setLaserSensorOffset(int val);

 void setLaserSensorCrossTalk(int val);


    void disableDefaultThreeKey();

    void enalbeDefaultThreeKey();

    void addThreeKeyPolicy(IThreeKeyPolicy policy);

    void removeThreeKeyPolicy(IThreeKeyPolicy policy);

    void resetThreeKey();

    int getThreeKeyStatus();
}
