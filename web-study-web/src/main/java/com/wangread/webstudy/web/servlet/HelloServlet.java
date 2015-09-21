package com.wangread.webstudy.web.servlet;

import com.alibaba.fastjson.JSONObject;
import com.wangread.webstudy.service.redis.RedisCluster;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by yfwangrui on 2015/7/24.
 *
 * hello world
 */
public class HelloServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        WebApplicationContext springContext = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        DataSource dataSource = (DataSource) springContext.getBean("dataSource");
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            connection = dataSource.getConnection();
            String sql = "select * from cms_clientinfo where client = ?";
            ps = connection.prepareStatement(sql);
            ps.setString(1, "iwatch");
            rs = ps.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("clientinfo_id");
                String client = rs.getString("client");
                System.out.println(id + ":" + client);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }


        RedisCluster redisCluster = (RedisCluster) springContext.getBean("redisCluster");

        PrintWriter writer = null;
        try {
            writer = resp.getWriter();
            Map<String, Set<String>> redisClusterMap = new HashMap<>();
            redisClusterMap.put("master", redisCluster.getMasterSet());
            redisClusterMap.put("slave", redisCluster.getSlaveSet());
            writer.write(JSONObject.toJSONString(redisClusterMap));
            writer.flush();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
}
