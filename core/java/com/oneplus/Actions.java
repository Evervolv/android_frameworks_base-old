/*
 * Copyright (C) 2015 OnePlus Project
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

package com.oneplus;

/**
 * OnePlus special actions.
 *
 * @hide
 */
public class Actions {

    /**
     * The Intent sent by the TriStateKeyService on switch state change.
     * Includes the {@link #TRI_STATE_KEY_INTENT_EXTRA}.
     * @hide
     */
    public static final String TRI_STATE_KEY_INTENT =
            "com.oem.intent.action.THREE_KEY_MODE";

    /**
     * The Intent sent by the TriStateKeyService on boot.
     * Includes the {@link #TRI_STATE_KEY_INTENT_EXTRA}.
     * @hide
     */
    public static final String TRI_STATE_KEY_BOOT_INTENT =
            "com.oem.intent.action.THREE_KEY_MODE_BOOT";

    /**
     * The Intent extra included with the {@link TRI_STATE_KEY_INTENT}.
     * Valid values are 1,2,3 representing the top,middle,bottom switch state.
     * @hide
     */
    public static final String TRI_STATE_KEY_INTENT_EXTRA = "switch_state";


}
