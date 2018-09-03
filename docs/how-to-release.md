# How to release a new Briefcase version

**Table of Contents**

   * [How to release a new Briefcase version](#how-to-release-a-new-briefcase-version)
      * [About semantic versioning](#about-semantic-versioning)
      * [Releasing a minor version](#releasing-a-minor-version)
         * [Releasing a beta of a minor version](#releasing-a-beta-of-a-minor-version)
      * [Releasing a hot fix (patch version)](#releasing-a-hot-fix-patch-version)
      * [Post-release considerations](#post-release-considerations)


## About semantic versioning

- Briefcase uses semantic versioning: vx.y.z
  - x represents the *mayor* version number. Different mayor versions can be incompatible between them
  - y represents the *minor* version number. Different minor versions of the same mayor versions are compatible between them and bring new features
  - z represents the *patch* version number. Patch versions are use to release bug fixes only.

## Releasing a minor version

0. It's highly recommended to clone the git repo in a separate directory to keep the development workspace apart from the release workspace.

1. Go to the [GitHub repo releases page](https://github.com/opendatakit/briefcase/releases)  and draft a new release

2. Enter the new release version e.g. `v1.12.0`

    - Ensure that the selected `Target` branch is `master`
    - Choose a title following the template `ODK Briefcase vx.y.z`
    - Write a description following the template:
    
      ```markdown
      [All changes](https://github.com/opendatakit/briefcase/compare/v1...v2)
      
      **Highlights**
      - Some highlighted feature
      - Some highlighted technical improvement
      - Some highlighted bugfix
      - Some important notice about breaking changes
      
      **Added**
      - #123 Title of issue 123
        - Author of the solution of #123
      
      **Removed**
      - #123 Title of issue 123
        - Author of the solution of #123
      
      **Fixed**
      - #123 Title of issue 123
        - Author of the solution of #123
      ```
      
      Notes:
      - Replace the versions of the `All changes` link with the previous and current tags e.g. `v1.11.3...v.1.12.0`
      - In the `Added`, `Removed`, and `Fixed` sections, always link in this order: Issue > PR > Commit
      - The `Added` list would compile every new added feature
      - The `Removed` list would compile every removed feature
      - The `Fixed` list would compile bug fixes
      - An easy way to get a comprehensive list of items for these lists consists on searching on the `All changes` diff the terms `Merged` and ` and `. The second term will reveal merges by more than one author which indicate a "squash merge".
      - The list of items should be sorted in the same order as they've been closed
    
3. Once the release information has been completed, click on the `Publish release` button.

    GitHub will create a tag from the selected branch
  
4. Go to your releases workspace and sync your local repo with `git pull`

    You should see a message telling you about the new tag GitHub has created for your release

5. Check out the tag with `git checkout vx.y.z`. (replace x.y.z with the actual numbers you've used in step 2)

    Git should announce that you're on a `detached HEAD`. That's OK.
    
6. Verify that logging, error monitoring, and user tracking have production configurations

   **Logging configuration**
   
   Copy the file at `res/logback.xml.example-release` to `res/logback.xml`
   
   **Error monitoring, and user tracking configuration**
   
   Create the file `gradle.properties` at the root of the project and add these lines:
   
   ```groovy
   sentry.enabled=true
   googleAnalytics.trackingId={TRACKING_ID}  

   ```
   
   Use the Google Analytics tracking ID obtained in the [Google Analytics Dashboard](https://analytics.google.com):
   
   - Select the `ODK Briefcase` app
   - Go to `Admin > (Property section) Property Settings`
   
   Tracking IDs follow the pattern `UA-12345678-1`
     
  
7. Build the release JAR file with `./gradlew clean build -xtest`

    The JAR file will be located at `build/libs` and the filename should be like: `ODK Briefcase vx.y.z.jar`. (replace x.y.z with the actual numbers you've used in step 2)
  
    If you JAR's filename has `-dirty` or any other deviation from the previous template, you've made some mistake while sync'ing your repo after publishing the release on GitHub.
  
8. Go back to the release at GitHub and edit it. Open a file browser and drag your JAR file into the box. Click on `Update release`.

    GitHub will replace the whitespace characters on your JAR's filename with dots. Change them with dashes on the text field that appears when the upload finishes. E.g. `ODK Briefcase v1.12.0.jar` becomes `ODK.Briefcase.v1.12.0.jar` and you edit it into `ODK-Briefcase-v1.12.0.jar`.

### Releasing a beta of a minor version

- The process is basically the same as with a minor version with some small differences:
  - We suffix the version number with `-beta.N`, replacing `N` with the number of the beta, starting with `0`
  - We check the `This is a pre-release` checkbox to avoid going to it with the link https://github.com/opendatakit/briefcase/releases/latest
  
## Post-release considerations

- The new release should be linked in Sentry by sending the git refs involved (usually, the last from previous release and the last from the new release).

  The following curl carl with take care of this:
  
  ```shell
  curl https://sentry.io/api/0/organizations/opendatakit/releases/ \
   -X POST \
   -H 'Authorization: Bearer {SENTRY_AUTH_TOKEN}' \
   -H 'Content-Type: application/json' \
   -d '
   {
     "version": "v1.12.0",
     "refs": [{
         "repository":"opendatakit/briefcase",
         "commit":"{LAST_COMMIT_OF_THIS_RELEASE}",
         "previousCommit":"{LAST_COMMIT_OF_PREVIOUS_RELEASE}"
     }],
     "projects":["briefcase"]
   }
   '
  ```
  
  Replace:
  
  - Sentry auth token, obtained at https://sentry.io/settings/account/api/auth-tokens
  - Last commit of this release, obtained with `git log --pretty=oneline -1`
  - Last commit of the previous release, obtained by checking it out and executing `git log --pretty=oneline -1` 

- There should be a forum post announcing the new release. This post should be on the `Releases` category, or in the `Pre-releases` category if it's a beta pre-release.

  This is an example of a forum post announcing a minor release:
  
  ```markdown
  **Release highlights**
  * v1.12.0 - Sep 12, 2018
    - Improved performance of the export operation
    - Improvements to the UI, including:
      - Less cluttered layout and dialogs on the export tab
      - New checkbox to toggle exporting media files on the export tab
      - Reload source button on the pull tab
    - New CLI flags:
      - Enable splitting multiple choice fields when exporting forms
      - Enable pulling forms before export
      - Enable pulling submissions in parallel
    - New errors export output to make it easier to provide support
  
  **Download release**
  * [ODK-Briefcase-v1.12.0.jar](https://github.com/opendatakit/briefcase/releases/download/v1.12.0/ODK-Briefcase-v1.12.0.jar)
  
  **Report issues**
  * https://forum.opendatakit.org/c/support
  ```

  This is an example of a forum post announcing a beta release:

  ```markdown
  We need your help to ship great software, so please download this beta, try it, and report the issues you find. The release will be delayed until all reported issues with the beta are fixed.
  
  @Tonym, Kunal Mulwani, @jpknox, and others who filed issues: thank you for your contributions! Please confirm that the beta fixes those issues. If not, please report it below.
  
  For everyone else who uses Briefcase, the important changes to verify are:
  - Improved performance of the export operation
  - Improvements to the UI, including:
    - Less cluttered layout and dialogs on the export tab
    - New checkbox to toggle exporting media files on the export tab
    - Reload source button on the pull tab
  - New CLI flags:
    - Enable splitting multiple choice fields when exporting forms
    - Enable pulling forms before export
    - Enable pulling submissions in parallel
  - New errors export output to make it easier to provide support
  
  If you have a bit more time, please also verify the other changes in the [release notes](https://github.com/opendatakit/briefcase/releases/tag/v1.12.0-beta.0). Again, this beta will be released on Wednesday unless you report issues below.
  
  **Download beta**
  * [ODK-Briefcase-v1.12.0-beta.0.jar](https://github.com/opendatakit/briefcase/releases/download/v1.12.0-beta.0/ODK-Briefcase-v1.12.0-beta.0.jar)
  ```
