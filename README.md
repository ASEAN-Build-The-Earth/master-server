# master-server
ASEAN's Master Server management & utility plugin.

## Planned Features
-[ ] Discord integration with [**DiscordSRV-Ascension**](https://github.com/DiscordSRV/Ascension)
-[ ] Geospatial data support using our (WIP) [**geotools-utils**](https://github.com/ASEAN-Build-The-Earth/geotools-utils) library.
  -[ ] Exports to multiple file formats including minecraft schematic(s).
  -[ ] Import and write Geospatial data straight in minecraft using FastAsyncWorldEdit API.
-[ ] Build-site management using in-house database.

---

Third party libraries
---------------------
There are some third party libraries which are directly included in the source code tree, in particular:

* DiscordSRV: Discord bridging plugin for minecraft.
  > licensed under the GNU General Public License v3
  > **https://discordsrv.com/**
  > **https://github.com/DiscordSRV/Ascension**
  - package [`com.discordsrv.common.util.function.*`](https://github.com/DiscordSRV/Ascension/tree/00bc363d472cbb52d3c872c187ce434722bddc02/common/src/main/java/com/discordsrv/common/util/function)
  - package [`com.discordsrv.common.core.scheduler.*`](https://github.com/DiscordSRV/Ascension/tree/00bc363d472cbb52d3c872c187ce434722bddc02/common/src/main/java/com/discordsrv/common/core/scheduler)
  - class [`com.discordsrv.common.util.ComponentUtil`](https://github.com/DiscordSRV/Ascension/blob/00bc363d472cbb52d3c872c187ce434722bddc02/common/src/main/java/com/discordsrv/common/util/ComponentUtil.java)
  - class [`com.discordsrv.common.util.TaskUtil`](https://github.com/DiscordSRV/Ascension/blob/00bc363d472cbb52d3c872c187ce434722bddc02/common/src/main/java/com/discordsrv/common/util/TaskUtil.java)
  - class [`com.discordsrv.common.discord.api.entity.message.util.SendableDiscordMessageUtil`](https://github.com/DiscordSRV/Ascension/blob/00bc363d472cbb52d3c872c187ce434722bddc02/common/src/main/java/com/discordsrv/common/discord/api/entity/message/util/SendableDiscordMessageUtil.java)
  - class [`com.discordsrv.common.command.combined.abstraction.Text`](https://github.com/DiscordSRV/Ascension/blob/00bc363d472cbb52d3c872c187ce434722bddc02/common/src/main/java/com/discordsrv/common/command/combined/abstraction/Text.java)
  - class [`com.discordsrv.common.command.combined.abstraction.CommandExecution`](https://github.com/DiscordSRV/Ascension/blob/00bc363d472cbb52d3c872c187ce434722bddc02/common/src/main/java/com/discordsrv/common/command/combined/abstraction/CommandExecution.java)
  - class [`com.discordsrv.common.command.combined.abstraction.DiscordCommandExecution`](https://github.com/DiscordSRV/Ascension/blob/00bc363d472cbb52d3c872c187ce434722bddc02/common/src/main/java/com/discordsrv/common/command/combined/abstraction/DiscordCommandExecution.java)