# AGENTS.md

Forge 1.20.1 mod `wieldyourpower` (力量掌控). Java 17, official mappings, Forge `1.20.1-47.3.10`, Gradle 8.8 wrapper, daemon disabled. No tests.

## Commands
- Build: `.\gradlew.bat clean build --console=plain`. `JAVA_HOME` must point at JDK 17.
- There is no test suite (`src/test` is empty). "Verify" = build succeeds + manual in-game check.

## Version bump (easy to miss)
Bump BOTH or they drift: `gradle.properties` `mod_version` and `WieldYourPower.VERSION`. Output is `build/libs/wieldyourpower-1.20.1-forge-<ver>.jar`.

## Build quirks
- Cloth Config is a **required** compile dependency read from `modpackModsDir` (`build.gradle`, default project-relative `libs/`; override with `-PmodpackModsDir=...`). A missing `cloth-config-*.jar` fails the build; runtime also requires Cloth.
- Do not rewrite `gradle.properties`/`.java` with PowerShell `Set-Content -Encoding UTF8` (adds a BOM and breaks compilation); use the edit tools.

## Mixins
- MixinGradle 0.7.38 + Mixin 0.8.5 processor. Config: `src/main/resources/wieldyourpower.mixins.json`; refmap wired via the `mixin { add ...; config ... }` block in `build.gradle`; `MixinConfigs` is also set manually in the jar manifest.
- `injectors.defaultRequire = 1`: a failed injection crashes at runtime, not at build time. To add a mixin, add the class name to the config's `mixins` list (`package` is `net.wieldyourpower.mixin`).
- Existing: `LivingEntitySetHealthMixin` rewrites `setHealth` args with `@ModifyVariable` (refmap maps `setHealth -> m_21153_`).

## Access transformer
`src/main/resources/META-INF/accesstransformer.cfg` already exposes what the mod needs (`LivingEntity.die/dead/DATA_HEALTH_ID`, `Entity.unsetRemoved/isAddedToWorld`). Add an entry (SRG names) before writing code that touches other private/package vanilla members.

## Architecture
- `capability/` = per-player self-limits (`IPlayerLimits`, UUID fallback); `network/` syncs them; `command/` registers `/wyp`; `common/*Events` are Forge event subscribers; `client/` enforces movement/mining client-side; `compat/` is the keyword-reflection last resort (default OFF) guarded by an ASM bytecode scan (`ClassSafety`).
- Speed limits are client-enforced (`client/ClientEvents`), while kill/freeze/protection are server-authoritative.

## Hard constraints (project rules)
- Never edit other mods' files or save data (`SavedData`); prefer generic, non-mod-specific approaches. No per-mod compat code/mixins.
- Stay within normal Java/Forge reach: events, Mixin method-body injection, access transformer. Do NOT use launch plugins, ASM class transformers, reflection into modlauncher internals, or `Unsafe`. (Some mods do; those are out of scope for this mod even when they make it lose.)
- `/wyp kill` must always bypass protections: gate new protection on `KillUtil.isForceKilling(entity)`.
- Author's favor (`AuthorsFavorEvents`) coefficients default to "no effect" (`damageCoefficient` 0, `maxHealthCoefficient` 0, `maxHealthChangeCoefficient` 1) on purpose: opt-in per entity for pack authors. Don't "fix" the defaults. It only touches `setHealth`/`die` calls that bypass the vanilla damage chain; vanilla damage is never modified.
- Keep `assets/wieldyourpower/lang/en_us.json` and `zh_cn.json` in sync.
- `Reference/` is extracted reference material for study only; nothing there is compiled or shipped.
