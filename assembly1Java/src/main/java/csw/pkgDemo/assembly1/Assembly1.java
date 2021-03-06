package csw.pkgDemo.assembly1;

import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import csw.pkgDemo.hcd2.Hcd2;
import csw.services.ccs.HcdController;
import csw.services.ccs.SequentialExecutor;
import csw.services.ccs.Validation;
import csw.services.loc.Connection;
import csw.services.loc.LocationService;
import csw.services.loc.LocationSubscriberActor;
import csw.util.config.Configurations.*;
import csw.util.config.StateVariable;
import csw.util.config.StateVariable.*;
import javacsw.services.ccs.JAssemblyControllerWithPubSub;
import javacsw.services.ccs.JHcdStatusMatcherActor;
import javacsw.services.loc.JLocationSubscriberActor;
import javacsw.services.pkg.JSupervisor;
import javacsw.util.config.JPublisherActor;
import csw.services.loc.LocationService.ResolvedAkkaLocation;
import csw.services.pkg.Supervisor.LifecycleFailureInfo;

import java.util.*;
import java.util.stream.Collectors;

import static javacsw.services.ccs.JValidation.Valid;
import static scala.compat.java8.OptionConverters.toJava;

/**
 * A Java based test assembly that just forwards configs to HCDs based on prefix
 */
@SuppressWarnings({"FieldCanBeLocal", "OptionalUsedAsFieldOrParameterType", "unused"})
public class Assembly1 extends JAssemblyControllerWithPubSub {
  private final AssemblyInfo info;

  private final LoggingAdapter log = Logging.getLogger(context().system(), this);

  // The HCD actors (located via the location service)
  private Map<Connection.AkkaConnection, ResolvedAkkaLocation> connections = new HashMap<>();

  // Holds the current HCD states, used to answer requests
  private Map<String, StateVariable.CurrentState> stateMap = new HashMap<>();

  /**
   * @param info contains information about the assembly and the components it depends on
   * @param supervisor a reference to this component's supervisor actor
   */
  public Assembly1(AssemblyInfo info, ActorRef supervisor) {
    super(info);
    this.info = info;

    ActorRef trackerSubscriber = context().actorOf(LocationSubscriberActor.props());
    trackerSubscriber.tell(JLocationSubscriberActor.Subscribe, self());
    LocationSubscriberActor.trackConnections(info.connections(), trackerSubscriber);

    // Receive actor messages
    receive(defaultReceive().orElse(ReceiveBuilder.
      match(LocationService.Location.class, location -> {
        if (location instanceof ResolvedAkkaLocation) {
          ResolvedAkkaLocation l = (ResolvedAkkaLocation) location;
          if (l.getActorRef().isPresent() && l.isResolved()) {
            connections.put(l.connection(), l);
            log.info("Got actorRef: " + l.getActorRef().get());
            if (connections.size() == 2)
              supervisor.tell(JSupervisor.Initialized, self());

            // XXX TODO FIXME: replace with telemetry
            l.getActorRef().get().tell(JPublisherActor.Subscribe, self());
          }
        }
      }).

      match(CurrentState.class, this::updateCurrentState).

      matchEquals(JSupervisor.Running, x -> log.info("Received running")).

      matchEquals(JSupervisor.RunningOffline, x -> log.info("Received running offline")).

      matchEquals(JSupervisor.DoRestart, x -> log.info("Received dorestart")).

      matchEquals(JSupervisor.DoShutdown, x -> {
        log.info("Received doshutdown");
        // Just say complete for now
        supervisor.tell(JSupervisor.ShutdownComplete, self());
      }).

      match(LifecycleFailureInfo.class, x -> log.info("Received failed state: " + x.state() + "for reason: " + x.reason())).


      match(SequentialExecutor.ExecuteOne.class, t -> {
        SetupConfig sc = t.sc();
        Optional<ActorRef> commandOriginator = toJava(t.commandOriginator());
        getActorRef(sc.prefix()).ifPresent(hcdActorRef -> {
          // Submit the config to the HCD
          hcdActorRef.tell(new HcdController.Submit(sc), self());
          // If a commandOriginator was given, start a matcher actor that will reply with the command status
          commandOriginator.ifPresent(replyTo ->
            context().actorOf(JHcdStatusMatcherActor.props(new DemandState(sc), hcdActorRef, replyTo))
          );
        });
      }).


      matchAny(x -> log.error("Unexpected message: " + x)).build())
    );

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
  public List<Validation.Validation> setup(SetupConfigArg sca, Optional<ActorRef> commandOriginator) {
    // Returns validations for all
    List<Validation.Validation> validations = validateSequenceConfigArg(sca);
    if (Validation.isAllValid(validations)) {
      context().actorOf(SequentialExecutor.props(self(), sca, commandOriginator));
    }
    return validations;
  }

  /**
   * Returns the ActorRef for a component that is resolved and matches the config's prefix (if found)
   */
  private Optional<ActorRef> getActorRef(String targetPrefix) {
    Optional<ActorRef> result = Optional.empty();
    for (ResolvedAkkaLocation c : connections.values()) {
      if (c.prefix().equals(targetPrefix)) {
        result = c.getActorRef();
        break;
      }
    }
    return result;
  }

  /**
   * Performs the initial validation of the incoming SetupConfgiArg
   */
  private List<Validation.Validation> validateSequenceConfigArg(SetupConfigArg sca) {
    return sca.getConfigs().stream().map(this::validateOneSetupConfig).collect(Collectors.toList());
  }

  private Validation.Validation validateOneSetupConfig(SetupConfig sc) {
    if (sc.exists(Hcd2.filterKey) || sc.exists(Hcd2.disperserKey)) return Valid;
    else return new Validation.Invalid(new Validation.WrongConfigKeyIssue("Expected a filter or disperser config"));
  }
}

