package com.xy.pak;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class BlockVpnService extends VpnService {

    public static final String ACTION_START = "com.xy.pak.VPN_START";
    public static final String ACTION_STOP = "com.xy.pak.VPN_STOP";
    private static final String GAME_PKG = "com.tencent.tmgp.pubgmhd";

    private static final AtomicBoolean running = new AtomicBoolean(false);
    private ParcelFileDescriptor vpnInterface;
    private Thread worker;

    public static boolean isRunning() { return running.get(); }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopVpn(); stopSelf(); return START_NOT_STICKY;
        }
        startForegroundCompat();
        startVpn();
        return START_STICKY;
    }

    private void startForegroundCompat() {
        String chId = "vpn_block";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel ch = new NotificationChannel(chId, "\u9632\u5c01\u670d\u52a1",
                NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(ch);
        }
        Notification n = new Notification.Builder(this, chId)
            .setContentTitle("\u5927\u5385\u9632\u5c01\u8fd0\u884c\u4e2d")
            .setContentText("\u6b63\u5728\u62e6\u622a\u5c01\u53f7\u68c0\u6d4b")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build();
        startForeground(1, n);
    }

    private void startVpn() {
        if (running.get()) return;
        try {
            Builder b = new Builder();
            b.setSession("\u5c0f\u6708\u9632\u5c01");
            b.addAddress("10.8.0.2", 32);
            // \u53ea\u8def\u7531\u9ed1\u540d\u5355IP\u6bb5\u8fdbVPN(\u5176\u4f59\u6d41\u91cf\u7cfb\u7edf\u76f4\u8fde,\u6e38\u620f\u6b63\u5e38\u8054\u7f51)
            addBlockRoutes(b);
            // DNS\u4e5f\u8def\u7531\u8fdb\u6765(\u4e3a\u4e86\u57df\u540d\u62e6\u622a),\u7531\u4e8e\u662f10.x\u865a\u62dfDNS,\u4e0d\u5f71\u54cd\u771f\u5b9eDNS
            b.addDnsServer("10.8.0.1");
            b.addRoute("10.8.0.1", 32);
            try { b.addAllowedApplication(GAME_PKG); } catch (Exception e) {}
            b.setMtu(1500);
            vpnInterface = b.establish();
            if (vpnInterface == null) { stopSelf(); return; }
            running.set(true);
            worker = new Thread(new Runnable() {
                @Override public void run() { runLoop(); }
            });
            worker.setDaemon(true);
            worker.start();
        } catch (Exception e) { stopSelf(); }
    }

    private void stopVpn() {
        running.set(false);
        if (worker != null) { worker.interrupt(); worker = null; }
        try { if (vpnInterface != null) { vpnInterface.close(); vpnInterface = null; } } catch (Exception ignored) {}
    }

    @Override
    public void onDestroy() { stopVpn(); super.onDestroy(); }

    private void addBlockRoutes(Builder b) {
        // \u7cbe\u786eIP /32
        for (String ip : BLOCK_IP_ARR) {
            try { b.addRoute(ip, 32); } catch (Exception ignored) {}
        }
        // /24 \u7f51\u6bb5
        for (String s : SUBNET24_ARR) {
            try { b.addRoute(s + ".0", 24); } catch (Exception ignored) {}
        }
        // /16 \u7f51\u6bb5
        for (String s : SUBNET16_ARR) {
            try { b.addRoute(s + ".0.0", 16); } catch (Exception ignored) {}
        }
    }

    private void runLoop() {
        FileInputStream in = new FileInputStream(vpnInterface.getFileDescriptor());
        FileOutputStream out = new FileOutputStream(vpnInterface.getFileDescriptor());
        byte[] buf = new byte[32767];
        while (running.get()) {
            try {
                int len = in.read(buf);
                if (len <= 0) { Thread.sleep(5); continue; }
                handlePacket(buf, len, out);
            } catch (Exception e) { if (!running.get()) break; }
        }
        try { in.close(); out.close(); } catch (Exception ignored) {}
    }

    private void handlePacket(byte[] data, int len, FileOutputStream out) {
        try {
            if (len < 20) return;
            int ipVer = (data[0] >> 4) & 0xF;
            if (ipVer != 4) return;
            int ihl = (data[0] & 0xF) * 4;
            int proto = data[9] & 0xFF;
            String dstIp = (data[16] & 0xFF) + "." + (data[17] & 0xFF) + "."
                         + (data[18] & 0xFF) + "." + (data[19] & 0xFF);

            // DNS\u67e5\u8be2(\u53d1\u5f8010.8.0.1:53)
            if (proto == 17) {
                int dstPort = ((data[ihl + 2] & 0xFF) << 8) | (data[ihl + 3] & 0xFF);
                if (dstPort == 53) {
                    handleDns(data, ihl, len, out);
                    return;
                }
            }
            // \u5176\u4f59\u8fdbVPN\u7684\u90fd\u662f\u9ed1\u540d\u5355IP\u8def\u7531\u7684 -> \u4e22\u5f03(\u62e6\u622a)
            // \u4ec0\u4e48\u90fd\u4e0d\u505a = \u4e22\u5f03
        } catch (Exception ignored) {}
    }

    // \u5904\u7406DNS:\u89e3\u6790\u57df\u540d,\u547d\u4e2d\u9ed1\u540d\u5355\u4e22\u5f03,\u5426\u5219\u8f6c\u53d1\u771f\u5b9eDNS\u5e76\u56de\u5305
    private void handleDns(byte[] data, int ihl, int len, FileOutputStream out) {
        try {
            int udpStart = ihl;
            int dnsStart = udpStart + 8;
            String domain = parseDnsDomain(data, dnsStart, len);
            if (domain != null && isBlockedDomain(domain)) {
                // \u547d\u4e2d -> \u4e0d\u54cd\u5e94(\u4e22\u5f03),\u6e38\u620f\u89e3\u6790\u4e0d\u5230\u5c01\u53f7\u670d\u52a1\u5668
                return;
            }
            // \u672a\u547d\u4e2d -> \u8f6c\u53d1\u5230\u771f\u5b9eDNS,\u62ff\u56de\u7ed3\u679c\u5199\u56deVPN
            int dnsLen = len - dnsStart;
            byte[] dnsQuery = new byte[dnsLen];
            System.arraycopy(data, dnsStart, dnsQuery, 0, dnsLen);

            DatagramSocket sock = new DatagramSocket();
            protect(sock);
            sock.setSoTimeout(3000);
            InetAddress realDns = InetAddress.getByName("114.114.114.114");
            DatagramPacket req = new DatagramPacket(dnsQuery, dnsLen, realDns, 53);
            sock.send(req);
            byte[] resp = new byte[1500];
            DatagramPacket respPkt = new DatagramPacket(resp, resp.length);
            sock.receive(respPkt);
            sock.close();
            // \u7ec4\u88c5\u56deIP\u5305\u5199\u56deVPN(\u4ea4\u6362\u6e90/\u76ee\u6807)
            byte[] reply = buildDnsReply(data, ihl, len, resp, respPkt.getLength());
            if (reply != null) { out.write(reply); out.flush(); }
        } catch (Exception ignored) {}
    }

    // \u6784\u9020DNS\u54cd\u5e94IP\u5305(\u4ea4\u6362\u6e90\u76ee\u6807IP\u548c\u7aef\u53e3)
    private byte[] buildDnsReply(byte[] req, int ihl, int reqLen, byte[] dnsResp, int dnsRespLen) {
        try {
            int udpStart = ihl;
            int totalLen = ihl + 8 + dnsRespLen;
            byte[] pkt = new byte[totalLen];
            // \u590d\u5236IP\u5934
            System.arraycopy(req, 0, pkt, 0, ihl);
            // \u4ea4\u6362\u6e90/\u76ee\u6807IP
            for (int i = 0; i < 4; i++) {
                pkt[12 + i] = req[16 + i]; // src = \u539fdst
                pkt[16 + i] = req[12 + i]; // dst = \u539fsrc
            }
            // IP\u603b\u957f\u5ea6
            pkt[2] = (byte)((totalLen >> 8) & 0xFF);
            pkt[3] = (byte)(totalLen & 0xFF);
            // UDP\u5934:\u4ea4\u6362\u7aef\u53e3
            pkt[udpStart]     = req[udpStart + 2];
            pkt[udpStart + 1] = req[udpStart + 3];
            pkt[udpStart + 2] = req[udpStart];
            pkt[udpStart + 3] = req[udpStart + 1];
            int udpLen = 8 + dnsRespLen;
            pkt[udpStart + 4] = (byte)((udpLen >> 8) & 0xFF);
            pkt[udpStart + 5] = (byte)(udpLen & 0xFF);
            pkt[udpStart + 6] = 0; pkt[udpStart + 7] = 0; // checksum=0(\u53ef\u9009)
            // DNS\u8d1f\u8f7d
            System.arraycopy(dnsResp, 0, pkt, udpStart + 8, dnsRespLen);
            // \u91cd\u7b97IP\u6821\u9a8c\u548c
            pkt[10] = 0; pkt[11] = 0;
            int sum = 0;
            for (int i = 0; i < ihl; i += 2) {
                int word = ((pkt[i] & 0xFF) << 8) | (pkt[i+1] & 0xFF);
                sum += word;
            }
            while ((sum >> 16) != 0) sum = (sum & 0xFFFF) + (sum >> 16);
            sum = ~sum & 0xFFFF;
            pkt[10] = (byte)((sum >> 8) & 0xFF);
            pkt[11] = (byte)(sum & 0xFF);
            return pkt;
        } catch (Exception e) { return null; }
    }

    private boolean isBlockedDomain(String domain) {
        domain = domain.toLowerCase();
        if (BLOCK_DOMAINS.contains(domain)) return true;
        for (String key : DOMAIN_KEYWORDS) { if (domain.contains(key)) return true; }
        return false;
    }

    private String parseDnsDomain(byte[] data, int dnsStart, int len) {
        try {
            int p = dnsStart + 12;
            StringBuilder sb = new StringBuilder();
            while (p < len) {
                int l = data[p] & 0xFF;
                if (l == 0) break;
                if (sb.length() > 0) sb.append('.');
                for (int i = 1; i <= l && p + i < len; i++) sb.append((char)(data[p + i] & 0xFF));
                p += l + 1;
            }
            return sb.toString();
        } catch (Exception e) { return null; }
    }

    // ============ \u9ed1\u540d\u5355\u6570\u636e ============

    private static final HashSet<String> DOMAIN_KEYWORDS = new HashSet<String>();
    static {
        String[] k = {
            "anticheatexpert.com", "gamesafe.qq.com", "tdm.qq.com", "crashsight.qq.com",
            "bugly.qq.com", "rqd.qq.com", "beacon.qq.com", "tgpa",
            "itop.qq.com", "ysdk.qq.com", "msdk.qq.com", "perfsight.qq.com",
            "wetest.qq.com", "faceid", "freeze", "kick",
            "punish", "riskcontrol", "accountrisk", "accstatecheck",
            "realname", "idcheck", "ipban", "tssgw",
            "tssreport", "tdmsec", "tdmreport", "tdmcheck",
            "gameguard", "gameverauth", "netriskold", "netdetect",
            "tencent-anticheat", "tencent-faceid", "tencent-risk", "verify-anticheat"
        };
        for (String x : k) DOMAIN_KEYWORDS.add(x.toLowerCase());
    }

    private static final HashSet<String> BLOCK_DOMAINS = new HashSet<String>();
    static {
        String[] d = {
            "0.qzone.com", "0078.com", "00ond7.cq673.com", "093777.com",
            "10.url.cn", "1004885836.qq.qzone.com", "101.qq.com", "1017305258.qzone.com",
            "1129551422.qzone.com", "117.wefun.vip", "11964948.qzone.com", "1347896.s11.cdntip.com",
            "1352840758.qzone.com", "13745978.s21d-13.faiusrd.com", "15.au.download.windowsupdate.com", "15.tlu.dl.delivery.mp.microsoft.com",
            "16clouds.com", "1716874739.qzone.com", "17500.03boy.cn", "21282481.qzone.com",
            "229322.com", "22j6.com", "3497609377.qzone.com", "349912916.qzone.com",
            "39939583.qzone.com", "3atv.cc", "4216839.qzone.com", "442882.com",
            "4444kk.com", "480893.qzone.com", "5101.vip", "520541.com",
            "5242.com", "52858p.com", "52jg.xyz", "563462729.qq.qzone.com",
            "6.url.cn", "626969.cc", "719uu.com", "7236.com",
            "731525424.qq.qzone.com", "773556637.qq.qzone.com", "778yw.com", "9331.com",
            "96xxoowww.se.6nxx.com.cnyake.qzone.com", "96xxoowww.se.6om.cnyake.qzone.com", "a.com", "a.msdk.qq.com",
            "a.ssl.msdk.qq.com", "aaid.umeng.com", "ac.qq.com", "accauth.qq.com",
            "access1.tpns.sh.tencent.com", "access1.tpns.shs.qq.com", "access1.tpns.tencent.com", "account-auth-lock.qq.com",
            "accountrisk.qq.com", "accstatecheck.qq.com", "activity.acfun.cn", "activity.h-world.com",
            "activity.huazhu.com", "alive.qq.com", "allawntech.com", "android-v1.perfsight.qq.com",
            "android.ac.qq.com", "android.bugly.qq.com", "android.crashsight.qq.com", "android.rqd.qq.com",
            "anti-cheat-token-clear.ysdk.qq.com", "anti-risk-auth.qq.com", "anti.qq.com", "anti2.qq.com",
            "anticheat-auth-kick.qq.com", "anticheat-lock.qq.com", "anticheatexpert.com", "anxiangge.cc",
            "apd-pcdnieghplogin.teg.tencent-cloud.net", "apd-pcdniegwzlogin.teg.tencent-cloud.net", "apd-pcdnvodstat.teg.tencent-cloud.net", "apd-pcdnwxlogin.teg.tencent-cloud.net",
            "apd-pcdnwxnat.teg.tencent-cloud.net", "apd-pcdnwxstat.teg.tencent-cloud.net", "apd-vodp2plogin.teg.tencent-cloud.net", "apd-vodp2pnat.teg.tencent-cloud.net",
            "apd2.payba.cn", "api.tdm.qq.com", "api.translator.qq.com", "api.xiequ.cn",
            "api.xunyou.mobi", "api.ysdk.qq.com", "apm.wetest.qq.com", "app.dm530.net",
            "apple.qqrrieg.qq.com", "appsupport.qq.com", "aqq.mrgame.qq.com", "area-token-reset.qq.com",
            "astrategy.beacon.qq.com", "auth-backstage-reset.qq.com", "auth-cache-clear.qq.com", "auth-config.qq.com",
            "auth-data.tdm.qq.com", "auth-error-report-clean.qq.com", "auth-heartbeat.ysdk.qq.com", "auth-inner-kill.qq.com",
            "auth-inner-refresh.ysdk.qq.com", "auth-keepalive.qq.com", "auth-log-upload.qq.com", "auth-log.qq.com",
            "auth-refresh.qq.com", "auth-refresh.ysdk.qq.com", "auth-risk-revoke.qq.com", "auth-rule.qq.com",
            "auth-safe-kick.qq.com", "auth-scan.game.qq.com", "auth-scan.ysdk.qq.com", "auth-session-kill.ysdk.qq.com",
            "auth-session-reset.qq.com", "auth-status-push.qq.com", "auth-ticket.qq.com", "auth-token-sync.gameqq.com",
            "auth-version-limit.qq.com", "auth.game.qq.com", "authflush.qq.com", "authinner.qq.com",
            "authkeep.qq.com", "authsync.qq.com", "auto-dd.myapp.com", "autopatchcn.bhsr.com",
            "av.jdav01.xyz", "avavav.xnfxxx.xyz", "b.c2r.ts.cdn.office.net", "b.msdk.qq.com",
            "backcheck.qq.com", "backend-token-manage.qq.com", "backup-auth-save.qq.com", "backup-token-destroy.qq.com",
            "baomathree.mqsng.qq.com", "bbs.hycdn.cn", "bbs.mumayi.net", "beat.qq.com",
            "bh3.com", "bkcommdata.v.qq.com", "bkhlslive-ty-cdn.ysp.cctv.cn", "browserapi.micloud.xiaomi.net",
            "btrace.qq.com", "bufferfly.mqsng.qq.com", "bullet.video.qq.com", "bygj.com",
            "c.msdk.qq.com", "cache.tv.qq.com", "capi.voice.gcloud.qq.com", "catocr.com",
            "cbistorge.ubtrobot.com", "cdn-cg.minyea.com", "cdn.anticheatexpert.com", "cdn.chapangzhan.com",
            "cdn.helper.qq.com", "cdn.read.html5.qq.com", "cdn.wetest.qq.com", "cdn.wetest.qq.com.sched.legopic2.tdnsv6.com",
            "cdn.zt.361757.com", "cfg-update.qq.com", "cgi.connect.qq.com", "chapangzhan.com",
            "check-login-state.qq.com", "check.device.qq.com", "check.login.qq.com", "chuhaoshu.com",
            "cimciot.com", "cjm.broker", "cjm.broker.tplay.qq.com", "client.100520.com",
            "cloud-env-auth-kill.qq.com", "cloudctrl.gcloud.qq.com", "cloudctrl.gcloudsdk.com", "cmdw.top",
            "cmshow.qq.com", "cn.voice.gcloudcs.com", "cod.wefun.vip", "coding.qq.com",
            "codm-league.tga.qq.com", "com.ycujizz.www.qzone.com", "comic.qq.com", "community.myapp.com",
            "config.game.qq.com", "conn-service-cn-05.allawntech.com", "connlive.qq.com", "consolev2.gcloud.qq.com",
            "control.mna.qq.com", "control.mocmna.qq.com", "count.qq.com", "crash-auth-log.qq.com",
            "crash.ysdk.qq.com", "crashreport.qq.com", "cross-login.qq.com", "cross-net-auth-kick.qq.com",
            "cross-session-auth.qq.com", "cs.mainconn.gamesafe.qq.com", "cs.mbgame.anticheatexpert.com", "cs.mbgame.gamesafe.qq.com",
            "cs.wefun.vip", "cschannel.anticheatexpert.com", "ctldl.windowsupdate.com", "d3g.qq.com",
            "daoju.qq.com", "data-auth.tdm.qq.com", "dc.it168.com", "dd9.app",
            "default.tdatamaster.com", "devcheck.qq.com", "devcheck.ysdk.qq.com", "device-auth-token.qq.com",
            "device-auth.qq.com", "device-bind-auth.qq.com", "device-bind.qq.com", "device-check.ysdk.qq.com",
            "device-env-auth.qq.com", "device-env-check.qq.com", "device-lock.qq.com", "device-root-check.qq.com",
            "device-sn-auth.qq.com", "device-verify.ysdk.qq.com", "devicereport.qq.com", "df.com",
            "dictweb.translator.qq.com", "dl.app.qq.com", "dl.qidian.com", "dl.url.cn",
            "dl1.gj.qq.com", "dldir1.akm.qq.com", "dldir1.legao.sched.dcloudstc.com", "dldir1.legodlied1.sched.dcloudstc.com",
            "dldir1.qq.com", "dldir1.tc.qq.com", "dldir1.tcdn.qq.com", "dldir1v6.qq.com",
            "dldir1v6.qq.com.legodlied1-dk.sched.dcloudstc.com", "dldir3.qq.com", "dlied1.qq.com.sched.dlied1.tdnsv5.com", "dlied1.qq.com.tcdn.qq.com",
            "dlied4.myapp.com", "dlied4.myapp.tc.qq.com", "dlied5.akm.qq.com", "dlied5.myapp.com",
            "dlied5.myapp.tc.qq.com", "dlied5.qq.com", "dlied6.myapp.com", "dlied6.myapp.tc.qq.com",
            "dlied6.tcdn.qq.com", "dlied7.myapp.com", "dlied9.myapp.com", "dnf.qq.com",
            "dns.game.qq.com", "dns.heipingjingying.xyz", "dns.wangzherongyaodingzhi.top", "dns.ysdk.qq.com",
            "doctor.gcloud.tencent.com", "double-token-verify.qq.com", "douyu.com", "down.anticheatexpert.com",
            "down.anticheatexpert.com.wsdvs.com", "down.anticheatexpert.com.wsdvs.com.chnc.cloudcsp.com", "down.dm530.net", "down.ggcj.com",
            "down.qq.com", "down.qq.com.legodlied1.sched.dcloudstc.com", "download-wnjp.qcplay.com", "drone.xunyou.mobi",
            "ds1010.ymzx.qq.com", "dyesuntech.com", "eagle.mqbcsng.qq.com", "easytomessage.com",
            "ee1.app", "emu-device-auth-reject.qq.com", "env-auth-expire.qq.com", "env-auth-reject.qq.com",
            "env-check.game.qq.com", "env-check.ysdk.qq.com", "env-key-check.qq.com", "env-risk-auth.qq.com",
            "env-token-ban.qq.com", "env-verify.ysdk.qq.com", "env-version-auth.qq.com", "envauth.tdm.qq.com",
            "envverify.qq.com", "equip-auth-check.qq.com", "equipcheck.qq.com", "err-auth-report.qq.com",
            "err-log-auth.qq.com", "errreportauth.qq.com", "event.pull.hebtv.com", "expire-token.qq.com",
            "extendauth.qq.com", "ezone09.114ic.com", "f.totope.com", "f1eb10ae-1d82-4c6a-b4dd-bd9525520956.com",
            "faascjm.native.qq.com", "face-auth-check.qq.com", "faceid-lock.qq.com", "faceid.qcloud.com",
            "faceid.qq.com", "faceid.tencentcloudapi.com", "faceid2.qq.com", "faceid3.qq.com",
            "faceid4.qq.com", "fast-auth-reset.qq.com", "feedbackcenter.cdn.qq.com", "file.ippzone.com",
            "film.qq.com", "finger.qq.com", "flvlive-ty-cdn.ysp.cctv.cn", "freeze.qq.com",
            "freeze2.qq.com", "freeze3.qq.com", "galleryapi.micloud.xiaomi.net", "game-auth-kick-inner.qq.com",
            "game-auth-kick.qq.com", "game-auth-monitor.qq.com", "game-auth.qq.com", "game-auth.ysdk.qq.com",
            "game-login-expel.qq.com", "game-login-sync.qq.com", "game-safe-auth-out.qq.com", "game.bls.mdt.qq.com",
            "game.eve.mdt.qq.com", "game.str.mdt.qq.com", "gameguard.qq.com", "gamesafe-auth-clear.qq.com",
            "gamesafe-freeze.qq.com", "gamesafe-inner-auth.qq.com", "gameverauth.qq.com", "godlied4.myapp.com",
            "gp-auth.qq.com", "gp-livecheck.qq.com", "gp-sync.qq.com", "gp-token.qq.com",
            "gpcloud.tgpa.qq.com", "graph.qq.com", "guid.payba.cn", "guid.tpns.qq.com",
            "guid.tpns.sh.tencent.com", "guid.tpns.tencent.com", "gxh.vip.qq.com", "h.trace.qq.com",
            "h5.bbs.17500.cn", "h5.dexunzhenggu.cn", "h5.huanle.qq.com.cloud.tc.qq.com", "h5.xueersi.com",
            "hadjy.maitix.com", "haoportal.huazhu.com", "hardware-token-limit.qq.com", "hb.game.qq.com",
            "hb.url.cn", "hb.ysdk.qq.com", "hc.tdm.qq.com", "hc1.tdm.qq.com",
            "hc2.tdm.qq.com", "hc3.tdm.qq.com", "hc4.tdm.qq.com", "hcdnw101.gslb.c.cdnhwc2.com",
            "he8nvbq7.dayugslb.com", "health-freeze.qq.com", "health.qq.com", "health2.qq.com",
            "health3.qq.com", "heartbeat-auth.qq.com", "heartbeat.game.qq.com", "heping-android.crashsight.qq.com",
            "heping-crash.qq.com", "hh8899.com", "hippy.imtt.qq.com", "hippy.ysdk.qq.com",
            "hlslive-ty-cdn-test.ysp.cctv.cn", "hnjyf.wxhxp.cn", "hpjy-op.tga.qq.com", "hpjy.itop.qq.com",
            "hpyj.itop.qq.com", "hs.changhong.com", "httpdns.browser.miui.com", "httpdns.cnuer.cn",
            "httpdns.game.qq.com", "httpdns.qq.com", "httpdns.ysdk.qq.com", "huiyang-p2-cloud.itouchtv.cn",
            "hungarian.cri.cn", "hxdcloud.com", "hxsj.qq.com", "hy.cfm.qq.com.cloud.tc.qq.com",
            "hycdn.cfmh5.qq.com", "i.t.cn", "i1.go2yd.com", "ib11.go2yd.com",
            "idcconfig.gcloudsdk.com", "idcheck.qq.com", "idcheck2.qq.com", "identity.tdm.qq.com",
            "ieventlog.beacon.qq.com", "iip138.com", "iips.speed.qq.com", "image.cqsj.qq.com",
            "image.cqtxsy.qq.com", "image.hjol.qq.com", "image.jx3m.qq.com", "image.kok.qq.com",
            "image.ppjbr.qq.com", "image.qqchess.qq.com", "image.smoba.qq.com.cloud.tc.qq.com", "image.uc.cn",
            "image.vxd.qq.com", "image93.360doc.cn", "img.xuannaer.com", "img3.sobot.com",
            "img4.2345.com", "imgwx2.2345.com", "imtt.dd.qq.com", "infinitecdn.m.qq.com",
            "infopic.url.cn", "inner-auth-kick.qq.com", "inner-token.qq.com", "ins-0xt2bis9.ias.tencent-cloud.net",
            "ins-2ybret5v.ias.tencent-cloud.net", "ins-5776sx9h.ias.tencent-cloud.net", "ins-gk5vby51.ias.tencent-cloud.net", "ins-x9e4tvue.ias.tencent-cloud.net",
            "ins-ydl2zsx4.ias.tencent-cloud.net", "ins-zfnwnd8h.ias.tencent-cloud.net", "integralapi.webapi.aedu.cn", "intl.console.gcloud.tencent.com",
            "intl.gcloud.tencent.com", "iosqqdata.ab.qq.com", "ipban.qq.com", "iphone.ac.qq.com",
            "ipv6.cn.voice.gcloudcs.com", "ipv6.mainconn.anticheatexpert.com", "ipv6.mainconn.gamesafe.qq.com", "ipv6.tpns.qq.com",
            "isee.weishi.qq.com", "istrategy.beacon.qq.com", "itea-cdn.qq.com", "iwan.qq.com",
            "jcd868.114ic.com", "jiankong.com", "jisuanke.com", "js-live-screenshot.gitv.tv",
            "js.data.auto.qq.com", "jsonatm.broker.tplay.qq.com", "jsqmt.qq.com", "k.xunlei.com",
            "k8yy.com", "keep-alive-auth.qq.com", "keepalive.qq.com", "kg.qq.com",
            "kick-auth-internal.qq.com", "kick-data-upload.qq.com", "kick-policy.qq.com", "kick.qq.com",
            "kick2.qq.com", "kicklock.qq.com", "king.myapp.com", "kjh.55128.cn",
            "kofd.qq.com", "l.cztvcloud.com", "lede.com", "legao.tc.qq.com",
            "legao.tcdn.qq.com", "lfgdw.cn", "license-auth-check.qq.com", "license-check.qq.com",
            "limit-auth-check.qq.com", "limit-auth-clear.qq.com", "limit-check.qq.com", "limit-token-revoke.qq.com",
            "live-gitv-gs-yh.189smarthome.com", "live.5club.cctv.cn", "liveauth.qq.com", "livedetect.qq.com",
            "livehealth.qq.com", "livestate.qq.com", "log.95xiu.com", "log.pg.qq.com",
            "log.tpns.sh.tencent.com", "logauth.qq.com", "login-anomaly-detect.qq.com", "login-auth-cancel.qq.com",
            "login-auth-clear.qq.com", "login-auth-restore.qq.com", "login-auth-revoke.qq.com", "login-jump-auth.qq.com",
            "login-kick-auth.qq.com", "login-multi-kick.qq.com", "login-online-clear.qq.com", "login-protect-auth.qq.com",
            "login-reset.qq.com", "login-risk-control.qq.com", "login-state-clear.qq.com", "login-throttle.qq.com",
            "login-token-auth.qq.com", "login-token-cancel.qq.com", "login-token-renew.qq.com", "login-verify-auth.qq.com",
            "login.ysdk.qq.com", "loginauthinner.qq.com", "loginexpire.ysdk.qq.com", "loginkick.qq.com",
            "loginrefresh.qq.com", "loginstate.ysdk.qq.com", "logtdm.qq.com", "lol.qq.com",
            "lolm.qq.com", "longtokenmonitor.qq.com", "love.qq.com", "lrsm.urlsec.qq.com",
            "m.4399.cn", "m.52tt.com", "m.ac.qq.com", "m.dm530.net",
            "m.film.qq.com", "map.wap.qq.com", "mat1.qq.com", "mb.yidianzixun.com",
            "mh.whypay.top", "mia.payba.cn", "mid.apd-vodp2plogin.teg.tencent-cloud.net", "mig.str.mdt.qq.com",
            "milo.qq.com", "minigame.qq.com", "monitor.uu.qq.com", "mp.cc",
            "msdk-auth.qq.com", "msdk-token.qq.com", "msdk-verify.qq.com", "mspeed-op.tga.qq.com",
            "mtt.str.mdt.qq.com", "multi-login-check.qq.com", "net-auth.qq.com", "net-check-auth.qq.com",
            "net-environment-token.qq.com", "netdetect.qq.com", "netriskold.qq.com", "network-jump-auth-clear.qq.com",
            "network-risk-auth.qq.com", "new-auth-sync.ysdk.qq.com", "new-otheve.play.aiseet.atianqi.com", "new-otheve.play.t002.ottcn.com",
            "nggproxy.3g.qq.com", "nj.cschannel.anticheatexpert.com", "nj.payba.cn", "njlfskjc.com",
            "noteapi.micloud.xiaomi.net", "notice-freeze.qq.com", "oauth.qq.com", "offline.gtimg.cn",
            "offlinecheck.qq.com", "old-version-auth-expire.qq.com", "oldheart.qq.com", "oldpassport.qq.com",
            "oldticket.qq.com", "online-auth-beat.qq.com", "online-auth-heartbeat.qq.com", "online-beat.qq.com",
            "onlinestat.qq.com", "openmobile.qq.com", "opensdk.qq.com", "ops.gp.qq.com",
            "oss4liview.moji.com", "osscdn.xuntongwx.com", "oth.bls.mdt.qq.com", "oth.eve.mdt.qq.com",
            "oth.str.mdt.qq.com", "oxy.imbceon.com", "p.wchunh.top", "passport.ysdk.qq.com",
            "passport.zj.qq.com", "passport2.aedu.cn", "pay.qq.com", "payments.qq.com",
            "pc.52tt.com", "pcg.xmwb.qq.com", "pcgame.myapp.com", "pdc.micloud.xiaomi.net",
            "persisttokencheck.qq.com", "phonecallapi.micloud.xiaomi.net", "phoneverify.qq.com", "pic.epoint.com.cn",
            "ping.game.qq.com", "ping.ysdk.qq.com", "pingma.qq.com", "pm.myapp.com",
            "portal.xunyou.mobi", "post.mp.qq.com", "pp.myapp.com", "pp141.com",
            "proc-check.qq.com", "profile.z.qingting.fm", "prolongtoken.qq.com", "province-auth-restrict.qq.com",
            "puap.qpic.cn", "puep.qpic.cn", "punish-auth-cancel.qq.com", "punish-token-cancel.qq.com",
            "punish.qq.com", "push.game.qq.com", "push.tpns.qq.com", "push.ysdk.qq.com",
            "pvp.qq.com", "q.unipay.qq.com", "qd.myapp.com", "qos.gcloud.qq.com",
            "qosidc.gcloud.qq.com", "qosidc.gcloudsdk.com", "qq1951339527.qzone.com", "qqauth.qq.com",
            "qqpublic.qpic.cn", "qqtj666.com", "qqtuan.qq.com", "qqvip-web.cdn-go.cn",
            "quick-login-kick.qq.com", "quick-session-out.ysdk.qq.com", "qyhtech.com", "qzact.qzone.qq.com",
            "qzone.qq.com", "r.release.qq.com", "rdelivery.qq.com", "rdt.tfogc.com",
            "realname-auth-check.qq.com", "realname.qq.com", "realname2.qq.com", "receiver.tdm.qq.com",
            "recheck-login-auth.qq.com", "refresh-token.game.qq.com", "refresh.game.qq.com", "region-auth-kick.ysdk.qq.com",
            "region-auth-limit.qq.com", "remote-auth-control.qq.com", "renew-token.qq.com", "renew.game.qq.com",
            "renew.ysdk.qq.com", "report-auth.game.qq.com", "report.game.qq.com", "report.tga.qq.com",
            "res.wx.qq.com", "reset-login-state.qq.com", "resstatic.servicewechat.com", "resstatic.servicewechat.tc.qq.com",
            "risk-auth-forbid.qq.com", "risk-auth-reset.qq.com", "risk-check.ysdk.qq.com", "risk-verify.ysdk.qq.com",
            "riskcontrol.qq.com", "rmonitor.qq.com", "root-auth-reject.qq.com", "root-detect-auth.qq.com",
            "root-env-auth-ban.qq.com", "router.tdm.qq.com", "rpt.qq.com", "rqd-v6.ias.tencent-cloud.net",
            "rqd-v6.sparta.mig.tencent-cloud.net", "rqd.ias.tencent-cloud.net", "rqd.sparta.mig.tencent-cloud.net", "rqd.uu.qq.com",
            "rygiene.com", "s.dd.myapp.com", "s.pc.qq.com", "s.url.cn",
            "s1.url.cn", "s3m4.fenxi.com", "safe-auth.qq.com", "safe-check.qq.com",
            "safe-tdm.qq.com", "safe-token-clear.qq.com", "safe.mt2.cn", "sales.549it.com",
            "savelogincheck.qq.com", "scanlogin.qq.com", "sched.dma.tdnsdl1.cn", "sdk-auth-forcedown.qq.com",
            "sdk-heart.qq.com", "sdk-online-heart.qq.com", "sdk.tdm.qq.com", "sdklogin.qq.com",
            "sdkping.qq.com", "sduwl.net", "se.5nxx.coomwww.leqi.infovip.qzone.com", "sec-live.qq.com",
            "sec.qq.com", "secpassport.qq.com", "session-expire-check.qq.com", "session-heartbeat.qq.com",
            "session-kick.qq.com", "session-refresh-auth.qq.com", "session-timeout-check.qq.com", "sesskill.ysdk.qq.com",
            "sgame.qq.com", "shopthree.mqsng.qq.com", "shorttoken.qq.com", "shuwangtc.com",
            "sjb.qlwb.com.cn", "soft.imtt.qq.com", "softimtt.myapp.com", "spam.qq.com",
            "speedm-team.tga.qq.com", "speedm.qq.com", "speedtest.qq.com", "sq.03boy.cn",
            "sq47.cg.qq.com", "sq7.pg.qq.com", "sq9.pg.qq.com", "sqdd.myapp.com",
            "ssl-cdn.static.browser.mi-img.com", "ssl.login.qq.com", "ssl.zc.qq.com", "sso-refresh.qq.com",
            "sso-ticket.qq.com", "sso.qq.com", "stat-login.qq.com", "stat.game.qq.com",
            "stat.microvirt.com", "stat.tpns.sh.tencent.com", "stat.ysdk.qq.com", "state-sync.qq.com",
            "statelogout.qq.com", "static-res.qq.com", "static.91jkys.com", "static.res.qq.com",
            "static.wecity.qq.com", "stattdm.qq.com", "sub-sdk-token-out.qq.com", "sync-state.ysdk.qq.com",
            "t.dstrategy.myapp.com", "t49cc.com", "task.zhubajie.com", "tbs.imtt.qq.com",
            "tdm-auth-forced-logout.qq.com", "tdm-local-auth-reset.qq.com", "tdm.qq.com", "tdmcheck.qq.com",
            "tdmreport.qq.com", "tdmsec.qq.com", "tdmstat.qq.com", "tech.tom.com",
            "tencent-anticheat.qq.com", "tencent-faceid.qq.com", "tencent-risk.qq.com", "test.mhzx.qq.com",
            "tgpa.imtmp.net", "tgpa.itop.qq.com", "tgpa.qq.com", "tgpa.tencent.com",
            "ticket-auth.qq.com", "ticket.game.qq.com", "tlu.dl.delivery.mp.microsoft.com.cdn.dnsv1.com", "token-anomaly-check.qq.com",
            "token-auth-store.qq.com", "token-cache-clear.qq.com", "token-detect-auth.qq.com", "token-expire-kick.qq.com",
            "token-expire.qq.com", "token-faceid.qq.com", "token-finger.qq.com", "token-keepalive.qq.com",
            "token-rotation-check.qq.com", "token-sync-kill.qq.com", "tools.payba.cn", "tools.tmga.qq.com",
            "trace.qq.com", "ts.qq.com", "tssgw.qq.com", "tssreport.qq.com",
            "tv.pull.hebtv.com", "ulogs.umeng.com", "ulogs.umengcloud.com", "upage.imtt.qq.com",
            "update1.dlied.tc.qq.com", "uz95.v.bsclink.cn", "uz95.v.trpcdn.net", "v2-web.delicloud.com",
            "v5.douyinvod.com", "vatplat.gtimg.cn", "vc.qpic.cn", "verify-anticheat.qq.com",
            "verify-data.qq.com", "verifyserver.qq.com", "verifytoken.qq.com", "video.qq.com",
            "videotranspond.3g.qq.com", "videotranspondplus.3g.qq.com", "vrgt.tdm.qq.com", "vurl.qq.com",
            "wa.qq.com", "wapword5.360doc.cn", "web.gcloud.qq.com", "web.gcloud.tencent.com",
            "web2.cgi.weiyun.com", "webapi.amap.com", "webcdn.m.qq.com", "webpage.qidian.qq.com",
            "wecar.myapp.com", "wecard.tenpay.com", "wgeo.weather.com.cn", "wifiapi.micloud.xiaomi.net",
            "wj.mgtv.com", "wsdcheck.qq.com", "wuji.video.qq.com", "wuyou.ahduobang.com",
            "ww.53yyy.com.www.69.4usey.qzone.com", "www-yukusoft-com.b0.aicdn.com", "www.10062531.qzone.com", "www.1317708729.qzone.com",
            "www.168cp.com", "www.16clouds.com", "www.17ng.com", "www.276.com",
            "www.3123.df.com", "www.33674.com", "www.34h.com", "www.393444.com",
            "www.4399.com", "www.5181688.com", "www.51kk.com", "www.51yc.cn",
            "www.544.hk", "www.5544433.com", "www.655se.com-www.qzone.com", "www.722.me",
            "www.7336.com", "www.8711.df.com", "www.8888.ye.com115252sswww.qzone.com", "www.9088.com.cn",
            "www.91ss.com", "www.91ww.com", "www.92922.com", "www.9377.com",
            "www.956172093.qzone.com", "www.976181.com", "www.aedu.cn", "www.aiqiyi.com",
            "www.av599.com", "www.b2b400.com", "www.blgsb.com", "www.cmdw.top",
            "www.cxbio199.com", "www.df.com", "www.dirock.cn", "www.dm530.cc",
            "www.dm530.com", "www.dm530.net", "www.doglovepig.qzone.com", "www.epwk.com",
            "www.h4y3.com", "www.ikeazue.com", "www.it168.com", "www.jjj.comwww.jjj.com",
            "www.liebaowh.cn", "www.luoyanghcgm.com", "www.m.dm530.com", "www.maomp.com",
            "www.meinu.com", "www.mfcclub.net", "www.miduactivity.com", "www.nenbi.infowww.qiuse.inwww.qzone.com",
            "www.rzhushou.com", "www.s21v.faiusr.com", "www.sb888.qq.qzone.com", "www.sdzccpa.com",
            "www.shikexiu.com", "www.taimei.com", "www.tom.com", "www.ttxnn.com",
            "www.tv9090.com", "www.ulinix.cn", "www.wangxinghu.com", "www.wg999.com",
            "www.winyingwave.com", "www.xcxbk.com", "www.xcxtlq.com", "www.xigua365.com",
            "www.xunyou.mobi", "www.xushang114.com", "www.yaoka.com", "www.ycujizz.com.qzone.com",
            "www.yes321.com", "www.yesky.com", "www.youlu.com", "www.yukusoft.com",
            "www.zhaost.com", "www.zhongrou.net", "www.ztlsj.com", "www.zyrisen.com",
            "www294949.com", "www68uu.c0mwww.qzone.com", "www865.df.com", "wwwbb4444.com",
            "wx1.pg.qq.com", "wx57.cg.qq.com", "wxlobby.pg.qq.com", "wxsnsdy.tc.qq.com",
            "wzdmfm.com", "xbyszx.com.a.bdydns.com", "xggd&flx&gzo", "xj.weather.com.cn",
            "xmodhub.cn", "xnxx-cdn.com", "xp.qpic.cn", "xsddz1.114ic.com",
            "xvideos-cdn.com", "xvideos.91mv.co", "xvideos.comwww679922.com", "xvideos.xxsaobi.xyz",
            "xvideos.zhaofeizi.co", "xvideos.zhaosaozi.co", "xy2.qq.com", "ynfs8.com",
            "youxi.gamecenter.qq.com", "yoyo.qq.com", "ys.mihoyo.com", "ysdk.qq.com",
            "yyb.str.mdt.qq.com", "yyjlove6.qq.qzone.com", "zhi.zhe800.com", "zhuoyou.com",
            "zkres.myzaker.com", "zy.rzhushou.com", "zydz88.114ic.com", "zydz888.114ic.com"
        };
        for (String x : d) BLOCK_DOMAINS.add(x);
    }

    private static final String[] BLOCK_IP_ARR = {
        "1.194.202.243", "101.226.150.243", "101.226.96.217", "101.227.164.167",
        "101.89.42.237", "101.91.21.27", "101.91.23.183", "101.91.33.16",
        "101.91.42.188", "106.55.209.88", "111.174.4.109", "112.60.24.11",
        "112.60.24.81", "113.108.28.224", "113.96.19.173", "117.135.175.106",
        "117.27.241.244", "117.89.177.167", "119.147.15.191", "119.147.15.52",
        "119.147.15.57", "119.45.69.203", "121.11.171.220", "123.139.191.137",
        "124.226.72.35", "124.95.184.107", "125.70.173.132", "139.186.105.126",
        "14.29.101.180", "140.249.64.121", "140.249.64.60", "171.105.187.97",
        "171.105.217.213", "171.105.219.210", "171.105.220.55", "171.105.25.180",
        "171.107.77.45", "171.108.212.63", "171.108.212.68", "171.108.214.55",
        "171.108.223.207", "171.109.104.73", "171.111.157.136", "175.12.127.153",
        "175.12.2.146", "175.12.23.137", "175.24.208.107", "175.6.84.30",
        "175.6.84.70", "180.102.211.116", "180.102.211.18", "180.102.211.42",
        "180.102.211.93", "180.109.156.92", "180.109.171.23", "182.107.81.132",
        "182.254.116.117", "183.192.196.27", "183.60.230.197", "183.60.230.198",
        "183.60.230.201", "218.77.196.39", "222.216.230.182", "222.216.230.73",
        "222.94.109.22", "27.155.112.58", "27.155.112.93", "36.155.132.88",
        "36.155.202.119", "58.216.107.92", "58.49.227.229", "61.151.229.151",
        "81.69.100.220"
    };

    private static final String[] SUBNET24_ARR = {
        "110.72.105", "111.9.74", "113.0.124", "113.59.32",
        "115.236.128", "115.238.189", "116.196.140", "117.135.158",
        "117.143.60", "117.27.241", "118.180.45", "119.147.15",
        "119.188.64", "120.226.82", "122.247.212", "122.247.214",
        "123.180.181", "124.225.141", "124.232.158", "140.249.64",
        "150.139.224", "150.139.240", "171.107.25", "171.107.77",
        "171.109.109", "175.12.121", "180.102.211", "182.40.25",
        "182.90.219", "183.131.42", "183.194.239", "183.247.185",
        "221.204.216", "221.204.217", "221.204.48", "222.216.230",
        "222.94.109", "27.155.112", "42.4.50", "58.212.47",
        "61.189.2"
    };

    private static final String[] SUBNET16_ARR = { "223.83", "223.95", "36.150" };
}
