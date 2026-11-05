// Binding types for lift-kit-worker.
// Re-generate with `npm run types` after wrangler config changes.
interface Env {
  WORKOUTS: KVNamespace;
  INGEST_TOKEN?: string;
}