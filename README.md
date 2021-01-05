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

After the toolbox downloaded, navigate to your MongoDB folder, and copy all of the files from the toolbox into your MongoDB folder (the path for it should be the same as on screenshot unless you changed it during installation): 

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/MongoDB/5.JPG)


Now, let's start the mongodb. From the bin folder of MongoDB from the previous step, type mongo and press enter in the command prompt: 

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/MongoDB/1.jpg)

The marked address should be the same, mongodb://127.0.0.1:27017, take a note of it in order to take a look at the db from the Mongo Compass interface later. 

Press CTRL-C, you should see "bye" output:

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/MongoDB/2.jpg)

Go back to Eclipse and locate "InitialDB" in the project: 

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/MongoDB/3.jpg)

Right click on it and do "Show in -> System Explorer":

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/MongoDB/4.jpg)

The Explorer window will pop-up. Save the path. Go back to your mongodb folder where you copied all the files and run: mongorestore -d db_gpms C:\Users\dubro\git\GPMS-NGAC\InitialDB\db_gpms  , of course, change the path appropriately: 

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/MongoDB/6.jpg)

You should see the following out, and the most important part marked with red box: 

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/MongoDB/7.jpg)

Go back to Eclipse, and restart the server similarly (maybe not needed, but better to restart just in case): 

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/MongoDB/8.jpg)

Now you can login with the Username: "nazmul" and Password: "gpmspassword".

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/MongoDB/9.jpg)

If no error messages pop-up, you set it up correctly. Just to make sure, click on My Proposals button: 

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/MongoDB/10.jpg)

You should see the following screen. If you see the proposals' list, then access control is working and everything is great: 

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/MongoDB/11.JPG)

Let's log out and log in as admin, Username: "admin", Password: "gpmspassword". You should see the following screen. Click on "Manage Users":

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/MongoDB/14.jpg)

Here, you will find all the usernames. The password is the same for each user: "gpmspassword". 
Pay attention to whether a user is deleted and is active, it displays this information in the table: 

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/MongoDB/15.jpg)

You may also access the database through mongodb compass, use the exact same address and press connect: 

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/MongoDB/12.jpg)

After you press connect, you should the complete database. The passwords are encrypted, but it is the same one for each user: "gpmspassword". 

# USEFUL HINTS FOR FIXING THE PROJECT

1. You should now have any problem if you follow every step. However, software changes, updates, etc. Almost any problem can be solved by search online the error message. 

2. If you get 404 page, build the project (there is a screenshot above), and restart the server.

3. If there are compilation errors, update the project from Maven (screenshot above), then build the project (update cleans the project if "clean" is checks, the .class files should be regenerated), restart the server. 

4. Clean the server: 

![](https://github.com/dianxiangxu/GPMS-NGAC/blob/master/Documentation/InstallationScreenshots/MongoDB/16.jpg)

Restart the server after cleaning. 

5. Make sure you install Java prior to starting Eclipse. 

6. Contact Vlad at vadpb7@umsystem.edu or dubrovenski.v@gmail.com

NOTE: the guide is good for installing any tomcat 8/mongodb project, and, actually, a tomcat project with any database as long as you know how to start that db. 



