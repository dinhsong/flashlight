package com.betaorion.flashlight;

import android.app.IntentService;
import android.app.NotificationManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class FlashlightIntentService extends IntentService {
    // Fields
    private static Camera camera;
    private static int[] ids;

    private static boolean canWork = false;

    // ACTIONS
    public static final String ACTION_TURN_ON = "com.betaorion.flashlight.TURN_ON";
    public static final String ACTION_TURN_OFF = "com.betaorion.flashlight.TURN_OFF";

    public FlashlightIntentService() {
        super("FlashlightIntentService");
    }

    private void fillFields() {
        // Initialize the fields
        canWork = this.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        // Get the IDs if any.
        ids = AppWidgetManager.getInstance(this)
                .getAppWidgetIds(new ComponentName(this, FlashLightWidget.class));


        if(canWork) {
            // Get the camera instance
            if(camera == null) {
                // Only if the camera object is null. Otherwise,
                // we may have captured the camera instance in last visit.
                try {
                    camera = Camera.open();

                    // Set the properties.
                    Camera.Parameters p = camera.getParameters();

                    // Set it to flash mode
                    p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    camera.setParameters(p);
                } catch (Exception ignored) {
                    notifyUser();
                }
            }
        } else {
            notifyUser();
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Fill the fields
        fillFields();

        // Handle the intent
        if(intent.getAction().equals(ACTION_TURN_OFF)) {
            alter(false);
        } else if(intent.getAction().equals(ACTION_TURN_ON)) {
            alter(true);
        }
    }

    private void alter(boolean on) {
        if(canWork) {
            if(on) {
                try {
                    // Start the "flashlight"
                    camera.startPreview();
                    FlashLightWidget.lightOn = true;
                    notifyWidgets();
                } catch (Exception e) {
                    Toast.makeText(FlashlightIntentService.this, e.getMessage(), Toast.LENGTH_SHORT)
                            .show();
                }
            } else {
                try {
                    // Stop everything and just release the resource
                    camera.stopPreview();
                    camera.release();
                    camera = null;
                    FlashLightWidget.lightOn = false;
                    notifyWidgets();
                } catch (Exception e) {
                    Toast.makeText(FlashlightIntentService.this, e.getMessage(), Toast.LENGTH_SHORT)
                            .show();
                }
            }
        } else {
            notifyUser();
        }
    }

    private void notifyWidgets() {
        if(ids != null && ids.length > 0) {
            for (int id : ids) {
                FlashLightWidget.updateAppWidget(this, AppWidgetManager.getInstance(this), id);
            }
        }
    }

    private void notifyUser() {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("Cannot access flashlight")
                        .setContentText("Camera application may be running in a background application.");

        int mId = 1234;
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(mId, mBuilder.build());
    }
}
