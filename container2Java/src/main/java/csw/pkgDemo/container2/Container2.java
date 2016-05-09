package csw.pkgDemo.container2;

import javacsw.services.apps.containerCmd.JContainerCmd;

import java.util.Optional;

/**
 * Java example that creates container1 based on resources/container1.conf
 */
public class Container2 {
    public static void main(String[] args) {
        JContainerCmd.createContainerCmd(args, Optional.of("container2.conf"));
    }
}
