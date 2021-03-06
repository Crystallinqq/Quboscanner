package qubo.net;

import java.io.Closeable;
import java.io.IOException;
import java.net.*;

class TCPPinger {

    // try different ports in sequence, starting with 80 (which is most probably not filtered)
    private static final int[] PROBE_TCP_PORTS = {80, 80, 443, 8080, 22, 7};
    private final int timeout;
    public TCPPinger(int timeout) {
        this.timeout = timeout;
    }

    public boolean ping(InetAddress address, int count) {
        //PingResult result = new PingResult(subject.getAddress(), count);

        Socket socket;
        for (int i = 0; i < count && !Thread.currentThread().isInterrupted(); i++) {
            socket = new Socket();
            // cycle through different ports until a working one is found
            int probePort = PROBE_TCP_PORTS[i % PROBE_TCP_PORTS.length];
            // change the first port to the requested one, if it is available
            /*if (i == 0 && subject.isAnyPortRequested())
                probePort = subject.requestedPortsIterator().next();*/

            long startTime = System.currentTimeMillis();
            try {
                // set some optimization options
                socket.setReuseAddress(true);
                socket.setReceiveBufferSize(32);
                //int timeout = result.isTimeoutAdaptationAllowed() ? min(result.getLongestTime() * 2, this.timeout) : this.timeout;
                socket.connect(new InetSocketAddress(address, probePort), timeout);
                if (socket.isConnected()) {
                    // it worked - success
                    //success(result, startTime);
                    closeQuietly(socket);
                    return true;
                    // it worked! - remember the current port
                    //workingPort = probePort;
                }
            }
            catch (SocketTimeoutException ignore) {
            }
            catch (NoRouteToHostException e) {
                // this means that the host is down
                break;
            }
            catch (IOException e) {
                String msg = e.getMessage();

                // RST should result in ConnectException, but not all Java implementations respect that
                if (msg.contains(/*Connection*/"refused")) {
                    // we've got an RST packet from the host - it is alive
                    closeQuietly(socket);
                    return true;
                    //success(result, startTime);
                }
                else
                    // this should result in NoRouteToHostException or ConnectException, but not all Java implementation respect that
                    if (msg.contains(/*No*/"route to host") || msg.contains(/*Host is*/"down") || msg.contains(/*Network*/"unreachable") || msg.contains(/*Socket*/"closed")) {
                        // host is down
                        break;
                    }
                    /*else {
                        // something unknown
                        //LOG.log(FINER, subject.toString(), e);
                    }*/
            }
            finally {
                closeQuietly(socket);
            }
        }

        return false;
    }

    private static void closeQuietly(Socket socket) {
        if (socket != null) try {
            socket.close();
        }
        catch (IOException ignore) {
        }
    }


    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        }
        catch (IOException ignore) {
        }
    }
}