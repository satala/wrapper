import com.qunar.qfwrapper.bean.booking.BookingResult;
import com.qunar.qfwrapper.bean.booking.BookingInfo;
import com.qunar.qfwrapper.bean.search.FlightSearchParam;
import com.qunar.qfwrapper.bean.search.ProcessResultInfo;
import com.qunar.qfwrapper.bean.search.OneWayFlightInfo;
import com.qunar.qfwrapper.bean.search.FlightDetail;
import com.qunar.qfwrapper.bean.search.FlightSegement;
import com.qunar.qfwrapper.interfaces.QunarCrawler;
import com.qunar.qfwrapper.util.QFGetMethod;
import com.qunar.qfwrapper.util.QFHttpClient;
import com.qunar.qfwrapper.constants.Constants;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;

import com.google.common.collect.Lists;

import org.apache.commons.lang.StringUtils;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public class Wrapper_gjdairps001 implements QunarCrawler{

	public static void main(String[] args) {

		FlightSearchParam searchParam = new FlightSearchParam();
		searchParam.setDep("KBP");
		searchParam.setArr("LWO");
		searchParam.setDepDate("2014-06-28");
		searchParam.setTimeOut("60000");
		searchParam.setToken("");
		Wrapper_gjdairps001 p = new Wrapper_gjdairps001();
		String html = p.getHtml(searchParam); 

		ProcessResultInfo result = new ProcessResultInfo();
		result = p.process(html,searchParam);
		if(result.isRet() && result.getStatus().equals(Constants.SUCCESS))
		{
			List<OneWayFlightInfo> flightList = (List<OneWayFlightInfo>) result.getData();
			for (OneWayFlightInfo in : flightList){
				System.out.println("************" + in.getInfo().toString());
				System.out.println("++++++++++++" + in.getDetail().toString());
			}
		}
		else
		{
			System.out.println(result.getStatus());
		}	
		
	}
	
	public BookingResult getBookingInfo(FlightSearchParam arg0) {

		String bookingUrlPre = "http://booking.flyuia.com/en/index.php";
		BookingResult bookingResult = new BookingResult();
		
		BookingInfo bookingInfo = new BookingInfo();
		bookingInfo.setAction(bookingUrlPre);
		bookingInfo.setMethod("get");
		String[] dates = arg0.getDepDate().split("-");
		String depDate = dates[2] + "-" + dates[1] + "-" + dates[0];//形如 05-08-2013
		
		Map<String, String> inputs = new LinkedHashMap<String, String>();
		inputs.put("PRICER_PREF", "FRP");
		inputs.put("next", "1");
		inputs.put("ID_LOCATION", "UA");
		inputs.put("AIRLINES", "PS");
		inputs.put("CABIN_PREF", "");
		inputs.put("S2_WEB_ID", "symphony");
		inputs.put("DEP_0", arg0.getDep());
		inputs.put("ARR_0", arg0.getArr());
		inputs.put("DEP_1", "");
		inputs.put("ARR_1", "");
		inputs.put("JOURNEY_TYPE", "OW");
		inputs.put("ADTCOUNT", "1");
		inputs.put("YTHCOUNT", "0");
		inputs.put("CHDCOUNT", "0");
		inputs.put("YCDCOUNT", "0");
		inputs.put("INFCOUNT", "0");
		inputs.put("FULL_DATE_0", depDate);
		inputs.put("FULL_DATE_1", depDate);
		
		bookingInfo.setInputs(inputs);		
		bookingResult.setData(bookingInfo);
		bookingResult.setRet(true);
		return bookingResult;
		
	}

	public String getHtml(FlightSearchParam arg0) {
		
		String[] dates = arg0.getDepDate().split("-");
		String depDate = dates[2] + "-" + dates[1] + "-" + dates[0];//形如 05-08-2013

		String firstURL = String.format("http://booking.flyuia.com/en/index.php?PRICER_PREF=FRP&next=1&ID_LOCATION=UA&AIRLINES=PS&CABIN_PREF=&S2_WEB_ID=symphony&DEP_0=%s&ARR_0=%s&DEP_1=&ARR_1=&JOURNEY_TYPE=OW&ADTCOUNT=1&YTHCOUNT=0&CHDCOUNT=0&YCDCOUNT=0&INFCOUNT=0&FULL_DATE_0=%s&FULL_DATE_1=%s", arg0.getDep(),arg0.getArr(),depDate,depDate);
		
		GetMethod get = null;

		try { 
			
			QFHttpClient httpClient = new QFHttpClient(arg0, false);
			//第一次GET
			get = new QFGetMethod(firstURL);
			httpClient.executeMethod(get);
			int status = get.getStatusCode();
			if(status != 200)
			{
		    	return "StatusError" + status;
			}
			Header[] headers = get.getResponseHeaders("Set-Cookie");
			Header header;
			String sid = "";
			for(int i=0; i < headers.length; i++)
			{
				header = headers[i];
				sid = StringUtils.substringBetween(header.getValue(), "sid=", ";");
				if(!StringUtils.isEmpty(sid))
				{
					break;
				}
			}

			if(StringUtils.isEmpty(sid)){
				return "StatusError" + "Sid";
			}
			
			//第二次GET
			String secondURL = String.format("http://booking.flyuia.com/en/familyFaresPricer.php?sid=%s", sid);
			get = new QFGetMethod(secondURL);
			httpClient.executeMethod(get);
			status = get.getStatusCode();
			if(status != 200)
			{
		    	return "StatusError" + status;
			}
			
	    	//第三次GET
			String thirdURL = String.format("http://booking.flyuia.com/en/ajaxSectorCalendarOffer.php?sector=0&fareOfferData=0&sid=%s&userFareOfferData=0", sid);
			get = new QFGetMethod(thirdURL);
			httpClient.executeMethod(get);
			status = get.getStatusCode();
			if(status != 200)
			{
			    	return "StatusError" + status;
			}
			String fareId = "";
			String[] dateParam = StringUtils.substringsBetween(get.getResponseBodyAsString(), "<input type=\"radio\" name=\"calendarDay0\" value=\"","\"");
			for(int i = 0; i < dateParam.length; i++)
			{
				if(dateParam[i].contains(arg0.getDepDate()))
				{
					fareId = dateParam[i];
					break;
				}
			}
			
			if(StringUtils.isEmpty(fareId))
			{
				return "StatusError" + "FareId";
			}
			
			//第四次GET
			String fourthURL = String.format("http://booking.flyuia.com/en/ajaxSectorItineraryOffer.php?sector=0&fareId0=%s&sid=%s",URLEncoder.encode(fareId, "UTF-8"), sid);
			get = new QFGetMethod(fourthURL);

			httpClient.executeMethod(get);
			status = get.getStatusCode();
			if(status != 200)
			{
				return "StatusError" + status;
			}
			return get.getResponseBodyAsString();
		} catch (Exception e) {
			e.getStackTrace();
		} finally{
			if(get != null){
				get.releaseConnection();
			}

		}
		return "Exception";
	}


	public ProcessResultInfo process(String arg0, FlightSearchParam arg1) {
		String html = arg0;
		
		ProcessResultInfo result = new ProcessResultInfo();
		if ("Exception".equals(html)) {	
			result.setRet(false);
			result.setStatus(Constants.CONNECTION_FAIL);
			return result;			
		}	
		if (html.startsWith("StatusError")) {
			result.setRet(false);
			result.setStatus(Constants.CONNECTION_FAIL);
			return result;		
		}		
		//需要有明显的提示语句，才能判断是否INVALID_DATE|INVALID_AIRLINE|NO_RESULT
		//无效日期
		if (html.contains("There are no available flights to your destination. Please choose another departure date.")) {
			result.setRet(false);
			result.setStatus(Constants.INVALID_DATE);
			return result;
		}
		if (html.contains("Flights for requested date are not available")) {
			result.setRet(false);
			result.setStatus(Constants.INVALID_DATE);
			return result;	
		}		

		try {
			List<OneWayFlightInfo> flightList = new ArrayList<OneWayFlightInfo>();
			//具体解析逻辑写在 try 里面
			String info = StringUtils.substringBetween(html,"<description>", "</description>");
			List<String> flightsHtml = new ArrayList<String>();
			
			int start = info.indexOf("<table class=\"vnorena\">");
			int length = new String("<table class=\"vnorena\">").length();
			int end = -1;
			while(start > 0)
			{
				end = info.indexOf("<table class=\"vnorena\">", start + length);
				if(end > 0 && end > start)
				{
					String tmpbuf = info.substring(start, end);
					flightsHtml.add(tmpbuf);
				}
				else
				{
					String tmpbuf = info.substring(start);
					flightsHtml.add(tmpbuf);
					break;
				}
				start = end;
				end = -1;
			}

//			List<BaseFlightInfo> flightList = new ArrayList<BaseFlightInfo>();
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			Date date = format.parse(arg1.getDepDate());
			for (String tmp : flightsHtml) {
				
				OneWayFlightInfo flight = new OneWayFlightInfo();
				FlightDetail detail = new FlightDetail();
				List<FlightSegement> fsegs = new ArrayList<FlightSegement>();
				
				detail.setDepcity(arg1.getDep());
				detail.setArrcity(arg1.getArr());

				detail.setDepdate(date);
				detail.setWrapperid(arg1.getWrapperid());
				
				//fightno
				List<String> flightno = Lists.newArrayList();
				String[] codes = StringUtils.substringsBetween(tmp, "<th>Flight number</th><td><b>", "</b>");
				String code="";
				if(codes.length == 0)
				{
					continue;
				}
				for(int j=0; j<codes.length; j++)
				{
						code=codes[j].replace("Â ", "");
						flightno.add(code);
						FlightSegement fseg = new FlightSegement();
						fseg.setFlightno(code);
						fsegs.add(j, fseg);
				}
				detail.setFlightno(flightno);
				
				boolean parse_ok = true;
				String[] depTmpBufs = StringUtils.substringsBetween(tmp, "<th>Departure airport</th><td><strong>", "</td>");
				if(depTmpBufs.length == 0)
				{
					continue;
				}
				
				for(int i=0;i<depTmpBufs.length;i++)
				{
					String depAirport = StringUtils.substringBetween(depTmpBufs[i], "(",")");
					if(StringUtils.isEmpty(depAirport))
					{
						parse_ok=false;
						break;
					}
					fsegs.get(i).setDepairport(depAirport);
				}
				if(!parse_ok)
				{
					continue;
				}
			
				String[] arrTmpBufs = StringUtils.substringsBetween(tmp, "<th>Arrival airport</th><td><strong>", "</td>");
				if(arrTmpBufs.length == 0)
				{
					continue;
				}
				
				for(int i=0;i<arrTmpBufs.length;i++)
				{
					String arrAirport = StringUtils.substringBetween(arrTmpBufs[i], "(",")");
					if(StringUtils.isEmpty(arrAirport))
					{
						parse_ok=false;
						break;
					}
					fsegs.get(i).setArrairport(arrAirport);
				}
				if(!parse_ok)
				{
					continue;
				}
							
				String[] depTimes = StringUtils.substringsBetween(tmp, "<th>Departure time (local)</th><td>", "</td>");
				if(depTimes.length == 0)
				{
					continue;
				}	
				for(int i=0;i<depTimes.length;i++)
				{
					String departureTime = depTimes[i].split(",")[0];
					if(StringUtils.isEmpty(departureTime))
					{
						parse_ok=false;
						break;
					}
					fsegs.get(i).setDeptime(departureTime);
				}
				if(!parse_ok)
				{
					continue;
				}				

				
				String[] arrTimes = StringUtils.substringsBetween(tmp, "<th>Arrival time (local)</th><td>", "</td>");
				if(arrTimes.length == 0)
				{
					continue;
				}
				for(int i=0;i<arrTimes.length;i++)
				{
					String arrivalTime = arrTimes[i].split(",")[0];
					if(StringUtils.isEmpty(arrivalTime))
					{
						parse_ok=false;
						break;
					}
					fsegs.get(i).setArrtime(arrivalTime);
				}
				if(!parse_ok)
				{
					continue;
				}								
				
				
/*				String planeType = StringUtils.substringBetween(tmp, "<th>Aircraft type</th><td>", "</td>");
				flight.planeType = planeType;*/
				
				//promo
				boolean lowIndex=false;
				String priceInfo = StringUtils.substringBetween(tmp, "<td class=\"promo\">", "</td>");
				String price = "";
				if(priceInfo != null && !priceInfo.contains("Sold Out"))
				{
					lowIndex=true;
					price = StringUtils.substringBetween(priceInfo, "<div class=\"\">", "</div>");
					if(StringUtils.isEmpty(price))
					{
						price = StringUtils.substringBetween(priceInfo, "<div class=\"ww_checked\">", "</div>");
					}
				}
				
				//standard
				if(!lowIndex)
				{
					priceInfo = StringUtils.substringBetween(tmp, "<td class=\"standard\">", "</td>");
					if(priceInfo != null && !priceInfo.contains("Sold Out"))
					{
						lowIndex = true;
						price = StringUtils.substringBetween(priceInfo, "<div class=\"\">", "</div>");	
						if(StringUtils.isEmpty(price))
						{
							price = StringUtils.substringBetween(priceInfo, "<div class=\"ww_checked\">", "</div>");
						}
					}
				}
				//plus
				if(!lowIndex)
				{
					priceInfo = StringUtils.substringBetween(tmp, "<td class=\"plus\">", "</td>");
					if(priceInfo != null && !priceInfo.contains("Sold Out"))
					{
						lowIndex = true;
						price = StringUtils.substringBetween(priceInfo, "<div class=\"\">", "</div>");
						if(StringUtils.isEmpty(price))
						{
							price = StringUtils.substringBetween(priceInfo, "<div class=\"ww_checked\">", "</div>");
						}
					}
				}				
				//flexi
				if(!lowIndex)
				{
					priceInfo = StringUtils.substringBetween(tmp, "<td class=\"flexi\">", "</td>");
					if(priceInfo != null && !priceInfo.contains("Sold Out"))
					{
						lowIndex = true;
						price = StringUtils.substringBetween(priceInfo, "<div class=\"\">", "</div>");
						if(StringUtils.isEmpty(price))
						{
							price = StringUtils.substringBetween(priceInfo, "<div class=\"ww_checked\">", "</div>");
						}
					}
				}
				//business
				if(!lowIndex)
				{
					priceInfo = StringUtils.substringBetween(tmp, "<td class=\"business\">", "</td>");
					if(priceInfo != null && !priceInfo.contains("Sold Out"))
					{
						lowIndex = true;
						price = StringUtils.substringBetween(priceInfo, "<div class=\"\">", "</div>");
						if(StringUtils.isEmpty(price))
						{
							price = StringUtils.substringBetween(priceInfo, "<div class=\"ww_checked\">", "</div>");
						}
					}
				}
				String currencyCode = "";
				float retailPrice =  0;
				if(price.length()  >  0)
				{
					String[] item = price.split(" ");
					if(item.length == 2)
					{
						retailPrice = Math.round(Float.parseFloat(item[0].replaceAll(",", "").trim()));
						currencyCode = item[1];
					}
				}
				if(retailPrice==0)
				{
					continue;
				}
				detail.setPrice(retailPrice);		
				detail.setMonetaryunit(currencyCode);
				detail.setTax(0);
				flight.setDetail(detail);
				flight.setInfo(fsegs);
				flightList.add(flight);
			}
			if(flightList.size()==0)
			{
				result.setRet(false);
				result.setStatus(Constants.PARSING_FAIL);
				return result;
			}
			result.setRet(true);
			result.setStatus(Constants.SUCCESS);
			result.setData(flightList);
			return result;

		} catch (Exception e) {
			result.setRet(false);
			result.setStatus(Constants.PARSING_FAIL);
			return result;
		}		
	}
	
}
