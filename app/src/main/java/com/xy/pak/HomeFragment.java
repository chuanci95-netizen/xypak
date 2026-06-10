package com.xy.pak;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);

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
