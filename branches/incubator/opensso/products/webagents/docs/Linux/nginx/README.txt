OpenAM Policy Agent for Nginx
=============================

***THIS MODULE IN THE REVIEW PHASE***

# Platforms

Currently, This module will works Linux only.
I'm testing Red Hat Enterprise Linux 6.0 Scientific Linux 6, CentOS 6.

# Build Instructions

 1. Prepare extlib

    // TODO

 2. Build it

    $ cd products/webagents/
    $ ant nginx
    or
    $ ant nginx -Dbuild.type=64

# TODO
 * POST data handling
 * notification handling with multi processes
