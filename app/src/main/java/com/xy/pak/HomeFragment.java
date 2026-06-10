package com.xy.pak;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {

    private static final String LINK_URL = "http://518fkw.top/links/115CD2D8";
    private TextView btnStartFloat;
    private ObjectAnimator anim1, anim2;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        btnStartFloat = v.findViewById(R.id.btn_start_float);

        View.OnClickListener openLink = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(LINK_URL));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(getContext(), "打开链接失败", Toast.LENGTH_SHORT).show();
                }
            }
        };

        View btnGame = v.findViewById(R.id.btn_game_account);
        View cardSafe = v.findViewById(R.id.card_safe);
        View pillSafe = v.findViewById(R.id.pill_safe);

        if (btnGame != null) btnGame.setOnClickListener(openLink);
        if (cardSafe != null) cardSafe.setOnClickListener(openLink);

        // 呼吸闪烁动画
        if (btnGame != null) {
            anim1 = ObjectAnimator.ofFloat(btnGame, "alpha", 1f, 0.5f, 1f);
            anim1.setDuration(1800);
            anim1.setRepeatCount(ValueAnimator.INFINITE);
            anim1.setInterpolator(new LinearInterpolator());
            anim1.start();
        }
        if (pillSafe != null) {
            anim2 = ObjectAnimator.ofFloat(pillSafe, "alpha", 1f, 0.55f, 1f);
            anim2.setDuration(1800);
            anim2.setRepeatCount(ValueAnimator.INFINITE);
            anim2.setInterpolator(new LinearInterpolator());
            anim2.start();
        }

        btnStartFloat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleFloat();
            }
        });

        View.OnClickListener placeholder = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getContext(), "功能开发中", Toast.LENGTH_SHORT).show();
            }
        };
        if (v.findViewById(R.id.fn1) != null) v.findViewById(R.id.fn1).setOnClickListener(placeholder);
        if (v.findViewById(R.id.fn2) != null) v.findViewById(R.id.fn2).setOnClickListener(placeholder);
        if (v.findViewById(R.id.fn3) != null) v.findViewById(R.id.fn3).setOnClickListener(placeholder);
        if (v.findViewById(R.id.fn4) != null) v.findViewById(R.id.fn4).setOnClickListener(placeholder);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshFloatBtn();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (anim1 != null) anim1.cancel();
        if (anim2 != null) anim2.cancel();
    }

    private void refreshFloatBtn() {
        if (btnStartFloat != null) {
            if (FloatingService.isRunning) {
                btnStartFloat.setText("关闭悬浮窗  →");
            } else {
                btnStartFloat.setText("立即启动  →");
            }
        }
    }

    private void toggleFloat() {
        Context ctx = getContext();
        if (FloatingService.isRunning) {
            ctx.stopService(new Intent(ctx, FloatingService.class));
            Toast.makeText(ctx, "悬浮窗已关闭", Toast.LENGTH_SHORT).show();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(ctx)) {
                Toast.makeText(ctx, "请先授予悬浮窗权限", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + ctx.getPackageName()));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return;
            }
            ctx.startService(new Intent(ctx, FloatingService.class));
            Toast.makeText(ctx, "悬浮窗已开启", Toast.LENGTH_SHORT).show();
        }
        refreshFloatBtn();
    }
}
