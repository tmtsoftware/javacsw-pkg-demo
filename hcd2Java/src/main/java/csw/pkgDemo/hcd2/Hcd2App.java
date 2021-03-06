package csw.pkgDemo.hcd2;

import csw.services.loc.LocationService;
import csw.services.pkg.Component.HcdInfo;
import javacsw.services.pkg.JComponent;
import javacsw.services.pkg.JComponentSup;
import javacsw.services.pkg.JSupervisor;
import scala.concurrent.duration.FiniteDuration;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static javacsw.services.loc.JConnectionType.AkkaType;


/**
 * Starts Hcd2 as a standalone application.
 * Args: HCD-name: one of (HCD-2A, HCD-2B)
 */
public class Hcd2App {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Expected one argument: the HCD name");
            System.exit(1);
        }
        LocationService.initInterface();
        String hcdName = args[0];
        String prefix = Objects.equals(hcdName, "HCD-2A") ? Hcd2.filterPrefix : Hcd2.disperserPrefix;
        String className = "csw.pkgDemo.hcd2.Hcd2";
        HcdInfo hcdInfo = JComponentSup.hcdInfo(hcdName, prefix, className, JComponent.RegisterOnly,
                Collections.singleton(AkkaType), new FiniteDuration(1, TimeUnit.SECONDS));
        JSupervisor.create(hcdInfo);
    }
}
