package com.meneguello.bruno.weatherwallpaper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

/**
 * Created by bruno on 8/13/15.
 */
public class WeatherInfo {

    private final long created = System.currentTimeMillis();

    private long time;

    private long sunrise;

    private long sunset;

    public static WeatherInfo fromJSON(JSONObject jsonObject) throws JSONException {
        final WeatherInfo weatherInfo = new WeatherInfo();
        weatherInfo.setTime(jsonObject.getLong("dt") * 1000);
        weatherInfo.setSunrise(jsonObject.getJSONObject("sys").getLong("sunrise") * 1000);
        weatherInfo.setSunset(jsonObject.getJSONObject("sys").getLong("sunset") * 1000);
        return weatherInfo;
    }

    public long getSunrise() {
        return sunrise;
    }

    private void setSunrise(long sunrise) {
        this.sunrise = sunrise;
    }

    public long getSunset() {
        return sunset;
    }

    private void setSunset(long sunset) {
        this.sunset = sunset;
    }

    public long getTime() {
        return time;
    }

    private void setTime(long time) {
        this.time = time;
    }

    public boolean isUpToDate() {
        final long current = System.currentTimeMillis();
        final long oneHourAgo = current - TimeUnit.HOURS.toMillis(1);
        return created < oneHourAgo && getTime() < oneHourAgo;
    }
}
