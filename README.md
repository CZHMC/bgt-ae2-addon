# Building Gadgets 2 AE2 Addon

[中文文档](README_zh_CN.md)

An Applied Energistics 2 addon for Building Gadgets 2 that requests missing building materials from AE2's native autocrafting flow.

> Requires Building Gadgets 2, Applied Energistics 2, and NeoForge for Minecraft 1.21.1.

## Features

- Integrates AE2 autocrafting with the Building Gadgets 2 `BUILD` and `EXCHANGE` queues.
- Covers the normal and replacement paste stages of Copy-Paste Gadget queues when BGT uses those queues.
- Scans the complete remaining BGT queue before requesting materials.
- Aggregates missing items by AE2 item key, so repeated blocks are requested as one batch.
- Preserves BGT's source order when checking available materials: AE2 storage, the bound inventory, Curios, then the player's inventory.
- Uses AE2's native quantity and crafting-plan menus. No custom crafting GUI is added.
- Keeps the BGT queue paused until the native crafting batch has been submitted and the materials are available to BGT.

The addon does not submit crafting jobs silently in the background. The player confirms the amount in AE2's native amount screen, reviews each native crafting plan, and presses **Start** before a job is submitted.

## Supported Scope

Supported:

- Building Gadget (`BUILD`)
- Exchanging Gadget (`EXCHANGE`)
- Copy-Paste Gadget normal paste and replacement paste stages that use BGT's `BUILD`/`EXCHANGE` queues

Not handled by this addon:

- Cut-Paste Gadget (`CUT`)
- Destruction (`DESTROY`)
- Undo-destruction (`UNDO_DESTROY`)
- Queues with `needItems=false`
- Fluid materials
- Creative-mode players

Unsupported queues are left to Building Gadgets 2's original behavior.

## Requirements

| Component | Version |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.248 |
| Applied Energistics 2 | 19.2.17 or newer, below 20 |
| Building Gadgets 2 | 1.3.9 or newer |
| GuideME | 21.1.1 or newer when running AE2 in the development environment |

The addon targets BGT 1.3.9's `ServerTickHandler` `build` and `exchange` methods. A future BGT release that changes those methods, queue fields, or processing order may require an addon update.

## How It Works

1. Bind a Building Gadgets 2 gadget to an active AE2 Wireless Access Point.
2. Make sure the AE2 network has a pattern that can craft the required block item.
3. Start a supported BGT building or exchange operation.
4. When the remaining queue lacks materials, the addon scans all remaining positions and aggregates the missing quantities.
5. Confirm the quantities in AE2's native amount screen.
6. Review and start the native AE2 crafting plans.
7. Once the complete batch is submitted and its materials are available, BGT resumes its normal extraction and placement process.

AE2 can plan one requested item key per crafting plan. For several different missing items, AE2's native queue presents and submits those plans sequentially. The addon keeps the complete BGT batch paused throughout that sequence.

## No Public API

This project is an implementation addon, not an API library. It does not expose a stable public API for other mods. Classes and packages under `com.czhmc.bgt_ae2_addon` are internal implementation details and may change without compatibility guarantees.

## Development

```text
./gradlew test
./gradlew build
```

The development runtime also needs the dependencies declared in `build.gradle`, including GuideME for AE2's runtime dependency chain.

## Issues

Please open an issue with:

- Minecraft, NeoForge, AE2, and BGT versions;
- the operation type (`BUILD` or `EXCHANGE`);
- a concise reproduction;
- the relevant `latest.log` excerpt, with personal data removed.

Modrinth and CurseForge publishing are intentionally not configured yet.

## License

The addon source code in this repository is licensed under the [MIT License](LICENSE).

Third-party dependencies remain under their own licenses. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for the dependency and attribution boundary.

## Credits

- [Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2)
- [Building Gadgets 2](https://github.com/Direwolf20-MC/BuildingGadgets2)
- [GuideME](https://github.com/AppliedEnergistics/GuideME)
- [NeoForge](https://github.com/neoforged/NeoForge)
