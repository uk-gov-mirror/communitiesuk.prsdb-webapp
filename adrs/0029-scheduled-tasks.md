# ADR-0029: Scheduled Tasks

## Status

ACCEPTED

Date of decision: 2025-04-16

## Context and Problem Statement

We need multiple different jobs that will run periodically, e.g. cleaning up old partial property registrations, sending
out various different reminders to users etc.

In terms of infrastructure/ systems architecture, how should this be implemented?

## Considered Options

* Spring scheduled tasks in the main WebApp
* Spring scheduled tasks in a long-running copy of the WebApp container (without a webserver running)
* Spring scheduled tasks in a standalone long-running Spring application
* An ephemeral copy of the WebApp container (without a webserver running) triggered by Eventbridge Scheduler
* A separate ephemeral ECS task, triggered by Eventbridge Scheduler
* One or more Lambda functions, triggered by Eventbridge Scheduler

## Decision Outcome

An ephemeral copy of the WebApp container (without a webserver running) triggered by Eventbridge Scheduler, because it
allows us to implement scheduled tasks relatively quickly and relatively efficiently, and then iterate on that to
separate out shared code into a different package later if it becomes clear that it's necessary to do so.

## Pros and Cons of the Options

### Spring scheduled tasks in the main WebApp

Use Spring's built-in scheduled task functionality to run scheduled tasks in the main WebApp

* Good, because scheduling is simple to implement
* Good, because we can easily reuse existing code to access the database, send emails etc
* Good, because it would use existing infrastructure
* Bad, because either the scheduled tasks would compete for resources with normal requests to the Webapp, or it would
  require sending emails at strange times of day, e.g. the early hours of the morning when usage is lower

Note: A variation of this option would be to use the WebApp at a quiet time to do jobs such as database cleanup, and for
it to queue up jobs that need to be completed at a more sensible time (e.g. sending emails) to trigger one of the other
approaches listed below.

### Spring scheduled tasks in a long-running copy of the WebApp container (without a webserver running)

Use Spring's built-in scheduled task functionality to run scheduled tasks in a long-running copy of the WebApp

* Good, because scheduling is simple to implement
* Good, because we can easily reuse existing code to access the database, send emails etc
* Bad, because it's wasteful in terms of resources to run an entire container constantly that's only used infrequently
  for periodic tasks.

### Spring scheduled tasks in a standalone long-running Spring application

Use Spring's built-in scheduled task functionality to run scheduled tasks in a long-running standalone application

* Good, because scheduling is simple to implement.
* Bad, because we would need to either duplicate common code from our main WebApp or split it out into a separate
  package
* Bad, because it's wasteful in terms of resources to run an entire container constantly that's only used infrequently
  for periodic tasks.

### An ephemeral copy of the WebApp container (without a webserver running) triggered by Eventbridge Scheduler

Use Eventbridge Scheduler to trigger a copy of the WebApp to spin up and perform the scheduled job, before shutting
itself down when the job has been completed

* Neutral, because scheduling is more complex to setup but there are examples that can be followed
* Good, because we can easily reuse existing code to access the database, send emails etc
* Neutral, because while the container would only exist while it is needed, we would be deploying a lot more code than
  is actually used in each task, so it would still be a bit wasteful in terms of resources
* Neutral, because we would want to bundle up periodic tasks to avoid repeatedly spinning up and destroying relatively
  large containers or spinning up lots of them in parallel

### A separate ephemeral ECS task, triggered by Eventbridge Scheduler

Use Eventbridge Scheduler to trigger a standalone ECS task that performs the scheduled job, before shutting itself down
when the job has been completed

* Neutral, because scheduling is more complex to setup but there are examples that can be followed
* Bad, because we would need to either duplicate common code from our main WebApp or split it out into a separate
  package
* Good, because we would only be deploying the code required to complete the task
* Neutral, because we would want to bundle up periodic tasks to avoid repeatedly spinning up and destroying relatively
  large containers or spinning up lots of them in parallel

### One or more Lambda functions, triggered by Eventbridge Scheduler

Use Eventbridge Scheduler to trigger specific lambda functions for each of the scheduled jobs.

* Neutral, because scheduling is more complex to setup but there are examples that can be followed
* Bad, because we would need to either duplicate common code from our main WebApp or split it out into (multiple)
  separate
  packages
* Good, because we would only be deploying the code required to complete each task
* Good, because we wouldn't need to worry (from a resource perspective) about jobs running in parallel
* Bad, because some of the jobs might not be suitable for lambda functions, e.g. updating NGD
