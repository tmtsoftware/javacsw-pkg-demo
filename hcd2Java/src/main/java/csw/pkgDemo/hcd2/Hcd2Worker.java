package csw.pkgDemo.hcd2;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import akka.japi.pf.ReceiveBuilder;
import akka.util.ByteString;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import csw.util.cfg.Key;
import csw.util.cfg.StateVariable.CurrentState;
import csw.util.cfg.Configurations.*;
import csw.util.cfg.StandardKeys;
import javacsw.util.cfg.JConfigurations;
import javacsw.util.cfg.JCurrentState;
import javacsw.util.cfg.JSetupConfig;
import javacsw.util.cfg.JStandardKeys;
import org.zeromq.ZMQ;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.OptionalInt;

import static csw.pkgDemo.hcd2.Hcd2Worker.Msg.RequestCurrentState;


/**
 * An actor that does the work of matching a configuration
 */
@SuppressWarnings("WeakerAccess")
public class Hcd2Worker extends AbstractActor {

    /**
     * Used to create the Hcd2Worker actor
     *
     * @param prefix the HCD's prefix, used in configurations
     * @return the Props needed to create the actor
     */
    public static Props props(final String prefix) {
        return Props.create(new Creator<Hcd2Worker>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Hcd2Worker create() throws Exception {
                return new Hcd2Worker(prefix);
            }
        });
    }

    // Available choices
    private static final String[] FILTERS = new String[]{"None", "g_G0301", "r_G0303", "i_G0302", "z_G0304", "Z_G0322", "Y_G0323", "u_G0308"};
    private static final String[] DISPERSERS = new String[]{"Mirror", "B1200_G5301", "R831_G5302", "B600_G5303", "B600_G5307", "R600_G5304", "R400_G5305", "R150_G5306"};


    // Message requesting current state of HCD values
    public enum Msg {
        RequestCurrentState
    }

    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    // Load the settings from the resources/zmq.conf file
    Config settings = ConfigFactory.load("zmq");

    // The HCD's prefix
    private final String prefix;

    // The array of choices for the HCD values (filter or disperser choices)
    private final String[] choices;

    // The key to set the filter or disperser value in the config
    private final Key key;

    // Reference to actor that talks to the zmq process
    private final ActorRef zmqClient;

    //  -- Actor state while talking to the ZMQ process --

    // currentPos The current position (index in filter or disperser array)
    private int currentPos = 0;

    // The demand (requested) position (index in filter or disperser array)
    private int demandPos = 0;

    // Actor constructor is private: use props() method above.
    private Hcd2Worker(String prefix) {
        this.prefix = prefix;

        // The key used to talk to ZML
        String zmqKey = prefix.substring(prefix.lastIndexOf('.') + 1);

        // The key and list of choices used in configurations and CurrentState objects
        if (zmqKey.equals("filter")) {
            key = JStandardKeys.filter;
            choices = FILTERS;
        } else {
            key = JStandardKeys.disperser;
            choices = DISPERSERS;
        }

        // Get the ZMQ client
        String url = settings.getString("zmq." + zmqKey + ".url");
        log.info("For " + zmqKey + ": using ZMQ URL = " + url);
        zmqClient = getContext().actorOf(ZmqClient.props(url));

        // Receive actor messages
        receive(ReceiveBuilder.
                match(SetupConfig.class, this::handleSetupConfig).
                match(ByteString.class, this::handleZmqMessage).
                matchEquals(RequestCurrentState, x -> handleRequestCurrentState()).
                matchAny(t -> log.warning("Unknown message received: " + t)).
                build());
    }

    // Action when receiving a SetupConfig
    private void handleSetupConfig(SetupConfig setupConfig) {
        Optional<String> value = new JSetupConfig(setupConfig).getAsString(key);
        if (value.isPresent()) {
            int pos = Arrays.asList(choices).indexOf(value.get());
            setPos(currentPos, pos);
        }
    }

    // If the demand pos is not equal to the current pos, increment the position
    // (to simulate the filter or disperser wheel turning one position at a time
    // while updating the telemetry)
    private void setPos(int currentPos, int demandPos) {
        this.currentPos = currentPos;
        this.demandPos = demandPos;
        if (demandPos != currentPos) {
            zmqClient.tell(ZmqClient.Msg.Move, self());
        }
    }

    //  The reply from ZMQ should be the index of the current filter or disperser
    private void handleZmqMessage(ByteString reply) {
        int pos = Integer.parseInt(reply.decodeString(ZMQ.CHARSET.name()));
        log.info("ZMQ current pos: " + pos);
        String value = choices[pos];
        JCurrentState state = JConfigurations.createCurrentState(prefix).set(key, value);
        getContext().parent().tell(state.configType(), self());
        setPos(pos, demandPos);
    }

    // Send the parent the current state
    private void handleRequestCurrentState() {
        JCurrentState state = JConfigurations.createCurrentState(prefix).set(key, choices[currentPos]);
        getContext().parent().tell(state, self());
    }
}

