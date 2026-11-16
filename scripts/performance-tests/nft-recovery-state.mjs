import {mkdir, readFile, readdir, rename, writeFile} from 'node:fs/promises';
import {join} from 'node:path';
import {pathToFileURL} from 'node:url';
import {requireCondition} from './nft-shared.mjs';

function context({runId, attempt}) {
  requireCondition(/^[1-9][0-9]*$/.test(runId ?? ''), 'Expected a GitHub recovery run ID');
  const parsed = Number(attempt);
  requireCondition(Number.isSafeInteger(parsed) && parsed > 0, 'Expected a positive recovery attempt');
  return {runId, attempt: parsed};
}

function environment(env) {
  return context({runId: env.GITHUB_RUN_ID, attempt: env.GITHUB_RUN_ATTEMPT});
}

export function recoveryOwner(state, options) {
  const current = context(options);
  const owner = /^([1-9][0-9]*)-([1-9][0-9]*)$/.exec(state?.runId ?? '');
  requireCondition(owner && owner[1] === current.runId &&
    Number.isSafeInteger(Number(owner[2])) && Number(owner[2]) <= current.attempt,
  'Recovery state belongs to another run or a future attempt');
  return state.runId;
}

export function requireFreshAttempt(env = process.env) {
  requireCondition(environment(env).attempt === 1,
    'Performance retries require a fresh workflow run and seed restore; recovery jobs may be retried');
}

async function json(path) {
  try {
    return JSON.parse(await readFile(path, 'utf8'));
  } catch {
    throw new Error('Recovery checkpoint is missing or malformed');
  }
}

async function save(path, state) {
  await writeFile(`${path}.tmp`, `${JSON.stringify(state)}\n`, {mode: 0o600});
  await rename(`${path}.tmp`, path);
}

export async function markReadinessAttempted(path, env = process.env) {
  requireFreshAttempt(env);
  const state = await json(path);
  recoveryOwner(state, environment(env));
  state.readinessAttempted = true;
  await save(path, state);
}

export async function selectRecoveryState(directory, {runId, attempt, outputDirectory}) {
  const current = context({runId, attempt});
  const phases = {'restore-checkpoint': 1, 'restore-state': 2, 'load-checkpoint': 3, 'cleanup-state': 4, 'readiness-state': 0};
  const candidates = [];
  for (const entry of await readdir(directory, {withFileTypes: true})) {
    const match = /^nft-(restore-checkpoint|restore-state|load-checkpoint|cleanup-state|readiness-state)-([1-9][0-9]*)-([1-9][0-9]*)$/.exec(entry.name);
    if (!entry.isDirectory() || !match) continue;
    requireCondition(match[2] === current.runId && Number.isSafeInteger(Number(match[3])) &&
      Number(match[3]) <= current.attempt, 'Recovery checkpoint belongs to another run or a future attempt');
    candidates.push({path: join(directory, entry.name), phase: match[1], attempt: Number(match[3])});
  }
  candidates.sort((a, b) => b.attempt - a.attempt || phases[b.phase] - phases[a.phase]);
  const restore = candidates.find(value => value.phase !== 'readiness-state');
  requireCondition(restore, 'No restore checkpoint is available; manual recovery required');
  const state = await json(join(restore.path, 'nft-restore-state.json'));
  recoveryOwner(state, {runId, attempt: restore.attempt});

  let readiness;
  let readinessFound = false;
  for (const candidate of candidates) {
    if (!['readiness-state', 'cleanup-state'].includes(candidate.phase)) continue;
    const path = join(candidate.path, 'nft-state.json');
    try {
      await readFile(path);
    } catch (error) {
      if (error.code === 'ENOENT' && candidate.phase === 'cleanup-state') continue;
      throw new Error('Readiness checkpoint is missing or unreadable');
    }
    readiness = await json(path);
    readinessFound = true;
    requireCondition(readiness === null || readiness.marker === `PDJB-430:${state.runId}`,
      'Readiness state belongs to another restore owner');
    break;
  }
  requireCondition(!state.readinessAttempted || readinessFound,
    'Required readiness checkpoint is missing; manual recovery required');
  await mkdir(outputDirectory, {recursive: true});
  await save(join(outputDirectory, 'nft-restore-state.json'), state);
  await save(join(outputDirectory, 'nft-state.json'), readiness ?? null);
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  try {
    const command = process.argv[2];
    if (command === 'require-fresh-attempt') requireFreshAttempt();
    else {
      const directory = process.env.RUNNER_TEMP;
      requireCondition(directory, 'RUNNER_TEMP is required for recovery checkpoints');
      if (command === 'mark-readiness') {
        await markReadinessAttempted(join(directory, 'nft-restore-state.json'));
      } else if (command === 'select') {
        await selectRecoveryState(join(directory, 'nft-recovery-artifacts'), {
          ...environment(process.env), outputDirectory: directory,
        });
      } else throw new Error('Expected require-fresh-attempt, mark-readiness or select');
    }
  } catch (error) {
    console.log(`NFT recovery checkpoint details: ${error.message}`);
    console.error('NFT recovery checkpoint validation failed; operator recovery may be required.');
    process.exitCode = 1;
  }
}
