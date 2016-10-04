package csw.pkgDemo.container1;

import javacsw.services.apps.containerCmd.JContainerCmd;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Java example that creates container1 based on resources/container1.conf
 */
public class Container1 {
  public static void main(String[] args) {
    // This defines the names that can be used with the --start option and the config files used ("" is the default entry)
    Map<String, String> m = new HashMap<>();
    m.put("", "container1.conf"); // default value

    // Parse command line args for the application (app name is container1java, like the sbt project)
    JContainerCmd.createContainerCmd("container1java", args, m);
  }
}
