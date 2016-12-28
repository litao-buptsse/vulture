package com.sogou.vulture;

import com.sogou.vulture.common.util.CommonUtils;
import com.sogou.vulture.exec.ClusterExecutor;
import com.sogou.vulture.exec.Executor;
import com.sogou.vulture.model.LogDetail;
import com.sogou.vulture.model.LogMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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

  class Task implements Callable<Boolean> {
    private LogMeta logMeta;
    private LogDetail logDetail;
    private String targetTemperature;

    public Task(LogMeta logMeta, LogDetail logDetail, String targetTemperature) {
      this.logMeta = logMeta;
      this.logDetail = logDetail;
      this.targetTemperature = targetTemperature;
    }

    private Executor getExecutor() throws IOException {
      if (logMeta.getType().equals("HDFS") &&
          (targetTemperature.equals("WARM") || targetTemperature.equals("COLD"))) {
        return new ClusterExecutor();
      } else if (logMeta.getType().equals("HDFS") && targetTemperature.equals("DEAD") ||
          logMeta.getType().equals("HIVE") && targetTemperature.equals("DEAD")) {
        return new ClusterExecutor();
      } else {
        throw new IOException(String.format("No available executor (%s, %s)",
            logMeta.getType(), targetTemperature));
      }
    }

    private String getCommand() throws IOException {
      if (logMeta.getType().equals("HDFS") &&
          (targetTemperature.equals("WARM") || targetTemperature.equals("COLD"))) {
        return String.format("%s %s %s %s %s", Config.COMMAND_HDFS_COMPRESS,
            CommonUtils.fillConfVariablePattern(
                logMeta.getConf().get("pathPattern").toString(), logDetail.getTime()),
            CommonUtils.fillConfVariablePattern(
                logMeta.getConf().get("filePattern").toString(), logDetail.getTime()),
            targetTemperature, logDetail.getTime() + "-");
      } else if (logMeta.getType().equals("HDFS") && targetTemperature.equals("DEAD")) {
        return String.format("%s %s %s", Config.COMMAND_HDFS_CLEAN,
            CommonUtils.fillConfVariablePattern(
                logMeta.getConf().get("pathPattern").toString(), logDetail.getTime()),
            CommonUtils.fillConfVariablePattern(
                logMeta.getConf().get("filePattern").toString(), logDetail.getTime()));
      } else if (logMeta.getType().equals("HIVE") && targetTemperature.equals("DEAD")) {
        return String.format("%s %s %s %s", Config.COMMAND_HIVE_CLEAN,
            logMeta.getConf().get("database"), logMeta.getConf().get("table"),
            CommonUtils.fillConfVariablePattern(
                logMeta.getConf().get("partition").toString(), logDetail.getTime()));
      } else {
        throw new IOException(String.format("No available command (%s, %s)",
            logMeta.getType(), targetTemperature));
      }
    }

    public String getHadoopUgi() throws IOException {
      switch (logMeta.getType()) {
        case "HDFS":
          return Config.CLOTHO_HDFS_UGI;
        case "HIVE":
          return Config.CLOTHO_HIVE_UGI;
        default:
          throw new IOException(String.format("No available hadoopUgi (%s)", logMeta.getType()));
      }
    }

    @Override
    public Boolean call() throws Exception {
      try {
        // TODO statistic before
        boolean finished = getExecutor().exec(getCommand(), logDetail.getTime(), getHadoopUgi());
        if (finished) {
          logDetail.setTemperatureStatus(targetTemperature);
          if (logDetail.getTemperatureStatus().equals("DEAD")) {
            logDetail.setState("DELETED");
          }
          Config.LOG_DETAIL_DAO.updateLogDetail(logDetail);
        }
        // TODO statistic after
        return finished;
      } catch (IOException e) {
        LOG.error(String.format("Fail to run task (%s, %s)",
            logDetail.getId(), targetTemperature), e);
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
  }
}
