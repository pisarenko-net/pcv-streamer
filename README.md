# pcv-streamer
Real-time Power Commander V streamer (internal combustion engine data). A hackathon project the goal of which was to read statistics from a Dynojet Power Commander 5 and send that data to AWS.

The project is described at http://pisarenko.net/blog/2017/04/16/internet-connected-motorcycle-project/

The code is organized as a set of small Java programs, each of which can be built with maven. The `pcv-app` ties everything together and requires dependencies to be built and installed beforehand:

    $ cd pcv-mqtt-streamer/ ; mvn clean install
    $ cd pcv-usb-comm/ ; mvn clean install
    $ cd pcv-usb-dump-visualizer/ ; mvn clean install
    $ mvn package
