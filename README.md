# Instant Equip

Fabric mod (client-side) for **Minecraft 1.21.11**.

When you switch to an item (mace, sword, axe, ...) the hand raise animation is removed:
the item appears instantly fully raised, without the "pull out" movement.

## Menu

Press **K** (rebindable under Controls -> Instant Equip) to open the menu.
There you can switch the effect ON/OFF per item type: Mace, Swords, Axes, Pickaxes,
Shovels, Hoes, Spears, Trident, Bow, Crossbow, Shield, Blocks, Empty hand and Other items.
Settings are saved in `config/instantequip.json`.

## Download

Get the latest jar from the [Releases](../../releases) page.

## Requirements

- Minecraft 1.21.11
- Fabric Loader 0.18.0+
- Fabric API
- Java 21

## Build

```
gradle build
```

The jar is created in `build/libs/` (use the one without `-sources`).
