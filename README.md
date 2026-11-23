# Instant Equip

Fabric mod (client-side) for **Minecraft 1.21.11**.

When you switch to an item (mace, sword, axe, ...) the hand raise animation is removed:
the item appears instantly fully raised, without the "pull out" movement.

## Requirements

- Minecraft 1.21.11
- Fabric Loader 0.18.0+
- Java 21
- No Fabric API needed

## Build

```
gradle build
```

The jar is created in `build/libs/` (use the one without `-sources`).
The GitHub Action in `.github/workflows/build.yml` builds it on every push and
uploads the jar as an artifact.

## Notes

- Applies to the main hand and to every item, not only weapons.
- Only the raise animation is removed. Swing/attack animations are unchanged.
