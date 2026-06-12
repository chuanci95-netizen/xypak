package com.xy.pak;

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
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import rikka.shizuku.Shizuku;

public class PermissionFragment extends Fragment {

    private static final String PREF_NAME = "perm_prefs";
    private static final String KEY_MODE = "selected_mode";
    private static final String SHIZUKU_PKG = "moe.shizuku.privileged.api";
    private static final int SHIZUKU_CODE = 1001;

    private boolean rootGranted = false;
    private boolean shizukuGranted = false;

    private View checkRoot, checkShizuku;
    private TextView progressText, txtBottomHint, toolInstallTitle, toolInstallDesc;
    private ProgressBar progressBar;

    private Shizuku.OnRequestPermissionResultListener permListener = (requestCode, grantResult) -> {
        if (requestCode == SHIZUKU_CODE) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                shizukuGranted = true;
                rootGranted = false;
                saveMode("shizuku");
                Toast.makeText(getContext(), "Shizuku 授权成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Shizuku 授权被拒绝", Toast.LENGTH_SHORT).show();
            }
            updateUI();
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_permission, container, false);

        checkRoot = v.findViewById(R.id.check_root);
        checkShizuku = v.findViewById(R.id.check_shizuku);
        progressText = v.findViewById(R.id.progress_text);
        progressBar = v.findViewById(R.id.progress_bar);
        txtBottomHint = v.findViewById(R.id.txt_bottom_hint);
        toolInstallTitle = v.findViewById(R.id.tool_install_title);
        toolInstallDesc = v.findViewById(R.id.tool_install_desc);

        String savedMode = getPrefs().getString(KEY_MODE, "");
        if ("root".equals(savedMode)) rootGranted = true;
        else if ("shizuku".equals(savedMode)) shizukuGranted = true;

        Shizuku.addRequestPermissionResultListener(permListener);

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
            if (!isShizukuInstalled()) {
                Toast.makeText(getContext(), "请先安装 Shizuku", Toast.LENGTH_LONG).show();
                return;
            }

            try {
                if (!Shizuku.pingBinder()) {
                    Toast.makeText(getContext(), "Shizuku 服务未运行\n请先打开 Shizuku 并启动服务", Toast.LENGTH_LONG).show();
                    launchShizuku();
                    return;
                }
            } catch (Exception e) {
                Toast.makeText(getContext(), "Shizuku 服务未运行", Toast.LENGTH_LONG).show();
                launchShizuku();
                return;
            }

            if (shizukuGranted) {
                shizukuGranted = false;
                saveMode("");
                Toast.makeText(getContext(), "已取消 Shizuku", Toast.LENGTH_SHORT).show();
                updateUI();
            } else {
                // 检查是否已授权
                try {
                    if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                        shizukuGranted = true;
                        rootGranted = false;
                        saveMode("shizuku");
                        Toast.makeText(getContext(), "Shizuku 模式已启用", Toast.LENGTH_SHORT).show();
                        updateUI();
                    } else {
                        // 请求授权，会弹出图3那样的弹窗
                        Shizuku.requestPermission(SHIZUKU_CODE);
                    }
                } catch (Exception e) {
                    Toast.makeText(getContext(), "请在 Shizuku 中授权本应用", Toast.LENGTH_LONG).show();
                    launchShizuku();
                }
            }
        });

        // 快捷工具：安装/打开 Shizuku
        v.findViewById(R.id.tool_install_shizuku).setOnClickListener(view -> {
            if (isShizukuInstalled()) {
                launchShizuku();
            } else {
                installShizukuFromAssets();
            }
        });

        // 快捷工具：打开无线调试
        v.findViewById(R.id.tool_wireless_debug).setOnClickListener(view -> {
            try {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                startActivity(intent);
                Toast.makeText(getContext(), "请找到「无线调试」并打开", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(getContext(), "无法打开开发者选项", Toast.LENGTH_LONG).show();
            }
        });

        updateUI();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 自动检测 Shizuku 状态
        if (isShizukuInstalled()) {
            try {
                if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    if (!shizukuGranted && !"root".equals(getPrefs().getString(KEY_MODE, ""))) {
                        shizukuGranted = true;
                        saveMode("shizuku");
                    }
                }
            } catch (Exception ignored) {}
        }
        updateUI();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Shizuku.removeRequestPermissionResultListener(permListener);
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

        // 快捷工具文字动态变
        boolean installed = isShizukuInstalled();
        if (installed) {
            toolInstallTitle.setText("打开 Shizuku");
            toolInstallDesc.setText("Shizuku 已安装，点击打开");
        } else {
            toolInstallTitle.setText("安装 Shizuku");
            toolInstallDesc.setText("从内置包安装 Shizuku 服务");
        }

        // 底部提示
        if (installed) {
            try {
                if (Shizuku.pingBinder()) {
                    txtBottomHint.setText("运行中 ✓");
                    txtBottomHint.setTextColor(0xFF4CAF50);
                    return;
                }
            } catch (Exception ignored) {}
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
            ctx.getPackageManager().getPackageInfo(SHIZUKU_PKG, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void launchShizuku() {
        try {
            Context ctx = getContext();
            if (ctx == null) return;
            Intent intent = ctx.getPackageManager().getLaunchIntentForPackage(SHIZUKU_PKG);
            if (intent != null) startActivity(intent);
        } catch (Exception ignored) {}
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
