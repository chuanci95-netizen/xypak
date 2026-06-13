package com.xy.pak;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    public static final int ID_HOME = 1001;
    public static final int ID_PERM = 1002;
    public static final int ID_SETTINGS = 1004;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView nav = findViewById(R.id.bottom_nav);

        Menu menu = nav.getMenu();
        menu.add(0, ID_HOME, 0, "主页").setIcon(R.drawable.ic_home);
        menu.add(0, ID_SETTINGS, 1, "设置").setIcon(R.drawable.ic_settings);

        int[][] states = new int[][] {
            new int[] {  android.R.attr.state_checked },
            new int[] { -android.R.attr.state_checked }
        };
        int[] colors = new int[] {
            0xFF1FB960,
            0xFF9AA0A6
        };
        ColorStateList csl = new ColorStateList(states, colors);
        nav.setItemIconTintList(csl);
        nav.setItemTextColor(csl);

        nav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment f = null;
                int id = item.getItemId();
                if (id == ID_HOME) f = new HomeFragment();
                else if (id == ID_PERM) f = new PermissionFragment();
                else if (id == ID_SETTINGS) f = new PermissionFragment();
                if (f != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, f).commit();
                    return true;
                }
                return false;
            }
        });

        if (savedInstanceState == null) {
            nav.setSelectedItemId(ID_HOME);
        }
    }
}
