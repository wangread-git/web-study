package com.wangread.webstudy.common.db;

import com.wangread.webstudy.common.utils.ZookeeperUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.apache.commons.pool.impl.GenericKeyedObjectPoolFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by yfwangrui on 2015/8/17.
 * <p>
 * dynamic config data source
 */
@Component
public class DynamicConfigDataSourceBean extends BasicDataSource implements InitializingBean {

    private final static Log log = LogFactory.getLog(DynamicConfigDataSourceBean.class);

    @Autowired
    private ZookeeperUtils zookeeperUtils;
    private String dbRoot;
    private PathChildrenCache cache;

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }

    @Override
    protected synchronized DataSource createDataSource() throws SQLException {
        if (closed) {
            throw new SQLException("Data source is closed");
        }

        // Return the pool if we have already created it
        if (dataSource != null) {
            return (dataSource);
        }
        return actuallyCreateDataSource();
    }

    protected synchronized DataSource actuallyCreateDataSource() throws SQLException {
        ConnectionFactory driverConnectionFactory = createConnectionFactory();

        // create a pool for our connections
        createConnectionPool();

        // Set up statement pool, if desired
        GenericKeyedObjectPoolFactory statementPoolFactory = null;
        if (isPoolPreparedStatements()) {
            statementPoolFactory = new GenericKeyedObjectPoolFactory(null,
                    -1, // unlimited maxActive (per key)
                    GenericKeyedObjectPool.WHEN_EXHAUSTED_FAIL,
                    0, // maxWait
                    1, // maxIdle (per key)
                    maxOpenPreparedStatements);
        }

        // Set up the poolable connection factory
        createPoolableConnectionFactory(driverConnectionFactory, statementPoolFactory, null);

        // Create and return the pooling data source to manage the connections
        createDataSourceInstance();

        try {
            for (int i = 0; i < initialSize; i++) {
                connectionPool.addObject();
            }
        } catch (Exception e) {
            throw new SQLException("Error preloading the connection pool", e);
        }

        return dataSource;
    }

    private void reload(List<ChildData> childList) throws Exception {
        if (CollectionUtils.isNotEmpty(childList)) {
            List<String> urlList = childList.stream().map(child -> new String(child.getData())).collect(Collectors.toList());
            DataBaseSharded sharded = new DataBaseSharded(urlList);
            String url = sharded.getShard();
            if (!url.equals(getUrl())) {
                if (log.isInfoEnabled()) {
                    log.info("switch db url, new url is " + url);
                }
                setUrl(url);
                if (dataSource != null) {
                    GenericObjectPool pool = connectionPool;
                    actuallyCreateDataSource();
                    //close old pool
                    pool.close();
                }
            }
        } else {
            throw new Exception("datasource url config is empty!");
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        cache = new PathChildrenCache(zookeeperUtils.getClient(), dbRoot, true);
        cache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);

        PathChildrenCacheListener listener = (client, event) -> {
            switch (event.getType()) {
                case CHILD_ADDED:
                case CHILD_REMOVED:
                case CHILD_UPDATED:
                    List<ChildData> childList = cache.getCurrentData();
                    reload(childList);
                    break;
            }
        };
        cache.getListenable().addListener(listener);
        List<ChildData> childList = cache.getCurrentData();
        reload(childList);
    }

    public void setDbRoot(String dbRoot) {
        this.dbRoot = dbRoot;
    }
}
