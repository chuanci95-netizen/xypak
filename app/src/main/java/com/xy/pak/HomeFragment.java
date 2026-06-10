package com.xy.pak;

import android.animation.ArgbEvaluator;
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
    private ValueAnimator colorAnim;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        btnStartFloat = v.findViewById(R.id.btn_start_float);

        // 卡片跳转链接
        View cardSafe = v.findViewById(R.id.card_safe);
        if (cardSafe != null) {
            cardSafe.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(LINK_URL));
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "打开链接失败", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        // 七彩文字闪烁动画
        final TextView textSafe = v.findViewById(R.id.text_safe);
        if (textSafe != null) {
            int[] colors = new int[] {
                0xFFE11D48, // 红
                0xFFFF8A00, // 橙
                0xFFEAB308, // 黄
                0xFF22C55E, // 绿
                0xFF06B6D4, // 青
                0xFF3B82F6, // 蓝
                0xFF9B5BF5, // 紫
                0xFFEC4899, // 粉
                0xFFE11D48  // 回到红，形成循环
            };
            colorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), (Object[]) box(colors));
            colorAnim.setDuration(6000);
            colorAnim.setRepeatCount(ValueAnimator.INFINITE);
            colorAnim.setInterpolator(new LinearInterpolator());
            colorAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    textSafe.setTextColor((Integer) animation.getAnimatedValue());
                }
            });
            colorAnim.start();
        }

        // 启动悬浮窗
        btnStartFloat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleFloat();
            }
        });

        // 4 个盾牌
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

    /** 把 int[] 包装成 Integer[]（ValueAnimator.ofObject 不接受原生数组） */
    private Integer[] box(int[] arr) {
        Integer[] r = new Integer[arr.length];
        for (int i = 0; i < arr.length; i++) r[i] = arr[i];
        return r;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshFloatBtn();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (colorAnim != null) colorAnim.cancel();
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
