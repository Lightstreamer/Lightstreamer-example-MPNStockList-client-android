# Lightstreamer - MPN Stock-List Demo - Android Client

<!-- START DESCRIPTION lightstreamer-example-stocklist-client-android -->

This project contains a full example of an Android application that employs the [Lightstreamer Android Client library](http://www.lightstreamer.com/docs/client_android_api/index.html).

## Live Demo

![screenshot](screen_android_large.png)<br>

## Details

This is a Java-for-Android version of the [Lightstreamer - Basic Stock-List Demo - HTML client](https://github.com/Weswit/Lightstreamer-example-StockList-client-javascript#basic-stock-list-demo---html-client).<br>
This app uses the <b>Android Client API for Lightstreamer</b> to handle the communications with Lightstreamer Server. A simple user interface is implemented to display the real-time data received from Lightstreamer Server.<br>

Touch a row opens a new panel with the detailed info,  pdated in real time, of the stock.

The demo is ready to receive Mobile Push Notification through [Google Cloud Messaging for Android](https://developer.android.com/google/gcm/index.html).

Check out the sources for further explanations.
  
<i>NOTE: not all the functionalities of the Lightstreamer Android Java client are exposed by the classes listed above. You can easily expand those functionalities using the [Android Client API](http://www.lightstreamer.com/docs/client_android_api/index.html) as a reference. If in trouble check out the [specific Lightstreamer forum](http://forums.lightstreamer.com/forumdisplay.php?33-Android-Client-API).</i>

<!-- END DESCRIPTION lightstreamer-example-mpnstocklist-client-android -->

## Build

To build your own version of the demo please consider that this example is comprised of the following folders:
* `/src` Contains the sources to build the Java-for-Android application.
* `/res` Contains the images and other resourced needed to the demo. 
* `/lib` Drop here the `ls-android-client.jar` from the Lighstreamer SDK for Android Clients, to be used for the build process.
  
### Getting Started

To directly import the project as is you need the Eclipse IDE with the Android SDK and Android ADT plugin installed: Go to [http://www.eclipse.org/](http://www.eclipse.org/) and download the latest Eclipse distribution in its "classic" package.<br>
Go to http://developer.android.com/sdk/ and follow the instructions to install the Android SDK and ADT plugin in Eclipse.
You may want to read more detailed information regarding the required ADT Eclipse plugin, including choosing the best version for your IDE, at: [http://developer.android.com/sdk/eclipse-adt.html](http://developer.android.com/sdk/eclipse-adt.html).<br>
<i>NOTE: You may also use the sources of the demo with another IDE or without any IDE but such approach is not covered in this readme. In any case you need at least the Android SDK.</i>
<br>

You're now ready to import the project into eclipse, click on File->Import... then under General select Existing Projects into Workspace and choose the folder where the demo sources are.

### Compile and Run

From Eclipse, to compile and run the application right-click on the project in the Package Explorer and click Run As -> Android Application. If this is the first time an Android application is run, an extra window will appear asking to create a new Android Emulator Device "Android SDK and AVD Manager".
In this case, click on the "New..." button on the right and fill the form entering a name for your Android emulated device, a target platform of your choice and a SD card size. If you are unsure, select Android 1.1 as target platform and 128Mb as SD card size. Once done, click the "Create AVD" button and close the "Android SDK and AVD Manager window" clicking on the "X" title-bar button. At this point, your Android demo is compiled and executed inside the newly created Android emulator device.

### Deploy
  
You may run the demo against your local server or using our online server at http://push.lightstreamer.com:80. The server to which the demo will connect to is configured in the `res/values/strings.xml` file.
In the former case, the example requires that the [QUOTE_ADAPTER](https://github.com/Weswit/Lightstreamer-example-Stocklist-adapter-java) and [LiteralBasedProvider](https://github.com/Weswit/Lightstreamer-example-ReusableMetadata-adapter-java) have to be deployed in your local Lightstreamer server instance. 
The factory configuration of Lightstreamer server already provides this adapter deployed.<br>

## See Also

### Lightstreamer Adapters Needed by This Demo Client

<!-- START RELATED_ENTRIES -->

* [Lightstreamer - Stock-List Demo - Java Adapter](https://github.com/Weswit/Lightstreamer-example-Stocklist-adapter-java)
* [Lightstreamer - Reusable Metadata Adapters - Java Adapter](https://github.com/Weswit/Lightstreamer-example-ReusableMetadata-adapter-java)

<!-- END RELATED_ENTRIES -->

### Related Projects

* [Lightstreamer - Stock-List Demos - HTML Clients](https://github.com/Weswit/Lightstreamer-example-Stocklist-client-javascript)
* [Lightstreamer - Basic Stock-List Demo - jQuery (jqGrid) Client](https://github.com/Weswit/Lightstreamer-example-StockList-client-jquery)
* [Lightstreamer - Stock- List Demo Dojo Toolkit Client](https://github.com/Weswit/Lightstreamer-example-StockList-client-dojo)
* [Lightstreamer - Basic Stock-List Demo - .NET Client](https://github.com/Weswit/Lightstreamer-example-StockList-client-dotnet)
* [Lightstreamer - Round-Trip Demo - HTML Client](https://github.com/Weswit/Lightstreamer-example-RoundTrip-client-javascript)

## Lightstreamer Compatibility Notes

* Compatible with Lightstreamer Android Client API v. 1.1 or newer.
* For Lightstreamer Allegro (+ Android Client API support), Presto, Vivace.
