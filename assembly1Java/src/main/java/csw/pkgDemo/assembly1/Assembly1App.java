package csw.pkgDemo.assembly1;

import csw.services.loc.ComponentId;
import csw.services.loc.Connection;
import csw.services.loc.LocationService;
import csw.services.pkg.Component;
import javacsw.services.loc.JComponentId;
import javacsw.services.loc.JComponentType;
import javacsw.services.loc.JConnection;
import javacsw.services.loc.JConnectionType;
import javacsw.services.pkg.JComponent;
import javacsw.services.pkg.JSupervisor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Starts Assembly1 as a standalone application (as an alternative to starting it as part of Container1).
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Assembly1App {
    public static final String assemblyName = "Assembly-1";
    public static final String prefix = ""; // prefix is only important if using a distributor actor
    public static final String className = "csw.pkgDemo.assembly1.Assembly1";
    public static final ComponentId componentId = JComponentId.componentId(assemblyName, JComponentType.Assembly);

    public static void main(String[] args) {
        // This ennsures the location service and Akka use the primary IP address
        LocationService.initInterface();

        // Define the HCD connections
        Connection.AkkaConnection hcd2a = JConnection.akkaConnection(JComponentId.componentId("HCD-2A", JComponentType.HCD));
        Connection.AkkaConnection hcd2b = JConnection.akkaConnection(JComponentId.componentId("HCD-2B", JComponentType.HCD));
        Set<Connection> hcdConnections = new HashSet<>(Arrays.asList(hcd2a, hcd2b));

        // Describe the assembly
        Component.AssemblyInfo assemblyInfo = JComponent.assemblyInfo(
                assemblyName, prefix, className,
                JComponent.RegisterAndTrackServices,
                new HashSet<>(Arrays.asList(JConnectionType.AkkaType, JConnectionType.HttpType)),
                hcdConnections);

        // Create the supervisor and assembly in a new actor system
         JSupervisor.create(assemblyInfo);
    }
}
