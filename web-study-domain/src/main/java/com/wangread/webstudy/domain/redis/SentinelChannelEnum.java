package com.wangread.webstudy.domain.redis;

/**
 * Created by yfwangrui on 2015/8/13.
 *
 * sentinel channel enum
 */
public enum SentinelChannelEnum {
    SWITCH_MASTER("+switch-master"),
    ADD_SLAVE("+slave");

    private String channel;

    SentinelChannelEnum(String channel) {
        this.channel = channel;
    }

    public String getChannel() {
        return channel;
    }

    public static SentinelChannelEnum valueOfChannel(String channel) {
        for (SentinelChannelEnum enm : SentinelChannelEnum.values()) {
            if (enm.getChannel().equals(channel)) {
                return enm;
            }
        }
        return null;
    }
}
