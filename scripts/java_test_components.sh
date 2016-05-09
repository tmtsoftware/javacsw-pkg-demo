#!/bin/sh
exec scala "$0" "$@"
!#

// Demonstrates starting the test components without containers along with the location service.
// This script assumes that the csw/install/bin directory is in the shell path.

import scala.sys.process._

// Start the ZMQ based hardware simulation
"mtserver2 filter".run
"mtserver2 disperser".run

// Start Assembly-1
"assembly1java".run

// Start the test HCDs
"hcd2java HCD-2A".run
"hcd2java HCD-2B".run

