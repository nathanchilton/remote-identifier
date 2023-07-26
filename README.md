![image](https://github.com/nathanchilton/remote-identifier/assets/25993088/31a131dc-ee12-489f-9d9f-6bc0207b62a7)

# remote-identifier
An Android App to Transmit a Repeater's Call Sign From a Remote Radio

## Background
Amateur radio repeaters are required to identify themselves every 10 minutes when in use.  GMRS repeaters are also required to identify themselves when in use (every 15 minutes), unless that repeater is only being used exclusively by one family (using a single call sign) and all members of the family are identifying their transmissions every 15 minutes.

This identification is normally handled by a repeater controller, but low cost repeaters do not always have a means for adding a controller.  

## The purpose of this application
It occurred to me that it might be possible to create a simple solution to this problem.  Instead of connecting a real repeater controller, what if a separate device, in range of the repeater, could listen for traffic on the frequency and use text-to-speech to transmit a message over the air, on a specific interval, when the repeater is in use.

So, I created an Android application to perform this function.  The Android device needs to be connected to a radio which can be voice activated.  For instance, the Android phone could be connected to an HT with VOX enabled, using a BTECH APRS-K1 cable.

I designed the application to have an adjustable interval for transmitting.  For an amateur radio repeater, you can set it to identify every 10 minutes, and you can set it to identify every 15 minutes for a GMRS repeater.

By default, it will identify immediately after it hears a transmission, if it has been longer than the specified interval since the last time it identified itself.  Then, it will not identify itself again until 10/15 minutes has elapsed.  If you would like it to try to align its identifications to fractions of an hour (12:00, 12:10, 12:20, etc), you can toggle on the `Align to multiples of interval` setting.  If you find that the first word or two of the identification message gets clipped because it takes VOX a second to respond to audio, there is a `Trigger VOX with tone` setting, which will play a brief tone before transmitting your message.

![image](https://github.com/nathanchilton/remote-identifier/assets/25993088/99c36caa-2108-4227-bcee-c352aa208511)
