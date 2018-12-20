package com.github.lkq.instadocker.docker;

import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.net.ServerSocket;

public class PortFinder {
    public static int find() {
        int port = 0;
        int retry = 10;
        while (port == 0 && retry-- > 0) {
            try {
                ServerSocket socket = new ServerSocket(0);
                port = socket.getLocalPort();
                socket.close();
                return port;
            } catch (IOException ignored) {
            }
        }
        throw new NotFoundException("can't find available port after 10 retries");
    }
}
