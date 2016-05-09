package csw.pkgDemo.hcd2;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import csw.services.pkg.Supervisor;
import csw.util.cfg.Configurations.SetupConfig;
import javacsw.services.pkg.JHcdControllerWithLifecycleHandler;
import javacsw.services.pkg.JLifecycleManager;

// A test HCD that is configured with the given name and config path
@SuppressWarnings({"WeakerAccess", "unused"})
public class Hcd2 extends JHcdControllerWithLifecycleHandler {
    private final LoggingAdapter log = Logging.getLogger(context().system(), this);

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
        worker.tell(config, self());
    }

    @Override
    // Ask the worker actor to send us the current state (handled by parent trait)
    public void requestCurrent() {
        worker.tell(Hcd2Worker.Msg.RequestCurrentState, self());

    }
}
