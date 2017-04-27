package org.nuxeo.room.test;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.bulkread.service.BRService;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.elasticsearch.api.ElasticSearchIndexing;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;


@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@RepositoryConfig(cleanup = Granularity.CLASS)
@Deploy({"org.nuxeo.ecm.core.io", "org.nuxeo.ecm.automation.server", "org.nuxeo.ecm.platform.importer.core"})
@LocalDeploy({"org.nuxeo.bulkread.tests", "org.nuxeo.bulkread.tests:elasticsearch-test-contrib.xml",
        "org.nuxeo.bulkread.tests:storage-blob-test-contrib.xml"})
public class TestService {

    static String NAME = "yo";

    @Inject
    CoreSession session;

    @Inject
    BRService brs;

    @Inject
    EventService eventService;

    @Test
    public void checkServiceDeployed() throws Exception {
        BRService rs = Framework.getService(BRService.class);
        Assert.assertNotNull(rs);

        ElasticSearchService ess = Framework.getLocalService(ElasticSearchService.class);
        Assert.assertNotNull(ess);

        ElasticSearchIndexing esi = Framework.getLocalService(ElasticSearchIndexing.class);
        Assert.assertNotNull(esi);

    }

    @Test
    public void shouldCreateFolder() throws Exception {

        DocumentModel folder = brs.createBigFolder(NAME, 100, session, 1);
        Assert.assertNotNull(folder);

        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        Thread.sleep(1000);

        eventService.waitForAsyncCompletion();
    }

    @Test
    public void shouldExportFolder() throws Exception {

        String ppName="export_fullES";
        //String ppName = "export_core";

        DocumentModel folder = session.getDocument(new PathRef("/" + NAME));

        DocumentModelList children = session.getChildren(folder.getRef());

        System.out.println("CHILDREN=###" + children.size());

        String deviceName = "enp0s8";
        long delayMillis = 50;

        addNetEmDelay(deviceName, delayMillis);

        File export = brs.exportBigFolder(NAME, session, ppName);

        delNetEmDelay(deviceName);

        System.out.println(export.getAbsolutePath());

    }

    // Ensure your account has sudo nopasswd privilege to /usr/sbin/tc executable in order to successfully
    // invoke this method
    protected int addNetEmDelay(String deviceName, long delayMillis) throws IOException, InterruptedException {
        String[] tcCommandInfoArr = {"sudo", "/usr/sbin/tc", "qdisc", "add", "dev", deviceName, "root", "netem", "delay",
                Long.toString(delayMillis) + "ms"};
        Process process = executeCommand(Arrays.asList(tcCommandInfoArr));
        int exitValue = process.exitValue();
        if (exitValue != 0) {
            throw new RuntimeException("Failed to addNetEmDelay due to return code: " + exitValue
                    + " with error message: "
                    + new Scanner(process.getErrorStream(), "utf-8").useDelimiter("\\Z").next());
        }
        return exitValue;
    }

    // Ensure your account has sudo nopasswd privilege to /usr/sbin/tc executable in order to successfully
    // invoke this method
    protected int delNetEmDelay(String deviceName) throws IOException, InterruptedException {
        String[] tcCommandInfoArr = {"sudo", "/usr/sbin/tc", "qdisc", "del", "dev", deviceName, "root"};
        Process process = executeCommand(Arrays.asList(tcCommandInfoArr));
        int exitValue = process.exitValue();
        if (exitValue != 0) {
            throw new RuntimeException("Failed to delNetEmDelay due to return code: " + exitValue
                    + " with error message: "
                    + new Scanner(process.getErrorStream(), "utf-8").useDelimiter("\\Z").next());
        }
        return exitValue;
    }

    protected Process executeCommand(List<String> commandInformation) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(commandInformation);
        Process process = pb.start();
        process.waitFor();
        return process;
    }

}
