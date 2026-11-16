import {pathToFileURL} from 'node:url';
import {readFile} from 'node:fs/promises';
import {
  appName, appOrigin, awsCommand, cluster, requestFactory, requireCondition, servicesFactory,
  simulatorName, stable, waitFactory,
} from './nft-shared.mjs';

export async function restoreEnvironment(state, {
  aws, fetchImpl = fetch, appHealthy, log = console.log, sleep = ms => new Promise(resolve => setTimeout(resolve, ms)),
  now = Date.now, timeoutMs = 600000, pollMs = 10000,
}) {
  if (!state) {
    log('No preparation state to restore; nothing was changed.');
    return;
  }
  const {originalApp, originalSimulator, temporaryArn, securityGroup, cidr, marker, ownedSets,
    ingressAttempted, simulatorStarted, appSwitchAttempted} = state;
  const services = servicesFactory(aws);
  const wait = waitFactory({now, sleep, timeoutMs, pollMs});
  const request = requestFactory(fetchImpl, log);
  const waitApp = (arn, cleanup = false) => wait('App health', async () => {
    const {app} = await services();
    requireCondition(app.taskDefinition === arn, 'App task changed during health check');
    return stable(app, arn, originalApp.desiredCount) &&
      (appHealthy ? await appHealthy(app) : (await request(`${appOrigin}/healthcheck`))?.status === 200);
  }, cleanup);

  let appSafe = !appSwitchAttempted;
  const cleanupErrors = [];
  const cleanup = async (description, action) => {
    try {
      await action();
    } catch (error) {
      cleanupErrors.push(new Error(`${description}: ${error.message}`));
      log(`Cleanup failed (${description}); operator recovery required.`);
    }
  };

  if (appSwitchAttempted) {
    await cleanup('restore app', async () => {
      const {app} = await services();
      requireCondition([originalApp.taskDefinition, temporaryArn].includes(app.taskDefinition),
        'Unexpected app task; refusing to overwrite another deployment');
      requireCondition(app.desiredCount === originalApp.desiredCount, 'App count changed externally');
      if (app.taskDefinition === temporaryArn) {
        await aws('ecs', 'update-service', {cluster, service: appName, taskDefinition: originalApp.taskDefinition});
      }
      await waitApp(originalApp.taskDefinition, true);
      appSafe = true;
      log(`Restored app task and health: ${originalApp.taskDefinition}.`);
    });
  }
  if (simulatorStarted && appSafe) {
    await cleanup('restore simulator', async () => {
      const {app, simulator} = await services();
      requireCondition(app.taskDefinition === originalApp.taskDefinition, 'App task changed; retaining simulator');
      requireCondition(simulator.taskDefinition === originalSimulator.taskDefinition &&
        [0, 1].includes(simulator.desiredCount), 'Simulator changed externally; refusing to overwrite');
      await aws('ecs', 'update-service', {cluster, service: simulatorName, desiredCount: 0});
      await wait('Simulator shutdown', async () => stable((await services()).simulator, originalSimulator.taskDefinition, 0), true);
      log('Restored simulator desired/running count: 0.');
    });
  }
  if (appSafe) {
    if (ingressAttempted) {
      await cleanup('remove owned SG rule', async () => {
        const {SecurityGroupRules} = await aws('ec2', 'describe-security-group-rules', {Filters: [{Name: 'group-id', Values: [securityGroup]}]});
        const ids = SecurityGroupRules.filter(value => value.Description === marker && value.CidrIpv4 === cidr &&
          !value.IsEgress && value.IpProtocol === 'tcp' && value.FromPort === 443 && value.ToPort === 443)
          .map(value => value.SecurityGroupRuleId);
        if (ids.length) await aws('ec2', 'revoke-security-group-ingress', {GroupId: securityGroup, SecurityGroupRuleIds: ids});
        await wait('Owned SG rule removal', async () => {
          const result = await aws('ec2', 'describe-security-group-rules', {Filters: [{Name: 'group-id', Values: [securityGroup]}]});
          return !result.SecurityGroupRules.some(value => value.Description === marker && value.CidrIpv4 === cidr);
        }, true);
      });
    }
    for (const {input, wafRegion} of ownedSets) {
      await cleanup(`remove owned ${input.Scope} CIDR`, async () => {
        const current = await aws('wafv2', 'get-ip-set', input, wafRegion);
        if (current.IPSet.Addresses.includes(cidr)) {
          await aws('wafv2', 'update-ip-set', {...input, Addresses: current.IPSet.Addresses.filter(value => value !== cidr),
            LockToken: current.LockToken}, wafRegion);
        }
        await wait(`Owned ${input.Scope} CIDR removal`, async () => {
          const result = await aws('wafv2', 'get-ip-set', input, wafRegion);
          return !result.IPSet.Addresses.includes(cidr);
        }, true);
      });
    }
    if (temporaryArn) {
      await cleanup('retire temporary task', async () => {
        const {app} = await services();
        requireCondition(app.taskDefinition === originalApp.taskDefinition &&
          !app.deployments.some(value => value.taskDefinition === temporaryArn), 'Temporary task still referenced');
        await aws('ecs', 'deregister-task-definition', {taskDefinition: temporaryArn});
        log(`Retired temporary app task: ${temporaryArn}.`);
      });
    }
  } else {
    log(`App restoration is unverified; retaining simulator/network access. Recover original app ${originalApp.taskDefinition} before stopping simulator.`);
  }

  if (cleanupErrors.length) throw new AggregateError(cleanupErrors,
    `Readiness cleanup failed: ${cleanupErrors.map(error => error.message).join('; ')}`);
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const statePath = process.env.READINESS_STATE_PATH;
  try {
    const raw = await readFile(statePath, 'utf8');
    const state = JSON.parse(raw);
    await restoreEnvironment(state, {aws: awsCommand});
  } catch (error) {
    // error.message is already sanitized (see awsCommand/requireCondition/AggregateError above);
    // still, keep caught error details off console.error/warn/trace (CWE-209) and log the
    // summary there instead.
    console.log(`Restore details: ${error.message}`);
    console.error('NFT environment restoration failed.');
    process.exitCode = 1;
  }
}
