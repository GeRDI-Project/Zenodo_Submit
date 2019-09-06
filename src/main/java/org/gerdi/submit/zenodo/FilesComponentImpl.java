package org.gerdi.submit.zenodo;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1PersistentVolumeClaim;
import io.kubernetes.client.models.V1PersistentVolumeClaimList;
import io.kubernetes.client.util.Config;
import org.gerdi.submit.component.AbstractFilesComponent;
import org.gerdi.submit.model.files.IPathElement;
import org.gerdi.submit.model.files.Path;
import org.gerdi.submit.model.files.PathDir;
import org.gerdi.submit.model.files.PathFile;
import org.gerdi.submit.security.GeRDIUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

@Component
public class FilesComponentImpl extends AbstractFilesComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilesComponentImpl.class);
    // In-Memory-Cache for dir value; Does NOT constraint scalability of this service
    private static final Map<String, String> DIR_CACHE = new HashMap<>();
    public static final String PERSISTENT_VOL_DIR = "/local/persistent-volumes/jhub-claim-%s-%s";

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
            path = new File(getRootPathForUser(username) + directory);
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

    private String getRootPathForUser(final String username) throws ApiException {
        final String rootPath;
        if ((rootPath = DIR_CACHE.get(username)) != null) return rootPath;
        // TODO Get Path from K8s
        final String k8sPath = getPersistentVolumeClaim(username);
        if (k8sPath != null) return k8sPath;
        return null;
    }

    /**
     * Retrieves the directory used for the Kubernetes PersistentVolumeClaim
     *
     * @param username The username linked to the PersistentVolumeClaim
     * @return A File instance pointing to the directory or null if volume does not exist.
     */
    private final String getPersistentVolumeClaim(String username) throws ApiException {
        String retVal = null;
        V1PersistentVolumeClaimList list = k8sApi.listNamespacedPersistentVolumeClaim("jhub",null, null, null,null,null,null,null,null,null);
        for (V1PersistentVolumeClaim item : list.getItems()) {
            final String claimUsername = item.getMetadata().getAnnotations().get("hub.jupyter.org/username");
            if (claimUsername != null && claimUsername.equals(username)) {
                String volumeName = item.getSpec().getVolumeName();
                if (volumeName == null) break;
                retVal = String.format(PERSISTENT_VOL_DIR, username, volumeName);
                break;
            }
        }
        return retVal;
    }

}