package com.uestc.sohu.www;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.omg.CORBA.TIMEOUT;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.SessionNotFoundException;

import com.uestc.spider.www.CRUT;

public class SOHUGuoJiComment implements SOHUCOMMENT{
	
	private String DBName ;   //sql name
	private String DBTable ;  // collections name
	private String ENCODE ;   //html encode gb2312	
	//新闻主题links的正则表达式
	private String newsThemeLinksReg ; 
			
	//新闻内容links的正则表达式
	private String newsContentLinksReg ; 
	
	//已经访问过的url
	Vector<String> visitedUrl = new Vector<String>();
	
	//新闻主题link
	private String theme ;
	//时间
	Date bufDate = new Date();
	//downloadTime
	private String downloadTime;
	Calendar today = Calendar.getInstance();
	private int year = today.get(Calendar.YEAR);
	private int month = today.get(Calendar.MONTH)+1;
	private int date = today.get(Calendar.DATE);	
	
	public void getSOHUGuoJiComment(){
		System.out.println("sohuguoji start...");
		DBName = "SOHUCOMMENT";
		DBTable = "GJ";
		ENCODE = "gb2312";
		CRUT crut = new CRUT(DBName ,DBTable);
		//国际新闻 首页链接
		theme = "http://news.sohu.com/guojixinwen.shtml";
		
		//国际新闻中这个不太需要了
		newsThemeLinksReg = "";
		
		//新闻内容links的正则表达式 http://news.sohu.com/20141217/n407032613.shtml
		newsContentLinksReg = "http://news.sohu.com/[0-9]{4}[0-9]{2}[0-9]{2}/n[0-9]{9}.shtml";
		
		//保存国际新闻主题links
		Queue<String> guoJiNewsTheme = new LinkedList<String>();
		guoJiNewsTheme = findThemeLinks(theme,newsThemeLinksReg);
//		System.out.println(guoNeiNewsTheme);
		
		//获取国际新闻内容links
		Queue<String>guoJiNewsContent = new LinkedList<String>();
		guoJiNewsContent = findContentLinks(guoJiNewsTheme,newsContentLinksReg);
		if(guoJiNewsContent==null){
			crut.destory();
			return ;
		}
//		System.out.println(guoNeiNewsContent);
		if( month < 10)
			downloadTime = year+"0"+month;
		else 
			downloadTime = year+""+month;
		if(date < 10)
			downloadTime += "0" + date;
		else 
			downloadTime += date ;
		while(!guoJiNewsContent.isEmpty()){
			String url = guoJiNewsContent.poll();
			if(!crut.query("Url", url)){
				String commenturl = findNewsCommentUrl(url);
//				System.out.println(commenturl);
//				handleNewsComment(commenturl);
				crut.add(url, commenturl, handleNewsComment(commenturl), bufDate);
			}else {
				String commenturl = findNewsCommentUrl(url);
//				System.out.println(commenturl);
//				handleNewsComment(commenturl);
				crut.update(url, commenturl, handleNewsComment(commenturl), bufDate);
			}
		}
		crut.destory();
		System.out.println("sohuguoji over...");
	}
	@Override
	public Queue<String> findThemeLinks(String themeLink, String themeLinkReg) {
		Queue<String> themelinks = new LinkedList<String>();
		String html = findContentHtml(themeLink);
		if(html!=null){
			html = html.replaceAll("\\s+", "");
			String commentReg = "maxPage=(.*?);var";
		
			Pattern newPage = Pattern.compile(commentReg);
		
			Matcher themeMatcher = newPage.matcher(html);
			String mm = "";
			while(themeMatcher.find()){
				mm = themeMatcher.group();
				mm = mm.substring(8, mm.indexOf(";var"));
			}
		
			String s1 = "http://news.sohu.com/guojixinwen_";
			String s2 = ".shtml";
			themelinks.offer(themeLink);
			int number = Integer.parseInt(mm) - 1;
			int number1 = number - 2 ;
			for(int i = number ; i > number1 ; i--){
				themelinks.offer(s1+i+s2);
			}
		}
		return themelinks ;
	}

	@Override
	public Queue<String> findContentLinks(Queue<String> themeLink,String ContentLinkReg) {
		Queue<String> contentlinks = new LinkedList<String>(); // 临时征用
		Exception bufException = null ;
		Pattern newsContent = Pattern.compile(ContentLinkReg);
		while(!themeLink.isEmpty()){
			try {
				Parser parser = new Parser(themeLink.poll());
				parser.setEncoding(ENCODE);
				@SuppressWarnings("serial")
				NodeList nodeList = parser.extractAllNodesThatMatch(new NodeFilter(){
					public boolean accept(Node node)
					{
						if (node instanceof LinkTag)// 标记
							return true;
						return false;
					}
		
				});
			
				for (int i = 0 ; i < nodeList.size(); i++)
				{
			
					LinkTag n = (LinkTag) nodeList.elementAt(i);
					//新闻主题
					Matcher themeMatcher = newsContent.matcher(n.extractLink());
					if(themeMatcher.find()){
						if(!contentlinks.contains(n.extractLink())){
							
							contentlinks.offer(n.extractLink());
						}
					}
				}
			}catch(ParserException e){
				bufException = e ;
			}catch(Exception e){
				bufException = e ;
			}finally{
				if(bufException != null )
					return null;
			}		
		}
		return contentlinks;
	}

	public String findContentHtml(String url){
		
		String html = null;                 //网页html
		Exception bufException = null ;
		HttpURLConnection httpUrlConnection = null ;
	    InputStream inputStream;
	    BufferedReader bufferedReader;
	    
		int state = 0;
		//判断url是否为有效连接
		try{
			httpUrlConnection = (HttpURLConnection) new URL(url).openConnection(); //创建连接
			state = httpUrlConnection.getResponseCode();
			httpUrlConnection.disconnect();
		}catch (MalformedURLException e) {
//          e.printStackTrace();
			System.out.println("该连接"+url+"网络有故障，已经无法正常链接，无法获取新闻");
			bufException = e ;
		} catch (IOException e) {
          // TODO Auto-generated catch block
//          e.printStackTrace();
			System.out.println("该连接"+url+"网络超级慢，已经无法正常链接，无法获取新闻");
			bufException = e;
		}finally{
			if(bufException != null )
				return null;
		}
		if(state != 200 && state != 201){
			return null;
		}
  
        try {
        	httpUrlConnection = (HttpURLConnection) new URL(url).openConnection(); //创建连接
        	httpUrlConnection.setRequestMethod("GET");
        	httpUrlConnection.setConnectTimeout(3000);
			httpUrlConnection.setReadTimeout(1000);
            httpUrlConnection.setUseCaches(true); //使用缓存
            httpUrlConnection.connect();           //建立连接  链接超时处理
        } catch (IOException e) {
        	System.out.println("该链接访问超时...");
        	bufException = e ;
        }finally{
        	if(bufException != null )
        		return null;
        }
  
        try {
            inputStream = httpUrlConnection.getInputStream(); //读取输入流
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream, ENCODE)); 
            String string;
            StringBuffer sb = new StringBuffer();
            while ((string = bufferedReader.readLine()) != null) {
            	sb.append(string);
            	sb.append("\n");
            }
            html = sb.toString();
        } catch (IOException e) {
//            e.printStackTrace();
        }
//        System.out.println(html);
		return html;
	}
	@Override
	public String findNewsCommentUrl(String url) {
		if(url==null)
			return null;
		String commentUrl = url.substring(url.lastIndexOf("n")+1, url.lastIndexOf("."));
		return "http://quan.sohu.com/pinglun/cyqemw6s1/"+commentUrl;
	}

	@SuppressWarnings({ "static-access", "finally" })
	@Override
	public Queue<String> handleNewsComment(String commentUrl) {
		Queue<String> comment = new LinkedList<String>() ;
		Exception bufException = null;
//		System.getProperty("webdriver.chrome.driver");
		System.setProperty("webdriver.firefox.bin", "E:/Firefox/firefox.exe");
//		WebDriver driver = new ChromeDriver();
//		File file = new File("firebug-1.8.1.xpi");
//		FirefoxProfile firefoxProfile = new FirefoxProfile();
//		try {
//			firefoxProfile.addExtension(file);
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		firefoxProfile.setPreference("extensions.firebug.currentVersion", "1.8.1"); // Avoid startup screen
		WebDriver driver = new FirefoxDriver();
		try {
			driver.get(commentUrl);
			new Thread().sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			bufException = e ;
		}catch(TimeoutException e){
			bufException = e ;
			
		}finally{
			if(bufException != null )
				return null;
		}
		  
        WebElement webElement;
        String test = null;
        try{
        	webElement = driver.findElement(By.xpath("//div[@class='topic-changyan']"));  
        	test = webElement.getText();
        }catch(NoSuchElementException e){
        	bufException = e ;
        }catch(SessionNotFoundException e){
        	bufException = e ;
        }catch(TimeoutException e){
        	bufException = e ;
        }finally{
        	
    		driver.close();  
            driver.quit();
            if(bufException!= null )
            	return null;
        } 
        if(test!=null){
        	test = test.replaceAll("\\s+", "");
        	String commentReg = "](.*?)回复分享";
		
        	Pattern newPage = Pattern.compile(commentReg);
		
        	Matcher themeMatcher = newPage.matcher(test);
        	while(themeMatcher.find()){
        		String mm = themeMatcher.group();
        		mm = mm.replaceAll("]|(回复分享)", "");
        		comment.offer(mm+"--"+bufDate);
        	} 
        }
        return comment;
	}

	public static void main(String[] args){
		SOHUGuoJiComment test = new SOHUGuoJiComment();
		test.getSOHUGuoJiComment();
	}
}
