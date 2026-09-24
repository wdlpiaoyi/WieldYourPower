# AGENTS.md

Forge 1.20.1 mod `wieldyourpower` (力量掌控). Java 17, official mappings, Forge `1.20.1-47.3.10`, Gradle 8.8 wrapper, daemon disabled. Ships client + server code.

## Commands
- Build: `.\gradlew.bat clean build --console=plain` from the repo root. `JAVA_HOME` must point at JDK 17.
- Dev runs: `.\gradlew.bat runClient` / `runServer` (working directory `run/`).
- No tests, linter, formatter or typecheck tasks exist (`src/test` has only empty dirs). "Verify" = build succeeds + manual in-game check; do not invent test commands.

## Version bump (easy to miss)
Bump BOTH or they drift: `gradle.properties` `mod_version` and `WieldYourPower.VERSION`. Output is `build/libs/wieldyourpower-1.20.1-forge-<ver>.jar`.

## Build quirks
- Cloth Config is the only compile dependency, read from `modpackModsDir` (`build.gradle`, default project-relative `libs/`, which is gitignored; override with `-PmodpackModsDir=...`). The build fails without a `cloth-config-*.jar` there; runtime also requires Cloth.
- Do not rewrite `gradle.properties`/`.java` with PowerShell `Set-Content -Encoding UTF8` (adds a BOM and breaks compilation); use the edit tools.

## Config
- Config lives in `WYPConfig` (COMMON) and is surfaced through the Cloth screen (`client/cloth/ClothScreens`, plus the custom `QuickAddEntry` row reused by every list).
- Filter lists use the comma form `key,value` (a `:` inside an id is kept). Legacy `key:value` is migrated on load (`FilterSyntax.normalizeAll`; `killHonor` via `ConfigMigration`, the ally list on player-NBT load). Write the comma form in new code/UI.
- Other general toggles: `creativeBreaksProtectedBlocks` (creative may break blocks other mods protect), `blockBreakerEnabled` (admin Block Breaker item), `blockProtectionBypass` (keyword-driven third-party block-protection switch).

## Networking
- `WYPNetwork.VERSION` (currently `"2"`) is the channel protocol version; bump it whenever packet fields change so mismatched clients are rejected.

## Mixins
- MixinGradle 0.7.38 + Mixin 0.8.5 processor. Config: `src/main/resources/wieldyourpower.mixins.json`; refmap wired via the `mixin { add ...; config ... }` block in `build.gradle`; `MixinConfigs` is also set manually in `tasks.jar`.
- Add new mixins to that config: `mixins` = both sides, `client` = client only (package is `net.wieldyourpower.mixin`).
- `injectors.defaultRequire = 1`: a failed injection crashes at runtime, not at build time. All current mixins target vanilla; a mixin aimed at an optional third-party class would need its own config with `"required": false` + `"defaultRequire": 0`.
- Example: `LivingEntitySetHealthMixin` rewrites `setHealth` args with `@ModifyVariable` (refmap maps `setHealth -> m_21153_`).

## Access transformer
`src/main/resources/META-INF/accesstransformer.cfg` currently exposes (SRG names): `LivingEntity.die/dead/DATA_HEALTH_ID/lastHurt`, `Entity.unsetRemoved/isAddedToWorld`. Add an entry before touching other private/package vanilla members.

## Architecture
- `capability/` = per-player self-limits (`IPlayerLimits`, UUID fallback); `network/` syncs them; `command/` registers `/wyp`; `common/*Events` are Forge event subscribers; `client/` enforces movement/mining client-side; `compat/` holds the keyword-reflection helpers guarded by the ASM bytecode scans in `ClassSafety` (`BossDespawnCompat` default OFF; `BlockProtectionBypass`, which toggles a third-party block-protection switch, default ON); `util/` holds `FilterSyntax`/`EntityMatcher`/`KillUtil`/`FrozenEntities`/`ForcedRemoval`/`RemovalGuard`/`ForceBlockBreak`.
- Speed limits are client-enforced (`client/ClientEvents`), while kill/freeze/protection are server-authoritative.
- `common/AuthorsFavorPresence` keeps a strong reference to favored entities and re-adds them if they vanish from the level, guarding against reflective "deep removal" that bypasses `remove`/`setRemoved`. `BlockProtectionEvents` waits one tick after a creative left-click and force-breaks only if the block is still there and unchanged, so vanilla and vein miners run first. `BlockPlacementEvents` releases cancelled placement events (both sides) for a creative player holding a block item (not yet verified in-game).

## Hard constraints (project rules)
- Never edit other mods' files or save data (`SavedData`); prefer generic, non-mod-specific approaches. No per-mod compat code/mixins.
- Stay within normal Java/Forge reach: events, Mixin method-body injection, access transformer. Do NOT use launch plugins, ASM class transformers, reflection into modlauncher internals, or `Unsafe`. (Some mods do; those are out of scope for this mod even when they make it lose.)
- `BlockProtectionBypass` must stay keyword-driven: it names no mod/class, only scans loaded mods for a static `*bypass*(boolean)` switch and checks that method's bytecode via `ClassSafety.isMethodSafe`. Do not hardcode a mod's class there.
- `/wyp kill` must always bypass protections: gate new protection on `KillUtil.isForceKilling(entity)`.
- Author's favor (`AuthorsFavorEvents`) coefficients default to "no effect" (`damageCoefficient` 0, `maxHealthCoefficient` 0, `maxHealthChangeCoefficient` 1) on purpose: opt-in per entity for pack authors. Don't "fix" the defaults. Entities are chosen by `authorsFavor.filter` (comma matchers `tag,`/`type,`/`uuid,`, parsed by `EntityMatcher`/`FilterSyntax`). It only touches `setHealth`/`die` calls that bypass the vanilla damage chain; vanilla damage is never modified. The per-tick fallback restore stops once a death sequence has started (`dead` / `deathTime > 0`), so a scripted death animation can still finish. A favored entity that is still alive drops no death loot (`LivingEntityDropLootMixin`), so forcing `dropAllDeathLoot` cannot hand out kill rewards for a survived purge.
- Keep `assets/wieldyourpower/lang/en_us.json` and `zh_cn.json` in sync.
- `Reference/` is extracted third-party material (gitignored, study only); nothing there is compiled or shipped.
- Do NOT publish GitHub Releases or version tags for this project. Source only, so anyone who wants the mod must build it themselves - a small barrier against misuse. Releases/tags are removed on purpose.
