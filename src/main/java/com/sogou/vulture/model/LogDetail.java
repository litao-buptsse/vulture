package com.sogou.vulture.model;

/**
 * Created by Tao Li on 22/12/2016.
 */
public class LogDetail {
  private long id;
  private long logId;
  private String time;
  private String state;
  private String transferState;
  private String temperatureStatus;

  public LogDetail(long id, long logId, String time,
                   String state, String transferState, String temperatureStatus) {
    this.id = id;
    this.logId = logId;
    this.time = time;
    this.state = state;
    this.transferState = transferState;
    this.temperatureStatus = temperatureStatus;
  }

  public long getId() {
    return id;
  }

  public long getLogId() {
    return logId;
  }

  public String getTime() {
    return time;
  }

  public String getState() {
    return state;
  }

  public String getTransferState() {
    return transferState;
  }

  public String getTemperatureStatus() {
    return temperatureStatus;
  }

  @Override
  public String toString() {
    return "LogDetail{" +
        "id=" + id +
        ", logId=" + logId +
        ", time='" + time + '\'' +
        ", state='" + state + '\'' +
        ", transferState='" + transferState + '\'' +
        ", temperatureStatus='" + temperatureStatus + '\'' +
        '}';
  }
}
