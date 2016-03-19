package com.renhua.logAnalyze.charts;

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


/**
 * @author renhua
 * 绘制图表工厂类
 * 2016年3月19日
 */
public class AnalyzeChartFactory {
	private static final String PATH = AnalyzeChartFactory.class.getClassLoader().getResource("").getPath();
	
	/**
	 * 绘制一个柱状图
	 * @param facilityName
	 * @param dataset
	 * @param dateFrom
	 * @param dateTo
	 * @throws Exception
	 * @return
	 */
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
	
}
