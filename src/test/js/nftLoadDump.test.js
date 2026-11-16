import assert from 'node:assert/strict';
import {spawnSync} from 'node:child_process';
import {readFile, writeFile} from 'node:fs/promises';
import {test} from 'node:test';
import {fileURLToPath} from 'node:url';
import * as loader from '../../../scripts/performance-tests/nft-load-dump.mjs';
import {loadSeededDataset, migrateAndVerifySeededDataset, restoreDatabase, verifyDatabase, verifyQaFixtures} from '../../../scripts/performance-tests/nft-load-dump.mjs';

const config = {
  target: 'nft',
  account: '448120078528',
  bucket: 'prsdb-seed-data-nft',
  key: 'seed-data.dump',
};

const validSeedCounts = '2450026|272223|1|5|277777|0|0|0|0|0|2';
const requirements = {minimumCompleted: 2450000, minimumIncomplete: 272223, minimumFreeAddresses: 12537};

test('unsupported CLI modes fail rather than starting a restore', () => {
  const script = new URL('../../../scripts/performance-tests/nft-load-dump.mjs', import.meta.url);
  for (const mode of ['--export-registration-fixtures', '--unknown']) {
    const result = spawnSync(process.execPath, [fileURLToPath(script), mode], {encoding: 'utf8'});
    assert.equal(result.status, 1);
    assert.match(result.stdout, /Unsupported NFT database operation/);
  }
});

test('journey export CLI rejects an unsupported or missing family without restoring', () => {
  const script = new URL('../../../scripts/performance-tests/nft-load-dump.mjs', import.meta.url);
  for (const args of [['--export-journey-fixtures'], ['--export-journey-fixtures', '../agent', 'output.json']]) {
    const result = spawnSync(process.execPath, [fileURLToPath(script), ...args], {encoding: 'utf8'});
    assert.equal(result.status, 1);
    assert.match(result.stdout, /Unsupported journey fixtures family/);
  }
});

const savedFixtures = Array.from({length: 92}, (_, i) => ({
  subject: `owner-${i}`, resumeJourneyId: `resume-${i}`, deleteJourneyId: `delete-${i}`, address: `${i} Fixture Road`,
}));

const propertyFixtures = Array.from({length: 109}, (_, i) => ({
  subject: `owner-${i}`, propertyOwnershipId: i + 100, address: `${i} Fixture Road`,
  files: [],
}));

const organisationFixtures = Array.from({length: 92}, (_, i) => ({
  subject: `admin-${i}`, organisationId: i + 100, registrationNumber: i + 200, name: `Fixture organisation ${i}`,
}));

for (const [family, exportFixtures, fixtures] of [
  ['saved-registration', loader.exportSavedRegistrationFixtures, savedFixtures],
  ['organisation', loader.exportOrganisationFixtures, organisationFixtures],
]) {
  for (const size of [91, 92, 102]) {
    test(`${family} export requires 92 to 101 distinct fixtures (${size})`, async () => {
      const deps = dependencies();
      const rows = Array.from({length: size}, (_, i) => ({
        ...fixtures[i % fixtures.length], subject: `boundary-${i}`,
        resumeJourneyId: `resume-boundary-${i}`, deleteJourneyId: `delete-boundary-${i}`,
        organisationId: i + 1000, registrationNumber: i + 2000,
      }));
      let written = false;
      const result = () => exportFixtures('output.json', {
        aws: deps.aws, env: deps.env, log: deps.log,
        executeCommand: async () => ({stdout: JSON.stringify(rows)}),
        save: async () => { written = true; },
      });
      if (size === 92) await result();
      else await assert.rejects(result(), /fixtures/);
      assert.equal(written, size === 92);
    });
  }
}

test('exports role-correct organisation fixtures read-only through guarded NFT tunnel', async () => {
  assert.equal(typeof loader.exportOrganisationFixtures, 'function');
  const deps = dependencies();
  const commands = [];
  let written;
  await loader.exportOrganisationFixtures('organisations.json', {
    aws: deps.aws, env: deps.env, log: deps.log,
    executeCommand: async (...args) => {
      commands.push(args);
      return {stdout: JSON.stringify(organisationFixtures)};
    },
    save: async (...args) => { written = args; },
  });
  assert.deepEqual(JSON.parse(written[1]), organisationFixtures);
  assert.equal(written[2].mode, 0o600);
  assert.equal(commands[0][2].env.PGOPTIONS, '-c default_transaction_read_only=on');
  assert.match(commands[0][1].at(-1), /u\.role = 0/);
  assert.match(commands[0][1].at(-1), /LIMIT 101/);
  assert.doesNotMatch(commands[0][1].at(-1), /\b(INSERT|UPDATE|DELETE|TRUNCATE|ALTER|DROP)\b/i);
});

for (const [description, fixtures] of [
  ['insufficient organisation capacity', organisationFixtures.slice(0, 10)],
  ['shared administrator', organisationFixtures.map(row => ({...row, subject: 'shared-admin'}))],
  ['shared organisation', organisationFixtures.map(row => ({...row, organisationId: 1}))],
  ['shared registration number', organisationFixtures.map(row => ({...row, registrationNumber: 1}))],
  ['fractional organisation ID', organisationFixtures.map(row => ({...row, organisationId: 1.5}))],
  ['blank organisation name', organisationFixtures.map(row => ({...row, name: ' '}))],
]) {
  test(`organisation export rejects ${description} before writing`, async () => {
    assert.equal(typeof loader.exportOrganisationFixtures, 'function');
    const deps = dependencies();
    let written = false;
    await assert.rejects(loader.exportOrganisationFixtures('output.json', {
      aws: deps.aws, env: deps.env, log: deps.log,
      executeCommand: async () => ({stdout: JSON.stringify(fixtures)}),
      save: async () => { written = true; },
    }), /fixtures/);
    assert.equal(written, false);
  });
}

test('exports completed-property fixtures read-only through guarded NFT tunnel', async () => {
  assert.equal(typeof loader.exportCompletedPropertyFixtures, 'function');
  const deps = dependencies();
  const commands = [];
  let written;
  await loader.exportCompletedPropertyFixtures('properties.json', {
    aws: deps.aws, env: deps.env, log: deps.log,
    executeCommand: async (...args) => {
      commands.push(args);
      return {stdout: JSON.stringify(propertyFixtures)};
    },
    save: async (...args) => { written = args; },
  });
  assert.deepEqual(JSON.parse(written[1]), propertyFixtures);
  assert.equal(written[2].mode, 0o600);
  assert.equal(commands[0][2].env.PGOPTIONS, '-c default_transaction_read_only=on');
  assert.match(commands[0][1].at(-1), /LIMIT 121/);
  assert.doesNotMatch(commands[0][1].at(-1), /\b(INSERT|UPDATE|DELETE|TRUNCATE|ALTER|DROP)\b/i);
});

for (const [description, fixtures] of [
  ['insufficient property capacity', propertyFixtures.slice(0, 108)],
  ['reused property owner', propertyFixtures.map(row => ({...row, subject: 'shared-owner'}))],
  ['reused property', propertyFixtures.map(row => ({...row, propertyOwnershipId: 1}))],
  ['unsafe property ID', propertyFixtures.map(row => ({...row, propertyOwnershipId: '../other'}))],
  ['fractional property ID', propertyFixtures.map(row => ({...row, propertyOwnershipId: 1.5}))],
]) {
  test(`completed-property export rejects ${description} before writing`, async () => {
    assert.equal(typeof loader.exportCompletedPropertyFixtures, 'function');
    const deps = dependencies();
    let written = false;
    await assert.rejects(loader.exportCompletedPropertyFixtures('output.json', {
      aws: deps.aws, env: deps.env, log: deps.log,
      executeCommand: async () => ({stdout: JSON.stringify(fixtures)}),
      save: async () => { written = true; },
    }), /fixtures/);
    assert.equal(written, false);
  });
}

test('exports bounded saved-registration fixtures read-only through guarded NFT tunnel', async () => {
  assert.equal(typeof loader.exportSavedRegistrationFixtures, 'function');
  const deps = dependencies();
  const commands = [];
  let written;
  await loader.exportSavedRegistrationFixtures('output.json', {
    aws: deps.aws, env: deps.env, log: deps.log,
    executeCommand: async (...args) => {
      commands.push(args);
      return {stdout: JSON.stringify(savedFixtures)};
    },
    save: async (...args) => { written = args; },
  });
  assert.deepEqual(JSON.parse(written[1]), savedFixtures);
  assert.equal(written[2].mode, 0o600);
  assert.equal(commands[0][2].env.PGOPTIONS, '-c default_transaction_read_only=on');
  assert.match(commands[0][1].at(-1), /LIMIT 101/);
  assert.doesNotMatch(commands[0][1].at(-1), /\b(INSERT|UPDATE|DELETE|TRUNCATE|ALTER|DROP)\b/i);
});

for (const [description, fixtures] of [
  ['insufficient capacity', savedFixtures.slice(0, 10)],
  ['reused owner', savedFixtures.map(row => ({...row, subject: 'shared-owner'}))],
  ['reused saved state', savedFixtures.map(row => ({...row, deleteJourneyId: 'shared-id'}))],
  ['unsafe journey ID', savedFixtures.map(row => ({...row, resumeJourneyId: '../other'}))],
]) {
  test(`fixture export rejects ${description} before writing a file`, async () => {
    assert.equal(typeof loader.exportSavedRegistrationFixtures, 'function');
    const deps = dependencies();
    let written = false;
    await assert.rejects(loader.exportSavedRegistrationFixtures('output.json', {
      aws: deps.aws, env: deps.env, log: deps.log,
      executeCommand: async () => ({stdout: JSON.stringify(fixtures)}),
      save: async () => { written = true; },
    }), /fixtures/);
    assert.equal(written, false);
  });
}

test('seed verification uses actual unallocated capacity and the shared read-only allocation audit', async () => {
  const commands = [];
  const counts = await verifyDatabase({}, requirements, async (...args) => {
    commands.push(args);
    return {stdout: validSeedCounts};
  });
  assert.equal(counts.freeAddresses, 277777);
  const sql = commands[0][1].at(-1);
  assert.match(sql, /cachedAddresses/);
  assert.match(sql, /subject_identifier/);
  assert.doesNotMatch(sql, /\b(INSERT|UPDATE|DELETE|TRUNCATE|ALTER|DROP)\b/i);
  assert.equal(commands[0][2].env.PGOPTIONS, '-c default_transaction_read_only=on');
  assert.equal(commands[1][1].at(-1), 'ANALYZE;');
});

for (const [description, index, value] of [
  ['overlapping completed allocations', 5, '1'],
  ['duplicate incomplete allocations', 6, '1'],
  ['missing address metadata or wrong subject', 7, '1'],
  ['ambiguous selected-address labels', 8, '1'],
  ['invalid constraints or indexes', 9, '1'],
  ['missing search indexes', 10, '1'],
  ['insufficient actual reserve', 4, '0'],
]) {
  test(`seed verification rejects ${description} before analyze`, async () => {
    const counts = validSeedCounts.split('|');
    counts[index] = value;
    let queries = 0;
    await assert.rejects(verifyDatabase({}, requirements, async () => {
      queries++;
      return {stdout: counts.join('|')};
    }), /Restored dataset/);
    assert.equal(queries, 1);
  });
}

test('post-reset verification checks fixtures read-only through the guarded NFT tunnel', async () => {
  const deps = dependencies();
  const commands = [];
  await verifyQaFixtures({
    aws: deps.aws, env: deps.env, log: deps.log,
    executeCommand: async (...args) => {
      commands.push(args);
      return {stdout: '26|0|0|2|0|60\n'};
    },
  });
  assert.equal(commands.length, 1);
  const [, args, options] = commands[0];
  assert.ok(args.includes('ON_ERROR_STOP=1'));
  const sql = args[args.indexOf('--command') + 1];
  assert.match(sql, /ownership_link/);
  assert.doesNotMatch(sql, /\b(INSERT|UPDATE|DELETE|TRUNCATE|ALTER|DROP|ANALYZE)\b/i);
  assert.equal(options.env.PGPASSWORD, deps.env.FLYWAY_PASSWORD);
  assert.equal(options.env.PGOPTIONS, '-c default_transaction_read_only=on');
  assert.equal(args.includes(deps.env.FLYWAY_PASSWORD), false);
});

for (const [description, result] of [
  ['the observed nine-property partial reset', '9|0|17|0|0|60'],
  ['a wrong address, registration or landlord link', '26|0|1|2|0|60'],
  ['leftover performance data', '27|1|0|2|0|60'],
  ['missing organisation fixtures', '26|0|0|1|0|60'],
  ['failed migrations', '26|0|0|2|1|60'],
  ['missing migration history', '26|0|0|2|0|0'],
  ['malformed database output', ''],
]) {
  test(`post-reset verification rejects ${description}`, async () => {
    const deps = dependencies();
    await assert.rejects(verifyQaFixtures({
      aws: deps.aws, env: deps.env, log: deps.log,
      executeCommand: async () => ({stdout: result}),
    }), /QA fixture verification failed/);
  });
}

test('post-reset verification refuses another account or a non-tunnel database before querying', async () => {
  const deps = dependencies();
  const executeCommand = async () => assert.fail('Must not query the database');
  await assert.rejects(verifyQaFixtures({
    env: {...deps.env, FLYWAY_URL: 'jdbc:postgresql://private-rds:5432/prsdb'},
    aws: deps.aws, executeCommand,
  }), /local SSM tunnel/);
  await assert.rejects(verifyQaFixtures({
    env: deps.env, aws: async () => ({Account: '000000000000'}), executeCommand,
  }), /Wrong NFT AWS account/);
});

test('verification-only mode migrates and checks counts without downloading or restoring a dump', async () => {
  const deps = dependencies();
  await migrateAndVerifySeededDataset({
    aws: deps.aws, env: deps.env, log: deps.log,
    migrate: deps.migrateDatabase, verify: deps.verifyDatabase,
  });
  assert.deepEqual(deps.calls.map(([kind]) => kind), ['aws', 'migrate', 'verify']);
});

test('verification-only mode keeps the localhost tunnel and account guards', async () => {
  const deps = dependencies();
  await assert.rejects(migrateAndVerifySeededDataset({
    env: {...deps.env, FLYWAY_URL: 'jdbc:postgresql://private-rds:5432/prsdb'}, aws: deps.aws,
  }), /local SSM tunnel/);
  assert.deepEqual(deps.calls, []);
  await assert.rejects(migrateAndVerifySeededDataset({
    env: deps.env, aws: async () => ({Account: '000000000000'}),
    migrate: deps.migrateDatabase,
  }), /Wrong NFT AWS account/);
  assert.deepEqual(deps.calls, []);
});

function dependencies(overrides = {}) {
  const calls = [];
  return {
    calls,
    aws: async (service, operation, input = {}) => {
      calls.push(['aws', service, operation, input]);
      if (service === 'sts') return {Account: config.account};
      if (service === 's3api' && operation === 'head-object') {
        return {ContentLength: 1024, ETag: '"dump-etag"', VersionId: 'baseline-version'};
      }
      return {};
    },
    download: async input => {
      calls.push(['download', input]);
      await writeFile(input.destinationPath, Buffer.alloc(1024));
      return input.destinationPath;
    },
    inspectArchive: async path => calls.push(['inspect', path]),
    restoreDatabase: async path => calls.push(['restore', path]),
    migrateDatabase: async () => calls.push(['migrate']),
    verifyDatabase: async (environment, requirements) => calls.push(['verify', requirements]),
    env: {
      FLYWAY_URL: 'jdbc:postgresql://localhost:5432/prsdb',
      FLYWAY_USER: 'test-user',
      FLYWAY_PASSWORD: 'test-password', // pragma: allowlist secret
    },
    log: () => {},
    ...overrides,
  };
}

test('accepts omitted dependency overrides when invoked by the CLI', async () => {
  await assert.rejects(loadSeededDataset({...config, target: 'test'}), /NFT only/);
});

test('restores with four workers after clearing the schema and preserves fail-fast flags', async () => {
  const calls = [];
  const execute = async (...args) => {
    calls.push(args);
    return {stdout: ''};
  };
  const environment = {PGPASSWORD: 'test-password', PGDATABASE: 'prsdb'}; // pragma: allowlist secret

  await restoreDatabase('/tmp/seed-data.dump', environment, execute);

  assert.equal(calls.length, 2);
  assert.equal(calls[0][0], 'docker');
  assert.ok(calls[0][1].includes('psql'));
  assert.ok(calls[0][1].includes('DROP SCHEMA public CASCADE; CREATE SCHEMA public AUTHORIZATION CURRENT_USER;'));
  assert.equal(calls[1][0], 'docker');
  assert.ok(calls[1][1].includes('pg_restore'));
  for (const flag of ['--jobs=4', '--exit-on-error', '--no-owner', '--no-acl']) {
    assert.ok(calls[1][1].includes(flag), `Missing restore flag ${flag}`);
  }
  assert.equal(calls[1][2].env.PGPASSWORD, environment.PGPASSWORD);
  assert.equal(calls[1][1].includes(environment.PGPASSWORD), false);
});

test('reports timed phases and a heartbeat without exposing restore error details', async t => {
  t.mock.timers.enable({apis: ['Date', 'setInterval'], now: 0});
  const logs = [];
  let markStarted;
  const started = new Promise(resolve => { markStarted = resolve; });
  let failRestore;
  const restoration = new Promise((resolve, reject) => { failRestore = reject; });
  const deps = dependencies({
    log: message => logs.push(message),
    restoreDatabase: async () => {
      markStarted();
      await restoration;
    },
  });
  const loading = loadSeededDataset(config, deps);
  const rejected = assert.rejects(loading, /sensitive restore details/);
  await started;
  t.mock.timers.tick(60000);
  failRestore(new Error('sensitive restore details'));
  await rejected;

  const phases = logs.filter(message => message.includes(': started'));
  assert.deepEqual(phases.map(message => message.replace(/^.*Z /, '')), [
    'Download seed dump: started',
    'Inspect PostgreSQL archive: started',
    'Restore database (4 workers): started',
  ]);
  assert.ok(logs.some(message => /Z Download seed dump: completed in \d+\.\ds$/.test(message)));
  assert.ok(logs.some(message => /Z Restore database \(4 workers\): still running \(60\.0s elapsed\)$/.test(message)));
  assert.ok(logs.some(message => /Z Restore database \(4 workers\): failed after 60\.0s$/.test(message)));
  assert.equal(logs.some(message => message.includes('sensitive restore details')), false);
  const logCount = logs.length;
  t.mock.timers.tick(60000);
  assert.equal(logs.length, logCount);
  assert.equal(deps.calls.some(([kind]) => kind === 'migrate'), false);
});

test('rejects non-NFT targets before making AWS or database calls', async () => {
  const deps = dependencies();

  await assert.rejects(loadSeededDataset({...config, target: 'test'}, deps), /NFT only/);
  assert.deepEqual(deps.calls, []);
});

test('rejects a caller in the wrong AWS account before inspecting the dump', async () => {
  const deps = dependencies({
    aws: async (...args) => {
      deps.calls.push(['aws', ...args]);
      return {Account: '000000000000'};
    },
  });

  await assert.rejects(loadSeededDataset(config, deps), /Wrong NFT AWS account/);
  assert.deepEqual(deps.calls, [['aws', 'sts', 'get-caller-identity']]);
});

test('refuses an empty S3 object without stopping to restore the database', async () => {
  const deps = dependencies({
    aws: async (service, operation, input = {}) => {
      deps.calls.push(['aws', service, operation, input]);
      if (service === 'sts') return {Account: config.account};
      return {ContentLength: 0};
    },
  });

  await assert.rejects(loadSeededDataset(config, deps), /empty or missing/);
  assert.equal(deps.calls.some(([kind]) => kind === 'restore'), false);
});

test('rejects insufficient disk space before downloading or restoring the database', async () => {
  const deps = dependencies({
    fileStatFs: async () => ({bavail: 1, bsize: 1}),
  });

  await assert.rejects(loadSeededDataset(config, deps), /Insufficient runner disk space/);
  assert.equal(deps.calls.some(([kind]) => kind === 'download'), false);
  assert.equal(deps.calls.some(([kind]) => kind === 'restore'), false);
});

test('downloads the selected immutable dump version, verifies it, restores and migrates in order', async () => {
  const logs = [];
  const deps = dependencies({log: message => logs.push(message)});

  const result = await loadSeededDataset(config, deps);

  assert.equal(result.object.versionId, 'baseline-version');
  assert.equal(result.object.etag, '"dump-etag"');
  assert.deepEqual(deps.calls.map(([kind]) => kind), [
    'aws',
    'aws',
    'download',
    'inspect',
    'restore',
    'migrate',
    'verify',
  ]);
  assert.equal(deps.calls[2][0], 'download');
  assert.deepEqual(
    Object.fromEntries(Object.entries(deps.calls[2][1]).filter(([name]) => name !== 'destinationPath')),
    {
      bucket: config.bucket,
      key: config.key,
      versionId: 'baseline-version',
      ifMatch: undefined,
    },
  );
  assert.match(deps.calls[2][1].destinationPath, /nft-seed-dump-.*seed-data\.dump$/);
  assert.deepEqual(
    deps.calls.filter(([kind]) => kind === 'aws').map(([, service, operation]) => [service, operation]),
    [['sts', 'get-caller-identity'], ['s3api', 'head-object']],
  );
  assert.deepEqual(deps.calls.find(([kind]) => kind === 'verify')[1], {
    minimumCompleted: 2450000,
    minimumIncomplete: 272223,
    minimumFreeAddresses: 1,
  });
  assert.deepEqual(logs.filter(message => message.includes(': started'))
    .map(message => message.replace(/^.*Z /, '')), [
    'Download seed dump: started',
    'Inspect PostgreSQL archive: started',
    'Restore database (4 workers): started',
    'Apply Flyway migrations: started',
    'Verify dataset and analyze: started',
  ]);
  assert.equal(logs.filter(message => /: completed in \d+\.\ds$/.test(message)).length, 5);
});

test('uses configured minimum dataset counts when verifying the restored dump', async () => {
  const deps = dependencies({
    env: {
      FLYWAY_URL: 'jdbc:postgresql://localhost:5432/prsdb',
      FLYWAY_USER: 'test-user',
      FLYWAY_PASSWORD: 'test-password', // pragma: allowlist secret
      NFT_SEED_PROPERTIES: '100',
      NFT_SEED_INCOMPLETE_PROPERTIES: '20',
      NFT_LOAD_MINIMUM_FREE_ADDRESSES: '7',
    },
  });

  await loadSeededDataset(config, deps);

  assert.deepEqual(deps.calls.find(([kind]) => kind === 'verify')[1], {
    minimumCompleted: 100,
    minimumIncomplete: 20,
    minimumFreeAddresses: 7,
  });
});

test('rejects invalid dataset count overrides before contacting AWS', async () => {
  const deps = dependencies({
    env: {
      FLYWAY_URL: 'jdbc:postgresql://localhost:5432/prsdb',
      FLYWAY_USER: 'test-user',
      FLYWAY_PASSWORD: 'test-password', // pragma: allowlist secret
      NFT_SEED_PROPERTIES: '-1',
    },
  });

  await assert.rejects(loadSeededDataset(config, deps), /NFT_SEED_PROPERTIES must be a non-negative integer/);
  assert.deepEqual(deps.calls, []);
});

test('rejects a partial download before changing the database', async () => {
  const deps = dependencies({
    fileStat: async () => ({size: 512}),
  });

  await assert.rejects(loadSeededDataset(config, deps), /size does not match S3 metadata/);
  assert.equal(deps.calls.some(([kind]) => kind === 'inspect'), false);
  assert.equal(deps.calls.some(([kind]) => kind === 'restore'), false);
});

test('rejects an invalid PostgreSQL archive before replacing the database', async () => {
  const deps = dependencies({
    inspectArchive: async path => {
      deps.calls.push(['inspect', path]);
      throw new Error('invalid archive');
    },
  });

  await assert.rejects(loadSeededDataset(config, deps), /invalid archive/);
  assert.equal(deps.calls.some(([kind]) => kind === 'restore'), false);
  assert.equal(deps.calls.some(([kind]) => kind === 'migrate'), false);
});

test('uses an ETag condition when the bucket has no version ID', async () => {
  const deps = dependencies({
    aws: async (service, operation, input = {}) => {
      deps.calls.push(['aws', service, operation, input]);
      if (service === 'sts') return {Account: config.account};
      return {ContentLength: 1024, ETag: '"dump-etag"'};
    },
  });

  await loadSeededDataset(config, deps);

  assert.equal(deps.calls[2][0], 'download');
  assert.deepEqual(
    Object.fromEntries(Object.entries(deps.calls[2][1]).filter(([name]) => name !== 'destinationPath')),
    {
      bucket: config.bucket,
      key: config.key,
      versionId: undefined,
      ifMatch: '"dump-etag"',
    },
  );
});

test('does not migrate or verify when database restoration fails', async () => {
  const deps = dependencies({
    restoreDatabase: async path => {
      deps.calls.push(['restore', path]);
      throw new Error('restore failed');
    },
  });

  await assert.rejects(loadSeededDataset(config, deps), /restore failed/);
  assert.equal(deps.calls.some(([kind]) => kind === 'migrate'), false);
  assert.equal(deps.calls.some(([kind]) => kind === 'verify'), false);
});

test('does not report success when migration fails', async () => {
  const deps = dependencies({
    migrateDatabase: async () => {
      deps.calls.push(['migrate']);
      throw new Error('migration failed');
    },
  });

  await assert.rejects(loadSeededDataset(config, deps), /migration failed/);
  assert.equal(deps.calls.some(([kind]) => kind === 'verify'), false);
});

const uuidFor = i => `00000000-0000-4000-8000-${String(i).padStart(12, '0')}`;
const journeyFixtures = (family, count = family.startsWith('organisation-team') ? 17 : 28) => Array.from({length: count}, (_, i) => {
  const row = {ownerSubject: `owner-${i}`, propertyOwnershipId: i + 1, address: `${i} Fixture Road`, files: []};
  if (family === 'joint' || family === 'joint-accept') Object.assign(row, {inviteeSubject: `invitee-${i}`, invitedEmail: `joint-${i}@example.test`});
  if (family === 'joint-accept') Object.assign(row, {token: uuidFor(i), invitationId: i + 1});
  if (family === 'agent-access') Object.assign(row, {token: uuidFor(i), invitedEmail: `agent-${i}@example.test`, hasPassword: false});
  if (family === 'offboarding') Object.assign(row, {subject: `owner-${i}`, action: 'deregister-property'});
  if (family === 'organisation-team-ramp-preparation' || family === 'organisation-team-preparation') return {subject: `admin-${i}`, name: `Organisation ${i}`, organisationId: i + 1, registrationNumber: i + 100};
  if (family.startsWith('organisation-team')) return {subject: `admin-${i}`, name: `Organisation ${i}`, organisationId: i + 1, registrationNumber: i + 100, invitationToken: uuidFor(i), invitedEmail: `team-${i}@example.test`};
  if (family === 'operator') return {subject: `operator-${i}`, councilName: 'Council', councilId: 1, targetId: i + 1, targetName: `Target ${i}`, targetEmail: `target-${i}@example.test`, targetSubject: `target-${i}`, targetManager: false};
  return row;
});
for (const family of ['organisation-team', 'organisation-team-preparation', 'organisation-team-ramp-preparation', 'organisation-team-ramp', 'joint', 'joint-accept', 'agent', 'agent-access', 'operator', 'offboarding']) {
  test(`journey export ${family} reads guarded fixtures and writes privately`, async () => {
    const fixtures = journeyFixtures(family);
    const deps = dependencies();
    let written;
    await loader.exportJourneyFixtures(family, 'output.json', {
      aws: deps.aws, env: deps.env, log: deps.log,
      executeCommand: async (_command, args, options) => {
        assert.equal(options.env.PGOPTIONS, '-c default_transaction_read_only=on');
        assert.doesNotMatch(args.at(-1), /\b(INSERT|UPDATE|DELETE|TRUNCATE|ALTER|DROP)\b/i);
        return {stdout: JSON.stringify(fixtures)};
      },
      save: async (...args) => { written = args; },
    });
    assert.deepEqual(JSON.parse(written[1]), fixtures);
    assert.equal(written[2].mode, 0o600);
  });
}
for (const [family, mutate] of [
  ['agent', rows => { rows.splice(0, 1); }],
  ['agent-access', rows => { rows[0].hasPassword = true; }],
  ['agent-access', rows => { rows[0].token = 'invalid'; }],
  ['joint-accept', rows => { rows[0].inviteeSubject = rows[1].ownerSubject; }],
  ['organisation-team', rows => { rows[1].invitationToken = rows[0].invitationToken; }],
  ['organisation-team-ramp-preparation', rows => { rows[0].invitationToken = uuidFor(1); }],
  ['organisation-team-preparation', rows => { rows[0].invitedEmail = 'existing@example.test'; }],
  ['offboarding', rows => { rows[0].action = 'deregister-landlord'; }],
  ['agent', rows => { rows[0].files = [{objectKey: '../escape', extension: 'pdf'}]; }],
  ['operator', rows => { rows[0].targetSubject = rows[1].subject; }],
]) {
  test(`journey export ${family} rejects unsafe fixtures`, async () => {
    const rows = journeyFixtures(family); mutate(rows);
    const deps = dependencies(); let written = false;
    await assert.rejects(loader.exportJourneyFixtures(family, 'output.json', {
      aws: deps.aws, env: deps.env, log: deps.log,
      executeCommand: async () => ({stdout: JSON.stringify(rows)}),
      save: async () => { written = true; },
    }));
    assert.equal(written, false);
  });
}


test('concurrent team fixture allocation is disjoint from profile and sequential pools', async () => {
  const sql = await readFile(new URL('../../main/resources/data-nft-organisation-team-ramp-fixtures.sql', import.meta.url), 'utf8');
  assert.match(sql, /OFFSET 101 LIMIT 17/);
  const sequential = await readFile(new URL('../../main/resources/data-nft-organisation-team-fixtures.sql', import.meta.url), 'utf8');
  assert.match(sequential, /OFFSET 118 LIMIT 17/);
  assert.match(sql, /LEFT JOIN organisational_landlord_invitation/);
  assert.match(sql, /json_strip_nulls/);
});

test('concurrent preparation precedes ramp and correctness journeys use fresh exports', async () => {
  const workflow = await readFile(new URL('../../../.github/workflows/run-performance-tests.yml', import.meta.url), 'utf8');
  const position = name => {
    const found = workflow.indexOf(`id: ${name}\n`);
    assert.notEqual(found, -1, name);
    return found;
  };
  for (const id of ['exportRampTeam', 'exportRampAgentAccess', 'exportRampJointAcceptance', 'exportRampOffboarding']) {
    assert.ok(position(id) < position('rampedJourneys'));
    assert.ok(workflow.includes(`steps.${id}.outcome == 'success'`));
  }
  for (const id of ['exportAgentFixtures', 'exportJointFixtures', 'exportOffboardingFixtures']) {
    assert.ok(position('rampedJourneys') < position(id));
  }
  assert.match(workflow, /gatling\.ramp\.operatorFixtureOffset=11/);
  assert.match(workflow, /gatling\.ramp\.additionalPerMinute=1/);
});


test('prepared token SQL is restricted to the reserved actor pools and recent invitations', async () => {
  const sqlFor = name => readFile(new URL(`../../main/resources/data-nft-${name}-fixtures.sql`, import.meta.url), 'utf8');
  const agent = await sqlFor('agent-access');
  const joint = await sqlFor('joint-accept');
  assert.match(agent, /OFFSET 1400 LIMIT 200/);
  assert.match(agent, /access\.created_date >= current_timestamp - interval '1 day'/);
  assert.match(agent, /access\.invited_email = 'agent-access-' \|\| property\.id/);
  assert.match(joint, /OFFSET 2200 LIMIT 200/);
  assert.match(joint, /OFFSET 2400 LIMIT 200/);
  assert.match(joint, /invitation\.created_date >= current_timestamp - interval '1 day'/);
});

// Run the actual workflow heredocs with an in-memory filesystem: no AWS, runner files or real invitations.
test('workflow token matchers select current prepared invitations and reject mismatches', async () => {
  const {runInNewContext} = await import('node:vm');
  const workflow = await readFile(new URL('../../../.github/workflows/run-performance-tests.yml', import.meta.url), 'utf8');
  const scripts = [...workflow.matchAll(/node --input-type=module -[^\n]*<<'NODE'\n([\s\S]*?)\n          NODE/g)]
    .map(match => match[1].replace(/^ {10}/gm, '').replace(/^import .*;\n/gm, ''));
  assert.equal(scripts.length, 8);
  const team = journeyFixtures('organisation-team-ramp');
  const teamPrepared = team.map(({invitationToken, invitedEmail, ...row}) => row);
  const teamCurrent = team.map(row => ({...row, invitedEmail: `org-team-123-2-${row.organisationId}-kept@example.com`}));
  const agent = journeyFixtures('agent');
  const agentCurrent = agent.map(row => ({...row, token: uuidFor(row.propertyOwnershipId),
    invitedEmail: `agent-access-${row.propertyOwnershipId}@example.test`, hasPassword: false}));
  const joint = journeyFixtures('joint');
  const jointCurrent = joint.map(row => ({...row, token: uuidFor(row.propertyOwnershipId), invitationId: row.propertyOwnershipId}));
  const organisation = journeyFixtures('organisation', 39).map((_, i) => ({
    subject: `admin-${i}`, organisationId: i + 1, registrationNumber: i + 100, name: `Organisation ${i}`,
  }));
  const sequentialTeam = organisation.slice(22).map(row => ({...row, invitationToken: uuidFor(row.organisationId),
    invitedEmail: `org-team-123-2-${row.organisationId}-kept@example.com`}));
  const makeFiles = () => ({
    '/runner/nft-organisation-team-ramp-preparation-fixtures.json': teamPrepared,
    '/runner/nft-organisation-team-ramp-fixtures.json': teamCurrent,
    '/runner/nft-agent-fixtures.json': agent,
    '/runner/nft-agent-access-fixtures.json': agentCurrent,
    '/runner/nft-joint-fixtures.json': joint,
    '/runner/nft-joint-accept-fixtures.json': jointCurrent,
    '/runner/nft-offboarding-fixtures.json': journeyFixtures('offboarding'),
    '/runner/nft-organisation-fixtures.json': organisation,
    '/runner/nft-organisation-team-preparation-fixtures.json': organisation.slice(22),
    '/runner/nft-organisation-team-fixtures.json': sequentialTeam,
  });
  const execute = (index, files, ...paths) => {
    const writes = {};
    runInNewContext(scripts[index], {
      process: {argv: ['node', '-', ...paths], env: {RUNNER_TEMP: '/runner', GITHUB_RUN_ID: '123', GITHUB_RUN_ATTEMPT: '2'}},
      readFileSync: path => JSON.stringify(files[path]),
      writeFileSync: (path, data, options) => { assert.equal(options.mode, 0o600); writes[path] = JSON.parse(data); },
      join: (...parts) => parts.join('/'),
    });
    return writes;
  };
  const files = makeFiles();
  files['/runner/nft-agent-access-fixtures.json'] = [...agentCurrent].reverse();
  const writes = execute(0, files);
  assert.deepEqual(writes['/runner/nft-ramp-agent.json'], agentCurrent);
  assert.deepEqual(writes['/runner/nft-ramp-team.json'], teamCurrent);
  assert.deepEqual(writes['/runner/nft-ramp-joint.json'], jointCurrent);
  assert.deepEqual(execute(2, files, '/runner/nft-organisation-team-preparation-fixtures.json', '/runner/nft-organisation-team-fixtures.json')
    ['/runner/nft-organisation-team-fixtures.json'], sequentialTeam);
  assert.deepEqual(execute(4, files, '/runner/nft-agent-fixtures.json', '/runner/nft-agent-access-fixtures.json')
    ['/runner/nft-agent-access-fixtures.json'], agentCurrent);
  assert.deepEqual(execute(6, files, '/runner/nft-joint-fixtures.json', '/runner/nft-joint-accept-fixtures.json')
    ['/runner/nft-joint-accept-fixtures.json'], jointCurrent);
  const newTeam = teamPrepared.map(row => ({...row, subject: `fresh-${row.subject}`, organisationId: row.organisationId + 1000}));
  const teamPaths = ['/runner/team-preparation.json', '/runner/profiles.json', '/runner/team-ramp.json'];
  const teamInputs = {[teamPaths[0]]: newTeam, [teamPaths[1]]: organisation, [teamPaths[2]]: teamCurrent};
  execute(1, teamInputs, ...teamPaths);
  const overlap = structuredClone(teamInputs);
  overlap[teamPaths[0]][0].subject = teamCurrent[0].subject;
  assert.throws(() => execute(1, overlap, ...teamPaths), /actors overlap/);
  for (const [index, family, count, field] of [
    [3, 'agent', 28, 'ownerSubject'],
    [5, 'joint', 28, 'inviteeSubject'],
    [7, 'offboarding', 17, 'subject'],
  ]) {
    const candidates = journeyFixtures(family, count + 28);
    const ramp = candidates.slice(0, count);
    const paths = [`/runner/nft-${family}-fixtures.json`, `/runner/nft-ramp-${family}.json`];
    const input = {[paths[0]]: candidates, [paths[1]]: ramp};
    const selected = execute(index, input, ...paths)[paths[0]];
    assert.deepEqual(selected, candidates.slice(count));
    assert.ok(selected.every(row => !ramp.some(used => used[field] === row[field])));
    const insufficient = {[paths[0]]: candidates.slice(0, -1), [paths[1]]: ramp};
    assert.throws(() => execute(index, insufficient, ...paths), /Insufficient correctness actors/);
  }
  for (const [index, path, mutate, args] of [
    [0, '/runner/nft-organisation-team-ramp-fixtures.json', rows => { rows[0].invitedEmail = 'old-run@example.com'; }, []],
    [0, '/runner/nft-agent-access-fixtures.json', rows => rows.pop(), []],
    [0, '/runner/nft-joint-accept-fixtures.json', rows => { rows[0].invitedEmail = 'wrong@example.com'; }, []],
    [2, '/runner/nft-organisation-team-fixtures.json', rows => { rows[0].subject = 'wrong'; }, ['/runner/nft-organisation-team-preparation-fixtures.json', '/runner/nft-organisation-team-fixtures.json']],
    [4, '/runner/nft-agent-access-fixtures.json', rows => { rows[0].invitedEmail = 'wrong@example.com'; }, ['/runner/nft-agent-fixtures.json', '/runner/nft-agent-access-fixtures.json']],
    [6, '/runner/nft-joint-accept-fixtures.json', rows => rows.pop(), ['/runner/nft-joint-fixtures.json', '/runner/nft-joint-accept-fixtures.json']],
  ]) {
    const broken = structuredClone(makeFiles());
    mutate(broken[path]);
    assert.throws(() => execute(index, broken, ...args), /invitations? .*match/i);
  }
});
