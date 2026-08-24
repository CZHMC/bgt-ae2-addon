# Third-Party Notices

This repository contains the source code of **Building Gadgets 2 AE2 Addon**. The addon is distributed under the MIT License in [`LICENSE`](LICENSE).

The following projects are dependencies of the runtime or development environment. Their names, trademarks, source code, assets, and licenses remain the property of their respective authors. This addon does not include copies of their source code or assets.

## Applied Energistics 2

- Project: [Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2)
- Used as: required runtime dependency and API integration target
- License information published by the project: **LGPLv3, MIT, and CC BY-NC-SA 3.0**, depending on the component
- License details: [AE2 license information](https://github.com/AppliedEnergistics/Applied-Energistics-2?tab=readme-ov-file#license)

This addon links against AE2 APIs and uses AE2 runtime menus. It does not relicense AE2 code.

## Building Gadgets 2

- Project: [Building Gadgets 2](https://github.com/Direwolf20-MC/BuildingGadgets2)
- Used as: required runtime dependency and placement-flow integration target
- License declared by the project build metadata: **MIT**

This addon integrates with the public behavior of Building Gadgets 2 and does not include or relicense its source code.

## GuideME

- Project: [GuideME](https://github.com/AppliedEnergistics/GuideME)
- Used as: AE2's required runtime dependency in the development environment
- License declared by the project: **LGPL**, with some embedded libraries under MIT and Apache License 2.0
- License details: [GuideME license section](https://github.com/AppliedEnergistics/GuideME#license)

## NeoForge and Minecraft

- [NeoForge](https://github.com/neoforged/NeoForge) is the mod loader and development platform; its repository is licensed under **LGPL-2.1**.
- Minecraft is a separate proprietary product of Mojang Studios/Microsoft.

Neither NeoForge nor Minecraft is included in this repository. Their own terms and licenses apply to their respective software.

## Scope

The addon does not expose a stable public API for other mods. Packages and classes are implementation details and may change without compatibility guarantees.
