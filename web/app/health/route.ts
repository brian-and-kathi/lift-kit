import { json, run } from "../../lib/http";

export const dynamic = "force-dynamic";

function dbHost(): string | null {
  const raw = process.env.DATABASE_URL;
  if (!raw) return null;
  try {
    return new URL(raw).hostname;
  } catch {
    const m = /@([^/?]+)/.exec(raw);
    return m ? m[1] : null;
  }
}

export function GET(request: Request) {
  return run(request, async () =>
    json({
      service: "lift-kit",
      ok: true,
      authRequired: true,
      dbHost: dbHost(),
    }),
  );
}

export function OPTIONS(request: Request) {
  return run(request, async () => json({}));
}
