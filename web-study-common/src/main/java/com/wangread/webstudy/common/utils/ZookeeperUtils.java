package com.wangread.webstudy.common.utils;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.data.Stat;

import java.util.List;

/**
 * Created by yfwangrui on 2015/8/3.
 *
 * zookeeper utils
 */
public class ZookeeperUtils {
    public final static String SLASH = "/";
    private String root;

    private final CuratorFramework client;

    public ZookeeperUtils(String zkAddress, String root) {
        this.root = root;
        client = CuratorFrameworkFactory.newClient(zkAddress, new ExponentialBackoffRetry(1000, 3));
        client.start();
    }

    public void create(String path, List<String> childList) {
        String parentPath = root + SLASH + path;
        try {
            Stat stat = client.checkExists().forPath(parentPath);
            if (stat == null) {
                client.create().forPath(parentPath, "".getBytes());
            }
            for (String child : childList) {
                String childPath = parentPath + SLASH + child;
                client.create().forPath(childPath, "".getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void delete(String path, List<String> childList) {
        String parentPath = root + SLASH + path;
        try {
            Stat stat = client.checkExists().forPath(parentPath);
            if (stat == null) {
                client.create().forPath(parentPath, "".getBytes());
            }
            for (String child : childList) {
                String childPath = parentPath + SLASH + child;
                client.delete().forPath(childPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clear(String path) {
        try {
            List<String> childList = client.getChildren().forPath(path);
            for (String child : childList) {
                clear(path + SLASH + child);
            }
            client.delete().forPath(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> getChildList(String path) {
        List<String> childList = null;
        try {
            childList = client.getChildren().forPath(root + SLASH + path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return childList;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public void setZkAddress(String zkAddress) {
    }

    public CuratorFramework getClient() {
        return client;
    }
}
