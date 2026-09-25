# Agent Guidelines & Environment Configuration

## Build & Environment Rules

- **Java Environment on macOS**:
  - When the host operating system is macOS (Darwin), commands running inside the sandbox cannot locate Java via the system launcher `/usr/libexec/java_home`.
  - When executing `./gradlew` or Java commands on macOS, always explicitly specify `JAVA_HOME`:
    ```bash
    JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk-21.jdk/Contents/Home ./gradlew <tasks>
    ```
  - On non-macOS host systems (e.g., Linux, Windows/WSL), do NOT use this macOS-specific path; use the system default or host-configured `JAVA_HOME`.
