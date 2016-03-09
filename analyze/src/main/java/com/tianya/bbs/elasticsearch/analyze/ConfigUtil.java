package com.tianya.bbs.elasticsearch.analyze;

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

import com.tianya.bbs.elasticsearch.analyze.email.SendMail;
import com.tianya.bbs.elasticsearch.analyze.email.Sender;

/**
 * @author JackRen
 * 项目配置相关的基础类
 */
public class ConfigUtil {
	private static final String FILE_NAME = "analyze.xml";
	private static final String FILE_PATH = ConfigUtil.class.getClassLoader().getResource(FILE_NAME).getPath(); 
	private static final String SUBJECT = "logReport for ";
	private static Sender SENDER = null;
	private static ConcurrentMap<String, String> configMap = new ConcurrentHashMap<String, String>();
	public static final long ERROR_COUNT = 1000;
	
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
				throw new RuntimeException("没有配置发件邮箱及相关信息！");
			}
		} catch (DocumentException e) {
			e.printStackTrace();
		}
	}
	
	public static String getAdminNames(String facilityName) {
		if (facilityName != null && !facilityName.isEmpty()) {
			facilityName = facilityName.toLowerCase();
			String adminNames = configMap.get(facilityName);
			if (adminNames != null && !adminNames.isEmpty()) {
				return adminNames;
			}else{
				return "renhua";
			}
		}else {
			return "renhua";
		}
	}
	
	private static String getCCMail() {
		return "wangliang@staff.tianya.cn";
	}
	
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
					return "renhua@staff.tianya.cn";
				} 
			}else{
				return getMail(facilityName.substring(index + 1));
			}
		}else{
			return "renhua@staff.tianya.cn";
		}
	}
	
	public static String getMail(String adminName) {
		return adminName + "@staff.tianya.cn";
	}
	
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
				.append("1、去<a href='http://124.225.214.143/'>日志平台</a>看更详细内容    ")
				.append("2、打开<a href='http://amm.yanfa.tianya.cn/appindex'>amm运维平台</a>查看服务器状态    ")
				.append("3、打开<a href='https://it.tytech.tianya.cn/'>it运维平台重启某台机器</a>")
				.append("<p>此致，敬礼 <br>论坛管家</p>");
		return signature.toString();
	}
	
	public static void main(String[] args) throws Exception {
//		sendMail("tianyaQingWeb", "日志收集系统-汇报邮件-请勿回复", "下面是一个图片","C:/Users/M7011DEY/Pictures/pie.jpg");
		System.out.println(getAdminMailList("post_task#zhuyf"));
		System.out.println("finish");
	}
	
}
