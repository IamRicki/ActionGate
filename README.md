# ActionGate

ActionGate is a Paper plugin that controls gameplay actions through standard Bukkit permissions. A server owner defines rules in YAML; a permission plugin such as LuckPerms decides which players receive the required permissions.

ActionGate does not provide ranks, jobs, an economy, or a player database. It is a focused gameplay restriction engine.

## Features

- Config-driven rules with no Java changes needed for new targets
- Per-rule permissions, messages, enabled state, targets, and worlds
- Standard Bukkit permission compatibility (including LuckPerms)
- `&` color codes and denial-message placeholders
- Message rate limiting without weakening enforcement
- Runtime reload and validation commands
- Admin rule browser with enable/disable controls and pagination
- Efficient in-memory indexes for frequent gameplay events
- Invalid-rule isolation with clear startup/reload warnings
- Explicit non-OP gameplay permissions and bypass permission

## Requirements and installation

This project is built and tested against Paper 26.2 and Java 25.

1. Build or download `ActionGate-1.0.0.jar`.
2. Place it in the Paper server's `plugins` directory.
3. Start the server once to create `plugins/ActionGate/config.yml`.
4. Edit that runtime configuration.
5. Run `/actiongate reload` or restart the server.

## Quick start

Every rule has an ID and the following fields:

```yaml
rules:
  diamond-mining:
    enabled: true
    action: BLOCK_BREAK
    targets:
      - DIAMOND_ORE
      - DEEPSLATE_DIAMOND_ORE
    permission: "actiongate.mine.diamond"
    message: "&cYou need permission &e{permission} &cto mine Diamond Ore."
    worlds:
      - "*"
```

Material and entity targets use Bukkit enum names. `worlds: ["*"]` applies everywhere; otherwise, use exact world folder names such as `world` and `world_nether`.

Available message placeholders are `{player}`, `{permission}`, `{rule}`, `{target}`, and `{action}`.

## Supported action types

| Action | Targets | Behavior |
| --- | --- | --- |
| `BLOCK_BREAK` | Required materials | Blocks breaking selected blocks |
| `BLOCK_PLACE` | Required materials | Blocks placing selected blocks |
| `FISH` | None | Blocks fishing events |
| `USE_ITEM` | Required materials | Blocks use of the selected held items without cancelling unrelated block use |
| `INTERACT_BLOCK` | Required block materials | Blocks interaction with selected blocks |
| `ENTITY_INTERACT` | Required entity types | Blocks right-click entity interaction |
| `ATTACK_PLAYER` | None | Blocks direct player-versus-player damage |
| `ATTACK_MOB` | Optional entity types | Blocks direct player attacks against all mobs or selected entity types |
| `CRAFT` | Required result materials | Blocks crafting recipes producing selected items, including shift-click attempts |
| `ENCHANT` | Optional item materials | Blocks all enchanting or only selected item materials |
| `ELYTRA` | None | Blocks beginning Elytra gliding without removing the Elytra |
| `PORTAL` | Required: `NETHER` or `END` | Blocks the corresponding portal transition |
| `ITEM_DROP` | Required materials | Blocks dropping selected items |
| `ITEM_PICKUP` | Required materials | Blocks pickup without deleting the item |

When multiple applicable rules match the same action, the player must satisfy every matching rule.

## Commands

All commands require `actiongate.admin`. `/ag` is an alias for `/actiongate`.

| Command | Purpose |
| --- | --- |
| `/actiongate` | Show status and command overview |
| `/actiongate reload` | Reload the runtime config and rebuild all rules |
| `/actiongate rules` | List loaded rules and enabled states |
| `/actiongate info <rule>` | Show complete information for one loaded rule |
| `/actiongate validate` | Validate the current runtime file without applying it |
| `/actiongate debug on` | Enable console decision logging |
| `/actiongate debug off` | Disable console decision logging |
| `/actiongate test <player> <rule>` | Report whether an online player would pass a rule |
| `/actiongate gui` | Open the administration GUI |

The GUI shows status, rule counts, reload and validation controls, debug state, and a paginated rule menu. Clicking a rule updates only the runtime configuration and applies the change immediately.

## Permissions

| Permission | Default | Purpose |
| --- | --- | --- |
| `actiongate.admin` | OP | Administration commands and GUI |
| `actiongate.bypass` | False | Ignore every gameplay restriction |
| Rule permissions | False | Permissions named by rules, registered dynamically |

Gameplay permissions and `actiongate.bypass` are deliberately not granted to operators by default.

## LuckPerms examples

ActionGate has no hard dependency on LuckPerms. It uses `Player#hasPermission`, so LuckPerms works through Bukkit's normal permission system.

Example role setup:

```text
/lp creategroup miner
/lp group miner permission set actiongate.mine.diamond true
/lp group miner permission set actiongate.mine.gold true

/lp creategroup fisherman
/lp group fisherman permission set actiongate.action.fishing true

/lp creategroup farmer
/lp group farmer permission set actiongate.action.farming true
```

The Farmer permission is an example for a rule you define; ActionGate does not hardcode groups or role names.

To grant a user the explicit global bypass:

```text
/lp user <player> permission set actiongate.bypass true
```

## Runtime configuration

`src/main/resources/config.yml` is a template bundled in the JAR. ActionGate never edits it at runtime.

The live file is `plugins/ActionGate/config.yml`. With this project's Gradle test server, it is `run/plugins/ActionGate/config.yml`. Commands, validation, migration, and GUI updates operate on that live file.

Older proof-of-concept configurations containing `restricted-blocks` are migrated once to generic `BLOCK_BREAK` rules. Existing materials, permissions, and messages—including custom Gold restrictions—are preserved.

## Validation and troubleshooting

ActionGate validates rules on startup and reload. A bad rule is skipped without disabling valid rules. Warnings identify the rule and issue, including unknown actions, invalid materials/entity types, missing permissions or targets, invalid portal targets, duplicate IDs, and malformed sections.

- Run `/actiongate validate` after editing YAML.
- Run `/actiongate info <rule>` to inspect what is loaded.
- Run `/actiongate test <player> <rule>` to inspect permission and bypass state.
- Temporarily use `/actiongate debug on` for console decisions, then turn it off to avoid noisy logs.
- Confirm you edited the runtime file, not `src/main/resources/config.yml`.
- Permission changes are managed by your Bukkit-compatible permission plugin; ActionGate does not cache player permission results.

## Development and building

The repository includes the Gradle Wrapper and the recommended Paper run task.

```powershell
.\gradlew.bat clean build
```

The JAR is written to `build/libs/ActionGate-1.0.0.jar`.

Start a local Paper 26.2 server with:

```powershell
.\gradlew.bat runServer
```

The run task builds and loads the plugin automatically. Stop a running server before a clean rebuild on Windows because Paper keeps the plugin JAR open while running.

## Compatibility scope

ActionGate 1.0 has been built and startup-tested on Paper 26.2 with Java 25. Gameplay events should still be tested on a staging server with the server's actual plugins and rule set before production deployment.
