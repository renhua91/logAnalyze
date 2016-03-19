package com.renhua.logAnalyze;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.renhua.logAnalyze.bean.ErrorLogStat;
import com.renhua.logAnalyze.charts.AnalyzeChartFactory;

/**
 * @author renhua
 * 分析类
 * 2016年3月19日
 */
public class AnalyzeService {
	private static Log logger = LogFactory.getLog(AnalyzeService.class);
	
	public boolean needSendMail(ErrorLogStat stat) {
		boolean needSendMail = false;
		if (stat != null) {
			if (stat.getServiceCountAverage() >= ConfigUtil.ERROR_COUNT) {
				needSendMail = true;
			}
		}
		return needSendMail;
	}
	
	public void reportPopularErrorStatInOneFacility(ErrorLogStat stat) {
		String facilityName = stat.getFacilityName();
		try {
			ConfigUtil.sendMail(facilityName, null, stat.getSuggestions(), 
					AnalyzeChartFactory.buildBarChart(facilityName, stat.getDataset(), 
							stat.getFrom(), stat.getTo()));
		} catch (Exception e) {
			try {
				ConfigUtil.sendMail("analyze", "日志收集系统-reportPopularErrorStatInOneFacility出错", 
						e.getStackTrace().toString(), null);
			} catch (Exception e1) {
				logger.error("发送错误报告邮件给admin出错",e1);
			}
		}
	}
	
	public static void main(String[] args) {
		long from = 1454550473983L;
		long to = 1454552273983L;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		System.out.println(sdf.format(new Date(from)));
		System.out.println(sdf.format(new Date(to)));
	}
	
}
