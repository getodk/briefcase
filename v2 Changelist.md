# v2 Changelist

## User facing features

### No more "storage directory"

- The "storage directory" has been renamed to "workspace location".

  This is to avoid confusion between the path a user provides to launch Briefcase with the `ODK Briefcase Storage` subdirectory that Briefcase will use to store its files.

#### Workspace location is required to launch Briefcase

- Users must provide a workspace location when launching Briefcase CLI operations with the `-wl` arg.
- When launching the GUI, providing a workspace location is optional. If users omit the `-wl` arg, then they'll be asked to provide it in a GUI dialog.

  In this dialog, users will be able to select a location from their computer or choose between the last 5 different locations previously selected. 

#### The workspace location can't be changed during a GUI session

- Users will have to close Briefcase and launch it again to work under another workspace location

#### How the `ODK Briefcase Storage` subdirectory gets created

- The `ODK Briefcase Storage` subdirectory inside the workspace location is used by Briefcase to store all its files.

  This to have Briefcase work in isolation of other files users might have in their workspace locations.
  
- If there's already an `ODK Briefcase Storage` subdirectory inside the provided workspace location, then Briefcase will use it as is.

- If no `ODK Briefcase Storage` subdirectory is present inside the provided workspace location, or if the workspace location doesn't exist in the user's computer (when launching a CLI operation), then Briefcase will create all required directories.

### No more `metadata.json` files in the workspace location (previously known as "storage location")

- All form and submission metadata is stored in an HSQLDB database

### No more `cache.ser` cache file in the workspace location (previously known as "storage location")

- Since all form&submission metadata is stored in a database, no form cache is required anymore.

## Portable workspace location (form files, submission files, attachments, preferences, and metadata)

- Now the workspace location contains all the information Briefcase requires to work with forms and submissions.

  Users can share workspace locations with their colleagues by zipping and sending them, for example.
  
### No more system-wide Java preferences (kinda)

- Now Briefcase will store all preferences and form&submission metadata in an HSQLDB database, backed by files in the `db` subdirectory inside the workspace location.

  Briefcase will enable external connections to the database while it's runnning. You can use a JDBC compatible database client and the DSN `jdbc:hsqldb:hsql://localhost:9001/briefcase` to connect to the database.
  
- Briefcase will continue using the system-wide Java preference system to store just the last selected 5 workspace locations.

#### Users will be able to import Briefcase v1 preferences and metadata into v2's database

- When Briefcase starts to use a new workspace location, it will scan for legacy preferences and, when found, offer users to either import them, ignore them, or defer the decision to the next launch.

  This option will only be presented when launching Briefcase's GUI. When launching CLI operations, users will get an informative message.  

## Workspace location directory structure and file naming schemes.

### Form directories

- Form directories will include all the available information to uniquely identify the form, including the form's ID and the form's name, when it's available.

  Directory names must be sanitized to avoid problems with the filesystem's naming restrictions.
  
  Briefcase will always favor human-readable form directory names. When no human-readable alternatives can be used, hexadecimal representations will be used.
  
### Multiple form versions 

- Briefcase will store all the form versions that get pulled from any source.

  This means that, when pushing forms to a server that supports form versioning (ODK Central), Briefcase will be able to provide all known versions of a form.
  
- When a form has version information, it will be used to qualify its form file inside the workspace location.

  For example, lets consider the following metadata about a form:
  
    - Form ID: `some-form`
    - Form name: `Some form`
    - Versions: `2019110101`, and `2019110102`
    
  Then, inside the form's directory, users will find the following files:
  
    - `some-form(Some form)[2019110101].xml`
    - `some-form(Some form)[2019110102].xml`
    
- When pushing forms to a server that doesn't support form versioning (ODK Aggregate), Briefcase will push only the default form version, which is the last pulled version of a form.

### Submission directories
   
- In order to gracefully deal with hundreds of thousands of submissions, Briefcase will use a tree-like directory structure to store submission files and their attachments.
  
  The tree will follow the first 6 characters of the submission's instance ID.
  
  For example, given a submission with an instance ID of `uuid:a3aab3fb-8b72-4f18-ab93-a05f5613c971`, then it will be stored at `instances/a/3/a/a/b/3/submission.xml`
  
### Only submissions with instance ID will be stored

- Briefcase won't store any submission lacking of an instance ID when pulling forms. 

## ISO8601 adoption

- Briefcase won't transform date and time data found in source submissions into local formats when exporting forms. 

  This means that users should expect date and time data to be formatted in the ISO8601 format that clients such as ODK Collect produce. 
  
  This includes not only `date`, `time`, and `dateTime` xforms fields, but submission and completion date metadata as well. 

## Pending changes

### Import legacy `metadata.json` information into the database

- This is pending.

  Follow the same process as with the legacy preferences to be sure that we extract form metadata about the last used cursor and the last exported submission's submission date from the `metadata.json` file.

### Submission directory structure

- This is pending. 

  Given a submission with an instance ID of `uuid:a3aab3fb-8b72-4f18-ab93-a05f5613c971`, then it should be stored at `instances/a/3/a/a/b/3/submission.xml`

### Support for multiple form versions

- Context:
  - The change to the form directory naming scheme has been already done. See the `WorkspaceTest` class for reference

  - Since we use a compound primary key (form id, form version) in the database, Briefcase already should gracefully deal with multiple versions of the same form when storing them.

    This means that, when there's more than one versions of the same form, users should see as many instances of the form in the form tables in the Pull and Export panels.

  - We're already marking which version is the default one, because we update the field when updating the `FormMetadata` after completing a pull.
    
  - Since all submissions are stored in the same table, this shouldn't be a problem as well, since we could filter queries to get submissions by just the form ID, instead of using the full form ID + form version key.

- So, what's pending is:
  - We need to adapt the form & submission sync commands to deal with multiple versions of a form when scanning for forms in the provided path
  
  - We should check that we mark default versions when pulling from all sources. Maybe it's just from Aggregate. Must check this.
  
  - We need to present users just one entry in GUI tables, regardless of how many versions of the same form there are. 
  
    Beware of `FormKey` map keys in GUI form classes. 
    
    We might want to deal with an instance pointing to the default version in memory.
     
  - We need to adapt push processes to deal with multiple versions.
  
    When working with Aggregate, we must push only the default version
    
    When working with Central, we must push all versions  
    
### ISO8601 adoption

- This is pending.

  We need to pass through raw `date`, `time` and `dateTime` data from submissions into the exported files. 
    
  
