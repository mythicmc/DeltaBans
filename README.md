# DeltaBans
Banning plugin for BungeeCord and Spigot servers that relies on SQL storage, [DbShare](https://github.com/geeitszee/DbShare)
for connection pooling, and [SockExchange](https://github.com/geeitszee/SockExchange)

## Installation
Copy the same JAR into the plugins directory of your BungeeCord and Spigot installations. The
default configurations should be fine except for the `DbShareDataSourceName`. Change that setting
to match the name of the DbShare data source to load from and save to. In case of authentication
plugins, configure the `IpLookup` section of the Spigot configuration. This will add IPs to bans
if they are found otherwise all player bans will be based on just the name.

## Commands
`/ban`
- Permission: `DeltaBans.Ban`
- Description: Used to ban a player name, IP, or both (if IpLookup is set up correctly)

`/banned`
- Permission: `DeltaBans.CheckBan`
- Description: Used to check if a player or IP is banned

`/kick`
- Permission: `DeltaBans.Kick`
- Description: Used to kick a player

`/rangeban`
- Permission: `DeltaBans.RangeBan`
- Description: Used to ban consecutive IPs

`/rangebanwhitelist`
- Permission: `DeltaBans.RangeBan`
- Description: Used to add/remove a name from the range ban whitelist.

`/rangeunban`
- Permission: `DeltaBans.RangeBan`
- Description: Used to remove ban on consecutive IPs

`/tempban`
- Permission: `DeltaBans.Ban`
- Description: Used to ban a player name, IP, or both (if IpLookup is set up correctly) temporarily

`/unban`
- Permission: `DeltaBans.Ban`
- Description: Used to unban a player name or IP. If unbanning an IP finds a name also banned, the name will stay banned until the name is unbanned.

`/unwarn`
- Permission: `DeltaBans.Ban`
- Description: Used to remove one or more warnings

`/warn`
- Permission: `DeltaBans.Warn`
- Description: Used to warn and possibly run commands at certain warning amounts

`/warnings`
- Permission: `DeltaBans.CheckWarnings`
- Description: Used to check all warnings for a player (warner and message)

`/whitelist`
- Permission: `DeltaBans.Whitelist`
- Description: Used to enable/disable the network whitelist or to add/remove a name from the whitelist. This is a network version of the Minecraft whitelist. 

## Licence ([GPLv3](http://www.gnu.org/licenses/gpl-3.0.en.html))
```
DeltaEssentials - Basic server functionality for Bukkit/Spigot servers using BungeeCord.
Copyright (C) 2015  Trace Bachi (tracebachi@gmail.com)

DeltaEssentials is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

DeltaEssentials is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with DeltaEssentials.  If not, see <http://www.gnu.org/licenses/>.
```
