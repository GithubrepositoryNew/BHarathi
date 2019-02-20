package SL_CommonErrorHandling;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CommonErrorHandling_ServiceMaintTimes {
	
	public Map<String, String> getServiceMaintTimesMap() {
		return serviceMaintTimesMap;
	}
	
	public Iterator<String> keyIterator(){
		return serviceMaintTimesMap.keySet().iterator();
	}
	
	private static final HashMap<String, String> serviceMaintTimesMap = new HashMap<String, String>();
	static {
		serviceMaintTimesMap.put("ProcessReturn_MF:1", "Sunday:17:00,Monday:02:00");
		serviceMaintTimesMap.put("ProcessReturn_MF:2", "Monday:21:15,Tuesday:02:00");
		serviceMaintTimesMap.put("ProcessReturn_MF:3", "Tuesday:21:15,Wednesday:02:00");
		serviceMaintTimesMap.put("ProcessReturn_MF:4", "Wednesday:21:15,Thursday:02:00");
		serviceMaintTimesMap.put("ProcessReturn_MF:5", "Thursday:21:15,Friday:02:00");
		serviceMaintTimesMap.put("ProcessReturn_MF:6", "Friday:21:15,Saturday:02:00");
		serviceMaintTimesMap.put("ProcessReturn_MF:7", "Saturday:17:00,Sunday:08:00");
		//		
		serviceMaintTimesMap.put("gen/ReturnService_Application:1", "Sunday:17:00,Monday:02:00");
		serviceMaintTimesMap.put("gen/ReturnService_Application:2", "Monday:21:15,Tuesday:02:00");
		serviceMaintTimesMap.put("gen/ReturnService_Application:3", "Tuesday:21:15,Wednesday:02:00");
		serviceMaintTimesMap.put("gen/ReturnService_Application:4", "Wednesday:21:15,Thursday:02:00");
		serviceMaintTimesMap.put("gen/ReturnService_Application:5", "Thursday:21:15,Friday:02:00");
		serviceMaintTimesMap.put("gen/ReturnService_Application:6", "Friday:21:15,Saturday:02:00");
		serviceMaintTimesMap.put("gen/ReturnService_Application:7", "Saturday:17:00,Sunday:08:00");
		//
		serviceMaintTimesMap.put("gen/ReturnService_IS:1", "Sunday:17:00,Monday:02:00");
		serviceMaintTimesMap.put("gen/ReturnService_IS:2", "Monday:21:15,Tuesday:02:00");
		serviceMaintTimesMap.put("gen/ReturnService_IS:3", "Tuesday:21:15,Wednesday:02:00");
		serviceMaintTimesMap.put("gen/ReturnService_IS:4", "Wednesday:21:15,Thursday:02:00");
		serviceMaintTimesMap.put("gen/ReturnService_IS:5", "Thursday:21:15,Friday:02:00");
		serviceMaintTimesMap.put("gen/ReturnService_IS:6", "Friday:21:15,Saturday:02:00");
		serviceMaintTimesMap.put("gen/ReturnService_IS:7", "Saturday:17:00,Sunday:08:00");
		//
	}
}
