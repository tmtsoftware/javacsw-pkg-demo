#!/bin/sh
exec scala "$0" "$@"
!#

// Demonstrates starting the java versions of the test containers.
// This script assumes that the csw/install/bin directory is in the shell path.

import scala.sys.process._

// Start the ZMQ based hardware simulation
"mtserver2 filter".run
"mtserver2 disperser".run

// Start the containers with the default configuration
"container1java".run
"container2java".run

