package com.sogou.vulture.common.exec;

import com.sogou.vulture.Config;
import com.sogou.vulture.common.util.CommonUtils;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Tao Li on 22/12/2016.
 */
public class ClusterExecutor implements Executor {
  private static final String CLOTHO_SUBMIT_JOB_API = Config.CLOTHO_ROOT_API + "/job/vadd";
  private static final String CLOTHO_GET_JOBS_API = Config.CLOTHO_ROOT_API + "/job/jobs";

  private static final Map<String, String> HEADERS = new HashMap<>();

  static {
    HEADERS.put("token", Config.CLOTHO_TOKEN);
  }

  private static long submitJob(String command, String time, String hadoopUgi) throws IOException {
    Map<String, String> data = new HashMap<>();

    data.put("taskid", Config.CLOTHO_TASK_ID);
    data.put("groupName", Config.CLOTHO_GROUP_NAME);
    data.put("image", Config.CLOTHO_IMAGE);
    data.put("version", Config.CLOTHO_VERSION);
    data.put("memory", Config.CLOTHO_MEMORY);
    data.put("clusterUgi", hadoopUgi);
    data.put("state", JobState.WAIT.name());
    data.put("emails", Config.CLOTHO_EMAILS);
    data.put("noticeType", Config.CLOTHO_NOTICE_TYPE);
    data.put("name", Config.CLOTHO_NAME);
    data.put("timeout", Config.CLOTHO_TIMEOUT);
    data.put("maxRunCount", Config.CLOTHO_MAX_RUN_COUNT);

    data.put("command", command);
    data.put("time", time);
    data.put("startTime", CommonUtils.now());

    Response response = CommonUtils.sendPostRequest(CLOTHO_SUBMIT_JOB_API, HEADERS, data,
        MediaType.TEXT_HTML_TYPE);

    if (response.getStatus() == 200) {
      try {
        JSONObject json = new JSONObject(response.readEntity(String.class));
        if (json.getBoolean("isOk")) {
          return json.getJSONObject("data").getLong("id");
        }
      } catch (JSONException e) {
        throw new IOException(e);
      }
    }

    throw new IOException(String.format("Fail to submit job (%s, %s)", command, time));
  }

  private static JobState getJobState(long jobId) throws IOException {
    String uri = String.format("%s?id=%s", CLOTHO_GET_JOBS_API, jobId);
    Response response = CommonUtils.sendGetRequest(uri, HEADERS, MediaType.APPLICATION_JSON_TYPE);

    if (response.getStatus() == 200) {
      try {
        JSONObject json = new JSONObject(response.readEntity(String.class));
        if (json.getBoolean("isOk")) {
          return JobState.valueOf(
              json.getJSONArray("data").getJSONObject(0).getString("state"));
        }
      } catch (JSONException e) {
        throw new IOException(e);
      }
    }

    throw new IOException(String.format("Fail to get job state, jobId=%s", jobId));
  }

  private static boolean waitForJobCompletion(long jobId) throws IOException {
    long startTime = System.currentTimeMillis();
    while (true) {
      JobState state = getJobState(jobId);
      if (state.equals(JobState.SUCC)) {
        return true;
      } else if (state.equals(JobState.DEAD) || state.equals(JobState.CANCEL)) {
        return false;
      }

      try {
        Thread.sleep(60 * 1000);
      } catch (InterruptedException e) {
        throw new IOException(e);
      }

      if (System.currentTimeMillis() - startTime > Config.CLOTHO_CLIENT_TIMEOUT * 1000) {
        throw new IOException(
            String.format("Job %s timeout for %s millis", jobId, Config.CLOTHO_CLIENT_TIMEOUT));
      }
    }
  }

  @Override
  public boolean exec(String command, String time, String hadoopUgi,
                      StreamProcessor stdout, StreamProcessor stderr) throws IOException {
    long jobId = submitJob(command, time, hadoopUgi);
    return waitForJobCompletion(jobId);
  }

  public boolean exec(String command, String time, String hadoopUgi) throws IOException {
    return exec(command, time, hadoopUgi, null, null);
  }

  private enum JobState {
    WAIT, LOCK, SUBMITTING, RUN, SUCC, FAIL, DEAD, CANCEL
  }
}
