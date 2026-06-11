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
import android.widget.ImageView;
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
    private final String[] injectNames = {"红色内透", "至尊美化", "裸奔范围0.35", "功能文件", "测试", "测试"};
    private final int[] tileIcons = {
            R.drawable.ic_rocket, R.drawable.ic_bolt, R.drawable.ic_refresh,
            R.drawable.ic_wrench, R.drawable.ic_bell, R.drawable.ic_lock_status
    };
    private final int[] tileBgs = {
            R.drawable.bg_icon_tile_green, R.drawable.bg_icon_tile_orange, R.drawable.bg_icon_tile_blue,
            R.drawable.bg_icon_tile_purple, R.drawable.bg_icon_tile_red, R.drawable.bg_icon_tile_gray
    };

            // 初始化 6 个注入卡片
            int[] swIds = {R.id.inject_sw1, R.id.inject_sw2, R.id.inject_sw3, R.id.inject_sw4, R.id.inject_sw5, R.id.inject_sw6};
            int[] trackIds = {R.id.inject_track1, R.id.inject_track2, R.id.inject_track3, R.id.inject_track4, R.id.inject_track5, R.id.inject_track6};
            int[] thumbIds = {R.id.inject_thumb1, R.id.inject_thumb2, R.id.inject_thumb3, R.id.inject_thumb4, R.id.inject_thumb5, R.id.inject_thumb6};
            for (int i = 0; i < 6; i++) {
                final View track = floatView.findViewById(trackIds[i]);
                final View thumb = floatView.findViewById(thumbIds[i]);
                final int idx = i;
                floatView.findViewById(swIds[i]).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        injectOn[idx] = !injectOn[idx];
                        updateSwitch(track, thumb, injectOn[idx], 44);
                        if (idx == 0) {
                            if (injectOn[idx]) injectPakFile();
                            else removePakFile();
                        } else {
                            showMsg(injectNames[idx] + (injectOn[idx] ? " 已开启" : " 已关闭"));
                        }
                    }
                });
            }
    private boolean[] injectOn = new boolean[6];

    // 路径常量
    private static final String SRC_DIR = "/storage/emulated/0/和平PAK文件/内透[红]";
    private static final String DST_DIR = "/storage/emulated/0/Android/data/com.tencent.tmgp.pubgmhd/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Paks";
    private static final String PAK_NAME = "game_patch_1.36.11.15380.pak";

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

            // 初始化 6 个注入卡片
            int[] swIds = {R.id.inject_sw1, R.id.inject_sw2, R.id.inject_sw3, R.id.inject_sw4, R.id.inject_sw5, R.id.inject_sw6};
            int[] trackIds = {R.id.inject_track1, R.id.inject_track2, R.id.inject_track3, R.id.inject_track4, R.id.inject_track5, R.id.inject_track6};
            int[] thumbIds = {R.id.inject_thumb1, R.id.inject_thumb2, R.id.inject_thumb3, R.id.inject_thumb4, R.id.inject_thumb5, R.id.inject_thumb6};
            for (int i = 0; i < 6; i++) {
                final View track = floatView.findViewById(trackIds[i]);
                final View thumb = floatView.findViewById(thumbIds[i]);
                final int idx = i;
                floatView.findViewById(swIds[i]).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        injectOn[idx] = !injectOn[idx];
                        updateSwitch(track, thumb, injectOn[idx], 44);
                        if (idx == 0) {
                            if (injectOn[idx]) injectPakFile();
                            else removePakFile();
                        } else {
                            showMsg(injectNames[idx] + (injectOn[idx] ? " 已开启" : " 已关闭"));
                        }
                    }
                });
            }
            @Override
            public void onClick(View v) {
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


    /** 从 assets 提取 pak 到 app 内部目录 */
    private String extractAssetToInternal(String assetName) {
        try {
            java.io.File outFile = new java.io.File(getFilesDir(), assetName);
            java.io.InputStream in = getAssets().open(assetName);
            java.io.FileOutputStream out = new java.io.FileOutputStream(outFile);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in.close();
            out.flush();
            out.close();
            outFile.setReadable(true, false);
            return outFile.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    /** 用 Root 复制 pak 到游戏目录 */
    private void injectPakFile() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String src = extractAssetToInternal("pak_on.pak");
                    if (src == null || !new File(src).exists()) {
                        showMsg("提取文件失败");
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
                        showMsg("内透已开启 ✓");
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
                    String src = extractAssetToInternal("pak_off.pak");
                    if (src == null || !new java.io.File(src).exists()) {
                        showMsg("提取原版文件失败");
                        return;
                    }
                    Process p = Runtime.getRuntime().exec("su");
                    DataOutputStream os = new DataOutputStream(p.getOutputStream());
                    os.writeBytes("mkdir -p '" + DST_DIR + "'\n");
                    os.writeBytes("cp '" + src + "' '" + DST_DIR + "/" + PAK_NAME + "'\n");
                    os.writeBytes("chmod 644 '" + DST_DIR + "/" + PAK_NAME + "'\n");
                    os.writeBytes("exit\n");
                    os.flush();
                    int code = p.waitFor();
                    if (code == 0) {
                        showMsg("内透已关闭 ✓");
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
