# ftbq-better-fluids

FTBQ Better Fluids (FBF) is a mod for Forge + Minecraft 1.20.1 that adds a new `Fluid+` task type to the FTB Quests questbook mod.

This task type differs from the built-in Fluid type in two main ways:

* A `Fluid+` task completes as soon as the player holds any item in their inventory or hotbar that implements the Forge fluid handler capability. This includes buckets as well as most modded tanks, cells, drums, etc.
* A `Fluid+` task can have its recipes looked up in the installed recipe viewer like item tasks can.

Recipe viewing is implemented in FTB-Quests 1.20.4, but it's not been backported to 1.20.1.

## Usage

Quest-book authors can add `Fluid+` types to their questbook. The workflow is almost identical to Fluid quests:

![./img/menu.png]

When the player views the quest, they can click on the fluid and see its recipe in their recipe viewer. The info text is similar to item quests:

![./img/detail.png]