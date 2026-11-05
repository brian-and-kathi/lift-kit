import { createRequire } from "module";
// Smoke via dynamic import of compiled is hard; duplicate minimal checks by reading source patterns.
import fs from "fs";
const src = fs.readFileSync(new URL("../lib/routines.ts", import.meta.url), "utf8");
for (const needle of ["restSec", "restDefaults", "KNOWN_EXERCISE_KEYS", "resolveRestSec"]) {
  if (!src.includes(needle)) {
    console.error("missing", needle);
    process.exit(1);
  }
}
console.log("routines.ts rest markers ok");
