# Genesys Cloud Platform Connector Web Application

As on-premise platforms come in many different flavours as well as requiring different methods of access, the single public 'REST ONLY' DataActions methods from Genesys Cloud are not suitable.

This application acts as a standardised web interface connector between Genesys Cloud and on-premise platforms.

It utilises a plugin-based architecture, which allows different backend systems to be written as modules, and exposed via a java interface back to Genesys Cloud as a JSON based API.

The plugin forms part of the URL. The data being transmitted is 'irrelevant' to the container application, which passes it back to the plugin for consumption. As such, the JSON model is loosely defined with only a couple of required parameters.
The plugin author will dictate which data needs to be transmitted via the web requests for successful integration.
 
This project is licensed under the Affero General Public License v3 as detailed in the LICENSE file attached. Source code is available in the GitHub repository, or the Sources.jar file can be downloaded directly from the running application at http://application/source/code
This license requires that when a modified version is used to provide a service over a network, the complete source code of the modified version must be made available.

