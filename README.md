# Simple application server in Java

My first Java implementation of a Server (it was 2016). It features:
1. A HTTP server
2. A chat server (with web sockets)

```bash
javac -sourcepath src -d out Application.java
java -classpath out Application [-chat] [-http] [chat port] [http port]
```
- ```-chat``` disables the chat server
- ```-http``` disables the HTTP server
- ```chat port``` runs the chat server on the given port (default: 12345)
- ```http port``` runs the HTTP server on the given port (default: 9000)

To connect to the chat server with a client, you can use ```telnet``` or ```nc```:
```bash
nc 127.0.0.1 12345
```
You will be prompted to chose between:
- **ECHO_SERVER**: echoes the messages you send
- **REVERSE_SERVER**: reverse the messages you send
- **BROADCAST_SERVER**: forwards the messages you send to all the connected sockets

