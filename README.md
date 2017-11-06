## varanus: _A set of applications for providing resilient communications in a Software-Defined Network_

### Overview
TODO

### Requisites
For all applications:
- 64-bit Linux-based operating system with a bash shell

For the sdncontroller and collector applications:
- Java (8 or higher) for building and running the applications
- Ant build tool for building the applications
- Libpcap for running the collector application

For the network-manager application:
- Mininet (v.2.2.2 or higher) for running the application
- Open vSwitch (v2.5 or higher) for managing software switches in the local machine

For the network-visualiser application:
- Tomcat (v8.5 or higher) for running the local server
- Java (8 or higher) for running the tomcat server and for building and running the xml-proxy
- Web browser with enabled JavaScript for interacting with the application

### Demo instructions
Build all Java applications:
<big><pre>
$ cd &lt;repository directory&gt;
$ ant pack-all
</pre></big>

Open a terminal and launch the network manager as root:
<big><pre>
$ cd &lt;repository directory&gt;
$ ./demo/launch\_network.sh
</pre></big>

Open another terminal and launch the network visualiser:
<big><pre>
$ cd &lt;repository directory&gt;
$ ./demo/launch\_visualiser.sh
</pre></big>

Open a web browser and go to 'localhost:8080/visualiser' (default password is "password").
### Contributors
Ricardo Fonseca

Eric Vial

Nuno Neves

Fernando Ramos

