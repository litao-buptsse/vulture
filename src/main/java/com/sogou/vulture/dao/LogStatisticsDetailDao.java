package com.sogou.vulture.dao;

import com.sogou.vulture.Config;
import com.sogou.vulture.common.db.ConnectionPoolException;
import com.sogou.vulture.common.db.JDBCUtils;
import com.sogou.vulture.model.LogStatisticsDetail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tao Li on 29/12/2016.
 */
public class LogStatisticsDetailDao {
  private final static String TABLE_NAME = "LogStatisticsDetail";

  private List<LogStatisticsDetail> getLogStatisticsDetails(String whereClause)
      throws ConnectionPoolException, SQLException {
    String sql = String.format("SELECT * FROM %s %s", TABLE_NAME, whereClause);
    Connection conn = Config.POOL.getConnection();
    try {
      try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        try (ResultSet rs = stmt.executeQuery()) {
          List<LogStatisticsDetail> logStatisticsDetails = new ArrayList<>();
          while (rs.next()) {
            logStatisticsDetails.add(new LogStatisticsDetail(
                rs.getString("date"),
                rs.getString("type"),
                rs.getLong("logId"),
                rs.getString("time"),
                rs.getString("temperature"),
                rs.getLong("size"),
                rs.getLong("num"),
                rs.getString("targetTemperature"),
                rs.getLong("targetSize"),
                rs.getLong("targetNum"),
                rs.getString("state")
            ));
          }
          return logStatisticsDetails;
        }
      }
    } finally {
      Config.POOL.releaseConnection(conn);
    }
  }

  private LogStatisticsDetail getLogStatisticsDetail(String whereClause)
      throws ConnectionPoolException, SQLException {
    List<LogStatisticsDetail> logStatisticsDetails = getLogStatisticsDetails(whereClause);
    return logStatisticsDetails.size() == 0 ? null : logStatisticsDetails.get(0);
  }

  public LogStatisticsDetail getLogStatisticsDetail(long logId, String time)
      throws ConnectionPoolException, SQLException {
    return getLogStatisticsDetail(String.format("WHERE logId='%s' AND `time`='%s'", logId, time));
  }

  private boolean existLogStatisticsDetail(LogStatisticsDetail logStatisticsDetail)
      throws ConnectionPoolException, SQLException {
    return getLogStatisticsDetail(
        logStatisticsDetail.getLogId(), logStatisticsDetail.getTime()) != null ? true : false;
  }

  public void createLogStatisticsDetail(LogStatisticsDetail logStatisticsDetail)
      throws ConnectionPoolException, SQLException {
    JDBCUtils.execute(Config.POOL, String.format(
        "INSERT INTO %s (`date`, `type`, logId, `time`, temperature, size, num, " +
            "targetTemperature, targetSize, targetNum, `state`) " +
            "VALUES('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s')", TABLE_NAME,
        logStatisticsDetail.getDate(), logStatisticsDetail.getType(),
        logStatisticsDetail.getLogId(), logStatisticsDetail.getTime(),
        logStatisticsDetail.getTemperature(), logStatisticsDetail.getSize(),
        logStatisticsDetail.getNum(), logStatisticsDetail.getTargetTemperature(),
        logStatisticsDetail.getTargetSize(), logStatisticsDetail.getTargetNum(),
        logStatisticsDetail.getState()));
  }

  public void updateLogStatisticsDetail(LogStatisticsDetail logStatisticsDetail)
      throws ConnectionPoolException, SQLException {
    JDBCUtils.execute(Config.POOL, String.format(
        "UPDATE %s SET `type`='%s', temperature='%s', size='%s', num='%s', " +
            "targetTemperature='%s', targetSize='%s', targetNum='%s', `state`='%s'" +
            "WHERE logId='%s' AND `time`='%s'", TABLE_NAME,
        logStatisticsDetail.getType(), logStatisticsDetail.getTemperature(),
        logStatisticsDetail.getSize(), logStatisticsDetail.getNum(),
        logStatisticsDetail.getTargetTemperature(), logStatisticsDetail.getTargetSize(),
        logStatisticsDetail.getTargetNum(), logStatisticsDetail.getState(),
        logStatisticsDetail.getLogId(), logStatisticsDetail.getTime()));
  }

  public void createOrUpdateLogStatisticsDetail(LogStatisticsDetail logStatisticsDetail)
      throws ConnectionPoolException, SQLException {
    if (existLogStatisticsDetail(logStatisticsDetail)) {
      updateLogStatisticsDetail(logStatisticsDetail);
    } else {
      createLogStatisticsDetail(logStatisticsDetail);
    }
  }
}
