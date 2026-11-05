import { NextResponse } from "next/server";

export const dynamic = "force-dynamic";

/** Dark-mode mock of the Android rest overlay + routine rest chips for preview screenshots. */
export function GET(request: Request) {
  const url = new URL(request.url);
  const state = url.searchParams.get("state") || "counting"; // counting | go | defaults | picker | rows
  const html = `<!DOCTYPE html>
<html lang="en"><head>
<meta charset="utf-8"/><meta name="viewport" content="width=device-width,initial-scale=1"/>
<title>Rest preview</title>
<style>
  :root { --bg:#121212; --card:#1b1b1d; --line:#2c2c30; --text:#ececec; --mute:#9a9aa2; --dim:#6e6e76; --acc:#F9F72E; }
  * { box-sizing:border-box; }
  body { margin:0; background:var(--bg); color:var(--text); font-family:ui-sans-serif,system-ui,Roboto,sans-serif; }
  .phone { width:390px; min-height:844px; margin:0 auto; padding:16px; position:relative; }
  .title { background:#5B2C6F; color:var(--acc); font-size:28px; font-weight:800; text-align:center; padding:10px; border-radius:12px 12px 0 0; }
  .ring-wrap { display:flex; flex-direction:column; align-items:center; padding:24px 12px; background:#121212ee; border:1px solid var(--line); border-radius:0 0 16px 16px; min-height:640px; }
  .ring { width:220px; height:220px; border-radius:50%; border:10px solid #3a3a40; border-top-color:var(--acc); display:flex; align-items:center; justify-content:center; font-size:48px; font-weight:800; font-variant-numeric:tabular-nums; color:var(--acc); margin-top:20px; }
  .ring.go { border-color:var(--acc); color:var(--acc); background:#2a2408; }
  .next { color:var(--mute); margin-top:14px; font-size:16px; }
  .src { color:var(--dim); margin-top:6px; font-size:13px; }
  .btns { display:flex; gap:8px; width:100%; margin-top:28px; }
  .btns button { flex:1; height:56px; border-radius:12px; border:1px solid var(--line); background:#1b1b1d; color:var(--text); font-size:18px; font-weight:700; }
  .go-label { font-size:48px; font-weight:800; color:var(--acc); margin-top:18px; }
  .rest-defaults { display:flex; align-items:center; gap:8px; flex-wrap:wrap; margin:16px 0; padding:10px 12px; background:var(--card); border:1px solid var(--line); border-radius:12px; }
  .rest-defaults .lab { color:var(--mute); font-size:13px; }
  .chip { background:#141416; border:1px solid var(--line); color:var(--text); border-radius:999px; padding:8px 12px; font-size:13px; font-weight:600; }
  .chip span { color:var(--mute); margin-left:6px; }
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
</style></head><body><div class="phone">
${state === "defaults" || state === "rows" || state === "picker" ? `
  <div class="rest-defaults"><span class="lab">Rest defaults</span>
    <span class="chip">Easy<span>0:20</span></span>
    <span class="chip">Good<span>0:40</span></span>
    <span class="chip">Hard<span>1:00</span></span>
  </div>
  <div class="ex"><div class="n">Bench Press</div><span class="tag">⏱ 1:30</span></div>
  <div class="ex"><div class="n">Incline Press</div><span class="tag def">⏱ default</span></div>
  <div class="ex"><div class="n">Cable Fly</div><span class="tag def">⏱ default</span></div>
` : `
  <div class="title">Bench Press</div>
  <div class="ring-wrap">
    <div class="ring ${state === "go" ? "go" : ""}">${state === "go" ? "Go" : "1:00"}</div>
    <div class="next">Next · Incline Press · 45</div>
    <div class="src">Hard · 1:00</div>
    ${state === "go" ? `<div class="go-label">Go</div>` : `<div class="btns"><button>−15s</button><button>+15s</button><button>Skip</button></div>`}
  </div>
`}
${state === "picker" ? `
  <div class="picker"><div class="box">
    <h2 style="margin:0">Bench Press</h2>
    <p style="color:var(--mute);font-size:13px">Override for this exercise</p>
    <div class="wheel"><button>−</button><div class="val">1:30</div><button>+</button></div>
    <div class="actions"><button>Use default</button><button>Cancel</button><button class="primary">Set</button></div>
  </div></div>` : ""}
</div></body></html>`;
  return new NextResponse(html, { headers: { "content-type": "text/html; charset=utf-8" } });
}
