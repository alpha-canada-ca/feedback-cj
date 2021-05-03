package ca.gc.tbs;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.List;


import java.util.ArrayList;
import java.util.Arrays;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.DeleteDimensionRequest;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;


public class GoogleSheetsAPI {
	
	private static Sheets sheetsService;
	private static String APPLICATION_NAME = "example";
	private static String SPREADSHEET_ID = "1B16qEbfp7SFCfIsZ8fcj7DneCy1WkR0GPh4t9L9NRSg";
	
	private static Credential authorize() throws IOException, GeneralSecurityException {
		
		InputStream in = GoogleSheetsAPI.class.getResourceAsStream("/spreadsheetCredentials.json");

		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
				JacksonFactory.getDefaultInstance(), new InputStreamReader(in)
		);
		
		List<String> scopes = Arrays.asList(SheetsScopes.SPREADSHEETS);
		
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
				GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(),clientSecrets, scopes)
				.setDataStoreFactory(new FileDataStoreFactory( new java.io.File("tokens")))
				.setAccessType("offline")
				.build();
		Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver())
				.authorize("user");
		
		return credential;
	}
	
	public static Sheets getSheetsService() throws IOException, GeneralSecurityException {
		Credential credential = authorize();
		return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(),
				JacksonFactory.getDefaultInstance(), credential)
				.setApplicationName(APPLICATION_NAME)
				.build();
	}
	
	public static void main(String[] args) throws IOException, GeneralSecurityException {
		//sheetsService = getSheetsService();
		
		//////////////////////////////////////
		//String range = "A1:A500";
		
//		PRINT VALUES IN SPREADSHEET
//		ValueRange response = sheetsService.spreadsheets().values()
//				.get(SPREADSHEET_ID, range)
//				.execute();
//		
//		List<List<Object>> values = response.getValues();
//		
//		if (values == null || values.isEmpty()) {
//			System.out.println("No data found.");
//		} else {
//			for(List row : values) {
//				System.out.printf("1: %s \n", row.get(0));
//			}
//		}
//		
		//////////////////////////////////////	
//		
//		String url = "https://www.canada.ca/fr/agence-revenu/services/paiements-arc/paiements-particuliers/faire-paiement.html";
//		addEntry(url);
//		
		
		//////////////////////////////////////
		//CHANGE VALUES IN SPREADSHEET OF SPECIFIC ENTRY
		
//		ValueRange body = new ValueRange()
//				.setValues(Arrays.asList( 
//						Arrays.asList("changed it lets go!")
//				));
//		
//		UpdateValuesResponse result = sheetsService.spreadsheets().values()
//				.update(SPREADSHEET_ID, "A5", body)
//				.setValueInputOption("RAW")
//				.execute();
//		
//		//////////////////////////////////////
		//DELETE SPECIFIC ENTRY FROM SPREADSHEET
//		
//		DeleteDimensionRequest deleteRequest = new DeleteDimensionRequest()
//				.setRange(
//						new DimensionRange()
//						.setSheetId(0)
//						.setDimension("ROWS")
//						.setStartIndex(6) //can give index of url in the cj map
//						.setEndIndex(7)
//				);
//		
//		List<Request> requests = new ArrayList<>();
//		requests.add(new Request().setDeleteDimension(deleteRequest));
//		
//		BatchUpdateSpreadsheetRequest bodyDelete = new BatchUpdateSpreadsheetRequest().setRequests(requests);
//		sheetsService.spreadsheets().batchUpdate(SPREADSHEET_ID, bodyDelete).execute();
	}
	
	public static void addEntry(String url) throws IOException, GeneralSecurityException {
		sheetsService = getSheetsService();
		String range = "A1:A500";
		ValueRange appendBody = new ValueRange()
				.setValues(Arrays.asList( 
						Arrays.asList(url)
				));
		
		try {
			AppendValuesResponse appendResult = sheetsService.spreadsheets().values()
					.append(SPREADSHEET_ID, range, appendBody)
					.setValueInputOption("USER_ENTERED")
					.setInsertDataOption("INSERT_ROWS")
					.setIncludeValuesInResponse(true)
					.execute();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
}
