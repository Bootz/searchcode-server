/*
 * Copyright (c) 2016 Boyter Online Services
 *
 * Use of this software is governed by the Fair Source License included
 * in the LICENSE.TXT file, but will be eventually open under GNU General Public License Version 3
 * see the README.md for when this clause will take effect
 *
 * Version 1.3.11
 */

package com.searchcode.app.jobs;

import com.searchcode.app.config.Values;
import com.searchcode.app.model.RepoResult;
import com.searchcode.app.service.Singleton;
import com.searchcode.app.util.Properties;
import org.apache.commons.io.FileUtils;
import org.quartz.*;

import java.io.File;
import java.util.List;

/**
 * The job which deletes repositories from the database index and disk where one exists in the deletion queue.
 * TODO fix race condition where it can start deleting while the repo has been re-added to be indexed
 * TODO add some tests for this to ensure everything such as the early return occurs correctly
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class DeleteRepositoryJob implements Job {
    public void execute(JobExecutionContext context) throws JobExecutionException {
        if (!Singleton.getSharedService().getBackgroundJobsEnabled()) {
            return;
        }

        List<String> persistentDelete = Singleton.getDataService().getPersistentDelete();
        if (persistentDelete.isEmpty()) {
            return;
        }

        RepoResult rr = Singleton.getRepo().getRepoByName(persistentDelete.get(0));
        if (rr == null) {
            Singleton.getDataService().removeFromPersistentDelete(persistentDelete.get(0));
            return;
        }

        try {
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

            Singleton.getUniqueGitRepoQueue().delete(rr);

            if (Singleton.getRunningIndexRepoJobs().containsKey(rr.getName())) {
                return;
            }

            Singleton.getLogger().info("Deleting repository. " + rr.getName());
            Singleton.getIndexService().deleteByRepo(rr);

            // remove the directory
            String repoLocations = Properties.getProperties().getProperty(Values.REPOSITORYLOCATION, Values.DEFAULTREPOSITORYLOCATION);
            FileUtils.deleteDirectory(new File(repoLocations + rr.getName() + "/"));

            // Remove from the database
            Singleton.getRepo().deleteRepoByName(rr.getName());

            // Remove from the persistent queue
            Singleton.getDataService().removeFromPersistentDelete(rr.getName());
        }
        catch (Exception ignored) {}
    }
}
