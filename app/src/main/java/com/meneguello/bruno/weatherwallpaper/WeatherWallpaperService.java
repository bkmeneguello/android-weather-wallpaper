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

        private Sky sky = new Sky();

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            this.holder = holder;
            this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            this.locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, TimeUnit.MINUTES.toMillis(15), 10000, this);
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
            sky.setFrame(new Rect(frame.left, frame.top, frame.right, frame.bottom - land.height()));
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
            if (weatherInfo == null || weatherInfo.isUpToDate()) {
                Log.i("WEATHERWALLPAPAER", "update why not weather info");
                update();
            } else {
                final Surface surface = holder.getSurface();
                final Rect frame = holder.getSurfaceFrame();
                final Canvas canvas = surface.lockCanvas(frame);

                sky.draw(canvas);

                final Paint groundPaint = new Paint();
                groundPaint.setColor(Color.BLACK);
                canvas.drawRect(land, groundPaint);

                surface.unlockCanvasAndPost(canvas);
            }
        }

        @Override
        public void onWeatherChanged(WeatherInfo weatherInfo) {
            this.weatherInfo = weatherInfo;
            sky.setSunrise(weatherInfo.getSunrise());
            sky.setSunset(weatherInfo.getSunset());
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

        private class Sun {

            private final Drawable drawable;

            public Sun(Drawable drawable) {
                this.drawable = drawable;
            }

            public void draw(Canvas canvas, double x, double y) {
                drawable.setBounds(((int)x) - drawable.getIntrinsicWidth() / 2, ((int)y), ((int)x) + drawable.getIntrinsicWidth() / 2, ((int)y) + drawable.getIntrinsicHeight());
                drawable.draw(canvas);
            }
        }

        private class Sky {

            private final Paint paint = new Paint();

            private final Sun sun;

            private Rect frame;

            private long sunrise;

            private long sunset;

            private Sky() {
                this.sun = new Sun(getDrawable(R.drawable.sun));
            }

            public Rect getFrame() {
                return frame;
            }

            public void setFrame(Rect frame) {
                this.frame = frame;
            }

            public void setSunrise(long sunrise) {
                this.sunrise = sunrise;
            }

            public void setSunset(long sunset) {
                this.sunset = sunset;
            }

            public void draw(Canvas canvas) {
                final long current = System.currentTimeMillis();
                final double ratio = ((double) (current - sunrise)) / (sunset - sunrise);
                final double sunX = frame.width() * ratio;
                final double sunY = frame.height() * (1 - Math.sin(Math.PI * ratio));
                final int[] colors = {
                        Color.rgb(0xC5, 0xE7, 0xFF),
                        Color.rgb(0x42, 0x9F, 0xDA)
                };
                final int radius = frame.width() * 2;
                paint.setShader(new RadialGradient(((int)sunX), ((int)sunY) + 32, radius, colors, null, Shader.TileMode.CLAMP));
                canvas.drawRect(frame, paint);
                sun.draw(canvas, sunX, sunY);
            }
        }
    }


}
