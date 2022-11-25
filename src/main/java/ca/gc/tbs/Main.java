package ca.gc.tbs;

import ca.gc.tbs.domain.Problem;
import ca.gc.tbs.domain.TopTaskSurvey;
import ca.gc.tbs.repository.ProblemRepository;
import ca.gc.tbs.repository.TopTaskRepository;
import ca.gc.tbs.service.ContentService;
import com.sybit.airtable.Airtable;
import com.sybit.airtable.Base;
import com.sybit.airtable.Table;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.datatables.DataTablesRepositoryFactoryBean;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.lang.System.exit;

@SpringBootApplication
@ComponentScan(basePackages = {"ca.gc.tbs.domain", "ca.gc.tbs.repository"})
@EnableMongoRepositories(repositoryFactoryBeanClass = DataTablesRepositoryFactoryBean.class)
public class Main implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    // Tier 2 entries do not populate to AirTable.
    private final Set<String> tier2Spreadsheet = new HashSet<>();
    private final HashMap<String, String[]> tier1Spreadsheet = new HashMap<>();

    private final HashMap<String, String> mainPageTitleIds = new HashMap<>();
    private final HashMap<String, String> mainUrlLinkIds = new HashMap<>();
    private final HashMap<String, String> mainMlTagIds = new HashMap<>();

    private final HashMap<String, String> healthPageTitleIds = new HashMap<>();
    private final HashMap<String, String> healthUrlLinkIds = new HashMap<>();
    private final HashMap<String, String> healthMlTagIds = new HashMap<>();

    private final HashMap<String, String> CRA_PageTitleIds = new HashMap<>();
    private final HashMap<String, String> CRA_UrlLinkIds = new HashMap<>();
    private final HashMap<String, String> CRA_MlTagIds = new HashMap<>();

    private final HashMap<String, String> travelPageTitleIds = new HashMap<>();
    private final HashMap<String, String> travelUrlLinkIds = new HashMap<>();
    private final HashMap<String, String> travelMlTagIds = new HashMap<>();

    private final HashMap<String, String> IRCC_PageTitleIds = new HashMap<>();
    private final HashMap<String, String> IRCC_UrlLinkIds = new HashMap<>();
    private final HashMap<String, String> IRCC_MlTagIds = new HashMap<>();
    private final ContentService contentService = new ContentService();
    @Autowired
    private ProblemRepository problemRepository;
    @Autowired
    private TopTaskRepository topTaskRepository;
    // Main AirTable
    @Value("${airtable.key}")
    private String airtableKey;
    @Value("${airtable.tab}")
    private String problemAirtableTab;
    @Value("${airtable.pageTitleLookup}")
    private String airtablePageTitleLookup;
    @Value("${airtable.mlTags}")
    private String airtableMLTags;
    @Value("${airtable.URL_link}")
    private String airtableURLLink;
    @Value("${airtable.base}")
    private String problemAirtableBase;

    // Health AirTable
    @Value("${health.airtable.base}")
    private String healthAirtableBase;

    // CRA AirTable
    @Value("${cra.airtable.base}")
    private String CRA_AirtableBase;

    // Travel AirTable
    @Value("${travel.airtable.base}")
    private String travelAirtableBase;

    // IRCC AirTable
    @Value("${ircc.airtable.base}")
    private String irccAirtableBase;

    private Base mainBase;
    private Base healthBase;
    private Base CRA_Base;
    private Base travelBase;
    private Base IRCC_Base;

    public static void main(String[] args) {
        new SpringApplicationBuilder(Main.class).web(WebApplicationType.NONE) // .REACTIVE, .SERVLET
                .run(args);
    }

    // Main Loop, Runs all functions needed.
    @Override
    public void run(String... args) throws Exception {

        Airtable airTableKey = new Airtable().configure(this.airtableKey);

        System.out.println("---------------------CONNECTING TO AIRTABLE BASES---------------------");
        this.mainBase = airTableKey.base(this.problemAirtableBase);
        this.healthBase = airTableKey.base(this.healthAirtableBase);
        this.CRA_Base = airTableKey.base(this.CRA_AirtableBase);
        this.travelBase = airTableKey.base(this.travelAirtableBase);
        this.IRCC_Base = airTableKey.base(this.irccAirtableBase);

        System.out.println("---------------------REMOVING PERSONAL INFO FROM TTS---------------------");
        this.removePersonalInfoExitSurvey();

        System.out.println("---------------------REMOVING PERSONAL INFO FROM COMMENTS---------------------");
        this.removePersonalInfoProblems();

        System.out.println("---------------------REMOVING JUNK DATA FROM TTS---------------------");
        this.removeJunkDataTTS();

        System.out.println("---------------------IMPORTING SPREADSHEETS---------------------");
        this.importTier1();
        this.importTier2();

        System.out.println("---------------------RETRIEVING AIRTABLE VALUES---------------------");
        this.getPageTitleIds(mainBase);
        this.getPageTitleIds(healthBase);
        this.getPageTitleIds(CRA_Base);
        this.getPageTitleIds(travelBase);
        this.getPageTitleIds(IRCC_Base);

        this.getMLTagIds(mainBase);
        this.getMLTagIds(healthBase);
        this.getMLTagIds(CRA_Base);
        this.getMLTagIds(travelBase);
        this.getMLTagIds(IRCC_Base);

        this.getURLLinkIds(mainBase);
        this.getURLLinkIds(healthBase);
        this.getURLLinkIds(CRA_Base);
        this.getURLLinkIds(travelBase);
        this.getURLLinkIds(IRCC_Base);

        System.out.println("---------------------AUTO TAGGING---------------------");
        this.autoTag();

        System.out.println("---------------------AIRTABLE & SPREADSHEET SYNC---------------------");
        this.airTableSpreadsheetSync();

        System.out.println("---------------------MARK AS PROCESSED ---------------------");
        this.completeProcessing();
    }

    // Scrubs tasks (Exit Survey) that have not been cleaned using the cleaning script
    public void removePersonalInfoExitSurvey() {
        List<TopTaskSurvey> tList = this.topTaskRepository.findByPersonalInfoProcessed(null);
        tList.addAll(this.topTaskRepository.findByPersonalInfoProcessed("false"));
        for (TopTaskSurvey task : tList) {
            try {
                if (task.getThemeOther() != null) {
                    String details = this.contentService.cleanContent(task.getThemeOther());
                    task.setThemeOther(details);
                }
                if (task.getTaskOther() != null) {
                    String details = this.contentService.cleanContent(task.getTaskOther());
                    task.setTaskOther(details);
                }
                if (task.getTaskImproveComment() != null) {
                    String details = this.contentService.cleanContent(task.getTaskImproveComment());
                    task.setTaskImproveComment(details);
                }
                if (task.getTaskWhyNotComment() != null) {
                    String details = this.contentService.cleanContent(task.getTaskWhyNotComment());
                    task.setTaskWhyNotComment(details);
                }
                task.setPersonalInfoProcessed("true");
                this.topTaskRepository.save(task);
            } catch (Exception e) {
                System.out.println("Could not process task: " + task.getId() + " : " + task.getDateTime() + " : " + task.getTaskOther() + " : "
                        + task.getTaskImproveComment() + " : " + task.getTaskWhyNotComment());
            }
        }
        System.out.println("Private info removed...");
    }

    // Scrubs comments that have not been cleaned using the cleaning script
    public void removePersonalInfoProblems() {
        List<Problem> pList = this.problemRepository.findByPersonalInfoProcessed(null);
        pList.addAll(this.problemRepository.findByPersonalInfoProcessed("false"));
        for (Problem problem : pList) {
            try {
                String details = this.contentService.cleanContent(problem.getProblemDetails());
                problem.setProblemDetails(details);
                problem.setPersonalInfoProcessed("true");
                this.problemRepository.save(problem);
            } catch (Exception e) {
                System.out.println("Could not process problem:" + problem.getId() + ":" + problem.getProblemDetails());
            }
        }
        System.out.println("Private info removed...");
    }

    // Removes white space values from comments to improve the filter for write in comments on the Feedback-Viewer.
    public void removeJunkDataTTS() {
        List<TopTaskSurvey> tList = this.topTaskRepository.findByProcessed("false");
        System.out.println("Amount of non processed entries (TTS) : " + tList.size());
        for (TopTaskSurvey task : tList) {
            if (task == null || containsHTML(task.getTaskOther(), task.getThemeOther(), task.getTaskImproveComment(), task.getTaskWhyNotComment())) {
                assert task != null;
                System.out.println("Deleting task: " + task.getId() + " , Task was null or had a hyperlink");
                this.topTaskRepository.delete(task);
                continue;
            }
            if (task.getTaskOther() != null && task.getTaskOther().trim().equals("") && task.getTaskOther().length() != 0) {
                System.out.println("found junk data in taskOther.");
                task.setTaskOther("");
            }
            if (task.getThemeOther() != null && task.getThemeOther().trim().equals("") && task.getThemeOther().length() != 0) {
                System.out.println("found junk data in themeOther.");
                task.setThemeOther("");
            }
            if (task.getTaskImproveComment() != null && task.getTaskImproveComment().trim().equals("") && task.getTaskImproveComment().length() != 0) {
                System.out.println("found junk data in taskImproveComment.");
                task.setTaskImproveComment("");
            }
            if (task.getTaskWhyNotComment() != null && task.getTaskWhyNotComment().trim().equals("") && task.getTaskWhyNotComment().length() != 0) {
                System.out.println("found junk data in taskWhyNotComment.");
                task.setTaskWhyNotComment("");
            }
            task.setProcessed("true");
            this.topTaskRepository.save(task);
        }
    }

    // Retrieves ALL model & bases and imports them to the TIER 1 map.
    public void importTier1() throws Exception {
        final Reader reader = new InputStreamReader(
                new URL("https://docs.google.com/spreadsheets/d/1eOmX_b8XCR9eLNxUbX3Gwkp2ywJ-vhapnC7ApdRbnSg/export?format=csv").openConnection()
                        .getInputStream(),
                StandardCharsets.UTF_8);
        final CSVFormat csvFormat = CSVFormat.Builder.create().setHeader().setAllowMissingColumnNames(true).build();
        final Iterable<CSVRecord> records = csvFormat.parse(reader);
        try {
            for (final CSVRecord record : records) {
                try {
                    tier2Spreadsheet.add(record.get("URL").toLowerCase());
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
            }
        } finally {
            reader.close();
        }
    }

    // Retrieves ALL URLs and imports them to the TIER 2 map.
    public void importTier2() throws Exception {
        final Reader reader = new InputStreamReader(
                new URL("https://docs.google.com/spreadsheets/d/1B16qEbfp7SFCfIsZ8fcj7DneCy1WkR0GPh4t9L9NRSg/export?format=csv").openConnection()
                        .getInputStream(),
                StandardCharsets.UTF_8);
        final CSVFormat csvFormat = CSVFormat.Builder.create().setHeader().setAllowMissingColumnNames(true).build();
        final Iterable<CSVRecord> records = csvFormat.parse(reader);
        try {
            for (final CSVRecord record : records) {
                try {
                    tier2Spreadsheet.add(record.get("URL").toLowerCase());
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                }
            }
        } finally {
            reader.close();
        }
    }

    // Retrieves Page feedback statistics page IDs and adds them to a hashmap for their respective AirTable base.
    private void getPageTitleIds(Base base) throws Exception {
        @SuppressWarnings("unchecked")
        Table<AirTableStat> statsTable = base.table(this.airtablePageTitleLookup, AirTableStat.class);
        List<AirTableStat> stats = statsTable.select();
        HashMap<String, String> m = selectMapPageTitleIds(base);
        stats.forEach(entry -> {
            if (entry.getPageTitle() != null) {
                try {
                    m.put(entry.getPageTitle().trim().toUpperCase(), entry.getId());
                } catch (Exception e) {
                    System.out.println(e.getMessage() + " Could not add Page Title ID: " + entry.getPageTitle() + " TO page title ID map.");
                }
            }
        });
    }

    // Retrieves Page groups by URL and adds them to a hashmap for their respective AirTable base.
    private void getURLLinkIds(Base base) throws Exception {
        @SuppressWarnings("unchecked")
        Table<AirTableURLLink> urlLinkTable = base.table(this.airtableURLLink, AirTableURLLink.class);
        List<AirTableURLLink> urlLinks = urlLinkTable.select();
        HashMap<String, String> m = selectMapUrlLinkIds(base);
        urlLinks.forEach(entry -> {
            if (entry.getURLlink() != null) {
                try {
                    m.put(entry.getURLlink().trim().toUpperCase(), entry.getId());
                } catch (Exception e) {
                    System.out.println(e.getMessage() + " Could not add URL Link ID: " + entry.getURLlink() + " TO url link ID map.");
                }
            }
        });
    }

    // Retrieves ML Tags and adds them to a hashmap for their respective AirTable base.
    private void getMLTagIds(Base base) throws Exception {
        @SuppressWarnings("unchecked")
        Table<AirTableMLTag> tagsTable = base.table(airtableMLTags, AirTableMLTag.class);
        List<AirTableMLTag> tags = tagsTable.select();
        HashMap<String, String> m = selectMapMLTagIds(base);
        tags.forEach(entry -> {
            if (entry.getTag() != null) {
                try {
                    m.put(entry.getTag().trim().toUpperCase(), entry.getId());
                } catch (Exception e) {
                    System.out.println(e.getMessage() + " Could not add ML Tag ID: " + entry.getTag() + " TO ML tag ID map.");
                }
            }
        });
    }

    // Assigns tags to non-processed problems.
    public void autoTag() {
        List<Problem> pList = this.problemRepository.findByAutoTagProcessed("false");
        pList.addAll(this.problemRepository.findByAutoTagProcessed(null));
        System.out.println("Amount of entries to be tagged: " + pList.size());
        for (Problem problem : pList) {
            String model = "";
            try {
                // If problem has comment, assign language & model.
                if (!problem.getProblemDetails().trim().equals("")) {
                    String lang = "en";
                    if (problem.getLanguage().equalsIgnoreCase("fr")) {
                        lang = "fr";
                    }
                    String text = URLEncoder.encode(problem.getProblemDetails(), StandardCharsets.UTF_8.name());
                    String URL = removeQueryAfterHTML(problem.getUrl()).toLowerCase();

                    if (tier1Spreadsheet.containsKey(URL)) {
                        model = tier1Spreadsheet.get(URL)[0];
                        System.out.println("model: " + model);
                    }
                    // Then feed through the suggestion script (Feedback-Classification-RetroAction
                    // Repository) if model exists
                    // and assign tags if applicable.
                    if (!model.equals("")) {
                        Document doc = Jsoup
                                .connect(
                                        "https://suggestion.tbs.alpha.canada.ca/suggestCategory?lang=" + lang + "&text=" + text + "&section=" + model)
                                .maxBodySize(0).get();
                        String tags = doc.select("body").html();
                        System.out.println("Text:" + text + " : " + tags);
                        String[] splitTags = tags.split(",");
                        problem.getTags().addAll(Arrays.asList(splitTags));
                    }
                }
            } catch (Exception e) {
                System.out.println("Could not auto tag because:" + e.getMessage() + " model:" + model);
            }
            problem.setAutoTagProcessed("true");
            this.problemRepository.save(problem);
        }

    }

    // Populates entries to the AirTable bases and Tier 2 spreadsheet (inventory).
    public void airTableSpreadsheetSync() {
        // Connect to AirTable bases
        @SuppressWarnings("unchecked")
        Table<AirTableProblemEnhanced> problemTable = mainBase.table(this.problemAirtableTab, AirTableProblemEnhanced.class);
        @SuppressWarnings("unchecked")
        Table<AirTableProblemEnhanced> healthTable = healthBase.table(this.problemAirtableTab, AirTableProblemEnhanced.class);
        @SuppressWarnings("unchecked")
        Table<AirTableProblemEnhanced> craTable = CRA_Base.table(this.problemAirtableTab, AirTableProblemEnhanced.class);
        @SuppressWarnings("unchecked")
        Table<AirTableProblemEnhanced> travelTable = travelBase.table(this.problemAirtableTab, AirTableProblemEnhanced.class);
        @SuppressWarnings("unchecked")
        Table<AirTableProblemEnhanced> irccTable = IRCC_Base.table(this.problemAirtableTab, AirTableProblemEnhanced.class);
        // Find problems that have not been run through this function
        List<Problem> pList = this.problemRepository.findByAirTableSync(null);
        pList.addAll(this.problemRepository.findByAirTableSync("false"));
        System.out.println("Connected to MongoDB & Airtable");
        System.out.println("Found " + pList.size() + " records that need to be added.");
        int i = 1;
        int maxToSync = 150;
        for (Problem problem : pList) {
            try {

                boolean problemIsProcessed = problem.getPersonalInfoProcessed().equals("true") && problem.getAutoTagProcessed().equals("true");
                boolean emptyComment = problem.getProblemDetails().trim().equals("");

                if (emptyComment) {
                    System.out.println("Empty comment, deleting entry...");
                    problemRepository.delete(problem);
                    continue;
                }
                if (containsHTML(problem.getProblemDetails()) || problem.getUrl().equals("https://www.canada.ca/")) {
                    this.problemRepository.delete(problem);
                    continue;
                }
                String UTM_value = returnQueryAfterHTML(problem.getUrl());
                problem.setUrl(removeQueryAfterHTML(problem.getUrl().toLowerCase()));
                // if tier 1 and tier 2 spreadsheet don't contain URL, add it to Tier 2 and set sync to true
                if (!tier1Spreadsheet.containsKey(problem.getUrl()) && !tier2Spreadsheet.contains(problem.getUrl())) {
                    System.out.println("Processed record : " + i + ": url not in spreadsheet " + problem.getUrl() + ", Adding url to Tier 2 Spreadsheet.");
                    tier2Spreadsheet.add(problem.getUrl());
                    GoogleSheetsAPI.appendURL(problem.getUrl());
                    problem.setAirTableSync("true");
                }
                // if tier 2 spreadsheet contains URL, do nothing and set AirTable sync to true
                // TIER 2 entries end here.
                else if (tier2Spreadsheet.contains(problem.getUrl())) {
                    System.out.println(i + ": Tier 2 spreadsheet contains url already: " + problem.getUrl());
                    problem.setAirTableSync("true");
                } else {
                    AirTableProblemEnhanced airProblem = new AirTableProblemEnhanced();
                    airProblem.setUTM(UTM_value);
                    // LAST condition: check if conditions met to go to main AirTable and populate.
                    if (problemIsProcessed && tier1Spreadsheet.get(problem.getUrl())[1].equals("main")) {

                        if (!this.mainUrlLinkIds.containsKey(problem.getUrl().trim().toUpperCase())) {
                            this.createUrlLinkEntry(problem.getUrl(), mainBase, airtableURLLink);
                        }
                        airProblem.getURLLinkIds().add(this.mainUrlLinkIds.get(problem.getUrl().trim().toUpperCase()));
                        if (!this.mainPageTitleIds.containsKey(problem.getTitle().trim().toUpperCase())) {
                            this.createPageTitleEntry(problem.getTitle(), mainBase, airtablePageTitleLookup);
                        }
                        airProblem.getPageTitleIds().add(this.mainPageTitleIds.get(problem.getTitle().trim().toUpperCase()));

                        for (String tag : problem.getTags()) {
                            if (this.mainMlTagIds.containsKey(tag.trim().toUpperCase())) {
                                airProblem.getTags().add(this.mainMlTagIds.get(tag.trim().toUpperCase()));
                            } else {
                                System.out.println("Missing tag id for:" + tag);
                            }
                        }
                        setAirProblemAttributes(airProblem, problem);
                        problemTable.create(airProblem);
                        problem.setAirTableSync("true");
                        System.out.println("Processed record : " + i + " For Main, Date: " + airProblem.getDate());
                    }
                    // Check if conditions met to go to health AirTable and populate.
                    if (problemIsProcessed && tier1Spreadsheet.get(problem.getUrl())[1].equals("health")) {
                        if (!this.healthUrlLinkIds.containsKey(problem.getUrl().trim().toUpperCase())) {
                            this.createUrlLinkEntry(problem.getUrl(), healthBase, airtableURLLink);
                        }
                        airProblem.getURLLinkIds().add(this.healthUrlLinkIds.get(problem.getUrl().trim().toUpperCase()));
                        if (!this.healthPageTitleIds.containsKey(problem.getTitle().trim().toUpperCase())) {
                            this.createPageTitleEntry(problem.getTitle(), healthBase, airtablePageTitleLookup);
                        }
                        airProblem.getPageTitleIds().add(this.healthPageTitleIds.get(problem.getTitle().trim().toUpperCase()));

                        for (String tag : problem.getTags()) {
                            if (this.healthMlTagIds.containsKey(tag.trim().toUpperCase())) {
                                airProblem.getTags().add(this.healthMlTagIds.get(tag.trim().toUpperCase()));
                            } else {
                                System.out.println("Missing tag id for:" + tag);
                            }
                        }
                        setAirProblemAttributes(airProblem, problem);
                        healthTable.create(airProblem);
                        problem.setAirTableSync("true");
                        System.out.println("Processed record : " + i + " For Health, Date: " + airProblem.getDate());
                    }
                    // Check if conditions met to go to CRA AirTable and populate.
                    if (problemIsProcessed && tier1Spreadsheet.get(problem.getUrl())[1].equals("cra")) {

                        if (!this.CRA_UrlLinkIds.containsKey(problem.getUrl().trim().toUpperCase())) {
                            this.createUrlLinkEntry(problem.getUrl(), CRA_Base, airtableURLLink);
                        }

                        airProblem.getURLLinkIds().add(this.CRA_UrlLinkIds.get(problem.getUrl().trim().toUpperCase()));

                        if (!this.CRA_PageTitleIds.containsKey(problem.getTitle().trim().toUpperCase())) {
                            this.createPageTitleEntry(problem.getTitle(), CRA_Base, airtablePageTitleLookup);
                        }
                        airProblem.getPageTitleIds().add(this.CRA_PageTitleIds.get(problem.getTitle().trim().toUpperCase()));

                        for (String tag : problem.getTags()) {
                            if (this.CRA_MlTagIds.containsKey(tag.trim().toUpperCase())) {
                                airProblem.getTags().add(this.CRA_MlTagIds.get(tag.trim().toUpperCase()));
                            } else {
                                System.out.println("Missing tag id for:" + tag);
                            }
                        }
                        setAirProblemAttributes(airProblem, problem);
                        craTable.create(airProblem);
                        problem.setAirTableSync("true");
                        System.out.println("Processed record : " + i + " For CRA, Date: " + airProblem.getDate());
                    }
                    if (problemIsProcessed && tier1Spreadsheet.get(problem.getUrl())[1].equals("travel")) {
                        if (!this.travelUrlLinkIds.containsKey(problem.getUrl().trim().toUpperCase())) {
                            this.createUrlLinkEntry(problem.getUrl(), travelBase, airtableURLLink);
                        }

                        airProblem.getURLLinkIds().add(this.travelUrlLinkIds.get(problem.getUrl().trim().toUpperCase()));

                        if (!this.travelPageTitleIds.containsKey(problem.getTitle().trim().toUpperCase())) {
                            this.createPageTitleEntry(problem.getTitle(), travelBase, airtablePageTitleLookup);
                        }
                        airProblem.getPageTitleIds().add(this.travelPageTitleIds.get(problem.getTitle().trim().toUpperCase()));

                        for (String tag : problem.getTags()) {
                            if (this.travelMlTagIds.containsKey(tag.trim().toUpperCase())) {
                                airProblem.getTags().add(this.travelMlTagIds.get(tag.trim().toUpperCase()));
                            } else {
                                System.out.println("Missing tag id for:" + tag);
                            }
                        }
                        setAirProblemAttributes(airProblem, problem);
                        travelTable.create(airProblem);
                        System.out.println("Processed record : " + i + " For Travel, Date: " + airProblem.getDate());
                        problem.setAirTableSync("true");
                    }
                    if (problemIsProcessed && tier1Spreadsheet.get(problem.getUrl())[1].equals("ircc")) {

                        if (!this.IRCC_UrlLinkIds.containsKey(problem.getUrl().trim().toUpperCase())) {
                            this.createUrlLinkEntry(problem.getUrl(), IRCC_Base, airtableURLLink);
                        }

                        airProblem.getURLLinkIds().add(this.IRCC_UrlLinkIds.get(problem.getUrl().trim().toUpperCase()));

                        if (!this.IRCC_PageTitleIds.containsKey(problem.getTitle().trim().toUpperCase())) {
                            this.createPageTitleEntry(problem.getTitle(), IRCC_Base, airtablePageTitleLookup);
                        }
                        airProblem.getPageTitleIds().add(this.IRCC_PageTitleIds.get(problem.getTitle().trim().toUpperCase()));

                        for (String tag : problem.getTags()) {
                            if (this.IRCC_MlTagIds.containsKey(tag.trim().toUpperCase())) {
                                airProblem.getTags().add(this.IRCC_MlTagIds.get(tag.trim().toUpperCase()));
                            } else {
                                System.out.println("Missing tag id for:" + tag);
                            }
                        }
                        setAirProblemAttributes(airProblem, problem);
                        irccTable.create(airProblem);
                        System.out.println("Processed record : " + i + " For IRCC, Date: " + airProblem.getDate());
                        problem.setAirTableSync("true");
                    }
                }
                if (i >= maxToSync) {
                    System.out.println("Sync only " + maxToSync + " records at a time...");
                    break;
                }
                i++;
                this.problemRepository.save(problem);
            } catch (Exception e) {

                System.out.println(e.getMessage() + " Could not sync record : " + problem.getId() + " URL:" + problem.getUrl());

            }
        }
    }

    // Marks problems as processed if applicable.
    public void completeProcessing() {
        List<Problem> pList = this.problemRepository.findByProcessed("false");
        pList.addAll(this.problemRepository.findByProcessed(null));
        for (Problem problem : pList) {
            try {
                if (problem.getPersonalInfoProcessed().equals("true") && problem.getAutoTagProcessed().equals("true")
                        && problem.getAirTableSync().equals("true") && (problem.getProcessed() == null || problem.getProcessed().equals("false"))) {
                    problem.setProcessed("true");
                    this.problemRepository.save(problem);
                }
            } catch (Exception e) {
                System.out.println("Could not mark completed because:" + e.getMessage() + ": ID:" + problem.getId());
            }
        }
        System.out.println("Finished processing...");
        exit(0);
    }

    // Temp solution to combat users entering hyperlinks with href HTML tags
    public boolean containsHTML(String... comments) {
        for (String comment : comments) {
            // System.out.println(comment.trim().replaceAll(" +", " ").length());
            // System.out.println(html2text(comment).length());
            // System.out.println(html2text(comment));
            // System.out.println(comment.trim().replaceAll(" +", " "));
            // for some reason, html2text subtracts 1 from the length.
            if (comment != null && (comment.trim().replaceAll(" +", " ").length() != html2text(comment).length())) {
                System.out.println("Detected HTML, deleting entry belonging to comment: " + comment);
                return true;
            }
        }
        return false;
    }

    //TODO: add a check for null
    public String removeQueryAfterHTML(String url) {
        String[] arrOfStr = url.split("(?<=.html)");
        return arrOfStr[0];
    }

    public String returnQueryAfterHTML(String url) {
        String[] arrOfStr = url.split("(?<=.html)");
        if (arrOfStr.length == 2) {
            return arrOfStr[1];
        }
        return null;
    }

    // Test removing personal information from a comment after any changes to cleaning script. (test case)
    public Boolean testRemovePII() {
        return containsHTML(
                "A little easier to look up the Boxes in filling out the T4.  I used Google to find help on the items and that worked well.  It pointed me the CRA help.  The CRA Help was clear for my situation, so this worked well.");
    }

    public void clean(String sentence) {
        String sentenceCleaned = this.contentService.cleanContent(sentence);
    }

    // Sets attributes. Made it into a function to make the code look a bit more readable.
    public void setAirProblemAttributes(AirTableProblemEnhanced airProblem, Problem problem) {
        airProblem.setUniqueID(problem.getId());
        airProblem.setDate(problem.getProblemDate());
        airProblem.setTimeStamp(problem.getTimeStamp());
        airProblem.setURL(problem.getUrl());
        airProblem.setLang(problem.getLanguage().toUpperCase());
        airProblem.setWhatswrong(problem.getProblem());
        airProblem.setComment(problem.getProblemDetails());
        airProblem.setIgnore(null);
        airProblem.setTagsConfirmed(null);
        airProblem.setRefiningDetails("");
        airProblem.setActionable(null);
        airProblem.setMainSection(problem.getSection());
        airProblem.setStatus("New");
        airProblem.setLookupTags(null);
        airProblem.setInstitution(problem.getInstitution());
        airProblem.setTheme(problem.getTheme());
        airProblem.setId(null);
    }

    // Creates record for new titles
    private void createPageTitleEntry(String title, Base base, String pageTitle) throws Exception {
        @SuppressWarnings("unchecked")
        Table<AirTableStat> statsTable = base.table(pageTitle, AirTableStat.class);
        AirTableStat stat = new AirTableStat(title.trim());
        stat = statsTable.create(stat);
        HashMap<String, String> basePageTitleMap = selectMapPageTitleIds(base);
        basePageTitleMap.put(title.trim().toUpperCase(), stat.getId());
    }

    // Creates record for new URLs
    private void createUrlLinkEntry(String url, Base base, String pageTitle) throws Exception {
        @SuppressWarnings("unchecked")
        Table<AirTableURLLink> urlLinkTable = base.table(pageTitle, AirTableURLLink.class);
        AirTableURLLink urlLink = new AirTableURLLink(url.trim());
        urlLink = urlLinkTable.create(urlLink);
        HashMap<String, String> baseURLMap = selectMapUrlLinkIds(base);
        baseURLMap.put(url.trim().toUpperCase(), urlLink.getId());
    }

    public String html2text(String html) {
        return Jsoup.parse(html).text();
    }

    public HashMap<String, String> selectMapPageTitleIds(Base base) {
        if (base.equals(mainBase))
            return this.mainPageTitleIds;
        if (base.equals(healthBase))
            return this.healthPageTitleIds;
        if (base.equals(CRA_Base))
            return this.CRA_PageTitleIds;
        if (base.equals(travelBase))
            return this.travelPageTitleIds;
        if (base.equals(IRCC_Base))
            return this.IRCC_PageTitleIds;
        return null;
    }

    public HashMap<String, String> selectMapUrlLinkIds(Base base) {
        if (base.equals(mainBase))
            return this.mainUrlLinkIds;
        if (base.equals(healthBase))
            return this.healthUrlLinkIds;
        if (base.equals(CRA_Base))
            return this.CRA_UrlLinkIds;
        if (base.equals(travelBase))
            return this.travelUrlLinkIds;
        if (base.equals(IRCC_Base))
            return this.IRCC_UrlLinkIds;
        return null;
    }

    public HashMap<String, String> selectMapMLTagIds(Base base) {
        if (base.equals(mainBase))
            return this.mainMlTagIds;
        if (base.equals(healthBase))
            return this.healthMlTagIds;
        if (base.equals(CRA_Base))
            return this.CRA_MlTagIds;
        if (base.equals(travelBase))
            return this.travelMlTagIds;
        if (base.equals(IRCC_Base))
            return this.IRCC_MlTagIds;
        return null;
    }

}
