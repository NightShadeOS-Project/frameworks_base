/*
 * Copyright (C) 2022 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.systemui.keyguard.domain.interactor

import android.animation.ValueAnimator
import com.android.systemui.animation.Interpolators
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Application
import com.android.systemui.keyguard.data.repository.KeyguardTransitionRepository
import com.android.systemui.keyguard.shared.model.KeyguardState
import com.android.systemui.keyguard.shared.model.TransitionInfo
import com.android.systemui.keyguard.shared.model.WakefulnessModel.Companion.isSleepingOrStartingToSleep
import com.android.systemui.keyguard.shared.model.WakefulnessModel.Companion.isWakingOrStartingToWake
import com.android.systemui.util.kotlin.sample
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@SysUISingleton
class AodLockscreenTransitionInteractor
@Inject
constructor(
    @Application private val scope: CoroutineScope,
    private val keyguardInteractor: KeyguardInteractor,
    private val keyguardTransitionRepository: KeyguardTransitionRepository,
    private val keyguardTransitionInteractor: KeyguardTransitionInteractor,
) : TransitionInteractor("AOD<->LOCKSCREEN") {

    override fun start() {
        scope.launch {
            /*
             * Listening to the startedKeyguardTransitionStep (last started step) allows this code
             * to interrupt an active transition, as long as they were either going to LOCKSCREEN or
             * AOD state. One example is when the user presses the power button in the middle of an
             * active transition.
             */
            keyguardInteractor.wakefulnessState
                .sample(
                    keyguardTransitionInteractor.startedKeyguardTransitionStep,
                    { a, b -> Pair(a, b) }
                )
                .collect { pair ->
                    val (wakefulnessState, lastStartedStep) = pair
                    if (
                        isSleepingOrStartingToSleep(wakefulnessState) &&
                            lastStartedStep.to == KeyguardState.LOCKSCREEN
                    ) {
                        keyguardTransitionRepository.startTransition(
                            TransitionInfo(
                                name,
                                KeyguardState.LOCKSCREEN,
                                KeyguardState.AOD,
                                getAnimator(),
                            )
                        )
                    } else if (
                        isWakingOrStartingToWake(wakefulnessState) &&
                            lastStartedStep.to == KeyguardState.AOD
                    ) {
                        keyguardTransitionRepository.startTransition(
                            TransitionInfo(
                                name,
                                KeyguardState.AOD,
                                KeyguardState.LOCKSCREEN,
                                getAnimator(),
                            )
                        )
                    }
                }
        }
    }

    private fun getAnimator(): ValueAnimator {
        return ValueAnimator().apply {
            setInterpolator(Interpolators.LINEAR)
            setDuration(TRANSITION_DURATION_MS)
        }
    }

    companion object {
        private const val TRANSITION_DURATION_MS = 500L
    }
}
