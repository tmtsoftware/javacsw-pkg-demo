// This simple application represents something like a filter or disperser wheel with a given number of positions.
// You can send a message to increment the current position (with wrap-around) and you receive a reply with
// the current position index.

#include "zhelpers2.h"
#include <pthread.h>
#include <signal.h>

static char* name;
static int numPositions = 8; // Should match the number of filters or dispersers...
static int currentPos = 0;

static int s_interrupted = 0;
static void s_signal_handler (int signal_value)
{
    s_interrupted = 1;
}

static void s_catch_signals (void)
{
    struct sigaction action;
    action.sa_handler = s_signal_handler;
    action.sa_flags = 0;
    sigemptyset (&action.sa_mask);
    sigaction (SIGINT, &action, NULL);
    sigaction (SIGTERM, &action, NULL);
}

static void *
worker_routine (void *context) {
    //  Socket to talk to dispatcher
    void *receiver = zmq_socket (context, ZMQ_REP);
    zmq_connect (receiver, "inproc://workers");

    while (!s_interrupted) {
        char *inputStr = s_recv (receiver);
        if (inputStr) {
            //  Simulate the filter or disperser wheel rotating and send back the current position until we reach the target
            printf ("%s: Received request: [%s]\n", name, inputStr);
            int value = atoi(inputStr);
            free (inputStr);
            currentPos = (currentPos + value) % numPositions;
            usleep(500000);
            //  Send reply back to client
            printf("%s: Sending current position: [%d]\n", name, currentPos);
            char msg[16];
            sprintf(msg, "%d", currentPos);
            s_send(receiver, msg);
        } else {
            break;
        }
    }
    if (s_interrupted) printf("Interrupted\n");
    zmq_close (receiver);
    return NULL;
}

int main (int argc, char** argv)
{
    if (argc != 2) {
	printf("Expected one argument: filter or disperser\n");
	exit(1);
    }
    // Use a different port depending on the argument (filter, disperser)
    // Make sure this matches the values in resources/zmq.conf.
    // Later on, this should be read from a config file or service.
    char* url = "tcp://*:6565";
    if (strcmp(argv[1], "filter") == 0) {
        url = "tcp://*:6565";
    } else if (strcmp(argv[1], "disperser") == 0) {
        url = "tcp://*:6566";
    }
    printf("Listening for %s commands on %s\n", argv[1], url);

    void *context = zmq_init (1);
    s_catch_signals ();

    //  Socket to talk to clients
    void *clients = zmq_socket (context, ZMQ_ROUTER);
    zmq_bind (clients, url);

    //  Socket to talk to workers
    void *workers = zmq_socket (context, ZMQ_DEALER);
    zmq_bind (workers, "inproc://workers");

    //  Launch pool of worker threads
    int thread_nbr;
    for (thread_nbr = 0; thread_nbr < 5; thread_nbr++) {
        pthread_t worker;
        pthread_create (&worker, NULL, worker_routine, context);
    }
    //  Connect work threads to client threads via a queue
    zmq_device (ZMQ_QUEUE, clients, workers);

    //  We never get here but clean up anyhow
    zmq_close (clients);
    zmq_close (workers);
    zmq_term (context);
    return 0;
}
