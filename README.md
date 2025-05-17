**Below is the step-by-step launching guide on a windows computer.**

**The program will be configured to run on a local db to ensure safe and problem-free connection.**

- Download and setup MySQL community version from **https://dev.mysql.com/downloads/installer/**.
- Create a server connection on localhost. (127.0.0.1:3306)
- In the server query, click "Open a SQL scripts file in a new query tab" (located near the top left)
- Locate to Palveo/db_scripts
- Open and run **00_initial_setup.sql**
- From the navigator, switch to schemas > right click > refresh all > double click palveo_db. Its name should appear in bold after completing
- Open and run **01_schema.sql**
- Close the MySQL Workbench
- Download the latest JavaFX version, 24.0.1 as of now, to **C:\JavaFX\javafx-sdk-24.0.1** from **https://gluonhq.com/products/javafx/**. (The archive has to be extracted to this location)
- Open Visual Studio Code, open Palveo as a project
- Open MainApp.java in editor to let the program be recognised as a java project. **Java Projects view** should be available at the bottom of the project explorer.
- Expand the Java Projects view, and add the all of the libraries in C:\JavaFX\javafx-sdk-24.0.1\lib to the **referenced libraries**.
- Preferrably from the Run & Debug view, launch the app using the "Palveo App" configuration. VS Code tends to auto-assign non-working launch configurations unless the app is started as instructed.

The program should run without any problems at this point.

**Because our TA left, we missed the part where we needed to open a github repository and add our TA.**
**Instead, we met face-to-face, working from a single computer most of the time - we shared the code either via flash drives or sent it through whatsapp to each other.**
**We could only setup the repo after being finished with the project.**

**We still did the commits, but have no tracable history of the work we have done.**
**Hence, we are leaving the task sharing below**

**GUI**
  - Mehmet Eren Sarıgül 
    -> /resources folder
    -> GUI design
  - Batuhan Yıldırım
    -> /gui folder
    -> GUI controllers

**Database**
  - Ömür Meriç Arıcı
    -> /dao folder
    -> /db folder
    -> /config folder
    -> /db_scripts folder

**Back-End**
  - Ahmet Alp Çamlıbel
    -> MainApp.Java
    -> /service folder
    -> Back-End / Front-End communication
  - Batuhan Küçük
    -> /util
    -> /model
    -> General testing & debugging
    -> Developing the interfaces from the UML diagrams
