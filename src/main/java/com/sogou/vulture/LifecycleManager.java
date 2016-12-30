package com.sogou.vulture;

import com.sogou.vulture.common.db.ConnectionPoolException;
import com.sogou.vulture.common.exec.ClusterExecutor;
import com.sogou.vulture.common.exec.LocalExecutor;
import com.sogou.vulture.common.exec.StreamCollector;
import com.sogou.vulture.common.util.CommonUtils;
import com.sogou.vulture.model.LogDetail;
import com.sogou.vulture.model.LogMeta;
import com.sogou.vulture.model.LogStatisticsDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by Tao Li on 22/12/2016.
 */
public class LifecycleManager {
  private final static Logger LOG = LoggerFactory.getLogger(LifecycleManager.class);

  private void run(String date) throws Exception {
    ExecutorService service = Executors.newFixedThreadPool(Config.THREAD_NUM);

    Map<Long, Future<Boolean>> futures = new HashMap<>();

    List<LogMeta> logMetas = Config.LOG_META_DAO.getAliveLogMetas();
    for (LogMeta logMeta : logMetas) {
      List<LogDetail> logDetails = Config.LOG_DETAIL_DAO.getAliveLogDetails(logMeta.getId(), date);
      for (LogDetail logDetail : logDetails) {
        String targetTemperature = calculateTargetTemperature(logMeta, logDetail);
        // FIXME targetTemperature should be great than logDetail.getTemperatureStatus()
        if (!targetTemperature.equals(logDetail.getTemperatureStatus())) {
          futures.put(logDetail.getId(),
              service.submit(new Task(logMeta, logDetail, targetTemperature)));
        }
      }
    }

    for (Map.Entry<Long, Future<Boolean>> entry : futures.entrySet()) {
      boolean finished = entry.getValue().get();
      if (finished) {
        LOG.info("Succeed to run logDetail " + entry.getKey());
      } else {
        LOG.error("Failed to run logDetail " + entry.getKey());
      }
    }

    service.shutdown();
  }

  private String calculateTargetTemperature(LogMeta logMeta, LogDetail logDetail) {
    LocalDateTime start = CommonUtils.getDateTimeOfTime(logDetail.getTime());
    LocalDateTime end = LocalDateTime.now();

    long hotRange = logMeta.getHotLivetime();
    long warmRange = hotRange + logMeta.getWarmLivetime();
    long coldRange = warmRange + logMeta.getColdLivetime();

    if (end.compareTo(start.plusDays(coldRange)) > 0) {
      return "DEAD";
    } else if (end.compareTo(start.plusDays(warmRange)) > 0) {
      return "COLD";
    } else if (end.compareTo(start.plusDays(hotRange)) > 0) {
      return "WARM";
    } else {
      return "HOT";
    }
  }

  static class Task implements Callable<Boolean> {
    private LogMeta logMeta;
    private LogDetail logDetail;
    private String targetTemperature;
    private TaskType taskType = TaskType.INVALID;
    private LogStatisticsDetail logStatisticsDetail;

    enum TaskType {
      HDFS_COMPRESS_WARM, HDFS_COMPRESS_COLD, HDFS_CLEAN, HIVE_CLEAN, INVALID
    }

    public Task(LogMeta logMeta, LogDetail logDetail, String targetTemperature) {
      this.logMeta = logMeta;
      this.logDetail = logDetail;
      this.targetTemperature = targetTemperature;

      if (logMeta.getType().equals("HDFS")) {
        if (targetTemperature.equals("WARM")) {
          taskType = TaskType.HDFS_COMPRESS_WARM;
        } else if (targetTemperature.equals("COLD")) {
          taskType = TaskType.HDFS_COMPRESS_COLD;
        } else if (targetTemperature.equals("DEAD")) {
          taskType = TaskType.HDFS_CLEAN;
        }
      } else if (logMeta.getType().equals("HIVE")) {
        if (targetTemperature.equals("DEAD")) {
          taskType = TaskType.HIVE_CLEAN;
        }
      }

      logStatisticsDetail = new LogStatisticsDetail(
          logDetail.getTime().substring(0, 8), logMeta.getType(),
          logDetail.getLogId(), logDetail.getTime(),
          logDetail.getTemperatureStatus(), 0, 0,
          targetTemperature, 0, 0, "INIT"
      );
    }

    private String getCommand() {
      switch (taskType) {
        case HDFS_COMPRESS_WARM:
        case HDFS_COMPRESS_COLD:
          return String.format("%s %s %s %s %s", Config.COMMAND_HDFS_COMPRESS,
              CommonUtils.fillConfVariablePattern(
                  logMeta.getConf().get("pathPattern").toString(), logDetail.getTime()),
              CommonUtils.fillConfVariablePattern(
                  logMeta.getConf().get("filePattern").toString(), logDetail.getTime()),
              targetTemperature, logDetail.getTime() + "-");
        case HDFS_CLEAN:
          return String.format("%s %s %s", Config.COMMAND_HDFS_CLEAN,
              CommonUtils.fillConfVariablePattern(
                  logMeta.getConf().get("pathPattern").toString(), logDetail.getTime()),
              CommonUtils.fillConfVariablePattern(
                  logMeta.getConf().get("filePattern").toString(), logDetail.getTime()));
        case HIVE_CLEAN:
          return String.format("%s %s %s %s", Config.COMMAND_HIVE_CLEAN,
              logMeta.getConf().get("database"), logMeta.getConf().get("table"),
              CommonUtils.fillConfVariablePattern(
                  logMeta.getConf().get("partition").toString(), logDetail.getTime()));
        default:
          return null;
      }
    }

    private String getHadoopUgi() {
      switch (logMeta.getType()) {
        case "HDFS":
          return Config.CLOTHO_HDFS_UGI;
        case "HIVE":
          return Config.CLOTHO_HIVE_UGI;
        default:
          return null;
      }
    }

    private long[] statistics() throws IOException {
      String command = null;
      switch (logMeta.getType()) {
        case "HDFS":
          command = String.format("%s %s %s",
              Config.COMMAND_HDFS_STATISTICS,
              CommonUtils.fillConfVariablePattern(
                  logMeta.getConf().get("pathPattern").toString(), logDetail.getTime()),
              CommonUtils.fillConfVariablePattern(
                  logMeta.getConf().get("filePattern").toString(), logDetail.getTime()));
          break;
        case "HIVE":
          command = String.format("%s %s %s %s",
              Config.COMMAND_HIVE_STATISTICS,
              logMeta.getConf().get("database"), logMeta.getConf().get("table"),
              CommonUtils.fillConfVariablePattern(
                  logMeta.getConf().get("partition").toString(), logDetail.getTime()));
          break;
      }

      StreamCollector stdout = new StreamCollector();
      boolean finished = new LocalExecutor().exec(
          command, logDetail.getTime(), getHadoopUgi(), stdout, null);
      if (finished) {
        String[] output = stdout.getOutput().get(0).split(",");
        long size = Long.parseLong(output[0]);
        long num = Long.parseLong(output[1]);
        return new long[]{size, num};
      }

      throw new IOException("Fail to statistics " + logDetail.getId());
    }

    private void beforeExec() throws IOException, ConnectionPoolException, SQLException {
      long[] info = statistics();
      long size = info[0];
      long num = info[1];

      logStatisticsDetail.setSize(size);
      logStatisticsDetail.setNum(num);
      logStatisticsDetail.setTargetSize(size);
      logStatisticsDetail.setTargetNum(num);
      logStatisticsDetail.setState("INIT");

      Config.LOG_STATISTICS_DETAIL_DAO.createOrUpdateLogStatisticsDetail(logStatisticsDetail);
    }

    private void afterExec(boolean finished) throws IOException, ConnectionPoolException, SQLException {
      if (finished) {
        // Update LogDetail state and targetTemperature
        logDetail.setTemperatureStatus(targetTemperature);
        if (logDetail.getTemperatureStatus().equals("DEAD")) {
          logDetail.setState("DELETED");
        }
        Config.LOG_DETAIL_DAO.updateLogDetail(logDetail);

        // Statistics
        long size = 0;
        long num = 0;
        if (taskType == TaskType.HDFS_COMPRESS_WARM || taskType == TaskType.HDFS_COMPRESS_COLD) {
          long[] info = statistics();
          size = info[0];
          num = info[1];
        }

        logStatisticsDetail.setTargetSize(size);
        logStatisticsDetail.setTargetNum(num);
        logStatisticsDetail.setState("SUCC");
      } else {
        logStatisticsDetail.setState("FAIL");
      }

      Config.LOG_STATISTICS_DETAIL_DAO.updateLogStatisticsDetail(logStatisticsDetail);
    }

    @Override
    public Boolean call() throws Exception {
      if (taskType == TaskType.INVALID) {
        throw new Exception("Invalid task type " + logDetail.getId());
      }

      try {
        beforeExec();
        boolean finished = new ClusterExecutor().
            exec(getCommand(), logDetail.getTime(), getHadoopUgi());
        afterExec(finished);
        return finished;
      } catch (Exception e) {
        LOG.error("Fail to run task " + logDetail.getId(), e);
        return false;
      }
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length < 1) {
      LOG.error("need args: date");
      System.exit(1);
    }

    Config.init();
    Config.POOL.start();

    try {
      new LifecycleManager().run(args[0]);
    } finally {
      Config.POOL.close();
    }

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      // TODO kill all runnning tasks
    }));
  }
}
