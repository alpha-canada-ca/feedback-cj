package ca.gc.tbs;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.datatables.DataTablesRepositoryFactoryBean;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.sybit.airtable.Airtable;
import com.sybit.airtable.Base;
import com.sybit.airtable.Table;

import ca.gc.tbs.domain.Problem;
import ca.gc.tbs.domain.TopTaskSurvey;
import ca.gc.tbs.repository.ProblemRepository;
import ca.gc.tbs.repository.TopTaskRepository;
import ca.gc.tbs.service.ContentService;

import static java.lang.System.exit;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
@ComponentScan(basePackages = { "ca.gc.tbs.domain", "ca.gc.tbs.repository" })
@EnableMongoRepositories(repositoryFactoryBeanClass = DataTablesRepositoryFactoryBean.class)
public class Main implements CommandLineRunner {

	public static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	public static final SimpleDateFormat INPUT_FORMAT = new SimpleDateFormat("EEE MMM dd yyyy");

	@Autowired
	private ProblemRepository problemRepository;
	
	@Autowired
	private TopTaskRepository topTaskRepository;

	private ContentService contentService = new ContentService();
	
	// Main AirTable
	@Value("${airtable.key}")
	private String airtableKey;

	@Value("${airtable.base}")
	private String problemAirtableBase;

	@Value("${airtable.tab}")
	private String problemAirtableTab;
	
	@Value("${airtable.pageTitleLookup}")
	private String airtablePageTitleLookup;

	@Value("${airtable.mlTags}")
	private String airtableMLTags;
	
	@Value("${airtable.URL_link}")
	private String airtableURLLink;
	
	// Health AirTable
	@Value("${health.airtable.base}")
	private String healthAirtableBase;
	
	// CRA AirTable
	@Value("${cra.airtable.base}")
	private String CRA_AirtableBase;

	private Airtable AirTableKey;
	
	private Base mainBase;
	private Base healthBase;
	private Base CRA_Base;

	private HashMap<String, String[]> modelBaseByURL = new HashMap<String, String[]>();
	
	private HashMap<String, String> problemPageTitleIds = new HashMap<String, String>();
	private HashMap<String, String> healthPageTitleIds = new HashMap<String, String>();
	private HashMap<String, String> CRA_PageTitleIds = new HashMap<String, String>();
	
	private HashMap<String, String> problemUrlLinkIds = new HashMap<String, String>();
	private HashMap<String, String> healthUrlLinkIds = new HashMap<String, String>();
	private HashMap<String, String> CRA_UrlLinkIds = new HashMap<String, String>();
	
	private HashMap<String, String> problemMlTagIds = new HashMap<String, String>();
	private HashMap<String, String> healthMlTagIds = new HashMap<String, String>();
	private HashMap<String, String> CRA_MlTagIds = new HashMap<String, String>();
	
	public HashMap<String, String> selectMapPageTitleIds(Base base) {
		  if(base.equals(mainBase))
		    return this.problemPageTitleIds;
		  if(base.equals(healthBase))
		    return this.healthPageTitleIds;
		  if(base.equals(CRA_Base))
		    return this.CRA_PageTitleIds;
		  return null;
	} 
	
	public HashMap<String, String> selectMapUrlLinkIds(Base base) {
		  if(base.equals(mainBase))
		    return this.problemUrlLinkIds;
		  if(base.equals(healthBase))
		    return this.healthUrlLinkIds;
		  if(base.equals(CRA_Base))
		    return this.CRA_UrlLinkIds;
		  return null;
	} 
	public HashMap<String, String> selectMapMLTagIds(Base base) {
		  if(base.equals(mainBase))
		    return this.problemMlTagIds;
		  if(base.equals(healthBase))
		    return this.healthMlTagIds;
		  if(base.equals(CRA_Base))
		    return this.CRA_MlTagIds;
		  return null;
	} 
	
	public static void main(String args[]) throws Exception {
		new SpringApplicationBuilder(Main.class).web(WebApplicationType.NONE) // .REACTIVE, .SERVLET
				.run(args);
	}

	public Main() throws Exception {

	}
	
	// Main Loop, Runs all functions needed.
	@Override
	public void run(String... args) throws Exception {
		this.AirTableKey = new Airtable().configure(this.airtableKey);
		
		this.mainBase = this.AirTableKey.base(this.problemAirtableBase);
		this.healthBase = this.AirTableKey.base(this.healthAirtableBase);
		this.CRA_Base = this.AirTableKey.base(this.CRA_AirtableBase);
		
		this.importModels();
		this.getPageTitleIds(mainBase);
		this.getPageTitleIds(healthBase);
		this.getPageTitleIds(CRA_Base);
		
		this.getMLTagIds(mainBase);
		this.getMLTagIds(healthBase);
		this.getMLTagIds(CRA_Base);
		
		this.getURLLinkIds(mainBase);
		this.getURLLinkIds(healthBase);
		this.getURLLinkIds(CRA_Base);
		
		this.removePersonalInfoExitSurvey();
		this.removePersonalInfo();
		this.autoTag();
		this.airTableSync();
		this.completeProcessing();
	}

	// Use this function to test removing personal information from a comment after any changes to cleaning code. (test case)
	public void testRemovePII() {
		String content = this.contentService.cleanContent("We own our business\n" + "Property\n" + "Need\n"
				+ "Buss  relate to travel\n" + "And shut  down since March\n"
				+ "But have to pay   city  403  735 6090 taxes   condo  fee and  utility   \n"
				+ "Cant  get  rent  assistance  do  not have  rent  to  pay \n"
				+ "How  can  I  get help to  pay   city  taxes ut and condo fee    etc  \n"
				+ "Call  me   403  735 6090\n" + "Than");
		System.out.println("Content cleaned." + content);
	}

	// This function sets problem entries to setAirTableSync="false" after date given to function
	public void reSyncDataAfterDate(String date) throws ParseException {
		Date afterDate = DATE_FORMAT.parse(date);
		List<Problem> pList = this.problemRepository.findByAirTableSync("true");
		for (Problem problem : pList) {
			try {
				Date problemDate = INPUT_FORMAT.parse(problem.getProblemDate());
				if (problemDate.after(afterDate)) {
					problem.setAirTableSync("false");
					this.problemRepository.save(problem);
				}
			} catch (Exception e) {
				System.out.println("Could not process: " + problem.getId() + ":" + problem.getProblemDate());
			}
		}
	}

	// This function finds data that has already been ran by airTableSync and sets processed values to true (not in use)
	public void flagAlreadyAddedData() {
		List<Problem> pList = this.problemRepository.findByAirTableSync("true");
		for (Problem problem : pList) {
			try {
				problem.setAutoTagProcessed("true");
				problem.setPersonalInfoProcessed("true");
				problem.setProcessed("true");
				this.problemRepository.save(problem);
			} catch (Exception e) {
				System.out.println(e.getMessage() + " could not set existing record as processed.");
			}
		}
	}

	// Function resets problems that meet if criteria by setting variables to false forcing them to get processed again (not being used)
	public void resetEverything() {
		List<Problem> pList = this.problemRepository.findAll();
		for (Problem problem : pList) {
			// exclude health for right now
			if (!problem.getTitle().contains("Symptoms") || !problem.getTitle().contains("Prevention+risks")) {
				problem.setAutoTagProcessed("true");
				problem.setAirTableSync("false");
				problem.setPersonalInfoProcessed("false");
				problem.setProcessed("false");
			}
		}
	}
	

	// This function grabs all the models and associated URLs from the google spreadsheet.
	public void importModels() throws Exception {
		final Reader reader = new InputStreamReader(new URL(
				"https://docs.google.com/spreadsheets/d/1eOmX_b8XCR9eLNxUbX3Gwkp2ywJ-vhapnC7ApdRbnSg/export?format=csv")
						.openConnection().getInputStream(),
				"UTF-8");
		final CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader());
		try {
			for (final CSVRecord record : parser) {
				try {
					String[] modelBase = {record.get("MODEL"), record.get("BASE")};
					modelBaseByURL.put(record.get("URL"), modelBase);			
				} catch (Exception e) {
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
			}
		} finally {
			parser.close();
			reader.close();
		}
	}

	//This function grabs all problems that have not been assigned auto tags and assigns tags.
	public void autoTag() {
		List<Problem> pList = this.problemRepository.findByAutoTagProcessed("false");
		pList.addAll(this.problemRepository.findByAutoTagProcessed(null));
		for (Problem problem : pList) {
			String model = "";
			try {
				// If problem has comment, assign language & model. 
				if (!problem.getProblemDetails().trim().equals("")) {
					String lang = "en";
					if (problem.getLanguage().toLowerCase().equals("fr")) {
						lang = "fr";
					}
					String text = URLEncoder.encode(problem.getProblemDetails(), StandardCharsets.UTF_8.name());
					String URL = removeQueryAfterHTML(problem.getUrl()).toLowerCase();
					
					if(modelBaseByURL.containsKey(URL)) {
						model = modelBaseByURL.get(URL)[0];
						System.out.println("model: " + model);
					}
					// Then feed through the suggestion script (Feedback-Classification-RetroAction Repository) if model exists
					// and assign tags if applicable.
					if (!model.equals("")) {
						Document doc = Jsoup.connect("https://suggestion.tbs.alpha.canada.ca/suggestCategory?lang="
								+ lang + "&text=" + text + "&section=" + model).maxBodySize(0).get();
						String tags = doc.select("body").html();
						System.out.println("Text:" + text + " : " + tags);
						String splitTags[] = tags.split(",");
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

	public String removeQueryAfterHTML(String url) {
		String[] arrOfStr 	= url.split("(?<=.html)");
		return arrOfStr[0];
	}
	public String returnQueryAfterHTML(String url) {
		String[] arrOfStr 	= url.split("(?<=.html)");
		if(arrOfStr.length == 2) {
			return arrOfStr[1];
		}
		return null;
	}
	
	// This function marks problems as processed if applicable.
	public void completeProcessing() {
		List<Problem> pList = this.problemRepository.findByProcessed("false");
		pList.addAll(this.problemRepository.findByProcessed(null));
		for (Problem problem : pList) {
			try {
				if (problem.getPersonalInfoProcessed().equals("true") && problem.getAutoTagProcessed().equals("true")
						&& problem.getAirTableSync().equals("true")
						&& (problem.getProcessed() == null || problem.getProcessed().equals("false"))) {
					problem.setProcessed("true");
					this.problemRepository.save(problem);
				}
			} catch (Exception e) {
				System.out.println("Could not mark completed because:" + e.getMessage() + ": ID:" + problem.getId());
			}
		}
		System.out.println("Finished processiing...");
		exit(0);
	}

	// This function sets AirTableSync value to false for all problems. (not being used)
	public void resetAirTableFlag() {
		List<Problem> pList = this.problemRepository.findByAirTableSync("true");
		int i = 0;
		for (Problem problem : pList) {
			i++;
			if(problem.getSection().toLowerCase().equals("ptr")) {
				problem.setAirTableSync("false");
				this.problemRepository.save(problem);
				System.out.println("Reset PTR: " + i);
			}
		}
	}

	// This function sets PersonalInfoProcessed value to true for all problems. (not being used)
	public void setPrivateFlagForSync() {
		List<Problem> pList = this.problemRepository.findByPersonalInfoProcessed("false");
		for (Problem problem : pList) {
			problem.setPersonalInfoProcessed("true");
			this.problemRepository.save(problem);
		}
	}

	// This function finds problems that have not been cleaned and runs them through the cleaning code (cleanContent in FeedbackViewer repo)
	public void removePersonalInfo() {
		System.out.println("Starting private info removal...");
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
	
	// This function finds tasks (Exit Survey) that have not been cleaned and runs them through the cleaning code. 
	// (cleanContent in FeedbackViewer Repository)
	public void removePersonalInfoExitSurvey() {
		System.out.println("Starting private info removal TOP TASK...");
		List<TopTaskSurvey> tList = this.topTaskRepository.findByPersonalInfoProcessed(null);
		tList.addAll(this.topTaskRepository.findByPersonalInfoProcessed("false"));
		for (TopTaskSurvey task : tList) {
			try {
				if(task.getThemeOther() != null) {
					String details = this.contentService.cleanContent(task.getThemeOther());
					task.setThemeOther(details);
				}
				if(task.getTaskOther() != null) {
					String details = this.contentService.cleanContent(task.getTaskOther());
					task.setTaskOther(details);
				}
				if(task.getTaskImproveComment() != null) {
					String details = this.contentService.cleanContent(task.getTaskImproveComment());
					task.setTaskImproveComment(details);
				}
				if(task.getTaskWhyNotComment() != null) {
					String details = this.contentService.cleanContent(task.getTaskWhyNotComment());
					task.setTaskWhyNotComment(details);
				}
				task.setPersonalInfoProcessed("true");
				this.topTaskRepository.save(task);
			} catch (Exception e) {
				System.out.println("Could not process task: " + task.getId() + " : " + task.getDateTime() + " : " + task.getTaskOther() + " : " + task.getTaskImproveComment() + " : " + task.getTaskWhyNotComment());
			}
		}
		System.out.println("Private info removed top task...");
	}


	// This function populates problem entries to AirTable base.
	public void airTableSync() throws Exception {
		// Connect to AirTable bases
		@SuppressWarnings("unchecked")
		Table<AirTableProblemEnhanced> problemTable = mainBase.table(this.problemAirtableTab, AirTableProblemEnhanced.class);
		@SuppressWarnings("unchecked")
		Table<AirTableProblemEnhanced> healthTable 	= healthBase.table(this.problemAirtableTab, AirTableProblemEnhanced.class);
		@SuppressWarnings("unchecked")
		Table<AirTableProblemEnhanced> craTable 	= CRA_Base.table(this.problemAirtableTab, AirTableProblemEnhanced.class);
		System.out.println("Connected to Airtable");
		// Find problems that have not been ran through this function
		List<Problem> pList = this.problemRepository.findByAirTableSync(null);
		pList.addAll(this.problemRepository.findByAirTableSync("false"));
		System.out.println("Connected to MongoDB");
		System.out.println("Found " + pList.size() + " records that need to be added.");
		int i = 1;
		int maxToSync = 100;
		for (Problem problem : pList) {
			try {
				boolean sectionCRABASE = problem.getSection().toLowerCase().equals("ptr") || problem.getSection().toLowerCase().equals("itb") || problem.getSection().toLowerCase().equals("ecm");
				boolean problemIsProcessed = problem.getPersonalInfoProcessed().equals("true") && problem.getAutoTagProcessed().equals("true") && !problem.getProblemDetails().trim().equals("");
				
				String UTM_value = returnQueryAfterHTML(problem.getUrl());
				problem.setUrl(removeQueryAfterHTML(problem.getUrl()));
				
				if(modelBaseByURL.get(problem.getUrl())[1].toLowerCase().equals("nobase")) {
					//do nothing
				} else {
					
					// Check if conditions met to go to main AirTable and populate.
					if (problemIsProcessed && ((!problem.getInstitution().toLowerCase().contains("health") && !sectionCRABASE)
							|| (modelBaseByURL.get(problem.getUrl()) != null && modelBaseByURL.get(problem.getUrl())[1].toLowerCase().equals("main")))) {
						AirTableProblemEnhanced airProblem = new AirTableProblemEnhanced();
						airProblem.setUTM(UTM_value);
						
						if (!this.problemUrlLinkIds.containsKey(problem.getUrl().trim().toUpperCase())) {
							this.createUrlLinkEntry(problem.getUrl(), mainBase, airtableURLLink);
						}
						airProblem.getURLLinkIds().add(this.problemUrlLinkIds.get(problem.getUrl().trim().toUpperCase()));
						if (!this.problemPageTitleIds.containsKey(problem.getTitle().trim().toUpperCase())) {
							this.createPageTitleEntry(problem.getTitle(), mainBase, airtablePageTitleLookup);
						}
						airProblem.getPageTitleIds().add(this.problemPageTitleIds.get(problem.getTitle().trim().toUpperCase()));
	
						for (String tag : problem.getTags()) {
							if (this.problemMlTagIds.containsKey(tag.trim().toUpperCase())) {
								airProblem.getTags().add(this.problemMlTagIds.get(tag.trim().toUpperCase()));
							} else {
								System.out.println("Missing tag id for:" + tag);
							}
						}  
						
						setAirProblemAttributes(airProblem, problem);
						problemTable.create(airProblem);
						System.out.println("# of processed records: "+ i + " Date: "+ airProblem.getDate());
					} 
					// Check if conditions met to go to health AirTable and populate.
					if((problemIsProcessed && !sectionCRABASE)
							&& (problem.getInstitution().toLowerCase().contains("health") || ( modelBaseByURL.get(problem.getUrl()) != null && modelBaseByURL.get(problem.getUrl())[1].toLowerCase().equals("health")))) {
						AirTableProblemEnhanced airProblem = new AirTableProblemEnhanced();
						airProblem.setUTM(UTM_value);
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
						System.out.println("Processed record: "+ i + " Date: "+ airProblem.getDate());
					}
					// Check if conditions met to go to CRA AirTable and populate.
					if(problemIsProcessed && (sectionCRABASE || (modelBaseByURL.get(problem.getUrl()) != null && modelBaseByURL.get(problem.getUrl())[1].toLowerCase().equals("cra")))) {
						AirTableProblemEnhanced airProblem = new AirTableProblemEnhanced();
						airProblem.setUTM(UTM_value);
						
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
						System.out.println("Processed record: "+ i + " Date: "+ airProblem.getDate());
					}
				}
				if (i >= maxToSync) {
					System.out.println("Sync only "+ maxToSync +" records at a time...");
					break;
				}
				i++;
				problem.setAirTableSync("true");
				this.problemRepository.save(problem);
			} catch (Exception e) {
				
				System.out.println(
						e.getMessage() + " Could not sync record: " + problem.getId() + " URL:" + problem.getUrl());

			}
		}
	}
	public void setAirProblemAttributes(AirTableProblemEnhanced airProblem, Problem problem ) {
		airProblem.setUniqueID(problem.getId());
		airProblem.setDate(problem.getProblemDate());
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

	// This function grabs Page feedback statistics page IDs and adds them to a hashmap for their respective AirTable (main or health)
	private void getPageTitleIds(Base base) throws Exception {
		@SuppressWarnings("unchecked")
		Table<AirTableStat> statsTable = base.table(this.airtablePageTitleLookup, AirTableStat.class);
		System.out.println("Connected to Airtable Stats");
		List<AirTableStat> stats 	= statsTable.select();
		HashMap<String, String> m 	= selectMapPageTitleIds(base);
		stats.forEach(entry -> {try { m.put(entry.getPageTitle().trim().toUpperCase(), entry.getId());
	        } catch(Exception e) {
	        	System.out.println(e.getMessage() + " Could not add Page Title ID: " + entry.getPageTitle() + " in base: " + base.name());
	        }
	    });
	}
	
	// This function grabs Page groups by URL and adds them to a hashmap for their respective AirTable (main or health)
	private void getURLLinkIds(Base base) throws Exception {
		@SuppressWarnings("unchecked")
		Table<AirTableURLLink> urlLinkTable = base.table(this.airtableURLLink, AirTableURLLink.class);
		System.out.println("Connected to Airtable Stats");
		List<AirTableURLLink> urlLinks = urlLinkTable.select();
		HashMap<String, String> m 	= selectMapUrlLinkIds(base);
		urlLinks.forEach(entry -> {try { m.put(entry.getURLlink().trim().toUpperCase(), entry.getId());
	        } catch(Exception e) {
	        	System.out.println(e.getMessage() + " Could not add URL Link ID: " + entry.getURLlink() + " in base: " + base.name());
	        }
	    });
	}

	
	// This function grabs ML Tags and adds them to a hashmap for the main AirTable
	private void getMLTagIds(Base base) throws Exception {
		@SuppressWarnings("unchecked")
		Table<AirTableMLTag> tagsTable = base.table(airtableMLTags, AirTableMLTag.class);
		System.out.println("Connected to Airtable Stats");
		List<AirTableMLTag> tags = tagsTable.select();
		HashMap<String, String> m 	= selectMapMLTagIds(base);
		tags.forEach(entry -> {try { m.put(entry.getTag().trim().toUpperCase(), entry.getId());
	        } catch(Exception e) {
	        	System.out.println(e.getMessage() + " Could not add ML Tag ID: " + entry.getTag() + " in base: " + base.name());
	        }
	    });
	}

	//Creates records for new titles
	private void createPageTitleEntry(String title, Base base, String pageTitle) throws Exception {
		@SuppressWarnings("unchecked")
		Table<AirTableStat> statsTable = base.table(pageTitle, AirTableStat.class);
		AirTableStat stat = new AirTableStat(title.trim());
		stat = statsTable.create(stat);
		if(base.equals(mainBase)) {
			this.problemPageTitleIds.put(title.trim().toUpperCase(), stat.getId());
		}
		if(base.equals(healthBase)) {
			this.healthPageTitleIds.put(title.trim().toUpperCase(), stat.getId());
		}
		if(base.equals(CRA_Base)) {
			this.CRA_PageTitleIds.put(title.trim().toUpperCase(), stat.getId());
		}
		//System.out.println("Created record for title in base: " + base);
	}
	 //Creates records for new URLs
	 private void createUrlLinkEntry(String url, Base base, String pageTitle) throws Exception {
		 @SuppressWarnings("unchecked")
			Table<AirTableURLLink> urlLinkTable = base.table(pageTitle, AirTableURLLink.class);
			AirTableURLLink urlLink = new AirTableURLLink(url.trim());
			urlLink = urlLinkTable.create(urlLink);
			if(base.equals(mainBase)) {
				this.problemUrlLinkIds.put(url.trim().toUpperCase(), urlLink.getId());
			}
			if(base.equals(healthBase)) {
				this.healthUrlLinkIds.put(url.trim().toUpperCase(), urlLink.getId());
			}
			if(base.equals(CRA_Base)) {
				this.CRA_UrlLinkIds.put(url.trim().toUpperCase(), urlLink.getId());
			}
			//System.out.println("Created record for title in base: " + base);
	} 

	public ProblemRepository getProblemRepository() {
		return problemRepository;
	}

	public void setProblemRepository(ProblemRepository problemRepository) {
		this.problemRepository = problemRepository;
	}

	public TopTaskRepository getTopTaskRepository() {
		return topTaskRepository;
	}

	public void setTopTaskRepository(TopTaskRepository topTaskRepository) {
		this.topTaskRepository = topTaskRepository;
	}

	public ContentService getContentService() {
		return contentService;
	}

	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}
}