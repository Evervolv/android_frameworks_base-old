package com.evervolv.android.systemui;

import android.content.Context;

import com.evervolv.android.systemui.dagger.EVGlobalRootComponent;
import com.evervolv.android.systemui.dagger.DaggerEVGlobalRootComponent;

import com.android.systemui.SystemUIFactory;
import com.android.systemui.dagger.GlobalRootComponent;

public class EVSystemUIFactory extends SystemUIFactory {
    @Override
    protected GlobalRootComponent buildGlobalRootComponent(Context context) {
        return DaggerEVGlobalRootComponent.builder()
                .context(context)
                .build();
    }
}
