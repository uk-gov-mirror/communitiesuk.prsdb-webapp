# ADR-0033: Asynchronous Tasks

## Status

DRAFT

Date of decision: TBC

## Context and Problem Statement

We will have a number of asynchronous tasks that need to be run on demand, triggered directly or indirectly by a user's actions in the
WebApp. Examples of this include handling virus scan results, processing bulk uploads of property data, and potentially others in future
such as generating reports and sending bulk emails.

## Considered Options

* Separate long-running ECS task with webserver and private endpoints
* Separate ECS service with SQS queue and custom scaling rule
* SQS queue with the main webapp listening to it
* Switching to Lambda functions

## Decision Outcome

SQS queue with the main webapp listening to it, as it allows us to implement asynchronous tasks relatively quickly and efficiently, takes
into account the fact that the load on ad-hoc tasks should scale with the load on the webapp itself, and allows us a pathway to switch to a
separate ECS service in the future if needed.

## Pros and Cons of the Options

### Separate long-running ECS task with webserver and private endpoints

We could run a mirror of our existing ECS service, with a private loadbalancer that is only accessible from within our VPC and endpoints
that could be targeted by Eventbridge Scheduler.

* Good, because it avoids the long startup time of the container.
* Good, because it would allow very easy reuse and sharing of code and patterns between the WebApp and asynchronous tasks.
* Good, because we could use normal scaling rules for the ECS service based on the load of the containers.
* Good, because it would be easy to trigger asynchronous tasks using either Eventbridge Scheduler or by sending a request from the WebApp.
* Bad, because we would have to handle retry logic ourselves if a task failed partway through, and there could be some cases where
  tasks could be lost if the container was stopped or restarted while processing a task.
* Bad, because it would require a full copy of the WebApp to be running all the time even when there are no asynchronous tasks, which is
  wasteful in terms of resources.

### Separate ECS service with SQS queue and custom scaling rule

We could have a long-running ECS service that reads tasks from an SQS queue. Eventbridge Scheduler would then send messages to the SQS queue
to trigger tasks. We could set up a custom scaling rule for the ECS service based on the average number of messages in the SQS queue per
task, so that it would scale up when there are tasks to process and scale down to zero when there are no tasks.

* Good, because would allow relatively easy reuse of our existing code.
* Good, because during high volumes of tasks it would avoid the long start-up time of the webapp container.
* Good, because it would scale down to zero when there are no tasks, avoiding wasteful resource usage.
* Good, because it would allow us to implement retry logic using the built-in features of SQS, and would avoid losing tasks if a container
  was stopped or restarted while processing a task.
* Good, because it would be easy to trigger asynchronous tasks using either Eventbridge Scheduler or by adding a message to the queue from
  the WebApp.
* Good, because it would not place any additional load on the main WebApp containers.
* Bad, because it would require some changes to our existing code to poll the queue for messages instead of receiving them via an
  environment variable.
* Bad, because it would require creating a custom scaling rule for the ECS service, including a lambda function to calculate the metrics,
  which is more complex than using the built-in scaling rules.

### Switching to Lambda functions

We could split out each scheduled task into a separate Lambda function, which would be triggered by Eventbridge Schedulers.

* Good, because we wouldn't need to worry about scaling rules.
* Good, because we would only be using resources when a task is actually running.
* Good, because it would avoid the long start-up time of the full webapp container.
* Neutral, because while we wouldn't get retry logic for free, it would be relatively trivial to set up an SQS queue per lambda to allow for
  this.
* Neutral, because while we wouldn't get the ability to trigger asynchronous tasks for free, it would be relatively trivial to set up an SQS
  queue per lambda to allow for this.
* Bad, because we would need to significantly refactor our existing code to split out common functionality into a separate package that
  could be used by the Lambda functions.
* Bad, because we would need to ensure no tasks take longer than 15 minutes to run, which may not be possible for some tasks.

### SQS queue with the main webapp listening to it

We could have the main WebApp containers read tasks from an SQS queue. Eventbridge Scheduler would then send messages to the SQS queue to
trigger tasks.

* Good, because it would allow us to implement asynchronous tasks relatively quickly with minimal changes to our existing code.
* Good, because it would avoid the long start-up time of the webapp container.
* Good, because it would allow us to implement retry logic using the built-in features of SQS, and would avoid losing tasks if a container
  was stopped or restarted while processing a task.
* Good, because it would be easy to trigger asynchronous tasks using either Eventbridge Scheduler or by adding a message to the queue from
  the WebApp itself.
* Good, because it would avoid the need to create a custom scaling rule for a separate ECS service.
* Neutral, because while the load on ad-hoc tasks should scale with the load on the webapp itself, there may be times when this is not
  ideal.
* Bad, because it would place additional load on the main WebApp containers, which could impact performance during periods of high load.
* Bad, because it would require some changes to our existing code to poll the queue for messages instead of receiving them via an
  environment variable.

## More Information

Custom ECS scaling rules: https://aws.amazon.com/blogs/containers/amazon-elastic-container-service-ecs-auto-scaling-using-custom-metrics/
