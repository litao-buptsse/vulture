package com.sogou.vulture.dao;

import com.sogou.vulture.Config;
import com.sogou.vulture.common.db.ConnectionPoolException;
import com.sogou.vulture.common.db.JDBCUtils;
import com.sogou.vulture.model.LogDetail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tao Li on 22/12/2016.
 */
public class LogDetailDao {
  private final static String TABLE_NAME = "LogDetail";

  public List<LogDetail> getLogDetails(String whereClause)
      throws ConnectionPoolException, SQLException {
    String sql = String.format("SELECT * FROM %s %s", TABLE_NAME, whereClause);
    Connection conn = Config.POOL.getConnection();
    try {
      try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        try (ResultSet rs = stmt.executeQuery()) {
          List<LogDetail> logDetails = new ArrayList<>();
          while (rs.next()) {
            logDetails.add(new LogDetail(
                rs.getLong("id"),
                rs.getLong("logId"),
                rs.getString("time"),
                rs.getString("state"),
                rs.getString("transferState"),
                rs.getString("temperatureStatus")
            ));
          }
          return logDetails;
        }
      }
    } finally {
      Config.POOL.releaseConnection(conn);
    }
  }

  public List<LogDetail> getAliveLogDetails(long logId, String date)
      throws ConnectionPoolException, SQLException {
    return getLogDetails(String.format("WHERE logId='%s' AND transferState='SUCC'" +
        " AND state='SUCC' AND temperatureStatus!='DEAD' AND time like '%s%%'", logId, date));
  }

  // FIXME only update state and temperatureStatus
  public void updateLogDetail(LogDetail logDetail) throws ConnectionPoolException, SQLException {
    JDBCUtils.execute(Config.POOL, String.format(
        "UPDATE %s SET state='%s', temperatureStatus='%s' WHERE id='%s'",
        TABLE_NAME, logDetail.getState(), logDetail.getTemperatureStatus(), logDetail.getId()));
  }
}
