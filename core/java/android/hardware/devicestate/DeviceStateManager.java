/*
 * Copyright (C) 2020 The Android Open Source Project
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

package android.hardware.devicestate;

import android.Manifest;
import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SystemService;
import android.annotation.TestApi;
import android.content.Context;

import com.android.internal.util.ArrayUtils;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Manages the state of the system for devices with user-configurable hardware like a foldable
 * phone.
 *
 * @hide
 */
@TestApi
@SystemService(Context.DEVICE_STATE_SERVICE)
public final class DeviceStateManager {
    /**
     * Invalid device state.
     *
     * @hide
     */
    public static final int INVALID_DEVICE_STATE = -1;

    /** The minimum allowed device state identifier. */
    public static final int MINIMUM_DEVICE_STATE = 0;

    /** The maximum allowed device state identifier. */
    public static final int MAXIMUM_DEVICE_STATE = 255;

    /**
     * Intent needed to launch the rear display overlay activity from SysUI
     *
     * @hide
     */
    public static final String ACTION_SHOW_REAR_DISPLAY_OVERLAY =
            "com.android.intent.action.SHOW_REAR_DISPLAY_OVERLAY";

    /**
     * Intent extra sent to the rear display overlay activity of the current base state
     *
     * @hide
     */
    public static final String EXTRA_ORIGINAL_DEVICE_BASE_STATE =
            "original_device_base_state";

    private final DeviceStateManagerGlobal mGlobal;

    /** @hide */
    public DeviceStateManager() {
        DeviceStateManagerGlobal global = DeviceStateManagerGlobal.getInstance();
        if (global == null) {
            throw new IllegalStateException(
                    "Failed to get instance of global device state manager.");
        }
        mGlobal = global;
    }

    /**
     * Returns the list of device states that are supported and can be requested with
     * {@link #requestState(DeviceStateRequest, Executor, DeviceStateRequest.Callback)}.
     */
    @NonNull
    public int[] getSupportedStates() {
        return mGlobal.getSupportedStates();
    }

    /**
     * Submits a {@link DeviceStateRequest request} to modify the device state.
     * <p>
     * By default, the request is kept active until one of the following occurs:
     * <ul>
     *     <li>The system deems the request can no longer be honored, for example if the requested
     *     state becomes unsupported.
     *     <li>A call to {@link #cancelStateRequest}.
     *     <li>Another processes submits a request succeeding this request in which case the request
     *     will be canceled.
     * </ul>
     * However, this behavior can be changed by setting flags on the {@link DeviceStateRequest}.
     *
     * @throws IllegalArgumentException if the requested state is unsupported.
     * @throws SecurityException if the caller is neither the current top-focused activity nor if
     * the {@link android.Manifest.permission#CONTROL_DEVICE_STATE} permission is held.
     *
     * @see DeviceStateRequest
     */
    @RequiresPermission(value = android.Manifest.permission.CONTROL_DEVICE_STATE,
            conditional = true)
    public void requestState(@NonNull DeviceStateRequest request,
            @Nullable @CallbackExecutor Executor executor,
            @Nullable DeviceStateRequest.Callback callback) {
        mGlobal.requestState(request, executor, callback);
    }

    /**
     * Cancels the active {@link DeviceStateRequest} previously submitted with a call to
     * {@link #requestState(DeviceStateRequest, Executor, DeviceStateRequest.Callback)}.
     * <p>
     * This method is noop if there is no request currently active.
     *
     * @throws SecurityException if the caller is neither the current top-focused activity nor if
     * the {@link android.Manifest.permission#CONTROL_DEVICE_STATE} permission is held.
     */
    @RequiresPermission(value = android.Manifest.permission.CONTROL_DEVICE_STATE,
            conditional = true)
    public void cancelStateRequest() {
        mGlobal.cancelStateRequest();
    }

    /**
     * Submits a {@link DeviceStateRequest request} to override the base state of the device. This
     * should only be used for testing, where you want to simulate the physical change to the
     * device state.
     * <p>
     * By default, the request is kept active until one of the following occurs:
     * <ul>
     *     <li>The physical state of the device changes</li>
     *     <li>The system deems the request can no longer be honored, for example if the requested
     *     state becomes unsupported.
     *     <li>A call to {@link #cancelBaseStateOverride}.
     *     <li>Another processes submits a request succeeding this request in which case the request
     *     will be canceled.
     * </ul>
     *
     * Submitting a base state override request may not cause any change in the presentation
     * of the system if there is an emulated request made through {@link #requestState}, as the
     * emulated override requests take priority.
     *
     * @throws IllegalArgumentException if the requested state is unsupported.
     * @throws SecurityException if the caller does not hold the
     * {@link android.Manifest.permission#CONTROL_DEVICE_STATE} permission.
     *
     * @see DeviceStateRequest
     */
    @RequiresPermission(android.Manifest.permission.CONTROL_DEVICE_STATE)
    public void requestBaseStateOverride(@NonNull DeviceStateRequest request,
            @Nullable @CallbackExecutor Executor executor,
            @Nullable DeviceStateRequest.Callback callback) {
        mGlobal.requestBaseStateOverride(request, executor, callback);
    }

    /**
     * Cancels the active {@link DeviceStateRequest} previously submitted with a call to
     * {@link #requestBaseStateOverride(DeviceStateRequest, Executor, DeviceStateRequest.Callback)}.
     * <p>
     * This method is noop if there is no base state request currently active.
     *
     * @throws SecurityException if the caller does not hold the
     * {@link android.Manifest.permission#CONTROL_DEVICE_STATE} permission.
     */
    @RequiresPermission(Manifest.permission.CONTROL_DEVICE_STATE)
    public void cancelBaseStateOverride() {
        mGlobal.cancelBaseStateOverride();
    }

    /**
     * Registers a callback to receive notifications about changes in device state.
     *
     * @param executor the executor to process notifications.
     * @param callback the callback to register.
     *
     * @see DeviceStateCallback
     */
    public void registerCallback(@NonNull @CallbackExecutor Executor executor,
            @NonNull DeviceStateCallback callback) {
        mGlobal.registerDeviceStateCallback(callback, executor);
    }

    /**
     * Unregisters a callback previously registered with
     * {@link #registerCallback(Executor, DeviceStateCallback)}.
     */
    public void unregisterCallback(@NonNull DeviceStateCallback callback) {
        mGlobal.unregisterDeviceStateCallback(callback);
    }

    /** Callback to receive notifications about changes in device state. */
    public interface DeviceStateCallback {
        /**
         * Called in response to a change in the states supported by the device.
         * <p>
         * Guaranteed to be called once on registration of the callback with the initial value and
         * then on every subsequent change in the supported states.
         *
         * @param supportedStates the new supported states.
         *
         * @see DeviceStateManager#getSupportedStates()
         */
        default void onSupportedStatesChanged(@NonNull int[] supportedStates) {}

        /**
         * Called in response to a change in the base device state.
         * <p>
         * The base state is the state of the device without considering any requests made through
         * calls to {@link #requestState(DeviceStateRequest, Executor, DeviceStateRequest.Callback)}
         * from any client process. The base state is guaranteed to match the state provided with a
         * call to {@link #onStateChanged(int)} when there are no active requests from any process.
         * <p>
         * Guaranteed to be called once on registration of the callback with the initial value and
         * then on every subsequent change in the non-override state.
         *
         * @param state the new base device state.
         */
        default void onBaseStateChanged(int state) {}

        /**
         * Called in response to device state changes.
         * <p>
         * Guaranteed to be called once on registration of the callback with the initial value and
         * then on every subsequent change in device state.
         *
         * @param state the new device state.
         */
        void onStateChanged(int state);
    }

    /**
     * Listens to changes in device state and reports the state as folded if the device state
     * matches the value in the {@link com.android.internal.R.integer.config_foldedDeviceState}
     * resource.
     * @hide
     */
    public static class FoldStateListener implements DeviceStateCallback {
        private final int[] mFoldedDeviceStates;
        private final Consumer<Boolean> mDelegate;

        @Nullable
        private Boolean lastResult;

        public FoldStateListener(Context context) {
            this(context, folded -> {});
        }

        public FoldStateListener(Context context, Consumer<Boolean> listener) {
            mFoldedDeviceStates = context.getResources().getIntArray(
                    com.android.internal.R.array.config_foldedDeviceStates);
            mDelegate = listener;
        }

        @Override
        public final void onStateChanged(int state) {
            final boolean folded = ArrayUtils.contains(mFoldedDeviceStates, state);

            if (lastResult == null || !lastResult.equals(folded)) {
                lastResult = folded;
                mDelegate.accept(folded);
            }
        }

        @Nullable
        public Boolean getFolded() {
            return lastResult;
        }
    }
}
