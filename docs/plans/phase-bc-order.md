# Phase B/C order after Phase A

Leaf-rank scan rerun on 2026-06-23. **State re-verified 2026-06-25** (see Status below).

## Status (verified 2026-06-25)

- **MediaViewController — DONE.** Converted to `.kt` and committed in `2ef238ed`
  ("missing conversions"), 9425 java lines → 9369 kt. Same commit also landed Phase A
  leftovers: `org/drinkmore/Tracer`, `telegram/MessageListener`, `telegram/TdlibProvider`.
  28 java files still call into it — fine, the Java-callable API was preserved.
- **MessagesController — DONE (uncommitted).** AS one-click J2K (12874 java → ~13.4k kt),
  then hand-fixed: 149 Kotlin + 30 Java interop errors → 0. Full gate green
  (`:app:compileUniversalDebugKotlin` + `:app:compileUniversalDebugJavaWithJavac`).
  Ripples this required: (1) vkryl `Future`, `FutureBool`, `FactorAnimator.Target` made
  `fun interface` (enable SAM); (2) MVC.kt `m.getChat()` → `m.chat` (property rename);
  (3) `@JvmField manager`/`wallpaperObject`, `@JvmStatic` on 6 companion members for Java
  callers; AS also refactored 3 Java callers (`getManager()`→`.manager`). NOT yet
  device-verified (main chat screen — high-priority render walk).
- **TGMessage — DONE (committed `621c7f80`).** AS J2K (9830 java → ~10k kt), base class of
  18 Java subclasses. Far hairier than MessagesController: J2K silently collapsed virtual
  dispatch (getContentWidth/getHeight/getBubbleContentPadding/getBottomLineContentWidth) into
  direct field reads — compiled green, would break layout for ~10 message types; reconciled
  per-site vs original Java. Plus @JvmField protected fields for subclasses, override-signature
  restoration, dropped @Suppress("WrongConstant"). Device-verify found 2 more nullable-return
  crashfixes in MVC (obtainCropState/getFilterState) + a pre-existing ContentPreview switch
  fallthrough (`4408e383`). NOT fully device-verified across all message types yet.
- **TdlibUi — DONE (uncommitted).** AS J2K (8218 java → ~8.9k kt), `extends Handler`, no class
  subclasses (only a nested-interface extension) so NO virtual-dispatch-collapse risk — far
  cleaner than TGMessage. Fixes: SimpleSendCallback/PremiumLimitCallback → `fun interface`;
  37 SAM labels; nested `private`→`internal`; dropped static-import consts re-imported; 14
  caller-ripple fixes in MessagesController/TGMessage/MVC (nullable params restored to match
  unannotated Java, e.g. `openMessage(context: TdlibDelegate?)`); `@JvmStatic` on
  sendTdlibLogs/removeAccount/reportChat/reportChats for Java callers. Audit clean: 0
  `return X!!`, 0 createIfEmpty patterns, handleMessage override intact. Full gate green.
- **Settings — DONE (committed `1e1214aa`).** AS J2K (7282 java → ~7.1k kt), singleton, not
  subclassed. Fixes: instance() non-null; const val for BADGE_FLAG_*/TDLIB_LOG_VERBOSITY_UNKNOWN/
  MAP_PROVIDER_UNSET (Java case labels); dual getLogSettings/getTdlibLogSettings aliases kept;
  public incognitoMode setter; CloudSetting fields @JvmField, isBuiltIn() kept as method; lazy-init
  preserved in preferredVideoLimit getter. Caller ripple in TGMessage/MVC/TdlibUi/MessagesController/
  SettingsCloudController. Compile gate green (Kotlin + :app Java).
- **Tdlib — DONE (committed `fd5c4a1d`).** AS J2K (~12.2k java → ~12.2k kt), 319 inbound, NOT
  subclassed. Root fixes: ui() non-null (collapsed ~158 ripple); ResultHandler/3 nested callbacks/
  vkryl Filter -> fun interface; J2K lambda-botch (`Type? {`->`Type {`) + labels + @file:OptIn;
  re-imported stripped tgx.td top-level consts; nested-private -> non-private (ClientHolder/
  Generation/StickerSet); reactions()/outline() internal. Nullability audited vs oracle —
  authPhoneCode/Number kept nullable (real regression caught + fixed), rest faithful. Caller ripple
  across 9 files; vkryl/core bumped (Filter `16f354c`). Kotlin + :app Java gate green.
- **Phase B COMPLETE.** All 6 god objects converted: MediaViewController, MessagesController,
  TGMessage, TdlibUi, Settings, Tdlib.
- All Phase C hierarchy files still `.java`.
- All 8 leaf remnants below still `.java`.
- This plan file (`docs/plans/`) is **untracked** in git — commit it if it should persist.

## Blast radius (inbound `import` count, verified 2026-06-25)

Drives the risk-ascending order. Counts are java files that `import` the symbol.

| God object         | inbound imports | status |
|--------------------|-----------------|--------|
| Tdlib              | 319             | **DONE** |
| Settings           | 153             | **DONE** |
| TdlibUi            | 84              | java   |
| MessagesController | 34              | java   |
| TGMessage          | 33              | java   |
| MediaViewController| (~22)           | **DONE** |

## What remains from the leaf scan

Files with zero inbound refs and still small enough to stay in the leaf pool (all `.java`):

- `ui/MapController.java`
- `component/attach/MediaBottomFilesController.java`
- `component/dialogs/SearchManager.java`
- `telegram/SortedList.java`
- `widget/emoji/EmojiLayoutSectionPager.java`
- `telegram/UserListManager.java`
- `util/StickerSetsDataProvider.java`
- `mediaview/MediaSpoilerSendDelegate.java`

Everything else in the 0-inbound set is either already Kotlin or too small to matter for the next tranche decision.

## Phase B — god objects

`MediaViewController` and `MessagesController` are done (see Status). Remaining,
**risk-ascending** (lowest blast radius first — order validated by the MVC + MessagesController runs):

1. `data/TGMessage.java` (33) — NOTE: base class with a subtype hierarchy
   (`TGMessageText`, `TGMessageMedia`, …); conversion ripples into subclass syntax, so it
   straddles Phase B/C. Convert with awareness of its subclasses.
2. `telegram/TdlibUi.java` (84)
3. `unsorted/Settings.java` (153)
4. `telegram/Tdlib.java` (319) — last; highest blast radius.

> The original plan ordered these payoff-first (Tdlib first). That was overridden in favor
> of risk-ascending: prove the per-god-object workflow on the smallest blast radius first,
> save Tdlib for last. MediaViewController (smallest, ~22) was the first proof and is done.

## Phase C — hierarchies

Order these after the god objects. They are mostly superclass / base-type migrations.

1. `navigation/ViewController.java` — **DONE (committed `fba0c47d`).** Root base, ~118 subclasses.
   1109 Kotlin + 164 Java errors → 0. Key: base restored to oracle method-shape + `open` + `protected`
   (Kotlin methods final-by-default → Java overrides break — JAVA GATE MANDATORY for base classes);
   `tdlib` kept nullable (3 account-less controllers), tdlib() non-null; reverted AS's project-wide
   `getX()`→`.x` Java-caller rewrites. Both gates green.
2. `ui/RecyclerViewController.java` — **DONE (committed `8efe1c75`).** ~40 subclasses. J2K over-nullabilized
   the supertype type-arg (`TelegramViewController<T?>`→`<T>`) + abstract onCreateView params; recyclerView
   `open var … protected set` (open getter for Java overrides). Both gates green.
3. `ui/EditBaseController.java` — **DONE (committed `6b8782fe`).** Base for ~14 edit/input
   controllers. Compile-gate: `<T?>`→`<T>`, abstract onCreateView params non-null, recyclerView
   `@JvmField protected` + explicit `open getRecyclerView()` (Java covariant override in
   EditChatFolderController). Adversarial audit workflow (4 dims × verify) caught **5 runtime
   regressions that compiled GREEN on both gates**: (1) CRASH — J2K nested done-button setup in
   `doneButton = DoneButton(ctx).apply { … contentView.addView(doneButton) }`; bare `doneButton`
   in the block = outer field, null until apply returns → addView(null) IllegalArgumentException
   on every edit screen; un-nested to oracle order. (2) same null-field bug in
   addThemeInvalidateListener(doneButton). (3) `.also { this.itemAnimator = it }` set the
   RecyclerView's own animator (apply receiver), left base field null → live NPE in
   EditChatFolderController; qualified `this@EditBaseController`. (4/5) `(Lang.rtl()?LEFT:RIGHT)`
   flattened to `Gravity.START` at FAB params + handleLanguageDirectionChange (tgx does RTL via
   Lang.rtl(), not layout-direction). Both gates green. NOT device-verified.
   **CORRECTION:** 6b8782fe actually shipped NON-COMPILING (dropped local SettingsAdapter cast in
   handleLanguagePackEvent → unresolved `adapter`; missing DECELERATE_INTERPOLATOR import; rootColorId
   needs `override`; nullable abstract onCreateView params broke EditTextController/CreateTopicController
   overrides). Branch was red from 6b8782fe until fixed in follow-up **`240897bf`**.
4. `ui/SharedBaseController.java` — **DONE (committed `46dd1e5a`).** Base for 5 shared-media Java
   subclasses. Compile-gate: non-null generics (`<T : MessageSourceProvider>`, `ViewController<Args>`);
   removed stray `import java.lang.Long` (shadowed kotlin.Long file-wide, ~15 errors) + explicit
   `java.lang.Long.compare`; `buildRequest(): TdApi.Function<*>?` (re-added displaced `import TdApi`,
   dropped bogus `import kotlin.Function`); onCreateView non-null; anon SettingsAdapter `this.isSearching`
   → `this@SharedBaseController`; dropped @JvmField from abstract `icon`; 3 MessageListener overrides
   nullable params + `!!`; reuse `ArrayList<ListItem>`. Java-gate (quirk 7): canSearch/provideSearchFilter
   → `protected open` (oracle protected; `open fun` widened to public, broke Java `protected` overrides),
   isMediaController `@JvmStatic`, reverted AS's ProfileController `getIcon()`→`.icon` rewrite. Caller
   ripple in MessagesController.kt (3 property accesses). Adversarial audit + hand pass: 0 findings.
   Both gates green. NOT device-verified.
5. `navigation/ViewPagerController.java` — **DONE (committed `b2e8b655`).** Base of MessagesController
   pager etc. Ripple: ViewController.kt AttachListener/FocusStateListener -> `fun interface` (SAM in
   VPC), and onEnterSelectMode/getSelectMenuId `protected`->**public** — Java `protected` is
   package-accessible so same-package siblings (VPC on its current pager item) call them
   cross-instance; Kotlin `protected` is subclass-only. Fixed all overrides (ChatsController public).
   Both gates green. NOTE: the initial single-file commit did NOT compile (SAM needs `fun interface`);
   base-class migrations must stage the whole ripple + run BOTH gates.
6. `navigation/TelegramViewController.java` — **DONE (committed `584a4ca5`).** Chat-search base of
   6 controllers. Fixes: `<T?>`->`<T>`; getSearchAntagonistView kept as method (J2K made it a val
   property, broke RecyclerViewController.kt override); VPC.kt caller isSearchAntagonistHidden()
   -> property. Audit: SearchManager !! placements match @NonNull/@Nullable contracts; anon
   this@TelegramViewController intact. Both gates green.
7. `ui/BottomSheetViewController.java` — **DONE (committed `e39235f4`).** Bottom-sheet base
   (MessageOptions/SetSender/SinglePage/ShareController...). Fixes: `<T?>`->`<T>` (self + nested
   BottomSheetBaseRecyclerViewController); dropped illegal `@JvmField` on abstract `contentOffset` +
   interface `recyclerView`; topEdge getter -> `protected` (J2K widened public, broke MessageOptions
   override). VPC.kt currentPosition/currentPositionOffset `internal`->`private` (oracle) — J2K's
   `internal` clashed with BottomSheet's same-named shadow fields (subclass-written, base-read).
   NOTE: AS J2K ran "correct usages" and rewrote Java callers to property syntax
   (getContentOffset()->contentOffset, getRecyclerView()->recyclerView) — reverted (they break on
   abstract/interface members). Decline that AS prompt next time. Both gates green.
8. `component/attach/MediaBottomBaseController.java` — **DONE (committed `41790e68`).** Base of 11
   attach/popup controllers. Fixes: `<T?>`->`<T>`; 7 methods `protected` (J2K widened public, broke
   Java subclass protected overrides) — onUpdateBottomBarFactor/preload/onCompleteShow/onMultiSendPress/
   addCustomItems/onCancelMultiSelection/createCustomBottomBar. These are called cross-instance by
   MediaLayout (same package) but MediaLayout is JAVA, so JVM-protected works (Java package-protected
   rules) — protected is faithful + zero Java churn (contrast the Kotlin-caller protected-package trap
   which needs public). `@JvmOverloads` on hideProgress default param. Both gates green.
9. `telegram/TdlibDataManager.java` — **DONE (committed `f9c8519b`).** Generic base; J2K over-nullabilized
   the type-param bounds/args → broke subclass variance. Restored non-null generics + nested types internal.
10. `filegen/GenerationInfo.java` — **DONE (committed `cbe44613`, batch with #11/#12).** Clean J2K; unused
    `hasThumb` ctor param + `isPost()`-style quirks confirmed faithful vs oracle. No fixes needed.
11. `data/PageBlock.java` — **DONE (committed `cbe44613`).** Biggest of the batch. Fixes: `@file:OptIn(
    ExperimentalContracts)` for the tgx.td.isEmpty extension; 12 `ParseContext` privates -> `internal`
    (Kotlin forbids companion -> nested-private; Java allowed same-top-level-class access); `parse()`
    `@JvmStatic`; `break` -> `return` in collage/slideshow when-branches (switch was method's last stmt);
    `relatedViewType` stays `abstract val` (getter — @JvmField illegal on abstract); `setDetails` JVM clash
    fixed with `@JvmField details`; uninitialized nullable vars given `= null`; `arrayOf<ListItemInfo?>`.
12. `data/InlineResult.java` — **DONE (committed `cbe44613`).** Fixes: `setTarget`/`setMessage` return
    types `<T?>` -> `<T>`. Rest 1:1.

Batch also reverted Android Studio "correct usages" property rewrites in Java callers where the target is
abstract or getter-only: InstantViewController (getRelatedViewType), PageBlockRichText/PageBlockTable
(getOriginalBlock), PageBlockRichText (getListItem). Rewrites onto @JvmField members left as-is.
Main hierarchy (#1-#12) complete; next is the secondary tail.

Secondary tail if the main hierarchy pass stays green:

- `navigation/TooltipOverlayView.java` — remaining (big, 1464 LOC)
- `charts/BaseChartView.java` — remaining (big, 1613 LOC)
- `mediaview/disposable/DisposableMediaViewController.java` — **DONE (committed `11801c03`).**
  Hand-converted (no AS J2K). ViewController<Args> base + 4 interfaces + nested Args/VideoHandler.
  lateinit protected contentView/tgMessage (subclass field access); @JvmStatic openMediaOrShowTooltip;
  getRevealFactor/getVisualProgress = getter+private-field+change-detecting-setter → `var; private set{}`.
  Facade traps: MathUtils.clamp→clamp, AnimatorUtils.DECELERATE→bare, Td.isVideoNote→msg.content.isVideoNote();
  Args widened protected→public (exposure rule). See [[kt-migration-jvmname-facade-trap]], [[kt-migration-hand-conversion-abi]].
- `util/text/TextEntity.java` — **DONE (committed `6c164ad1`).** Abstract base of TextEntityCustom/Message,
  wide caller fan-out. J2K glitches: doubled `@JvmField @JvmField` on every field + `@JvmField` on abstract
  vals (illegal, stripped). Restored dropped `getStart()/getEnd()` (start/end `@JvmField` + explicit getters).
  valueOf: `entities` → `Array<out TdApi.TextEntity?>?` (covariant, both callers) + `!!`; `in` → `String?`;
  `@JvmStatic` on 2 unmarked entry points. Reverted AS "correct usages" on abstract getter-only members
  across 6 caller files via whole-file `git checkout` (62 Java-gate breaks). See [[kt-migration-as-correct-usages]].
- `component/attach/MediaLayoutManager.java` — **DONE (committed `46557f91`, batch w/ BaseComponent + MenuMoreWrapAbstract).**
  Hand-converted. @JvmField context/tdlib/currentMediaLayout/openingMediaLayout (subclass field access);
  @JvmStatic singleMediaCallback (inherited-static call); ViewController.tdlib() is Tdlib? → `!!`.
- `support/SimpleShapeDrawable.java` — **DONE (committed `5808fda7`, Phase C warm-up).** Abstract base,
  2 subclasses override only abstract draw() → no virtual-dispatch surface. Faithful; gate green.
- `data/BaseComponent.java` — **DONE (committed `46557f91`).** Hand-converted. @JvmField protected viewProvider
  (subclass field read); abstract getters → abstract vals; open val file/fileProgress default null.
- `navigation/MenuMoreWrapAbstract.java` — **DONE (committed `46557f91`).** Hand-converted. getX getters →
  properties (itemsWidth/itemsHeight/anchorMode/revealRadius); shouldPivotBottom() stays a fn (name not getX).
- `telegram/ListManager.java` — **DONE (committed `83f61c28`).** Hand-converted. Generic base + nested Response/
  ListChangeListener. @JvmField tdlib/items/loadCount + `override fun tdlib()` coexist; COUNT_UNKNOWN const val;
  ListChangeListener widened public + @JvmSuppressWildcards (invariant List<T> so Java overrides match); default
  methods reach Java implementers (jvm-default active). See [[kt-migration-hand-conversion-abi]].
- `data/TGMessageGiveawayBase.java` — **DONE (committed `99405a0f`).** Hand-converted. TGMessage base + 5 nested
  classes (Content/ContentPart/ContentText/ContentBubbles/ContentDrawable). @JvmField content/rippleButton/
  giveawayInfo; const val BLOCK_MARGIN/CONTENT_PADDING_DP; @JvmStatic getGiveawayTextStyleProvider/getDateTime;
  own contentHeight field renamed (collided with TGMessage open val); genTextWrapper → file-level private (nested
  can't reach companion-private); facade traps StringUtils.isEmpty/PorterDuffPaint.get/Td.unsupported+assert*.

Secondary tail COMPLETE — both big ones done:
- `navigation/TooltipOverlayView.java` — **DONE (committed `8127c335`).** User hand-converted; I gated + fixed.
  TooltipContentView base made public (public newContent/reset/show expose it); width/height abstract props;
  LocationProvider/AvailabilityListener → fun interface (Kotlin-caller SAM); sibling/outer-accessed TooltipInfo
  members private→internal; Destroyable via object literal; OnAttachStateChange params non-null.
- `charts/BaseChartView.java` — **DONE (committed `8127c335`).** @JvmField enabled (setEnabled clash w/ View);
  AnimatorListenerAdapter.onAnimationEnd params non-null; `(if (useMinHeight) 0 else 1) until n`; PieChartView
  override widened public.

Post-migration runtime bug found + fixed: `RecyclerViewController.recyclerView` field→property made intra-class
reads virtual → subclasses overriding getRecyclerView() hijacked base's own read → NPE at construction
(committed `44611f6b`). See [[kt-migration-field-to-property-virtual-trap]]. Swept all other `open var` props:
`TGMessage.contentX` safe (original Java already used getContentX() internally).

## Status: Phase A/B/C named tranches COMPLETE

Phase B god objects ALL Kotlin: TGMessage, TdlibUi, Settings, Tdlib. Phase C bases + secondary tail done.
~1000 leaf `.java` files remain in `org.thunderdog.challegram` (long tail) — now attacked via risk-ascending
leaf tranches (T-L*).

## Leaf tranches (long tail)

Leaf pool recomputed fresh: 308 `.java` ≤220 LOC with ≤1 inbound import. Order: pure contracts → value
holders → stateful concretes. Per-tranche: hand-convert (2-space, space before `(`, match sibling `.kt`),
gate `:app:compileUniversalDebugJavaWithJavac` (both compilers), one commit. Device-test only UI/runtime tranches.

### T-L1 — pure listener/delegate interfaces — **DONE (11 files, gate GREEN)**
Interop rules applied: single-abstract-method → `fun interface` (enables Kotlin SAM, prevents the
`does not have constructors` error); multi-method / all-default → plain `interface` with default bodies
(jvm-default active reaches Java implementers); interface `int` constants → `companion object { const val }`
(const → static field on interface, Java `Interface.CONST` resolves); preserved `@ColorId`/`@ColorInt`.
Two existing Kotlin implementers forced signature alignment BEFORE writing: `MediaStackCallback.currentItem`
made nullable (matches `MediaViewController.kt` override + `MediaStack.java` notify), `ColorChangeAcceptorDelegate.getDrawColor()`
kept a `fun` not a property (matches `SimpleShapeDrawable.kt` override). `WrapperProvider` MUST be `fun interface`
(Kotlin SAM lambda at `Tdlib.kt`).
Files: navigation/{SystemBackEventListener, SelectDelegate}, telegram/{GlobalResolvableProblemListener,
PollListener, PrivateCallListener, AnimatedEmojiListener, GlobalProxyPingListener, AuthorizationListener},
util/{WrapperProvider, ColorChangeAcceptorDelegate}, mediaview/MediaStackCallback.
Excluded from leaf-interface set: 7 `@IntDef` annotation defs (no clean Kotlin equivalent — keep Java or
handle separately) + high-fanout/multi-default contracts (`loader/Watcher` 17 impls, `util/text/TextColorSetThemed`
13 defaults, `StretchyHeaderView`, `GlobalMessageListener`, `voip/ConnectionStateListener`) → future T-L2.

### T-L2 — value/data holders — **DONE (15 files, commit `3e2d612a`, gate GREEN + 3-lens verify clean)**
Executed via workflow (15 parallel converters, one per file) + 3-lens adversarial verify (behavior /
const-annotation-JNI / access-form). ABI + JNI preserved: NetworkStats `@JvmOverloads` reproduces the
`()V`+`(JJJJ)V` ctors tgvoip.cpp uses and `@JvmField` names match `SetLongField`-by-name; Socks5Proxy field
names match JNI reads (+`@Keep`); VoIPServerConfig keeps the native as companion `@JvmStatic external fun`
(Resampler.kt precedent). CameraError nested `@IntDef`→`annotation class Code` (SOURCE) + `const val
NOT_ENOUGH_SPACE=-1`. RecentEmoji field+method `isCustomEmoji` collapsed to a `val` (is-getter). Rewritten
bodies (CrashLog.deleteFile, VoIPServerConfig.getInt NPE, Socks5Proxy require) verified behavior-identical.
Files: charts/view_data/{TransitionParams, ChartBottomSignatureData}, video/old/Sample, data/{TGSwitchInline,
CrashLog, AvatarInfo}, emoji/RecentEmoji, voip/{NetworkStats, Socks5Proxy, VoIPPersistentConfig, VoIPServerConfig},
util/{RingtoneItem, DeviceStorageError}, ui/camera/CameraError, filegen/SimpleGenerationInfo.

### T-L2b — chart view_data hierarchy — **scoped, executing via Workflow**
Real hierarchy (checked, not Linear/DoubleLinear as originally guessed — those chart views use `LineViewData`
directly): `LineViewData` (base) ← `BarViewData`, `StackBarViewData`, `StackLinearViewData`. `updateColors()`
overridden in the 3 subclasses → base must be `open class` / `open fun updateColors()`. No base-constructor
call into an overridable method (no [[kt-migration-field-to-property-virtual-trap]] risk here). All fields
public, read/written as plain Java field syntax (`.alpha`, `.enabled`, `.line`, `.paint`, …) by 7 non-hierarchy
callers (`{Bar,StackBar,Linear,StackLinear,DoubleLinear}ChartView.java`, `PieChartView.java`, `BaseChartView.kt`)
→ every field must be `@JvmField` (`val` for `line`, `var` for the rest) to keep Java field-access ABI, per
[[kt-migration-hand-conversion-abi]]. `canBlend()`/`blendColor` duplicated identically in Bar/StackBarViewData
(no shared base) — preserve as-is, don't dedupe (not in scope). All 4 files directly `new`'d (not just
subclassed) — `LineViewData` itself needs a public non-abstract ctor.
Files: charts/view_data/{LineViewData, BarViewData, StackBarViewData, StackLinearViewData}.

### T-L3a — remaining pure top-level interfaces — **scoped, executing via Workflow**
Recomputed leaf pool fresh against current tree (~971 `.java` remain in org.thunderdog.challegram):
518 files ≤220 LOC. Of those, 73 are pure top-level `interface Foo` declarations (same shape as T-L1,
which did the first 11) — the single largest homogeneous, lowest-risk category left, so highest
files-converted-per-unit-risk. Absorbs the previously-deferred multi-default/high-fanout set (`Watcher`,
`TextColorSetThemed`, `ConnectionStateListener`, `StretchyHeaderView`, `GlobalMessageListener`) plus 68
more. Fanout mostly low (0-6 implementers); outliers needing extra caller-check: `AttachDelegate` (18),
`TextColorSet`/`ChatListener` (13), `RtlCheckListener` (12), `DrawableProvider`/`ThemeDelegate`/
`MoreDelegate`/`DrawModifier` (9-10), `Watcher`/`Receiver` (8).
Playbook: same as T-L1 — SAM → `fun interface`; multi-method/all-default → plain `interface`; `int`
constants → `companion object { const val }`; preserve `@ColorId`/`@ColorInt`/etc annotations; agents
self-discover implementers via grep before choosing shape (73 files too many to pre-analyze by hand).
Dirs: telegram/ (25), util/ (20, incl. util/text/*), navigation/ (8), loader/ (5), theme/ (4),
mediaview/ (3), component/ (2), emoji/ (2), widget/ (1), voip/ (1), ui/camera/ (1), data/ (1).

### T-L3b+ (not yet scoped)
After T-L3a: the 7 `@IntDef` annotation defs, then stateful concrete leaves (need device-test).
~157 more leaf candidates (≤220 LOC, non-interface) remain in the pool before moving past T-L3. Scope
when reached.
