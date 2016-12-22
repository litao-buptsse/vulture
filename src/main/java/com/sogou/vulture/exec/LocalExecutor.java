package com.sogou.vulture.exec;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Tao Li on 23/12/2016.
 */
public class LocalExecutor implements Executor {
  public static int runProcess(String command, StreamProcessor stdoutProcessor,
                               StreamProcessor stderrProcessor) throws IOException {
    ProcessBuilder builder = new ProcessBuilder("bin/ext/runner.py", command);
    Process process = null;
    try {
      process = builder.start();
      if (stdoutProcessor != null) {
        new StreamWatcher(process.getInputStream(), stdoutProcessor).start();
      }
      if (stderrProcessor != null) {
        new StreamWatcher(process.getErrorStream(), stderrProcessor).start();
      }
      return process.waitFor();
    } catch (IOException | InterruptedException e) {
      if (process != null) {
        process.destroy();
      }
      throw new IOException(e);
    }
  }

  static class StreamWatcher extends Thread {
    private InputStream stream;
    private StreamProcessor processor;

    public StreamWatcher(InputStream stream, StreamProcessor processor) {
      this.stream = stream;
      this.processor = processor;
    }

    @Override
    public void run() {
      try {
        processor.process(stream);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public boolean exec(String command, String time) throws IOException {
    String realCommand = String.format("%s %s", command, time);
    return runProcess(realCommand, null, null) == 0;
  }
}