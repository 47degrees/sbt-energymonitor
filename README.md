# sbt-energymonitor

Measure energy consumption around sbt commands.

The workflow this plugin supports is calling `energyMonitorPreSample` before you invoke some sbt commands, then calling `energyMonitorPostSample` afterward in order to obtain an `EnergyDiff` that you can track over time.

In order to use this plugin, you'll need to provide a jar containing `jRAPL` as a dependency in your `project/` folder -- you can see an example in the [scripted tests in this repo](./src/sbt-test/sbt-energymonitor/simple/project/lib). You can use the jar in this repo's `lib/` directory, which was copied from [Wojciech Mazur's fork](https://github.com/WojciechMazur/Energy-Languages/tree/6a75af59de2c7602c382c7f7271ddeaa563e29e0) of the `Energy-Languages` repo.

## Usage

This plugin requires sbt 1.0.0+. It provides four tasks: `energyMonitorPreSample`, `energyMonitorPostSample`, `energyMonitorPostSampleHttp`, and `energyMonitorPostSampleGitHub` and appropriate settings for configuring them.

### `energyMonitorPreSample`

This task takes an initial snapshot of energy usage and writes the results to the value configured in `energyMonitorOutputFile`.

If `energyMonitorDisableSampling` is true, this task will do nothing.

### `energyMonitorPostSample`

This task reads the initial sample from the path configured in `energyMonitorOutputFile`, takes a new snapshot,
and prints calculated energy usage to the console.

If `energyMonitorDisableSampling` is true, this task will do nothing.

### `energyMonitorPostSampleHttp`

This task works like `energyMonitorPostSample`, but instead of printing results to the console, sends
them to an HTTP server with some metadata. The server should accept POSTs to the configured url like:

```json
{
  "joules": 23,
  "seconds": 8,
  "organization": "47degrees",
  "repository": "sbt-energymonitor",
  "branch": "abcde",
  "run": 8,
  "recordedAt": "2022-05-05T10:15:00Z"
}
```

Its behavior is controlled by three settings: `energyMonitorPersistenceServerUrl`, and `energyMonitorPersistenceTag`.

However, if `energyMonitorDisableSampling` is `true`, this task will do nothing.

#### `energyMonitorPersistenceServerUrl`

This setting determines where the task should send the information about energy consumption. Its default value is `http://localhost:8080`, which will be correct if you're experimenting locally and using the demo server provided in the `energyMonitorPersistenceApp` module. You _should not use a local server for real testing though_, since the server's energy consumption will also show up in the power consumed by the CPU / memory during your energy tests.

#### `energyMonitorPersistenceTag`

This setting determines whether some arbitrary string will be included with the energy consumption sample. You might want to do this if there's some significant change that you think should explain differences in energy, for instance, "upgrade to cats-effect 3", or "refine types to shrink validation in core business logic," or something similar. You can put whatever information you want in the tag, or nothing at all. Its default value is `None`.

### `energyMonitorPostSampleGitHub`

This task works like `energyMonitorPostSample`, but instead of printing results to the console, sends them to a pull request comment.
It was written with the aim of posting energy consumption in every GitHub Actions CI run, but that's
[not going to work](https://github.com/47degrees/sbt-energymonitor/pull/6#issuecomment-1054567642) with the
current infrastructure backing GitHub Actions. You can still do this locally though if you're testing a PR and
want to add a comment documenting energy usage. To do so, you'll need to export four environment variables in
the shell session where you're running `sbt`:

- `GITHUB_REPOSITORY`: this value is the `org-name/repo-name` repository that you're working in, e.g. `47degrees/sbt-energymonitor`
- `GITHUB_TOKEN`: this value is a [GitHub personal access token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token)
- `GITHUB_RUN_ATTEMPT`: this value is an integer representing which CI run statistics correspond to. You can set this to any integer value,
  though tying it to the commit sequence in a pull request might make sense.
- `GITHUB_REF`: this is a reference like `refs/pull/1234/merge`, where `1234` is the PR number you want to add the comment to.

You can read more about any of these environment variables in the
[default environment variables](https://docs.github.com/en/actions/learn-github-actions/environment-variables#default-environment-variables)
GitHub Actions documentation.

You can see example comments in [this sandbox repo for learning `droste`](https://github.com/jisantuc/droste-playground/pull/5#issuecomment-1055702006).

If `energyMonitorDisableSampling` is true, this task will do nothing.

### `energyMonitorDisableSampling`

This setting controls whether any of the sampling tasks do anything. If it's set to `true`, none of them will do any work, and they'll
log a message that they're not doing any work because sampling is disabled.

### `energyMonitorOutputFile`

This setting contains the path that the `energyMonitorPreSample` task should write to and that the `energyMonitorPostSample`
and `energyMonitorPostSampleGitHub` tasks should read from. It defaults to `target/energy-sample`, so if you're ignoring
`target/` directories in `git`, it shouldn't annoy you. However, if you'd like to write the sample somewhere else, you
have that power.

### Testing

Run `test` for regular unit tests.

Run `scripted` for [sbt script tests](http://www.scala-sbt.org/1.x/docs/Testing-sbt-plugins.html).
