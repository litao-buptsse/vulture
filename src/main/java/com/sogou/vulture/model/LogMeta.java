package com.sogou.vulture.model;

import org.json.JSONObject;

/**
 * Created by Tao Li on 22/12/2016.
 */
public class LogMeta {
  private long id;
  private String type;
  private String state;
  private long hotLivetime;
  private long warmLivetime;
  private long coldLivetime;
  private JSONObject conf;
  private int temperatureSwitch;

  public LogMeta(long id, String type, String state,
                 long hotLivetime, long warmLivetime, long coldLivetime,
                 String conf, int temperatureSwitch) {
    this.id = id;
    this.type = type;
    this.state = state;
    this.hotLivetime = hotLivetime;
    this.warmLivetime = warmLivetime;
    this.coldLivetime = coldLivetime;
    this.conf = new JSONObject(conf);
    this.temperatureSwitch = temperatureSwitch;
  }

  public long getId() {
    return id;
  }

  public String getType() {
    return type;
  }

  public String getState() {
    return state;
  }

  public long getHotLivetime() {
    return hotLivetime;
  }

  public long getWarmLivetime() {
    return warmLivetime;
  }

  public long getColdLivetime() {
    return coldLivetime;
  }

  public JSONObject getConf() {
    return conf;
  }

  public int getTemperatureSwitch() {
    return temperatureSwitch;
  }

  @Override
  public String toString() {
    return "LogMeta{" +
        "id=" + id +
        ", type='" + type + '\'' +
        ", state='" + state + '\'' +
        ", hotLivetime=" + hotLivetime +
        ", warmLivetime=" + warmLivetime +
        ", coldLivetime=" + coldLivetime +
        ", conf='" + conf + '\'' +
        ", temperatureSwitch=" + temperatureSwitch +
        '}';
  }
}
