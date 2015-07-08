package com.uestc.NETEASE.www;

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

public class NETEASEWar implements NETEASE{

	
	private String DBName ;   //sql name
	private String DBTable ;  // collections name
	private String ENCODE ;   //html encode gb2312
	
	//新闻主题links的正则表达式
	private String newsThemeLinksReg ; //= "http://news.163.com/special/0001124J/guoneinews_[0-9]{1,2}.html#headList";
			
	//新闻内容links的正则表达式
	private String newsContentLinksReg ; //= "http://news.163.com/[0-9]{2}/[0-9]{4}/[0-9]{2}/(.*?).html#f=dlist";
		
	//新闻主题link
	private String theme ;
	//downloadTime
	private String downloadTime;
	Calendar today = Calendar.getInstance();
	private int year = today.get(Calendar.YEAR);
	private int month = today.get(Calendar.MONTH)+1;
	private int date = today.get(Calendar.DATE);	
	//图片计数
	private int imageNumber = 1 ;
	public NETEASEWar(){
	}
	
	public void getNETEASEWarNews(){
		System.out.println("网易新闻军事开始运行...");
		/*初始化各个标签等 编码 gb2312
		 * 
		 * */
		ENCODE = "GB2312";
		DBName = "NETEASE";   //数据库名称
		DBTable = "war";   //表名
		String[] newsTitleLabel = new String[]{"title",""};     //新闻标题标签 title or id=h1title
		String[] newsContentLabel = new String[]{"id" ,"endText"};  //新闻内容标签 "id","endText"
		String[] newsTimeLabel = new String[]{"class","ep-time-soure cDGray"};   //新闻时间"class","ep-time-soure cDGray"  
		String[] newsSourceLabel =new String[]{"class","ep-time-soure cDGray","网易新闻-军事新闻"}; //（3个参数）新闻来源 同新闻时间"class","ep-time-soure cDGray" 再加上一个"网易新闻-国内新闻"
		String[] newsCategroyLabel = new String[]{"class","ep-crumb JS_NTES_LOG_FE"} ; // "国内" "网易新闻-国内新闻-http://news.163.com/domestic/"
		CRUT crut = new CRUT(DBName,DBTable);
		//计算获取新闻的时间
		if( month < 10)
			downloadTime = year+"0"+month;
		else 
			downloadTime = year+""+month;
		if(date < 10)
			downloadTime += "0" + date;
		else 
			downloadTime += date ;
		/*这个模块分三部分抓取
		 * 1、主页"http://war.163.com/index.html"
		 * 2.详细分类一共10叶：http://war.163.com/special/millatestnews/ http://war.163.com/special/millatestnews_06/
		 * 3.军事历史：http://war.163.com/special/historyread/
		 * */
		//主题
		Queue<String> warNewsThemeLinks = new LinkedList<String>();
		//内容
		Queue<String> warNewsContentLinks = new LinkedList<String>();
		
		//处理主页
		theme = "http://war.163.com/index.html";
		//新闻内容的正则表达式http://war.163.com/14/1124/09/ABQAT7EM00011MTO.html
		newsContentLinksReg = "http://war.163.com/[0-9]{2}/[0-9]{4}/[0-9]{2}/(.*?).html";
		warNewsThemeLinks.offer(theme);
		warNewsContentLinks = findContentLinks(warNewsThemeLinks,newsContentLinksReg);
		if(warNewsContentLinks != null){
			while(!warNewsContentLinks.isEmpty()){
				String url = warNewsContentLinks.poll();
				if(!crut.query("Url", url)){
					Date date = new Date();
					String html = findContentHtml(url);  //获取新闻的html
//				System.out.println(url);
//				System.out.println("download:"+downloadTime);
//				System.out.println(findNewsTime(html,newsTimeLabel));
					if(html!=null)
						crut.add(findNewsTitle(html,newsTitleLabel,"_网易新闻中心"), findNewsOriginalTitle(html,newsTitleLabel,"_网易新闻中心"),findNewsOriginalTitle(html,newsTitleLabel,"_网易新闻中心"), findNewsTime(html,newsTimeLabel),findNewsContent(html,newsContentLabel), findNewsSource(html,newsSourceLabel),
							findNewsOriginalSource(html,newsSourceLabel), findNewsCategroy(html,newsCategroyLabel), findNewsOriginalCategroy(html,newsCategroyLabel), url, findNewsImages(html,newsTimeLabel),downloadTime,date);
				
				}
			}
		}
		
		//详细分类模块
		theme  = "http://war.163.com/special/millatestnews/";
//		newsThemeLinksReg = "http://war.163.com/special/millatestnews(_[0-9]{2})*/";  //主题正则表达式
		newsContentLinksReg = "http://war.163.com/[0-9]{2}/[0-9]{4}/[0-9]{2}/(.*?).html"; //内容正则表达式
//		warNewsThemeLinks = findThemeLinks(theme,newsThemeLinksReg);
		warNewsThemeLinks.offer(theme);
		warNewsContentLinks = findContentLinks(warNewsThemeLinks,newsContentLinksReg);
//		int k = 1;
		if(warNewsContentLinks != null){
			while(!warNewsContentLinks.isEmpty()){
				String url = warNewsContentLinks.poll();
				if(!crut.query("Url", url)){
					Date date = new Date();
					String html = findContentHtml(url);  //获取新闻的html
//					System.out.println(url);
//					System.out.println("download:"+downloadTime);
//					System.out.println(findNewsTime(html,newsTimeLabel));
//					System.out.println(findNewsComment(url,html,newsCategroyLabel));
				
					crut.add(findNewsTitle(html,newsTitleLabel,"_网易军事"), findNewsOriginalTitle(html,newsTitleLabel,"_网易军事"),findNewsOriginalTitle(html,newsTitleLabel,"_网易军事"), findNewsTime(html,newsTimeLabel),findNewsContent(html,newsContentLabel), findNewsSource(html,newsSourceLabel),
							findNewsOriginalSource(html,newsSourceLabel), findNewsCategroy(html,newsCategroyLabel), findNewsOriginalCategroy(html,newsCategroyLabel), url, findNewsImages(html,newsTimeLabel),downloadTime,date);
				
				}
			}
		}
//		System.out.println(k);
		
//		//军事历史
//		theme = "http://war.163.com/special/historyread/";
////		newsThemeLinksReg = "http://war.163.com/special/historyread(_[0-9]{2})*/";  //主题正则表达式
//		newsContentLinksReg = "http://war.163.com/[0-9]{2}/[0-9]{4}/[0-9]{2}/(.*?).html"; //内容正则表达式
//		warNewsThemeLinks.offer(theme);
//		warNewsContentLinks = findContentLinks(warNewsThemeLinks,newsContentLinksReg);
////		int j = 1 ;
//		if(warNewsContentLinks == null)
//			return ;
//		while(!warNewsContentLinks.isEmpty()){
//			String url = warNewsContentLinks.poll();
//			
//			if(!crut.query("Url", url)){
//				Date date = new Date();
//				String html = findContentHtml(url);  //获取新闻的html
//				System.out.println(url);
//				System.out.println("download:"+downloadTime);
//				System.out.println(findNewsTime(html,newsTimeLabel));
//				if(findNewsTime(html,newsTimeLabel)!= null && findNewsTime(html,newsTimeLabel) == downloadTime){
//					crut.add(findNewsTitle(html,newsTitleLabel,"_网易军事"), findNewsOriginalTitle(html,newsTitleLabel,"_网易军事"),findNewsOriginalTitle(html,newsTitleLabel,"_网易军事"), findNewsTime(html,newsTimeLabel),findNewsContent(html,newsContentLabel), findNewsSource(html,newsSourceLabel),
//							findNewsOriginalSource(html,newsSourceLabel), findNewsCategroy(html,newsCategroyLabel), findNewsOriginalCategroy(html,newsCategroyLabel), url, findNewsImages(html,newsTimeLabel),downloadTime,date);
//				}
//			}
//		}
		crut.destory();
		imageNumber = 1 ;
		System.out.println("网易新闻军事结束...");
	}
	@Override
	public Queue<String> findThemeLinks(String themeLink ,String themeLinkReg) {
		
		// TODO Auto-generated method stub
		Queue<String> themelinks = new LinkedList<String>();
		Pattern newsThemeLink = Pattern.compile(themeLinkReg);
		themelinks.offer(themeLink);
		Exception bufException = null;
		try {
				Parser parser = new Parser(themeLink);
				parser.setEncoding(ENCODE);
				@SuppressWarnings("serial")
				NodeList nodeList = parser.extractAllNodesThatMatch(new NodeFilter(){
					public boolean accept(Node node)
					{
						if (node instanceof LinkTag)// 标记
							return true;
						return false;
					}});
				
				for (int i = 0; i < nodeList.size(); i++)
				{
				
					LinkTag n = (LinkTag) nodeList.elementAt(i);
//		        	System.out.print(n.getStringText() + "==>> ");
//		       	 	System.out.println(n.extractLink());
					//新闻主题
					Matcher themeMatcher = newsThemeLink.matcher(n.extractLink());
					if(themeMatcher.find()){
						if(!themelinks.contains(n.extractLink()))
							themelinks.offer(n.extractLink());
		        	}
				}
			}catch(ParserException e){
				bufException = e ;
			}catch(Exception e){
				bufException = e ;
			}finally{
				if(bufException!=null){
					return null;
				}
			}
		return themelinks ;
	}

	public Queue<String> findContentLinks(Queue<String> themeLink ,String contentLinkReg) {
		// TODO Auto-generated method stub
		Queue<String> contentlinks = new LinkedList<String>(); // 临时征用
		Exception bufeException = null;
		Pattern newsContent = Pattern.compile(contentLinkReg);
		while(!themeLink.isEmpty()){
			
			String buf = themeLink.poll();
		
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
				bufeException = e ;
			}catch(Exception e){
				bufeException = e ;
			}finally{
				if(bufeException!= null){
					return null;
				}
			}		
		}
//		System.out.println(contentlinks);
		return contentlinks;
	}
	
	@Override
	public String findContentHtml(String url) {
		// TODO Auto-generated method stub
		String html = null;                 //网页html
		
		HttpURLConnection httpUrlConnection = null;
	    InputStream inputStream;
	    BufferedReader bufferedReader;
	    Exception bufeException = null;
		int state = 0;
		//判断url是否为有效连接
		try{
			httpUrlConnection = (HttpURLConnection) new URL(url).openConnection(); //创建连接
			state = httpUrlConnection.getResponseCode();
			httpUrlConnection.disconnect();
		}catch (MalformedURLException e) {
//          e.printStackTrace();
			System.out.println("该连接"+url+"网络有故障，已经无法正常链接，无法获取新闻");
			bufeException = e ;
		} catch (IOException e) {
          // TODO Auto-generated catch block
//          e.printStackTrace();
			System.out.println("该连接"+url+"网络超级慢，已经无法正常链接，无法获取新闻");
			bufeException = e ;
      }finally{
    	  if(bufeException!=null)
    		  return null;
      }
		if(state != 200 && state != 201){
			return null;
		}
		Exception bufeException1 = null;
        try {
        	httpUrlConnection = (HttpURLConnection) new URL(url).openConnection(); //创建连接
        	httpUrlConnection.setRequestMethod("GET");
        	httpUrlConnection.setConnectTimeout(3000);
			httpUrlConnection.setReadTimeout(1000);
            httpUrlConnection.setUseCaches(true); //使用缓存
            httpUrlConnection.connect();           //建立连接  链接超时处理
        } catch (IOException e) {
        	System.out.println("该链接访问超时...");
        	bufeException1 = e ;
        }finally{
        	if(bufeException1 != null)
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
		if(html == null )
			return null;
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
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
	//news title
	public String findNewsTitle(String html , String[] label,String buf) {
		String titleBuf ;
		if(label[1].equals("")){
			titleBuf = HandleHtml(html,label[0]);
		}else{
			titleBuf = HandleHtml(html,label[0],label[1]);
		}
		if(titleBuf!=null){
			if(titleBuf.contains(buf)){
				titleBuf = titleBuf.substring(0, titleBuf.indexOf(buf))	;
			}else if (titleBuf.contains("_网易军事")){
				titleBuf = titleBuf.substring(0, titleBuf.indexOf("_网易军事"))	;
			}else if(titleBuf.contains("_精兵堂")){
				titleBuf = titleBuf.substring(0, titleBuf.indexOf("_精兵堂"))	;
			}else;
		}
		return titleBuf;
	}
	//news 未处理标题
	public String findNewsOriginalTitle(String html , String[] label,String buf) {
		// TODO Auto-generated method stub
		String titleBuf ;
		if(label[1].equals("")){
			titleBuf = HandleHtml(html,label[0]);
		}else{
			titleBuf = HandleHtml(html,label[0],label[1]);
		}
		if(titleBuf!=null){
			if(titleBuf.contains(buf)){
				titleBuf = titleBuf.substring(0, titleBuf.indexOf(buf)+buf.length())	;
			}else if (titleBuf.contains("_网易军事")){
				titleBuf = titleBuf.substring(0, titleBuf.indexOf("_网易军事")+5)	;
			}else if(titleBuf.contains("_精兵堂")){
				titleBuf = titleBuf.substring(0, titleBuf.indexOf("_精兵堂")+4)	;
			}else;
		}
		return titleBuf;
	}
	@Override
	public String findNewsContent(String html , String[] label) {
		// TODO Auto-generated method stub
		String contentBuf;
		if(label[1].equals("")){
			contentBuf = HandleHtml(html,label[0]);
		}else{
			contentBuf = HandleHtml(html,label[0],label[1]);
		}
		if(contentBuf==null||contentBuf.equals("")){
			contentBuf = HandleHtml(html ,"class","feed-text");
//			System.out.println(contentBuf);
		}
		if(contentBuf!=null){
			if(contentBuf.contains("(NTES);")){
				contentBuf = contentBuf.substring(contentBuf.indexOf("(NTES);")+7, contentBuf.length());
			}
			contentBuf = contentBuf.replaceAll("\\s+", "\n");
			contentBuf = contentBuf.replaceFirst("\\s+", "");
		}
		return contentBuf;
	}
	@SuppressWarnings("unused")
	@Override
	public String findNewsImages(String html , String[] label) {
		// TODO Auto-generated method stub
		if(html==null)
			return null;
		String bufHtml = "";        //辅助
		String imageNameTime  = findNewsTime(html,label);
		if(imageNameTime==null)
			return null;
//		Queue<String> imageUrl = new LinkedList<String>();  //保存获取的图片链接
		if(html.contains("<div id=\"endText\"")&&html.contains("<!-- 分页 -->"))
			bufHtml = html.substring(html.indexOf("<div id=\"endText\""), html.indexOf("<!-- 分页 -->"));
		else if(html.contains("往期推荐")&&html.contains("<div id=\"endText\"")){ 
			
			bufHtml = html.substring(html.indexOf("<div id=\"endText\""), html.indexOf("往期推荐"));
			
			
		}else{
//			System.out.println("内容为空！！");
			return null;
		}
		//获取图片时间，为命名服务
		if(imageNameTime.length() >= 8){
				imageNameTime = imageNameTime ;
		
		}else{
			Calendar now = Calendar.getInstance();
			int year = now.get(Calendar.YEAR);
			int month = now.get(Calendar.MONTH)+1;
			int date = now.get(Calendar.DATE);
			imageNameTime += "" + year;
			if(month < 10){
				imageNameTime += "0"+month;
			}else
				imageNameTime += month ;
			if(date < 10){
				imageNameTime += "0"+date;
			}else
				imageNameTime += date;
		}
		//处理存放条图片的文件夹
    	File f = new File("NETEASEWar");
    	if(!f.exists()){
    		f.mkdir();
    	}
    	//加入具体时间 时分秒 防止图片命名重复
    	Calendar photoTime = Calendar.getInstance();
    	int photohour = photoTime.get(Calendar.HOUR_OF_DAY); 
    	int photominute = photoTime.get(Calendar.MINUTE);
    	int photosecond = photoTime.get(Calendar.SECOND); 
    	//保存图片文件的位置信息
    	Queue<String> imageLocation = new LinkedList<String>();
    	//图片正则表达式
		String imageReg = "(http://img[0-9]{1}.cache.netease.com/cnews/[0-9]{4}/[0-9]{1,2}/[0-9]{1,2}/(.*?).((jpg)|(png)|(jpeg)))|(http://img[0-9]{1}.cache.netease.com/catchpic/(.*?)/(.*?)/(.*?).((jpg)|(png)|(jpeg)))";
		Pattern newsImage = Pattern.compile(imageReg);
		Matcher imageMatcher = newsImage.matcher(bufHtml);
		//处理图片
		int i = 1 ;      //本条新闻图片的个数
		while(imageMatcher.find()){
			String bufUrl = imageMatcher.group();
//			System.out.println(bufUrl);
			File fileBuf;
//			imageMatcher.group();
			String imageNameSuffix = bufUrl.substring(bufUrl.lastIndexOf("."), bufUrl.length());  //图片后缀名
			try{
				URL uri = new URL(bufUrl);  
			
				InputStream in = uri.openStream();
				FileOutputStream fo;
				if(imageNumber < 10){
					fileBuf = new File("NETEASEWar",imageNameTime+photohour+photominute+photosecond+"000"+imageNumber+"000"+i+imageNameSuffix);
					fo = new FileOutputStream(fileBuf); 
					imageLocation.offer(fileBuf.getPath());
				}else if(imageNumber < 100){
					fileBuf = new File("NETEASEWar",imageNameTime+photohour+photominute+photosecond+"00"+imageNumber+"000"+i+imageNameSuffix);
					fo = new FileOutputStream(fileBuf);
					imageLocation.offer(fileBuf.getPath());
            
				}else if(imageNumber < 1000){
					fileBuf = new File("NETEASEWar",imageNameTime+photohour+photominute+photosecond+"0"+imageNumber+"000"+i+imageNameSuffix);
					fo = new FileOutputStream(fileBuf);
					imageLocation.offer(fileBuf.getPath());
  
				}else{
					fileBuf = new File("NETEASEWar",imageNameTime+photohour+photominute+photosecond+imageNumber+"000"+i+imageNameSuffix);
					fo = new FileOutputStream(fileBuf);
					imageLocation.offer(fileBuf.getPath());
				}
            
				byte[] buf = new byte[1024];  
				int length = 0;  
//           	 System.out.println("开始下载:" + url);  
				while ((length = in.read(buf, 0, buf.length)) != -1) {  
					fo.write(buf, 0, length);  
				}  
				in.close();  
				fo.close();  
//          	  System.out.println(imageName + "下载完成"); 
			}catch(Exception e){
				System.out.println("亲，图片下载失败！！");
				System.out.println("请检查网络是否正常！");
			}
			i ++;
			
        }  
		//如果该条新闻没有图片则图片的编号不再增加
		if(!imageLocation.isEmpty())
			imageNumber ++;
		return imageLocation.toString();
	}
	//新闻时间
	@Override
	public String findNewsTime(String html , String[] label) {
		// TODO Auto-generated method stub
		String timeBuf = null;
		if(label[1].equals("")){
			timeBuf = HandleHtml(html , label[0]);
		}else{
			timeBuf = HandleHtml(html , label[0],label[1]);
		}
//		System.out.println(timeBuf+"lllllllllll");
		if(timeBuf == null||timeBuf.equals("")){
			timeBuf = HandleHtml(html,"class","ep-info cDGray");
//			return timeBuf;
		}else if(label[0].equals("style")&&label[1].equals("float:left;")){
			timeBuf = timeBuf.substring(0,19);
		}else
			timeBuf = timeBuf.substring(9, 28);  //根据不同新闻 不同处理
		if(timeBuf!=null){
			timeBuf = timeBuf.replaceAll("[^0-9]", "");
			if(timeBuf.length() >= 8)
				timeBuf = timeBuf.substring(0, 8);
			else
				timeBuf = null;
		}
		if(timeBuf == null ){
			timeBuf = downloadTime ;
		}
		return timeBuf;
	}
	@Override
	public String findNewsSource(String html ,String[] label) {
		// TODO Auto-generated method stub
		if(label.length == 3 && (!label[2].equals("")))
			return label[2];
		else
			return null;
	}
	@Override
	public String findNewsOriginalSource(String html ,String[] label) {
		// TODO Auto-generated method stub
		String sourceBuf;
		if(label[1].equals("")){
			sourceBuf = HandleHtml(html , label[0]);
		}else{
			sourceBuf = HandleHtml(html , label[0],label[1]);
		}
		if(sourceBuf==null||sourceBuf.equals("")){
			sourceBuf = HandleHtml(html,"class","ep-info cDGray");
		}
		if(sourceBuf!=null){
			if(sourceBuf.contains("来源: ")){
				sourceBuf = sourceBuf.substring(sourceBuf.indexOf("来源: ")+4, sourceBuf.length());
			}
			if(sourceBuf.length() >29)
				sourceBuf = sourceBuf.substring(29, sourceBuf.length());  //根据不同新闻 不同处理
		}
		if(label.length == 3 && (!label[2].equals("")))
			return label[2]+"-"+sourceBuf;
		else
			return sourceBuf;
	}
	@Override
	public String findNewsCategroy(String html , String[] label) {
		// TODO Auto-generated method stub
		String categroyBuf ="";
		if(label[1].equals("")){
			categroyBuf = HandleHtml(html , label[0]);
		}else{
			categroyBuf = HandleHtml(html , label[0],label[1]);
		}
		if(categroyBuf!=null&&categroyBuf.contains("&gt;")){
			categroyBuf = categroyBuf.replaceAll("&gt;", "");
			categroyBuf = categroyBuf.replaceAll("\\s+", "");
			if(categroyBuf.contains("新闻中心")){
				categroyBuf = categroyBuf.substring(categroyBuf.indexOf("新闻中心")+4, categroyBuf.indexOf("正文"));
			}else if(categroyBuf.contains("新闻频道")){
				categroyBuf = categroyBuf.substring(categroyBuf.indexOf("新闻频道")+4, categroyBuf.length());
			}else if(categroyBuf.contains("网易首页")){
				categroyBuf = categroyBuf.substring(categroyBuf.indexOf("网易首页")+4, categroyBuf.indexOf("正文"));
			}else;
			
			
		}
		return categroyBuf;
	}
	@Override
	public String findNewsOriginalCategroy(String html , String[] label) {
		// TODO Auto-generated method stub
		String categroyBuf ="";
		if(label[1].equals("")){
			categroyBuf = HandleHtml(html , label[0]);
		}else{
			categroyBuf = HandleHtml(html , label[0],label[1]);
		}
		if(categroyBuf!=null&&categroyBuf.contains("&gt;")){
			categroyBuf = categroyBuf.replaceAll("&gt;", "");
		}
		return categroyBuf;
	}
	public static void main(String[] args){
		long start = System.currentTimeMillis();
		NETEASEWar test = new NETEASEWar();
		test.getNETEASEWarNews();
		long end = System.currentTimeMillis();
		System.out.println(end-start);
	}
}
