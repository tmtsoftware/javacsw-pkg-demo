package csw.pkgDemo.hcd2;

import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import csw.services.pkg.Component;
import csw.util.akka.SetLogLevelActor;
import csw.util.param.Parameters.Setup;
import csw.util.param.StringKey;
import javacsw.services.ccs.JHcdController;
import javacsw.services.pkg.JSupervisor;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

// A test HCD that is configured with the given name and config path
@SuppressWarnings({"WeakerAccess", "unused"})
public class Hcd2 extends JHcdController {
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

  // Hcd2Worker actor used to process configs
  private final ActorRef worker;

  // Handle SetLogLevel message as this was removed from the supervisor at some point
  private Receive logLevelReceive() {
    return receiveBuilder().
            match(SetLogLevelActor.SetLogLevel.class, logLevel -> {
              log.info("Received SetLogLevel("+logLevel.level()+")");
              try {
                Logger l = (Logger) LoggerFactory.getLogger(logLevel.rootPackage());
                l.setLevel(Level.toLevel(logLevel.level()));
              } catch (Exception e) {
                log.error(e, "Failed to set log level");
              }
            }).build();
  }

  /**
   * Creates the Hcd2 actor
   *
   * @param info the HCD's prefix, used in parameters
   * @param supervisor the HCD's supervisor actor
   */
  private Hcd2(final Component.HcdInfo info, ActorRef supervisor) {

    worker = getContext().actorOf(Hcd2Worker.props(info.prefix()));

    supervisor.tell(JSupervisor.Initialized, self());
  }

  @Override
  public Receive createReceive() {
    return jControllerReceive().orElse(logLevelReceive());
  }

  @Override
  // Send the config to the worker for processing
  public void process(Setup s) {
    worker.tell(s, self());
  }

  @Override
  // Ask the worker actor to send us the current state (handled by parent trait)
  public void requestCurrent() {
    worker.tell(Hcd2Worker.Msg.RequestCurrentState, self());

  }
}
