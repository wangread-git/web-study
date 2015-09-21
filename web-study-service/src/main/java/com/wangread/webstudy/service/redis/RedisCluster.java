package com.wangread.webstudy.service.redis;

import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.wangread.webstudy.domain.redis.SentinelChannelEnum;
import com.wangread.webstudy.common.utils.ZookeeperUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by yfwangrui on 2015/8/3.
 * <p>
 * redis cluster
 */
@Component
public class RedisCluster implements InitializingBean {

    private final static Log log = LogFactory.getLog(RedisCluster.class);

    private Set<String> sentinels;

    private ConcurrentHashSet<String> masterSet = new ConcurrentHashSet<>();
    private ConcurrentHashSet<String> slaveSet = new ConcurrentHashSet<>();

    @Autowired
    private ZookeeperUtils zookeeperUtils;

    public void init() {
        //todo: get sentinel config from zookeeper
        List<String> masterList = zookeeperUtils.getChildList("master");
        List<String> slaveList = zookeeperUtils.getChildList("slave");
        if (CollectionUtils.isNotEmpty(masterList)) {
            for (String master : masterList) {
                String[] hostAndPort = master.split(":");
                if (hostAndPort.length == 2) {
                    this.masterSet.add(master);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(slaveList)) {
            for (String slave : slaveList) {
                String[] hostAndPort = slave.split(":");
                if (hostAndPort.length == 2) {
                    this.slaveSet.add(slave);
                }
            }
        }
        ConcurrentHashSet<String> masterSet = new ConcurrentHashSet<>();
        ConcurrentHashSet<String> slaveSet = new ConcurrentHashSet<>();

        for (String address : sentinels) {
            String[] hostAndPortArr = address.split(":");
            if (hostAndPortArr.length == 2) {
                Jedis jedis = null;
                try {
                    String host = hostAndPortArr[0];
                    String port = hostAndPortArr[1];
                    jedis = new Jedis(host, Integer.parseInt(port));

                    List<Map<String, String>> masterInfoList = jedis.sentinelMasters();
                    Set<String> masterNameSet = new HashSet<>();
                    for (Map<String, String> masterInfo : masterInfoList) {
                        String name = masterInfo.get("name");
                        String masterHost = masterInfo.get("ip");
                        String masterPort = masterInfo.get("port");
                        masterNameSet.add(name);
                        masterSet.add(masterHost + ":" +masterPort);
                    }
                    List<String> addMaster = new ArrayList<>();
                    for (String hostAndPort : masterSet) {
                        if (!this.masterSet.contains(hostAndPort)) {
                            addMaster.add(hostAndPort);
                        }
                    }
                    zookeeperUtils.create("master", addMaster);
                    List<String> delMaster = new ArrayList<>();
                    for (String hostAndPort : this.masterSet) {
                        if (!masterSet.contains(hostAndPort)) {
                            delMaster.add(hostAndPort);
                        }
                    }
                    zookeeperUtils.delete("master", delMaster);

                    for (String name : masterNameSet) {
                        List<Map<String, String>> slaveInfoList = jedis.sentinelSlaves(name);
                        for (Map<String, String> slaveInfo : slaveInfoList) {
                            String slaveHost = slaveInfo.get("ip");
                            String slavePort = slaveInfo.get("port");
                            slaveSet.add(slaveHost + ":" + slavePort);
                        }
                    }
                    List<String> addSlave = new ArrayList<>();
                    for (String hostAndPort : slaveSet) {
                        if (!this.slaveSet.contains(hostAndPort)) {
                            addSlave.add(hostAndPort);
                        }
                    }
                    zookeeperUtils.create("slave", addSlave);
                    List<String> delSlave = new ArrayList<>();
                    for (String hostAndPort : this.slaveSet) {
                        if (!slaveSet.contains(hostAndPort)) {
                            delSlave.add(hostAndPort);
                        }
                    }
                    zookeeperUtils.delete("slave", delSlave);
                    SentinelEventListener sentinelEventListener = new SentinelEventListener(host, Integer.parseInt(port));
                    sentinelEventListener.start();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (jedis != null) {
                        jedis.close();
                    }
                }
            }
        }
        setMasterSet(masterSet);
        setSlaveSet(slaveSet);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }

    private class SentinelEventListener extends Thread {

        private String host;
        private int port;
        private long subscribeRetryWaitTimeMillis = 5000;
        private Jedis j;
        private AtomicBoolean running = new AtomicBoolean(false);

        protected SentinelEventListener() {
        }

        public SentinelEventListener(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public SentinelEventListener(String host, int port,
                                     long subscribeRetryWaitTimeMillis) {
            this(host, port);
            this.subscribeRetryWaitTimeMillis = subscribeRetryWaitTimeMillis;
        }

        public void run() {

            running.set(true);

            while (running.get()) {

                j = new Jedis(host, port);

                try {
                    j.subscribe(new JedisPubSub() {
                        @Override
                        public void onMessage(String channel, String message) {
                            log.info("Sentinel " + host + ":" + port + " published: " + message + ".");

                            SentinelChannelEnum channelEnum = SentinelChannelEnum.valueOfChannel(channel);
                            String[] switchMasterMsg = message.split(" ");
                            switch (channelEnum) {
                                case SWITCH_MASTER:
                                    if (switchMasterMsg.length > 3) {

                                        String oldMasterHost = switchMasterMsg[1];
                                        String oldMasterPort = switchMasterMsg[2];
                                        String newMasterHost = switchMasterMsg[3];
                                        String newMasterPort = switchMasterMsg[4];
                                        HostAndPort oldMaster = new HostAndPort(oldMasterHost, Integer.parseInt(oldMasterPort));
                                        HostAndPort newMaster = new HostAndPort(newMasterHost, Integer.parseInt(newMasterPort));
                                        masterSet.remove(oldMaster.toString());
                                        slaveSet.remove(newMaster.toString());
                                        masterSet.add(newMaster.toString());
                                        List<String> addMaster = new ArrayList<>();
                                        addMaster.add(newMaster.toString());
                                        List<String> delMaster = new ArrayList<>();
                                        delMaster.add(oldMaster.toString());
                                        zookeeperUtils.delete("master", delMaster);
                                        zookeeperUtils.delete("slave", addMaster);
                                        zookeeperUtils.create("master", addMaster);
                                    } else {
                                        log.warn("Ignoring message on +switch-master for master name "
                                                + switchMasterMsg[0] + ", our master name is " + switchMasterMsg[0]);
                                    }
                                    break;
                                case ADD_SLAVE:
                                    if (switchMasterMsg.length == 8) {
                                        String host = switchMasterMsg[2];
                                        String port = switchMasterMsg[3];
                                        HostAndPort newSlave = new HostAndPort(host, Integer.parseInt(port));
                                        slaveSet.add(host + ":" + port);
                                        List<String> addSlave = new ArrayList<>();
                                        addSlave.add(newSlave.toString());
                                        zookeeperUtils.create("slave", addSlave);
                                    }
                            }
                        }
                    }, SentinelChannelEnum.SWITCH_MASTER.getChannel());

                } catch (JedisConnectionException e) {

                    if (running.get()) {
                        log.error("Lost connection to Sentinel at " + host + ":" + port
                                + ". Sleeping 5000ms and retrying.");
                        try {
                            Thread.sleep(subscribeRetryWaitTimeMillis);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    } else {
                        log.error("Unsubscribing from Sentinel at " + host + ":" + port);
                    }
                }
            }
        }

        public void shutdown() {
            try {
                log.info("Shutting down listener on " + host + ":" + port);
                running.set(false);
                // This isn't good, the Jedis object is not thread safe
                j.disconnect();
            } catch (Exception e) {
                log.error("Caught exception while shutting down: " + e.getMessage());
            }
        }
    }

    public Set<String> getMasterSet() {
        return Collections.unmodifiableSet(masterSet);
    }

    void setMasterSet(ConcurrentHashSet<String> masterSet) {
        this.masterSet = masterSet;
    }

    public Set<String> getSlaveSet() {
        return Collections.unmodifiableSet(slaveSet);
    }

    void setSlaveSet(ConcurrentHashSet<String> slaveSet) {
        this.slaveSet = slaveSet;
    }

    public void setSentinels(Set<String> sentinels) {
        this.sentinels = sentinels;
    }
}
