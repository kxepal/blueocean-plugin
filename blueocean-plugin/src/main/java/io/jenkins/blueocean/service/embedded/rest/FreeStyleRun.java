package io.jenkins.blueocean.service.embedded.rest;

import hudson.model.Run;

/**
 * FreeStyleRun can add it's own element here
 *
 * @author Vivek Pandey
 */
public class FreeStyleRun extends AbstractBlueRun {

    public FreeStyleRun(Run run) {
        super(run);
    }
}
