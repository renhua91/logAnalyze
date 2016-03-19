package com.renhua.logAnalyze;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.FilteredQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;

import com.renhua.logAnalyze.bean.ErrorLogStat;

/**
 * @author renhua
 * 程序入口
 * 2016年3月19日
 */
public class App {
	
	private static Log logger = LogFactory.getLog(App.class);
	
	public static void main(String[] args) {
		new App().doJob();
	}
	
    @SuppressWarnings("resource")
	public void doJob() {
    	long beginTime = System.currentTimeMillis();
//    	boolean debug = true;
    	boolean debug = false;
        String host,indices;
        long from,to;
        if (debug) {
			host = "192.168.20.230";
			indices = "logstash-2015.12.24";
			from = 1450929473656L;
			to = 1450959173656L;
		}else{
			host = "127.0.0.1";
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd");
			indices = "logstash-" + sdf.format(new Date());
			to = System.currentTimeMillis();
			from = to - 1000 * 60 * 30;
		}
    	logger.info("indices : " + indices);
    	logger.info("host : " + host);
        Settings settings = ImmutableSettings.settingsBuilder()
        		.put("cluster.name","es_log")
        		.build();
        Client client = new TransportClient(settings)
        		.addTransportAddress(new InetSocketTransportAddress(host, 9300));
        
        //获取错误方法前十名
        List<Bucket> list = getBucketsfromOneSearch("SourceMethodName.raw", 10, client, 
        		indices, from, to);
        List<String> sourceMethodList = new ArrayList<String>();
        //排除了？的方法
        if (list != null && !list.isEmpty()) {
			for (Bucket bucket : list) {
				logger.info(bucket.getKey() + "----" + bucket.getDocCount());
				if (!bucket.getKey().equals("?")) {
					sourceMethodList.add(bucket.getKey());
				}
			}
		}
        //遍历方法列表，对每个方法做单独的分析
        for (String methodName : sourceMethodList) {
			logger.info("methodName : " + methodName);
			
			Map<String, String> fMap = new HashMap<String, String>();
			fMap.put("SourceMethodName", methodName);
			
			//获取单个方法的类前三名（因为有可能不同的类有相同的方法）
			List<Bucket> list1 = getBucketsfromOneSearch(fMap, "SourceClassName", 3, client,
					indices, from, to);
			
			if (list1 != null && !list1.isEmpty()) {
				for (Bucket bucket : list1) {
					String sourceClassName = bucket.getKey();
					fMap.put("SourceClassName", sourceClassName);
					long errorCount = bucket.getDocCount();
					//当这个指定的类-方法报错超过阀值
					if (errorCount > ConfigUtil.ERROR_COUNT) {
						//获取单个方法的一条详细信息
						Map<String, Object> resultMap = getOneResponse(fMap, client, indices, from,
								to);
						//每个方法的服务器排名
						List<Bucket> list2 = getBucketsfromOneSearch(fMap, "source_host.raw", 100, client,
								indices, from, to);
						logger.info("query finish cost : " + (System.currentTimeMillis() - beginTime));
						ErrorLogStat stat = new ErrorLogStat(resultMap).setFrom(from).setTo(to).setServiceCountMap(list2);
						stat.setSuggestions(getSuggestions(stat));
						AnalyzeService analyzeService = new AnalyzeService();
						//如果超过发邮件的阀值
						if (analyzeService.needSendMail(stat)) {
							analyzeService.reportPopularErrorStatInOneFacility(stat);
						}else{
							logger.info(String.format("没有达到发送邮件的标准 methodName : %s, sourceClassName : %s",
									methodName,sourceClassName));
						}
					}
				}
			}
		}
        client.close();
		long endTime = System.currentTimeMillis();
        
        logger.info("finish, cost : " + (endTime - beginTime));
        
    }
    
    public String getSuggestions(ErrorLogStat stat) {
    	if(stat == null) return null;
    	String topServerIp = null;
    	String stackMessage = "没有栈信息";
    	long topServerCount = 0;
    	List<Map.Entry<String, Long>> sortServiceList = stat.getSortServiceList();
    	if (sortServiceList != null && !sortServiceList.isEmpty()) {
    		Map.Entry<String, Long> topServer = sortServiceList.get(0);
    		topServerIp = topServer.getKey();
    		topServerCount = topServer.getValue();
		}
    	if (stat.getStackTrace() != null && !stat.getStackTrace().isEmpty()) {
    		stackMessage = stat.getStackTrace().replaceAll("\\bat\\b", "<br>at");
		}
    	String suggestions = String.format("<h2>Hi %s 我是应用日志管家~ </h2>"
    			+ "<br><br>您的应用 %s 现在出现了一些问题哦!\n"
    			+ "<br><br>时间：%s - %s"
    			+ "<br><br>类名：%s"
    			+ "<br><br>方法名：%s"
    			+ "<br><br>错误信息：%s"
    			+ "<br><br>栈信息：<br>%s"
    			+ "<p>下图是服务器错误-直方统计图，据统计，30分钟内，集群%d台服务器共计发生此错误%d次，"
    			+ "平均每台服务器%d次，其中%s出错数最多，达到了%d次</p>", 
    			ConfigUtil.getAdminNames(stat.getFacilityName()),
    			stat.getFacilityName(), stat.getFromStr(), stat.getToStr(),
    			stat.getClassName(), stat.getMethodName(), stat.getMessage(),
    			stackMessage, stat.getServiceSize(), stat.getServiceCountTotal(),
    			stat.getServiceCountAverage(), topServerIp, topServerCount);
    	return suggestions;
    }
    
    public List<Bucket> getBucketsfromOneSearch(String aggField, int bucketSize,  
    		Client client, String indices, long from, long to) {
    	return getBucketsfromOneSearch(null, aggField, bucketSize, client, indices, from, to);
    }
    
    
    /** 从一次聚合搜索中获取buckets结果集
     * @param fieldMap 搜索参数集
     * @param aggField
     * @param bucketSize
     * @param client
     * @param indices
     * @param from
     * @param to
     * @return
     */
    public List<Bucket> getBucketsfromOneSearch(Map<String, String> fieldMap, String aggField, int bucketSize,  
    		Client client, String indices, long from, long to) {
    	List<Bucket> list = null;
    	FilterBuilder filterBuilder = null;
    	//
    	if (fieldMap != null && !fieldMap.isEmpty()) {
    		BoolFilterBuilder boolFilterBuilder = FilterBuilders.boolFilter()
					.must(FilterBuilders.rangeFilter("@timestamp").from(from).to(to))
					.mustNot(FilterBuilders.queryFilter(QueryBuilders.matchPhrasePrefixQuery("Severity", "INFO")));
			for (Entry<String, String> entry : fieldMap.entrySet()) {
				boolFilterBuilder.must(FilterBuilders.queryFilter(QueryBuilders.matchPhraseQuery(entry.getKey(), entry.getValue())));
			}
			filterBuilder = boolFilterBuilder;
		}else{
			filterBuilder = FilterBuilders.boolFilter()
					.must(FilterBuilders.rangeFilter("@timestamp").from(from).to(to))
					.mustNot(FilterBuilders.queryFilter(QueryBuilders.matchPhrasePrefixQuery("Severity", "INFO")));
		}
    	SearchRequestBuilder srb = client.prepareSearch(indices).setSize(0)
    			.setFetchSource(false).setSearchType(SearchType.COUNT)
        		.setQuery(
        				new FilteredQueryBuilder(
        						QueryBuilders.queryStringQuery("*").analyzeWildcard(true),
        						filterBuilder
        					)
        				)
        		.addAggregation(AggregationBuilders.terms("agg1")
        				.field(aggField).size(bucketSize).order(Terms.Order.count(false)));
        logger.info("srb : " +srb);
        SearchResponse response = srb.execute().actionGet();
        logger.info(response);
        Terms agg1 = response.getAggregations().get("agg1");
        if (agg1 != null) list = agg1.getBuckets();
		return list;
    }
    
    /**
     * 获取某次搜索返回的单个结果
     * @param searchkey
     * @param field
     * @param client
     * @param indices
     * @param from
     * @param to
     * @return
     */
    public Map<String, Object> getOneResponse(Map<String, String> fieldMap, Client client, 
    		String indices, long from, long to) {
    	Map<String, Object> resultMap = null;
    	
    	//默认搜索全部，在某个时间段
    	BoolFilterBuilder boolFilterBuilder = FilterBuilders.boolFilter()
				.must(FilterBuilders.rangeFilter("@timestamp").from(from).to(to));
    	
    	//添加更多的匹配条件
    	if (fieldMap != null && !fieldMap.isEmpty()) {
    		for(Entry<String, String> entry : fieldMap.entrySet())  {
    			boolFilterBuilder.must(FilterBuilders
    					.queryFilter(QueryBuilders.matchPhraseQuery(entry.getKey(), entry.getValue())));
    		}
    	}
    	
    	try {
			SearchResponse response2 = client.prepareSearch(indices).setSize(1)
					.setQuery(
							new FilteredQueryBuilder(
									QueryBuilders.queryStringQuery("*").analyzeWildcard(true),
									boolFilterBuilder
								)
							)
					.execute().actionGet();
			logger.info("response2 : " + response2);
			SearchHit[] searchHits = response2.getHits().getHits();
			resultMap = searchHits[0].getSource();
			logger.info("hits 1 message : " + resultMap.get("message"));
		} catch (ElasticsearchException e) {
			logger.error("",e);
		}
        return resultMap;
    }
}
