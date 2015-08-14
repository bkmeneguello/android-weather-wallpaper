package com.meneguello.bruno.weatherwallpaper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class WeatherWallpaperService extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new WeatherEngine();
    }

    private class WeatherEngine extends Engine implements LocationListener, WeatherListener {

        private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

        private LocationManager locationManager;

        private SurfaceHolder holder;

        private WeatherInfo weatherInfo;

        private ScheduledFuture<?> scheduled;

        private final Runnable periodicRender = new Runnable() {
            @Override
            public void run() {
                Log.i("WEATHERWALLPAPAER", "render why timed out");
                render();
            }
        };

        private Rect land;

        private Rect sky;

        private Drawable sun;

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            this.holder = holder;
            this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            this.locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, TimeUnit.MINUTES.toMillis(15), 10000, this);
            this.sun = getDrawable(R.drawable.sun);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            if (visible) {
                Log.i("WEATHERWALLPAPAER", "scheduled 1s");
                this.scheduled = executor.scheduleWithFixedDelay(this.periodicRender, 0, 1, TimeUnit.SECONDS);
            } else if (this.scheduled != null) {
                Log.i("WEATHERWALLPAPAER", "scheduled cancelled");
                this.scheduled.cancel(false);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            final Rect frame = holder.getSurfaceFrame();
            this.land = new Rect(frame.left, frame.height()*3/4, frame.right, frame.bottom);
            this.sky = new Rect(frame.left, frame.top, frame.right, frame.bottom - land.height());
            Log.i("WEATHERWALLPAPAER", "render why surface changed");
            render();
        }

        private void update() {
            final Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                executor.execute(new WeatherUpdate(location, this));
            }
        }

        private void render() {
            final long current = System.currentTimeMillis();
            if (weatherInfo == null || weatherInfo.isUpToDate(current)) {
                Log.i("WEATHERWALLPAPAER", "update why not weather info");
                update();
            } else {
                final Surface surface = holder.getSurface();
                final Rect frame = holder.getSurfaceFrame();
                final Canvas canvas = surface.lockCanvas(frame);

                final long sunrise = weatherInfo.getSunrise();
                final long sunset = weatherInfo.getSunset();
                float ratio = ((float) (current - sunrise)) / (sunset - sunrise);

                final int left = (int) (frame.width() * ratio);
                final int top = (int) (frame.height() * .75 * (1 - Math.sin(Math.PI * ratio)));

                final Paint skyPaint = new Paint();
                skyPaint.setShader(new RadialGradient(left, top + 32, frame.width() * 2, new int[]{Color.RED, Color.BLUE}, null, Shader.TileMode.CLAMP));
                canvas.drawRect(sky, skyPaint);

                sun.setBounds(left - sun.getIntrinsicWidth()/2, top, left + sun.getIntrinsicWidth()/2, top + sun.getIntrinsicHeight());
                sun.draw(canvas);

                final Paint groundPaint = new Paint();
                groundPaint.setColor(Color.BLACK);
                canvas.drawRect(land, groundPaint);

                surface.unlockCanvasAndPost(canvas);
            }
        }

        @Override
        public void onWeatherChanged(WeatherInfo weatherInfo) {
            this.weatherInfo = weatherInfo;
            Log.i("WEATHERWALLPAPAER", String.format("weather (sr: %tc, ss: %tc)", weatherInfo.getSunrise(), weatherInfo.getSunset()));
            render();
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.i("WEATHERWALLPAPAER", String.format("update (%.5f, %.5f, %.1f)", location.getLatitude(), location.getLongitude(), location.getAltitude()));
            render();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }


}
