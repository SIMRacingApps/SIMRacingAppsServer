Download Jetty from https://www.eclipse.org/jetty/download.html
Extract the .zip file into a temporary location.
Copy over only the "lib" folder from the total jetty zip download because we are not running a full application server. We are only embedding Jetty.
I decided not to only copy the .jar files we are using to make it easier to update because all of them are really small.
Instead, the jetty.userlibraries file contains only the .jar files needed to start the SIMRacingAppsServer process.
It can be imported to the project's build path as a user library.

The build.xml file would also need to be kept in sync with the jetty.userlibraries until I have time to use it directly.
