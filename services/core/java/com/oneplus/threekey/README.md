#architecture
架构相关一些实现,不会去涉及具体的实现

##ThreeKeyInterface: 三段式接口,没什么功能

##ThreeKeyBase:三段式基本功能实现,如监听三段式按键变化,发出三段式变化广播,重置三段式的功能

三段式的几个状态(与Audio几个状态分离,利于重新映射和重用)

    private static final int SWITCH_STATE_ON = 1;
    private static final int SWITCH_STATE_MIDDLE = 2;
    private static final int SWITCH_STATE_DOWN = 3;
    private static final int SWITCH_STATE_UNINIT = -1;

设置三段式状态

    void setSwitchState(int switchState);

几个状态的实现回调接口,子类需要重载这三个接口来实现三段式的功能

    protected void setUp() {}
    protected void setMiddle() {}
    protected void setDown() {}

初始化和重置三段式(因为配置文件更改,需要重置三段式的接口

    public void init(int switchState);
    protected void reset();

注册三段式广播(由于对底层的访问还放在OemExService里,通过广播来通知三段式的变化,这里通过广播来监听三段式的变化)

    private void register();
    private class ThreeKeyBroadcastReceiver;


##ThreeKey:三段式具体功能的实现

三段式几个状态的实现,具体的工作又委托给各个ThreeKeyPolicyHelper

    protected void setUp();
    protected void setMiddle();
    protected void setDown();

ThreeKeyPolicyHelper列表,三段式的切换可能引起多方面的变化(切换时振动,音频策略变化等)

    private ArrayList<ThreeKeyPolicyHelper> helpers
    public void setThreeKeyPolicyHelper(ThreeKeyPolicyHelper helper);

重载初始化和重置函数,由于初始化和重置的时候不想要打扰用户(振动反馈等),所以给ThreeKeyPolicyHelper添加init mode

    public void init(int switchState);
    protected void reset();
    (调用Threekey的reset的调用链  ThreeKey.reset -> ThreeKeyBase.reset -> ThreeKey.init -> ThreeKeyBase.init -> ThreeKeyBase.setSwitchState)

设置的Watcher,用于监听三段式的变化(mSettingWatcher的实现是一旦三段式的设置变化了,便去重置三段式
(原先考虑把监控设置变换的代码放在ThreeKey里,这样就不需要SettingsWatcher了,不过这样会将音频方面的设置的逻辑散落在不同的地方,最后还是把音频设置处理放在了ThreeKeyAudioPolicyHelper里)

    public interface SettingsWatcher;
    private SettingsWatcher mSettingWatcher;

#implement
三段式各个模式下的具体实现

##ThreeKeyPolicyHelper:具体实现的实现接口

    void setSlient();
    void setDontDisturb();
    void setRing();

##ThreeKeyAudioPolicyHelper:Audio相关三段式的调整
音频几个模式的实现

    void setSlient();
    void setDontDisturb();
    void setRing();

初始化相关功能

    void setInitMode(boolean isInit);

ThreeKey监听设置变化,提供给ThreeKeyPolicyHelper重置ThreeKey的能力

    void setSettingsWatcher(SettingsWatcher settingsWatcher);

##ThreeKeyVibratorPolicyHelper: 调整三段式振动实现


#添加的api
在AudioManager与NotificationManager中分别有添加接口用于实现三段式的一些功能

##AudioManager

    void setOnePlusRingVolumeRange(int min,int max);

用于限制系统音量调节,实现如静音等功能**该功能会修改sdk的api**

##NotificationManager

    void setOnePlusVibrateInSilentMode(boolean vibrateFlag);

用于强制修改ZEN\_MODE\_ALARMS模式下的RingerMode

#hardware (未完成)
##ThreeKeyHw:对底层的封装


#三段式相关的一些类
实现
[ThreeKeyInterface](vendor/oneplus/framework/base/services/core/java/com/oneplus/threekey/ThreeKeyInterface.java)
[ThreeKeyBase](vendor/oneplus/framework/base/services/core/java/com/oneplus/threekey/ThreeKeyBase.java)
[ThreeKey](vendor/oneplus/framework/base/services/core/java/com/oneplus/threekey/ThreeKey.java)
[ThreeKeyPolicyHelper](vendor/oneplus/framework/base/services/core/java/com/oneplus/threekey/ThreeKeyPolicyHelper.java)
[ThreeKeyAudioPolicyHelper](vendor/oneplus/framework/base/services/core/java/com/oneplus/threekey/ThreeKeyAudioPolicyHelper.java)
[ThreeKeyVibratorPolicyHelper](vendor/oneplus/framework/base/services/core/java/com/oneplus/threekey/ThreeKeyVibratorPolicyHelper.java)
[ThreeKeyHw](vendor/oneplus/framework/base/services/core/java/com/oneplus/threekey/ThreeKeyHw.java)
