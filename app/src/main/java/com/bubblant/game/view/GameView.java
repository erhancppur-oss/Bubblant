package com.bubblant.game.view;

import android.content.Context;
import android.graphics.Canvas;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.bubblant.game.Prefs;
import com.bubblant.game.engine.GameWorld;

/** SurfaceView running the game loop + tilt input. */
public final class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable, SensorEventListener {
    public interface Host {
        void onPauseRequested();
    }

    private final SensorManager sensors;
    private final Sensor accel;
    private Thread thread;
    private volatile boolean running;
    private volatile boolean paused;
    private volatile GameWorld world;
    private volatile boolean reviveRequested;
    public final GameRenderer renderer = new GameRenderer();
    private Host host;

    private float rawX, rawY, offX, offY;
    private float calSumX, calSumY;
    private int calN;
    private boolean touchMode;
    private volatile float touchTX, touchTY;
    private volatile boolean touching;

    public GameView(Context c) {
        super(c);
        getHolder().addCallback(this);
        sensors = (SensorManager) c.getSystemService(Context.SENSOR_SERVICE);
        accel = sensors == null ? null : sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        setFocusable(true);
        setKeepScreenOn(true);
    }

    public void setHost(Host h) { host = h; }

    public void setWorld(GameWorld w) {
        world = w;
        calN = 0;
        calSumX = calSumY = 0;
    }

    public GameWorld getWorld() { return world; }

    public void setPaused(boolean p) { paused = p; }

    public void requestRevive() { reviveRequested = true; }

    public void resume() {
        touchMode = Prefs.control() == 1 || accel == null;
        if (accel != null) sensors.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME);
    }

    public void pause() {
        if (sensors != null) sensors.unregisterListener(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        running = true;
        thread = new Thread(this, "bubblant-game");
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        running = false;
        if (thread != null) {
            try { thread.join(500); } catch (InterruptedException ignored) {}
        }
    }

    @Override
    public void run() {
        long last = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            float dt = (now - last) / 1e9f;
            last = now;
            GameWorld w = world;
            if (w != null) {
                if (reviveRequested) {
                    reviveRequested = false;
                    w.revive();
                    calN = 0;
                    calSumX = calSumY = 0;
                }
                if (!paused) {
                    float[] tilt = currentTilt(w);
                    w.update(dt, tilt[0], tilt[1]);
                }
                Canvas c = null;
                try {
                    c = Build.VERSION.SDK_INT >= 26 ? getHolder().lockHardwareCanvas() : getHolder().lockCanvas();
                    if (c != null) renderer.draw(c, w, paused ? 0 : dt);
                } catch (Exception ignored) {
                } finally {
                    if (c != null) {
                        try { getHolder().unlockCanvasAndPost(c); } catch (Exception ignored) {}
                    }
                }
            }
            long spent = (System.nanoTime() - now) / 1_000_000;
            long target = paused ? 50 : 16;
            if (spent < target) {
                try { Thread.sleep(target - spent); } catch (InterruptedException ignored) {}
            }
        }
    }

    private final float[] tiltOut = new float[2];

    private float[] currentTilt(GameWorld w) {
        float sens = Prefs.sensitivity() / 100f;
        if (touchMode) {
            if (!touching) { tiltOut[0] = tiltOut[1] = 0; return tiltOut; }
            float range = getWidth() * 0.28f;
            float dx = (touchTX - renderer.bubbleSX) / range, dy = (touchTY - renderer.bubbleSY) / range;
            tiltOut[0] = clamp(dx * sens);
            tiltOut[1] = clamp(dy * sens);
            return tiltOut;
        }
        // auto-calibrate to the way the phone is held while the level is starting
        if (w.state == GameWorld.State.READY) {
            calSumX += rawX;
            calSumY += rawY;
            calN++;
            offX = calSumX / calN;
            offY = calSumY / calN;
            tiltOut[0] = tiltOut[1] = 0;
            return tiltOut;
        }
        float gain = 2.4f * sens;
        tiltOut[0] = clamp((rawX - offX) * gain);
        tiltOut[1] = clamp((rawY - offY) * gain);
        return tiltOut;
    }

    private static float clamp(float v) { return Math.max(-1f, Math.min(1f, v)); }

    @Override
    public void onSensorChanged(SensorEvent e) {
        float sx, sy;
        int rot = Surface.ROTATION_0;
        try {
            WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            if (wm != null) rot = wm.getDefaultDisplay().getRotation();
        } catch (Exception ignored) {}
        switch (rot) {
            case Surface.ROTATION_90: sx = -e.values[1]; sy = e.values[0]; break;
            case Surface.ROTATION_180: sx = -e.values[0]; sy = -e.values[1]; break;
            case Surface.ROTATION_270: sx = e.values[1]; sy = -e.values[0]; break;
            default: sx = e.values[0]; sy = e.values[1];
        }
        float nx = -sx / 9.81f, ny = sy / 9.81f;
        rawX += (nx - rawX) * 0.35f;
        rawY += (ny - rawY) * 0.35f;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        float x = ev.getX(), y = ev.getY();
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN && renderer.pauseRect.contains(x, y)) {
            if (host != null) host.onPauseRequested();
            return true;
        }
        if (touchMode) {
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    touching = true;
                    touchTX = x;
                    touchTY = y;
                    renderer.touchX = x;
                    renderer.touchY = y;
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    touching = false;
                    renderer.touchX = -1;
                    break;
            }
            return true;
        }
        return super.onTouchEvent(ev);
    }
}
