# Smart Flashlight

As a demo project, we thought it would be cool to show how even the simplest app can become smart and when added contextual awareness features powered by Sensiya.

We wanted to create a falshlight that will turn itself on when (and only when) it is really needed. So we decided that the trigger should be a sudden drop of light (provided through the device's light sensor).

But how do we avoid false-positives?

#### Context Awareness
When you go to sleep, you turn off the lights, and definotely don't want the flashlight to turn on. We've used the Sensiya SDK to eable contextual awareness and query it to make sure you're not at home and about to sleep, before allowing the flashlight to turn on.


#### Action Recognition
Sensiya's [Action Recognition] Action Recognition indetofies your real-world phydical activity. The app checks the last known activity through the Sensiya SDK to make sure it is not driving and that the the flashlight doesn't turn on when you're entering a tunnel for example.

#### Java Docs
You can read more about the various fucntions used on Sensiya's [SDK documentation].

#### Download
You can download the Smart Flashlight from the [Play store]

[Action Recognition]:http://sensiya.com/sdk.html#ar
[play store]: https://play.google.com/store
[SDK documentation]: http://developers.sensiya.com/