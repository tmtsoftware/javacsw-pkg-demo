package csw.pkgDemo.container1;

import javacsw.services.apps.containerCmd.JContainerCmd;

import java.util.Optional;

/**
 * Java example that creates container1 based on resources/container1.conf
 */
public class Container1 {
    public static void main(String[] args) {
        JContainerCmd.createContainerCmd(args, Optional.of("container1.conf"));
    }
}
