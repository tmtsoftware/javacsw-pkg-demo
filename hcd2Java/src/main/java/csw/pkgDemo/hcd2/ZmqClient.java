package csw.pkgDemo.hcd2;

import akka.actor.Props;
import akka.util.ByteString;
import org.zeromq.ZMQ;
import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import akka.japi.pf.ReceiveBuilder;

/**
 * Actor that handles talking to zmq based process
 * (Using Jeromq, since akka-zeromq not available in Scala-2.11.)
 */
@SuppressWarnings({"WeakerAccess", "FieldCanBeLocal", "unused"})
public class ZmqClient  extends AbstractActor {

    /**
     * Used to create the ZmqClient actor
     *
     * @param url the URL of the zmq process
     * @return the Props needed to create the actor
     */
    public static Props props(final String url) {
        return Props.create(new Creator<ZmqClient>() {
            private static final long serialVersionUID = 1L;

            @Override
            public ZmqClient create() throws Exception {
                return new ZmqClient(url);
            }
        });
    }

    // Message received by the actor to move the wheel by 1 position
    public enum Msg {
        Move
    }

    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    // Message sent to ZMQ to increment the position by 1
    byte[] zmqMsg = "1".getBytes(ZMQ.CHARSET);

    private final String url;
    private final ZMQ.Socket socket;

    private ZmqClient(String url) {
        this.url = url;

        ZMQ.Context zmqContext = ZMQ.context(1);
        socket = zmqContext.socket(ZMQ.REQ);
        socket.connect(url);

        // TODO: Set timeout for receive and handle timeout message
//        getContext().setReceiveTimeout(timeout);

        receive(ReceiveBuilder.
                matchEquals(Msg.Move, m -> move()).
                matchAny(t -> log.warning("Unknown message received: " + t)).
                build());
    }

    private void move() {
        socket.send(zmqMsg, 0);
        byte[] reply = socket.recv(0);
        sender().tell(ByteString.fromArray(reply), self());
    }


}

