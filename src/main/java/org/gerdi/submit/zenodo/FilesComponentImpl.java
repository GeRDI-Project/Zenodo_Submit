package org.gerdi.submit.zenodo;

import org.gerdi.submit.component.AbstractFilesComponent;
import org.gerdi.submit.model.files.File;
import org.gerdi.submit.model.files.IPathElement;
import org.gerdi.submit.model.files.Path;
import org.gerdi.submit.security.GeRDIUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@Component
public class FilesComponentImpl extends AbstractFilesComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilesComponentImpl.class);

    @Override
    public Path getPath(final String s, final GeRDIUser user) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = user.getPreferredUsername();
        System.out.println(username); // TODO Remove
        IPathElement elem = new File(s);
        Path hello = new Path(Arrays.asList(elem));
        return hello;
    }

}