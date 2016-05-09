package csw.pkgDemo.assembly1;

import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import csw.services.loc.LocationService;
import csw.util.cfg.Configurations.*;
import csw.util.cfg.StateVariable;
import csw.util.cfg.StateVariable.CurrentState;
import javacsw.services.ccs.JAssemblyController;
import javacsw.services.pkg.JAssemblyControllerWithLifecycleHandler;
import javacsw.services.pkg.JSupervisor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A Java based test assembly that just forwards configs to HCDs based on prefix
 */
@SuppressWarnings({"FieldCanBeLocal", "OptionalUsedAsFieldOrParameterType", "UnusedParameters", "unused"})
public class Assembly1 extends JAssemblyControllerWithLifecycleHandler {
    private final AssemblyInfo info;

    private final LoggingAdapter log = Logging.getLogger(context().system(), this);

    // Holds the current HCD states, used to answer requests
    private Map<String, StateVariable.CurrentState> stateMap = new HashMap<>();

    /**
     * @param info contains information about the assembly and the components it depends on
     */
    public Assembly1(AssemblyInfo info) {
        this.info = info;

        // Receive actor messages
        receive(defaultReceive().orElse(ReceiveBuilder.
                match(CurrentState.class, this::updateCurrentState).
                matchAny(x -> log.error("Unexpected message: " + x)).build())
        );

        // Starts the assembly
        JSupervisor.lifecycle(supervisor());

        // Get the connections to the HCDs this assembly uses and track them
        trackConnections(info.connections());

    }

    // Current state received from one of the HCDs: For now just forward it to any subscribers.
    // It might make more sense to create an Assembly state, built from the various HCD states and
    // publish that to the subscribers... TODO
    private void updateCurrentState(CurrentState s) {
        notifySubscribers(s);
        stateMap.put(s.prefix(), s);
    }

    // For now, when the current state is requested, send the HCD states.
    // TODO: Use assembly specific state
    @Override
    public void requestCurrent() {
        stateMap.values().forEach(this::notifySubscribers);
    }

    @Override
    public Validation observe(Boolean locationsResolved, ObserveConfigArg configArg, Optional<ActorRef> replyTo) {
        return JAssemblyController.Invalid("Wasn't expecting an observe config");
    }

    /**
     * Validates a received config arg
     */
    private Validation validate(SetupConfigArg config) {
        // TODO: add code to check if the config is valid
        return JAssemblyController.Valid;
    }

    @Override
    public Validation setup(Boolean locationsResolved, SetupConfigArg configArg, Optional<ActorRef> replyTo) {
        Validation valid = validate(configArg);
        if (valid.isValid()) {
            // The call below just distributes the configs to the HCDs based on matching prefix,
            // but you could just as well generate new configs and send them here...
            return distributeSetupConfigs(locationsResolved, configArg, replyTo);
        }
        return valid;
    }

    /**
     * Called when all HCD locations are resolved.
     * Override here so we can subscribe to status values from the HCD.
     */
    @Override
    public void allResolved(Set<LocationService.Location> locations) {
        subscribe(locations, self());
    }

}

