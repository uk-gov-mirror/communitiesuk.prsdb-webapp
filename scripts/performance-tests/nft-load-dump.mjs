import {execFile} from 'node:child_process';
import {promisify} from 'node:util';
import {mkdtemp, readFile, rm, stat, statfs, writeFile} from 'node:fs/promises';
import {tmpdir} from 'node:os';
import {join, resolve} from 'node:path';
import {pathToFileURL} from 'node:url';
import {account, awsCommand, region, requireCondition} from './nft-shared.mjs';

const execute = promisify(execFile);
const bucket = 'prsdb-seed-data-nft';
const key = 'seed-data.dump';
const postgresImage = 'postgres:16';
const database = 'prsdb';

function nonNegativeInteger(env, name, defaultValue) {
  const value = env[name] || String(defaultValue);
  requireCondition(/^\d+$/.test(value), `${name} must be a non-negative integer`);
  const parsed = Number(value);
  requireCondition(Number.isSafeInteger(parsed), `${name} must be a non-negative integer`);
  return parsed;
}

function datasetRequirements(env) {
  return {
    minimumCompleted: nonNegativeInteger(env, 'NFT_SEED_PROPERTIES', 2450000),
    minimumIncomplete: nonNegativeInteger(env, 'NFT_SEED_INCOMPLETE_PROPERTIES', 272223),
    minimumFreeAddresses: nonNegativeInteger(env, 'NFT_LOAD_MINIMUM_FREE_ADDRESSES', 1),
  };
}

async function run(command, args, options = {}) {
  try {
    return await execute(command, args, {
      ...options,
      timeout: options.timeout ?? 3600000,
      maxBuffer: options.maxBuffer ?? 4 * 1024 * 1024,
    });
  } catch (error) {
    throw new Error(`${command} failed (status ${error.code ?? 'unknown'})`);
  }
}

async function timedStage(name, operation, log) {
  const started = Date.now();
  const elapsed = () => ((Date.now() - started) / 1000).toFixed(1);
  const report = message => log(`${new Date().toISOString()} ${name}: ${message}`);
  report('started');
  const heartbeat = setInterval(() => report(`still running (${elapsed()}s elapsed)`), 60000);
  heartbeat.unref();
  try {
    const result = await operation();
    report(`completed in ${elapsed()}s`);
    return result;
  } catch (error) {
    report(`failed after ${elapsed()}s`);
    throw error;
  } finally {
    clearInterval(heartbeat);
  }
}

function postgresEnvironment(env = process.env) {
  requireCondition(env.FLYWAY_USER && env.FLYWAY_PASSWORD, 'NFT database credentials are required');
  requireCondition(
    env.FLYWAY_URL === 'jdbc:postgresql://localhost:5432/prsdb',
    'NFT database must be accessed through the local SSM tunnel',
  );
  return {
    PGPASSWORD: env.FLYWAY_PASSWORD,
    PGHOST: '127.0.0.1',
    PGPORT: '5432',
    PGUSER: env.FLYWAY_USER,
    PGDATABASE: database,
  };
}

async function downloadDump({bucket: objectBucket, key: objectKey, versionId, ifMatch, destinationPath}) {
  const args = [
    's3api',
    'get-object',
    '--region',
    region,
    '--no-cli-pager',
    '--bucket',
    objectBucket,
    '--key',
    objectKey,
  ];
  if (versionId) args.push('--version-id', versionId);
  else args.push('--if-match', ifMatch);
  args.push(destinationPath);
  await run('aws', args);
}

function postgresDockerArgs(environment, volumeArgs, postgresArgs) {
  return [
    'run',
    '--rm',
    '--network',
    'host',
    ...Object.entries(environment).flatMap(([name]) => ['--env', name]),
    ...volumeArgs,
    postgresImage,
    ...postgresArgs,
  ];
}

async function inspectArchive(archivePath) {
  await run('docker', postgresDockerArgs({}, ['--volume', `${archivePath}:/seed-data.dump:ro`], [
    'pg_restore',
    '--list',
    '/seed-data.dump',
  ]));
}

async function runPsql(environment, sql, executeCommand = run) {
  const {stdout} = await executeCommand('docker', postgresDockerArgs(environment, [], [
    'psql',
    '--no-psqlrc',
    '--set',
    'ON_ERROR_STOP=1',
    '--tuples-only',
    '--no-align',
    '--command',
    sql,
  ]), {env: {...process.env, ...environment}});
  return stdout.trim();
}

export async function restoreDatabase(archivePath, environment, executeCommand = run) {
  await runPsql(environment, 'DROP SCHEMA public CASCADE; CREATE SCHEMA public AUTHORIZATION CURRENT_USER;', executeCommand);
  await executeCommand('docker', postgresDockerArgs(environment, ['--volume', `${archivePath}:/seed-data.dump:ro`], [
    'pg_restore',
    '--jobs=4',
    '--clean',
    '--if-exists',
    '--exit-on-error',
    '--no-owner',
    '--no-acl',
    '--dbname',
    database,
    '/seed-data.dump',
  ]), {env: {...process.env, ...environment}});
}

async function migrateDatabase(env = process.env) {
  await run('./gradlew', ['flywayMigrate', 'flywayValidate', '--no-daemon', '--console=plain'], {
    cwd: process.cwd(),
    env: {...env, FLYWAY_LOCATIONS: 'filesystem:src/main/resources/db/migrations'},
  });
}

export async function verifyDatabase(environment, requirements, executeCommand = run) {
  const sql = await readFile(new URL('../../src/main/resources/data-nft-seed-verify.sql', import.meta.url), 'utf8');
  const counts = await runPsql(
    {...environment, PGOPTIONS: '-c default_transaction_read_only=on'},
    sql,
    executeCommand,
  );
  const values = counts.split('|').map(Number);
  requireCondition(
    values.length === 11 && values.every(value => Number.isSafeInteger(value) && value >= 0),
    'Restored dataset verification returned malformed counts',
  );
  const [completed, incomplete, fixtureLandlord, lookupAddresses, freeAddresses] = values;
  requireCondition(
    values.slice(5, 10).every(count => count === 0) && values[10] === 2,
    'Restored dataset has overlapping, duplicate, invalid or ambiguous incomplete allocations, or an incomplete schema',
  );
  requireCondition(
    completed >= requirements.minimumCompleted,
    'Restored dataset has fewer completed registrations than expected',
  );
  requireCondition(
    incomplete >= requirements.minimumIncomplete,
    'Restored dataset has fewer incomplete registrations than expected',
  );
  requireCondition(fixtureLandlord > 0, 'Required Gatling landlord is missing from the restored dataset');
  requireCondition(lookupAddresses >= 5, 'Required unambiguous performance lookup addresses are missing from the restored dataset');
  requireCondition(
    freeAddresses >= requirements.minimumFreeAddresses,
    'Restored dataset has too few eligible free addresses for registration journeys',
  );
  await runPsql(environment, 'ANALYZE;', executeCommand);
  return {completed, incomplete, lookupAddresses, freeAddresses};
}

export async function loadSeededDataset(config, {
  aws = awsCommand,
  download = downloadDump,
  inspectArchive: inspect = inspectArchive,
  restoreDatabase: restore = restoreDatabase,
  migrateDatabase: migrate = migrateDatabase,
  verifyDatabase: verify = verifyDatabase,
  createTempDir = prefix => mkdtemp(join(tmpdir(), prefix)),
  removeTempDir = directory => rm(directory, {recursive: true, force: true}),
  fileStat = stat,
  fileStatFs = statfs,
  env = process.env,
  log = console.log,
} = {}) {
  requireCondition(config?.target === 'nft', 'Seed dump loading is NFT only');
  requireCondition(config.account === account, 'Unexpected NFT AWS account configuration');
  requireCondition(config.bucket === bucket && config.key === key, 'Unexpected NFT seed dump location');
  const requirements = datasetRequirements(env);
  const environment = postgresEnvironment(env);
  requireCondition((await aws('sts', 'get-caller-identity')).Account === account, 'Wrong NFT AWS account');

  const metadata = await aws('s3api', 'head-object', {Bucket: bucket, Key: key});
  const size = Number(metadata.ContentLength);
  const etag = metadata.ETag;
  const versionId = metadata.VersionId && metadata.VersionId !== 'null' ? metadata.VersionId : undefined;
  requireCondition(Number.isSafeInteger(size) && size > 0 && etag, 'NFT seed dump is empty or missing');

  const temporaryDirectory = await createTempDir('nft-seed-dump-');
  const archivePath = resolve(temporaryDirectory, 'seed-data.dump');
  try {
    const disk = await fileStatFs(temporaryDirectory);
    const availableBytes = disk.bavail * disk.bsize;
    requireCondition(availableBytes >= size, 'Insufficient runner disk space for NFT seed dump');

    await timedStage('Download seed dump', () => download({
      bucket,
      key,
      versionId,
      ifMatch: versionId ? undefined : etag,
      destinationPath: archivePath,
    }), log);
    const archive = await fileStat(archivePath);
    requireCondition(archive.size === size, 'Downloaded NFT seed dump size does not match S3 metadata');
    await timedStage('Inspect PostgreSQL archive', () => inspect(archivePath), log);
    log(
      `Validated NFT seed dump: s3://${bucket}/${key}, version ${versionId ?? 'unversioned'}, ETag ${etag}, size ${size} bytes.`,
    );

    await timedStage('Restore database (4 workers)', () => restore(archivePath, environment), log);
    await timedStage('Apply Flyway migrations', () => migrate(env), log);
    const counts = await timedStage('Verify dataset and analyze', () => verify(environment, requirements), log);
    if (counts) {
      log(
        `Restored dataset counts: ${counts.completed} completed, ${counts.incomplete} incomplete, ` +
          `${counts.lookupAddresses} reserved lookup addresses, ${counts.freeAddresses} free eligible addresses.`,
      );
    }

    return {object: {versionId, etag, size}, counts};
  } finally {
    await removeTempDir(temporaryDirectory);
  }
}

export async function migrateAndVerifySeededDataset({
  aws = awsCommand, env = process.env, migrate = migrateDatabase, verify = verifyDatabase, log = console.log,
} = {}) {
  const environment = postgresEnvironment(env);
  const requirements = datasetRequirements(env);
  requireCondition((await aws('sts', 'get-caller-identity')).Account === account, 'Wrong NFT AWS account');
  await timedStage('Apply Flyway migrations', () => migrate(env), log);
  const counts = await timedStage('Verify dataset and analyze', () => verify(environment, requirements), log);
  log(`Restored dataset counts: ${JSON.stringify(counts)}; free address count excludes completed and incomplete allocations.`);
  return counts;
}

export async function verifyQaFixtures({
  aws = awsCommand, env = process.env, executeCommand = run, log = console.log,
} = {}) {
  const environment = {...postgresEnvironment(env), PGOPTIONS: '-c default_transaction_read_only=on'};
  requireCondition((await aws('sts', 'get-caller-identity')).Account === account, 'Wrong NFT AWS account');
  const sql = await readFile(new URL('../../src/main/resources/data-nft-qa-verify.sql', import.meta.url), 'utf8');
  const result = await runPsql(environment, sql, executeCommand);
  const counts = result.split('|').map(Number);
  requireCondition(
    counts.length === 6 && counts.every(Number.isSafeInteger) &&
      counts.slice(0, 5).every((count, index) => count === [26, 0, 0, 2, 0][index]) && counts[5] > 0,
    'QA fixture verification failed: expected 26 exact properties with address, registration and landlord links, ' +
      'two organisation landlords, no incomplete registrations and successful migration history',
  );
  log('QA reset verified: all 26 expected properties and required links, both organisation landlords, no performance data.');
}

export async function exportSavedRegistrationFixtures(outputPath, {
  aws = awsCommand, env = process.env, executeCommand = run, save = writeFile, log = console.log,
} = {}) {
  requireCondition(typeof outputPath === 'string' && outputPath.trim(), 'An output path is required for saved-registration fixtures');
  const environment = {...postgresEnvironment(env), PGOPTIONS: '-c default_transaction_read_only=on'};
  requireCondition((await aws('sts', 'get-caller-identity')).Account === account, 'Wrong NFT AWS account');
  const sql = await readFile(new URL('../../src/main/resources/data-nft-saved-registration-fixtures.sql', import.meta.url), 'utf8');
  const fixtures = JSON.parse(await runPsql(environment, sql, executeCommand));
  requireCondition(Array.isArray(fixtures) && fixtures.length >= 92 && fixtures.length <= 101,
    'Saved-registration fixtures need 92 to 101 distinct owners for correctness and mixed load');
  requireCondition(fixtures.every(row =>
    ['subject', 'resumeJourneyId', 'deleteJourneyId', 'address'].every(field =>
      typeof row?.[field] === 'string' && row[field].trim())),
  'Saved-registration fixtures have missing fields');
  const ids = fixtures.flatMap(row => [row.resumeJourneyId, row.deleteJourneyId]);
  requireCondition(new Set(fixtures.map(row => row.subject)).size === fixtures.length &&
    new Set(ids).size === ids.length && ids.every(id => /^[A-Za-z0-9-]+$/.test(id)),
  'Saved-registration fixtures contain shared owners, reused states or unsafe IDs');
  await save(outputPath, `${JSON.stringify(fixtures)}\n`, {mode: 0o600});
  log(`Exported ${fixtures.length} distinct saved-registration owners; database unchanged.`);
  return fixtures;
}

export async function exportCompletedPropertyFixtures(outputPath, {
  aws = awsCommand, env = process.env, executeCommand = run, save = writeFile, log = console.log,
} = {}) {
  requireCondition(typeof outputPath === 'string' && outputPath.trim(), 'An output path is required for completed-property fixtures');
  const environment = {...postgresEnvironment(env), PGOPTIONS: '-c default_transaction_read_only=on'};
  requireCondition((await aws('sts', 'get-caller-identity')).Account === account, 'Wrong NFT AWS account');
  const sql = await readFile(new URL('../../src/main/resources/data-nft-completed-property-fixtures.sql', import.meta.url), 'utf8');
  const fixtures = JSON.parse(await runPsql(environment, sql, executeCommand));
  requireCondition(Array.isArray(fixtures) && fixtures.length >= 109 && fixtures.length <= 121,
    'Completed-property fixtures need 109 to 121 distinct owners for correctness and mixed load');
  requireCondition(fixtures.every(row =>
    typeof row?.subject === 'string' && row.subject.trim() &&
    typeof row?.address === 'string' && row.address.trim() &&
    Number.isSafeInteger(row?.propertyOwnershipId) && row.propertyOwnershipId > 0),
  'Completed-property fixtures have missing fields or unsafe IDs');
  requireCondition(new Set(fixtures.map(row => row.subject)).size === fixtures.length &&
    new Set(fixtures.map(row => row.propertyOwnershipId)).size === fixtures.length,
  'Completed-property fixtures contain shared owners or reused properties');
  requireOwnedCertificateFiles(fixtures);
  await save(outputPath, `${JSON.stringify(fixtures)}\n`, {mode: 0o600});
  log(`Exported ${fixtures.length} distinct completed-property owners; database unchanged.`);
  return fixtures;
}

function requireOwnedCertificateFiles(fixtures) {
  const keys = new Set();
  requireCondition(fixtures.every(row => Array.isArray(row.files) && row.files.length <= 20 &&
    row.files.every(file => {
      const match = typeof file?.objectKey === 'string' &&
        /^certificateUpload\.seed-([0-9]+)\.[a-z-]+\.member-[0-9]+$/.exec(file.objectKey);
      if (!match || match[1] !== String(row.propertyOwnershipId) ||
          !['png', 'pdf'].includes(file?.extension) || keys.has(file.objectKey)) return false;
      keys.add(file.objectKey);
      return true;
    })), 'Certificate objects must be exclusive synthetic files owned by the exported property');
}

export async function exportJourneyFixtures(family, outputPath, {
  aws = awsCommand, env = process.env, executeCommand = run, save = writeFile, log = console.log,
} = {}) {
  const schemas = {
    operator: {
      strings: ['subject', 'councilName', 'targetName', 'targetEmail', 'targetSubject'],
      integers: ['councilId', 'targetId'],
      unique: ['subject', 'targetId'],
      minimum: 28,
    },
    agent: {
      strings: ['ownerSubject', 'address'],
      integers: ['propertyOwnershipId'],
      unique: ['ownerSubject', 'propertyOwnershipId'],
      minimum: 28,
      sqlFile: 'data-nft-agent-fixtures.sql',
    },
    joint: {
      strings: ['ownerSubject', 'address', 'inviteeSubject', 'invitedEmail'],
      integers: ['propertyOwnershipId'],
      unique: ['ownerSubject', 'propertyOwnershipId', 'inviteeSubject', 'invitedEmail'],
      minimum: 28,
      sqlFile: 'data-nft-joint-invite-fixtures.sql',
    },
    'joint-accept': {
      strings: ['ownerSubject', 'address', 'token', 'inviteeSubject', 'invitedEmail'],
      integers: ['propertyOwnershipId', 'invitationId'],
      unique: ['ownerSubject', 'propertyOwnershipId', 'token', 'invitationId', 'inviteeSubject', 'invitedEmail'],
      minimum: 28,
      sqlFile: 'data-nft-joint-accept-fixtures.sql',
    },
    'agent-access': {
      strings: ['ownerSubject', 'address', 'token', 'invitedEmail'],
      integers: ['propertyOwnershipId'],
      unique: ['ownerSubject', 'propertyOwnershipId', 'token', 'invitedEmail'],
      minimum: 28,
      sqlFile: 'data-nft-agent-access-fixtures.sql',
    },
    'organisation-team-ramp-preparation': {
      strings: ['subject', 'name'],
      integers: ['organisationId', 'registrationNumber'],
      unique: ['subject', 'organisationId', 'registrationNumber'],
      minimum: 17,
      sqlFile: 'data-nft-organisation-team-ramp-fixtures.sql',
    },
    'organisation-team-ramp': {
      strings: ['subject', 'name', 'invitationToken', 'invitedEmail'],
      integers: ['organisationId', 'registrationNumber'],
      unique: ['subject', 'organisationId', 'registrationNumber', 'invitationToken', 'invitedEmail'],
      minimum: 17,
      sqlFile: 'data-nft-organisation-team-ramp-fixtures.sql',
    },
    'organisation-team-preparation': {
      strings: ['subject', 'name'],
      integers: ['organisationId', 'registrationNumber'],
      unique: ['subject', 'organisationId', 'registrationNumber'],
      minimum: 17,
      sqlFile: 'data-nft-organisation-team-fixtures.sql',
    },
    'organisation-team': {
      strings: ['subject', 'name', 'invitationToken', 'invitedEmail'],
      integers: ['organisationId', 'registrationNumber'],
      unique: ['subject', 'organisationId', 'registrationNumber', 'invitationToken'],
      minimum: 17,
      sqlFile: 'data-nft-organisation-team-fixtures.sql',
    },
    offboarding: {
      strings: ['action', 'subject', 'address'],
      integers: ['propertyOwnershipId'],
      unique: ['subject', 'propertyOwnershipId'],
      minimum: 28,
      sqlFile: 'data-nft-joint-offboarding-deregister-property-fixtures.sql',
      action: 'deregister-property',
    },
  };
  const schema = Object.hasOwn(schemas, family) && schemas[family];
  requireCondition(schema, 'Unsupported journey fixtures family');
  requireCondition(typeof outputPath === 'string' && outputPath.trim(), 'An output path is required for journey fixtures');
  const environment = {...postgresEnvironment(env), PGOPTIONS: '-c default_transaction_read_only=on'};
  requireCondition((await aws('sts', 'get-caller-identity')).Account === account, 'Wrong NFT AWS account');
  const sql = await readFile(
    new URL(`../../src/main/resources/${schema.sqlFile ?? `data-nft-${family}-fixtures.sql`}`, import.meta.url),
    'utf8',
  );
  const fixtures = JSON.parse(await runPsql(environment, sql, executeCommand));
  requireCondition(Array.isArray(fixtures) && fixtures.length >= schema.minimum && fixtures.length <= 101,
    `Journey fixtures need ${schema.minimum} to 101 distinct actors`);
  requireCondition(fixtures.every(row =>
    schema.strings.every(field => typeof row?.[field] === 'string' && row[field].trim()) &&
    schema.integers.every(field => Number.isSafeInteger(row?.[field]) && row[field] > 0)),
  'Journey fixtures have missing fields or unsafe IDs');
  requireCondition(schema.unique.every(field => new Set(fixtures.map(row => row[field])).size === fixtures.length),
    'Journey fixtures contain shared actors or records');
  if (schema.action) {
    requireCondition(fixtures.every(row => row.action === schema.action),
      `Offboarding fixtures must all select ${schema.action}`);
  }
  if (family === 'joint' || family === 'joint-accept') {
    const owners = new Set(fixtures.map(row => row.ownerSubject));
    requireCondition(fixtures.every(row => !owners.has(row.inviteeSubject)),
      'Joint invitation owners and invitees must be disjoint');
  }
  if (['agent', 'joint', 'joint-accept', 'agent-access', 'offboarding'].includes(family)) {
    requireOwnedCertificateFiles(fixtures);
  }
  if (family === 'joint-accept' || family === 'agent-access' || family === 'organisation-team' || family === 'organisation-team-ramp') {
    requireCondition(fixtures.every(row => /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
      .test(row.token ?? row.invitationToken)),
      'Prepared invitation tokens must be UUIDs');
    if (family === 'agent-access') {
      requireCondition(fixtures.every(row => row.hasPassword === false), 'Agent access requires fresh password setup');
    }
  }
  if (family === 'organisation-team-ramp-preparation' || family === 'organisation-team-preparation') {
    requireCondition(fixtures.every(row => !row.invitationToken && !row.invitedEmail),
      'Organisation preparation requires uninvited actors');
  }
  if (family === 'operator') {
    const subjects = new Set(fixtures.map(row => row.subject));
    requireCondition(fixtures.every(row => typeof row.targetManager === 'boolean' && !subjects.has(row.targetSubject)),
      'Journey fixtures contain unsafe removal targets');
  }
  await save(outputPath, `${JSON.stringify(fixtures)}\n`, {mode: 0o600});
  log(`Exported ${fixtures.length} distinct ${family} actors; database unchanged.`);
  return fixtures;
}

export async function exportOrganisationFixtures(outputPath, {
  aws = awsCommand, env = process.env, executeCommand = run, save = writeFile, log = console.log,
} = {}) {
  requireCondition(typeof outputPath === 'string' && outputPath.trim(), 'An output path is required for organisation fixtures');
  const environment = {...postgresEnvironment(env), PGOPTIONS: '-c default_transaction_read_only=on'};
  requireCondition((await aws('sts', 'get-caller-identity')).Account === account, 'Wrong NFT AWS account');
  const sql = await readFile(new URL('../../src/main/resources/data-nft-organisation-fixtures.sql', import.meta.url), 'utf8');
  const fixtures = JSON.parse(await runPsql(environment, sql, executeCommand));
  requireCondition(Array.isArray(fixtures) && fixtures.length >= 92 && fixtures.length <= 101,
    'Organisation fixtures need 92 to 101 distinct administrators for correctness and mixed load');
  requireCondition(fixtures.every(row =>
    typeof row?.subject === 'string' && row.subject.trim() &&
    typeof row?.name === 'string' && row.name.trim() &&
    Number.isSafeInteger(row?.organisationId) && row.organisationId > 0 &&
    Number.isSafeInteger(row?.registrationNumber) && row.registrationNumber > 0),
  'Organisation fixtures have missing fields or unsafe IDs');
  requireCondition(['subject', 'organisationId', 'registrationNumber'].every(field =>
    new Set(fixtures.map(row => row[field])).size === fixtures.length),
  'Organisation fixtures contain shared administrators, organisations or registration numbers');
  await save(outputPath, `${JSON.stringify(fixtures)}\n`, {mode: 0o600});
  log(`Exported ${fixtures.length} distinct organisation administrators; database unchanged.`);
  return fixtures;
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  try {
    if (process.argv[2] === '--verify-only') await migrateAndVerifySeededDataset();
    else if (process.argv[2] === '--verify-qa') await verifyQaFixtures();
    else if (process.argv[2] === '--export-saved-fixtures') await exportSavedRegistrationFixtures(process.argv[3]);
    else if (process.argv[2] === '--export-property-fixtures') await exportCompletedPropertyFixtures(process.argv[3]);
    else if (process.argv[2] === '--export-organisation-fixtures') await exportOrganisationFixtures(process.argv[3]);
    else if (process.argv[2] === '--export-journey-fixtures') await exportJourneyFixtures(process.argv[3], process.argv[4]);
    else if (process.argv[2]) throw new Error('Unsupported NFT database operation');
    else await loadSeededDataset({
      target: process.env.DEPLOYMENT_ENVIRONMENT,
      account: process.env.AWS_ACCOUNT_ID,
      bucket: process.env.NFT_SEED_DUMP_BUCKET,
      key: process.env.NFT_SEED_DUMP_KEY,
    });
    console.log('NFT database operation completed and verified.');
  } catch (error) {
    console.log(`NFT seed dump load details: ${error.message}`);
    console.error('NFT seed dump load failed.');
    process.exitCode = 1;
  }
}
