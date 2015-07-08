package com.uestc.gov.www;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import com.uestc.spider.www.CRUT;
//西藏
public class XZGOV implements GOV{

	private String DBName ;   //sql name
	private String DBTable ;  // collections name
	private String ENCODE ;   //html encode gb2312
	
	//新闻主题links的正则表达式
	private String newsThemeLinksReg ; 		
	//新闻内容links的正则表达式
	private String newsContentLinksReg ; 		
	//新闻主题link 保留
	private String theme ;
	//downloadTime
	private String downloadTime;
	Calendar today = Calendar.getInstance();
	private int year = today.get(Calendar.YEAR);
	private int month = today.get(Calendar.MONTH)+1;
	private int date = today.get(Calendar.DATE);	
	//图片计数
	private int imageNumber = 1 ;
	
	public void getXZGOVNews(){
		System.out.println("XZ start...");
		DBName = "GOV";
		DBTable = "XZGOV";
		CRUT crut =  new CRUT(DBName,DBTable);
		
		String[] newsTitleLabel = new String[]{"class","content-title"};     //新闻标题标签 t
		String[] newsContentLabel = new String[]{"id" ,"content-word"};  //新闻内容标签 "id","endText"
		String[] newsTimeLabel = new String[]{"height","22"};   //新闻时间"class","ep-time-soure cDGray"  
		String[] newsSourceLabel =new String[]{"height","22","西藏自治区人民政府网站"}; //（3个参数）新闻来源 同新闻时间"class","ep-time-soure cDGray" 再加上一个"网易新闻-国内新闻"
		String[] newsCategroyLabel = new String[]{"height","36"} ; //
		
		String monthBuf ;
		String dateBuf ;
      //计算获取新闻的时间
  		if( month < 10){
  			downloadTime = year+"0"+month;
  			monthBuf = "0" + month;
  		}else{ 
  			downloadTime = year+""+month;
  			monthBuf = "" + month ;
  		}
  		if(date < 10){
  			downloadTime += "0" + date;
  			dateBuf = "0" + date ;
  		}else{ 
  			downloadTime += date ;
  			dateBuf = "" + date;
  		}
		
		ENCODE = "utf-8";
		//首页links
		Queue<String> themeLinks = new LinkedList<String>();
		//要闻首页link 
		themeLinks.offer("http://www.xizang.gov.cn/index.jhtml");
		// 内容link 正则http://www.xizang.gov.cn:80/tzgg/83220.jhtml
		newsContentLinksReg = "http://www.xizang.gov.cn:80/((tzgg)|(ldhd)|(ldjh)|(bmxxgk)|(zwgkzn)|(zwxw)|(xzshfz)|(xzjjfz))/[0-9]{5,6}.jhtml";
	
		//内容links
		Queue<String> contentLinks = new LinkedList<String>();
		contentLinks = getContentLinks(themeLinks,newsContentLinksReg);
//		int i = 1 ;
		if(contentLinks == null || contentLinks.isEmpty()){
			crut.destory();
			return ;
		}
		while(!contentLinks.isEmpty()){
			String url = contentLinks.poll();
			if(!crut.query("Url", url)){
				Date date = new Date();
				String html = getContentHtml(url);  //获取新闻的html
				if(html!=null)
				    crut.add(getNewsTitle(html,newsTitleLabel,""), getNewsOriginalTitle(html,newsTitleLabel,""),getNewsOriginalTitle(html,newsTitleLabel,""), getNewsTime(html,newsTimeLabel),getNewsContent(html,newsContentLabel), getNewsSource(html,newsSourceLabel),
						getNewsOriginalSource(html,newsSourceLabel), getNewsCategroy(html,newsCategroyLabel), getNewsOriginalCategroy(html,newsCategroyLabel), url, getNewsImages(html,newsTimeLabel),downloadTime,date);
				
			}
		}
//		System.out.println(i);
		crut.destory();
		System.out.println("XZ over...");
		
	}
	
	@Override
	public Queue<String> getThemeLinks(String themeLink, String themeLinkReg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Queue<String> getContentLinks(Queue<String> themeLink,String ContentLinkReg) {
		Queue<String> contentlinks = new LinkedList<String>(); // 临时征用
		Exception bufException  = null ;
		Pattern newsContent = Pattern.compile(ContentLinkReg);
		while(!themeLink.isEmpty()){
			
			String buf = themeLink.poll();
//			System.out.println(buf);
			try {
				Parser parser = new Parser(buf);
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
			
				for (int i = 0; i < nodeList.size(); i++)
				{
			
					LinkTag n = (LinkTag) nodeList.elementAt(i);
//	        	System.out.print(n.getStringText() + "==>> ");
//	       	 	System.out.println(n.extractLink());
					//新闻主题
					Matcher themeMatcher = newsContent.matcher(n.extractLink());
					if(themeMatcher.find()){
					
						if(!contentlinks.contains(n.extractLink()))
							contentlinks.offer(n.extractLink());
					}
				}
			}catch(ParserException e){
//				System.out.println(".1");
				bufException = e ;
			}catch(Exception e){
//				System.out.println(".2");
				bufException = e ;
			}finally{
				if(bufException != null)	
					return null;
			}		
		}
//		System.out.println(contentlinks);
		return contentlinks;
	}

	@Override
	public String getContentHtml(String url) {
		String html = null;                 //网页html
		Exception bufException = null ;
		HttpURLConnection httpUrlConnection = null;
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
			bufException = e ;
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
        	System.out.println(url+"该链接访问超时...");
        	bufException = e ;
        }finally{
        	if(bufException != null)
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
	public String HandleHtml(String html, String one) {
		if(html == null)
			return null;
		NodeFilter filter = new HasAttributeFilter(one);
		String buf = "";
		try{
			Parser parser = Parser.createParser(html, ENCODE);
			NodeList nodes = parser.extractAllNodesThatMatch(filter);
   		
			if(nodes!=null) {
				for (int i = 0; i < nodes.size(); i++) {
					Node textnode1 = (Node) nodes.elementAt(i);
					buf += textnode1.toPlainTextString();
					if(buf.contains("&nbsp;"))
						buf = buf.replaceAll("&nbsp;", " ");
				}
			}
		}catch(Exception e){
		   
		   
		}
		return buf ;
	}

	@Override
	public String HandleHtml(String html, String one, String two) {
		if(html == null)
			return null;
		NodeFilter filter = new HasAttributeFilter(one,two);
		String buf = "";
		try{
			Parser parser = Parser.createParser(html, ENCODE);
			NodeList nodes = parser.extractAllNodesThatMatch(filter);
   		
			if(nodes!=null) {
				for (int i = 0; i < nodes.size(); i++) {
					Node textnode1 = (Node) nodes.elementAt(i);
					buf += textnode1.toPlainTextString();
					if(buf.contains("&nbsp;"))
						buf = buf.replaceAll("&nbsp;", " ");
				}
			}
		}catch(Exception e){
 
		}
		return buf ;
	}

	@Override
	public String getNewsTitle(String html, String[] label, String buf) {
		String titleBuf ;
		if(label[1].equals("")){
			titleBuf = HandleHtml(html,label[0]);
		}else{
			titleBuf = HandleHtml(html,label[0],label[1]);
		}
		
		return titleBuf;
	}

	@Override
	public String getNewsOriginalTitle(String html, String[] label, String buf) {
		String titleBuf ;
		if(label[1].equals("")){
			titleBuf = HandleHtml(html,label[0]);
		}else{
			titleBuf = HandleHtml(html,label[0],label[1]);
		}
		return titleBuf;
	}

	@Override
	public String getNewsContent(String html, String[] label) {
		String contentBuf;
		if(label[1].equals("")){
			contentBuf = HandleHtml(html,label[0]);
		}else{
			contentBuf = HandleHtml(html,label[0],label[1]);
		}
		if(contentBuf == null || contentBuf ==  ""){
			contentBuf = HandleHtml(html,"class","content-word");
		}
		if(contentBuf!=null){
			contentBuf = contentBuf.replaceAll("&ldquo;", "“");
			contentBuf = contentBuf.replaceAll("&rdquo;", "”");
			contentBuf = contentBuf.replaceFirst("\\s+", "");
		}
		return contentBuf;
	}

	@Override
	public String getNewsImages(String html, String[] label) {
	//无图片 幸福。。
		return null ;
	}

	@Override
	public String getNewsTime(String html, String[] label) {
		String timeBuf ="";
		if(label[1].equals("")){
			timeBuf = HandleHtml(html , label[0]);
		}else{
			timeBuf = HandleHtml(html , label[0],label[1]);
		}
		if(timeBuf!=null && timeBuf.contains("发文日期:")){
			timeBuf = timeBuf.substring(timeBuf.indexOf("发文日期:"), timeBuf.length());
			timeBuf = timeBuf.replaceAll("[^0-9]", "");
			if(timeBuf.length() >= 8)
				timeBuf = timeBuf.substring(0, 8);
			else
				timeBuf = null;
		}
		return timeBuf;
	}
	@Override
	public String getNewsSource(String html, String[] label) {
		if(label.length == 3 && (!label[2].equals("")))
			return label[2];
		else
			return null;
	}

	@Override
	public String getNewsOriginalSource(String html, String[] label) {
		String sourceBuf;
		if(label[1].equals("")){
			sourceBuf = HandleHtml(html , label[0]);
		}else{
			sourceBuf = HandleHtml(html , label[0],label[1]);
		}
		if(sourceBuf!=null && sourceBuf.contains("发布机构:  ")&&sourceBuf.contains("发文日期:")){
			sourceBuf = sourceBuf.substring(sourceBuf.indexOf("发布机构:  ")+7, sourceBuf.indexOf("发文日期:"));
			sourceBuf = sourceBuf.replaceAll("&nbsp;", "");
		}
		
		return label[2]+"-"+sourceBuf;
	}

	@Override
	public String getNewsCategroy(String html, String[] label) {
		String categroyBuf ="";
		if(label[1].equals("")){
			categroyBuf = HandleHtml(html , label[0]);
		}else{
			categroyBuf = HandleHtml(html , label[0],label[1]);
		}
		if(categroyBuf==null)
			return null;
		categroyBuf = categroyBuf.replaceAll(" &raquo; ", "-");
		if(categroyBuf.contains("新闻中心"))
			categroyBuf = categroyBuf.substring(categroyBuf.indexOf("新闻中心")+5, categroyBuf.length());
		if(categroyBuf.contains("信息公开"))
			categroyBuf = categroyBuf.substring(categroyBuf.indexOf("信息公开")+5, categroyBuf.length());
		if(categroyBuf.contains("认识西藏"))
			categroyBuf = categroyBuf.substring(categroyBuf.indexOf("认识西藏")+5, categroyBuf.length());
		return categroyBuf;
	}

	@Override
	public String getNewsOriginalCategroy(String html, String[] label) {
		String categroyBuf ="";
		if(label[1].equals("")){
			categroyBuf = HandleHtml(html , label[0]);
		}else{
			categroyBuf = HandleHtml(html , label[0],label[1]);
		}
		if(categroyBuf != null)
			categroyBuf = categroyBuf.replaceAll(" &raquo; ", "-");
		return categroyBuf;
	}

	public static void main(String[] args){
		
		XZGOV test = new XZGOV();
		test.getXZGOVNews();
	}
}
