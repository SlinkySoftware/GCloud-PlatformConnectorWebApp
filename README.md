# Genesys Cloud Platform Connector Web Application

### Overview ### 

As on-premise platforms come in many different flavours as well as requiring different methods of access, the single public 'REST ONLY' DataActions methods from Genesys Cloud are not suitable.

This application acts as a standardised web interface connector between Genesys Cloud and on-premise platforms.

It utilises a plugin-based architecture, which allows different backend systems to be written as modules, and exposed via a java interface back to Genesys Cloud as a JSON based API.

The plugin forms part of the URL. The data being transmitted is 'irrelevant' to the container application, which passes it back to the plugin for consumption. As such, the JSON model is loosely defined with only a couple of required parameters.

The plugin author will dictate which data needs to be transmitted via the web requests for successful integration.

### Installation ###
* Compile the project using Apache Maven.
* Compile all plugins.
* Create a directory structure for the resultant application, and copy in JAR files
    - ```/opt/application```		-> This should contain the main container application JAR
	- ```/opt/application/lib```	-> This should contain any library dependencies for plugins, such as JDBC drivers
	- ```/opt/application/plugins```-> This should contain the plugin JAR files, as well as configuration properties (```pluginId.properties```)
* Run the application using a similar command to the content in ```example-run``` file
    - ```java -cp platformconnector-0.0.1.jar "-Dloader.path=lib" org.springframework.boot.loader.PropertiesLauncher```

### Notes ###
* Libraries in the ```lib/``` directory are only read on application boot. If they need to be added / modified for a particular plugin, then the whole instance needs to be restarted
* Libraries have a shared classpath. Meaning that only one class can exist for a particular namespace. You cannot mix-and-match versions, such as JDBC drivers, where the resulting class is the same


### License ###

This project is licensed under the Affero General Public License v3 as detailed in the LICENSE file attached. Source code is available in the GitHub repository, or the Sources.jar file can be downloaded directly from the running application at http://application/source/code

This license requires that when a modified version is used to provide a service over a network, the complete source code of the modified version must be made available.

