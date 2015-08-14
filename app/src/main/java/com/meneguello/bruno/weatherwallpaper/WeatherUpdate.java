package com.meneguello.bruno.weatherwallpaper;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

/**
 * Created by bruno on 8/13/15.
 */
public class WeatherUpdate implements Runnable {

    private static final String APPID = "11ac9016728bbba7654c3bc8f97ef2e9";

    private final Location location;

    private final WeatherListener listener;

    public WeatherUpdate(Location location, WeatherListener listener) {
        this.location = location;
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            final URL url = new URL(String.format("http://api.openweathermap.org/data/2.5/weather?lat=%.2f&lon=%.2f&APPID=%s", location.getLatitude(), location.getLongitude(), APPID));
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "");
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();

            final InputStream is = conn.getInputStream();

            final BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            final StringBuffer sb = new StringBuffer();
            String line = "";
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            conn.disconnect();

            final JSONObject jsonObject = new JSONObject(sb.toString());
            final WeatherInfo weatherInfo = WeatherInfo.fromJSON(jsonObject);

            listener.onWeatherChanged(weatherInfo);
        } catch (MalformedURLException e) {
            Log.e("WEATHERWALLPAPER", "Network failure", e);
        } catch (ProtocolException e) {
            Log.e("WEATHERWALLPAPER", "Network failure", e);
        } catch (JSONException e) {
            Log.e("WEATHERWALLPAPER", "Data failure", e);
        } catch (IOException e) {
            Log.e("WEATHERWALLPAPER", "Network failure", e);
        }
    }
}
