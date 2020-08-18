package ca.gc.tbs;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;

import com.sybit.airtable.Airtable;
import com.sybit.airtable.Base;
import com.sybit.airtable.Table;

import ca.gc.tbs.domain.Problem;
import ca.gc.tbs.repository.ProblemRepository;
import ca.gc.tbs.service.ContentService;

import static java.lang.System.exit;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
@ComponentScan(basePackages = { "ca.gc.tbs.domain", "ca.gc.tbs.repository" })
public class Main implements CommandLineRunner {

	public static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	public static final SimpleDateFormat INPUT_FORMAT = new SimpleDateFormat("EEE MMM dd yyyy");

	@Autowired
	private ProblemRepository problemRepository;

	private ContentService contentService = new ContentService();

	@Value("${airtable.key}")
	private String airtableKey;

	@Value("${airtable.base}")
	private String airtableBase;

	@Value("${airtable.tab}")
	private String airtableTab;

	@Value("${airtable.pageTitleLookup}")
	private String airtablePageTitleLookup;

	@Value("${airtable.mlTags}")
	private String airtableMLTags;

	private Airtable airTable;
	private Base base;

	private HashMap<String, String> pageTitleIds = new HashMap<String, String>();
	private HashMap<String, String> mlTagIds = new HashMap<String, String>();

	public static void main(String args[]) throws Exception {
		new SpringApplicationBuilder(Main.class).web(WebApplicationType.NONE) // .REACTIVE, .SERVLET
				.run(args);
	}

	public Main() throws Exception {

	}

	@Override
	public void run(String... args) throws Exception {
		this.airTable = new Airtable().configure(this.airtableKey);
		this.base = this.airTable.base(this.airtableBase);
		this.getPageTitleIds();
		this.getMLTagIds();
		this.removePersonalInfo();
		this.autoTag();
		this.airTableSync();
		this.completeProcessing();
		//testRemovePII();

	}
	
	public void testRemovePII() {
		String content =this.contentService.cleanContent("We own our business\n" + 
				"Property\n" + 
				"Need\n" + 
				"Buss  relate to travel\n" + 
				"And shut  down since March\n" + 
				"But have to pay   city  403  735 6090 taxes   condo  fee and  utility   \n" + 
				"Cant  get  rent  assistance  do  not have  rent  to  pay \n" + 
				"How  can  I  get help to  pay   city  taxes ut and condo fee    etc  \n" + 
				"Call  me   403  735 6090\n" + 
				"Than");
		System.out.println("Content cleaned."+content);
	}

	public void syncDataAfter(String date) throws ParseException {
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

	public void autoTag() {
		List<Problem> pList = this.problemRepository.findByAutoTagProcessed("false");
		pList.addAll(this.problemRepository.findByAutoTagProcessed(null));
		for (Problem problem : pList) {
			String model = "";
			try {
				if (problem.getYesno().toUpperCase().equals("NO")) {
					String lang = "en";
					if (problem.getLanguage().toLowerCase().equals("fr")) {
						lang = "fr";
					}
					String text = URLEncoder.encode(problem.getProblemDetails(), StandardCharsets.UTF_8.name());
					String title = problem.getTitle().toLowerCase();
					String inst = problem.getInstitution().toLowerCase();

					if (title.contains("symptoms") || title.contains("prevention") || title.contains("symptômes")) {
						model = "Health";

					} else if (title.contains("entreprise") || title.contains("business")) {
						model = "Business";
					} else if (inst != null && inst.contains("fin")) {
						model = "Business";
					}

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

	public void resetAirTableFlag() {
		List<Problem> pList = this.problemRepository.findByAirTableSync("true");
		for (Problem problem : pList) {
			problem.setAirTableSync("false");
			this.problemRepository.save(problem);
		}
	}

	public void setPrivateFlagForSync() {
		List<Problem> pList = this.problemRepository.findByPersonalInfoProcessed("false");
		for (Problem problem : pList) {
			problem.setPersonalInfoProcessed("true");
			this.problemRepository.save(problem);
		}
	}

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

	public void airTableSync() throws Exception {
		@SuppressWarnings("unchecked")
		Table<AirTableProblemEnhanced> problemTable = base.table(this.airtableTab, AirTableProblemEnhanced.class);
		System.out.println("Connected to Airtable");
		List<Problem> pList = this.problemRepository.findByAirTableSync(null);
		pList.addAll(this.problemRepository.findByAirTableSync("false"));
		System.out.println("Connected to MongoDB");
		System.out.println("Found " + pList.size() + " records that need to by added.");
		for (Problem problem : pList) {
			try {
				if (problem.getPersonalInfoProcessed().equals("true") && problem.getAutoTagProcessed().equals("true")) {
					AirTableProblemEnhanced airProblem = new AirTableProblemEnhanced();
					airProblem.setUniqueID(problem.getId());
					airProblem.setDate(DATE_FORMAT.format(INPUT_FORMAT.parse(problem.getProblemDate())));
					airProblem.setURL(problem.getUrl());
					if (!this.pageTitleIds.containsKey(problem.getTitle().trim().toUpperCase())) {
						this.createPageTitleEntry(problem.getTitle());
					}
					airProblem.getPageTitleIds().add(this.pageTitleIds.get(problem.getTitle().trim().toUpperCase()));
					airProblem.setLang(problem.getLanguage().toUpperCase());
					airProblem.setWhatswrong(problem.getProblem());
					airProblem.setComment(problem.getProblemDetails());
					airProblem.setIgnore(null);
					for (String tag : problem.getTags()) {
						if (this.mlTagIds.containsKey(tag.trim().toUpperCase())) {
							airProblem.getTags().add(this.mlTagIds.get(tag.trim().toUpperCase()));
						} else {
							System.out.println("Missing tag id for:" + tag);
						}
					}
					airProblem.setTagsConfirmed(null);
					airProblem.setRefiningDetails("");
					airProblem.setActionable(null);
					airProblem.setMainSection(problem.getSection());
					airProblem.setYesno(problem.getYesno());
					airProblem.setStatus("New");
					airProblem.setLookupTags(null);
					airProblem.setInstitution(problem.getInstitution());
					airProblem.setTheme(problem.getTheme());
					airProblem.setId(null);
					problemTable.create(airProblem);
					problem.setAirTableSync("true");
					this.problemRepository.save(problem);
				}
			} catch (Exception e) {
				System.out.println(
						e.getMessage() + " Could not process record: " + problem.getId() + " URL:" + problem.getUrl());

			}
		}

	}

	private void getPageTitleIds() throws Exception {
		@SuppressWarnings("unchecked")
		Table<AirTableStat> statsTable = base.table(this.airtablePageTitleLookup, AirTableStat.class);
		System.out.println("Connected to Airtable Stats");
		List<AirTableStat> stats = statsTable.select();
		for (AirTableStat stat : stats) {
			this.pageTitleIds.put(stat.getPageTitle().trim().toUpperCase(), stat.getId());
		}
	}

	private void getMLTagIds() throws Exception {
		@SuppressWarnings("unchecked")
		Table<AirTableMLTag> tagsTable = base.table(this.airtableMLTags, AirTableMLTag.class);
		System.out.println("Connected to Airtable Stats");
		List<AirTableMLTag> tags = tagsTable.select();
		for (AirTableMLTag tag : tags) {
			try {
				String tagName = tag.getTag().trim().toUpperCase();
				String id = tag.getId();
				this.mlTagIds.put(tagName, id);
			} catch (Exception e) {
				System.out.println("Could not add ML tag because:" + e.getMessage());
			}
		}
	}

	private void createPageTitleEntry(String title) throws Exception {
		@SuppressWarnings("unchecked")
		Table<AirTableStat> statsTable = base.table(this.airtablePageTitleLookup, AirTableStat.class);
		AirTableStat stat = new AirTableStat(title.trim());
		stat = statsTable.create(stat);
		this.pageTitleIds.put(title.trim().toUpperCase(), stat.getId());
		System.out.println("Created record for title");
	}

	public ProblemRepository getProblemRepository() {
		return problemRepository;
	}

	public void setProblemRepository(ProblemRepository problemRepository) {
		this.problemRepository = problemRepository;
	}

	public ContentService getContentService() {
		return contentService;
	}

	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}
}