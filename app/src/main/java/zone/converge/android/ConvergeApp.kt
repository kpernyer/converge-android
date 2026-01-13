// Copyright 2024-2025 Aprio One AB, Sweden
// SPDX-License-Identifier: MIT

package zone.converge.android

import android.app.Application
import zone.converge.android.grpc.ConvergeClient
import zone.converge.android.ml.*

/**
 * Converge Android Application.
 *
 * Initializes:
 * - gRPC client
 * - ML prediction engines
 * - Behavior storage
 */
class ConvergeApp : Application() {

    lateinit var convergeClient: ConvergeClient
        private set

    lateinit var behaviorStore: BehaviorStore
        private set

    lateinit var actionPredictor: ActionPredictor
        private set

    lateinit var outcomeTracker: OutcomeTracker
        private set

    lateinit var jtbdPredictor: JTBDPredictor
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize stores
        behaviorStore = BehaviorStore(this)

        // Initialize predictors
        outcomeTracker = OutcomeTracker(behaviorStore)
        jtbdPredictor = JTBDPredictor(outcomeTracker)
        actionPredictor = ActionPredictor(this, behaviorStore)
        actionPredictor.initialize()

        // Initialize gRPC client (will connect on demand)
        convergeClient = ConvergeClient()
    }

    override fun onTerminate() {
        super.onTerminate()
        actionPredictor.close()
    }
}
