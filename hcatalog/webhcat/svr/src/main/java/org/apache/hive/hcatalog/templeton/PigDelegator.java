/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.hive.hcatalog.templeton;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.exec.ExecuteException;
import org.apache.hive.hcatalog.templeton.tool.TempletonControllerJob;
import org.apache.hive.hcatalog.templeton.tool.TempletonUtils;

/**
 * Submit a Pig job.
 *
 * This is the backend of the pig web service.
 */
public class PigDelegator extends LauncherDelegator {
  public PigDelegator(AppConfig appConf) {
    super(appConf);
  }

  public EnqueueBean run(String user, Map<String, Object> userArgs,
               String execute, String srcFile,
               List<String> pigArgs, String otherFiles,
               String statusdir, String callback, String completedUrl, boolean enablelog)
    throws NotAuthorizedException, BadParam, BusyException, QueueException,
    ExecuteException, IOException, InterruptedException {
    runAs = user;
    List<String> args = makeArgs(execute,
      srcFile, pigArgs,
      otherFiles, statusdir, completedUrl, enablelog);

    return enqueueController(user, userArgs, callback, args);
  }

  private List<String> makeArgs(String execute, String srcFile,
                  List<String> pigArgs, String otherFiles,
                  String statusdir, String completedUrl, boolean enablelog)
    throws BadParam, IOException, InterruptedException {
    ArrayList<String> args = new ArrayList<String>();
    try {
      ArrayList<String> allFiles = new ArrayList<String>();
      if (TempletonUtils.isset(srcFile))
        allFiles.add(TempletonUtils.hadoopFsFilename
          (srcFile, appConf, runAs));
      if (TempletonUtils.isset(otherFiles)) {
        String[] ofs = TempletonUtils.hadoopFsListAsArray(otherFiles, appConf, runAs);
        allFiles.addAll(Arrays.asList(ofs));
      }

      args.addAll(makeLauncherArgs(appConf, statusdir, completedUrl, allFiles, enablelog, JobType.PIG));
      if (appConf.pigArchive() != null && !appConf.pigArchive().equals(""))
      {
        args.add("-archives");
        args.add(appConf.pigArchive());
      }

      args.add("--");
      TempletonUtils.addCmdForWindows(args);
      args.add(appConf.pigPath());
      //the token file location should be first argument of pig
      args.add("-D" + TempletonControllerJob.TOKEN_FILE_ARG_PLACEHOLDER);

      for (String pigArg : pigArgs) {
        args.add(TempletonUtils.quoteForWindows(pigArg));
      }
      if (TempletonUtils.isset(execute)) {
        args.add("-execute");
        args.add(TempletonUtils.quoteForWindows(execute));
      } else if (TempletonUtils.isset(srcFile)) {
        args.add("-file");
        args.add(TempletonUtils.hadoopFsPath(srcFile, appConf, runAs)
          .getName());
      }
    } catch (FileNotFoundException e) {
      throw new BadParam(e.getMessage());
    } catch (URISyntaxException e) {
      throw new BadParam(e.getMessage());
    }

    return args;
  }
}
