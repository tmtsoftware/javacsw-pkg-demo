package csw.pkgDemo.hcd2;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import csw.services.pkg.Supervisor;
import csw.util.config.ConfigJSON;
import csw.util.config.Configurations.SetupConfig;
import csw.util.config.StringKey;
import javacsw.services.pkg.JHcdControllerWithLifecycleHandler;
import javacsw.services.pkg.JLifecycleManager;

// A test HCD that is configured with the given name and config path
@SuppressWarnings({"WeakerAccess", "unused"})
public class Hcd2 extends JHcdControllerWithLifecycleHandler {
    private final LoggingAdapter log = Logging.getLogger(context().system(), this);

    /**
     * The prefix for filter configs
     */
    public static final String filterPrefix = "tcs.mobie.blue.filter";

    /**
     * The prefix for disperser configs
     */
    public static final String disperserPrefix = "tcs.mobie.blue.disperser";

    /**
     * The key for filter values
     */
    public static final StringKey filterKey = new StringKey("filter");

    /**
     * The key for disperser values
     */
    public static final StringKey disperserKey = new StringKey("disperser");

    /**
     * The available filters
     */
    public static final String[] FILTERS = new String[]{"None", "g_G0301", "r_G0303", "i_G0302", "z_G0304", "Z_G0322", "Y_G0323", "u_G0308"};

    /**
     * The available dispersers
     */
    public static final String[] DISPERSERS = new String[]{"Mirror", "B1200_G5301", "R831_G5302", "B600_G5303", "B600_G5307", "R600_G5304", "R400_G5305", "R150_G5306"};

    /**
     * Used to create the Hcd2 actor
     *
     * @param info the HCD's prefix, used in configurations
     * @return the Props needed to create the actor
     */
    public static Props props(final HcdInfo info) {
        return Props.create(new Creator<Hcd2>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Hcd2 create() throws Exception {
                return new Hcd2(info);
            }
        });
    }

    // Hcd2Worker actor used to process configs
    private final ActorRef worker;

    // Actor constructor: use the props() method to create the actor.
    private Hcd2(final HcdInfo info) {
        // Receive actor messages
        receive(defaultReceive());

        worker = getContext().actorOf(Hcd2Worker.props(info.prefix()));
        Supervisor.lifecycle(supervisor(), JLifecycleManager.Startup);
    }

    @Override
    // Send the config to the worker for processing
    public void process(SetupConfig config) {
        System.out.println("XXX HCD2 received " + ConfigJSON.writeConfig(config).toString());
        worker.tell(config, self());
    }

    @Override
    // Ask the worker actor to send us the current state (handled by parent trait)
    public void requestCurrent() {
        worker.tell(Hcd2Worker.Msg.RequestCurrentState, self());

    }
}
