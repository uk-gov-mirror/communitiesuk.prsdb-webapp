import {mkdtemp, readFile, rm, rmdir, writeFile} from 'node:fs/promises';
import {generateKeyPairSync} from 'node:crypto';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import {spawn} from 'node:child_process';
import {pathToFileURL} from 'node:url';

function port(name, fallback) {
  const value = process.env[name] ?? fallback;
  if (!/^\d+$/.test(value) || Number(value) < 1 || Number(value) > 65535) {
    throw new Error(`${name} must be a port between 1 and 65535`);
  }
  return value;
}

const appPort = port('SERVER_PORT', '8080');
const simulatorPort = port('ONE_LOGIN_SIMULATOR_PORT', '13000');
const simulatorUrl = `http://127.0.0.1:${simulatorPort}`;
const appUrl = `http://localhost:${appPort}`;
const clientId = 'prsdb-performance-local';
const directory = await mkdtemp(join(tmpdir(), 'prsdb-performance-keys-'));
const publicKeyPath = join(directory, 'public-key.pem');
const privateKeyPath = join(directory, 'private-key.pem');
try {
  const {publicKey, privateKey} = generateKeyPairSync('rsa', {
    modulusLength: 2048,
    publicKeyEncoding: {type: 'spki', format: 'pem'},
    privateKeyEncoding: {type: 'pkcs8', format: 'pem'},
  });
  await writeFile(privateKeyPath, privateKey, {mode: 0o600});
  await writeFile(publicKeyPath, publicKey);

  const response = await fetch(`${simulatorUrl}/config`, {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    signal: AbortSignal.timeout(10000),
    body: JSON.stringify({
      simulatorUrl,
      clientConfiguration: {
        clientId,
        publicKeySource: 'STATIC',
        publicKey: await readFile(publicKeyPath, 'utf8'),
        scopes: ['openid'],
        redirectUrls: [`${appUrl}/login/oauth2/code/one-login`],
        postLogoutRedirectUrls: [`${appUrl}/signout`],
        claims: [
          'https://vocab.account.gov.uk/v1/coreIdentityJWT',
          'https://vocab.account.gov.uk/v1/address',
          'https://vocab.account.gov.uk/v1/returnCode',
        ],
        identityVerificationSupported: true,
        idTokenSigningAlgorithm: 'ES256',
        clientLoCs: ['P0', 'P2'],
      },
    }),
  });
  if (!response.ok) {
    throw new Error(`Simulator configuration failed: HTTP ${response.status}: ${await response.text()}`);
  }

  const config = {
    'spring.sql.init.data-locations': [
      'classpath:data-local.sql',
      'classpath:data-metrics-local.sql',
      'classpath:data-nft-performance.sql',
      'classpath:data-placeholder-freeholders.sql',
    ],
    'spring.security.oauth2.client.registration.one-login.client-id': clientId,
    'spring.security.oauth2.client.provider.one-login.issuer-uri': `${simulatorUrl}/`,
    'spring.security.oauth2.client.provider.one-login.authorization-uri': `${simulatorUrl}/authorize`,
    'spring.security.oauth2.client.provider.one-login.token-uri': `${simulatorUrl}/token`,
    'spring.security.oauth2.client.provider.one-login.jwk-set-uri': `${simulatorUrl}/.well-known/jwks.json`,
    'spring.security.oauth2.client.provider.one-login.user-info-uri': `${simulatorUrl}/userinfo`,
    // Keep unused council-provider discovery local too; this does not implement council authentication.
    'spring.security.oauth2.client.provider.internal-access.issuer-uri': `${simulatorUrl}/`,
    'one-login.did.uri': `${simulatorUrl}/.well-known/did.json`,
    'one-login.jwt.public.key': pathToFileURL(publicKeyPath).href,
    'one-login.jwt.private.key': pathToFileURL(privateKeyPath).href,
  };
  // Windows needs the batch wrapper, which can only be spawned through a shell.
  const isWindows = process.platform === 'win32';
  const child = spawn(isWindows ? '.\\gradlew.bat' : './gradlew', [':bootRun', '--no-daemon', '--console=plain'], {
    shell: isWindows,
    stdio: 'inherit',
    env: {
      ...process.env,
      SPRING_PROFILES_ACTIVE: 'local,local-no-auth',
      SERVER_PORT: appPort,
      POSTGRES_PORT: port('POSTGRES_PORT', '5433'),
      REDIS_PORT: port('REDIS_PORT', '6379'),
      ELASTICMQ_PORT: port('ELASTICMQ_PORT', '9324'),
      SPRING_APPLICATION_JSON: JSON.stringify(config),
      JAVA_TOOL_OPTIONS: `${process.env.JAVA_TOOL_OPTIONS ?? ''} -Dspring.devtools.restart.enabled=false`,
    },
  });
  const stop = signal => child.kill(signal);
  const terminate = () => stop('SIGTERM');
  const interrupt = () => stop('SIGINT');
  process.on('SIGTERM', terminate);
  process.on('SIGINT', interrupt);
  process.exitCode = await new Promise((resolve, reject) => {
    child.once('error', reject);
    child.once('exit', code => resolve(code ?? 1));
  });
  process.off('SIGTERM', terminate);
  process.off('SIGINT', interrupt);
} finally {
  await rm(privateKeyPath, {force: true});
  await rm(publicKeyPath, {force: true});
  await rmdir(directory);
}
