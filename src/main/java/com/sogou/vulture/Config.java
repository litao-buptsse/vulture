package com.sogou.vulture;

import com.sogou.vulture.common.db.ConnectionPoolException;
import com.sogou.vulture.common.db.JDBCConnectionPool;
import com.sogou.vulture.dao.LogDetailDao;
import com.sogou.vulture.dao.LogMetaDao;
import com.sogou.vulture.dao.LogStatisticsDetailDao;
import com.typesafe.config.ConfigFactory;

/**
 * Created by Tao Li on 22/12/2016.
 */
public class Config {
  public static JDBCConnectionPool POOL;
  public static LogMetaDao LOG_META_DAO;
  public static LogDetailDao LOG_DETAIL_DAO;
  public static LogStatisticsDetailDao LOG_STATISTICS_DETAIL_DAO;
  public static int THREAD_NUM;
  public static String CLOTHO_ROOT_API;
  public static String CLOTHO_TOKEN;
  public static String CLOTHO_TASK_ID;
  public static String CLOTHO_GROUP_NAME;
  public static String CLOTHO_IMAGE;
  public static String CLOTHO_VERSION;
  public static String CLOTHO_MEMORY;
  public static String CLOTHO_HDFS_UGI;
  public static String CLOTHO_HIVE_UGI;
  public static String CLOTHO_EMAILS;
  public static String CLOTHO_NOTICE_TYPE;
  public static String CLOTHO_NAME;
  public static String CLOTHO_TIMEOUT;
  public static String CLOTHO_MAX_RUN_COUNT;
  public static long CLOTHO_CLIENT_TIMEOUT;
  public static String COMMAND_HDFS_COMPRESS;
  public static String COMMAND_HDFS_CLEAN;
  public static String COMMAND_HIVE_CLEAN;
  public static String COMMAND_HDFS_STATISTICS;
  public static String COMMAND_HIVE_STATISTICS;

  public static void init() throws ConnectionPoolException {
    com.typesafe.config.Config conf = ConfigFactory.load();
    com.typesafe.config.Config rootConf = conf.getConfig("root");
    com.typesafe.config.Config databaseConf = rootConf.getConfig("database");
    com.typesafe.config.Config schedulerConf = rootConf.getConfig("scheduler");
    com.typesafe.config.Config clusterExecutorConf =
        rootConf.getConfig("executor").getConfig("cluster");
    com.typesafe.config.Config commandConf = rootConf.getConfig("command");

    POOL = new JDBCConnectionPool(
        databaseConf.getString("driver"), databaseConf.getString("url"));
    POOL.setInitConnectionNum(databaseConf.getInt("initConnectionNum"));
    POOL.setMinConnectionNum(databaseConf.getInt("minConnectionNum"));
    POOL.setMaxConnectionNum(databaseConf.getInt("maxConnectionNum"));
    POOL.setIdleTimeout(databaseConf.getLong("idleTimeout"));
    POOL.setIdleQueueSize(databaseConf.getInt("idleQueueSize"));
    POOL.setIdleConnectionCloseThreadPoolSize(
        databaseConf.getInt("idleConnectionCloseThreadPoolSize"));

    LOG_META_DAO = new LogMetaDao();
    LOG_DETAIL_DAO = new LogDetailDao();
    LOG_STATISTICS_DETAIL_DAO = new LogStatisticsDetailDao();

    THREAD_NUM = schedulerConf.getInt("threadNum");

    CLOTHO_ROOT_API = clusterExecutorConf.getString("rootApi");
    CLOTHO_TOKEN = clusterExecutorConf.getString("token");
    CLOTHO_TASK_ID = clusterExecutorConf.getString("taskId");
    CLOTHO_GROUP_NAME = clusterExecutorConf.getString("groupName");
    CLOTHO_IMAGE = clusterExecutorConf.getString("image");
    CLOTHO_VERSION = clusterExecutorConf.getString("version");
    CLOTHO_MEMORY = clusterExecutorConf.getString("memory");
    CLOTHO_HDFS_UGI = clusterExecutorConf.getString("hdfsUgi");
    CLOTHO_HIVE_UGI = clusterExecutorConf.getString("hiveUgi");
    CLOTHO_EMAILS = clusterExecutorConf.getString("emails");
    CLOTHO_NOTICE_TYPE = clusterExecutorConf.getString("noticeType");
    CLOTHO_NAME = clusterExecutorConf.getString("name");
    CLOTHO_TIMEOUT = clusterExecutorConf.getString("timeout");
    CLOTHO_MAX_RUN_COUNT = clusterExecutorConf.getString("maxRunCount");
    CLOTHO_CLIENT_TIMEOUT = clusterExecutorConf.getLong("clientTimeout");

    COMMAND_HDFS_COMPRESS = commandConf.getString("hdfsCompress");
    COMMAND_HDFS_CLEAN = commandConf.getString("hdfsClean");
    COMMAND_HIVE_CLEAN = commandConf.getString("hiveClean");
    COMMAND_HDFS_STATISTICS = commandConf.getString("hdfsStatistics");
    COMMAND_HIVE_STATISTICS = commandConf.getString("hiveStatistics");
  }
}
