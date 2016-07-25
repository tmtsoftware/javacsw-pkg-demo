package csw.pkgDemo.assembly1;

import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import csw.pkgDemo.hcd2.Hcd2;
import csw.services.loc.LocationService;
import csw.util.config.ConfigJSON;
import csw.util.config.Configurations.*;
import csw.util.config.StateVariable;
import csw.util.config.StateVariable.*;
import javacsw.services.ccs.JAssemblyController;
import javacsw.services.pkg.JAssemblyControllerWithLifecycleHandler;
import javacsw.services.pkg.JSupervisor;

import java.util.*;

/**
 * A Java based test assembly that just forwards configs to HCDs based on prefix
 */
@SuppressWarnings({"FieldCanBeLocal", "OptionalUsedAsFieldOrParameterType", "unused"})
public class Assembly1 extends JAssemblyControllerWithLifecycleHandler {
    private final AssemblyInfo info;

    private final LoggingAdapter log = Logging.getLogger(context().system(), this);

    // Holds the current HCD states, used to answer requests
    private Map<String, StateVariable.CurrentState> stateMap = new HashMap<>();

    /**
     * @param info contains information about the assembly and the components it depends on
     */
    public Assembly1(AssemblyInfo info) {
        super(info);
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

    // Current state received from one of the HCDs
    private void updateCurrentState(CurrentState s) {
        stateMap.put(s.prefix(), s);
        requestCurrent();
    }

    // For now, when the current state is requested, send the HCD states.
    @Override
    public void requestCurrent() {
        CurrentStates states = StateVariable.createCurrentStates(new ArrayList<>(stateMap.values()));
        notifySubscribers(states);
    }

    @Override
    public Validation observe(Boolean locationsResolved, ObserveConfigArg configArg, Optional<ActorRef> replyTo) {
        return JAssemblyController.Invalid("Wasn't expecting an observe config");
    }

    // The argument contains a  list of setup configs. This method returns Valid if
    // each config contains either a filter or disperser key.
    private Validation validate(SetupConfigArg setupConfigArg) {
        Validation result = JAssemblyController.Valid;
        for (SetupConfig config : setupConfigArg.jconfigs()) {
            if (! ((config.exists(Hcd2.filterKey)) || (config.exists(Hcd2.disperserKey))))
                result = JAssemblyController.Invalid("Expected a filter or disperser key, but got: "
                        + ConfigJSON.writeConfig(config).toString());
        }
        return result;
    }

    @Override
    public Validation setup(Boolean locationsResolved, SetupConfigArg configArg, Optional<ActorRef> replyTo) {
        Validation valid = validate(configArg);
        if (valid.isValid()) {
            // The call below just distributes the configs to the HCDs based on matching prefix,
            // but you could just as well generate new configs and send them here...
            // Note that a CommandStatus message should eventually be sent to the replyTo actor.
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

