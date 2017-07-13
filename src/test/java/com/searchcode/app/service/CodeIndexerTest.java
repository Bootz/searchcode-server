package com.searchcode.app.service;

import com.searchcode.app.config.Values;
import com.searchcode.app.dao.Data;
import com.searchcode.app.dto.CodeIndexDocument;
import com.searchcode.app.model.RepoResult;
import junit.framework.TestCase;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.eclipse.jetty.util.ConcurrentArrayQueue;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;

public class CodeIndexerTest extends TestCase {
    public CodeIndexDocument codeIndexDocument = new CodeIndexDocument("repoLocationRepoNameLocationFilename", "repoName", "fileName", "fileLocation", "fileLocationFilename", "md5hash", "languageName", 100, "contents", "repoRemoteLocation", "codeOwner");
    public Queue<CodeIndexDocument> queue = new ConcurrentLinkedDeque<>();

    private void resetQueue() {
        this.queue.clear();
        this.queue.add(this.codeIndexDocument);
    }

    public void testIndexDocument() throws IOException {
        this.resetQueue();
        Singleton.getIndexService().indexDocument(this.queue);
    }

    public void testShouldPauseAddingExpectTrue() {
        SharedService sharedServiceMock = Mockito.mock(SharedService.class);
        when(sharedServiceMock.getPauseBackgroundJobs()).thenReturn(true);

        CodeIndexer codeIndexer = new CodeIndexer(null, null, null, sharedServiceMock);
        assertThat(codeIndexer.shouldPauseAdding()).isTrue();
    }

    public void testShouldPauseAddingExpectFalse() {
        Data dataMock = Mockito.mock(Data.class);
        StatsService statsServiceMock = Mockito.mock(StatsService.class);
        SharedService sharedServiceMock = Mockito.mock(SharedService.class);

        when(sharedServiceMock.getPauseBackgroundJobs()).thenReturn(false);
        when(statsServiceMock.getLoadAverage()).thenReturn("10000000");
        when(dataMock.getDataByName(Values.BACKOFFVALUE, Values.DEFAULTBACKOFFVALUE)).thenReturn("0");

        CodeIndexer codeIndexer = new CodeIndexer(dataMock, statsServiceMock, null, sharedServiceMock);
        assertThat(codeIndexer.shouldPauseAdding()).isFalse();
    }

    // TODO actually assert something in here
    public void testIndexDocuments() throws IOException {
        Queue<CodeIndexDocument> queue = new ConcurrentArrayQueue<>();
        queue.add(codeIndexDocument);
        Singleton.getIndexService().indexDocument(queue);
    }

    // TODO actually assert something in here
    public void testDeleteByRepoName() throws IOException {
        Singleton.getIndexService().deleteByRepo(new RepoResult());
    }

    // TODO actually assert something in here
    public void testDeleteByFilePath() throws IOException {
        Singleton.getIndexService().deleteByCodeId("./repo/test/README.md");
    }

    // TODO fix the assert rather then programming by exception
    public void testIndexDocumentsEmptyIssue() {
        try {
            CodeIndexDocument cid = new CodeIndexDocument("repoLocationRepoNameLocationFilename", "", "fileName", "fileLocation", "fileLocationFilename", "md5hash", "languageName", 0, null, "repoRemoteLocation", "codeOwner");

            Queue queue = new ConcurrentArrayQueue<CodeIndexDocument>();
            queue.add(cid);

            Singleton.getIndexService().indexDocument(queue);
        }
        catch(Exception ex) {
            assertTrue(false);
        }
    }

    public void testShouldBackOffWhenLoadVeryHigh() {
        Data dataMock = Mockito.mock(Data.class);
        StatsService statsServiceMock = Mockito.mock(StatsService.class);

        when(statsServiceMock.getLoadAverage()).thenReturn("10000000.0");
        when(dataMock.getDataByName(Values.BACKOFFVALUE, Values.DEFAULTBACKOFFVALUE)).thenReturn("1");

        CodeIndexer codeIndexer = new CodeIndexer(dataMock, statsServiceMock, null, null);
        assertThat(codeIndexer.shouldBackOff()).isTrue();
    }

    public void testShouldNotBackOffWhenLoadZero() {
        Data dataMock = Mockito.mock(Data.class);
        StatsService statsServiceMock = Mockito.mock(StatsService.class);
        when(statsServiceMock.getLoadAverage()).thenReturn("0.0");
        when(dataMock.getDataByName(Values.BACKOFFVALUE, Values.DEFAULTBACKOFFVALUE)).thenReturn("1");

        CodeIndexer codeIndexer = new CodeIndexer(dataMock, statsServiceMock, null, null);
        assertThat(codeIndexer.shouldBackOff()).isFalse();
    }

    public void testBuildDocument() {
        CodeIndexer codeIndexer = new CodeIndexer();
        Document indexableFields = codeIndexer.buildDocument(new CodeIndexDocument(
                "repoLocationRepoNameLocationFilename",
                "repo Name",
                "fileName",
                "fileLocation",
                "fileLocationFilename",
                "md5hash",
                "language Name",
                10,
                "contents",
                "repoRemoteLocation",
                "code Owner"
        ));

        assertThat(indexableFields.getFields().size()).isEqualTo(16);

        IndexableField[] fields = indexableFields.getFields(Values.REPONAME);
        assertThat(fields[0].stringValue()).isEqualTo("repo_Name");

        fields = indexableFields.getFields(Values.LANGUAGENAME);
        assertThat(fields[0].stringValue()).isEqualTo("language_Name");

        fields = indexableFields.getFields(Values.CODEOWNER);
        assertThat(fields[0].stringValue()).isEqualTo("code_Owner");

        // Verifies that we ran through the pipeline
        fields = indexableFields.getFields(Values.CONTENTS);
        assertThat(fields[0].stringValue()).isEqualTo(" filename filename filename filename filename filename  file name filelocationfilename filelocation contents contents contents contents contents contents");
    }

    // TODO expand on these tests
//    public void testIndexTimeDocuments() {
//        try {
//            Random rand = new Random();
//
//            String contents = "this is some code that should be found";
//
//            for (int j=0; j < 1000; j++) {
//                contents += " " + RandomStringUtils.randomAlphabetic(rand.nextInt(10) + 1);
//            }
//
//            CodeIndexDocument cid = new CodeIndexDocument("repoLocationRepoNameLocationFilename", "", "fileName", "fileLocation", "fileLocationFilename", "md5hash", "languageName", 0, contents, "repoRemoteLocation", "codeOwner");
//            cid.setRevision("99a5a271063def87b2473be79ce6f840d42d1f95");
//            cid.setYearMonthDay("20160101");
//
//            Queue queue = new ConcurrentArrayQueue<CodeIndexDocument>();
//            queue.add(cid);
//
//            CodeIndexer.indexTimeDocuments(queue);
//        }
//        catch(Exception ex) {
//            assertTrue(false);
//        }
//    }
}
