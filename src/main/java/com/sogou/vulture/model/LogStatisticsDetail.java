package com.sogou.vulture.model;

/**
 * Created by Tao Li on 29/12/2016.
 */
public class LogStatisticsDetail {
  private String date;
  private long logId;
  private String time;
  private String temperature;
  private long size;
  private long num;
  private String targetTemperature;
  private long targetSize;
  private long targetNum;
  private String state;

  public LogStatisticsDetail(String date, long logId, String time, String temperature,
                             long size, long num, String targetTemperature, long targetSize,
                             long targetNum, String state) {
    this.date = date;
    this.logId = logId;
    this.time = time;
    this.temperature = temperature;
    this.size = size;
    this.num = num;
    this.targetTemperature = targetTemperature;
    this.targetSize = targetSize;
    this.targetNum = targetNum;
    this.state = state;
  }

  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public long getLogId() {
    return logId;
  }

  public void setLogId(long logId) {
    this.logId = logId;
  }

  public String getTime() {
    return time;
  }

  public void setTime(String time) {
    this.time = time;
  }

  public String getTemperature() {
    return temperature;
  }

  public void setTemperature(String temperature) {
    this.temperature = temperature;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

  public long getNum() {
    return num;
  }

  public void setNum(long num) {
    this.num = num;
  }

  public String getTargetTemperature() {
    return targetTemperature;
  }

  public void setTargetTemperature(String targetTemperature) {
    this.targetTemperature = targetTemperature;
  }

  public long getTargetSize() {
    return targetSize;
  }

  public void setTargetSize(long targetSize) {
    this.targetSize = targetSize;
  }

  public long getTargetNum() {
    return targetNum;
  }

  public void setTargetNum(long targetNum) {
    this.targetNum = targetNum;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }
}
