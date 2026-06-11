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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class PermissionFragment extends Fragment {

    private static final String PREF_NAME = "perm_prefs";
    private static final String KEY_MODE = "selected_mode";

    // Shizuku 可能的包名
    private static final String[] SHIZUKU_PKGS = {
        "moe.shizuku.privileged.api",
        "rikka.shizuku"
    };

    // MT 管理器包名
    private static final String MT_PKG = "bin.mt.plus";

    private boolean rootGranted = false;
    private boolean shizukuGranted = false;
    private boolean shizukuInstalled = false;
    private boolean shizukuRunning = false;

    private View checkRoot, checkShizuku, checkInstall;
    private TextView progressText;
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

        // 读取保存的模式
        String savedMode = getPrefs().getString(KEY_MODE, "");
        if ("root".equals(savedMode)) {
            rootGranted = true;
        } else if ("shizuku".equals(savedMode)) {
            shizukuGranted = true;
        }

        // 检测 Shizuku 安装和运行状态
        shizukuInstalled = isShizukuInstalled();
        shizukuRunning = isShizukuRunning();

        // Root 模式点击
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

        // Shizuku 模式点击
        v.findViewById(R.id.card_shizuku).setOnClickListener(view -> {
            // 先刷新状态
            shizukuInstalled = isShizukuInstalled();
            shizukuRunning = isShizukuRunning();

            if (!shizukuInstalled) {
                Toast.makeText(getContext(), "请先安装 Shizuku（点击下方安装按钮）", Toast.LENGTH_LONG).show();
                return;
            }

            if (!shizukuRunning) {
                Toast.makeText(getContext(), "Shizuku 未运行，请先打开 Shizuku 并启动服务（无线调试/Root 方式）", Toast.LENGTH_LONG).show();
                // 打开 Shizuku 应用
                launchShizuku();
                return;
            }

            // 检查 MT 管理器是否安装
            if (!isAppInstalled(MT_PKG)) {
                Toast.makeText(getContext(), "请先安装 MT 管理器", Toast.LENGTH_LONG).show();
                return;
            }

            // Shizuku 已运行，切换选择
            if (shizukuGranted) {
                shizukuGranted = false;
                saveMode("");
                Toast.makeText(getContext(), "已取消 Shizuku", Toast.LENGTH_SHORT).show();
            } else {
                shizukuGranted = true;
                rootGranted = false;
                saveMode("shizuku");
                Toast.makeText(getContext(), "Shizuku 模式已启用\n请确保 MT 管理器已在 Shizuku 中授权", Toast.LENGTH_LONG).show();
            }
            updateUI();
        });

        // 安装 Shizuku 点击
        v.findViewById(R.id.card_install).setOnClickListener(view -> {
            shizukuInstalled = isShizukuInstalled();
            if (shizukuInstalled) {
                // 已安装，直接打开
                Toast.makeText(getContext(), "Shizuku 已安装，正在打开...", Toast.LENGTH_SHORT).show();
                launchShizuku();
            } else {
                // 未安装，从 assets 安装
                installShizukuFromAssets();
            }
            updateUI();
        });

        updateUI();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次回到页面刷新状态
        shizukuInstalled = isShizukuInstalled();
        shizukuRunning = isShizukuRunning();
        updateUI();
    }

    private void saveMode(String mode) {
        getPrefs().edit().putString(KEY_MODE, mode).apply();
    }

    private SharedPreferences getPrefs() {
        return requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
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
    }

    /** 申请 Root */
    private boolean requestRoot() {
        try {
            Process p = Runtime.getRuntime().exec("su");
            p.getOutputStream().write("exit\n".getBytes());
            p.getOutputStream().flush();
            int code = p.waitFor();
            return code == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** 检查 Shizuku 是否安装（兼容多个包名） */
    private boolean isShizukuInstalled() {
        PackageManager pm = requireContext().getPackageManager();
        for (String pkg : SHIZUKU_PKGS) {
            try {
                pm.getPackageInfo(pkg, 0);
                return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    /** 检查 Shizuku 服务是否在运行 */
    private boolean isShizukuRunning() {
        try {
            // 通过 ps 检测 shizuku 进程
            Process p = Runtime.getRuntime().exec("ps -A");
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("shizuku") || line.contains("rish")) {
                    br.close();
                    return true;
                }
            }
            br.close();
        } catch (Exception ignored) {}

        // 备用方案：检查 Shizuku binder 是否存在
        try {
            Process p = Runtime.getRuntime().exec("service check ShizukuService");
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String result = br.readLine();
            br.close();
            if (result != null && !result.contains("not found")) {
                return true;
            }
        } catch (Exception ignored) {}

        return false;
    }

    /** 检查某个 App 是否安装 */
    private boolean isAppInstalled(String pkg) {
        try {
            requireContext().getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** 打开 Shizuku 应用 */
    private void launchShizuku() {
        PackageManager pm = requireContext().getPackageManager();
        for (String pkg : SHIZUKU_PKGS) {
            Intent intent = pm.getLaunchIntentForPackage(pkg);
            if (intent != null) {
                startActivity(intent);
                return;
            }
        }
        Toast.makeText(getContext(), "无法打开 Shizuku", Toast.LENGTH_SHORT).show();
    }

    /** 从 assets 安装 Shizuku */
    private void installShizukuFromAssets() {
        try {
            Context ctx = requireContext();
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
