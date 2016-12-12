# ODK Briefcase
![Platform](https://img.shields.io/badge/platform-Java-blue.svg)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Slack status](http://slack.opendatakit.org/badge.svg)](http://slack.opendatakit.org)

ODK Briefcase is a desktop application that can locally store survey results gathered with [ODK Collect](https://opendatakit.org/use/collect). It can also be used to make local copies and CSV exports of data from [ODK Aggregate](https://opendatakit.org/use/aggregate/) (or compatible servers) and push data to those servers.   

ODK Briefcase is part of Open Data Kit (ODK), a free and open-source set of tools which help organizations author, field, and manage mobile data collection solutions. Learn more about the Open Data Kit project and its history [here](https://opendatakit.org/about/) and read about example ODK deployments [here](https://opendatakit.org/about/deployments/).

* ODK website: [https://opendatakit.org](https://opendatakit.org)
* ODK Briefcase usage instructions: [https://opendatakit.org/use/briefcase](https://opendatakit.org/use/briefcase)
* ODK community mailing list: [http://groups.google.com/group/opendatakit](http://groups.google.com/group/opendatakit)
* ODK developer mailing list: [http://groups.google.com/group/opendatakit-developers](http://groups.google.com/group/opendatakit-developers)
* ODK developer Slack chat: [http://slack.opendatakit.org](http://slack.opendatakit.org) 
* ODK developer Slack archive: [http://opendatakit.slackarchive.io](http://opendatakit.slackarchive.io) 
* ODK developer wiki: [https://github.com/opendatakit/opendatakit/wiki](https://github.com/opendatakit/opendatakit/wiki)

## Setting up your development environment

1. Fork the briefcase project ([why and how to fork](https://help.github.com/articles/fork-a-repo/))

1. Clone your fork of the project locally. At the command line:

        git clone https://github.com/YOUR-GITHUB-USERNAME/briefcase

We recommend using [IntelliJ IDEA](https://www.jetbrains.com/idea/) for development. On the welcome screen, click "Import Project" and navigate to your briefcase folder. Select `Create project from existing sources` and select defaults through the wizard. 

The main class is `org.opendatakit.briefcase.ui.MainBriefcaseWindow`. This repository also contains code for three smaller utilities with the following main classes:
- `org.opendatakit.briefcase.ui.CharsetConverterDialog` converts CSVs to UTF-8
- `org.opendatakit.briefcase.ui.MainClearBriefcasePreferencesWindow` clears Briefcase preferences
- `org.opendatakit.briefcase.ui.MainFormUploaderWindow` uploads blank forms to Aggregate instances
 
## Contributing code
Any and all contributions to the project are welcome. ODK Briefcase is used across the world primarily by organizations with a social purpose so you can have real impact!

## Downloading builds
The current production build can be found on the [ODK website](https://opendatakit.org/downloads/download-info/odk-briefcase/).