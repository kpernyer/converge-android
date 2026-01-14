// Copyright 2024-2025 Aprio One AB, Sweden
// SPDX-License-Identifier: MIT

package zone.converge.android

import android.app.Application
import android.os.StrictMode
import io.sentry.android.core.SentryAndroid
import io.sentry.android.timber.SentryTimberIntegration
import timber.log.Timber
import zone.converge.android.grpc.ConvergeClient
import zone.converge.android.ml.*

/**
 * Converge Android Application.
 *
 * Initializes:
 * - Observability (Sentry + Timber)
 * - StrictMode (debug builds)
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

        // Initialize observability first
        initObservability()

        // Enable StrictMode in debug builds
        if (BuildConfig.STRICT_MODE) {
            enableStrictMode()
        }

        Timber.d("Converge starting...")

        // Initialize stores
        behaviorStore = BehaviorStore(this)

        // Initialize predictors
        outcomeTracker = OutcomeTracker(behaviorStore)
        jtbdPredictor = JTBDPredictor(outcomeTracker)
        actionPredictor = ActionPredictor(this, behaviorStore)
        actionPredictor.initialize()

        // Initialize gRPC client (will connect on demand)
        convergeClient = ConvergeClient()

        Timber.i("Converge initialized")
    }

    private fun initObservability() {
        // Timber for structured logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Sentry for crash reporting + performance
        SentryAndroid.init(this) { options ->
            options.dsn = "" // Set in sentry.properties or CI
            options.isEnableAutoSessionTracking = true
            options.tracesSampleRate = if (BuildConfig.DEBUG) 1.0 else 0.2
            options.isEnableUserInteractionTracing = true
            options.isEnableUserInteractionBreadcrumbs = true

            // Integrate Timber with Sentry
            options.addIntegration(
                SentryTimberIntegration(
                    minEventLevel = io.sentry.SentryLevel.ERROR,
                    minBreadcrumbLevel = io.sentry.SentryLevel.INFO,
                ),
            )
        }
    }

    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build(),
        )

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectActivityLeaks()
                .penaltyLog()
                .build(),
        )

        Timber.d("StrictMode enabled")
    }

    override fun onTerminate() {
        super.onTerminate()
        actionPredictor.close()
    }
}
