import { fixMojibake } from "./mojibake";
import { asIso, ready, sql } from "./db";
import { json, readJsonBody } from "./http";

export const DAY_NAMES = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"] as const;
const RATINGS = new Set(["trivial", "easy", "solid", "good", "hard", "unachievable"]);
const PACES = new Set(["short", "medium", "long"]);
const ID_RE = /^[a-z0-9][a-z0-9-]{0,62}$/;
const MAX_EXERCISES = 40;
const MAX_REST_SEC = 300;
const KNOWN_EXERCISE_KEYS = new Set([
  "title",
  "weight",
  "reps",
  "sets",
  "notes",
  "easyGoodOrHard",
  "rating",
  "restSec",
  "restPace",
]);

export type RestDefaults = {
  easy: number;
  good: number;
  hard: number;
};

export type RoutineExercise = {
  title: string;
  weight: number;
  reps: number;
  sets: number;
  notes: string;
  easyGoodOrHard: string;
  restSec?: number;
  restPace?: string;
  [key: string]: unknown;
};

export type RoutineDay = {
  dayOfWeek: string;
  exercises: RoutineExercise[];
};

export type RoutineRecord = {
  id: string;
  name: string;
  updatedAt: string;
  days: RoutineDay[];
  restDefaults?: RestDefaults;
};

export function isRoutineId(id: string): boolean {
  return ID_RE.test(id);
}

/** Hardcoded fallbacks used by the phone when nothing else is set (existing off-by-one). */
export const HARDCODED_REST: RestDefaults = { easy: 19, good: 39, hard: 59 };

export function resolveRestSec(
  rating: string,
  restSec: number | undefined | null,
  defaults?: RestDefaults | null,
): number {
  if (typeof restSec === "number" && Number.isFinite(restSec) && restSec >= 0) {
    return Math.min(MAX_REST_SEC, Math.trunc(restSec));
  }
  const raw = (rating || "medium").toLowerCase();
  const pace =
    raw === "short" || raw === "medium" || raw === "long"
      ? raw
      : raw === "trivial" || raw === "easy"
        ? "short"
        : raw === "hard" || raw === "unachievable"
          ? "long"
          : "medium";
  const key = pace === "short" ? "easy" : pace === "long" ? "hard" : "good";
  const fromDefaults =
    defaults && typeof defaults[key] === "number" ? defaults[key] : undefined;
  if (typeof fromDefaults === "number" && Number.isFinite(fromDefaults) && fromDefaults >= 0) {
    return Math.min(MAX_REST_SEC, Math.trunc(fromDefaults));
  }
  return HARDCODED_REST[key];
}

export async function listRoutines(userId: string): Promise<Response> {
  const routine = await readUserRoutine(userId);
  return json({
    routine,
    routines: routine
      ? [{ id: routine.id, name: routine.name, updatedAt: routine.updatedAt }]
      : [],
  });
}

export async function getRoutine(userId: string, _id: string): Promise<Response> {
  const routine = await readUserRoutine(userId);
  if (!routine) return json({ error: "not found" }, 404);
  return json(routine);
}

async function readUserRoutine(userId: string): Promise<RoutineRecord | null> {
  await ready();
  const rows = await sql()`
    SELECT id, name, updated_at, days
    FROM routines
    WHERE user_id = ${userId}
    ORDER BY updated_at DESC
    LIMIT 1
  `;
  const row = rows[0];
  if (!row) return null;
  const parsed = parseStoredDays(row.days);
  if (!parsed) return null;
  const record: RoutineRecord = {
    id: String(row.id),
    name: String(row.name),
    updatedAt: asIso(row.updated_at),
    days: parsed.days,
  };
  if (parsed.restDefaults) record.restDefaults = parsed.restDefaults;
  return record;
}

export async function saveRoutine(userId: string, request: Request, pathId: string | null): Promise<Response> {
  await ready();
  const parsed = await readJsonBody(request);
  if (!parsed.ok) return parsed.response;
  if (!parsed.value || typeof parsed.value !== "object" || Array.isArray(parsed.value)) {
    return json({ error: "body must be an object" }, 400);
  }
  const body = parsed.value as Record<string, unknown>;
  const daysResult = normalizeDays(body.days ?? body.routine);
  if (!daysResult.ok) return json({ error: daysResult.error }, 400);

  const restDefaultsResult = normalizeRestDefaults(body.restDefaults);
  if (!restDefaultsResult.ok) return json({ error: restDefaultsResult.error }, 400);

  const suppliedName = typeof body.name === "string" ? body.name.trim() : "";
  const name = suppliedName ? suppliedName.slice(0, 80) : "Routine";
  const existing = await sql()`
    SELECT id, days FROM routines WHERE user_id = ${userId} ORDER BY updated_at DESC LIMIT 1
  `;
  const id = existing.length > 0 ? String(existing[0].id) : userId;

  // Merge unknown top-level keys from the previous payload (except managed ones).
  const previous = existing.length > 0 ? parseStoredDays(existing[0].days) : null;
  let restDefaults = restDefaultsResult.value;
  if (restDefaults === undefined && body.restDefaults === undefined && previous?.restDefaults) {
    restDefaults = previous.restDefaults;
  }

  const record: RoutineRecord = {
    id,
    name,
    updatedAt: new Date().toISOString(),
    days: daysResult.days,
  };
  if (restDefaults) record.restDefaults = restDefaults;

  const stored = serializeStoredDays(record.days, record.restDefaults);
  if (existing.length > 0) {
    await sql()`
      UPDATE routines
      SET name = ${record.name}, updated_at = ${record.updatedAt}, days = ${JSON.stringify(stored)}::jsonb
      WHERE user_id = ${userId} AND id = ${id}
    `;
    await sql()`DELETE FROM routines WHERE user_id = ${userId} AND id <> ${id}`;
  } else {
    await sql()`
      INSERT INTO routines (id, user_id, name, updated_at, days)
      VALUES (${record.id}, ${userId}, ${record.name}, ${record.updatedAt}, ${JSON.stringify(stored)}::jsonb)
    `;
  }
  console.log(JSON.stringify({ message: "routine stored", userId, replaced: existing.length > 0 }));
  return json(record, existing.length > 0 ? 200 : 201);
}

export async function deleteRoutine(userId: string, _id: string): Promise<Response> {
  await ready();
  const rows = await sql()`
    DELETE FROM routines WHERE user_id = ${userId} RETURNING id
  `;
  if (rows.length === 0) return json({ error: "not found" }, 404);
  console.log(JSON.stringify({ message: "routine deleted", userId }));
  return json({ ok: true });
}

type StoredDays = {
  days: RoutineDay[];
  restDefaults?: RestDefaults;
};

function parseStoredDays(value: unknown): StoredDays | null {
  let raw: unknown = value;
  if (typeof raw === "string") {
    try {
      raw = JSON.parse(raw) as unknown;
    } catch {
      return null;
    }
  }
  if (Array.isArray(raw)) {
    return { days: raw as RoutineDay[] };
  }
  if (raw && typeof raw === "object") {
    const obj = raw as Record<string, unknown>;
    if (Array.isArray(obj.days)) {
      const rest = normalizeRestDefaults(obj.restDefaults);
      return {
        days: obj.days as RoutineDay[],
        restDefaults: rest.ok ? rest.value : undefined,
      };
    }
  }
  return null;
}

function serializeStoredDays(days: RoutineDay[], restDefaults?: RestDefaults): unknown {
  if (!restDefaults) return days;
  return { days, restDefaults };
}

function parseDays(value: unknown): RoutineDay[] | null {
  const stored = parseStoredDays(value);
  return stored ? stored.days : null;
}

export function normalizeDays(
  input: unknown,
): { ok: true; days: RoutineDay[] } | { ok: false; error: string } {
  // Accept either a bare 7-day array or an envelope { days, restDefaults }.
  let daysInput = input;
  if (daysInput && typeof daysInput === "object" && !Array.isArray(daysInput)) {
    const obj = daysInput as Record<string, unknown>;
    if (Array.isArray(obj.days)) daysInput = obj.days;
  }
  if (!Array.isArray(daysInput)) {
    return { ok: false, error: "routine must be an array of 7 days" };
  }
  if (daysInput.length !== 7) {
    return { ok: false, error: "routine must contain exactly 7 days (Sun–Sat)" };
  }

  const byName = new Map<string, RoutineDay>();
  for (const item of daysInput) {
    if (!item || typeof item !== "object") {
      return { ok: false, error: "each day must be an object" };
    }
    const raw = item as Record<string, unknown>;
    const dayName = normalizeDayName(raw.dayOfWeek);
    if (!dayName) {
      return { ok: false, error: "dayOfWeek must be Sun, Mon, Tue, Wed, Thu, Fri, or Sat" };
    }
    if (byName.has(dayName)) {
      return { ok: false, error: `duplicate day: ${dayName}` };
    }
    const exercisesRaw = raw.exercises;
    if (exercisesRaw !== undefined && !Array.isArray(exercisesRaw)) {
      return { ok: false, error: `${dayName}: exercises must be an array` };
    }
    const exercises: RoutineExercise[] = [];
    for (const exercise of (exercisesRaw as unknown[]) ?? []) {
      const parsed = normalizeExercise(exercise);
      if (!parsed.ok) return { ok: false, error: `${dayName}: ${parsed.error}` };
      exercises.push(parsed.exercise);
    }
    if (exercises.length > MAX_EXERCISES) {
      return { ok: false, error: `${dayName}: too many exercises (max ${MAX_EXERCISES})` };
    }
    byName.set(dayName, { dayOfWeek: dayName, exercises });
  }

  const days: RoutineDay[] = [];
  for (const name of DAY_NAMES) {
    const day = byName.get(name);
    if (!day) return { ok: false, error: `missing ${name}` };
    days.push(day);
  }
  return { ok: true, days };
}

function normalizeDayName(value: unknown): string | null {
  if (typeof value === "number" && value >= 1 && value <= 7) {
    return DAY_NAMES[value - 1];
  }
  if (typeof value !== "string") return null;
  const short = value.trim().slice(0, 3);
  return DAY_NAMES.find((name) => name.toLowerCase() === short.toLowerCase()) ?? null;
}

function normalizeExercise(
  value: unknown,
): { ok: true; exercise: RoutineExercise } | { ok: false; error: string } {
  if (!value || typeof value !== "object") {
    return { ok: false, error: "exercise must be an object" };
  }
  const raw = value as Record<string, unknown>;
  const title = typeof raw.title === "string" ? fixMojibake(raw.title).trim() : "";
  if (!title) return { ok: false, error: "title is required" };
  const weight = asInt(raw.weight, 0);
  const reps = asInt(raw.reps, 10);
  const sets = asInt(raw.sets, 3);
  if (weight === null || reps === null || sets === null) {
    return { ok: false, error: `${title}: weight, reps, and sets must be numbers` };
  }
  if (weight < 0 || reps < 0 || sets < 0) {
    return { ok: false, error: `${title}: weight, reps, and sets cannot be negative` };
  }
  const notes = typeof raw.notes === "string" ? fixMojibake(raw.notes) : "";
  const ratingRaw =
    typeof raw.easyGoodOrHard === "string"
      ? raw.easyGoodOrHard
      : typeof raw.rating === "string"
        ? raw.rating
        : "solid";
  const easyGoodOrHard = RATINGS.has(ratingRaw.toLowerCase()) ? ratingRaw.toLowerCase() : "solid";

  const exercise: RoutineExercise = { title, weight, reps, sets, notes, easyGoodOrHard };

  if (typeof raw.restPace === "string" && PACES.has(raw.restPace.toLowerCase())) {
    exercise.restPace = raw.restPace.toLowerCase();
  }

  if (raw.restSec !== undefined && raw.restSec !== null && raw.restSec !== "") {
    const rest = asInt(raw.restSec, -1);
    if (rest === null || rest < 0 || rest > MAX_REST_SEC) {
      return { ok: false, error: `${title}: restSec must be 0–${MAX_REST_SEC}` };
    }
    exercise.restSec = rest;
  }

  // Preserve unknown fields so phone/web round-trips never drop them.
  for (const [key, val] of Object.entries(raw)) {
    if (KNOWN_EXERCISE_KEYS.has(key)) continue;
    if (key in exercise) continue;
    exercise[key] = val;
  }

  return { ok: true, exercise };
}

function normalizeRestDefaults(
  value: unknown,
): { ok: true; value?: RestDefaults } | { ok: false; error: string } {
  if (value === undefined || value === null) return { ok: true, value: undefined };
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    return { ok: false, error: "restDefaults must be an object" };
  }
  const raw = value as Record<string, unknown>;
  const easy = asInt(raw.easy, HARDCODED_REST.easy);
  const good = asInt(raw.good, HARDCODED_REST.good);
  const hard = asInt(raw.hard, HARDCODED_REST.hard);
  if (easy === null || good === null || hard === null) {
    return { ok: false, error: "restDefaults values must be numbers" };
  }
  if ([easy, good, hard].some((n) => n < 0 || n > MAX_REST_SEC)) {
    return { ok: false, error: `restDefaults values must be 0–${MAX_REST_SEC}` };
  }
  return { ok: true, value: { easy, good, hard } };
}

function asInt(value: unknown, fallback: number): number | null {
  if (value === undefined || value === null || value === "") return fallback;
  if (typeof value === "number" && Number.isFinite(value)) return Math.trunc(value);
  if (typeof value === "string" && Number.isFinite(Number(value))) return Math.trunc(Number(value));
  return null;
}

// Keep parseDays export shape for any callers that imported it indirectly.
void parseDays;
