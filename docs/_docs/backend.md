---
title: Java Backend
---

By using [Eclipse IDE](https://www.eclipse.org/) import project: File > Import... > Gradle > 'Existing Gradle Project'. 

In directory 'launch' are some eclipse launch configurations: File > Import... > 'Run/Debug' > 'Launch Configuration'.

| launch config | description | executed command |
| ------------- | ------------- | ------------- |
| **rsdb - server**  | start server  | run java main class 'run.terminal' with argument 'server' |
| **rsdb - build**  | build package  | run gradle task '_package' |

Java source files are in folder `src`.

[ANTLR](https://www.antlr.org/) grammar files are in folder `dsl` and generated java file are in folder `dsl/generated-sources`.