package fr.bouyguestelecom.tv.bambient;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.graphics.Palette;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;

import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.hue.sdk.PHBridgeSearchManager;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.hue.sdk.PHMessageType;
import com.philips.lighting.hue.sdk.PHSDKListener;
import com.philips.lighting.hue.sdk.utilities.PHUtilities;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHBridgeResourcesCache;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;

import java.util.List;

import static android.view.KeyEvent.KEYCODE_DPAD_CENTER;

public class MainActivity extends Activity {


    private ImageView imageView;
    private Context mContext;

    private PHHueSDK phHueSDK;
    private PHSDKListener listener;

    private int tmp = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = getApplicationContext();
        imageView = (ImageView) findViewById(R.id.imageView);

        Bitmap b = null;
        b = BitmapFactory.decodeResource(getResources(), R.drawable.red);
        findColor(b);

        initlight();

        startService(new Intent(this, BConnectService.class));

    }

    public float[] findColor(Bitmap b) {

        Palette p = Palette.from(b).generate();
        Palette.Swatch psVibrant = p.getVibrantSwatch();
        System.out.println("psVibrant " + psVibrant);

        Palette.Swatch psVibrantLight = p.getLightVibrantSwatch();
        Palette.Swatch psVibrantDark = p.getDarkVibrantSwatch();
        Palette.Swatch psMuted = p.getMutedSwatch();
        Palette.Swatch psMutedLight = p.getLightMutedSwatch();
        Palette.Swatch psMutedDark = p.getDarkMutedSwatch();

        int color = psVibrantLight.getRgb();
        float[] xyColor = PHUtilities.calculateXY(color, null);

        System.out.println("x  = " + xyColor[0]);
        System.out.println("y  = " + xyColor[1]);
        return xyColor;


        /*List<Palette.Swatch> pss;
        pss = p.getSwatches();
        for (int j = 0; j < pss.size(); j++) {
            Palette.Swatch ps = pss.get(j);
            int color = ps.getRgb();
            int population = ps.getPopulation();
            float[] hsl = ps.getHsl();

            int bodyTextColor = ps.getBodyTextColor();
            int titleTextColor = ps.getTitleTextColor();

            System.out.println("color  = " + color);


            float[] tmp2 = PHUtilities.calculateXY(color, null);
            xyColor.add(tmp2[0]);
            xyColor.add(tmp2[1]);
            System.out.println("x  = " + tmp2[0] );
            System.out.println("y  = " + tmp2[1] );

            imageView.setBackgroundColor(color);
            return xyColor;
        }*/

    }

    void initlight() {
        listener = new PHSDKListener() {

            @Override
            public void onAccessPointsFound(List<PHAccessPoint> accessPoint) {
                // Handle your bridge search results here.  Typically if multiple results are returned you will want to display them in a list
                // and let the user select their bridge.   If one is found you may opt to connect automatically to that bridge.

                for (int i = 0; i < accessPoint.size(); i++) {
                    System.out.println("accesspoint " + accessPoint.get(i).toString());
                    phHueSDK.connect(accessPoint.get(i));
                }

            }

            @Override
            public void onCacheUpdated(List cacheNotificationsList, PHBridge bridge) {
                // Here you receive notifications that the BridgeResource Cache was updated. Use the PHMessageType to
                // check which cache was updated, e.g.
                if (cacheNotificationsList.contains(PHMessageType.LIGHTS_CACHE_UPDATED)) {
                    System.out.println("Lights Cache Updated ");
                }
            }

            @Override
            public void onBridgeConnected(PHBridge b, String username) {
                System.out.println("onBridgeConnected");

                phHueSDK.setSelectedBridge(b);
                phHueSDK.enableHeartbeat(b, PHHueSDK.HB_INTERVAL);
                // Here it is recommended to set your connected bridge in your sdk object (as above) and start the heartbeat.
                // At this point you are connected to a bridge so you should pass control to your main program/activity.
                // The username is generated randomly by the bridge.
                // Also it is recommended you store the connected IP Address/ Username in your app here.  This will allow easy automatic connection on subsequent use.

                SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(getString(R.string.bridgeIp), b.getResourceCache().getBridgeConfiguration().getIpAddress());
                editor.putString(getString(R.string.username), username);
                editor.apply();
            }

            @Override
            public void onAuthenticationRequired(PHAccessPoint accessPoint) {
                phHueSDK.startPushlinkAuthentication(accessPoint);
                // Arriving here indicates that Pushlinking is required (to prove the User has physical access to the bridge).  Typically here
                // you will display a pushlink image (with a timer) indicating to to the user they need to push the button on their bridge within 30 seconds.

                System.out.println("press button");
            }

            @Override
            public void onConnectionResumed(PHBridge bridge) {
                System.out.println("onConnectionResumed");
                phHueSDK.setSelectedBridge(bridge);
                phHueSDK.enableHeartbeat(bridge, PHHueSDK.HB_INTERVAL);

            }

            @Override
            public void onConnectionLost(PHAccessPoint accessPoint) {
                // Here you would handle the loss of connection to your bridge.
                System.out.println("onConnectionLost");

            }

            @Override
            public void onError(int code, final String message) {
                // Here you can handle events such as Bridge Not Responding, Authentication Failed and Bridge Not Found
                System.out.println("onError " + message.toString());

            }

            @Override
            public void onParsingErrors(List parsingErrorsList) {
                // Any JSON parsing errors are returned here.  Typically your program should never return these.
                System.out.println("onParsingErrors " + parsingErrorsList.toString());
            }
        };

        phHueSDK = PHHueSDK.getInstance();
        phHueSDK.setAppName("test");     // e.g. phHueSDK.setAppName("QuickStartApp");
        phHueSDK.setDeviceName(android.os.Build.MODEL);  // e.g. If you are programming for Android: phHueSDK.setDeviceName(android.os.Build.MODEL);
        phHueSDK = PHHueSDK.create();  // or call .getInstance() effectively the same.
        // Register the PHSDKListener to receive callbacks from the bridge.
        phHueSDK.getNotificationManager().registerSDKListener(listener);

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);

        String bridgeIp = sharedPref.getString(getString(R.string.bridgeIp), null);
        String username = sharedPref.getString(getString(R.string.username), null);


        if (bridgeIp == null || username == null) {
            PHBridgeSearchManager sm = (PHBridgeSearchManager) phHueSDK.getSDKService(PHHueSDK.SEARCH_BRIDGE);
            sm.search(true, true);
        } else {
            PHAccessPoint accessPoint = new PHAccessPoint();
            accessPoint.setIpAddress(bridgeIp);
            accessPoint.setUsername(username);
            phHueSDK.connect(accessPoint);
        }
    }

    public void changeAllColorLight(float x, float y) {

        System.out.println("x " + (int) x);
        System.out.println("y " + y);
        // x = (float)1.0;
        //y = (float)0.4862745;

        PHBridgeResourcesCache cache = phHueSDK.getSelectedBridge().getResourceCache();
// And now you can get any resource you want, for example:
        List<PHLight> myLights = cache.getAllLights();

        PHBridge bridge = PHHueSDK.getInstance().getSelectedBridge();
        PHLightState lightState = new PHLightState();

        lightState.setX(x);
        lightState.setY(y);

        for (int i = 0; i < myLights.size(); i++) {
            bridge.updateLightState(myLights.get(i), lightState);    // light being a PHLight object obtained from the cache
            // System.out.println("updateLightState" + myLights.get(i).toString());
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        // System.out.println("event" + event.getAction());
        //System.out.println("code" + event.getKeyCode());

        if (event.getAction() == 0 && event.getKeyCode() == KEYCODE_DPAD_CENTER) {
            if (tmp == 0) {
                Bitmap b = null;
                b = BitmapFactory.decodeResource(getResources(), R.drawable.green);
                float[] xyColor = findColor(b);
                changeAllColorLight(xyColor[0], xyColor[1]);
                tmp++;
            } else if (tmp == 1) {
                Bitmap b = null;
                b = BitmapFactory.decodeResource(getResources(), R.drawable.red);
                float[] xyColor = findColor(b);
                changeAllColorLight(xyColor[0], xyColor[1]);
                tmp++;
            } else if (tmp == 2) {
                Bitmap b = null;
                b = BitmapFactory.decodeResource(getResources(), R.drawable.blue);
                float[] xyColor = findColor(b);
                changeAllColorLight(xyColor[0], xyColor[1]);
                tmp = 0;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void takeScreenshot() {
        try {
            // create bitmap screen capture
            View v1 = getWindow().getDecorView().getRootView();
            v1.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(v1.getDrawingCache());
            v1.setDrawingCacheEnabled(false);

        } catch (Throwable e) {
            // Several error may come out with file handling or OOM
            e.printStackTrace();
        }
    }
}

