## feedback-cj

**Page feedback cronjob:**
The purpose of this tool is to clean personal info of both the exit survey entries and page feedback entries, apply tags to page feedback entries, and sync the page feedback entries with airtable and the feedback-viewer.

**Anytime changes are made to the domain you will need to:**
  1. Comment out the plugins in the pom.xml file for the Feedback Viewer repository
  2. Run MVN Install on Feedback Viewer repository
  3. Copy the "PageSuccess" jar from the target folder
  4. Paste the copied jar into the lib folder for the page feedback cronjob and replace.
  5. Run MVN Install on the page feedback cronjob repository.
  6. Uncomment the plugins from step #1.

