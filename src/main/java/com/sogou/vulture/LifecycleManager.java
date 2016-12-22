package com.sogou.vulture;

import com.sogou.vulture.exec.ClusterExecutor;
import com.sogou.vulture.exec.Executor;
import com.sogou.vulture.exec.LocalExecutor;
import com.sogou.vulture.model.LogDetail;
import com.sogou.vulture.model.LogMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Tao Li on 22/12/2016.
 */
public class LifecycleManager {
  private final static Logger LOG = LoggerFactory.getLogger(LifecycleManager.class);

  private void run(String date) throws Exception {
    ExecutorService service = Executors.newFixedThreadPool(Config.THREAD_NUM);

    List<LogMeta> logMetas = Config.LOG_META_DAO.getAliveLogMetas();
    for (LogMeta logMeta : logMetas) {
      List<LogDetail> logDetails = Config.LOG_DETAIL_DAO.getAliveLogDetails(logMeta.getId(), date);
      for (LogDetail logDetail : logDetails) {
        String targetTemperature = calculateTargetTemperature(logMeta, logDetail);
        if (!targetTemperature.equals(logDetail.getTemperatureStatus())) {
          service.submit(new Task(logMeta, logDetail, targetTemperature));
        }
      }
    }

    service.shutdown();
  }

  // TODO need to implement
  private String calculateTargetTemperature(LogMeta logMeta, LogDetail logDetail) {
    return "DEAD";
  }


  class Task implements Runnable {
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
        return new LocalExecutor();
      } else {
        throw new IOException(String.format("No available executor (%s, %s)",
            logMeta.getType(), targetTemperature));
      }
    }

    private String getCommand() throws IOException {
      // TODO need to implement the command args
      if (logMeta.getType().equals("HDFS") &&
          (targetTemperature.equals("WARM") || targetTemperature.equals("COLD"))) {
        return Config.COMMAND_HDFS_COMPRESS;
      } else if (logMeta.getType().equals("HDFS") && targetTemperature.equals("DEAD")) {
        return Config.COMMAND_HDFS_CLEAN;
      } else if (logMeta.getType().equals("HIVE") && targetTemperature.equals("DEAD")) {
        return Config.COMMAND_HIVE_CLEAN;
      } else {
        throw new IOException(String.format("No available command (%s, %s)",
            logMeta.getType(), targetTemperature));
      }
    }

    @Override
    public void run() {
      try {
        // TODO statistic before
        getExecutor().exec(getCommand(), logDetail.getTime());
        // TODO statistic after
      } catch (IOException e) {
        LOG.error(String.format("Fail to run task (%s, %s)",
            logDetail.getId(), targetTemperature), e);
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
