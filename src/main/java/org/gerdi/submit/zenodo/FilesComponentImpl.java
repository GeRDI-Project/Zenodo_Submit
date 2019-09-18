package org.gerdi.submit.zenodo;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.util.Config;
import org.gerdi.submit.KubernetesUtil;
import org.gerdi.submit.component.AbstractFilesComponent;
import org.gerdi.submit.model.files.IPathElement;
import org.gerdi.submit.model.files.Path;
import org.gerdi.submit.model.files.PathDir;
import org.gerdi.submit.model.files.PathFile;
import org.gerdi.submit.security.GeRDIUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Component
public class FilesComponentImpl extends AbstractFilesComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilesComponentImpl.class);
    // In-Memory-Cache for dir value; Does NOT constraint scalability of this service; Open question: How to invalidate entries? They cannot persist forever
//    private static final Map<String, String> DIR_CACHE = new HashMap<>();

    private final CoreV1Api k8sApi;

    public FilesComponentImpl() throws IOException {
        super();
        ApiClient k8sClient = Config.defaultClient();
        Configuration.setDefaultApiClient(k8sClient);
        k8sApi = new CoreV1Api();
    }

    @Override
    public Path getPath(final String directory, final GeRDIUser user) {
        String username = user.getPreferredUsername();
        List<IPathElement> retVal = new ArrayList<>();
        File path = null;
        try {
            path = new File(KubernetesUtil.getRootPathForUser( username) + directory);
        } catch (ApiException e) {
            LOGGER.debug("Could not retrieve dir for user. Does it exist?");
            throw new IllegalStateException("No directory existing for user.", e);
        }
        LOGGER.info("Retrieving files from " + path.getAbsolutePath());
        if (path.listFiles() == null) LOGGER.error("Path returns null list. " + path.getAbsolutePath());
        for (File it: path.listFiles()) {
            if (it.isDirectory()) {
                retVal.add(new PathDir(it.getName()));
            } else {
                retVal.add(new PathFile(it.getName()));
            }
        }
        return new Path(retVal);
    }

}