# sbt-energymonitor

Measure energy consumption around sbt commands.

The workflow this plugin supports is calling `energyMonitorPreSample` before you invoke some sbt commands, then calling `energyMonitorPostSample` afterward in order to obtain an `EnergyDiff` that you can track over time.

In order to use this plugin, you'll need to provide a jar containing `jRAPL` as a dependency in your `project/` folder -- you can see an example in the [scripted tests in this repo](./src/sbt-test/sbt-energymonitor/simple/project/lib). You can use the jar in this repo's `lib/` directory, which was copied from [Wojciech Mazur's fork](https://github.com/WojciechMazur/Energy-Languages/tree/6a75af59de2c7602c382c7f7271ddeaa563e29e0) of the `Energy-Languages` repo.

## Usage

This plugin requires sbt 1.0.0+

### Testing

Run `test` for regular unit tests.

Run `scripted` for [sbt script tests](http://www.scala-sbt.org/1.x/docs/Testing-sbt-plugins.html).