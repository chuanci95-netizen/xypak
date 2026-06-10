package com.xy.pak;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class FloatingService extends Service {

    public static boolean isRunning = false;

    private WindowManager wm;
    private View floatView;
    private WindowManager.LayoutParams lp;
    private boolean expanded = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        showFloat();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (floatView != null && wm != null) {
            try { wm.removeView(floatView); } catch (Exception e) {}
        }
    }

    private void showFloat() {
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        floatView = LayoutInflater.from(this).inflate(R.layout.float_view, null);

        int type;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = 30;
        lp.y = 200;

        final View pill = floatView.findViewById(R.id.float_pill);
        final View ball = floatView.findViewById(R.id.float_ball);
        final TextView status = floatView.findViewById(R.id.float_status);

        // 初始显示小球
        ball.setVisibility(View.VISIBLE);
        pill.setVisibility(View.GONE);

        floatView.setOnTouchListener(new View.OnTouchListener() {
            int startX, startY;
            float touchX, touchY;
            boolean moved = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = lp.x;
                        startY = lp.y;
                        touchX = event.getRawX();
                        touchY = event.getRawY();
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) (event.getRawX() - touchX);
                        int dy = (int) (event.getRawY() - touchY);
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) moved = true;
                        lp.x = startX + dx;
                        lp.y = startY + dy;
                        wm.updateViewLayout(floatView, lp);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) {
                            expanded = !expanded;
                            if (expanded) {
                                ball.setVisibility(View.GONE);
                                pill.setVisibility(View.VISIBLE);
                                status.setText("运行中");
                            } else {
                                ball.setVisibility(View.VISIBLE);
                                pill.setVisibility(View.GONE);
                            }
                        }
                        return true;
                }
                return false;
            }
        });

        try {
            wm.addView(floatView, lp);
        } catch (Exception e) {
            stopSelf();
        }
    }
}
