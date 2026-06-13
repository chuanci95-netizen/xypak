package com.xy.pak;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.widget.Toast;

public class VpnAuthActivity extends Activity {
    private static final int REQ = 7001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent prepare = VpnService.prepare(this);
        if (prepare != null) {
            startActivityForResult(prepare, REQ);
        } else {
            onActivityResult(REQ, RESULT_OK, null);
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ && res == RESULT_OK) {
            Intent i = new Intent(this, BlockVpnService.class);
            i.setAction(BlockVpnService.ACTION_START);
            startService(i);
            Toast.makeText(this, "大厅防封已开启", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "需要授权 VPN 才能防封", Toast.LENGTH_LONG).show();
        }
        finish();
    }
}