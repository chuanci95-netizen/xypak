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

// 防封②:Java 无root噪声包拦截(最新965域名版)
public class AntiFreeze2 {

    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static final List<Thread> threads = new ArrayList<Thread>();
    private static Context appCtx;
    private static final Handler ui = new Handler(Looper.getMainLooper());

    // ── 5692盗号风险上报节点 ──
    private static final String[] RISK_5692_IPS = {
        "101.226.96.217", "101.227.164.167", "61.151.229.151",
        "81.69.100.220",  "175.24.208.107",  "101.91.21.27"
    };

    // ── 49155/49156反作弊实时探测节点 ──
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

    // ── 反作弊整段网段前缀 ──
    private static final String[] ANTICHEAT_SEGMENTS = {
        "61.189.2", "116.196.140", "120.226.82", "122.247.214",
        "221.204.216", "221.204.217", "221.204.48", "150.139.224",
        "124.225.141", "182.40.25", "113.59.32", "171.109.109"
    };

    // ── 安全审计/行为矩阵上报 ──
    private static final String[] AUDIT_IPS = {
        "106.55.209.88",  "139.186.105.126", "117.135.175.106",
        "36.155.132.88",  "180.109.156.92",  "183.192.196.27",
        "27.155.112.93"
    };

    // ── 14863/44863/54863异常探测 ──
    private static final String[] PROBE_IPS = {
        "140.249.64.107", "140.249.64.60", "140.249.64.121", "27.155.112.58"
    };

    // ── 高风险战绩异常上报 ──
    private static final String[] HIGHRISK_IPS = {
        "223.83.227.60", "223.83.33.14", "223.95.212.12",
        "58.212.47.187", "58.212.47.232"
    };

    // ── 高风险战绩上报网段 ──
    private static final String[] HIGHRISK_RANGE_IPS = {
        "183.247.185.5",  "183.247.185.10", "183.247.185.11",
        "183.247.185.39", "183.247.185.51", "183.247.185.52",
        "115.236.128.24", "115.236.128.36", "115.236.128.112", "115.236.128.120"
    };

    // ── 小黑屋行为判定 ──
    private static final String[] BLACKROOM_IPS = {
        "119.147.15.52", "119.147.15.57", "119.147.15.191"
    };

    // ── 封号/冻结评估/人脸/处罚/风控节点 ──
    private static final String[] EXTRA_BLOCK_IPS = {
        "117.27.241.244", "171.107.77.45", "222.94.109.22",
        "101.91.42.188",  "101.91.33.16",  "101.91.23.183",
        "101.226.150.243",
        "180.109.171.23", "117.89.177.167", "58.49.227.229",
        "58.216.107.92",  "124.95.184.107", "14.29.101.180",
        "1.194.202.243",  "182.107.81.132",
        "101.89.42.237",  "113.108.28.224",
        "175.6.84.70",    "175.6.84.30",
        "106.55.209.88",  "139.186.105.126", "117.135.175.106",
        "183.192.196.27", "36.155.132.88",  "180.109.156.92",
        "222.216.230.182","222.216.230.73",
        "171.105.219.210","171.105.217.213","171.105.187.97",
        "171.105.220.55", "171.108.212.63", "171.108.212.68",
        "171.108.214.55", "171.108.223.207","182.254.116.117",
        "175.12.127.153", "175.12.23.137",  "175.12.2.146",
        "183.60.230.197", "183.60.230.198", "183.60.230.201",
        "218.77.196.39",  "111.174.4.109",  "124.226.72.35",
        "171.109.104.73",
        "180.102.211.18", "180.102.211.42", "180.102.211.93", "180.102.211.116",
        "36.155.202.119"
    };

    // ── TerSafe风控端口 ──
    private static final int[] TERSAFE_PORTS = {
        5692, 31003, 50000, 20000, 8085, 14863, 54863, 44863, 64863,
        36761, 30851, 33987, 38932, 25899, 20528, 30578, 23316, 23358,
        20851, 32878, 33069, 43260, 24371, 21169, 22115, 22129, 22148,
        50851, 10851, 7370, 40851, 49155, 49156
    };

    // ── 合并去重全部域名 (1040个) ──
    private static final String[] ALL_DOMAINS = {
        "*.rdt.tfogc.com",
        "*.sched.dma.tdnsdl1.cn",
        "*.tgpa.imtmp.net",
        "0.qzone.com",
        "0078.com",
        "00ond7.cq673.com",
        "093777.com",
        "10.url.cn",
        "1004885836.qq.qzone.com",
        "101.qq.com",
        "1017305258.qzone.com",
        "112.60.24.11",
        "112.60.24.81",
        "1129551422.qzone.com",
        "117.wefun.vip",
        "119.45.69.203",
        "11964948.qzone.com",
        "1347896.s11.cdntip.com",
        "1352840758.qzone.com",
        "13745978.s21d-13.faiusrd.com",
        "15.au.download.windowsupdate.com",
        "15.tlu.dl.delivery.mp.microsoft.com",
        "16clouds.com",
        "1716874739.qzone.com",
        "17500.03boy.cn",
        "21282481.qzone.com",
        "229322.com",
        "22j6.com",
        "3497609377.qzone.com",
        "349912916.qzone.com",
        "39939583.qzone.com",
        "3atv.cc",
        "4216839.qzone.com",
        "442882.com",
        "4444kk.com",
        "480893.qzone.com",
        "5101.vip",
        "520541.com",
        "5242.com",
        "52858p.com",
        "52jg.xyz",
        "563462729.qq.qzone.com",
        "6.url.cn",
        "626969.cc",
        "719uu.com",
        "7236.com",
        "731525424.qq.qzone.com",
        "773556637.qq.qzone.com",
        "778yw.com",
        "9331.com",
        "96xxoowww.se.6nxx.com.cnyake.qzone.com",
        "96xxoowww.se.6om.cnyake.qzone.com",
        "a.com",
        "a.msdk.qq.com",
        "a.ssl.msdk.qq.com",
        "aaid.umeng.com",
        "ac.qq.com",
        "accauth.qq.com",
        "access1.tpns.sh.tencent.com",
        "access1.tpns.shs.qq.com",
        "access1.tpns.tencent.com",
        "account-auth-lock.qq.com",
        "accountrisk.qq.com",
        "accstatecheck.qq.com",
        "ace.tencent.com",
        "activity.acfun.cn",
        "activity.h-world.com",
        "activity.huazhu.com",
        "aegis.qq.com",
        "alive.qq.com",
        "allawntech.com",
        "android-v1.perfsight.qq.com",
        "android.ac.qq.com",
        "android.bugly.qq.com",
        "android.crashsight.qq.com",
        "android.rqd.qq.com",
        "anti-cheat-token-clear.ysdk.qq.com",
        "anti-risk-auth.qq.com",
        "anti.qq.com",
        "anti2.qq.com",
        "anticheat-auth-kick.qq.com",
        "anticheat-lock.qq.com",
        "anticheatexpert.com",
        "anxiangge.cc",
        "apd-pcdnieghplogin.teg.tencent-cloud.net",
        "apd-pcdniegwzlogin.teg.tencent-cloud.net",
        "apd-pcdnvodstat.teg.tencent-cloud.net",
        "apd-pcdnwxlogin.teg.tencent-cloud.net",
        "apd-pcdnwxnat.teg.tencent-cloud.net",
        "apd-pcdnwxstat.teg.tencent-cloud.net",
        "apd-vodp2plogin.teg.tencent-cloud.net",
        "apd-vodp2pnat.teg.tencent-cloud.net",
        "apd2.payba.cn",
        "api.tdm.qq.com",
        "api.translator.qq.com",
        "api.xiequ.cn",
        "api.xunyou.mobi",
        "api.ysdk.qq.com",
        "apm.wetest.qq.com",
        "app.dm530.net",
        "apple.qqrrieg.qq.com",
        "appsupport.qq.com",
        "aqq.mrgame.qq.com",
        "area-token-reset.qq.com",
        "astrategy.beacon.qq.com",
        "auth-backstage-reset.qq.com",
        "auth-cache-clear.qq.com",
        "auth-config.qq.com",
        "auth-data.tdm.qq.com",
        "auth-error-report-clean.qq.com",
        "auth-heartbeat.ysdk.qq.com",
        "auth-inner-kill.qq.com",
        "auth-inner-refresh.ysdk.qq.com",
        "auth-keepalive.qq.com",
        "auth-log-upload.qq.com",
        "auth-log.qq.com",
        "auth-refresh.qq.com",
        "auth-refresh.ysdk.qq.com",
        "auth-risk-revoke.qq.com",
        "auth-rule.qq.com",
        "auth-safe-kick.qq.com",
        "auth-scan.game.qq.com",
        "auth-scan.ysdk.qq.com",
        "auth-session-kill.ysdk.qq.com",
        "auth-session-reset.qq.com",
        "auth-status-push.qq.com",
        "auth-ticket.qq.com",
        "auth-token-sync.gameqq.com",
        "auth-version-limit.qq.com",
        "auth.game.qq.com",
        "authflush.qq.com",
        "authinner.qq.com",
        "authkeep.qq.com",
        "authsync.qq.com",
        "auto-dd.myapp.com",
        "autopatchcn.bhsr.com",
        "av.jdav01.xyz",
        "avavav.xnfxxx.xyz",
        "b.c2r.ts.cdn.office.net",
        "b.msdk.qq.com",
        "backcheck.qq.com",
        "backend-token-manage.qq.com",
        "backup-auth-save.qq.com",
        "backup-token-destroy.qq.com",
        "baomathree.mqsng.qq.com",
        "bbs.hycdn.cn",
        "bbs.mumayi.net",
        "beat.qq.com",
        "bh3.com",
        "bkcommdata.v.qq.com",
        "bkhlslive-ty-cdn.ysp.cctv.cn",
        "browserapi.micloud.xiaomi.net",
        "btrace.qq.com",
        "bufferfly.mqsng.qq.com",
        "bullet.video.qq.com",
        "bygj.com",
        "c.msdk.qq.com",
        "cache.tv.qq.com",
        "capi.voice.gcloud.qq.com",
        "catocr.com",
        "cbistorge.ubtrobot.com",
        "cdn-cg.minyea.com",
        "cdn.anticheatexpert.com",
        "cdn.chapangzhan.com",
        "cdn.helper.qq.com",
        "cdn.read.html5.qq.com",
        "cdn.wetest.qq.com",
        "cdn.wetest.qq.com.sched.legopic2.tdnsv6.com",
        "cdn.zt.361757.com",
        "cfg-update.qq.com",
        "cgi.connect.qq.com",
        "chapangzhan.com",
        "check-login-state.qq.com",
        "check.device.qq.com",
        "check.login.qq.com",
        "chuhaoshu.com",
        "cimciot.com",
        "cjm.broker",
        "cjm.broker.tplay.qq.com",
        "client.100520.com",
        "cloud-env-auth-kill.qq.com",
        "cloudctrl.gcloud.qq.com",
        "cloudctrl.gcloudsdk.com",
        "cmdw.top",
        "cmshow.qq.com",
        "cn.voice.gcloudcs.com",
        "cod.wefun.vip",
        "coding.qq.com",
        "codm-league.tga.qq.com",
        "com.ycujizz.www.qzone.com",
        "comic.qq.com",
        "community.myapp.com",
        "config.game.qq.com",
        "conn-service-cn-05.allawntech.com",
        "connlive.qq.com",
        "consolev2.gcloud.qq.com",
        "control.mna.qq.com",
        "control.mocmna.qq.com",
        "count.qq.com",
        "crash-auth-log.qq.com",
        "crash.ysdk.qq.com",
        "crashreport.qq.com",
        "cross-login.qq.com",
        "cross-net-auth-kick.qq.com",
        "cross-session-auth.qq.com",
        "cs.mainconn.gamesafe.qq.com",
        "cs.mbgame.anticheatexpert.com",
        "cs.mbgame.gamesafe.qq.com",
        "cs.wefun.vip",
        "cschannel.anticheatexpert.com",
        "ctldl.windowsupdate.com",
        "d3g.qq.com",
        "daoju.qq.com",
        "data-auth.tdm.qq.com",
        "datamore.qq.com",
        "dc.it168.com",
        "dcl.itop.qq.com",
        "dd9.app",
        "default.tdatamaster.com",
        "devcheck.qq.com",
        "devcheck.ysdk.qq.com",
        "device-auth-token.qq.com",
        "device-auth.qq.com",
        "device-bind-auth.qq.com",
        "device-bind.qq.com",
        "device-check.ysdk.qq.com",
        "device-env-auth.qq.com",
        "device-env-check.qq.com",
        "device-lock.qq.com",
        "device-root-check.qq.com",
        "device-sn-auth.qq.com",
        "device-verify.ysdk.qq.com",
        "devicereport.qq.com",
        "df.com",
        "dictweb.translator.qq.com",
        "dl.app.qq.com",
        "dl.qidian.com",
        "dl.url.cn",
        "dl1.gj.qq.com",
        "dldir1.akm.qq.com",
        "dldir1.legao.sched.dcloudstc.com",
        "dldir1.legodlied1.sched.dcloudstc.com",
        "dldir1.qq.com",
        "dldir1.tc.qq.com",
        "dldir1.tcdn.qq.com",
        "dldir1v6.qq.com",
        "dldir1v6.qq.com.legodlied1-dk.sched.dcloudstc.com",
        "dldir3.qq.com",
        "dlied1.qq.com.sched.dlied1.tdnsv5.com",
        "dlied1.qq.com.tcdn.qq.com",
        "dlied4.myapp.com",
        "dlied4.myapp.tc.qq.com",
        "dlied5.akm.qq.com",
        "dlied5.myapp.com",
        "dlied5.myapp.tc.qq.com",
        "dlied5.qq.com",
        "dlied6.myapp.com",
        "dlied6.myapp.tc.qq.com",
        "dlied6.tcdn.qq.com",
        "dlied7.myapp.com",
        "dlied9.myapp.com",
        "dnf.qq.com",
        "dns.game.qq.com",
        "dns.heipingjingying.xyz",
        "dns.wangzherongyaodingzhi.top",
        "dns.ysdk.qq.com",
        "doctor.gcloud.tencent.com",
        "double-token-verify.qq.com",
        "douyu.com",
        "down.anticheatexpert.com",
        "down.anticheatexpert.com.wsdvs.com",
        "down.anticheatexpert.com.wsdvs.com.chnc.cloudcsp.com",
        "down.dm530.net",
        "down.ggcj.com",
        "down.qq.com",
        "down.qq.com.legodlied1.sched.dcloudstc.com",
        "download-wnjp.qcplay.com",
        "drone.xunyou.mobi",
        "ds1010.ymzx.qq.com",
        "dyesuntech.com",
        "eagle.mqbcsng.qq.com",
        "easytomessage.com",
        "ee1.app",
        "emu-device-auth-reject.qq.com",
        "env-auth-expire.qq.com",
        "env-auth-reject.qq.com",
        "env-check.game.qq.com",
        "env-check.ysdk.qq.com",
        "env-key-check.qq.com",
        "env-risk-auth.qq.com",
        "env-token-ban.qq.com",
        "env-verify.ysdk.qq.com",
        "env-version-auth.qq.com",
        "envauth.tdm.qq.com",
        "envverify.qq.com",
        "equip-auth-check.qq.com",
        "equipcheck.qq.com",
        "err-auth-report.qq.com",
        "err-log-auth.qq.com",
        "errreportauth.qq.com",
        "event.pull.hebtv.com",
        "expire-token.qq.com",
        "extendauth.qq.com",
        "ezone09.114ic.com",
        "f.totope.com",
        "f1eb10ae-1d82-4c6a-b4dd-bd9525520956.com",
        "faascjm.native.qq.com",
        "face-auth-check.qq.com",
        "faceid-lock.qq.com",
        "faceid.qcloud.com",
        "faceid.qq.com",
        "faceid.tencentcloudapi.com",
        "faceid2.qq.com",
        "faceid3.qq.com",
        "faceid4.qq.com",
        "fast-auth-reset.qq.com",
        "feedbackcenter.cdn.qq.com",
        "file.ippzone.com",
        "film.qq.com",
        "finger.qq.com",
        "flvlive-ty-cdn.ysp.cctv.cn",
        "freeze.qq.com",
        "freeze2.qq.com",
        "freeze3.qq.com",
        "galleryapi.micloud.xiaomi.net",
        "game-auth-kick-inner.qq.com",
        "game-auth-kick.qq.com",
        "game-auth-monitor.qq.com",
        "game-auth.qq.com",
        "game-auth.ysdk.qq.com",
        "game-login-expel.qq.com",
        "game-login-sync.qq.com",
        "game-safe-auth-out.qq.com",
        "game.bls.mdt.qq.com",
        "game.eve.mdt.qq.com",
        "game.str.mdt.qq.com",
        "gameguard.qq.com",
        "gamesafe-auth-clear.qq.com",
        "gamesafe-freeze.qq.com",
        "gamesafe-inner-auth.qq.com",
        "gameverauth.qq.com",
        "godlied4.myapp.com",
        "gp-auth.qq.com",
        "gp-livecheck.qq.com",
        "gp-sync.qq.com",
        "gp-token.qq.com",
        "gpcloud.tgpa.qq.com",
        "graph.qq.com",
        "guid.payba.cn",
        "guid.tpns.qq.com",
        "guid.tpns.sh.tencent.com",
        "guid.tpns.tencent.com",
        "gxh.vip.qq.com",
        "h.trace.qq.com",
        "h5.bbs.17500.cn",
        "h5.dexunzhenggu.cn",
        "h5.huanle.qq.com.cloud.tc.qq.com",
        "h5.xueersi.com",
        "hadjy.maitix.com",
        "haoportal.huazhu.com",
        "hardware-token-limit.qq.com",
        "hb.game.qq.com",
        "hb.url.cn",
        "hb.ysdk.qq.com",
        "hc.tdm.qq.com",
        "hc1.tdm.qq.com",
        "hc2.tdm.qq.com",
        "hc3.tdm.qq.com",
        "hc4.tdm.qq.com",
        "hcdnw101.gslb.c.cdnhwc2.com",
        "he8nvbq7.dayugslb.com",
        "health-freeze.qq.com",
        "health.qq.com",
        "health2.qq.com",
        "health3.qq.com",
        "heartbeat-auth.qq.com",
        "heartbeat.game.qq.com",
        "heping-android.crashsight.qq.com",
        "heping-crash.qq.com",
        "hh8899.com",
        "hippy.imtt.qq.com",
        "hippy.ysdk.qq.com",
        "hlslive-ty-cdn-test.ysp.cctv.cn",
        "hnjyf.wxhxp.cn",
        "hpjy-op.tga.qq.com",
        "hpjy.itop.qq.com",
        "hpyj.itop.qq.com",
        "hs.changhong.com",
        "httpdns.browser.miui.com",
        "httpdns.cnuer.cn",
        "httpdns.game.qq.com",
        "httpdns.qq.com",
        "httpdns.ysdk.qq.com",
        "huiyang-p2-cloud.itouchtv.cn",
        "hungarian.cri.cn",
        "hxdcloud.com",
        "hxsj.qq.com",
        "hy.cfm.qq.com.cloud.tc.qq.com",
        "hycdn.cfmh5.qq.com",
        "i.t.cn",
        "i1.go2yd.com",
        "ib11.go2yd.com",
        "idcconfig.gcloudsdk.com",
        "idcheck.qq.com",
        "idcheck2.qq.com",
        "idcloud.qq.com",
        "identity.tdm.qq.com",
        "ieventlog.beacon.qq.com",
        "iip138.com",
        "iips.speed.qq.com",
        "image.cqsj.qq.com",
        "image.cqtxsy.qq.com",
        "image.hjol.qq.com",
        "image.jx3m.qq.com",
        "image.kok.qq.com",
        "image.ppjbr.qq.com",
        "image.qqchess.qq.com",
        "image.smoba.qq.com.cloud.tc.qq.com",
        "image.uc.cn",
        "image.vxd.qq.com",
        "image93.360doc.cn",
        "img.xuannaer.com",
        "img3.sobot.com",
        "img4.2345.com",
        "imgwx2.2345.com",
        "imtt.dd.qq.com",
        "infinitecdn.m.qq.com",
        "infopic.url.cn",
        "inner-auth-kick.qq.com",
        "inner-beat.game.qq.com",
        "inner-session-kill.ysdk.qq.com",
        "inner-token.qq.com",
        "inner-verify.qq.com",
        "innerlogincheck.qq.com",
        "ins-0xt2bis9.ias.tencent-cloud.net",
        "ins-2ybret5v.ias.tencent-cloud.net",
        "ins-5776sx9h.ias.tencent-cloud.net",
        "ins-gk5vby51.ias.tencent-cloud.net",
        "ins-x9e4tvue.ias.tencent-cloud.net",
        "ins-ydl2zsx4.ias.tencent-cloud.net",
        "ins-zfnwnd8h.ias.tencent-cloud.net",
        "inspect-login.game.qq.com",
        "inspect.game.qq.com",
        "integralapi.webapi.aedu.cn",
        "internal-rpc.game.qq.com",
        "intl.console.gcloud.tencent.com",
        "intl.gcloud.tencent.com",
        "iosqqdata.ab.qq.com",
        "ip-segment-auth-limit.qq.com",
        "ipban.qq.com",
        "iphone.ac.qq.com",
        "ipv6.cn.voice.gcloudcs.com",
        "ipv6.mainconn.anticheatexpert.com",
        "ipv6.mainconn.gamesafe.qq.com",
        "ipv6.tpns.qq.com",
        "isee.weishi.qq.com",
        "isolate-auth-lock.qq.com",
        "istrategy.beacon.qq.com",
        "itea-cdn.qq.com",
        "itop.qq.com",
        "iwan.qq.com",
        "jcd868.114ic.com",
        "jiankong.com",
        "jisuanke.com",
        "jkyx.qq.com",
        "joint.tdm.qq.com",
        "js-live-screenshot.gitv.tv",
        "js.data.auto.qq.com",
        "jsonatm.broker.tplay.qq.com",
        "jsqmt.qq.com",
        "jubao.qq.com",
        "k.xunlei.com",
        "k8yy.com",
        "keep-alive-auth.qq.com",
        "keepalive.qq.com",
        "keepalive.ysdk.qq.com",
        "key-verify-hit.qq.com",
        "kg.qq.com",
        "kick-auth-internal.qq.com",
        "kick-data-upload.qq.com",
        "kick-policy.qq.com",
        "kick.qq.com",
        "kick2.qq.com",
        "kicklock.qq.com",
        "kill-session.ysdk.qq.com",
        "king.myapp.com",
        "kjh.55128.cn",
        "kofd.qq.com",
        "l.cztvcloud.com",
        "latest-token-update.qq.com",
        "lede.com",
        "legao.tc.qq.com",
        "legao.tcdn.qq.com",
        "lfgdw.cn",
        "license-auth-check.qq.com",
        "license-check.qq.com",
        "limit-auth-check.qq.com",
        "limit-auth-clear.qq.com",
        "limit-check.qq.com",
        "limit-token-revoke.qq.com",
        "linecheck.qq.com",
        "link-risk.qq.com",
        "linkdetect.qq.com",
        "live-gitv-gs-yh.189smarthome.com",
        "live-user.qq.com",
        "live.5club.cctv.cn",
        "liveauth.qq.com",
        "livecheck.ysdk.qq.com",
        "livedetect.qq.com",
        "livehealth.qq.com",
        "livestate.qq.com",
        "local-auth-backup.qq.com",
        "local-auth-flush.qq.com",
        "local-auth-inner.game.qq.com",
        "local-auth-reset.ysdk.qq.com",
        "local-auth.game.qq.com",
        "local-auth.ysdk.qq.com",
        "local-check.game.qq.com",
        "local-rpc.game.qq.com",
        "local-token-clear.qq.com",
        "local-token.ysdk.qq.com",
        "local-verify.game.qq.com",
        "localauthreport.qq.com",
        "lock2.qq.com",
        "lock3.qq.com",
        "log-clear-auth.qq.com",
        "log.95xiu.com",
        "log.game.qq.com",
        "log.pg.qq.com",
        "log.tdm.qq.com",
        "log.tpns.sh.tencent.com",
        "log.ysdk.qq.com",
        "logauth.qq.com",
        "login-anomaly-detect.qq.com",
        "login-auth-cancel.qq.com",
        "login-auth-clear.qq.com",
        "login-auth-recheck.qq.com",
        "login-auth-restore.qq.com",
        "login-auth-revoke.qq.com",
        "login-auth.qq.com",
        "login-beat.game.qq.com",
        "login-cache-purge.qq.com",
        "login-jump-auth.qq.com",
        "login-kick-auth.qq.com",
        "login-multi-kick.qq.com",
        "login-online-clear.qq.com",
        "login-protect-auth.qq.com",
        "login-reset.qq.com",
        "login-risk-control.qq.com",
        "login-state-clear.qq.com",
        "login-state-reset.qq.com",
        "login-throttle.qq.com",
        "login-token-auth.qq.com",
        "login-token-cancel.qq.com",
        "login-token-clear.qq.com",
        "login-token-renew.qq.com",
        "login-token.qq.com",
        "login-verify-auth.qq.com",
        "login.ysdk.qq.com",
        "loginauthinner.qq.com",
        "loginexpire.ysdk.qq.com",
        "loginkick.qq.com",
        "loginrefresh.qq.com",
        "loginstate.ysdk.qq.com",
        "logtdm.qq.com",
        "lol.qq.com",
        "lolm.qq.com",
        "longtokenmonitor.qq.com",
        "love.qq.com",
        "lrsm.urlsec.qq.com",
        "m.4399.cn",
        "m.52tt.com",
        "m.ac.qq.com",
        "m.dm530.net",
        "m.film.qq.com",
        "map.wap.qq.com",
        "mat1.qq.com",
        "mb.yidianzixun.com",
        "mh.whypay.top",
        "mia.payba.cn",
        "mid.apd-vodp2plogin.teg.tencent-cloud.net",
        "mig.str.mdt.qq.com",
        "milo.qq.com",
        "minigame.qq.com",
        "monitor.uu.qq.com",
        "mp.cc",
        "msdk-auth.qq.com",
        "msdk-token.qq.com",
        "msdk-verify.qq.com",
        "mspeed-op.tga.qq.com",
        "mtt.str.mdt.qq.com",
        "multi-login-check.qq.com",
        "net-auth.qq.com",
        "net-check-auth.qq.com",
        "net-environment-token.qq.com",
        "netdetect.qq.com",
        "netriskold.qq.com",
        "network-jump-auth-clear.qq.com",
        "network-risk-auth.qq.com",
        "new-auth-sync.ysdk.qq.com",
        "new-otheve.play.aiseet.atianqi.com",
        "new-otheve.play.t002.ottcn.com",
        "nggproxy.3g.qq.com",
        "nj.cschannel.anticheatexpert.com",
        "nj.payba.cn",
        "njlfskjc.com",
        "noteapi.micloud.xiaomi.net",
        "notice-freeze.qq.com",
        "oauth.qq.com",
        "offline.gtimg.cn",
        "offlinecheck.qq.com",
        "old-version-auth-expire.qq.com",
        "oldheart.qq.com",
        "oldpassport.qq.com",
        "oldticket.qq.com",
        "online-auth-beat.qq.com",
        "online-auth-heartbeat.qq.com",
        "online-beat.qq.com",
        "onlinestat.qq.com",
        "openmobile.qq.com",
        "opensdk.qq.com",
        "ops.gp.qq.com",
        "oss4liview.moji.com",
        "osscdn.xuntongwx.com",
        "oth.bls.mdt.qq.com",
        "oth.eve.mdt.qq.com",
        "oth.str.mdt.qq.com",
        "oxy.imbceon.com",
        "p.wchunh.top",
        "passport.ysdk.qq.com",
        "passport.zj.qq.com",
        "passport2.aedu.cn",
        "pay.qq.com",
        "payments.qq.com",
        "pc.52tt.com",
        "pcg.xmwb.qq.com",
        "pcgame.myapp.com",
        "pdc.micloud.xiaomi.net",
        "persisttokencheck.qq.com",
        "phonecallapi.micloud.xiaomi.net",
        "phoneverify.qq.com",
        "pic.epoint.com.cn",
        "ping.game.qq.com",
        "ping.ysdk.qq.com",
        "pingma.qq.com",
        "pm.myapp.com",
        "portal.xunyou.mobi",
        "post.mp.qq.com",
        "pp.myapp.com",
        "pp141.com",
        "proc-check.qq.com",
        "profile.z.qingting.fm",
        "prolongtoken.qq.com",
        "province-auth-restrict.qq.com",
        "puap.qpic.cn",
        "puep.qpic.cn",
        "punish-auth-cancel.qq.com",
        "punish-token-cancel.qq.com",
        "punish.qq.com",
        "push.game.qq.com",
        "push.tpns.qq.com",
        "push.ysdk.qq.com",
        "pvp.qq.com",
        "q.unipay.qq.com",
        "qd.myapp.com",
        "qos.gcloud.qq.com",
        "qosidc.gcloud.qq.com",
        "qosidc.gcloudsdk.com",
        "qq1951339527.qzone.com",
        "qqauth.qq.com",
        "qqpublic.qpic.cn",
        "qqtj666.com",
        "qqtuan.qq.com",
        "qqvip-web.cdn-go.cn",
        "quick-login-kick.qq.com",
        "quick-session-out.ysdk.qq.com",
        "qyhtech.com",
        "qzact.qzone.qq.com",
        "qzone.qq.com",
        "r.release.qq.com",
        "rdelivery.qq.com",
        "realname-auth-check.qq.com",
        "realname.qq.com",
        "realname2.qq.com",
        "receiver.tdm.qq.com",
        "recheck-login-auth.qq.com",
        "refresh-token.game.qq.com",
        "refresh.game.qq.com",
        "region-auth-kick.ysdk.qq.com",
        "region-auth-limit.qq.com",
        "remote-auth-control.qq.com",
        "renew-token.qq.com",
        "renew.game.qq.com",
        "renew.ysdk.qq.com",
        "report-auth.game.qq.com",
        "report.game.qq.com",
        "report.tga.qq.com",
        "res.wx.qq.com",
        "reset-login-state.qq.com",
        "resstatic.servicewechat.com",
        "resstatic.servicewechat.tc.qq.com",
        "risk-auth-forbid.qq.com",
        "risk-auth-reset.qq.com",
        "risk-check.ysdk.qq.com",
        "risk-verify.ysdk.qq.com",
        "riskcontrol.qq.com",
        "rmonitor.qq.com",
        "root-auth-reject.qq.com",
        "root-detect-auth.qq.com",
        "root-env-auth-ban.qq.com",
        "router.tdm.qq.com",
        "rpt.qq.com",
        "rqd-v6.ias.tencent-cloud.net",
        "rqd-v6.sparta.mig.tencent-cloud.net",
        "rqd.ias.tencent-cloud.net",
        "rqd.sparta.mig.tencent-cloud.net",
        "rqd.uu.qq.com",
        "rygiene.com",
        "s.dd.myapp.com",
        "s.pc.qq.com",
        "s.url.cn",
        "s1.url.cn",
        "s3m4.fenxi.com",
        "safe-auth.qq.com",
        "safe-check.qq.com",
        "safe-tdm.qq.com",
        "safe-token-clear.qq.com",
        "safe.mt2.cn",
        "safe.qq.com",
        "sales.549it.com",
        "savelogincheck.qq.com",
        "scanlogin.qq.com",
        "sdk-auth-forcedown.qq.com",
        "sdk-heart.qq.com",
        "sdk-online-heart.qq.com",
        "sdk.tdm.qq.com",
        "sdkconfig.itop.qq.com",
        "sdklogin.qq.com",
        "sdkping.qq.com",
        "sduwl.net",
        "se.5nxx.coomwww.leqi.infovip.qzone.com",
        "sec-live.qq.com",
        "sec.qq.com",
        "secpassport.qq.com",
        "session-expire-check.qq.com",
        "session-heartbeat.qq.com",
        "session-kick.qq.com",
        "session-refresh-auth.qq.com",
        "session-timeout-check.qq.com",
        "sesskill.ysdk.qq.com",
        "sgame.qq.com",
        "shopthree.mqsng.qq.com",
        "shorttoken.qq.com",
        "shuwangtc.com",
        "sjb.qlwb.com.cn",
        "sngsuc.qq.com",
        "soft.imtt.qq.com",
        "softimtt.myapp.com",
        "spam.qq.com",
        "speedm-team.tga.qq.com",
        "speedm.qq.com",
        "speedtest.qq.com",
        "sq.03boy.cn",
        "sq47.cg.qq.com",
        "sq7.pg.qq.com",
        "sq9.pg.qq.com",
        "sqdd.myapp.com",
        "ssl-cdn.static.browser.mi-img.com",
        "ssl.login.qq.com",
        "ssl.zc.qq.com",
        "sso-refresh.qq.com",
        "sso-ticket.qq.com",
        "sso.qq.com",
        "stat-login.qq.com",
        "stat.game.qq.com",
        "stat.microvirt.com",
        "stat.tpns.sh.tencent.com",
        "stat.ysdk.qq.com",
        "state-sync.qq.com",
        "statelogout.qq.com",
        "static-res.qq.com",
        "static.91jkys.com",
        "static.res.qq.com",
        "static.wecity.qq.com",
        "stattdm.qq.com",
        "sub-sdk-token-out.qq.com",
        "sync-state.ysdk.qq.com",
        "t.dstrategy.myapp.com",
        "t49cc.com",
        "task.zhubajie.com",
        "tbs.imtt.qq.com",
        "tdir.qq.com",
        "tdm-auth-forced-logout.qq.com",
        "tdm-local-auth-reset.qq.com",
        "tdm-report.qq.com",
        "tdm-upload.game.qq.com",
        "tdm.cdn.tencentgame.com",
        "tdm.qq.com",
        "tdmcheck.qq.com",
        "tdmlogin.qq.com",
        "tdmreport.qq.com",
        "tdmsec.qq.com",
        "tdmstat.game.qq.com",
        "tdmstat.qq.com",
        "tdsdk.qq.com",
        "tech.tom.com",
        "tempauth.qq.com",
        "tencent-anticheat.qq.com",
        "tencent-faceid.qq.com",
        "tencent-risk.qq.com",
        "test.m.tencent.com",
        "test.mhzx.qq.com",
        "tgpa.imtmp.net",
        "tgpa.itop.qq.com",
        "tgpa.qq.com",
        "tgpa.tencent.com",
        "third-party-auth-ban.qq.com",
        "third-party-auth-check.qq.com",
        "third-sdk-auth-clear.qq.com",
        "ticket-auth.qq.com",
        "ticket-refresh.qq.com",
        "ticket-revoke.qq.com",
        "ticket.game.qq.com",
        "tlu.dl.delivery.mp.microsoft.com.cdn.dnsv1.com",
        "tmeta.qq.com",
        "token-anomaly-check.qq.com",
        "token-auth-store.qq.com",
        "token-backup-clean.qq.com",
        "token-cache-clear.qq.com",
        "token-detect-auth.qq.com",
        "token-expire-check.qq.com",
        "token-expire-kick.qq.com",
        "token-expire.qq.com",
        "token-faceid.qq.com",
        "token-finger.qq.com",
        "token-forced-invalidate.qq.com",
        "token-keepalive.qq.com",
        "token-kick-inner.qq.com",
        "token-kick.qq.com",
        "token-lose-check.ysdk.qq.com",
        "token-refresh.qq.com",
        "token-rotation-check.qq.com",
        "token-status-check.qq.com",
        "token-sync-kill.qq.com",
        "token-sync.qq.com",
        "tokenclear.qq.com",
        "tokeninner.qq.com",
        "tools.payba.cn",
        "tools.tmga.qq.com",
        "trace.qq.com",
        "ts.qq.com",
        "tssgw.qq.com",
        "tssn.qq.com",
        "tssreport.qq.com",
        "tv.pull.hebtv.com",
        "ui.ptlogin2.qq.com",
        "uinauth.qq.com",
        "ulogs.umeng.com",
        "ulogs.umengcloud.com",
        "upage.imtt.qq.com",
        "update1.dlied.tc.qq.com",
        "upload-tdm.qq.com",
        "upload.tdm.qq.com",
        "user-state.qq.com",
        "user-sync.game.qq.com",
        "userapi.qq.com",
        "userauthreset.qq.com",
        "userlive.qq.com",
        "uz95.v.bsclink.cn",
        "uz95.v.trpcdn.net",
        "v1.login.qq.com",
        "v2-web.delicloud.com",
        "v5.douyinvod.com",
        "vatplat.gtimg.cn",
        "vc.qpic.cn",
        "ver-auth-revoke.qq.com",
        "verify-anticheat.qq.com",
        "verify-data.qq.com",
        "verify-login.qq.com",
        "verify.auth.qq.com",
        "verify.pay.qq.com",
        "verifyserver.qq.com",
        "verifytoken.qq.com",
        "veroldchk.qq.com",
        "version-auth-check.qq.com",
        "version.qq.com",
        "version.ysdk.qq.com",
        "video.qq.com",
        "videotranspond.3g.qq.com",
        "videotranspondplus.3g.qq.com",
        "virtual-device-auth.qq.com",
        "virtual-device-token.qq.com",
        "virtual-env-auth.qq.com",
        "virtual-machine-auth.qq.com",
        "vrgt.tdm.qq.com",
        "vurl.qq.com",
        "wa.qq.com",
        "wapword5.360doc.cn",
        "web.gcloud.qq.com",
        "web.gcloud.tencent.com",
        "web2.cgi.weiyun.com",
        "webapi.amap.com",
        "webcdn.m.qq.com",
        "webpage.qidian.qq.com",
        "wecar.myapp.com",
        "wecard.tenpay.com",
        "wgeo.weather.com.cn",
        "wifiapi.micloud.xiaomi.net",
        "wj.mgtv.com",
        "wsdcheck.qq.com",
        "wuji.video.qq.com",
        "wuyou.ahduobang.com",
        "ww.53yyy.com.www.69.4usey.qzone.com",
        "www-yukusoft-com.b0.aicdn.com",
        "www.10062531.qzone.com",
        "www.1317708729.qzone.com",
        "www.168cp.com",
        "www.16clouds.com",
        "www.17ng.com",
        "www.276.com",
        "www.3123.df.com",
        "www.33674.com",
        "www.34h.com",
        "www.393444.com",
        "www.4399.com",
        "www.5181688.com",
        "www.51kk.com",
        "www.51yc.cn",
        "www.544.hk",
        "www.5544433.com",
        "www.655se.com-www.qzone.com",
        "www.722.me",
        "www.7336.com",
        "www.8711.df.com",
        "www.8888.ye.com115252sswww.qzone.com",
        "www.9088.com.cn",
        "www.91ss.com",
        "www.91ww.com",
        "www.92922.com",
        "www.9377.com",
        "www.956172093.qzone.com",
        "www.976181.com",
        "www.aedu.cn",
        "www.aiqiyi.com",
        "www.av599.com",
        "www.b2b400.com",
        "www.blgsb.com",
        "www.cmdw.top",
        "www.cxbio199.com",
        "www.df.com",
        "www.dirock.cn",
        "www.dm530.cc",
        "www.dm530.com",
        "www.dm530.net",
        "www.doglovepig.qzone.com",
        "www.epwk.com",
        "www.h4y3.com",
        "www.ikeazue.com",
        "www.it168.com",
        "www.jjj.comwww.jjj.com",
        "www.liebaowh.cn",
        "www.luoyanghcgm.com",
        "www.m.dm530.com",
        "www.maomp.com",
        "www.meinu.com",
        "www.mfcclub.net",
        "www.miduactivity.com",
        "www.nenbi.infowww.qiuse.inwww.qzone.com",
        "www.rzhushou.com",
        "www.s21v.faiusr.com",
        "www.sb888.qq.qzone.com",
        "www.sdzccpa.com",
        "www.shikexiu.com",
        "www.taimei.com",
        "www.tom.com",
        "www.ttxnn.com",
        "www.tv9090.com",
        "www.ulinix.cn",
        "www.wangxinghu.com",
        "www.wg999.com",
        "www.winyingwave.com",
        "www.xcxbk.com",
        "www.xcxtlq.com",
        "www.xigua365.com",
        "www.xunyou.mobi",
        "www.xushang114.com",
        "www.yaoka.com",
        "www.ycujizz.com.qzone.com",
        "www.yes321.com",
        "www.yesky.com",
        "www.youlu.com",
        "www.yukusoft.com",
        "www.zhaost.com",
        "www.zhongrou.net",
        "www.ztlsj.com",
        "www.zyrisen.com",
        "www294949.com",
        "www68uu.c0mwww.qzone.com",
        "www865.df.com",
        "wwwbb4444.com",
        "wx1.pg.qq.com",
        "wx57.cg.qq.com",
        "wxlobby.pg.qq.com",
        "wxsnsdy.tc.qq.com",
        "wzdmfm.com",
        "xbyszx.com.a.bdydns.com",
        "xggd&flx&gzo",
        "xj.weather.com.cn",
        "xmodhub.cn",
        "xnxx-cdn.com",
        "xp.qpic.cn",
        "xsddz1.114ic.com",
        "xvideos-cdn.com",
        "xvideos.91mv.co",
        "xvideos.comwww679922.com",
        "xvideos.xxsaobi.xyz",
        "xvideos.zhaofeizi.co",
        "xvideos.zhaosaozi.co",
        "xy2.qq.com",
        "ynfs8.com",
        "youxi.gamecenter.qq.com",
        "yoyo.qq.com",
        "ys.mihoyo.com",
        "ysdk-auth.qq.com",
        "ysdk-check.qq.com",
        "ysdk-heart.qq.com",
        "ysdk-live.qq.com",
        "ysdk-login.qq.com",
        "ysdk-token.qq.com",
        "ysdk.qq.com",
        "ysdkservice.qq.com",
        "ysdktoken.qq.com",
        "yyb.str.mdt.qq.com",
        "yyjlove6.qq.qzone.com",
        "zhi.zhe800.com",
        "zhuoyou.com",
        "zkres.myzaker.com",
        "zy.rzhushou.com",
        "zydz88.114ic.com",
        "zydz888.114ic.com"
    };


    public static boolean isRunning() { return running.get(); }

    public static void start(Context ctx) {
        installCrashCatcher();
        if (running.get()) { toast("防封②已在运行中"); return; }
        appCtx = ctx.getApplicationContext();
        running.set(true);
        toast("大厅防封②已开启");
        startAll();
    }

    private static void installCrashCatcher() {
        final Thread.UncaughtExceptionHandler old = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override public void uncaughtException(Thread t, Throwable e) {
                writeCrash(t, e);
                if (old != null) old.uncaughtException(t, e);
            }
        });
    }

    private static void writeCrash(Thread t, Throwable e) {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append("线程: ").append(t.getName()).append("\n");
            sb.append("异常: ").append(e.toString()).append("\n");
            for (StackTraceElement s : e.getStackTrace()) sb.append("    at ").append(s.toString()).append("\n");
            Throwable c = e.getCause();
            while (c != null) {
                sb.append("原因: ").append(c.toString()).append("\n");
                for (StackTraceElement s : c.getStackTrace()) sb.append("    at ").append(s.toString()).append("\n");
                c = c.getCause();
            }
        } catch (Throwable ignored) {}
        String content = sb.toString();
        // 尝试多个路径,哪个成哪个
        String[] paths = {
            "/sdcard/小月崩溃.txt",
            "/storage/emulated/0/小月崩溃.txt",
            "/storage/emulated/0/Download/小月崩溃.txt"
        };
        for (String path : paths) {
            try {
                java.io.FileWriter fw = new java.io.FileWriter(path, false);
                fw.write(content); fw.flush(); fw.close();
            } catch (Throwable ignored) {}
        }
        if (appCtx != null) {
            try {
                java.io.File dir = appCtx.getExternalFilesDir(null);
                if (dir != null) {
                    java.io.FileWriter fw = new java.io.FileWriter(new java.io.File(dir, "崩溃.txt"), false);
                    fw.write(content); fw.flush(); fw.close();
                }
            } catch (Throwable ignored) {}
        }
    }

    public static void stop() {
        running.set(false);
        synchronized (threads) {
            for (Thread t : threads) { try { t.interrupt(); } catch (Exception ignored) {} }
            threads.clear();
        }
        toast("防封②已关闭");
    }

    private static void startAll() {
                    addThread(new Runnable() {
                        @Override public void run() {
                            while (running.get()) {
                                for (String ip : RISK_5692_IPS) {
                                    if (!running.get()) return;
                                    sendUdp(ip, 5692);
                                    sendTcp(ip, 5692);
                                }
                                sleep(250);
                            }
                        }
                    });

                    addThread(new Runnable() {
                        @Override public void run() {
                            while (running.get()) {
                                for (String ip : ANTICHEAT_49155_IPS) {
                                    if (!running.get()) return;
                                    sendUdp(ip, 49155);
                                    sendUdp(ip, 49156);
                                }
                                sleep(200);
                            }
                        }
                    });

                    addThread(new Runnable() {
                        @Override public void run() {
                            while (running.get()) {
                                for (String seg : ANTICHEAT_SEGMENTS) {
                                    if (!running.get()) return;
                                    for (int host = 1; host <= 254; host++) {
                                        if (!running.get()) return;
                                        String ip = seg + "." + host;
                                        sendUdp(ip, 49155);
                                        sendUdp(ip, 49156);
                                    }
                                }
                                sleep(800);
                            }
                        }
                    });

                    addThread(new Runnable() {
                        @Override public void run() {
                            while (running.get()) {
                                for (String ip : AUDIT_IPS) {
                                    if (!running.get()) return;
                                    sendUdp(ip, 17500);
                                    sendUdp(ip, 5692);
                                    sendUdp(ip, 14863);
                                }
                                sleep(300);
                            }
                        }
                    });

                    addThread(new Runnable() {
                        @Override public void run() {
                            while (running.get()) {
                                for (String ip : PROBE_IPS) {
                                    if (!running.get()) return;
                                    sendUdp(ip, 14863);
                                    sendUdp(ip, 44863);
                                    sendUdp(ip, 54863);
                                    sendTcp(ip, 14863);
                                }
                                sleep(350);
                            }
                        }
                    });

                    addThread(new Runnable() {
                        @Override public void run() {
                            while (running.get()) {
                                for (String ip : HIGHRISK_IPS) {
                                    if (!running.get()) return;
                                    sendUdp(ip, 38932);
                                    sendUdp(ip, 25899);
                                    sendUdp(ip, 40851);
                                    sendUdp(ip, 20851);
                                    sendUdp(ip, 10851);
                                    sendUdp(ip, 30851);
                                }
                                for (String ip : HIGHRISK_RANGE_IPS) {
                                    if (!running.get()) return;
                                    sendUdp(ip, 36761);
                                    sendUdp(ip, 33987);
                                    sendUdp(ip, 20528);
                                    sendUdp(ip, 30578);
                                    sendUdp(ip, 23316);
                                    sendUdp(ip, 20851);
                                    sendUdp(ip, 30851);
                                }
                                sleep(250);
                            }
                        }
                    });

                    addThread(new Runnable() {
                        @Override public void run() {
                            while (running.get()) {
                                for (String ip : BLACKROOM_IPS) {
                                    if (!running.get()) return;
                                    sendUdp(ip, 50000);
                                    sendUdp(ip, 20000);
                                    sendTcp(ip, 50000);
                                    sendTcp(ip, 20000);
                                }
                                sendUdp("180.138.0.196", 7370);
                                sleep(300);
                            }
                        }
                    });

                    addThread(new Runnable() {
                        @Override public void run() {
                            while (running.get()) {
                                for (String ip : EXTRA_BLOCK_IPS) {
                                    if (!running.get()) return;
                                    for (int port : TERSAFE_PORTS) {
                                        sendUdp(ip, port);
                                    }
                                    sendTcp(ip, 443);
                                    sendTcp(ip, 80);
                                }
                                sleep(300);
                            }
                        }
                    });

                    addThread(new Runnable() {
                        @Override public void run() {
                            String[] dnsServers = {"8.8.8.8","114.114.114.114","223.5.5.5"};
                            while (running.get()) {
                                for (String host : ALL_DOMAINS) {
                                    if (!running.get()) return;
                                    for (String dns : dnsServers) { sendDnsQuery(dns, host); }
                                }
                                sleep(500);
                            }
                        }
                    });

                    addThread(new Runnable() {
                        @Override public void run() {
                            while (running.get()) {
                                for (String host : ALL_DOMAINS) {
                                    if (!running.get()) return;
                                    try { InetAddress.getAllByName(host); } catch (Exception ignored) {}
                                }
                                sleep(2000);
                            }
                        }
                    });

                    addThread(new Runnable() {
                        @Override public void run() {
                            try {
                                StringBuilder sb = new StringBuilder();
                                for (String host : ALL_DOMAINS) {
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

    private static void addThread(final Runnable task) {
        Thread t = new Thread(new Runnable() {
            @Override public void run() {
                try { task.run(); } catch (Throwable ignored) {}
            }
        });
        t.setDaemon(true);
        synchronized (threads) { threads.add(t); }
        t.start();
    }

    private static void toast(final String msg) {
        if (appCtx == null) return;
        ui.post(new Runnable() {
            @Override public void run() {
                Toast.makeText(appCtx, msg, Toast.LENGTH_SHORT).show();
            }
        });
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
            byte[] q = buildDnsQueryPacket(domain);
            s.send(new DatagramPacket(q, q.length, InetAddress.getByName(dnsServer), 53));
            s.close();
        } catch (Exception ignored) {}
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (Exception ignored) {}
    }

    private static byte[] buildDnsQueryPacket(String domain) {
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
}
