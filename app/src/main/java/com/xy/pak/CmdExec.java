package com.xy.pak;

import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class CmdExec {

    public static boolean hasRoot() {
        try {
            Process p = Runtime.getRuntime().exec("su -c id");
            p.waitFor();
            return p.exitValue() == 0;
        } catch (Exception e) { return false; }
    }

    public static boolean hasShizuku() {
        try {
            return rikka.shizuku.Shizuku.pingBinder()
                && rikka.shizuku.Shizuku.checkSelfPermission()
                   == android.content.pm.PackageManager.PERMISSION_GRANTED;
        } catch (Throwable e) { return false; }
    }

    public static String run(String cmd) {
        if (hasRoot()) return runRoot(cmd);
        if (hasShizuku()) return runShizuku(cmd);
        return "NO_PERMISSION";
    }

    private static String runRoot(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes(cmd + "\n");
            os.writeBytes("exit\n");
            os.flush();
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            p.waitFor();
            return sb.toString();
        } catch (Exception e) { return "ERR:" + e.getMessage(); }
    }

    private static String runShizuku(String cmd) {
        try {
            java.lang.Class<?> sz = Class.forName("rikka.shizuku.Shizuku");
            java.lang.reflect.Method m = sz.getDeclaredMethod(
                "newProcess", String[].class, String[].class, String.class);
            m.setAccessible(true);
            Process p = (Process) m.invoke(null,
                new String[]{"sh", "-c", cmd}, null, null);
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            p.waitFor();
            return sb.toString();
        } catch (Throwable e) { return "ERR:" + e.getMessage(); }
    }

    // 返回一个可写命令的 shell 进程:优先 Root,其次 Shizuku
    public static Process getShell() throws Exception {
        if (hasRoot()) {
            return Runtime.getRuntime().exec("su");
        }
        if (hasShizuku()) {
            java.lang.Class<?> sz = Class.forName("rikka.shizuku.Shizuku");
            java.lang.reflect.Method m = sz.getDeclaredMethod(
                "newProcess", String[].class, String[].class, String.class);
            m.setAccessible(true);
            return (Process) m.invoke(null, new String[]{"sh"}, null, null);
        }
        throw new Exception("无 Root 或 Shizuku 权限");
    }
}
