package com.xy.pak;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class AntiFreeze {

    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static final List<Thread> threads = new ArrayList<Thread>();
    private static Context appCtx;
    private static final Handler ui = new Handler(Looper.getMainLooper());

    // 5692盗号风险上报节点
    private static final String[] RISK_5692_IPS = {
        "101.226.96.217", "101.227.164.167", "61.151.229.151",
        "81.69.100.220",  "175.24.208.107",  "101.91.21.27"
    };

    // 反作弊实时探测节点
    private static final String[] ANTICHEAT_49155_IPS = {
        "171.109.109.66", "171.109.109.68", "171.109.109.69",
        "171.109.109.70", "171.109.109.71", "171.109.109.72",
        "171.109.109.73", "171.109.109.74", "171.109.109.75",
        "171.109.109.76", "182.40.25.26",   "182.40.25.32",
        "113.59.32.155",  "113.59.32.9",    "113.59.32.23",
        "113.59.32.7",    "113.59.32.153",  "116.196.140.69",
        "116.196.140.70", "116.196.140.74", "150.139.224.179",
        "150.139.224.141","218.61.39.109",  "218.61.10.21",
        "221.204.217.17", "221.204.217.22", "221.204.216.97",
        "123.139.191.137","61.189.2.103",   "61.189.2.100",
        "61.189.2.108",   "61.189.2.209",   "61.189.2.27",
        "123.180.181.6",  "42.4.50.75",     "42.4.50.72",
        "122.247.214.33", "120.226.82.103", "120.226.82.100",
        "115.238.189.134","124.225.141.51", "111.9.74.174",
        "113.0.124.201",  "116.196.140.68", "118.180.45.6",
        "119.188.64.2",   "120.226.82.17",  "120.226.82.21",
        "122.247.212.16", "122.247.214.9",  "122.247.214.10",
        "122.247.214.21", "124.225.132.11", "124.225.141.72",
        "150.139.224.162","182.40.25.11",   "182.40.25.19",
        "182.40.25.25",   "183.131.42.10",  "221.204.216.96",
        "221.204.217.21", "221.204.217.31", "221.204.48.197",
        "221.204.48.198", "61.189.2.15",    "61.189.2.16",
        "61.189.2.23",    "61.189.2.24",    "61.189.2.109",
        "61.189.2.113"
    };

    // 安全审计/行为矩阵上报IP
    private static final String[] AUDIT_IPS = {
        "106.55.209.88",  "139.186.105.126", "117.135.175.106",
        "36.155.132.88",  "180.109.156.92",  "183.192.196.27",
        "27.155.112.93"
    };

    // 异常探测节点
    private static final String[] PROBE_IPS = {
        "140.249.64.107", "140.249.64.60", "140.249.64.121", "27.155.112.58"
    };

    // 高风险战绩异常实时上报节点
    private static final String[] HIGHRISK_IPS = {
        "223.83.227.60", "223.83.33.14", "223.95.212.12",
        "58.212.47.187", "58.212.47.232"
    };

    // 高风险战绩上报网段
    private static final String[] HIGHRISK_RANGE_IPS = {
        "183.247.185.5",  "183.247.185.10", "183.247.185.11",
        "183.247.185.39", "183.247.185.51", "183.247.185.52",
        "115.236.128.24", "115.236.128.36", "115.236.128.112", "115.236.128.120"
    };

    // 小黑屋行为判定上报节点
    private static final String[] BLACKROOM_IPS = {
        "119.147.15.52", "119.147.15.57", "119.147.15.191"
    };

    // 封号/冻结/人脸/处罚精准节点
    private static final String[] EXTRA_BLOCK_IPS = {
        "117.27.241.244", "171.107.77.45", "222.94.109.22",
        "101.91.42.188",  "101.91.33.16",  "101.91.23.183",
        "180.109.171.23", "117.89.177.167","1.194.202.243",
        "182.107.81.132", "101.89.42.237", "113.108.28.224",
        "119.147.15.57",  "119.147.15.191","119.147.15.52",
        "27.155.112.93",  "140.249.64.60", "175.6.84.70",
        "106.55.209.88",  "139.186.105.126","117.135.175.106",
        "183.192.196.27", "36.155.132.88", "180.109.156.92",
        "222.216.230.182","222.216.230.73",
        "180.102.211.18","180.102.211.42","180.102.211.93","180.102.211.116"
    };

    // 黑名单域名
    private static final String[] BLOCK_HOSTS = {
        "android.bugly.qq.com",          "android.crashsight.qq.com",
        "android.rqd.qq.com",            "astrategy.beacon.qq.com",
        "istrategy.beacon.qq.com",       "ieventlog.beacon.qq.com",
        "pingma.qq.com",                 "cschannel.anticheatexpert.com",
        "cs.mbgame.anticheatexpert.com", "anticheatexpert.com",
        "hc.tdm.qq.com",                "hc3.tdm.qq.com",
        "tdm.qq.com",                   "log.pg.qq.com",
        "rqd.ias.tencent-cloud.net",    "rqd-v6.ias.tencent-cloud.net",
        "test.m.tencent.com",           "hpyj.itop.qq.com",
        "cjm.broker",                   "itop.qq.com",
        "dcl.itop.qq.com",              "aegis.qq.com",
        "tssn.qq.com",                  "tdsdk.qq.com",
        "jubao.qq.com",                 "sngsuc.qq.com",
        "datamore.qq.com",              "h.trace.qq.com",
        "tdir.qq.com",                  "tmeta.qq.com",
        "safe.qq.com",                  "tdm.cdn.tencentgame.com",
        "joint.tdm.qq.com",             "ace.tencent.com",
        "devicereport.qq.com",          "sdkconfig.itop.qq.com"
    };

    // 有root版新增精准拦截节点（封号/冻结评估/人脸/处罚/风控矩阵）
    private static final String[] EXTRA_BLOCK_IPS = {
        "117.27.241.244", "171.107.77.45", "222.94.109.22",
        "101.91.42.188",  "101.91.33.16",  "101.91.23.183",
        "180.109.171.23", "117.89.177.167","1.194.202.243",
        "182.107.81.132", "101.89.42.237", "113.108.28.224",
        "119.147.15.57",  "119.147.15.191","119.147.15.52",
        "27.155.112.93",  "140.249.64.60", "175.6.84.70",
        "106.55.209.88",  "139.186.105.126","117.135.175.106",
        "183.192.196.27", "36.155.132.88", "180.109.156.92",
        "222.216.230.182","222.216.230.73",
        "180.102.211.18","180.102.211.42","180.102.211.93","180.102.211.116"
    };

    // 大厅防鉴权域名（390个鉴权/登录/token/冻结相关）
    private static final String[] AUTH_DOMAINS = {
        "a.msdk.qq.com","a.ssl.msdk.qq.com","accauth.qq.com","access1.tpns.shs.qq.com",
        "account-auth-lock.qq.com","accountrisk.qq.com","accstatecheck.qq.com","alive.qq.com",
        "android-v1.perfsight.qq.com","android.crashsight.qq.com","anti-cheat-token-clear.ysdk.qq.com",
        "anti-risk-auth.qq.com","anti.qq.com","anti2.qq.com","anticheat-auth-kick.qq.com",
        "anticheat-lock.qq.com","anticheatexpert.com","api.tdm.qq.com","api.ysdk.qq.com",
        "area-token-reset.qq.com","auth-backstage-reset.qq.com","auth-cache-clear.qq.com",
        "auth-config.qq.com","auth-data.tdm.qq.com","auth-error-report-clean.qq.com",
        "auth-heartbeat.ysdk.qq.com","auth-inner-kill.qq.com","auth-inner-refresh.ysdk.qq.com",
        "auth-keepalive.qq.com","auth-log-upload.qq.com","auth-log.qq.com","auth-refresh.qq.com",
        "auth-refresh.ysdk.qq.com","auth-risk-revoke.qq.com","auth-rule.qq.com",
        "auth-safe-kick.qq.com","auth-scan.game.qq.com","auth-scan.ysdk.qq.com",
        "auth-session-kill.ysdk.qq.com","auth-session-reset.qq.com","auth-status-push.qq.com",
        "auth-ticket.qq.com","auth-version-limit.qq.com","auth.game.qq.com","authflush.qq.com",
        "authinner.qq.com","authkeep.qq.com","authsync.qq.com","b.msdk.qq.com","backcheck.qq.com",
        "backend-token-manage.qq.com","backup-auth-save.qq.com","backup-token-destroy.qq.com",
        "beat.qq.com","btrace.qq.com","c.msdk.qq.com","cdn.anticheatexpert.com",
        "cfg-update.qq.com","cgi.connect.qq.com","check-login-state.qq.com","check.device.qq.com",
        "check.login.qq.com","cloud-env-auth-kill.qq.com","config.game.qq.com","connlive.qq.com",
        "crash-auth-log.qq.com","crash.ysdk.qq.com","crashreport.qq.com","cross-login.qq.com",
        "cross-net-auth-kick.qq.com","cross-session-auth.qq.com","cs.mainconn.gamesafe.qq.com",
        "cs.mbgame.anticheatexpert.com","cs.mbgame.gamesafe.qq.com","cschannel.anticheatexpert.com",
        "data-auth.tdm.qq.com","devcheck.qq.com","devcheck.ysdk.qq.com","device-auth-token.qq.com",
        "device-auth.qq.com","device-bind-auth.qq.com","device-bind.qq.com",
        "device-check.ysdk.qq.com","device-env-auth.qq.com","device-env-check.qq.com",
        "device-lock.qq.com","device-root-check.qq.com","device-sn-auth.qq.com",
        "device-verify.ysdk.qq.com","devicereport.qq.com","dns.game.qq.com","dns.ysdk.qq.com",
        "double-token-verify.qq.com","down.anticheatexpert.com","emu-device-auth-reject.qq.com",
        "env-auth-expire.qq.com","env-auth-reject.qq.com","env-check.game.qq.com",
        "env-check.ysdk.qq.com","env-key-check.qq.com","env-risk-auth.qq.com",
        "env-token-ban.qq.com","env-verify.ysdk.qq.com","env-version-auth.qq.com",
        "envauth.tdm.qq.com","envverify.qq.com","equip-auth-check.qq.com","equipcheck.qq.com",
        "err-auth-report.qq.com","err-log-auth.qq.com","errreportauth.qq.com",
        "expire-token.qq.com","extendauth.qq.com","faascjm.native.qq.com",
        "face-auth-check.qq.com","faceid-lock.qq.com","faceid.qcloud.com","faceid.qq.com",
        "faceid.tencentcloudapi.com","faceid2.qq.com","faceid3.qq.com","faceid4.qq.com",
        "fast-auth-reset.qq.com","finger.qq.com","freeze.qq.com","freeze2.qq.com","freeze3.qq.com",
        "game-auth-kick-inner.qq.com","game-auth-kick.qq.com","game-auth-monitor.qq.com",
        "game-auth.qq.com","game-auth.ysdk.qq.com","game-login-expel.qq.com",
        "game-login-sync.qq.com","game-safe-auth-out.qq.com","gameguard.qq.com",
        "gamesafe-auth-clear.qq.com","gamesafe-freeze.qq.com","gamesafe-inner-auth.qq.com",
        "gameverauth.qq.com","gp-auth.qq.com","gp-livecheck.qq.com","gp-sync.qq.com",
        "gp-token.qq.com","gpcloud.tgpa.qq.com","guid.tpns.qq.com","h.trace.qq.com",
        "hardware-token-limit.qq.com","hb.game.qq.com","hb.ysdk.qq.com",
        "hc.tdm.qq.com","hc1.tdm.qq.com","hc2.tdm.qq.com","hc3.tdm.qq.com","hc4.tdm.qq.com",
        "health-freeze.qq.com","health.qq.com","health2.qq.com","health3.qq.com",
        "heartbeat-auth.qq.com","heartbeat.game.qq.com","heping-android.crashsight.qq.com",
        "heping-crash.qq.com","hippy.imtt.qq.com","hippy.ysdk.qq.com",
        "hpjy-op.tga.qq.com","hpjy.itop.qq.com","httpdns.game.qq.com","httpdns.qq.com",
        "httpdns.ysdk.qq.com","idcheck.qq.com","idcheck2.qq.com","idcloud.qq.com",
        "inner-beat.game.qq.com","inner-session-kill.ysdk.qq.com","inner-token.qq.com",
        "inner-verify.qq.com","innerlogincheck.qq.com","inspect-login.game.qq.com",
        "inspect.game.qq.com","internal-rpc.game.qq.com","ip-segment-auth-limit.qq.com",
        "ipv6.mainconn.gamesafe.qq.com","isolate-auth-lock.qq.com","jkyx.qq.com",
        "keepalive.ysdk.qq.com","key-verify-hit.qq.com","kill-session.ysdk.qq.com",
        "latest-token-update.qq.com","linecheck.qq.com","link-risk.qq.com","linkdetect.qq.com",
        "live-user.qq.com","livecheck.ysdk.qq.com","local-auth-backup.qq.com",
        "local-auth-flush.qq.com","local-auth-inner.game.qq.com","local-auth-reset.ysdk.qq.com",
        "local-auth.game.qq.com","local-auth.ysdk.qq.com","local-check.game.qq.com",
        "local-rpc.game.qq.com","local-token-clear.qq.com","local-token.ysdk.qq.com",
        "local-verify.game.qq.com","localauthreport.qq.com","lock2.qq.com","lock3.qq.com",
        "log-clear-auth.qq.com","log.game.qq.com","log.tdm.qq.com","log.ysdk.qq.com",
        "login-auth-recheck.qq.com","login-auth.qq.com","login-beat.game.qq.com",
        "login-cache-purge.qq.com","login-state-reset.qq.com","login-token-clear.qq.com",
        "login-token.qq.com","login-verify-auth.qq.com","login.ysdk.qq.com",
        "loginauthinner.qq.com","loginexpire.ysdk.qq.com","loginkick.qq.com",
        "loginrefresh.qq.com","loginstate.ysdk.qq.com","logtdm.qq.com","longtokenmonitor.qq.com",
        "msdk-auth.qq.com","msdk-token.qq.com","msdk-verify.qq.com","multi-login-check.qq.com",
        "net-auth.qq.com","net-check-auth.qq.com","net-environment-token.qq.com",
        "netdetect.qq.com","netriskold.qq.com","network-jump-auth-clear.qq.com",
        "network-risk-auth.qq.com","new-auth-sync.ysdk.qq.com","nj.cschannel.anticheatexpert.com",
        "notice-freeze.qq.com","oauth.qq.com","offlinecheck.qq.com",
        "old-version-auth-expire.qq.com","oldheart.qq.com","oldpassport.qq.com","oldticket.qq.com",
        "online-auth-beat.qq.com","online-auth-heartbeat.qq.com","online-beat.qq.com",
        "onlinestat.qq.com","opensdk.qq.com","oth.eve.mdt.qq.com","oth.str.mdt.qq.com",
        "passport.ysdk.qq.com","passport.zj.qq.com","pay.qq.com","payments.qq.com",
        "persisttokencheck.qq.com","phoneverify.qq.com","ping.game.qq.com","ping.ysdk.qq.com",
        "proc-check.qq.com","prolongtoken.qq.com","province-auth-restrict.qq.com",
        "punish-auth-cancel.qq.com","punish-token-cancel.qq.com","punish.qq.com",
        "push.game.qq.com","push.tpns.qq.com","push.ysdk.qq.com","qqauth.qq.com",
        "quick-login-kick.qq.com","quick-session-out.ysdk.qq.com","realname-auth-check.qq.com",
        "realname.qq.com","realname2.qq.com","receiver.tdm.qq.com","recheck-login-auth.qq.com",
        "refresh-token.game.qq.com","refresh.game.qq.com","region-auth-kick.ysdk.qq.com",
        "region-auth-limit.qq.com","remote-auth-control.qq.com","renew-token.qq.com",
        "renew.game.qq.com","renew.ysdk.qq.com","report-auth.game.qq.com","report.game.qq.com",
        "reset-login-state.qq.com","risk-auth-forbid.qq.com","risk-auth-reset.qq.com",
        "risk-check.ysdk.qq.com","risk-verify.ysdk.qq.com","riskcontrol.qq.com",
        "root-auth-reject.qq.com","root-detect-auth.qq.com","root-env-auth-ban.qq.com",
        "router.tdm.qq.com","rpt.qq.com","safe-auth.qq.com","safe-check.qq.com",
        "safe-tdm.qq.com","safe-token-clear.qq.com","savelogincheck.qq.com","scanlogin.qq.com",
        "sdk-auth-forcedown.qq.com","sdk-heart.qq.com","sdk-online-heart.qq.com","sdk.tdm.qq.com",
        "sdklogin.qq.com","sdkping.qq.com","sec-live.qq.com","sec.qq.com","secpassport.qq.com",
        "session-expire-check.qq.com","session-heartbeat.qq.com","session-kick.qq.com",
        "session-refresh-auth.qq.com","session-timeout-check.qq.com","sesskill.ysdk.qq.com",
        "sgame.qq.com","shorttoken.qq.com","speedtest.qq.com","ssl.login.qq.com","ssl.zc.qq.com",
        "sso-refresh.qq.com","sso-ticket.qq.com","sso.qq.com","stat-login.qq.com",
        "stat.game.qq.com","stat.ysdk.qq.com","state-sync.qq.com","statelogout.qq.com",
        "stattdm.qq.com","sub-sdk-token-out.qq.com","sync-state.ysdk.qq.com",
        "tdm-auth-forced-logout.qq.com","tdm-local-auth-reset.qq.com","tdm-report.qq.com",
        "tdm-upload.game.qq.com","tdmlogin.qq.com","tdmstat.game.qq.com","tempauth.qq.com",
        "third-party-auth-ban.qq.com","third-party-auth-check.qq.com","third-sdk-auth-clear.qq.com",
        "ticket-refresh.qq.com","ticket-revoke.qq.com","token-backup-clean.qq.com",
        "token-expire-check.qq.com","token-forced-invalidate.qq.com","token-kick-inner.qq.com",
        "token-kick.qq.com","token-lose-check.ysdk.qq.com","token-refresh.qq.com",
        "token-status-check.qq.com","token-sync.qq.com","tokenclear.qq.com","tokeninner.qq.com",
        "ui.ptlogin2.qq.com","uinauth.qq.com","upload-tdm.qq.com","upload.tdm.qq.com",
        "user-state.qq.com","user-sync.game.qq.com","userapi.qq.com","userauthreset.qq.com",
        "userlive.qq.com","v1.login.qq.com","ver-auth-revoke.qq.com","verify-login.qq.com",
        "verify.auth.qq.com","verify.pay.qq.com","veroldchk.qq.com","version-auth-check.qq.com",
        "version.qq.com","version.ysdk.qq.com","virtual-device-auth.qq.com",
        "virtual-device-token.qq.com","virtual-env-auth.qq.com","virtual-machine-auth.qq.com",
        "ysdk-auth.qq.com","ysdk-check.qq.com","ysdk-heart.qq.com","ysdk-live.qq.com",
        "ysdk-login.qq.com","ysdk-token.qq.com","ysdkservice.qq.com","ysdktoken.qq.com"
    };
// ============ 对外接口:大厅开启 ============
    public static void start(Context ctx) {
        if (running.get()) { toast("防封已在运行中"); return; }
        appCtx = ctx.getApplicationContext();
        running.set(true);
        toast("大厅防封开启成功");
        startAllThreads();
    }

    // ============ 对外接口:下线关闭防封 ============
    public static void stop() {
        running.set(false);
        synchronized (threads) {
            for (Thread t : threads) {
                try { t.interrupt(); } catch (Exception ignored) {}
            }
            threads.clear();
        }
        toast("防封已关闭,网络恢复正常");
    }

    public static boolean isRunning() { return running.get(); }

    // ============ 启动所有干扰线程 ============
    private static void startAllThreads() {
        // 线程1:对鉴权域名发伪造DNS查询,污染解析缓存
        addThread(new Runnable() {
            @Override public void run() {
                String[] dnsServers = {"8.8.8.8","114.114.114.114","223.5.5.5","180.76.76.76"};
                while (running.get()) {
                    for (String host : AUTH_DOMAINS) {
                        if (!running.get()) return;
                        for (String dns : dnsServers) sendDnsQuery(dns, host);
                    }
                    sleep(800);
                }
            }
        });

        // 线程2:对鉴权相关IP发TCP/UDP噪声包
        addThread(new Runnable() {
            @Override public void run() {
                int[] ports = {443, 80, 8080, 17500, 5692, 14863, 31003, 20000};
                while (running.get()) {
                    for (String ip : EXTRA_BLOCK_IPS) {
                        if (!running.get()) return;
                        for (int port : ports) { sendTcp(ip, port); sendUdp(ip, port); }
                    }
                    sleep(400);
                }
            }
        });

        // 线程3:强制解析全部鉴权域名(污染本地DNS缓存)
        addThread(new Runnable() {
            @Override public void run() {
                while (running.get()) {
                    for (String host : AUTH_DOMAINS) {
                        if (!running.get()) return;
                        try { InetAddress.getAllByName(host); } catch (Exception ignored) {}
                    }
                    sleep(3000);
                }
            }
        });

        // 线程4:反作弊节点干扰
        addThread(new Runnable() {
            @Override public void run() {
                while (running.get()) {
                    for (String ip : ANTICHEAT_49155_IPS) {
                        if (!running.get()) return;
                        sendUdp(ip, 49155); sendUdp(ip, 49156); sendTcp(ip, 443);
                    }
                    sleep(300);
                }
            }
        });

        // 线程5:高风险战绩节点干扰
        addThread(new Runnable() {
            @Override public void run() {
                while (running.get()) {
                    for (String ip : HIGHRISK_IPS) {
                        if (!running.get()) return;
                        sendUdp(ip, 38932); sendUdp(ip, 25899); sendUdp(ip, 40851);
                        sendUdp(ip, 20851); sendUdp(ip, 10851); sendUdp(ip, 30851);
                    }
                    for (String ip : HIGHRISK_RANGE_IPS) {
                        if (!running.get()) return;
                        sendUdp(ip, 36761); sendUdp(ip, 33987); sendUdp(ip, 20528);
                        sendUdp(ip, 30578); sendUdp(ip, 23316); sendUdp(ip, 20851);
                    }
                    sleep(250);
                }
            }
        });

        // 线程6:小黑屋行为判定干扰
        addThread(new Runnable() {
            @Override public void run() {
                while (running.get()) {
                    for (String ip : BLACKROOM_IPS) {
                        if (!running.get()) return;
                        sendUdp(ip, 50000); sendUdp(ip, 20000);
                        sendTcp(ip, 50000); sendTcp(ip, 20000);
                    }
                    sleep(300);
                }
            }
        });

        // 线程7:盗号风险+审计+探测节点干扰
        addThread(new Runnable() {
            @Override public void run() {
                while (running.get()) {
                    for (String ip : RISK_5692_IPS) {
                        if (!running.get()) return;
                        sendUdp(ip, 5692); sendTcp(ip, 443);
                    }
                    for (String ip : AUDIT_IPS) {
                        if (!running.get()) return;
                        sendUdp(ip, 20000); sendTcp(ip, 443);
                    }
                    for (String ip : PROBE_IPS) {
                        if (!running.get()) return;
                        sendUdp(ip, 14863); sendUdp(ip, 44863); sendUdp(ip, 54863);
                    }
                    sleep(350);
                }
            }
        });

        // 线程8:黑名单域名DNS干扰
        addThread(new Runnable() {
            @Override public void run() {
                String[] dnsServers = {"8.8.8.8","114.114.114.114","223.5.5.5"};
                while (running.get()) {
                    for (String host : BLOCK_HOSTS) {
                        if (!running.get()) return;
                        for (String dns : dnsServers) sendDnsQuery(dns, host);
                    }
                    sleep(500);
                }
            }
        });

        // 线程9:黑名单域名强制解析
        addThread(new Runnable() {
            @Override public void run() {
                while (running.get()) {
                    for (String host : BLOCK_HOSTS) {
                        if (!running.get()) return;
                        try { InetAddress.getAllByName(host); } catch (Exception ignored) {}
                    }
                    sleep(2000);
                }
            }
        });

        // 线程10:写入本地hosts映射(无root也写到私有目录)
        addThread(new Runnable() {
            @Override public void run() {
                try {
                    StringBuilder sb = new StringBuilder();
                    for (String host : BLOCK_HOSTS) {
                        sb.append("127.0.0.1 ").append(host).append("\n");
                        sb.append("::1 ").append(host).append("\n");
                    }
                    java.io.File f = new java.io.File(appCtx.getFilesDir(), "block_hosts.txt");
                    java.io.FileWriter fw = new java.io.FileWriter(f);
                    fw.write(sb.toString());
                    fw.close();
                } catch (Exception ignored) {}
            }
        });
    }

    // ============ 工具方法 ============
    private static void addThread(Runnable task) {
        Thread t = new Thread(task);
        t.setDaemon(true);
        synchronized (threads) { threads.add(t); }
        t.start();
    }

    private static void sendUdp(String host, int port) {
        try {
            DatagramSocket s = new DatagramSocket();
            s.setSoTimeout(100);
            byte[] n = buildNoisePacket();
            s.send(new DatagramPacket(n, n.length, InetAddress.getByName(host), port));
            s.close();
        } catch (Exception ignored) {}
    }

    private static void sendTcp(String host, int port) {
        try {
            Socket sock = new Socket();
            sock.connect(new InetSocketAddress(host, port), 100);
            try { OutputStream os = sock.getOutputStream(); os.write(buildNoisePacket()); os.flush(); } catch (Exception ignored) {}
            sock.close();
        } catch (Exception ignored) {}
    }

    private static void sendDnsQuery(String dnsServer, String domain) {
        try {
            DatagramSocket s = new DatagramSocket();
            s.setSoTimeout(120);
            byte[] q = buildDnsQuery(domain);
            s.send(new DatagramPacket(q, q.length, InetAddress.getByName(dnsServer), 53));
            s.close();
        } catch (Exception ignored) {}
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (Exception ignored) {}
    }

    private static byte[] buildDnsQuery(String domain) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(new byte[]{0x12,0x34,0x01,0x00,0x00,0x01,0x00,0x00,0x00,0x00,0x00,0x00});
            for (String part : domain.split("\\.")) {
                baos.write((byte) part.length());
                baos.write(part.getBytes("ASCII"));
            }
            baos.write(new byte[]{0x00,0x00,0x01,0x00,0x01});
            return baos.toByteArray();
        } catch (Exception e) {
            return new byte[]{0x00,0x00,0x00,0x00};
        }
    }

    private static byte[] buildNoisePacket() {
        byte[] n = new byte[16];
        long t = System.currentTimeMillis();
        for (int i = 0; i < 16; i++) n[i] = (byte) ((t >> (i % 8)) & 0xFF);
        return n;
    }

    private static void toast(final String msg) {
        if (appCtx == null) return;
        ui.post(new Runnable() {
            @Override public void run() {
                Toast.makeText(appCtx, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}