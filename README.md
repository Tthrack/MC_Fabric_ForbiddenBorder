# Forbidden Border (Fabric, server-side)

Minecraft 1.21.1 Fabric server-side mod (Java 21) that provides an **inverted cylindrical border**:

- `/border enable|disable|center|radius|status`
- Survival/Adventure players cannot enter/build inside the cylinder
- Survival/Adventure can move and build outside the cylinder
- Creative/Spectator can pass both directions
- Creative can build inside
- Barrier particle visualization on the cylinder perimeter

## Commands

All commands require permission level 2 (`op`).

- `/border enable`
- `/border disable`
- `/border center` (uses your current X/Z)
- `/border center <x> <z>`
- `/border radius <value>`
- `/border status`

## Build

```bash
gradle build
```

Jars are written to `build/libs`.

> If you prefer using `./gradlew`, regenerate the Gradle wrapper jar locally with:
>
> `gradle wrapper --gradle-version 8.8 --no-validate-url`


GitHub Actions builds with a pinned Gradle 8.8 distribution.
