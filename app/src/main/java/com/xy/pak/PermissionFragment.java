package com.xy.pak;

import android.content.Context;
import android.content.Intent;
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

        shizukuInstalled = isShizukuInstalled();
        updateUI();

        v.findViewById(R.id.card_root).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (rootGranted) {
                    rootGranted = false;
                    Toast.makeText(getContext(), "已取消 Root", Toast.LENGTH_SHORT).show();
                    updateUI();
                    return;
                }
                if (requestRoot()) {
                    rootGranted = true;
                    Toast.makeText(getContext(), "Root 权限已获取", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Root 不可用，请确认设备已 Root", Toast.LENGTH_LONG).show();
                }
                updateUI();
            }
        });

        v.findViewById(R.id.card_shizuku).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!shizukuInstalled) {
                    Toast.makeText(getContext(), "请先安装 Shizuku", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (shizukuGranted) {
                    shizukuGranted = false;
                    Toast.makeText(getContext(), "已取消 Shizuku", Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        Intent intent = getContext().getPackageManager()
                                .getLaunchIntentForPackage("moe.shizuku.privileged.api");
                        if (intent != null) {
                            startActivity(intent);
                            shizukuGranted = true;
                            Toast.makeText(getContext(), "请在 Shizuku 中授权后返回", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getContext(), "未找到 Shizuku", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "启动 Shizuku 失败", Toast.LENGTH_SHORT).show();
                    }
                }
                updateUI();
            }
        });

        v.findViewById(R.id.card_install).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                installShizukuFromAssets();
            }
        });

        v.findViewById(R.id.card_game_boost).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getContext(), "游戏加速：功能开发中", Toast.LENGTH_SHORT).show();
            }
        });

        v.findViewById(R.id.card_net_boost).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getContext(), "网络优化：功能开发中", Toast.LENGTH_SHORT).show();
            }
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        shizukuInstalled = isShizukuInstalled();
        updateUI();
    }

    private void updateUI() {
        checkRoot.setVisibility(rootGranted ? View.VISIBLE : View.INVISIBLE);
        checkShizuku.setVisibility(shizukuGranted ? View.VISIBLE : View.INVISIBLE);
        checkInstall.setVisibility(shizukuInstalled ? View.VISIBLE : View.INVISIBLE);

        int count = 0;
        if (rootGranted) count++;
        if (shizukuGranted) count++;
        if (shizukuInstalled) count++;

        if (count == 0) {
            progressText.setText("请选择模式");
            progressBar.setProgress(0);
        } else {
            progressText.setText("已选择 " + count + "/3");
            progressBar.setProgress(count * 100 / 3);
        }
    }

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

    private boolean isShizukuInstalled() {
        try {
            getContext().getPackageManager()
                    .getPackageInfo("moe.shizuku.privileged.api", 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void installShizukuFromAssets() {
        try {
            Context ctx = getContext();
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
