import { escapeHtml } from "./http";
import { revisionMark } from "./revision";

export function renderRoutineEditor(options: {
  apiBase: string;
  calendarHref: string;
  slug: string;
  signedIn?: boolean;
  accessToken?: string;
  embedded?: boolean;
}): string {
  const embedded = options.embedded === true;
  const body = `<div class="routine-root">
  <style>
    .routine-root { width: 100%; --acc: #e9cf4f; --card: #1b1b1d; --line: #2c2c30; --text: #ececec; --mute: #9a9aa2; --dim: #6e6e76; --field: #141416; color: var(--text); }
    .routine-root * { box-sizing: border-box; }
    .routine-root a { color: #00ffff; }
    .routine-root .layout { width: 100%; max-width: 720px; margin: 0 auto; padding: 0; }
    .routine-root input, .routine-root select, .routine-root textarea, .routine-root button { font: inherit; }
    .routine-root .bar { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
    .routine-root .days { display: flex; gap: 4px; flex: 1; min-width: 0; }
    .routine-root .days button { flex: 1; min-width: 0; background: none; border: 1px solid var(--line); color: var(--mute); border-radius: 8px; padding: 6px 0; font-size: 13px; line-height: 1.1; cursor: pointer; min-height: 44px; }
    .routine-root .days button small { display: block; font-size: 10px; color: var(--dim); min-height: 11px; }
    .routine-root .days button.active { border-color: var(--acc); color: var(--text); }
    .routine-root .days button.active small { color: var(--acc); }
    .routine-root .save { background: var(--acc); color: #1a1606; border: 0; border-radius: 8px; font-weight: 700; padding: 0 14px; min-height: 44px; cursor: pointer; }
    .routine-root .status { min-height: 0; color: var(--mute); margin: 0 0 8px; font-size: 14px; }
    .routine-root .status:empty { display: none; }
    .routine-root .status.err { color: #ff8a9a; }
    .routine-root .status.ok { color: #8fc5a0; }
    .routine-root .rest { color: var(--mute); font-style: italic; margin: 8px 0 12px; }
    .routine-root .ex { display: flex; background: var(--card); border: 1px solid var(--line); border-radius: 12px; margin-bottom: 8px; overflow: hidden; }
    .routine-root .ex.dragging { opacity: .55; border-color: var(--acc); }
    .routine-root .ex.drop-before { box-shadow: 0 -3px 0 var(--acc); }
    .routine-root .ex.drop-after { box-shadow: 0 3px 0 var(--acc); }
    .routine-root .rail { width: 40px; flex: none; display: flex; flex-direction: column; align-items: center; gap: 2px; padding: 4px 0; border-right: 1px solid var(--line); }
    .routine-root .rail button { background: none; border: 0; color: var(--mute); width: 40px; height: 40px; font-size: 15px; padding: 0; cursor: pointer; }
    .routine-root .rail button:disabled { opacity: .3; cursor: default; }
    .routine-root .rail .grip { font-size: 14px; letter-spacing: -2px; color: var(--dim); cursor: grab; touch-action: none; }
    .routine-root .rail .del { margin-top: auto; color: #8a5a5f; font-size: 13px; }
    .routine-root .ex-body { flex: 1; min-width: 0; padding: 8px 10px; }
    .routine-root .h { display: flex; align-items: center; gap: 8px; }
    .routine-root .h .n { flex: 1; min-width: 0; background: none; border: 1px solid transparent; border-radius: 6px; color: var(--text); font-weight: 600; font-size: 16px; padding: 4px 4px; margin-left: -5px; text-overflow: ellipsis; }
    .routine-root .h .n:focus { outline: none; border-color: var(--line); background: var(--field); }
    .routine-root .pill { appearance: none; -webkit-appearance: none; font-size: 11px; font-weight: 600; border-radius: 999px; padding: 3px 9px; border: 1px solid #3a3a40; color: var(--mute); background: transparent; cursor: pointer; }
    .routine-root .pill option { background: #1b1b1d; color: var(--text); }
    .routine-root .pill.trivial { color: #9aa0aa; border-color: #3a3a40; }
    .routine-root .pill.easy { color: #8fc5a0; border-color: #2f4a38; }
    .routine-root .pill.solid, .routine-root .pill.good { color: #e6d36a; border-color: #5a5230; }
    .routine-root .pill.hard { color: #e7a07a; border-color: #5a3a2c; }
    .routine-root .pill.unachievable { color: #e07a7a; border-color: #5a2c2c; }
    .routine-root .pillwrap { position: relative; flex: none; }
    .routine-root .pillwrap .pill { padding-right: 20px; }
    .routine-root .pillwrap::after { content: "▾"; position: absolute; right: 8px; top: 50%; transform: translateY(-50%); font-size: 10px; color: var(--mute); pointer-events: none; }
    .routine-root .nums { display: grid; grid-template-columns: repeat(3, 1fr); gap: 6px; margin-top: 6px; }
    .routine-root .num { position: relative; background: var(--field); border: 1px solid var(--line); border-radius: 9px; height: 46px; text-align: center; }
    .routine-root .num label { position: absolute; top: 3px; left: 0; right: 0; font-size: 10px; letter-spacing: .06em; text-transform: uppercase; opacity: .85; pointer-events: none; }
    .routine-root .num .v { gap: 0; display: flex; align-items: center; justify-content: space-between; height: 100%; padding-top: 9px; }
    .routine-root .num .v input { width: 100%; min-width: 0; flex: 1; background: none; border: 0; padding: 0; text-align: center; font-size: 19px; font-weight: 600; font-variant-numeric: tabular-nums; -moz-appearance: textfield; }
    .routine-root .num .v input::-webkit-outer-spin-button, .routine-root .num .v input::-webkit-inner-spin-button { -webkit-appearance: none; margin: 0; }
    .routine-root .num .v input:focus { outline: none; }
    .routine-root .num:focus-within { border-color: #4a4a50; }
    .routine-root .num .v button { flex: none; background: none; border: 0; color: var(--mute); width: 28px; height: 44px; margin-top: -9px; padding-top: 9px; font-size: 18px; cursor: pointer; }
    .routine-root .num.weight label, .routine-root .num.weight input { color: #00FFFF; }
    .routine-root .num.reps label, .routine-root .num.reps input { color: #F1C40F; }
    .routine-root .num.sets label, .routine-root .num.sets input { color: #00FF00; }
    .routine-root .notes { display: block; width: 100%; margin-top: 8px; background: var(--field); border: 1px solid #34343a; border-radius: 9px; padding: 9px 11px; color: var(--text); font-size: 1rem; line-height: 1.5; min-height: calc(4 * 1.5rem + 20px); field-sizing: content; resize: none; overflow: hidden; }
    .routine-root .notes:focus { outline: none; border-color: var(--acc); }
    .routine-root .add { width: 100%; background: none; border: 1px dashed #3a3a40; color: var(--mute); border-radius: 12px; padding: 11px; margin-top: 2px; cursor: pointer; }
    .routine-root .foot { margin-top: 14px; border-top: 1px solid var(--line); }
    .routine-root .row { display: flex; width: 100%; align-items: center; gap: 8px; padding: 12px 2px; color: var(--mute); font-size: 14px; background: none; border: 0; border-bottom: 1px solid var(--line); text-align: left; cursor: pointer; }
    .routine-root .row .chev { display: inline-block; width: 10px; font-size: 15px; }
    .routine-root .card { background: var(--card); border: 1px solid var(--line); border-radius: 16px; padding: 14px; }
    .routine-root .gate input, .routine-root .modal textarea { background: var(--field); color: var(--text); border: 1px solid #34343a; border-radius: 8px; padding: 8px 10px; }
    .routine-root .modal-actions button, .routine-root .gate button { background: none; color: var(--acc); border: 1px solid var(--acc); border-radius: 8px; padding: 8px 14px; cursor: pointer; font-weight: 600; }
    .routine-root .modal-actions button.primary, .routine-root .gate button.primary { background: var(--acc); color: #1a1606; }
    .routine-root .gate { position: fixed; inset: 0; background: #121212ee; display: none; align-items: center; justify-content: center; padding: 24px; z-index: 20; }
    .routine-root .gate.show { display: flex; }
    .routine-root .gate .box { width: min(420px, 100%); }
    .routine-root .hidden { display: none !important; }
    .routine-root .modal { position: fixed; inset: 0; background: #000a; display: none; align-items: center; justify-content: center; padding: 16px; z-index: 20; }
    .routine-root .modal.show { display: flex; }
    .routine-root .modal .box { width: min(720px, 100%); max-height: 90vh; overflow: auto; }
    .routine-root .modal textarea { width: 100%; min-height: 420px; font-family: ui-monospace, monospace; font-size: 13px; }

    .routine-root .rest-defaults { display:flex; align-items:center; gap:6px; flex-wrap:nowrap; margin:0 0 12px; padding:10px 10px; background:var(--card); border:1px solid var(--line); border-radius:12px; overflow-x:auto; }
    .routine-root .rest-defaults .lab { color:var(--mute); font-size:12px; margin-right:2px; white-space:nowrap; flex:none; }
    .routine-root .rest-chip { appearance:none; background:#141416; border:1px solid var(--line); color:var(--text); border-radius:999px; padding:8px 10px; font-size:12px; font-weight:600; cursor:pointer; min-height:40px; white-space:nowrap; flex:none; }
    .routine-root .rest-chip:hover { border-color:var(--acc); }
    .routine-root .rest-chip span { color:var(--mute); font-weight:500; margin-left:6px; font-variant-numeric:tabular-nums; }
    .routine-root .rest-tag { appearance:none; background:none; border:1px solid var(--line); color:var(--mute); border-radius:999px; padding:4px 10px; font-size:12px; font-weight:600; cursor:pointer; margin-left:auto; flex:none; }
    .routine-root .rest-tag.override { color:var(--acc); border-color:#5a5230; }
    .routine-root .picker-box { width:min(360px,100%); text-align:center; }
    .routine-root .picker-box h2 { margin:0 0 4px; font-size:18px; }
    .routine-root .picker-box .sub { color:var(--mute); font-size:13px; margin:0 0 14px; }
    .routine-root .wheel { display:flex; align-items:center; justify-content:center; gap:10px; margin:8px 0 16px; }
    .routine-root .wheel button { width:48px; height:48px; border-radius:12px; border:1px solid var(--line); background:#141416; color:var(--text); font-size:22px; cursor:pointer; }
    .routine-root .wheel .val { font-size:42px; font-weight:700; font-variant-numeric:tabular-nums; min-width:4.5ch; color:var(--acc); }
    .routine-root .picker-actions { display:flex; gap:8px; justify-content:center; flex-wrap:wrap; }
    .routine-root .picker-actions button { min-height:44px; padding:0 16px; border-radius:10px; border:1px solid var(--line); background:none; color:var(--text); cursor:pointer; font-weight:600; }
    .routine-root .picker-actions button.primary { background:var(--acc); color:#1a1606; border-color:var(--acc); }
    .routine-root .picker-actions button.ghost { color:var(--mute); }
  </style>
  <div class="layout">
    <div class="bar">
      <div class="days" id="dayTabs"></div>
      <button type="button" class="save" id="saveBtn">Save</button>
    </div>
    <div class="status" id="status"></div>
    <div class="rest-defaults" id="restDefaultsRow" aria-label="Rest defaults">
      <span class="lab">Rest defaults</span>
      <button type="button" class="rest-chip" data-rating="easy" id="defEasy">Short<span>0:19</span></button>
      <button type="button" class="rest-chip" data-rating="good" id="defGood">Medium<span>0:39</span></button>
      <button type="button" class="rest-chip" data-rating="hard" id="defHard">Long<span>0:59</span></button>
    </div>
    <p class="rest hidden" id="restHint">Rest day — add an exercise to train.</p>
    <div id="exercises"></div>
    <button type="button" class="add" id="addExBtn">+ Add exercise</button>
    <div class="foot">
      <button type="button" class="row" id="pasteBtn"><span class="chev">›</span>Edit as JSON</button>
    </div>
  </div>
  <div class="gate" id="gate">
    <div class="box card">
      <h2>Profile token</h2>
      <p>Paste the token from your connect page. It is the same value saved in the app.</p>
      <input id="tokenInput" type="text" placeholder="Token" style="width:100%;margin:12px 0;" />
      <button type="button" class="primary" id="tokenBtn">Continue</button>
      <p class="status err" id="tokenErr"></p>
    </div>
  </div>
  <div class="modal" id="pasteModal">
    <div class="box card">
      <h2>Edit as JSON</h2>
      <p>This is the current week. Edit it, then apply to the form.</p>
      <textarea id="pasteText"></textarea>
      <div class="modal-actions" style="display:flex;gap:8px;margin-top:12px;">
        <button type="button" class="primary" id="pasteApply">Apply</button>
        <button type="button" id="pasteClose">Cancel</button>
      </div>
    </div>
  </div>
  <div class="modal" id="restPicker">
    <div class="box card picker-box">
      <h2 id="pickerTitle">Rest</h2>
      <p class="sub" id="pickerSub"></p>
      <div class="wheel">
        <button type="button" id="pickerMinus" aria-label="Decrease">−</button>
        <div class="val" id="pickerVal">0:39</div>
        <button type="button" id="pickerPlus" aria-label="Increase">+</button>
      </div>
      <div class="picker-actions">
        <button type="button" class="ghost hidden" id="pickerReset">Use default</button>
        <button type="button" id="pickerCancel">Cancel</button>
        <button type="button" class="primary" id="pickerOk">Set</button>
      </div>
    </div>
  </div>
  <script>
    const DAYS = ["Sun","Mon","Tue","Wed","Thu","Fri","Sat"];
    const API_BASE = ${JSON.stringify(options.apiBase)};
    const TOKEN_KEY = ${JSON.stringify(`lk_token_${options.slug}`)};
    const signedIn = ${options.signedIn === true ? "true" : "false"};
    let token = ${JSON.stringify(options.accessToken ?? "")} || sessionStorage.getItem(TOKEN_KEY) || "";
    let authRequired = false;
    let versions = [];
    let currentId = null;
    let days = emptyDays();
    let activeDay = "Mon";
    let restDefaults = { easy: 19, good: 39, hard: 59 };
    let lastPickedSec = 40;
    let pickerMode = null; // { type:'default', rating } | { type:'exercise', ex }
    let pickerSec = 39;

    function formatMss(sec) {
      sec = Math.max(0, Math.min(300, Math.round(Number(sec) || 0)));
      var m = Math.floor(sec / 60);
      var s = sec % 60;
      return m + ":" + String(s).padStart(2, "0");
    }
    function snap5(sec) {
      return Math.max(0, Math.min(300, Math.round(sec / 5) * 5));
    }
    function defaultFor(rating) {
      var r = (rating || "good").toLowerCase();
      if (typeof restDefaults[r] === "number") return restDefaults[r];
      return r === "easy" ? 19 : r === "hard" ? 59 : 39;
    }
    function paceOf(ex) {
      var p = String(ex.restPace || "").toLowerCase();
      if (p === "short" || p === "medium" || p === "long") return p;
      var r = String(ex.easyGoodOrHard || "solid").toLowerCase();
      if (r === "trivial" || r === "easy") return "short";
      if (r === "hard" || r === "unachievable") return "long";
      return "medium";
    }
    function secondsForPace(pace) {
      if (pace === "short") return defaultFor("easy");
      if (pace === "long") return defaultFor("hard");
      return defaultFor("good");
    }
    function renderRestDefaults() {
      [["easy","defEasy"],["good","defGood"],["hard","defHard"]].forEach(function (pair) {
        var el = document.getElementById(pair[1]);
        if (!el) return;
        var span = el.querySelector("span");
        if (span) span.textContent = formatMss(defaultFor(pair[0]));
      });
    }
    function openPicker(mode, initial, title, sub, showReset) {
      pickerMode = mode;
      pickerSec = snap5(initial);
      lastPickedSec = pickerSec;
      document.getElementById("pickerTitle").textContent = title;
      document.getElementById("pickerSub").textContent = sub || "";
      document.getElementById("pickerVal").textContent = formatMss(pickerSec);
      document.getElementById("pickerReset").classList.toggle("hidden", !showReset);
      document.getElementById("restPicker").classList.add("show");
    }
    function closePicker() {
      document.getElementById("restPicker").classList.remove("show");
      pickerMode = null;
    }
    function bumpPicker(dir) {
      pickerSec = snap5(pickerSec + dir * 5);
      lastPickedSec = pickerSec;
      document.getElementById("pickerVal").textContent = formatMss(pickerSec);
    }
    function commitPicker() {
      if (!pickerMode) return;
      if (pickerMode.type === "default") {
        restDefaults[pickerMode.rating] = pickerSec;
        renderRestDefaults();
        renderDay();
      } else if (pickerMode.type === "exercise") {
        pickerMode.ex.restSec = pickerSec;
        renderDay();
      }
      closePicker();
    }
    function resetExerciseRest() {
      if (!pickerMode || pickerMode.type !== "exercise") return;
      delete pickerMode.ex.restSec;
      renderDay();
      closePicker();
    }

    function emptyDays() {
      return DAYS.map(function (d) { return { dayOfWeek: d, exercises: [] }; });
    }
    function dayOf(name) {
      return days.find(function (d) { return d.dayOfWeek === name; });
    }
    function setStatus(msg, kind) {
      var el = document.getElementById("status");
      el.textContent = msg || "";
      el.className = "status" + (kind ? " " + kind : "");
    }
    function headers() {
      var h = { "Content-Type": "application/json" };
      if (token) h.Authorization = "Bearer " + token;
      return h;
    }
    function api(path, opts) {
      opts = opts || {};
      return fetch(path, {
        method: opts.method || "GET",
        headers: headers(),
        body: opts.body ? JSON.stringify(opts.body) : undefined
      }).then(function (res) {
        return res.text().then(function (text) {
          var data = null;
          try { data = text ? JSON.parse(text) : null; } catch (e) { data = { error: text }; }
          if (res.status === 401) {
            sessionStorage.removeItem(TOKEN_KEY);
            token = "";
            showGate(true);
            throw new Error("unauthorized");
          }
          if (!res.ok) throw new Error((data && data.error) || ("HTTP " + res.status));
          return data;
        });
      });
    }

    function showGate(on) {
      document.getElementById("gate").classList.toggle("show", on);
    }

    function renderVersions() {}

    function renderTabs() {
      var root = document.getElementById("dayTabs");
      root.innerHTML = "";
      DAYS.forEach(function (d) {
        var btn = document.createElement("button");
        btn.type = "button";
        var n = dayOf(d).exercises.length;
        btn.appendChild(document.createTextNode(d));
        var small = document.createElement("small");
        small.textContent = n ? String(n) : "";
        btn.appendChild(small);
        btn.setAttribute("aria-label", d + (n ? ", " + n + " exercise" + (n === 1 ? "" : "s") : ", rest day"));
        if (d === activeDay) btn.className = "active";
        btn.addEventListener("click", function () { activeDay = d; renderDay(); renderTabs(); });
        root.appendChild(btn);
      });
    }

    var NATIVE_FIELD_SIZING = typeof CSS !== "undefined" && CSS.supports && CSS.supports("field-sizing", "content");
    function autoGrow(el) {
      if (NATIVE_FIELD_SIZING) return;
      el.style.height = "auto";
      el.style.height = (el.scrollHeight + 2) + "px";
    }
    function growAll() {
      document.querySelectorAll(".routine-root .notes").forEach(autoGrow);
    }
    window.addEventListener("resize", growAll);

    function move(list, from, to) {
      if (to < 0 || to >= list.length || from === to) return;
      var item = list.splice(from, 1)[0];
      list.splice(to, 0, item);
      renderDay();
    }

    function stepper(ex, key, label, step) {
      var wrap = document.createElement("div");
      wrap.className = "num " + key;
      var lab = document.createElement("label");
      lab.textContent = label;
      var id = "f" + Math.random().toString(36).slice(2, 9);
      lab.htmlFor = id;
      var v = document.createElement("div");
      v.className = "v";
      var minus = document.createElement("button");
      minus.type = "button"; minus.textContent = "−"; minus.setAttribute("aria-label", "Less " + label.toLowerCase());
      var plus = document.createElement("button");
      plus.type = "button"; plus.textContent = "+"; plus.setAttribute("aria-label", "More " + label.toLowerCase());
      var inp = document.createElement("input");
      inp.type = "text"; inp.inputMode = "numeric"; inp.pattern = "[0-9]*"; inp.autocomplete = "off"; inp.id = id;
      inp.value = String(ex[key]);
      inp.addEventListener("input", function () {
        var clean = inp.value.replace(/[^0-9]/g, "").slice(0, 4);
        if (clean !== inp.value) inp.value = clean;
        ex[key] = Number(clean || 0);
      });
      inp.addEventListener("blur", function () { inp.value = String(ex[key]); });
      function bump(dir) {
        var cur = Number(ex[key]) || 0;
        var next = dir > 0 ? Math.floor(cur / step) * step + step : Math.ceil(cur / step) * step - step;
        ex[key] = Math.max(0, next);
        inp.value = String(ex[key]);
      }
      minus.addEventListener("click", function () { bump(-1); });
      plus.addEventListener("click", function () { bump(1); });
      v.appendChild(minus); v.appendChild(inp); v.appendChild(plus);
      wrap.appendChild(lab); wrap.appendChild(v);
      return wrap;
    }

    function railBtn(label, text, cls, fn) {
      var b = document.createElement("button");
      b.type = "button";
      b.textContent = text;
      b.setAttribute("aria-label", label);
      b.title = label;
      if (cls) b.className = cls;
      if (fn) b.addEventListener("click", fn);
      return b;
    }

    function enableDrag(grip, card, list, idx) {
      grip.addEventListener("pointerdown", function (e) {
        e.preventDefault();
        grip.setPointerCapture(e.pointerId);
        card.classList.add("dragging");
        var cards = Array.prototype.slice.call(document.querySelectorAll("#exercises .ex"));
        var target = idx;
        function over(y) {
          cards.forEach(function (c) { c.classList.remove("drop-before", "drop-after"); });
          target = idx;
          for (var i = 0; i < cards.length; i++) {
            var r = cards[i].getBoundingClientRect();
            if (y >= r.top && y <= r.bottom) {
              var before = y < r.top + r.height / 2;
              if (i !== idx) cards[i].classList.add(before ? "drop-before" : "drop-after");
              target = before ? i : i + 1;
              if (target > idx) target -= 1;
              break;
            }
          }
        }
        function onMove(ev) { over(ev.clientY); }
        function onUp() {
          grip.removeEventListener("pointermove", onMove);
          grip.removeEventListener("pointerup", onUp);
          grip.removeEventListener("pointercancel", onUp);
          card.classList.remove("dragging");
          cards.forEach(function (c) { c.classList.remove("drop-before", "drop-after"); });
          if (target !== idx) move(list, idx, target);
        }
        grip.addEventListener("pointermove", onMove);
        grip.addEventListener("pointerup", onUp);
        grip.addEventListener("pointercancel", onUp);
      });
    }

    function renderDay() {
      var list = dayOf(activeDay).exercises;
      document.getElementById("restHint").classList.toggle("hidden", list.length > 0);
      var root = document.getElementById("exercises");
      root.innerHTML = "";
      list.forEach(function (ex, idx) {
        var card = document.createElement("div");
        card.className = "ex";

        var rail = document.createElement("div");
        rail.className = "rail";
        var grip = railBtn("Drag to reorder", "⋮⋮", "grip");
        enableDrag(grip, card, list, idx);
        var up = railBtn("Move up", "↑", "", function () { move(list, idx, idx - 1); });
        var down = railBtn("Move down", "↓", "", function () { move(list, idx, idx + 1); });
        up.disabled = idx === 0;
        down.disabled = idx >= list.length - 1;
        var del = railBtn("Remove exercise", "✕", "del", function () {
          if (String(ex.title).trim() && !confirm("Remove " + ex.title + "?")) return;
          list.splice(idx, 1); renderDay(); renderTabs();
        });
        rail.appendChild(grip); rail.appendChild(up); rail.appendChild(down); rail.appendChild(del);
        card.appendChild(rail);

        var body = document.createElement("div");
        body.className = "ex-body";
        var head = document.createElement("div");
        head.className = "h";
        var title = document.createElement("input");
        title.type = "text";
        title.className = "n";
        title.value = ex.title;
        title.placeholder = "Exercise name";
        title.setAttribute("aria-label", "Exercise name");
        title.addEventListener("input", function () { ex.title = title.value; });
        var pillWrap = document.createElement("span");
        pillWrap.className = "pillwrap";
        var rating = document.createElement("select");
        rating.setAttribute("aria-label", "Effort");
        var effortChoices = [
          ["trivial", "Trivial"],
          ["easy", "Easy"],
          ["solid", "Solid"],
          ["hard", "Hard"],
          ["unachievable", "Unachievable"]
        ];
        var currentEffort = ex.easyGoodOrHard === "good" ? "solid" : (ex.easyGoodOrHard || "solid");
        effortChoices.forEach(function (pair) {
          var o = document.createElement("option");
          o.value = pair[0]; o.textContent = pair[1];
          if (currentEffort === pair[0]) o.selected = true;
          rating.appendChild(o);
        });
        rating.className = "pill " + currentEffort;
        rating.addEventListener("change", function () { ex.easyGoodOrHard = rating.value; rating.className = "pill " + rating.value; });
        pillWrap.appendChild(rating);
        head.appendChild(title);
        head.appendChild(pillWrap);
        var restTag = document.createElement("button");
        restTag.type = "button";
        var hasOverride = typeof ex.restSec === "number";
        var pace = paceOf(ex);
        var paceName = pace === "short" ? "Short" : pace === "long" ? "Long" : "Medium";
        restTag.className = "rest-tag" + (hasOverride ? " override" : "");
        restTag.textContent = hasOverride ? ("Custom " + formatMss(ex.restSec)) : (paceName + " · " + formatMss(secondsForPace(pace)));
        restTag.setAttribute("aria-label", "Rest length");
        restTag.addEventListener("click", function () {
          var order = ["short", "medium", "long"];
          if (typeof ex.restSec === "number") {
            delete ex.restSec;
            ex.restPace = paceOf(ex);
          } else {
            var i = order.indexOf(paceOf(ex));
            ex.restPace = order[(i + 1) % order.length];
            delete ex.restSec;
          }
          renderDay();
        });
        head.appendChild(restTag);
        body.appendChild(head);

        var nums = document.createElement("div");
        nums.className = "nums";
        nums.appendChild(stepper(ex, "weight", "Weight", 5));
        nums.appendChild(stepper(ex, "reps", "Reps", 1));
        nums.appendChild(stepper(ex, "sets", "Sets", 1));
        body.appendChild(nums);

        var notes = document.createElement("textarea");
        notes.className = "notes";
        notes.placeholder = "Notes / cues";
        notes.setAttribute("aria-label", "Notes");
        notes.value = ex.notes || "";
        notes.addEventListener("input", function () { ex.notes = notes.value; autoGrow(notes); });
        body.appendChild(notes);
        card.appendChild(body);
        root.appendChild(card);
      });
      growAll();
    }

    function blankExercise() {
      return { title: "", weight: 0, reps: 10, sets: 3, notes: "", easyGoodOrHard: "solid", restPace: "medium" };
    }

    function applyRecord(rec) {
      days = rec.days || emptyDays();
      if (rec.restDefaults && typeof rec.restDefaults === "object") {
        restDefaults = {
          easy: Number(rec.restDefaults.easy != null ? rec.restDefaults.easy : 19),
          good: Number(rec.restDefaults.good != null ? rec.restDefaults.good : 39),
          hard: Number(rec.restDefaults.hard != null ? rec.restDefaults.hard : 59)
        };
      }
      renderRestDefaults();
      renderTabs();
      renderDay();
    }

    function showEmpty() {
      days = emptyDays();
      activeDay = "Mon";
      renderTabs();
      renderDay();
      setStatus("No routine stored yet.");
    }

    function save() {
      var missing = days.some(function (d) {
        return d.exercises.some(function (ex) { return !String(ex.title).trim(); });
      });
      if (missing) { setStatus("Every exercise needs a title.", "err"); return; }
      setStatus("Saving…");
      api(API_BASE + "/routines", { method: "PUT", body: { days: days, restDefaults: restDefaults } }).then(function (rec) {
        applyRecord(rec);
        setStatus("Saved.", "ok");
      }).catch(function (err) { setStatus(err.message, "err"); });
    }

    document.getElementById("saveBtn").onclick = save;
    document.getElementById("addExBtn").onclick = function () {
      dayOf(activeDay).exercises.push(blankExercise());
      renderDay();
      renderTabs();
    };
    document.getElementById("pasteBtn").onclick = function () {
      document.getElementById("pasteText").value = JSON.stringify(days, null, 2);
      document.getElementById("pasteModal").classList.add("show");
    };
    document.getElementById("pasteClose").onclick = function () {
      document.getElementById("pasteModal").classList.remove("show");
    };
    document.getElementById("defEasy").onclick = function () {
      openPicker({ type: "default", rating: "easy" }, lastPickedSec || defaultFor("easy"), "Short rest", "How long a Short rest lasts", false);
    };
    document.getElementById("defGood").onclick = function () {
      openPicker({ type: "default", rating: "good" }, lastPickedSec || defaultFor("good"), "Medium rest", "How long a Medium rest lasts", false);
    };
    document.getElementById("defHard").onclick = function () {
      openPicker({ type: "default", rating: "hard" }, lastPickedSec || defaultFor("hard"), "Long rest", "How long a Long rest lasts", false);
    };
    function bindHold(el, dir) {
      var holdTimer = null, paceTimer = null;
      function start(e) {
        e.preventDefault();
        bumpPicker(dir);
        var delay = 400;
        function tick() {
          bumpPicker(dir);
          delay = Math.max(60, delay * 0.7);
          paceTimer = setTimeout(tick, delay);
        }
        holdTimer = setTimeout(tick, delay);
      }
      function stop() {
        clearTimeout(holdTimer); clearTimeout(paceTimer);
        holdTimer = paceTimer = null;
      }
      el.addEventListener("pointerdown", start);
      ["pointerup","pointerleave","pointercancel"].forEach(function (ev) {
        el.addEventListener(ev, stop);
      });
    }
    bindHold(document.getElementById("pickerMinus"), -1);
    bindHold(document.getElementById("pickerPlus"), 1);
    document.getElementById("pickerCancel").onclick = closePicker;
    document.getElementById("pickerOk").onclick = commitPicker;
    document.getElementById("pickerReset").onclick = resetExerciseRest;
    renderRestDefaults();
    document.getElementById("pasteApply").onclick = function () {
      try {
        var parsed = JSON.parse(document.getElementById("pasteText").value);
        var incoming = Array.isArray(parsed) ? parsed : (parsed && parsed.days);
        if (!Array.isArray(incoming) || incoming.length !== 7) throw new Error("Need an array of 7 days");
        days = DAYS.map(function (name) {
          var found = incoming.find(function (d) {
            var n = String(d.dayOfWeek || "").slice(0, 3);
            return n.toLowerCase() === name.toLowerCase();
          });
          var exercises = ((found && found.exercises) || []).map(function (ex) {
            var ratingIn = ex.easyGoodOrHard || ex.rating || "solid";
            if (ratingIn === "good") ratingIn = "solid";
            var out = {
              title: ex.title || "",
              weight: Number(ex.weight || 0),
              reps: Number(ex.reps || 10),
              sets: Number(ex.sets || 3),
              notes: ex.notes || "",
              easyGoodOrHard: ratingIn
            };
            if (typeof ex.restSec === "number") out.restSec = ex.restSec;
            if (ex.restPace === "short" || ex.restPace === "medium" || ex.restPace === "long") out.restPace = ex.restPace;
            Object.keys(ex).forEach(function (k) {
              if (!(k in out) && k !== "rating" && k !== "restPace" && k !== "restSec") out[k] = ex[k];
            });
            return out;
          });
          return { dayOfWeek: name, exercises: exercises };
        });
        document.getElementById("pasteModal").classList.remove("show");
        renderTabs();
        renderDay();
        setStatus("JSON applied. Save to keep it on your profile.", "ok");
      } catch (err) {
        setStatus(err.message || "Invalid JSON", "err");
      }
    };
    document.getElementById("tokenBtn").onclick = function () {
      token = document.getElementById("tokenInput").value.trim();
      sessionStorage.setItem(TOKEN_KEY, token);
      document.getElementById("tokenErr").textContent = "";
      boot().catch(function (err) {
        document.getElementById("tokenErr").textContent = err.message;
      });
    };

    function boot() {
      return api(API_BASE + "/routines").then(function (data) {
        showGate(false);
        if (data && data.routine) applyRecord(data.routine);
        else showEmpty();
      });
    }

    if (signedIn) {
      boot().catch(function (err) { setStatus(err.message, "err"); });
    } else {
    fetch("/health").then(function (r) { return r.json(); }).then(function (h) {
      authRequired = !!h.authRequired;
      if (authRequired && !token) { showGate(true); return; }
      boot().catch(function (err) {
        if (authRequired) {
          showGate(true);
          document.getElementById("tokenErr").textContent = err.message;
        } else {
          setStatus(err.message, "err");
        }
      });
    }).catch(function () { boot(); });
    }
  </script>
</div>`;
  if (embedded) return body;
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <meta name="referrer" content="no-referrer" />
  <title>Lift Kit routines</title>
</head>
<body style="margin:0;background:#121212;font-family:ui-sans-serif,system-ui,Roboto,sans-serif;padding:10px 12px">
${revisionMark()}
${body}
</body>
</html>`;
}
