import { NextResponse } from "next/server";

export const dynamic = "force-dynamic";

/** Dark-mode mock of the Android rest overlay + routine rest chips for preview screenshots. */
export function GET(request: Request) {
  const url = new URL(request.url);
  const state = url.searchParams.get("state") || "counting"; // counting | go | defaults | picker | rows
  // Ring math: remaining/total. At 1:00 of a 1:00 rest the ring is full (not a quarter).
  const remaining = 60;
  const total = 60;
  const frac = Math.max(0, Math.min(1, remaining / total));
  const circumference = 2 * Math.PI * 100; // r=100
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
  .title { background:#5B2C6F; color:var(--acc); font-size:28px; font-weight:800; text-align:center; padding:10px; border-radius:12px 12px 0 0; }
  .ring-wrap { display:flex; flex-direction:column; align-items:center; padding:24px 12px; background:#121212ee; border:1px solid var(--line); border-radius:0 0 16px 16px; min-height:640px; }
  .ring-svg { width:220px; height:220px; margin-top:20px; }
  .ring-label { font-size:48px; font-weight:800; font-variant-numeric:tabular-nums; fill:var(--acc); }
  .ring-label.go { fill:var(--acc); }
  .next { color:var(--mute); margin-top:14px; font-size:16px; }
  .src { color:var(--dim); margin-top:6px; font-size:13px; }
  .btns { display:flex; gap:0; width:100%; margin-top:28px; align-items:stretch; }
  .btns button { height:56px; border-radius:12px; border:1px solid var(--line); background:#1b1b1d; color:var(--text); font-size:18px; font-weight:700; }
  .btns .adj { flex:1; }
  .btns .adj.minus { margin-right:4px; }
  .btns .adj.plus { margin-left:4px; margin-right:16px; }
  .btns .skip { flex:1.15; font-weight:800; border-color:#5a5230; color:var(--acc); }
  .go-label { font-size:48px; font-weight:800; color:var(--acc); margin-top:18px; }
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
  <div class="title">Bench Press</div>
  <div class="ring-wrap">
    <svg class="ring-svg" viewBox="0 0 220 220" aria-hidden="true">
      <circle cx="110" cy="110" r="100" fill="none" stroke="#3a3a40" stroke-width="10"/>
      ${state === "go" ? "" : `<circle cx="110" cy="110" r="100" fill="none" stroke="var(--acc)" stroke-width="10"
        stroke-dasharray="${dash.toFixed(2)} ${gap.toFixed(2)}"
        stroke-dashoffset="0"
        transform="rotate(-90 110 110)"/>`}
      <text x="110" y="110" text-anchor="middle" dominant-baseline="central" class="ring-label ${state === "go" ? "go" : ""}">${state === "go" ? "Go" : "1:00"}</text>
    </svg>
    <div class="next">Next · Incline Press · 45 lb</div>
    <div class="src">Hard · 1:00</div>
    ${state === "go" ? `<div class="go-label">Go</div>` : `<div class="btns"><button class="adj minus">−15s</button><button class="adj plus">+15s</button><button class="skip">Skip</button></div>`}
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
