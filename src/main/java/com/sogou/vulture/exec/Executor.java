package com.sogou.vulture.exec;

import java.io.IOException;

/**
 * Created by Tao Li on 22/12/2016.
 */
public interface Executor {
  boolean exec(String command, String time, String hadoopUgi) throws IOException;
}
