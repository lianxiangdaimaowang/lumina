package com.lianxiangdaimaowang.lumina.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

@Component
public class DatabaseMonitor {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseMonitor.class);
    
    @Autowired
    private DataSource dataSource;
    
    /**
     * 每小时检查一次数据库连接状态
     */
    @Scheduled(fixedRate = 3600000) // 1小时
    public void checkDatabaseConnection() {
        logger.info("执行数据库连接状态检查: {}", new Date());
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1");
             ResultSet rs = ps.executeQuery()) {
            
            if (rs.next() && rs.getInt(1) == 1) {
                logger.info("数据库连接正常");
            } else {
                logger.error("数据库连接测试失败");
            }
            
        } catch (SQLException e) {
            logger.error("数据库连接错误: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 每天检查一次数据库性能
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点
    public void checkDatabasePerformance() {
        logger.info("执行数据库性能检查: {}", new Date());
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SHOW GLOBAL STATUS LIKE 'Threads_%'");
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                logger.info("数据库状态 - {}: {}", rs.getString(1), rs.getString(2));
            }
            
        } catch (SQLException e) {
            logger.error("数据库性能检查错误: {}", e.getMessage(), e);
        }
    }
} 