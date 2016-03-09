package com.tianya.bbs.elasticsearch.analyze.bean;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * @author JackRen
 * 单一条错误日志的统计结果
 */
public class ErrorLogStat {
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	private String SourceMethodName;
	private String SourceClassName;
	private String facility;
	private String exceptionName;
	private String StackTrace;
	private String suggestions;
	private String message;
	private Map<String, Long> servicesCountMap;
	private List<Map.Entry<String,Long>> sortServiceList;
	private Date from;
	private Date to;
	
	public ErrorLogStat(Map<String, Object> resultMap) {
		if (resultMap != null && !resultMap.isEmpty()) {
			this.SourceMethodName = String.valueOf(resultMap.get("SourceMethodName"));
			this.SourceClassName = String.valueOf(resultMap.get("SourceClassName"));
			this.facility = String.valueOf(resultMap.get("facility"));
			this.message = String.valueOf(resultMap.get("message"));
			Object stackTraceObj = resultMap.get("StackTrace");
			if (stackTraceObj != null) {
				this.StackTrace = String.valueOf(stackTraceObj);
			}else {
				this.StackTrace = null;
			}
			this.exceptionName = null;
			if (StackTrace != null && !StackTrace.isEmpty()) {
				int token = StackTrace.indexOf(":");
				if (token > 0) {
					this.exceptionName = StackTrace.substring(0, token);
				} else {
					int length = StackTrace.length();
					if (length > 10) {
						this.exceptionName = StackTrace.substring(0, 10);
					} else {
						this.exceptionName = StackTrace;
					}
				}
			} else {
				this.exceptionName = "";
			} 
		}
	}
	
	public ErrorLogStat(String SourceMethodName, String SourceClassName, String facility,
			String exceptionName, String stackTrace, String suggestions,
			Map<String, Long> servicesCountMap,
			long from, long to) {
		this.SourceMethodName = SourceMethodName;
		this.SourceClassName = SourceClassName;
		this.facility = facility;
		this.exceptionName = exceptionName;
		this.StackTrace = stackTrace;
		this.suggestions = suggestions;
		this.servicesCountMap = servicesCountMap;
		this.from = new Date(from);
		this.to = new Date(to);
	}
	
	public List<Map.Entry<String, Long>> getSortServiceList() {
		if (sortServiceList == null) {
			if (servicesCountMap != null && !servicesCountMap.isEmpty()) {
				sortServiceList = new ArrayList<Map.Entry<String,Long>>(servicesCountMap.entrySet());
		        Collections.sort(sortServiceList,new Comparator<Map.Entry<String,Long>>() {
		            public int compare(Entry<String, Long> o1,
		                    Entry<String, Long> o2) {
		                return o2.getValue().compareTo(o1.getValue());
		            }
		        });
			}
		}
		return sortServiceList;
	}

	public CategoryDataset getDataset() {
		DefaultCategoryDataset mDataset = new DefaultCategoryDataset();
		String description = getExceptionName();  
		if (servicesCountMap != null && !servicesCountMap.isEmpty()) {
			List<Map.Entry<String,Long>> list 
					= new ArrayList<Map.Entry<String,Long>>(servicesCountMap.entrySet());
	        Collections.sort(list,new Comparator<Map.Entry<String,Long>>() {
	            public int compare(Entry<String, Long> o1,
	                    Entry<String, Long> o2) {
	                return o2.getValue().compareTo(o1.getValue());
	            }
	        });
			for (Entry<String, Long> entry : list) {
				mDataset.addValue(entry.getValue(), description, entry.getKey());
			}
		}
		return mDataset;
	}
	public String getMethodName() {
		return SourceMethodName;
	}
	public String getClassName() {
		return SourceClassName;
	}
	public String getFacilityName() {
		return facility;
	}
	public String getExceptionName() {
		return exceptionName;
	}
	public String getStackTrace() {
		return StackTrace;
	}
	public String getSuggestions() {
		return suggestions;
	}
	public String getMessage() {
		return message;
	}
	public Map<String, Long> getServicesCountMap() {
		return servicesCountMap;
	}
	public Date getFrom() {
		return from;
	}
	public Date getTo() {
		return to;
	}
	public int getServiceSize() {
		int count = 0;
		if (servicesCountMap != null && !servicesCountMap.isEmpty()) {
			count = getServicesCountMap().size();
		}
		return count;
	}

	public long getServiceCountTotal() {
		long total = 0;
		if (servicesCountMap != null && !servicesCountMap.isEmpty()) {
			for (long c : servicesCountMap.values()) {
				total += c;
			}
		}
		return total;
	}
	
	public long getServiceCountAverage() {
		long average = 0;
		if (getServiceSize() > 0) {
			average = getServiceCountTotal() / getServiceSize();
		}
		return average;
	}
	public String getFromStr() {
		return sdf.format(getFrom());
	}
	public String getToStr() {
		String toStr = sdf.format(getTo());
		if (getFromStr().substring(0, 10).equals(toStr.substring(0, 10))) {
			toStr = toStr.substring(11, 16);
		}
		return toStr;
	}
	public ErrorLogStat setSuggestions(String suggestions) {
		this.suggestions = suggestions;
		return this;
	}
	public ErrorLogStat setFrom(long from) {
		this.from = new Date(from);
		return this;
	}
	public ErrorLogStat setTo(long to) {
		this.to = new Date(to);
		return this;
	}
	public ErrorLogStat setServiceCountMap(List<Bucket> list) {
		this.servicesCountMap = new HashMap<String, Long>();
        if (list != null && !list.isEmpty()) {
			for (Bucket bucket : list) {
				String ip = bucket.getKey();
				String[] ips = ip.split("\\.");
				String key = null;
				if (ips != null && ips.length > 2) {
					key = ips[2] + "." + ips[3];
				}else{
					key = ip;
				}
				this.servicesCountMap.put(key, bucket.getDocCount());
			}
		}
        return this;
	}
}
