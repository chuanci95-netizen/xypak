package com.xy.pak;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.DataOutputStream;
import java.io.File;

public class FloatingService extends Service {

    public static boolean isRunning = false;

    private WindowManager wm;
    private View floatView;
    private WindowManager.LayoutParams lp;
    private boolean expanded = false;

    private boolean[] tileOn = new boolean[6];
    private final String[] tileNames = {"悬浮窗", "游戏模式", "清理后台", "120Hz", "免打扰", "状态栏"};
    private final int[] tileIcons = {
            R.drawable.ic_rocket, R.drawable.ic_bolt, R.drawable.ic_refresh,
            R.drawable.ic_wrench, R.drawable.ic_bell, R.drawable.ic_lock_status
    };
    private final int[] tileBgs = {
            R.drawable.bg_icon_tile_green, R.drawable.bg_icon_tile_orange, R.drawable.bg_icon_tile_blue,
            R.drawable.bg_icon_tile_purple, R.drawable.bg_icon_tile_red, R.drawable.bg_icon_tile_gray
    };

    // 内透[红] 开关状态
    private boolean injectRedOn = false;

    // 路径常量
    private static final String SRC_DIR = "/storage/emulated/0/和平PAK文件/内透[红]";
    private static final String DST_DIR = "/storage/emulated/0/Android/data/com.tencent.tmgp.pubgmhd/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Paks";
    private static final String PAK_NAME = "game_patch_1_36_11_15380.pak";

    @Override
    public IBinder onBind(Intent intent) { return null; }

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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                centerOnScreen();
            }
        }, 100);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void centerOnScreen() {
        if (lp == null || wm == null || floatView == null) return;
        try {
            lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            lp.x = 0;
            lp.y = dp(32);
            wm.updateViewLayout(floatView, lp);
        } catch (Exception e) {}
    }

    private void showFloat() {
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        floatView = LayoutInflater.from(this).inflate(R.layout.float_view, null);

        int type = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.x = 0;
        lp.y = dp(32);

        final View pill = floatView.findViewById(R.id.float_pill);
        final View panel = floatView.findViewById(R.id.float_panel);
        final ImageView closeBtn = floatView.findViewById(R.id.float_close);
        final TextView batteryText = floatView.findViewById(R.id.float_battery);

        final TextView tabList = floatView.findViewById(R.id.tab_list);
        final TextView tabSafe = floatView.findViewById(R.id.tab_safe);
        final TextView tabSettings = floatView.findViewById(R.id.tab_settings);
        final View pageList = floatView.findViewById(R.id.page_list);
        final View pageSafe = floatView.findViewById(R.id.page_safe);
        final View pageSettings = floatView.findViewById(R.id.page_settings);

        // 内透[红] 开关
        final View swTestTrack = floatView.findViewById(R.id.sw_test_track);
        final View swTestThumb = floatView.findViewById(R.id.sw_test_thumb);
        floatView.findViewById(R.id.sw_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                injectRedOn = !injectRedOn;
                updateSwitch(swTestTrack, swTestThumb, injectRedOn, 44);
                if (injectRedOn) {
                    injectPakFile();
                } else {
                    removePakFile();
                }
            }
        });

        // 6 个磁贴
        int[] tileIds = {R.id.tile1, R.id.tile2, R.id.tile3, R.id.tile4, R.id.tile5, R.id.tile6};
        for (int i = 0; i < 6; i++) {
            final int idx = i;
            View tile = floatView.findViewById(tileIds[i]);
            TextView text = tile.findViewById(R.id.tile_text);
            ImageView icon = tile.findViewById(R.id.tile_icon);
            View iconBg = (View) icon.getParent();
            final View track = tile.findViewById(R.id.tile_track);
            final View thumb = tile.findViewById(R.id.tile_thumb);

            text.setText(tileNames[i]);
            icon.setImageResource(tileIcons[i]);
            iconBg.setBackgroundResource(tileBgs[i]);

            tile.findViewById(R.id.tile_switch).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tileOn[idx] = !tileOn[idx];
                    updateSwitch(track, thumb, tileOn[idx], 40);
                }
            });
        }

        pill.setVisibility(View.VISIBLE);
        panel.setVisibility(View.GONE);

        pill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                expanded = true;
                pill.setVisibility(View.GONE);
                panel.setVisibility(View.VISIBLE);
                try {
                    BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
                    int level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                    batteryText.setText(level + "%");
                } catch (Exception e) {}
                centerOnScreen();
            }
        });

        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                expanded = false;
                panel.setVisibility(View.GONE);
                pill.setVisibility(View.VISIBLE);
                centerOnScreen();
            }
        });

        View.OnClickListener tabClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                pageList.setVisibility(id == R.id.tab_list ? View.VISIBLE : View.GONE);
                pageSafe.setVisibility(id == R.id.tab_safe ? View.VISIBLE : View.GONE);
                pageSettings.setVisibility(id == R.id.tab_settings ? View.VISIBLE : View.GONE);
            }
        };
        tabList.setOnClickListener(tabClick);
        tabSafe.setOnClickListener(tabClick);
        tabSettings.setOnClickListener(tabClick);

        try {
            wm.addView(floatView, lp);
        } catch (Exception e) {
            Toast.makeText(this, "悬浮窗启动失败", Toast.LENGTH_SHORT).show();
            stopSelf();
        }
    }

    /** 用 Root 复制 pak 到游戏目录 */
    private void injectPakFile() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String src = SRC_DIR + "/" + PAK_NAME;
                    File srcFile = new File(src);
                    if (!srcFile.exists()) {
                        showMsg("源文件不存在: " + PAK_NAME);
                        return;
                    }

                    Process p = Runtime.getRuntime().exec("su");
                    DataOutputStream os = new DataOutputStream(p.getOutputStream());
                    // 确保目标目录存在
                    os.writeBytes("mkdir -p '" + DST_DIR + "'\n");
                    // 复制文件
                    os.writeBytes("cp '" + src + "' '" + DST_DIR + "/" + PAK_NAME + "'\n");
                    // 设置权限
                    os.writeBytes("chmod 644 '" + DST_DIR + "/" + PAK_NAME + "'\n");
                    os.writeBytes("exit\n");
                    os.flush();
                    int code = p.waitFor();

                    if (code == 0) {
                        showMsg("内透[红] 已注入 ✓");
                    } else {
                        showMsg("注入失败，请确认 Root 权限");
                    }
                } catch (Exception e) {
                    showMsg("注入失败: " + e.getMessage());
                }
            }
        }).start();
    }

    /** 用 Root 删除游戏目录里的 pak */
    private void removePakFile() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Process p = Runtime.getRuntime().exec("su");
                    DataOutputStream os = new DataOutputStream(p.getOutputStream());
                    os.writeBytes("rm -f '" + DST_DIR + "/" + PAK_NAME + "'\n");
                    os.writeBytes("exit\n");
                    os.flush();
                    int code = p.waitFor();

                    if (code == 0) {
                        showMsg("内透[红] 已关闭 ✓");
                    } else {
                        showMsg("关闭失败，请确认 Root 权限");
                    }
                } catch (Exception e) {
                    showMsg("关闭失败: " + e.getMessage());
                }
            }
        }).start();
    }

    private void showMsg(final String msg) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(FloatingService.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSwitch(View track, View thumb, boolean on, int trackWidthDp) {
        track.setBackgroundResource(on ? R.drawable.switch_track_on : R.drawable.switch_track_off);
        float dx = on ? dp(trackWidthDp - 22 - 2) : 0;
        thumb.animate().translationX(dx).setDuration(150).start();
    }
}
