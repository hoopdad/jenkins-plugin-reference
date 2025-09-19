# Plugin Development Environment Setup

This was tested in Ubuntu 22 running on WSL on a Windows 11 workstation.

## Java

Setup Java. Use Java 17 for current Jenkins deployment. Other versions will not work when you go to deploy to a server such as CloudBees. You can use the instructions at [https://adoptium.net/installation/linux] which are summarized here.

```bash
sudo apt install -y wget apt-transport-https gpg
sudo wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor | sudo tee /etc/apt/trusted.gd/adoptium.gpg > /dev/null
sudo echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) mai" | sudo tee /etc/apt/sources.list.d/adoptium.list
sudo apt update
sudo apt install temurin-17-jdk
java -version
```

The final command will output your new java version. It will look something like this.

```bash
openjdk version "21.0.8" 2025-07-15 LTS
OpenJDK Runtime Environment Temurin-21.0.8+9 (build 21.0.8+9-LTS)
OpenJDK 64-Bit Server VM Temurin-21.0.8+9 (build 21.0.8+9-LTS, mixed mode, sharing)
```

## Maven Install

Install the latest version of maven. This might not be available in apt, and was not for this setup, so these are the steps.

Check for the latest version  for maximum maven enjoyment.

```bash
wget https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz
tar xzf apache-maven-3.9.9-bin.tar.gz
sudo mv apache-maven-3.9.9 /opt/maven
sudo ln -s /opt/maven/bin/mvn /usr/bin/mvn
rm apache-maven-3.9.9-bin.tar.gz 
 ```

## Maven Configuration

Edit file `~/.m2/settings.xml`. It might not be created for you automatically and that's ok. This maps in some Jenkins libraries that you'll need.

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <pluginGroups>
    <pluginGroup>org.jenkins-ci.tools</pluginGroup>
  </pluginGroups>
  <profiles>
    <profile>
      <id>jenkins</id>
      <activation>
        <activeByDefault>true</activeByDefault>
      </activation>
      <repositories>
        <repository>
          <id>repo.jenkins-ci.org</id>
          <url>https://repo.jenkins-ci.org/public/</url>
        </repository>
      </repositories>
      <pluginRepositories>
        <pluginRepository>
          <id>repo.jenkins-ci.org</id>
          <url>https://repo.jenkins-ci.org/public/</url>
        </pluginRepository>
      </pluginRepositories>
    </profile>
  </profiles>
</settings>
```

## Project Initiation

For purposes of this example, we built a hello-world plugin, which is a plugin that can be invoked during build. You may research the other templates which maven calls archetypes, and find a better one for your purposes. This is in the first question after you run the below.

```bash
mvn archetype:generate -Dfilter=io.jenkins.archetypes:plugin
```

This will generate the folder hierarchy undera single folder that you can move where you want.In this repo, it was moved to the `event-processing` subdirectory.

## Build and Run

This was run within WSL but a browser was used from the Windows host. This required using the `0.0.0.0` bind address as otherwise it was not accessible. If unable to reach the port, add a rule for your Windows Firewall to get to your desired port for private networks. It was found to need running as root, even though it's an unrestricted port.

```bash
cd event-processing
mvn clean hpi:run -Dport=8888 -Dhost=0.0.0.0
```

## Test Locally

Navigate to [http://localhost:8888/jenkins/]

1. Create a job
2. Give it a name.
3. Pick the "Freestyle Job" option and click the OK button.
4. The config screen will come up for the new job.
5. Scroll to the Build Steps section.
6. Create a new build step by clicking the "Add Build Step" drop down.
7. The "Say hello world" option will be in this list. Pick that.
8. The form asks for a name. Be creative.
9. Click Save at the bottom.
10. Click the Build Now blade on the left.
11. Click the job run on the bottom left of the left nav, #1.
12. Click Console output.
13. See the fruits of your creative naming where it says "hello, <name>!"

You will see output in your console from which you launched it. 

## Server Deployment

Once you have built the project, above, and presumably are satisfied with your rigorous testing, you can use the .hpi file in the target directory. You can upload this to your artifact repository or directly to a Jenkins server if that's appropriate.

## The Inferno of Seven Levels of Jenkins, Java and Maven Versions

You might need to adjust your Jenkins Plugin versions, based on error message from CloudBees or Jenkins.

Check your Java and Maven versions as above and make sure you followed all steps.

If you need to adjust your Jenkins library versions, try to change the "BOM" first since it will wrap up compatible versions. 

To pick the latest version:

1. go to  https://github.com/jenkinsci/bom
2. find the link for "latest bom"
3. click the link for your target Jenkins version, i.e. 2.504.x for a Jenkins platform on 2.504.3 which you put into jenkins.properties
4. Get the maven-metadata.xml file
5. Grab the version for the release tags
6. Paste that into the <version> tag below in the DependencyManagement section of your pom.xml

```xml
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>io.jenkins.tools.bom</groupId>
        <artifactId>bom-${jenkins.baseline}.x</artifactId>
        <version>5388.v3ea_2e00a_719a_</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
```

## References

The following were useful in figuring out the steps.

- [https://www.jenkins.io/doc/developer/tutorial/]
- [https://jenkinsci.github.io/maven-hpi-plugin/run-mojo.html]
- [https://jenkinsci.github.io/maven-hpi-plugin/]
- [https://github.com/jenkinsci/configuration-as-code-plugin/blob/master/README.md]
- [https://adoptium.net/installation/linux]
