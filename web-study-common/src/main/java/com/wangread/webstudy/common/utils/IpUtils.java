package com.wangread.webstudy.common.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.*;
import java.util.Enumeration;

/**
 * Created by yfwangrui on 2015/8/17.
 *
 * ip utils
 */
public class IpUtils {
    private final static Log log = LogFactory.getLog(IpUtils.class);

    private static String localhost;

    public static String localAddress() {
        if (localhost != null) {
            return localhost;
        }
        InetAddress local = null;
        try {
            local = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            log.error("IpUtil->localAddress", e);
        }
        if (local == null || local.isLoopbackAddress()) {
            try {
                //获取网卡
                Enumeration<NetworkInterface> netEnm = NetworkInterface.getNetworkInterfaces();
                while (netEnm != null && netEnm.hasMoreElements()) {
                    NetworkInterface network = netEnm.nextElement();
                    Enumeration<InetAddress> addressEnm = network.getInetAddresses();
                    while (addressEnm.hasMoreElements()) {
                        InetAddress address = addressEnm.nextElement();
                        if (address instanceof Inet4Address && address.isSiteLocalAddress() && !address.isLoopbackAddress()) {
                            local = address;
                        }
                    }
                }
            } catch (SocketException e) {
                log.error("IpUtil->localAddress", e);
            }
        }
        localhost = local == null ? InetAddress.getLoopbackAddress().getHostAddress() : local.getHostAddress();
        return localhost;
    }
}
