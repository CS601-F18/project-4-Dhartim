package cs601.project4.FrontEndService;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.naming.ConfigurationException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import cs601.project4.RequestResponseManager;
import cs601.project4.Configuration.Configuration;
import cs601.project4.Events.EventList;
import cs601.project4.Events.EventResponseWithEventId;
import cs601.project4.Tickets.TransferTicketBuffer;
import cs601.project4.Users.UserDetails;
import cs601.project4.Users.UserResponseFrontEnd;

//GET /users/{userid}
//POST /users/{userid}/tickets/transfer
@SuppressWarnings("serial")
public class GetUserHandler extends HttpServlet
{
	HttpURLConnection httpConn;
	InputStream input;
	RequestResponseManager reMngr = new RequestResponseManager();
	Configuration config = Configuration.getInstance();
	Gson gson = new Gson();
	//GET /users/{userid}
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
	{
		resp.setStatus(HttpServletResponse.SC_OK);
		String eventid = req.getPathInfo().split("/")[1];
		System.out.println(req.getPathInfo());
		//check for eventid
		if(eventid == null || eventid.isEmpty() || !(eventid.matches("\\d+")) || req.getPathInfo().split("/").length != 2)
		{
			System.out.println("here");
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		URL url = new URL(config.getUserserviceurl() + Integer.parseInt(eventid));
		System.out.println(url);
		httpConn = (HttpURLConnection) url.openConnection();
		httpConn.setRequestMethod("GET");
		httpConn.setRequestProperty("Content-Type", "application/json");
		httpConn.setRequestProperty("Accept", "application/json");
		httpConn.setDoOutput(false); // false indicates this is a GET request
		httpConn.connect();
		resp.setStatus(httpConn.getResponseCode());
		if(resp.getStatus() != HttpServletResponse.SC_OK)
		{
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		//get event details in front end....how ???
		System.out.println("send request");
		input = httpConn.getInputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
		StringBuffer responsebuffer = new StringBuffer();
		String line="";
		while ((line = reader.readLine()) != null) 
		{
			responsebuffer.append(line);
		}
		System.out.println(responsebuffer.toString());
		UserDetails userDetails = gson.fromJson(responsebuffer.toString(), UserDetails.class);
		
		/* new code */
		UserResponseFrontEnd userResponseFrontEnd = getEachEventDetails(userDetails, req, resp);
		String data = gson.toJson(userResponseFrontEnd, UserResponseFrontEnd.class);
		// reader.close();
		PrintWriter writer = resp.getWriter().append(data + "\n");
		writer.println();
		/* new code end */
		//getEachEventDetails(userDetails, resp);
		//reader.close();
		//PrintWriter writer = resp.getWriter().append(responsebuffer+"\n");
		//writer.println();
	}
	// get eventdetails for each event id
		public UserResponseFrontEnd getEachEventDetails(UserDetails userDetails, HttpServletRequest req,
				HttpServletResponse resp) {
			/* update this function */
			EventResponseWithEventId envent = new EventResponseWithEventId();
			UserResponseFrontEnd uResponseFrontEnd = new UserResponseFrontEnd();
			List<EventResponseWithEventId> eventList = new ArrayList<EventResponseWithEventId>();

			List<HashMap<String, Integer>> tickets = userDetails.getTickets();
			for (HashMap<String, Integer> listOfEvents : tickets) {
				int eventid = listOfEvents.get("eventid");
				System.out.println(eventid);
				// create request to get event details
				envent = reMngr.getEventDetails(eventid, req, resp);
				eventList.add(envent);
				System.out.println(eventList);
			}
			uResponseFrontEnd = new UserResponseFrontEnd(userDetails.getUserid(), userDetails.getUsername(), eventList);
			return uResponseFrontEnd;
		}
//	//get eventdetails for each event id
//	public void getEachEventDetails(UserDetails userDetails, HttpServletRequest req, HttpServletResponse resp)
//	{
//		//List<EventList> eventsList = new ArrayList<EventList>();
//		List<HashMap<String, Integer>> tickets = userDetails.getTickets();
//		for(HashMap<String, Integer> listOfEvents : tickets)
//		{
//			int eventid = listOfEvents.get("eventid");
//			System.out.println(eventid);
//			//create request to get event details
//			reMngr.getEventDetails(eventid, resp);
//			//String jsondata = gson.toJson(reMngr.getEventDetails(eventid, req, resp));
//		}
//	}

	//POST /users/{userid}/tickets/transfer
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
	{
		resp.setStatus(HttpServletResponse.SC_OK);
		String requestURI[] = req.getPathInfo().split("/"); 
		System.out.println(req.getPathInfo());
		//check incorrect url
		if(requestURI.length != 4 || !(requestURI[1].matches("\\d+") && requestURI[3].equals("transfer") && requestURI[2].matches("tickets")))
		{
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		//create request and pass it to api
		 TransferTicketBuffer ticketBuffer = gson.fromJson(reMngr.readRequestBody(req).toString(), TransferTicketBuffer.class);
		URL url = new URL(config.getUserserviceurl() + req.getPathInfo().replaceFirst("/", "")); //to get better uri
		System.out.println(url.toString());
		//pass it to create request and send it further
		reMngr.createTransferRequest(url, ticketBuffer, resp, req);
		if(resp.getStatus() != HttpServletResponse.SC_OK)
		{
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
	}
}