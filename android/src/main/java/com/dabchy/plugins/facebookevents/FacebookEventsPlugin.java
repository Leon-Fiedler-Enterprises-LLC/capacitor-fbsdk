package com.dabchy.plugins.facebookevents;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.facebook.FacebookSdk;
import com.facebook.LoggingBehavior;
import com.facebook.appevents.AppEventsLogger;
import com.getcapacitor.JSObject;
import com.getcapacitor.Logger;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.util.ArrayList;
import java.util.List;

@CapacitorPlugin(name = "FacebookEvents")
public class FacebookEventsPlugin extends Plugin {

    private static final String LOG_TAG = "FacebookEventsPlugin";

    // Without FacebookInitProvider, the SDK starts this long after the plugin loads,
    // well past app launch.
    private static final long DEFERRED_INITIALIZATION_DELAY_MS = 10_000;

    private FacebookEvents facebookEvents;

    private volatile boolean sdkReady = false;

    // Plugin calls made before the SDK is ready. Only touched on the plugin thread.
    private final List<Runnable> pendingCalls = new ArrayList<>();

    @Override
    public void load() {
        facebookEvents = new FacebookEvents(this);

        if (FacebookSdk.isInitialized()) {
            // FacebookInitProvider already initialized the SDK during process start.
            initializeFacebookSdk();
            sdkReady = true;
            return;
        }

        // The app removed FacebookInitProvider to keep the SDK off the launch path.
        new Handler(Looper.getMainLooper()).postDelayed(
            () -> bridge.execute(this::initializeAfterStartup),
            DEFERRED_INITIALIZATION_DELAY_MS
        );
    }

    @Override
    protected void handleOnStart() {
        super.handleOnStart();
        // Before that, the deferred initialization activates the app.
        if (!sdkReady) {
            return;
        }

        Application application = resolveApplication();

        if (application == null) {
            Logger.warn(LOG_TAG, "Unable to activate Facebook App Events because the application reference is null.");
            return;
        }

        initializeFacebookSdk();
        AppEventsLogger.activateApp(application);
    }

    @Override
    protected void handleOnStop() {
        super.handleOnStop();
    }

    @Override
    protected void handleOnDestroy() {
        super.handleOnDestroy();
        if (facebookEvents != null) {
            facebookEvents.reset();
        }
    }

    @PluginMethod
    public void setAdvertiserTrackingEnabled(PluginCall call) {
        Boolean enabled = call.getBoolean("enabled");

        if (enabled == null) {
            call.reject("Missing enabled argument");
            return;
        }

        whenSdkReady(() -> {
            initializeFacebookSdk();
            FacebookSdk.setAdvertiserIDCollectionEnabled(enabled);
            FacebookSdk.setAutoLogAppEventsEnabled(enabled);
            FacebookSdk.setAutoInitEnabled(enabled);
        });
        call.resolve();
    }

    @PluginMethod
    public void logEvent(PluginCall call) {
        String event = call.getString("event");
        if (event == null || event.trim().isEmpty()) {
            call.reject("Missing event argument");
            return;
        }

        JSObject params = call.getObject("params", new JSObject());
        whenSdkReady(() -> {
            initializeFacebookSdk();

            if (facebookEvents == null) {
                facebookEvents = new FacebookEvents(this);
            }

            facebookEvents.logEvent(event, params);
        });
        call.resolve();
    }


    @PluginMethod
    public void logPurchase(PluginCall call) {
        Double amount = call.getDouble("amount");
        String currencyString = call.getString("currency");
        String transactionId = call.getString("transactionId");
        String productId = call.getString("productId");
        JSObject params = call.getObject("params", new JSObject());

        if (amount == null || currencyString == null || transactionId == null || productId == null) {
            call.reject("Amount, Currency, Transaction ID, and Product ID are required");
            return;
        }

        whenSdkReady(() -> {
            initializeFacebookSdk();

            if (facebookEvents == null) {
                facebookEvents = new FacebookEvents(this);
            }

            facebookEvents.logPurchase(amount, currencyString, transactionId, productId, params);
        });

        call.resolve();
    }


    @PluginMethod
    public void getFBAnonymousID(PluginCall call) {
        // Resolves once the SDK is ready, which may be after the startup delay.
        whenSdkReady(() -> {
            initializeFacebookSdk();

            Application application = resolveApplication();
            if (application == null) {
                call.reject("Unable to get anonymous ID because the application reference is null.");
                return;
            }

            try {
                String anonymousId = AppEventsLogger.getAnonymousAppDeviceGUID(application);
                JSObject result = new JSObject();
                result.put("anonymousID", anonymousId);
                call.resolve(result);
            } catch (Exception e) {
                call.reject("Failed to get anonymous ID: " + e.getMessage());
            }
        });
    }

    /** Runs on the plugin thread. */
    private void whenSdkReady(Runnable work) {
        if (sdkReady) {
            work.run();
        } else {
            pendingCalls.add(work);
        }
    }

    /** Runs on the plugin thread, so SDK setup stays off the main thread. */
    private void initializeAfterStartup() {
        initializeFacebookSdk();
        new Handler(Looper.getMainLooper()).post(this::activateAfterStartup);
    }

    /** Runs on the main thread, like the activation in handleOnStart. */
    private void activateAfterStartup() {
        Application application = resolveApplication();
        if (application != null) {
            AppEventsLogger.activateApp(application);
        }

        sdkReady = true;
        bridge.execute(() -> {
            List<Runnable> calls = new ArrayList<>(pendingCalls);
            pendingCalls.clear();
            for (Runnable call : calls) {
                call.run();
            }
        });
    }

    private void initializeFacebookSdk() {
        Application application = resolveApplication();
        if (application == null) {
            Logger.warn(LOG_TAG, "Unable to initialize the Facebook SDK because the application reference is null.");
            return;
        }

        Context appContext = application.getApplicationContext();
        if (appContext == null) {
            Logger.warn(LOG_TAG, "Unable to initialize the Facebook SDK because the application context is null.");
            return;
        }

        synchronized (FacebookEventsPlugin.class) {
            if (!FacebookSdk.isInitialized()) {
                FacebookSdk.sdkInitialize(appContext);
            }

            // Check if app is in debug mode
            boolean isDebugMode = (appContext.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            if (isDebugMode) {
                FacebookSdk.setIsDebugEnabled(true);
                FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS);
            }

            FacebookSdk.setAutoInitEnabled(true);
            FacebookSdk.setAutoLogAppEventsEnabled(false);
            FacebookSdk.setAdvertiserIDCollectionEnabled(true);
        }
    }

    @Nullable
    private Application resolveApplication() {
        AppCompatActivity activity = getActivity();
        if (activity != null) {
            return activity.getApplication();
        }

        Context context = getContext();
        if (context != null) {
            Context appContext = context.getApplicationContext();
            if (appContext instanceof Application) {
                return (Application) appContext;
            }
        }

        return null;
    }

    
}
