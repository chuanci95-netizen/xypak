package com.xy.pak;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {

    private static final String LINK_URL = "http://518fkw.top/links/115CD2D8";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);

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

        // 游戏小号按钮跳转
        View btnGame = v.findViewById(R.id.btn_game_account);
        if (btnGame != null) btnGame.setOnClickListener(openLink);

        // 和平小号安全有保障 卡片跳转
        View cardSafe = v.findViewById(R.id.card_safe);
        if (cardSafe != null) cardSafe.setOnClickListener(openLink);

        // 原有逻辑
        v.findViewById(R.id.action_file).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new InjectDialog().show(getParentFragmentManager(), "inject");
            }
        });

        v.findViewById(R.id.btn_start_float).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getContext(), "悬浮窗功能：稍后自行扩展", Toast.LENGTH_SHORT).show();
            }
        });

        v.findViewById(R.id.action_server).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getContext(), "服务器：香港服", Toast.LENGTH_SHORT).show();
            }
        });

        v.findViewById(R.id.action_perm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getContext(), "权限模式：Root", Toast.LENGTH_SHORT).show();
            }
        });

        v.findViewById(R.id.action_float).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getContext(), "悬浮窗：未启动", Toast.LENGTH_SHORT).show();
            }
        });

        return v;
    }
}
