/*
 * Copyright (c) 2016 Boyter Online Services
 *
 * Use of this software is governed by the Fair Source License included
 * in the LICENSE.TXT file, but will be eventually open under GNU General Public License Version 3
 * see the README.md for when this clause will take effect
 *
 * Version 1.3.11
 */


package com.searchcode.app.jobs.repository;

import com.searchcode.app.config.Values;
import com.searchcode.app.dto.RunningIndexJob;
import com.searchcode.app.model.RepoResult;
import com.searchcode.app.service.SharedService;
import com.searchcode.app.service.Singleton;
import com.searchcode.app.util.SearchcodeLib;
import com.searchcode.app.util.UniqueRepoQueue;
import org.quartz.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;

/**
 * This job is responsible for pulling and indexing file repositories which are kept upto date by some external
 * job such as cron or the like
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class IndexFileRepoJob extends IndexBaseRepoJob {

    public String repoName;


    public IndexFileRepoJob() {
        this(Singleton.getSharedService());
    }

    public IndexFileRepoJob(SharedService sharedService) {
        this.sharedService = sharedService;
    }

    /**
     * The main method used for finding jobs to index and actually doing the work
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        if (!isEnabled()) {
            return;
        }

        if (!this.sharedService.getBackgroundJobsEnabled()) {
            return;
        }

        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

        while (this.indexService.shouldRepoJobPause()) {
            Singleton.getLogger().info("Pausing parser.");
            return;
        }

        // Pull the next repo to index from the queue
        UniqueRepoQueue repoQueue = this.getNextQueuedRepo();

        RepoResult repoResult = repoQueue.poll();

        if (repoResult != null && !Singleton.getRunningIndexRepoJobs().containsKey(repoResult.getName())) {

            Singleton.getLogger().info("File Indexer Indexing " + repoResult.getName());
            repoResult.getData().indexStatus = "indexing";
            repoResult.getData().jobRunTime = Instant.now();
            Singleton.getRepo().saveRepo(repoResult);

            try {
                Singleton.getRunningIndexRepoJobs().put(repoResult.getName(),
                        new RunningIndexJob("Indexing", Singleton.getHelpers().getCurrentTimeSeconds()));

                JobDataMap data = context.getJobDetail().getJobDataMap();

                String repoName = repoResult.getName();
                this.repoName = repoName;
                String repoRemoteLocation = repoResult.getUrl();

                String repoLocations = data.get("REPOLOCATIONS").toString();
                this.LOWMEMORY = Boolean.parseBoolean(data.get("LOWMEMORY").toString());

                Path docDir = Paths.get(repoRemoteLocation);

                this.indexDocsByPath(docDir, repoName, repoLocations, repoRemoteLocation, true);

                int runningTime = Singleton.getHelpers().getCurrentTimeSeconds() - Singleton.getRunningIndexRepoJobs().get(repoResult.getName()).startTime;
                repoResult.getData().averageIndexTimeSeconds = (repoResult.getData().averageIndexTimeSeconds + runningTime) / 2;
                repoResult.getData().indexStatus = "success";
                repoResult.getData().jobRunTime = Instant.now();
                Singleton.getRepo().saveRepo(repoResult);
            }
            finally {
                // Clean up the job
                Singleton.getRunningIndexRepoJobs().remove(repoResult.getName());
            }
        }
    }

    @Override
    public String getFileLocationFilename(String fileToString, String fileRepoLocations) {
        if (this.repoName == null) {
            return fileToString.replace(fileRepoLocations, Values.EMPTYSTRING);
        }
        return this.repoName + fileToString.replace(fileRepoLocations, Values.EMPTYSTRING);
    }

    @Override
    public UniqueRepoQueue getNextQueuedRepo() {
        return Singleton.getUniqueFileRepoQueue();
    }

    @Override
    public String getCodeOwner(List<String> codeLines, String newString, String repoName, String fileRepoLocations, SearchcodeLib scl) {
        return "File System";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean ignoreFile(String fileParent) {
        return Singleton.getHelpers().ignoreFiles(fileParent);
    }
}
