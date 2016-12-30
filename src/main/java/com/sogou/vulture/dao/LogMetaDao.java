package com.sogou.vulture.dao;

import com.sogou.vulture.Config;
import com.sogou.vulture.common.db.ConnectionPoolException;
import com.sogou.vulture.model.LogMeta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tao Li on 22/12/2016.
 */
public class LogMetaDao {
  private final static String TABLE_NAME = "LogMeta";

  private List<LogMeta> getLogMetas(String whereClause)
      throws ConnectionPoolException, SQLException {
    String sql = String.format("SELECT * FROM %s %s", TABLE_NAME, whereClause);
    Connection conn = Config.POOL.getConnection();
    try {
      try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        try (ResultSet rs = stmt.executeQuery()) {
          List<LogMeta> logMetas = new ArrayList<>();
          while (rs.next()) {
            logMetas.add(new LogMeta(
                rs.getLong("id"),
                rs.getString("type"),
                rs.getString("state"),
                rs.getLong("hotLivetime"),
                rs.getLong("warmLivetime"),
                rs.getLong("coldLivetime"),
                rs.getString("conf"),
                rs.getInt("temperatureSwitch")
            ));
          }
          return logMetas;
        }
      }
    } finally {
      Config.POOL.releaseConnection(conn);
    }
  }

  public List<LogMeta> getAliveLogMetas() throws ConnectionPoolException, SQLException {
    // TODO only select sunshine logMetas
    return getLogMetas("WHERE state='RUN' AND temperatureSwitch='1' AND clusterId='3'");
  }
}
