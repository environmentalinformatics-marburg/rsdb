---
title: Administration
---

## Running RSDB Server in foreground

RSDB can be run directly from bash terminal. Server stops when terminal is closed.

~~~ bash
# Start server in foreground.
# Stop server and go back to commandline by pressing "ctrl c".
./rsdb.sh server
~~~

## Running RSDB Server in background

Running RSDB Server in background can be managed by `screen` [Screen](https://help.ubuntu.com/community/Screen) command.
Screen is a terminal multiplexer. You can create a new terminal session, switch it to background and later switch that session back to foreground.

---

From normal terminal start new screen session with name *rsdb* and bring that session into foreground:

~~~ bash
screen -S rsdb
~~~

---

Start **RSDB server** in that *rsdb screen session*:

~~~ bash
./rsdb.sh server
~~~

---

Switch *rsdb screen session* into background (**RSDB server** keeps running) and go back to normal terminal:

Key: `ctrl+a` `d` (press and hold `ctrl`, press `a`, release both, press and release `d`)

---

From normal terminal switch to *rsdb screen session* again:

~~~ bash
screen -r rsdb
~~~

---

In *rsdb screen session* stop **RSDB server**:

Key: `ctrl+c` (press and hold `ctrl`, press `c`, release both)

---

In *rsdb screen session* (and stopped **RSDB server**) close *rsdb screen session* and go back to normal terminal:

~~~ bash
exit
~~~

---