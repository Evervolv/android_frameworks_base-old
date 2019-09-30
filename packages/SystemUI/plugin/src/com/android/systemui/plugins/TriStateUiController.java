/*
 * Copyright (C) 2019 CypherOS
 * Copyright 2014-2019 Paranoid Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.plugins;

import com.android.systemui.plugins.annotations.DependsOn;
import com.android.systemui.plugins.annotations.ProvidesInterface;
import com.android.systemui.plugins.VolumeDialog.Callback;

@ProvidesInterface(action = TriStateUiController.ACTION, version = TriStateUiController.VERSION)
@DependsOn(target = Callback.class)
public interface TriStateUiController extends Plugin {
    String ACTION = "com.android.systemui.action.PLUGIN_TRI_STATE_UI";
    int VERSION = 1;

    public interface UserActivityListener {
        void onTriStateUserActivity();
    }
}
