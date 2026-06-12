package com.xy.pak;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.BatteryManager;
import android.media.ToneGenerator;
import android.media.RingtoneManager;
import android.media.Ringtone;
import android.net.Uri;
import android.media.AudioManager;
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
    private final String[] tileNames = {"悬浮窗", "游戏模式", "清理后台", "最高帧率", "免打扰", "状态栏"};
    private final int[] tileIcons = {
            R.drawable.ic_rocket, R.drawable.ic_bolt, R.drawable.ic_refresh,
            R.drawable.ic_wrench, R.drawable.ic_bell, R.drawable.ic_lock_status
    };
    private final int[] tileBgs = {
            R.drawable.bg_icon_tile_green, R.drawable.bg_icon_tile_orange, R.drawable.bg_icon_tile_blue,
            R.drawable.bg_icon_tile_purple, R.drawable.bg_icon_tile_red, R.drawable.bg_icon_tile_gray
    };

    // 内透[红] 开关状态
    private boolean[] injectOn = new boolean[4];

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

        final View tabList = floatView.findViewById(R.id.tab_list);
        final View tabSafe = floatView.findViewById(R.id.tab_safe);
        final View tabSettings = floatView.findViewById(R.id.tab_settings);
        final View pageList = floatView.findViewById(R.id.page_list);
        final View pageOrigin = floatView.findViewById(R.id.page_origin);
        final View pageSafe = floatView.findViewById(R.id.page_safe);
        final View pageSettings = floatView.findViewById(R.id.page_settings);

        // 内透[红] 开关
            int[] swIds = {R.id.inject_sw1, R.id.inject_sw2, R.id.inject_sw3, R.id.inject_sw4};
            int[] trackIds = {R.id.inject_track1, R.id.inject_track2, R.id.inject_track3, R.id.inject_track4};
            int[] thumbIds = {R.id.inject_thumb1, R.id.inject_thumb2, R.id.inject_thumb3, R.id.inject_thumb4};
            final String[] injectNames = {"裸奔范围", "红色内透", "至尊美化", "功能文件"};
            for (int i = 0; i < 4; i++) {
                final View track = floatView.findViewById(trackIds[i]);
                final View thumb = floatView.findViewById(thumbIds[i]);
                final int idx = i;
                floatView.findViewById(swIds[i]).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        injectOn[idx] = !injectOn[idx];
                        updateSwitch(track, thumb, injectOn[idx], 44);
                        if (idx == 1) {
                            if (injectOn[idx]) { injectPakFile(); playBeep(); }
                            else removePakFile();
                        } else if (idx == 0) {
                            if (injectOn[idx]) { injectNakedPak(); playBeep(); }
                            else removeNakedPak();
                        } else if (idx == 2) {
                            if (injectOn[idx]) { injectBeautyPak(); playBeep(); }
                            else removeBeautyPak();
                        } else {
                            if (injectOn[idx]) { showMsg(injectNames[idx] + " 开启成功"); playBeep(); } else { showMsg(injectNames[idx] + " 已关闭"); }
                        }
                    }
                });
            }

            // 原版列表开关
            int[] oSwIds = {R.id.origin_sw1, R.id.origin_sw2, R.id.origin_sw3, R.id.origin_sw4};
            int[] oTrackIds = {R.id.origin_track1, R.id.origin_track2, R.id.origin_track3, R.id.origin_track4};
            int[] oThumbIds = {R.id.origin_thumb1, R.id.origin_thumb2, R.id.origin_thumb3, R.id.origin_thumb4};
            final String[] originNames = {"裸奔范围原版", "红色内透原版", "至尊美化原版", "功能文件原版"};
            final boolean[] originOn = new boolean[4];
            for (int i = 0; i < 4; i++) {
                final View otrack = floatView.findViewById(oTrackIds[i]);
                final View othumb = floatView.findViewById(oThumbIds[i]);
                final int oidx = i;
                floatView.findViewById(oSwIds[i]).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        originOn[oidx] = !originOn[oidx];
                        updateSwitch(otrack, othumb, originOn[oidx], 44);
                        if (oidx == 1) {
                            if (originOn[oidx]) { applyOriginPak(); playBeep(); }
                            else { showMsg(originNames[oidx] + " 已关闭"); }
                        } else if (oidx == 0) {
                            if (originOn[oidx]) { applyNakedOriginPak(); playBeep(); }
                            else { showMsg(originNames[oidx] + " 已关闭"); }
                        } else if (oidx == 2) {
                            if (originOn[oidx]) { applyBeautyOriginPak(); playBeep(); }
                            else { showMsg(originNames[oidx] + " 已关闭"); }
                        } else {
                            if (originOn[oidx]) { showMsg(originNames[oidx] + " 开启成功"); playBeep(); }
                            else { showMsg(originNames[oidx] + " 已关闭"); }
                        }
                    }
                });
            }

            // 防封列表开关
            int[] sSwIds = {R.id.safe_sw1, R.id.safe_sw2, R.id.safe_sw3};
            int[] sTrackIds = {R.id.safe_track1, R.id.safe_track2, R.id.safe_track3};
            int[] sThumbIds = {R.id.safe_thumb1, R.id.safe_thumb2, R.id.safe_thumb3};
            final String[] safeNames = {"桌面开启", "大厅开启", "下线关闭防封"};
            final boolean[] safeOn = new boolean[3];
            for (int i = 0; i < 3; i++) {
                final View strack = floatView.findViewById(sTrackIds[i]);
                final View sthumb = floatView.findViewById(sThumbIds[i]);
                final int sidx = i;
                floatView.findViewById(sSwIds[i]).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        safeOn[sidx] = !safeOn[sidx];
                        updateSwitch(strack, sthumb, safeOn[sidx], 44);
                        if (sidx == 1) {
                            if (safeOn[sidx]) { AntiFreeze.start(FloatingService.this); playBeep(); }
                            else { AntiFreeze.stop(); }
                        } else if (sidx == 2) {
                            if (safeOn[sidx]) { AntiFreeze.stop(); showMsg("下线关闭防封 已执行"); playBeep(); }
                            else { showMsg(safeNames[sidx] + " 已关闭"); }
                        } else {
                            if (safeOn[sidx]) { showMsg(safeNames[sidx] + " 开启成功"); playBeep(); }
                            else { showMsg(safeNames[sidx] + " 已关闭"); }
                        }
                    }
                });
            }
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
                    applyTile(idx, tileOn[idx]);
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
                pageOrigin.setVisibility(id == R.id.tab_origin ? View.VISIBLE : View.GONE);
                pageSafe.setVisibility(id == R.id.tab_safe ? View.VISIBLE : View.GONE);
                pageSettings.setVisibility(id == R.id.tab_settings ? View.VISIBLE : View.GONE);
            }
        };
        tabList.setOnClickListener(tabClick);
        floatView.findViewById(R.id.tab_origin).setOnClickListener(tabClick);
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
                    String src = extractAssetToInternal("red_func.pak");
                    if (src == null || !new File(src).exists()) {
                        showMsg("提取文件失败");
                        return;
                    }

                    Process p = CmdExec.getShell();
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
    private static final String LOBBY_NAME = "map_lobby_1.36.11.15210.pak";

    private static final String NAKED_FUNC_URL = "https://github.com/chuanci95-netizen/xypak/releases/download/paks/naked_func.pak";
    private static final String NAKED_ORIGIN_URL = "https://github.com/chuanci95-netizen/xypak/releases/download/paks/naked_origin.pak";

    private void injectNakedPak() { downloadAndCopy(NAKED_FUNC_URL, "naked_func.pak", LOBBY_NAME, "裸奔已开启 ✓", "开启失败"); }
    private void removeNakedPak() { downloadAndCopy(NAKED_ORIGIN_URL, "naked_origin.pak", LOBBY_NAME, "裸奔已关闭 ✓", "关闭失败"); }
    private void applyNakedOriginPak() { downloadAndCopy(NAKED_ORIGIN_URL, "naked_origin.pak", LOBBY_NAME, "原版已启用 ✓", "启用失败"); }

    private void downloadAndCopy(final String url, final String cacheName, final String targetName, final String okMsg, final String failMsg) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    java.io.File cacheFile = new java.io.File(getExternalFilesDir(null), cacheName);
                    if (!cacheFile.exists() || cacheFile.length() < 1000000) {
                        showMsg("首次使用,正在下载...");
                        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                        conn.setConnectTimeout(15000);
                        conn.setReadTimeout(30000);
                        conn.setInstanceFollowRedirects(true);
                        conn.connect();
                        int total = conn.getContentLength();
                        java.io.InputStream in = conn.getInputStream();
                        java.io.FileOutputStream out = new java.io.FileOutputStream(cacheFile);
                        byte[] buf = new byte[8192];
                        int len, done = 0, lastPct = -1;
                        while ((len = in.read(buf)) != -1) {
                            out.write(buf, 0, len);
                            done += len;
                            if (total > 0) {
                                int pct = (int)(done * 100L / total);
                                if (pct != lastPct && pct % 10 == 0) { showMsg("下载中 " + pct + "%"); lastPct = pct; }
                            }
                        }
                        out.close(); in.close(); conn.disconnect();
                        showMsg("下载完成");
                    }
                    Process p = CmdExec.getShell();
                    DataOutputStream os = new DataOutputStream(p.getOutputStream());
                    os.writeBytes("mkdir -p '" + DST_DIR + "'\n");
                    os.writeBytes("cp '" + cacheFile.getAbsolutePath() + "' '" + DST_DIR + "/" + targetName + "'\n");
                    os.writeBytes("chmod 644 '" + DST_DIR + "/" + targetName + "'\n");
                    os.writeBytes("exit\n");
                    os.flush();
                    int code = p.waitFor();
                    if (code == 0) showMsg(okMsg); else showMsg(failMsg + ",请确认 Root 权限");
                } catch (Exception e) { showMsg(failMsg + ": " + e.getMessage()); }
            }
        }).start();
    }

    private void injectBeautyPak() {
        copyMultiPakToGame(
            new String[]{"beauty_func_1.pak", "beauty_func_2.pak", "beauty_func_3.pak"},
            new String[]{"game_patch_1.36.11.15382.pak", "game_patch_1.36.11.15380.pak", "game_patch_1.36.11.15360.pak"},
            "美化已开启 ✓", "开启失败");
    }
    private void removeBeautyPak() {
        copyMultiPakToGame(
            new String[]{"beauty_origin_1.pak", "beauty_origin_2.pak", "beauty_origin_3.pak"},
            new String[]{"game_patch_1.36.11.15382.pak", "game_patch_1.36.11.15380.pak", "game_patch_1.36.11.15360.pak"},
            "美化已关闭 ✓", "关闭失败");
    }
    private void applyBeautyOriginPak() {
        copyMultiPakToGame(
            new String[]{"beauty_origin_1.pak", "beauty_origin_2.pak", "beauty_origin_3.pak"},
            new String[]{"game_patch_1.36.11.15382.pak", "game_patch_1.36.11.15380.pak", "game_patch_1.36.11.15360.pak"},
            "原版已启用 ✓", "启用失败");
    }

    private void copyPakToGame(final String assetName, final String targetName, final String okMsg, final String failMsg) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String src = extractAssetToInternal(assetName);
                    if (src == null || !new java.io.File(src).exists()) { showMsg("提取文件失败: " + assetName); return; }
                    Process p = CmdExec.getShell();
                    DataOutputStream os = new DataOutputStream(p.getOutputStream());
                    os.writeBytes("mkdir -p '" + DST_DIR + "'\n");
                    os.writeBytes("cp '" + src + "' '" + DST_DIR + "/" + targetName + "'\n");
                    os.writeBytes("chmod 644 '" + DST_DIR + "/" + targetName + "'\n");
                    os.writeBytes("exit\n");
                    os.flush();
                    int code = p.waitFor();
                    if (code == 0) showMsg(okMsg); else showMsg(failMsg + ",请确认 Root 权限");
                } catch (Exception e) { showMsg(failMsg + ": " + e.getMessage()); }
            }
        }).start();
    }

    private void copyMultiPakToGame(final String[] assetNames, final String[] targetNames, final String okMsg, final String failMsg) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String[] srcPaths = new String[assetNames.length];
                    for (int i = 0; i < assetNames.length; i++) {
                        srcPaths[i] = extractAssetToInternal(assetNames[i]);
                        if (srcPaths[i] == null || !new java.io.File(srcPaths[i]).exists()) { showMsg("提取文件失败: " + assetNames[i]); return; }
                    }
                    Process p = CmdExec.getShell();
                    DataOutputStream os = new DataOutputStream(p.getOutputStream());
                    os.writeBytes("mkdir -p '" + DST_DIR + "'\n");
                    for (int i = 0; i < srcPaths.length; i++) {
                        os.writeBytes("cp '" + srcPaths[i] + "' '" + DST_DIR + "/" + targetNames[i] + "'\n");
                        os.writeBytes("chmod 644 '" + DST_DIR + "/" + targetNames[i] + "'\n");
                    }
                    os.writeBytes("exit\n");
                    os.flush();
                    int code = p.waitFor();
                    if (code == 0) showMsg(okMsg); else showMsg(failMsg + ",请确认 Root 权限");
                } catch (Exception e) { showMsg(failMsg + ": " + e.getMessage()); }
            }
        }).start();
    }

    private void applyOriginPak() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String src = extractAssetToInternal("red_origin.pak");
                    if (src == null || !new java.io.File(src).exists()) {
                        showMsg("提取原版文件失败");
                        return;
                    }
                    Process p = CmdExec.getShell();
                    DataOutputStream os = new DataOutputStream(p.getOutputStream());
                    os.writeBytes("mkdir -p '" + DST_DIR + "'\n");
                    os.writeBytes("cp '" + src + "' '" + DST_DIR + "/" + PAK_NAME + "'\n");
                    os.writeBytes("chmod 644 '" + DST_DIR + "/" + PAK_NAME + "'\n");
                    os.writeBytes("exit\n");
                    os.flush();
                    int code = p.waitFor();
                    if (code == 0) showMsg("原版已启用 ✓");
                    else showMsg("启用失败,请确认 Root 权限");
                } catch (Exception e) {
                    showMsg("启用失败: " + e.getMessage());
                }
            }
        }).start();
    }

    private void removePakFile() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String src = extractAssetToInternal("red_origin.pak");
                    if (src == null || !new java.io.File(src).exists()) {
                        showMsg("提取原版文件失败");
                        return;
                    }
                    Process p = CmdExec.getShell();
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

    private void playBeep() {
        try {
            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), uri);
            if (r != null) r.play();
        } catch (Exception e) {}
    }

    private void showMsg(final String msg) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(FloatingService.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int getMaxRefreshRate() {
        try {
            android.view.WindowManager wm =
                (android.view.WindowManager) getSystemService(WINDOW_SERVICE);
            android.view.Display.Mode[] modes =
                wm.getDefaultDisplay().getSupportedModes();
            float max = 60f;
            for (android.view.Display.Mode m : modes) {
                if (m.getRefreshRate() > max) max = m.getRefreshRate();
            }
            return Math.round(max);
        } catch (Throwable e) {
            return 120;
        }
    }

    private void clearBackground() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 第一步:用 shell 抓所有正在运行的第三方进程包名(绕过包可见性限制)
                String myPkg = getPackageName();
                String listOut = CmdExec.run(
                    "ps -A -o NAME | grep -E '\\.' | sort -u");
                java.util.HashSet<String> pkgs = new java.util.HashSet<String>();
                if (listOut != null && !listOut.startsWith("ERR") && !"NO_PERMISSION".equals(listOut)) {
                    for (String line : listOut.split("\n")) {
                        String name = line.trim();
                        // 取进程名主包名部分(去掉 :xxx 子进程后缀)
                        int colon = name.indexOf(':');
                        if (colon > 0) name = name.substring(0, colon);
                        if (name.length() < 3 || !name.contains(".")) continue;
                        // 排除自己和关键系统进程
                        if (name.equals(myPkg)) continue;
                        if (name.startsWith("android")) continue;
                        if (name.startsWith("com.android.systemui")) continue;
                        if (name.startsWith("com.android.launcher")) continue;
                        if (name.startsWith("com.miui.home")) continue;
                        if (name.startsWith("/")) continue;
                        pkgs.add(name);
                    }
                }
                int count = pkgs.size();
                StringBuilder cmd = new StringBuilder();
                for (String pkg : pkgs) {
                    cmd.append("am force-stop ").append(pkg).append(";");
                    cmd.append("am kill ").append(pkg).append(";");
                }
                cmd.append("am kill-all;");
                cmd.append("echo 3 > /proc/sys/vm/drop_caches;");
                final String res = CmdExec.run(cmd.toString());
                final int fcount = count;
                if (floatView != null) {
                    floatView.post(new Runnable() {
                        @Override
                        public void run() {
                            if ("NO_PERMISSION".equals(res)) {
                                showMsg("需要 Root 或 Shizuku 授权");
                            } else {
                                showMsg("已清理 " + fcount + " 个后台应用");
                                playBeep();
                            }
                            updateSwitch(
                                (View) floatView.findViewById(R.id.tile3).findViewById(R.id.tile_track),
                                (View) floatView.findViewById(R.id.tile3).findViewById(R.id.tile_thumb),
                                false, 40);
                        }
                    });
                }
            }
        }).start();
    }

    private void applyTile(int idx, boolean on) {
        final String cmd;
        final String okMsg;
        switch (idx) {
            case 0: // 悬浮窗 - 保持原逻辑,不发命令
                return;
            case 1: // 游戏模式
                cmd = on ? "settings put system game_mode 1"
                         : "settings put system game_mode 0";
                okMsg = on ? "游戏模式 已开启" : "游戏模式 已关闭";
                break;
            case 2: // 清理后台 - 强力清理所有第三方后台应用
                tileOn[2] = false;
                clearBackground();
                return;
            case 3: // 最高帧率 - 读取系统支持的最高刷新率
                if (on) {
                    int maxHz = getMaxRefreshRate();
                    cmd = "settings put system peak_refresh_rate " + maxHz
                        + " && settings put system min_refresh_rate " + maxHz;
                    okMsg = "已开启最高帧率 " + maxHz + "Hz";
                } else {
                    cmd = "settings put system peak_refresh_rate 60"
                        + " && settings put system min_refresh_rate 60";
                    okMsg = "已恢复 60Hz";
                }
                break;
            case 4: // 免打扰
                cmd = on ? "cmd notification set_dnd priority"
                         : "cmd notification set_dnd off";
                okMsg = on ? "免打扰 已开启" : "免打扰 已关闭";
                break;
            case 5: // 状态栏 - 隐藏/恢复
                cmd = on ? "settings put global policy_control immersive.status=*"
                         : "settings put global policy_control null";
                okMsg = on ? "状态栏 已隐藏" : "状态栏 已恢复";
                break;
            default:
                return;
        }
        final boolean clearTile2 = (idx == 2);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String res = CmdExec.run(cmd);
                if (floatView != null) {
                    floatView.post(new Runnable() {
                        @Override
                        public void run() {
                            if ("NO_PERMISSION".equals(res)) {
                                showMsg("需要 Root 或 Shizuku 授权");
                            } else if (res != null && res.startsWith("ERR")) {
                                showMsg("执行失败");
                            } else {
                                showMsg(okMsg);
                                playBeep();
                            }
                            if (clearTile2) {
                                updateSwitch(
                                    (View) floatView.findViewById(R.id.tile3).findViewById(R.id.tile_track),
                                    (View) floatView.findViewById(R.id.tile3).findViewById(R.id.tile_thumb),
                                    false, 40);
                            }
                        }
                    });
                }
            }
        }).start();
    }

    private void updateSwitch(View track, View thumb, boolean on, int trackWidthDp) {
        track.setBackgroundResource(on ? R.drawable.switch_track_on : R.drawable.switch_track_off);
        float dx = on ? dp(trackWidthDp - 22 - 2) : 0;
        thumb.animate().translationX(dx).setDuration(150).start();
    }
}
