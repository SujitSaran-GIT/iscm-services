package com.iscm.iam.monitoring;

import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.JmxException;
import org.springframework.jmx.support.MBeanServerConnectionFactoryBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectionPoolMonitoringService {

    @Autowired(required = false)
    private MBeanServerConnectionFactoryBean mBeanServerFactory;

    private final Map<String, PoolStatistics> poolStatistics = new HashMap<>();

    // ========== Connection Pool Monitoring ==========

    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void monitorConnectionPools() {
        try {
            collectHikariPoolMetrics();
            logPoolStatistics();
        } catch (Exception e) {
            log.error("Failed to monitor connection pools", e);
        }
    }

    private void collectHikariPoolMetrics() {
        try {
            MBeanServerConnection mBeanServer = ManagementFactory.getPlatformMBeanServer();
            Set<ObjectName> hikariPools = mBeanServer.queryNames(
                new ObjectName("com.zaxxer.hikari:type=Pool,*"), null);

            for (ObjectName poolName : hikariPools) {
                String poolNameStr = poolName.getKeyProperty("name");
                PoolStatistics stats = collectPoolStatistics(mBeanServer, poolName);
                poolStatistics.put(poolNameStr, stats);
            }

        } catch (Exception e) {
            log.error("Failed to collect Hikari pool metrics", e);
        }
    }

    private PoolStatistics collectPoolStatistics(MBeanServerConnection mBeanServer, ObjectName poolName) {
        try {
            PoolStatistics stats = PoolStatistics.builder().build();

            // Basic pool metrics
            stats.setActiveConnections(getAttribute(mBeanServer, poolName, "ActiveConnections", Integer.class));
            stats.setIdleConnections(getAttribute(mBeanServer, poolName, "IdleConnections", Integer.class));
            stats.setTotalConnections(getAttribute(mBeanServer, poolName, "TotalConnections", Integer.class));
            stats.setThreadsAwaitingConnection(getAttribute(mBeanServer, poolName, "ThreadsAwaitingConnection", Integer.class));

            // Pool configuration
            stats.setMaximumPoolSize(getAttribute(mBeanServer, poolName, "MaxPoolSize", Integer.class));
            stats.setMinimumIdle(getAttribute(mBeanServer, poolName, "MinIdle", Integer.class));

            // Timing metrics
            stats.setAverageConnectionUsageTime(getAttribute(mBeanServer, poolName, "AverageConnectionUsageTime", Long.class));
            stats.setConnectionCreationMillis(getAttribute(mBeanServer, poolName, "ConnectionCreationMillis", Long.class));

            // Pool health metrics
            stats.setPercentIdle(stats.getTotalConnections() > 0 ?
                (double) stats.getIdleConnections() / stats.getTotalConnections() * 100 : 0.0);
            stats.setPercentUtilized(stats.getMaximumPoolSize() > 0 ?
                (double) stats.getActiveConnections() / stats.getMaximumPoolSize() * 100 : 0.0);

            return stats;

        } catch (Exception e) {
            log.error("Failed to collect statistics for pool: {}", poolName, e);
            return PoolStatistics.builder().build();
        }
    }

    private <T> T getAttribute(MBeanServerConnection mBeanServer, ObjectName objectName,
                              String attributeName, Class<T> type) {
        try {
            Object value = mBeanServer.getAttribute(objectName, attributeName);
            return type.cast(value);
        } catch (Exception e) {
            log.debug("Failed to get attribute {} from {}", attributeName, objectName, e);
            return null;
        }
    }

    private void logPoolStatistics() {
        poolStatistics.forEach((poolName, stats) -> {
            if (stats.getPercentUtilized() > 80.0) {
                log.warn("High connection pool utilization detected - Pool: {}, Utilization: {:.2f}%, Active: {}, Max: {}",
                        poolName, stats.getPercentUtilized(), stats.getActiveConnections(), stats.getMaximumPoolSize());
            }

            if (stats.getThreadsAwaitingConnection() > 5) {
                log.warn("High connection wait queue detected - Pool: {}, Waiting: {}",
                        poolName, stats.getThreadsAwaitingConnection());
            }

            log.debug("Pool {} - Active: {}, Idle: {}, Total: {}, Utilization: {:.2f}%",
                    poolName, stats.getActiveConnections(), stats.getIdleConnections(),
                    stats.getTotalConnections(), stats.getPercentUtilized());
        });
    }

    // ========== Public API ==========

    public Map<String, PoolStatistics> getPoolStatistics() {
        return new HashMap<>(poolStatistics);
    }

    public PoolStatistics getPoolStatistics(String poolName) {
        return poolStatistics.get(poolName);
    }

    public boolean isPoolHealthy(String poolName) {
        PoolStatistics stats = poolStatistics.get(poolName);
        if (stats == null) return false;

        // Consider pool unhealthy if utilization > 90% or waiting threads > 10
        return stats.getPercentUtilized() < 90.0 && stats.getThreadsAwaitingConnection() < 10;
    }

    public Map<String, Object> getConnectionPoolHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("healthy", true);
        health.put("pools", new HashMap<String, Object>());

        Map<String, Object> poolsHealth = (Map<String, Object>) health.get("pools");

        poolStatistics.forEach((poolName, stats) -> {
            Map<String, Object> poolHealth = new HashMap<>();
            boolean healthy = isPoolHealthy(poolName);

            poolHealth.put("healthy", healthy);
            poolHealth.put("utilization", stats.getPercentUtilized());
            poolHealth.put("activeConnections", stats.getActiveConnections());
            poolHealth.put("totalConnections", stats.getTotalConnections());
            poolHealth.put("waitingThreads", stats.getThreadsAwaitingConnection());

            if (!healthy) {
                health.put("healthy", false);
            }

            poolsHealth.put(poolName, poolHealth);
        });

        return health;
    }

    // ========== Scheduled Tasks ==========

    @Scheduled(cron = "0 */5 * * * ?") // Every 5 minutes
    public void logPoolPerformanceSummary() {
        if (poolStatistics.isEmpty()) {
            log.info("No connection pools to monitor");
            return;
        }

        poolStatistics.forEach((poolName, stats) -> {
            log.info("Connection Pool Summary - {}: Utilization {:.2f}%, Active: {}/{}, Waiting: {}, Avg Usage: {}ms",
                    poolName, stats.getPercentUtilized(), stats.getActiveConnections(), stats.getMaximumPoolSize(),
                    stats.getThreadsAwaitingConnection(), stats.getAverageConnectionUsageTime());
        });
    }

    // ========== Inner Classes ==========

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    public static class PoolStatistics {
        private int activeConnections;
        private int idleConnections;
        private int totalConnections;
        private int threadsAwaitingConnection;
        private int maximumPoolSize;
        private int minimumIdle;
        private long averageConnectionUsageTime;
        private long connectionCreationMillis;
        private double percentIdle;
        private double percentUtilized;
    }
}