# Multicast Group Chat

A command-line group chat written in Java using UDP multicast. Everyone who runs the program with the same multicast address and port can send and receive messages in a shared chat room, with no central server.

## Features

- Serverless group chat over UDP multicast (`MulticastSocket`)
- Each user picks a display name at startup
- Messages are shown as `name: message`
- Separate receiver thread, so incoming messages appear while you type
- Input validation for the multicast address and port range
- Type `Exit` to leave the chat

## Requirements

- Java 8 or newer (JDK for compiling)
- A network that allows multicast traffic (e.g. the same LAN, or several terminals on one machine)

## Compile

```bash
javac GroupChat.java
```

## Usage

```bash
java GroupChat <multicast-address> <port-number>
```

| Argument | Description |
|----------|-------------|
| `<multicast-address>` | An IPv4 multicast address in the range `224.0.0.0` to `239.255.255.255` |
| `<port-number>` | A port between `1024` and `65535` |

### Example

Open two or more terminals (or run on several machines on the same network) and start each with the same arguments:

```bash
java GroupChat 230.0.0.1 1234
```

Each instance prompts for a name:

```
Enter your name: Alice
Start typing messages.....
```

Anything one participant types is delivered to all the others. Type `Exit` (case-sensitive) to quit.

## How It Works

- **`GroupChat`** (main class): validates the arguments, reads your name, creates a `MulticastSocket`, starts the receiver thread, and then loops reading lines from standard input. Each line is prefixed with your name and sent as a `DatagramPacket` addressed to the multicast group.
- **`ReadThread`**: runs in the background, blocking on `socket.receive()`. Received packets are decoded as UTF-8 and printed to the console. Messages beginning with your own name are skipped, so you don't see an echo of what you sent.
- **Shutdown**: typing `Exit` sets a shared `volatile` flag (`finished`), leaves the group, and closes the socket, which also unblocks and ends the receiver thread.
- **TTL**: the time-to-live is set to `1`, so packets stay on the local network and are not forwarded by routers.

## Limits and Notes

- Maximum message size is 1000 bytes (the receive buffer size in `ReadThread`); longer messages are truncated.
- Messages are sent over UDP, so delivery and ordering are not guaranteed.
- There is no encryption or authentication. Anyone on the network who knows the address and port can read and post messages.
- Your own messages are filtered by checking whether a message starts with your name, so another user whose name begins with yours (e.g. "Al" and "Alice") may have their messages hidden from you.

## Known Issues

- **The socket never joins the multicast group.** The code calls `socket.leaveGroup(group)` on exit but never calls `socket.joinGroup(group)` after creating the socket. On many systems this means other participants' messages will not be received, and leaving can throw a `SocketException`. Adding `socket.joinGroup(group);` right after `socket.setTimeToLive(1);` in `main` fixes this.
- **Usage message mismatch.** When the wrong number of arguments is given, the program prints `<name> <port-number>`, but the first argument is actually the multicast address (the name is entered at the prompt).

## License

MIT
