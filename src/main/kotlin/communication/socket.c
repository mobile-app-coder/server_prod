#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <fcntl.h>
#include <sys/socket.h>
#include <poll.h>
#include <errno.h>

#define PORT 5000
#define BUFFER_SIZE 1024
#define MAX_CLIENTS 1000
int server_socket;

void setNonBlocking(int socket_fd) {
    int flags = fcntl(socket_fd, F_GETFL, 0);
    if (flags == -1) {
        perror("fcntl(F_GETFL)");
        return;
    }
    if (fcntl(socket_fd, F_SETFL, flags | O_NONBLOCK) == -1) {
        perror("fcntl(F_SETFL)");
    }
}

JNIEXPORT jint JNICALL Java_server_communication_SocketKt_createAndListen(JNIEnv *env, jobject obj) {
    struct sockaddr_in server_addr;
    server_socket = socket(AF_INET, SOCK_STREAM, 0);
    if (server_socket == -1) {
        perror("Socket creation failed");
        return -1;
    }

    int opt = 1;
    if (setsockopt(server_socket, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt)) < 0) {
        perror("Setsockopt failed");
        close(server_socket);
        return -1;
    }

    server_addr.sin_family = AF_INET;
    server_addr.sin_addr.s_addr = INADDR_ANY;
    server_addr.sin_port = htons(PORT);

    if (bind(server_socket, (struct sockaddr *)&server_addr, sizeof(server_addr)) < 0) {
        perror("Bind failed");
        close(server_socket);
        return -1;
    }

    if (listen(server_socket, 10) < 0) {
        perror("Listen failed");
        close(server_socket);
        return -1;
    }

    setNonBlocking(server_socket);
    return server_socket;
}

JNIEXPORT jint JNICALL Java_server_communication_SocketKt_acceptClient(JNIEnv *env, jobject obj) {
    struct sockaddr_in client_addr;
    socklen_t addr_len = sizeof(client_addr);
    struct pollfd pfd;
    pfd.fd = server_socket;
    pfd.events = POLLIN;

    int poll_status = poll(&pfd, 1, 100);  // Poll with timeout
    if (poll_status > 0) {
        if (pfd.revents & POLLIN) {
            int client_socket = accept(server_socket, (struct sockaddr *)&client_addr, &addr_len);
            if (client_socket >= 0) {
                setNonBlocking(client_socket);
                printf("Accepted connection from %s:%d\n",
                       inet_ntoa(client_addr.sin_addr), ntohs(client_addr.sin_port));
                return client_socket;
            } else {
                perror("Accept failed");
            }
        }
    } else if (poll_status == 0) {
        // Timeout occurred, no incoming connection
        return -1;
    } else {
        perror("Poll failed");
        return -1;
    }
    return -1;
}

JNIEXPORT jstring JNICALL Java_server_communication_SocketKt_getMessage(JNIEnv *env, jobject obj, jint client_socket) {
    if (client_socket < 0) {
        fprintf(stderr, "Invalid or closed socket: %d\n", client_socket);
        return NULL;
    }

    char buffer[BUFFER_SIZE];
    ssize_t bytes_read = recv(client_socket, buffer, sizeof(buffer) - 1, 0);

    if (bytes_read > 0) {
        buffer[bytes_read] = '\0';
        return (*env)->NewStringUTF(env, buffer);
    } else if (bytes_read == 0) {
        printf("Client disconnected: socket %d\n", client_socket);
        close(client_socket);
        return NULL;
    } else if (bytes_read < 0) {
        if (errno != EAGAIN && errno != EWOULDBLOCK) {
            perror("Recv error");
            close(client_socket);
        }
        return NULL;
    }

    return NULL;
}

JNIEXPORT jint JNICALL Java_server_communication_SocketKt_sendMessage(JNIEnv *env, jobject obj, jint client_socket, jstring message) {
    if (client_socket < 0) {
        fprintf(stderr, "Invalid socket: %d\n", client_socket);
        return -1;
    }

    const char *msg = (*env)->GetStringUTFChars(env, message, NULL);
    ssize_t bytes_sent = send(client_socket, msg, strlen(msg), 0);
    (*env)->ReleaseStringUTFChars(env, message, msg);

    if (bytes_sent < 0) {
        if (errno != EAGAIN && errno != EWOULDBLOCK) {
            perror("Send error");
            return -1;
        }
    }

    return bytes_sent;
}

int client_sockets[MAX_CLIENTS]; // Array to store client sockets


void cleanupClients() {
    for (int i = 0; i < MAX_CLIENTS; i++) {
        if (client_sockets[i] > 0) {
            close(client_sockets[i]);
            client_sockets[i] = -1;
        }
    }
}

JNIEXPORT void JNICALL Java_server_communication_SocketKt_stopServer(JNIEnv *env, jobject obj) {
    cleanupClients();
    if (server_socket != -1) {
        close(server_socket);
        printf("Server stopped.\n");
    }
}

JNIEXPORT jstring JNICALL Java_server_communication_SocketKt_getClientAddress(JNIEnv *env, jobject obj, jint client_socket) {
    struct sockaddr_in addr;
    socklen_t addr_len = sizeof(addr);

    if (getpeername(client_socket, (struct sockaddr *)&addr, &addr_len) == 0) {
        char *ip_str = inet_ntoa(addr.sin_addr);
        return (*env)->NewStringUTF(env, ip_str);
    } else {
        perror("getpeername");
        return NULL;
    }
}

JNIEXPORT jint JNICALL Java_server_communication_SocketKt_getClientPortAddress(JNIEnv *env, jobject obj, jint client_socket) {
    struct sockaddr_in addr;
    socklen_t addr_len = sizeof(addr);

    if (getpeername(client_socket, (struct sockaddr *)&addr, &addr_len) == 0) {
        return ntohs(addr.sin_port);
    } else {
        perror("getpeername");
        return -1;
    }
}


