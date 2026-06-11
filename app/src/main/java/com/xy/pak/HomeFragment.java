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

    private static final String LINK_DATA = "http://518fkw.top/links/115CD2D8";
    private static final String LINK_BUY = "http://518fkw.top/links/1A2C3839";
    private static final String LINK_CHANNEL = "https://t.me/XYZDZR";

    private TextView btnStartFloat;
    private ValueAnimator animSafe;
    private ValueAnimator animBuy;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        btnStartFloat = v.findViewById(R.id.btn_start_float);

        // 「购买小月内部」→ 跳转购买链接
        final TextView textBuy = v.findViewById(R.id.text_buy);
        if (textBuy != null) {
            textBuy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openUrl(LINK_BUY);
                }
            });
            animBuy = startColorAnim(textBuy);
        }

        // 「和平高质量数据号」→ 跳转数据号链接（同时七彩）
        final TextView textSafe = v.findViewById(R.id.text_safe);
        if (textSafe != null) {
            textSafe.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openUrl(LINK_DATA);
                }
            });
            animSafe = startColorAnim(textSafe);
        }

        // 公告条目 1 → 跳转 Telegram 频道
        View notice1 = v.findViewById(R.id.notice_1);
        if (notice1 != null) {
            notice1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openUrl(LINK_CHANNEL);
                }
            });
        }

        btnStartFloat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleFloat();
            }
        });

        return v;
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "打开链接失败", Toast.LENGTH_SHORT).show();
        }
    }

    /** 给 TextView 启动七彩循环动画 */
    private ValueAnimator startColorAnim(final TextView target) {
        int[] colors = new int[] {
            0xFFE11D48, 0xFFFF8A00, 0xFFEAB308, 0xFF22C55E,
            0xFF06B6D4, 0xFF3B82F6, 0xFF9B5BF5, 0xFFEC4899, 0xFFE11D48
        };
        Integer[] boxed = new Integer[colors.length];
        for (int i = 0; i < colors.length; i++) boxed[i] = colors[i];

        ValueAnimator anim = ValueAnimator.ofObject(new ArgbEvaluator(), (Object[]) boxed);
        anim.setDuration(6000);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.setInterpolator(new LinearInterpolator());
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                target.setTextColor((Integer) animation.getAnimatedValue());
            }
        });
        anim.start();
        return anim;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshFloatBtn();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (animSafe != null) animSafe.cancel();
        if (animBuy != null) animBuy.cancel();
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
