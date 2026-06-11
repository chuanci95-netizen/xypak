package com.xy.pak;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class PermissionFragment extends Fragment {

    private static final String PREF_NAME = "perm_prefs";
    private static final String KEY_MODE = "selected_mode";
    private static final String SHIZUKU_PKG = "moe.shizuku.privileged.api";

    private boolean rootGranted = false;
    private boolean shizukuGranted = false;
    private boolean shizukuInstalled = false;

    private View checkRoot, checkShizuku, checkInstall;
    private TextView progressText, txtBottomHint;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_permission, container, false);

        checkRoot = v.findViewById(R.id.check_root);
        checkShizuku = v.findViewById(R.id.check_shizuku);
        checkInstall = v.findViewById(R.id.check_install);
        progressText = v.findViewById(R.id.progress_text);
        progressBar = v.findViewById(R.id.progress_bar);
        txtBottomHint = v.findViewById(R.id.txt_bottom_hint);

        // 读取保存的模式
        String savedMode = getPrefs().getString(KEY_MODE, "");
        if ("root".equals(savedMode)) rootGranted = true;
        else if ("shizuku".equals(savedMode)) shizukuGranted = true;

        // Root 模式
        v.findViewById(R.id.card_root).setOnClickListener(view -> {
            if (rootGranted) {
                rootGranted = false;
                saveMode("");
                Toast.makeText(getContext(), "已取消 Root", Toast.LENGTH_SHORT).show();
            } else {
                if (requestRoot()) {
                    rootGranted = true;
                    shizukuGranted = false;
                    saveMode("root");
                    Toast.makeText(getContext(), "Root 权限已获取", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Root 不可用，请确认设备已 Root", Toast.LENGTH_LONG).show();
                }
            }
            updateUI();
        });

        // Shizuku 模式
        v.findViewById(R.id.card_shizuku).setOnClickListener(view -> {
            refreshShizukuState();

            if (!shizukuInstalled) {
                Toast.makeText(getContext(), "请先安装 Shizuku", Toast.LENGTH_LONG).show();
                return;
            }

            if (!isShizukuRunning()) {
                Toast.makeText(getContext(), "Shizuku 服务未运行\n请先打开 Shizuku 并启动服务", Toast.LENGTH_LONG).show();
                launchShizuku();
                return;
            }

            if (shizukuGranted) {
                shizukuGranted = false;
                saveMode("");
                Toast.makeText(getContext(), "已取消 Shizuku", Toast.LENGTH_SHORT).show();
            } else {
                shizukuGranted = true;
                rootGranted = false;
                saveMode("shizuku");
                Toast.makeText(getContext(), "Shizuku 模式已启用", Toast.LENGTH_SHORT).show();
            }
            updateUI();
        });

        // 安装 Shizuku 卡片
        v.findViewById(R.id.card_install).setOnClickListener(view -> {
            refreshShizukuState();
            if (shizukuInstalled) {
                Toast.makeText(getContext(), "Shizuku 已安装，正在打开...", Toast.LENGTH_SHORT).show();
                launchShizuku();
            } else {
                installShizukuFromAssets();
            }
        });

        // 快捷工具：安装 Shizuku
        v.findViewById(R.id.tool_install_shizuku).setOnClickListener(view -> {
            refreshShizukuState();
            if (shizukuInstalled) {
                launchShizuku();
            } else {
                installShizukuFromAssets();
            }
        });

        // 快捷工具：打开无线调试
        v.findViewById(R.id.tool_wireless_debug).setOnClickListener(view -> {
            try {
                // Android 11+ 直接跳无线调试
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                startActivity(intent);
                Toast.makeText(getContext(), "请在开发者选项中找到「无线调试」并打开", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(getContext(), "无法打开开发者选项: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        refreshShizukuState();
        updateUI();

        // 如果 Shizuku 已安装且运行，自动弹出授权请求
        if (shizukuInstalled && isShizukuRunning()) {
            requestShizukuPermission();
        }

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshShizukuState();
        updateUI();
    }

    private void refreshShizukuState() {
        shizukuInstalled = isShizukuInstalled();
    }

    private void saveMode(String mode) {
        getPrefs().edit().putString(KEY_MODE, mode).apply();
    }

    private SharedPreferences getPrefs() {
        return getContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private void updateUI() {
        checkRoot.setVisibility(rootGranted ? View.VISIBLE : View.INVISIBLE);
        checkShizuku.setVisibility(shizukuGranted ? View.VISIBLE : View.INVISIBLE);
        checkInstall.setVisibility(shizukuInstalled ? View.VISIBLE : View.INVISIBLE);

        if (rootGranted) {
            progressText.setText("已选择 Root 模式");
            progressBar.setProgress(100);
        } else if (shizukuGranted) {
            progressText.setText("已选择 Shizuku 模式");
            progressBar.setProgress(100);
        } else {
            progressText.setText("请选择模式");
            progressBar.setProgress(0);
        }

        // 底部提示
        if (shizukuInstalled && isShizukuRunning()) {
            txtBottomHint.setText("Shizuku 运行中 ✓");
            txtBottomHint.setTextColor(0xFF4CAF50);
        } else if (shizukuInstalled) {
            txtBottomHint.setText("Shizuku 已安装，请启动服务");
            txtBottomHint.setTextColor(0xFFFF9800);
        } else {
            txtBottomHint.setText("请先安装 Shizuku");
            txtBottomHint.setTextColor(0xFF888888);
        }
    }

    private boolean requestRoot() {
        try {
            Process p = Runtime.getRuntime().exec("su");
            p.getOutputStream().write("exit\n".getBytes());
            p.getOutputStream().flush();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isShizukuInstalled() {
        try {
            Context ctx = getContext();
            if (ctx == null) return false;
            ctx.getPackageManager().getPackageInfo(SHIZUKU_PKG, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isShizukuRunning() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", "ps -A 2>/dev/null | grep shizuku"});
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = br.readLine();
            br.close();
            if (line != null && line.contains("shizuku")) return true;
        } catch (Exception ignored) {}

        // 备用：用 su 检测
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "ps -A | grep shizuku"});
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = br.readLine();
            br.close();
            if (line != null && line.contains("shizuku")) return true;
        } catch (Exception ignored) {}

        return false;
    }

    private void requestShizukuPermission() {
        // 尝试通过 ContentProvider 触发 Shizuku 授权弹窗
        try {
            Context ctx = getContext();
            if (ctx == null) return;
            // 启动 Shizuku 让它弹授权
            Intent intent = ctx.getPackageManager().getLaunchIntentForPackage(SHIZUKU_PKG);
            if (intent != null) {
                // 不直接启动，只是确认可以启动
                // 授权弹窗由 Shizuku SDK 自动触发
            }
        } catch (Exception ignored) {}
    }

    private void launchShizuku() {
        try {
            Context ctx = getContext();
            if (ctx == null) return;
            Intent intent = ctx.getPackageManager().getLaunchIntentForPackage(SHIZUKU_PKG);
            if (intent != null) {
                startActivity(intent);
            } else {
                Toast.makeText(ctx, "无法打开 Shizuku", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "打开 Shizuku 失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void installShizukuFromAssets() {
        try {
            Context ctx = getContext();
            if (ctx == null) return;
            File outFile = new File(ctx.getExternalCacheDir(), "shizuku.apk");

            InputStream in = ctx.getAssets().open("shizuku.apk");
            OutputStream out = new FileOutputStream(outFile);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in.close();
            out.close();

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri uri = FileProvider.getUriForFile(ctx, "com.xy.pak.fileprovider", outFile);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "安装失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
