package com.xy.pak;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class CmdExec {

    // 是否有 Shizuku（服务运行且已授权）
    public static boolean hasShizuku() {
        try {
            return rikka.shizuku.Shizuku.pingBinder()
                && rikka.shizuku.Shizuku.checkSelfPermission()
                   == android.content.pm.PackageManager.PERMISSION_GRANTED;
        } catch (Throwable e) { return false; }
    }

    // 执行命令并返回输出（纯 Shizuku）
    public static String run(String cmd) {
        if (!hasShizuku()) return "NO_PERMISSION";
        try {
            Process p = getShell();
            java.io.DataOutputStream os = new java.io.DataOutputStream(p.getOutputStream());
            os.writeBytes(cmd + "\n");
            os.writeBytes("exit\n");
            os.flush();
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            p.waitFor();
            return sb.toString();
        } catch (Throwable e) { return "ERR:" + e.getMessage(); }
    }

    // 返回一个可写命令的 shell 进程（纯 Shizuku）
    public static Process getShell() throws Exception {
        if (!hasShizuku()) throw new Exception("Shizuku 未授权");
        java.lang.Class<?> sz = Class.forName("rikka.shizuku.Shizuku");
        java.lang.reflect.Method m = sz.getDeclaredMethod(
            "newProcess", String[].class, String[].class, String.class);
        m.setAccessible(true);
        return (Process) m.invoke(null, new String[]{"sh"}, null, null);
    }
}
