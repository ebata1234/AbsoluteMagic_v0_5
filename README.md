# Absolute Magic v0.5.0 (未コンパイル・未検証)

Iron's Spells 'n Spellbooks 3.16.2 (1.20.1 / Forge 47.4.0+) 用アドオン。

| 魔法 | 内部名 | 学派 | 内容 |
|---|---|---|---|
| フィックスト・ストライク | fixed_damage | Holy | 攻撃のたびに固定値の確定ダメージ追加(v0.3.0 から変更なし) |
| パーセント・ストライク | percent_damage | Ender | 攻撃のたびに敵HP割合の確定ダメージ追加(v0.3.0 から変更なし) |
| **タイム・ボム** (新) | time_bomb | Fire | 狙った相手に時限爆弾 (下記) |

## タイム・ボム
- 視線の先の生き物(射程32)に発動。マナ100、クールダウン60秒、最大レベル1。
- 導火線は10秒 (config `time_bomb.fuseSeconds`)。残り時間はエフェクトアイコンにも出る。
- **プレイヤー**: 毎秒チャットに「[時限爆弾] 爆発まで N」。0で爆発して死亡(トーテム無効、吸収ハートも無視)。
- **モブ**: 10秒後に確定ダメージ20 (config `mobDamage`)。即死はしない。
- 爆発は演出のみ(ブロック破壊なし、周囲への巻き込みダメージなし)。
- 爆弾を付けられたプレイヤーが**他のプレイヤーを近接攻撃**すると、爆弾は相手に移る(残り時間は引き継ぎ。
  `transferResetsTimer=true` で毎回フルに戻す)。キルは最初に仕掛けた術者の撃破として扱う。
- クリエイティブ/スペクテイター中のプレイヤーは対象外(爆発時は不発)。牛乳では解除できない。`/effect clear` で解除可。
- 爆弾中にログアウトすると導火線は止まり、ログインで再開する。

## 魔術師 (Magician) 特性 (v0.5 新規 / L2 Hostility 2.5.19 必須)
- L2Hostility の特性 `absolutemagic:magician` (最大ランク5)。付いたモブが、標的に Iron's Spells の魔法を撃つ。
- 全モブ対象。付き方は L2Hostility の通常の特性抽選 (最低レベル40・コスト30・重み80)。
  変更は `data/absolutemagic/l2hostility_config/trait/magician.json` (データパックで上書き可)。
- 特性レベルNのモブは、config の level1〜levelN の魔法リストすべて(累積)からランダムに選ぶ。
- マナは使わない。**撃ち終わってから最低6秒**(config `cooldownSeconds`、既定8、下限6)は次を撃たない。
- 詠唱中は**立ち止まる**(移動速度-100%、経路停止)。専用モーションが無いので腕を振る動作で代用。
  INSTANT魔法は `instantWindupTicks`(既定20=1秒)、LONG魔法は魔法自身の詠唱時間。
- 威力 = 魔法レベル(特性レベル+offset)と、モブの `spell_power` 属性。
  spell_power に「特性レベル×0.15 + モブレベル×0.004」(上限2.0)を加算 (config `[power]`)。
- 誤射対策は無し。持続型(CONTINUOUS)の魔法は未対応(自動でスキップ)。
- config: `config/absolutemagic-magician.toml`
- 動作確認: 特性レベル付きのモブを出す。例 `/l2hostility` 系コマンドや、ボスのエンティティ設定の traits に
  `absolutemagic:magician` を指定する。魔法が出ない時は latest.log を `[Magician]` で検索。

## ビルド
0. L2Hostility 用に `libs/` へ次の5つを置く(元のファイル名):
   `l2hostility-2.5.19.jar`、`l2library-2.5.3.jar`、
   `l2damagetracker-0.4.3.jar`(l2hostility の jar を zip として開き `META-INF/jarjar/` から取り出す)、
   `l2serial-1.2.2.jar` と `Registrate-MC1.20-1.3.11.jar`(l2library の jar の `META-INF/jarjar/` から)。
1. JDK 17。`libs/` に irons_spellbooks-1.20.1-3.16.2.jar を元のファイル名で置く
   (足りないと言われたら irons_lib も。build.gradle のコメント参照)。
2. Forge 1.20.1-47.4.0 MDK の gradle wrapper (gradlew, gradle/) をこのフォルダにコピー。
3. `./gradlew build` → `build/libs/absolutemagic-0.5.0.jar` を mods へ。旧 absolutemagic の jar は取り除く。

## 動作確認
- 魔法を覚える前に効果だけ: `/effect give <対象> absolutemagic:time_bomb 10 0 true`
  (この方法だと術者がいないので、キル扱いなしで爆発する)

## 設定 config/absolutemagic-common.toml  [time_bomb]
fuseSeconds(10) / mobDamage(20.0) / canTargetPlayers(true) / transferResetsTimer(false) / forceKillPlayers(true)
