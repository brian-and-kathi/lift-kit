import { NextResponse } from "next/server";

export const dynamic = "force-dynamic";

/** Dark-mode mock of the Android rest band + routine rest chips for preview screenshots. */
export function GET(request: Request) {
  const url = new URL(request.url);
  const state = url.searchParams.get("state") || "counting"; // counting | go | defaults | picker | rows
  // Ring math: remaining/total. At 1:00 of a 1:00 rest the ring is full (not a quarter).
  const remaining = 60;
  const total = 60;
  const frac = Math.max(0, Math.min(1, remaining / total));
  const circumference = 2 * Math.PI * 28; // compact ring, r=28
  const dash = circumference * frac;
  const gap = circumference - dash;

  const html = `<!DOCTYPE html>
<html lang="en"><head>
<meta charset="utf-8"/><meta name="viewport" content="width=device-width,initial-scale=1"/>
<title>Lift Kit · Rest preview</title>
<style>
  :root { --bg:#121212; --card:#1b1b1d; --line:#2c2c30; --text:#ececec; --mute:#9a9aa2; --dim:#6e6e76; --acc:#F9F72E; }
  * { box-sizing:border-box; }
  body { margin:0; background:var(--bg); color:var(--text); font-family:ui-sans-serif,system-ui,Roboto,sans-serif; }
  .phone { width:390px; min-height:844px; margin:0 auto; padding:16px; position:relative; }
  .card { background:#1e1e1e; border:1px solid #3a3a3a; border-radius:20px; min-height:812px; display:flex; flex-direction:column; overflow:hidden; }
  .title { background:#5B2C6F; color:var(--acc); font-size:28px; font-weight:800; text-align:center; padding:10px 40px; position:relative; }
  .help { position:absolute; right:10px; top:50%; width:22px; height:22px; margin-top:-11px; border:2px solid var(--acc); border-radius:50%; }
  .rest-band { margin:8px 8px 0; padding:8px; background:#242428; border:1px solid #2c2c30; border-radius:12px; }
  .rest-top { display:flex; align-items:center; gap:10px; }
  .mini-ring { width:68px; height:68px; flex:none; }
  .ring-label { font-size:16px; font-weight:800; font-variant-numeric:tabular-nums; fill:#FF6B6B; }
  .rest-meta { min-width:0; }
  .next { color:var(--mute); font-size:14px; line-height:1.25; }
  .src { color:var(--dim); margin-top:2px; font-size:12px; }
  .btns { display:flex; gap:8px; width:100%; margin-top:6px; align-items:stretch; }
  .btns button { height:40px; border-radius:12px; border:1px solid var(--line); background:#1b1b1d; color:var(--text); font-size:15px; font-weight:700; }
  .btns .adj { flex:1; }
  .btns .skip { flex:1.15; font-weight:800; border-color:#5a5230; color:var(--acc); }
  .go-label { height:40px; margin-top:6px; display:flex; align-items:center; justify-content:center; font-size:22px; font-weight:800; color:var(--acc); }
  .stats { display:flex; margin:12px 8px 0; }
  .stats > div { flex:1; text-align:center; }
  .lab { color:#bdbdbd; font-size:12px; }
  .val { font-size:40px; font-weight:800; line-height:1; margin-top:2px; }
  .val.weight { color:#ffff00; }
  .val.reps { color:#00ffff; }
  .val.sets { color:#00ff00; }
  .pie-wrap { flex:1; display:flex; align-items:center; justify-content:center; min-height:180px; }
  .set-pie { width:210px; height:210px; border-radius:50%; box-shadow:0 0 0 3px #bdbdbd; background:
      radial-gradient(circle, #1e1e1e 0 32%, transparent 33%),
      conic-gradient(#4CBB17 0 82deg, #1e1e1e 82deg 90deg, #424242 90deg 172deg, #1e1e1e 172deg 180deg, #424242 180deg 262deg, #1e1e1e 262deg 270deg, #424242 270deg 352deg, #1e1e1e 352deg 360deg);
  }
  .diff { margin:0 16px 8px; }
  .track { height:6px; border-radius:99px; background:#3a3a40; position:relative; margin:8px 10px 6px; }
  .thumb { position:absolute; left:100%; top:50%; width:24px; height:24px; border-radius:50%; background:var(--acc); transform:translate(-50%,-50%); }
  .labs { display:flex; font-size:16px; color:#9e9e9e; }
  .labs span { flex:1; }
  .labs span:nth-child(2) { text-align:center; }
  .labs span:nth-child(3) { text-align:right; }
  .labs .on { color:#fff; font-weight:700; }
  .notes { text-align:center; font-size:18px; color:#fff; margin:8px 16px 16px; }
  .rest-defaults { display:flex; align-items:center; gap:6px; flex-wrap:nowrap; margin:16px 0; padding:10px 10px; background:var(--card); border:1px solid var(--line); border-radius:12px; width:100%; }
  .rest-defaults .lab { color:var(--mute); font-size:12px; flex:none; white-space:nowrap; }
  .chip { background:#141416; border:1px solid var(--line); color:var(--text); border-radius:999px; padding:8px 10px; font-size:12px; font-weight:600; white-space:nowrap; flex:none; }
  .chip span { color:var(--mute); margin-left:5px; }
  .ex { background:var(--card); border:1px solid var(--line); border-radius:12px; padding:12px; margin-bottom:8px; display:flex; align-items:center; gap:8px; }
  .ex .n { flex:1; font-weight:700; }
  .tag { border:1px solid #5a5230; color:var(--acc); border-radius:999px; padding:4px 10px; font-size:12px; font-weight:600; }
  .tag.def { color:var(--mute); border-color:var(--line); }
  .picker { position:fixed; inset:0; background:#000a; display:flex; align-items:center; justify-content:center; }
  .picker .box { width:min(340px,92%); background:var(--card); border:1px solid var(--line); border-radius:16px; padding:18px; text-align:center; }
  .wheel { display:flex; align-items:center; justify-content:center; gap:12px; margin:16px 0; }
  .wheel .val { font-size:42px; font-weight:800; color:var(--acc); min-width:4.5ch; }
  .wheel button { width:48px; height:48px; border-radius:12px; border:1px solid var(--line); background:#141416; color:var(--text); font-size:22px; }
  .actions { display:flex; gap:8px; justify-content:center; }
  .actions button { min-height:44px; padding:0 16px; border-radius:10px; border:1px solid var(--line); background:none; color:var(--text); font-weight:600; }
  .actions .primary { background:var(--acc); color:#1a1606; border-color:var(--acc); }
  .hint { color:var(--dim); font-size:11px; margin-top:8px; }
</style></head><body><div class="phone">
${state === "defaults" || state === "rows" || state === "picker" ? `
  <div class="rest-defaults"><span class="lab">Rest defaults</span>
    <span class="chip">Easy<span>0:19</span></span>
    <span class="chip">Good<span>0:39</span></span>
    <span class="chip">Hard<span>0:59</span></span>
  </div>
  <div class="ex"><div class="n">Bench Press</div><span class="tag">⏱ 1:30</span></div>
  <div class="ex"><div class="n">Incline Press</div><span class="tag def">⏱ default</span></div>
  <div class="ex"><div class="n">Cable Fly</div><span class="tag def">⏱ default</span></div>
` : `
  <div class="card">
  <div class="title">Bench Press<span class="help" aria-hidden="true"></span></div>
  <div class="rest-band">
    <div class="rest-top">
      <svg class="mini-ring" viewBox="0 0 68 68" aria-hidden="true">
        <circle cx="34" cy="34" r="28" fill="none" stroke="#3a3a40" stroke-width="4.5"/>
        ${state === "go" ? "" : `<circle cx="34" cy="34" r="28" fill="none" stroke="#F9F72E" stroke-width="4.5"
          stroke-dasharray="${dash.toFixed(2)} ${gap.toFixed(2)}"
          stroke-dashoffset="0"
          transform="rotate(-90 34 34)"/>`}
        <text x="34" y="36" text-anchor="middle" dominant-baseline="central" class="ring-label">${state === "go" ? "Go" : "1:00"}</text>
      </svg>
      <div class="rest-meta">
        <div class="next">Next · Incline Press · 45 lb</div>
        <div class="src">Hard · 1:00</div>
      </div>
    </div>
    ${state === "go" ? `<div class="go-label">Go</div>` : `<div class="btns"><button class="adj minus">−15s</button><button class="adj plus">+15s</button><button class="skip">Skip</button></div>`}
  </div>
  <div class="stats">
    <div><div class="lab">Weight</div><div class="val weight">45</div></div>
    <div><div class="lab">Reps</div><div class="val reps">8</div></div>
    <div><div class="lab">Sets</div><div class="val sets">4</div></div>
  </div>
  <div class="pie-wrap"><div class="set-pie" aria-label="1 of 4 sets"></div></div>
  <div class="diff">
    <div class="track"><div class="thumb"></div></div>
    <div class="labs"><span>Easy</span><span>Good</span><span class="on">Hard</span></div>
  </div>
  <div class="notes">Notes: Keep elbows in</div>
  </div>
`}
${state === "picker" ? `
  <div class="picker"><div class="box">
    <h2 style="margin:0">Bench Press</h2>
    <p style="color:var(--mute);font-size:13px">Override for this exercise</p>
    <div class="wheel"><button id="m">−</button><div class="val" id="v">1:30</div><button id="p">+</button></div>
    <div class="actions"><button>Use default</button><button>Cancel</button><button class="primary">Set</button></div>
    <p class="hint">Long-press +/− to step faster</p>
  </div></div>
  <script>
  (function(){
    var sec=90, hold=null, pace=null;
    function fmt(s){var m=Math.floor(s/60),r=s%60;return m+":"+String(r).padStart(2,"0");}
    function bump(d){sec=Math.max(0,Math.min(300,sec+d*5));document.getElementById("v").textContent=fmt(sec);}
    function start(d){bump(d);var delay=400;function tick(){bump(d);delay=Math.max(60,delay*0.7);pace=setTimeout(tick,delay);} hold=setTimeout(tick,delay);}
    function stop(){clearTimeout(hold);clearTimeout(pace);hold=pace=null;}
    var m=document.getElementById("m"),p=document.getElementById("p");
    m.addEventListener("pointerdown",function(e){e.preventDefault();start(-1);});
    p.addEventListener("pointerdown",function(e){e.preventDefault();start(1);});
    ["pointerup","pointerleave","pointercancel"].forEach(function(ev){m.addEventListener(ev,stop);p.addEventListener(ev,stop);});
  })();
  </script>` : ""}
</div></body></html>`;
  return new NextResponse(html, { headers: { "content-type": "text/html; charset=utf-8" } });
}
