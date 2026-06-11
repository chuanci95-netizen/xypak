package com.xy.pak;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
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

public class PermissionFragment extends Fragment {

    private static final String PREF_NAME = "perm_prefs";
    private static final String KEY_MODE = "selected_mode";

    private boolean rootGranted = false;
    private boolean shizukuGranted = false;
    private boolean shizukuInstalled = false;

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

        String savedMode = getPrefs().getString(KEY_MODE, "");
        if ("root".equals(savedMode)) {
            rootGranted = true;
        } else if ("shizuku".equals(savedMode)) {
            shizukuGranted = true;
        }

        shizukuInstalled = isShizukuInstalled();

        v.findViewById(R.id.card_root).setOnClickListener(view -> {
            if (!rootGranted) {
                try {
                    Process p = Runtime.getRuntime().exec("su -c echo ok");
                    p.waitFor();
                    rootGranted = true;
                    shizukuGranted = false;
                    saveMode("root");
                    Toast.makeText(getContext(), "Root 权限已获取", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Root 请求失败", Toast.LENGTH_SHORT).show();
                }
            } else {
                rootGranted = false;
                saveMode("");
            }
            updateUI();
        });

        v.findViewById(R.id.card_shizuku).setOnClickListener(view -> {
            if (shizukuInstalled) {
                shizukuGranted = !shizukuGranted;
                rootGranted = false;
                saveMode(shizukuGranted ? "shizuku" : "");
                updateUI();
            } else {
                Toast.makeText(getContext(), "请先安装 Shizuku", Toast.LENGTH_SHORT).show();
            }
        });

        v.findViewById(R.id.card_install_shizuku).setOnClickListener(view -> {
            installShizuku();
        });

        updateUI();
        return v;
    }

    private void saveMode(String mode) {
        getPrefs().edit().putString(KEY_MODE, mode).apply();
    }

    private SharedPreferences getPrefs() {
        return requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private void updateUI() {
        // 用 visibility 控制勾选（布局里是 TextView 不是 ImageView）
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

    private boolean isShizukuInstalled() {
        try {
            requireContext().getPackageManager().getPackageInfo("moe.shizuku.privileged.api", 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void installShizuku() {
        try {
            InputStream is = requireContext().getAssets().open("shizuku.apk");
            File out = new File(requireContext().getCacheDir(), "shizuku.apk");
            OutputStream os = new FileOutputStream(out);
            byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) > 0) os.write(buf, 0, len);
            os.close();
            is.close();

            Uri uri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", out);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "安装失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
