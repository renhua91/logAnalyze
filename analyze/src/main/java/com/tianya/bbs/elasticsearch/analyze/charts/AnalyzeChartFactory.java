package com.tianya.bbs.elasticsearch.analyze.charts;

import java.awt.Font;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

public class AnalyzeChartFactory {
	private static final String PATH = AnalyzeChartFactory.class.getClassLoader().getResource("").getPath();
	
	public static void main(String[] args) throws Exception {
//		buildBarChart("tianyaQingWeb", GetDataset(), new Date(), new Date(System.currentTimeMillis() + 60 * 30 * 1000));
		String ip = "19.2.174.70";
		String[] ips = ip.split("\\.");
		System.out.println(ips.length);
		System.out.println("finish");
	}
	
	public static String buildBarChart(String facilityName, CategoryDataset dataset, 
			Date dateFrom, Date dateTo) throws Exception {
		String imageName = facilityName + "_" + "barChart";
		String imagePath = PATH + imageName + ".jpg";
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		String strFrom = sdf.format(dateFrom);
		String strTo = sdf.format(dateTo);
		if (strFrom.substring(0, 10).equals(strTo.substring(0, 10))) {
			strTo = strTo.substring(11, 16);
		}
		String title = facilityName + " errorReport \n " + strFrom + " to " + strTo;
		//创建主题样式
//		StandardChartTheme mChartTheme = new StandardChartTheme("CN");
		//设置图表标题
//		mChartTheme.setExtraLargeFont(new Font("黑体", Font.BOLD, 20));
		//设置轴向字体
//		mChartTheme.setLargeFont(new Font("宋体", Font.PLAIN, 15));
		//设置图例字体
//		mChartTheme.setRegularFont(new Font("宋体", Font.PLAIN, 15));
		//应用主题
//		ChartFactory.setChartTheme(mChartTheme);
		//创建图表
		CategoryDataset mDataset = dataset;
		JFreeChart mChart = ChartFactory.createBarChart3D(title, "server ip", "count",
				mDataset, PlotOrientation.VERTICAL, true, true,true);
		//设置内部属性
		CategoryPlot mPlot = (CategoryPlot)mChart.getPlot();
		//设置纵轴和横轴
		CategoryAxis mDomainAxis = mPlot.getDomainAxis();
		//设置柱状图距离x轴最左端（即y轴）的距离百分比10%
		//mDomainAxis.setLowerMargin(0.1);
		mDomainAxis.setUpperMargin(0.1);
		//柱体显示数值
		BarRenderer mRenderer = new BarRenderer();
		mRenderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());  
        mRenderer.setBaseItemLabelFont(new Font("宋体", Font.PLAIN, 15));  
        mRenderer.setBaseItemLabelsVisible(true);  
		mPlot.setRenderer(mRenderer);
		// 注意 获得绘图区 和 chart 的不同意义
		ChartUtilities.saveChartAsJPEG(new File(imagePath), mChart, 1300, 500);
		return imagePath; 
	}
	
	public static CategoryDataset GetDataset() {
		DefaultCategoryDataset mDataset = new DefaultCategoryDataset();
		String exception = "Nullpoint Exception";
		mDataset.addValue(2000, exception, "174.70");
		mDataset.addValue(1500, exception, "174.71");
		mDataset.addValue(1000, exception, "174.72");
		mDataset.addValue(900, exception, "174.73");
		mDataset.addValue(800, exception, "174.74");
		mDataset.addValue(300, exception, "174.75");
		mDataset.addValue(600, exception, "174.76");
		mDataset.addValue(400, exception, "174.77");
		return mDataset;
	}
	
}
