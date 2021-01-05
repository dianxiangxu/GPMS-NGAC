# GPMS
Proposal Workflow Management System - A web-based application for automating the approval process of grant submissions at an academic institution. The original version used the XACML standard for the access control policy. GPMC-NGAC uses the NGAC standard for access control, while keeping the same web-based user interface. 

# Installation of Project
## Prerequisites: 

Java 1.8

MongoDB Community Edition: https://docs.mongodb.com/manual/administration/install-community/

Tomcat 8/8.5 : https://tomcat.apache.org/download-80.cgi

Eclipse IDE for Enterprise Java Developers : https://www.eclipse.org/downloads/packages/

## Installation
The following instructions are for Windows 10. For other OS, the installation is the same. 

There are multiple ways of starting the project. We recommend installing and starting the Eclipse IDE first.


### Eclipse Project Import from Git

After the installation of Eclipse is complete, open eclipse, choose the workspace (default is fine), and do file -> import: 

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/ProjectImport/1.jpg)

After that, in this window select Git -> Project from Git (with smart import) :

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/ProjectImport/2.jpg)

Choose Clone URI and press next: 

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/ProjectImport/3.jpg)

Paste the following in URI section:  https://github.com/dianxiangxu/GPMS-NGAC.git

Also, put your Github login and password in Authentication and click next: 

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/ProjectImport/4.jpg)

Leave only the "master" branch selected and press next: 

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/ProjectImport/5.jpg)

Choose the directore or leave the default one and press next: 

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/ProjectImport/6.jpg)

Click finish after the project is loaded: 

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/ProjectImport/7.jpg)

Project structure should look like this: 

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/ProjectImport/8.jpg)

Right click on the project -> Maven -> Update Project: 

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/ProjectImport/10.jpg)

Choose the GPMS-NGAC and press ok:

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/ProjectImport/11.jpg)

The project is all ready (you still need to build it, I do it after the tomcat is installed)

### Tomcat Installation

Download the tomcat from link in Prerequisites section and unpack it.  

Click on Magnifying glass and search for "servers" -> click on it: 

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/TomcatInstallation/1.jpg)

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/TomcatInstallation/2.jpg)

Click on the link to create a new server: 

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/TomcatInstallation/3.jpg)

Choose tomcat v8.5 or v8 depending on which one you got -> click next: 

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/TomcatInstallation/4.jpg)

Click browse and navigate to your tomcat folder:

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/TomcatInstallation/5.jpg)

The folder has to contain "bin" folder, like in screenshot. Click "Select Folder":

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/TomcatInstallation/6.jpg)

Pick 1.8 jdk from the list: 

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/TomcatInstallation/7.jpg)

Click on GPMS-NGAC -> click on "Add": 

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/TomcatInstallation/8.jpg)

Click Finish after the project moved to "Configured" column:

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/TomcatInstallation/9.jpg)

Right click on the project -> "Build Project": 

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/TomcatInstallation/10.jpg)

Left-click on tomcat and press start/restart button :

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/TomcatInstallation/9.1.jpg)

Go ahead and visit the "localhost:8080/GPMS-NGAC/Login.jsp". Do no expect to login though as we still need to restore the database in the next section: 

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/TomcatInstallation/11.png)


### MongoDB import database

Follow the instructions to install MongoDB if have not already in the link above for your OS (INCLUDING CREATION OF C:\data FOR WINDOWS!). 
You will also need to download this toolbox for MongoDB: https://www.mongodb.com/try/download/database-tools
