package com.renhua.logAnalyze;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.mail.MessagingException;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.renhua.logAnalyze.email.SendMail;
import com.renhua.logAnalyze.email.Sender;

/**
 * @author renhua
 * 项目配置相关的基础类
 * 2016年3月19日
 */
public class ConfigUtil {
	private static final String FILE_NAME = "analyze.xml";
	private static final String FILE_PATH = ConfigUtil.class.getClassLoader().getResource(FILE_NAME).getPath(); 
	private static final String SUBJECT = "logReport for ";
	private static Sender SENDER = null;
	private static String ADMIN = null;
	private static String ADMIN_EMAIL = null;
	private static String EMAIL_SUFFIX = null;
	private static ConcurrentMap<String, String> configMap = new ConcurrentHashMap<String, String>();
	public static final long ERROR_COUNT = 1000;	//错误阀值
	
	static {
		readXml();
	}
	
	@SuppressWarnings("unchecked")
	private static void readXml(){
		File file = new File(FILE_PATH);
		SAXReader saxReader = new SAXReader();
		try {
			Document document = saxReader.read(file);
			Element project = document.getRootElement();
			Element facilities = project.element("facilities");
			for(Element facility : (List<Element>)facilities.elements("facility")) {
				configMap.put(facility.attributeValue("name"), facility.attributeValue("admins"));
			}
			Element sender = project.element("sender");
			if (sender != null) {
				SENDER = new Sender(sender.attributeValue("address"), sender.attributeValue("password"));
			}else {
				throw new RuntimeException("没有在analyze.xml中配置发件邮箱及相关信息！");
			}
			Element logAdmin = project.element("logAdmin");
			if (logAdmin != null) {
				ADMIN = logAdmin.attributeValue("name");
				ADMIN_EMAIL = logAdmin.attributeValue("email");
			}else {
				throw new RuntimeException("没有在analyze.xml中配置日志系统管理员信息！");
			}
			Element company = project.element("company");
			if (company != null) {
				EMAIL_SUFFIX = company.attributeValue("emailSuffix");
			}else {
				throw new RuntimeException("没有在analyze.xml中配置公司统一邮箱后缀！");
			}
		} catch (DocumentException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 获取某个应用的负责人
	 * @param facilityName
	 * @return
	 */
	public static String getAdminNames(String facilityName) {
		if (facilityName != null && !facilityName.isEmpty()) {
			facilityName = facilityName.toLowerCase();
			String adminNames = configMap.get(facilityName);
			if (adminNames != null && !adminNames.isEmpty()) {
				return adminNames;
			}else{
				return ADMIN;
			}
		}else {
			return ADMIN;
		}
	}
	
	/**
	 * 主管的邮箱（除了发邮件给应用负责人，还抄送给主管一份）
	 * @return
	 */
	private static String getCCMail() {
		return "";
	}
	
	/**
	 * 获取应用负责人的邮箱列表
	 * @param facilityName
	 * @return
	 * @throws MessagingException
	 */
	private static String getAdminMailList(String facilityName) throws MessagingException {
		if (facilityName != null && !facilityName.isEmpty()) {
			facilityName = facilityName.toLowerCase();
			int index = facilityName.indexOf("#");
			if (index < 0) {
				String admins = configMap.get(facilityName);
				if (admins != null && !admins.isEmpty()) {
					String[] s = admins.split(",");
					StringBuffer sb = new StringBuffer();
					for (int i = 0; i < s.length; i++) {
						sb.append(getMail(s[i]));
						if (i != s.length - 1) {
							sb.append(",");
						}
					}
					return sb.toString();
				} else {
					return ADMIN_EMAIL;
				} 
			}else{
				return getMail(facilityName.substring(index + 1));
			}
		}else{
			return ADMIN_EMAIL;
		}
	}
	
	public static String getMail(String adminName) {
		return adminName + "@" + EMAIL_SUFFIX;
	}
	
	/**
	 * 发送邮件的方法
	 * @param facilityName
	 * @param subject
	 * @param textContent
	 * @param picPath
	 * @throws Exception
	 */
	public static void sendMail(String facilityName, String subject, String textContent, 
			String picPath) throws Exception {
		String content = textContent; 
		String recivers = getAdminMailList(facilityName);
		Map<String,String> image = new HashMap<String,String>();
		List<String> list = new ArrayList<String>();
		if (picPath != null && !picPath.isEmpty()) {
			content += "<br/><img src=\"cid:a00000001\"><br/>";
			image.put("a00000001", picPath);
		}
		content += getSignature();
		Map<String,String> map = new HashMap<String,String>();
		map.put("smtp", "smtp." + SENDER.getDomain());
		map.put("protocol", "smtp");
		map.put("username", SENDER.getUsername());
		map.put("password", SENDER.getPassword());
		map.put("from", SENDER.getAddress());
		map.put("to", recivers);
		map.put("cc", getCCMail());
		map.put("subject", SUBJECT + facilityName);
		map.put("body", content);
		SendMail sm = new SendMail(map,list,image);
		sm.send();
	}
	
	public static String getSignature() {
		StringBuffer signature = new StringBuffer()
				.append("<br><br>建议手册：")
				.append("1、****    ")
				.append("2、****    ")
				.append("3、****    ")
				.append("<p>此致，敬礼 <br>应用日志管家</p>");
		return signature.toString();
	}
	
}
