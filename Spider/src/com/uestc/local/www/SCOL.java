package com.uestc.local.www;

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

import javax.rmi.CORBA.Tie;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import com.uestc.spider.www.CRUT;

/*
 * 四川在线
 * http://sichuan.scol.com.cn/dwzw/
 * */
public class SCOL {
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
	//图片个数
	private int imageNumber = 1;
	public void getSCOLNews(){
		System.out.println("scol start...");
		DBName = "LOCALNEWS";
		DBTable = "SCOL";
		ENCODE = "utf-8";
		String[] newsTitleLabel = new String[]{"title",""};     //新闻标题标签 t
		String[] newsContentLabel = new String[]{"id" ,"scol_txt"};  //新闻内容标签 
		String[] newsTimeLabel = new String[]{"id","pubtime_baidu"};   //新闻时间 
		String[] newsSourceLabel =new String[]{"id","source_baidu","四川在线"}; //来源
		String[] newsCategroyLabel = new String[]{"id","col3nav"} ; // 属性
		String monthBuf = null;
		String dateBuf = null;
		//计算获取新闻的时间
		if( month < 10){
			downloadTime = year+"0"+month;
			monthBuf = "0" + month;	
		}
		else 
			downloadTime = year+""+month;
		if(date < 10){
			downloadTime += "0" + date;
			dateBuf = "0" + date ;
		}
		else {
			downloadTime += date ;
			dateBuf = "" +date ;
		}
		CRUT crut = new CRUT(DBName ,DBTable);
		//天府新闻 首页链接 
		theme = "http://sichuan.scol.com.cn/dwzw/";
		String theme1 = "http://sichuan.scol.com.cn/dwzw/index_2.html";

		
		//新闻内容links的正则表达式 http://sichuan.scol.com.cn/
		newsContentLinksReg = "/dwzw/"+year+monthBuf+"/[0-9]{8}.html";
		//保存国内新闻主题links
		Queue<String> guoNeiNewsTheme = new LinkedList<String>();
		guoNeiNewsTheme.offer(theme);
		guoNeiNewsTheme.offer(theme1);

		System.out.println("scol: test 1...");
		//获取国内新闻内容links
		Queue<String>guoNeiNewsContent = new LinkedList<String>();
		guoNeiNewsContent = findContentLinks(guoNeiNewsTheme,newsContentLinksReg);
//		System.out.println(guoNeiNewsContent);
		System.out.println("scol: test 2...");
		//获取每个新闻网页的html
		int i = 0;
		if(guoNeiNewsContent == null || guoNeiNewsContent.isEmpty() ){
			crut.destory();
			return ;
		}
		System.out.println("scol: test 3...");
		while(!guoNeiNewsContent.isEmpty()){
			System.out.println("scol: test 4...");
			String url = guoNeiNewsContent.poll();
			if(!crut.query("Url", url)){
				Date date = new Date();
				String html = findContentHtml(url);  //获取新闻的html
				System.out.println("scol: test 5...");
				if(html!=null)
					crut.add(findNewsTitle(html,newsTitleLabel,"_四川新闻_天府要闻_四川在线"), findNewsOriginalTitle(html,newsTitleLabel,"_四川新闻_天府要闻_四川在线"),findNewsOriginalTitle(html,newsTitleLabel,"_四川新闻_天府要闻_四川在线"), findNewsTime(html,newsTimeLabel),findNewsContent(html,newsContentLabel), findNewsSource(html,newsSourceLabel),
							findNewsOriginalSource(html,newsSourceLabel), findNewsCategroy(html,newsCategroyLabel), findNewsOriginalCategroy(html,newsCategroyLabel), url, findNewsImages(html,newsTimeLabel),downloadTime,date);
				System.out.println("scol: test 6...");
			}
//			System.out.println(i);
		}
		crut.destory();
		System.out.println("scol over...");
	}
	
	public Queue<String> findThemeLinks(String themeLink ,String themeLinkReg) {
		
		Queue<String> themelinks = new LinkedList<String>();
		Pattern newsThemeLink = Pattern.compile(themeLinkReg);
		themelinks.offer(themeLink);
		
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
				return null;
			}catch(Exception e){
				return null;
			}
		return themelinks ;
	}

	public Queue<String> findContentLinks(Queue<String> themeLink ,String contentLinkReg) {
		
		Queue<String> contentlinks = new LinkedList<String>(); // 临时征用
		Exception  bufException = null ;
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
							contentlinks.offer("http://sichuan.scol.com.cn/"+n.extractLink());
					}
				}
			}catch(ParserException e){
				bufException = e ;
			}catch(Exception e){
				bufException = e ;
			}finally{
				if(bufException != null)
					return null;
			}		
		}
//		System.out.println(contentlinks);
		return contentlinks;
	}
	
	public String findContentHtml(String url) {
		Exception bufException = null ;
		String html = null;                 //网页html
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
        }finally{
        	httpUrlConnection.disconnect();
        }
//        System.out.println(html);
		return html;
	}
	
	public String HandleHtml(String html, String one) {
		if(html == null )
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
	
	public String HandleHtml(String html, String one, String two) {
		if(html == null )
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
		if(titleBuf!=null&&titleBuf.contains(buf))
			titleBuf = titleBuf.substring(0, titleBuf.indexOf(buf))	;
		
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
		if(titleBuf!=null&&titleBuf.contains(buf)){
			titleBuf = titleBuf.substring(0, titleBuf.indexOf(buf)+buf.length())	;
		}
		return titleBuf;
	}
	public String findNewsContent(String html , String[] label) {
		// TODO Auto-generated method stub
		if(html == null)
			return null;
		String contentBuf;
		if(label[1].equals("")){
			contentBuf = HandleHtml(html,label[0]);
		}else{
			contentBuf = HandleHtml(html,label[0],label[1]);
			if(contentBuf==null||contentBuf.equals("")){
				contentBuf = HandleHtml(html,"class","a2");
			}
		}
		
////		System.out.println(html);
//		if(contentBuf.equals("") || contentBuf==null){
//			
//			if(html.contains("class=\"content14\">")&&html.contains("[编辑")){
//				contentBuf = html.substring(html.indexOf("class=\"content14\">")+18, html.indexOf("[编辑"));
//			}else if(html.contains("class=\"content\">")&&(html.contains("[编辑"))){
//				contentBuf = html.substring(html.indexOf("class=\"content\">")+16, html.indexOf("[编辑"));
//				
//			}
//		}
//		contentBuf = contentBuf.replaceAll("<(.*?)>", "");
		if(contentBuf!=null)
			contentBuf = contentBuf.replaceFirst("\\s+", "");
		return contentBuf;
	}
	//处理图片，使用时间label
	public String findNewsImages(String html , String[] label) {
		if(html == null )
			return null;
		String bufHtml = "";        //辅助
		String imageNameTime  = "";
//		Queue<String> imageUrl = new LinkedList<String>();  //保存获取的图片链接
		bufHtml = html;	
		//获取图片时间，为命名服务
		imageNameTime = findNewsTime(html,label) ;
		if(imageNameTime == null || imageNameTime.equals(""))
			return null;
		//处理存放条图片的文件夹
    	File f = new File("SCOL");
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
		String imageReg = "(http://img.scol.com.cn/[0-9]{6}/[0-9]{2}/[0-9]{1,10}/[0-9]{17,19}.jpg)|(http://wccdaily.scol.com.cn/hxdsb/[0-9]{8}/(.*?).jpg)";
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
					fileBuf = new File("SCOL",imageNameTime+photohour+photominute+photosecond+"000"+imageNumber+"000"+i+imageNameSuffix);
					fo = new FileOutputStream(fileBuf); 
					imageLocation.offer(fileBuf.getPath());
				}else if(imageNumber < 100){
					fileBuf = new File("SCOL",imageNameTime+photohour+photominute+photosecond+"00"+imageNumber+"000"+i+imageNameSuffix);
					fo = new FileOutputStream(fileBuf);
					imageLocation.offer(fileBuf.getPath());
            
				}else if(imageNumber < 1000){
					fileBuf = new File("SCOL",imageNameTime+photohour+photominute+photosecond+"0"+imageNumber+"000"+i+imageNameSuffix);
					fo = new FileOutputStream(fileBuf);
					imageLocation.offer(fileBuf.getPath());
  
				}else{
					fileBuf = new File("SCOL",imageNameTime+photohour+photominute+photosecond+imageNumber+"000"+i+imageNameSuffix);
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
				System.out.println("亲，图片下载失败！！"+e);
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
	public String findNewsTime(String html , String[] label) {
		// TODO Auto-generated method stub
		String timeBuf ="";
		if(label[1].equals("")){
			timeBuf = HandleHtml(html , label[0]);
		}else{
			timeBuf = HandleHtml(html , label[0],label[1]);
		}
		if(timeBuf!=null){
			timeBuf = timeBuf.replaceAll("[^0-9]", "");
			if(timeBuf.length() >= 8)
				timeBuf = timeBuf.substring(0, 8);
			else
				timeBuf = null;
		}
		return timeBuf;
	}
	public String findNewsSource(String html ,String[] label) {
		// TODO Auto-generated method stub
		if(label.length == 3 && (!label[2].equals("")))
			return label[2];
		else
			return null;
	}
	public String findNewsOriginalSource(String html ,String[] label) {
		// TODO Auto-generated method stub
		String sourceBuf;
		if(label[1].equals("")){
			sourceBuf = HandleHtml(html , label[0]);
		}else{
			sourceBuf = HandleHtml(html , label[0],label[1]);
		}
		if(label.length == 3 && (!label[2].equals(""))&&sourceBuf!=null)
			return label[2]+"-"+sourceBuf;
		else
			return sourceBuf;
	}
	public String findNewsCategroy(String html , String[] label) {
		// TODO Auto-generated method stub
		String categroyBuf ="";
		if(label[1].equals("")){
			categroyBuf = HandleHtml(html , label[0]);
		}else{
			categroyBuf = HandleHtml(html , label[0],label[1]);
		}
		if(categroyBuf!=null){	
			categroyBuf = categroyBuf.replaceAll("\\s+", "");
			categroyBuf = categroyBuf.replaceAll(">|(&gt;)", "");
			if(categroyBuf.contains("四川新闻")&&categroyBuf.contains("正文"))
				categroyBuf = categroyBuf.substring(categroyBuf.indexOf("四川新闻")+4, categroyBuf.indexOf("正文"));
			if(categroyBuf.equals(""))
				return "四川在线";
		}
		return categroyBuf;
	}
	public String findNewsOriginalCategroy(String html , String[] label) {
		// TODO Auto-generated method stub
		String categroyBuf ="";
		if(label[1].equals("")){
			categroyBuf = HandleHtml(html , label[0]);
		}else{
			categroyBuf = HandleHtml(html , label[0],label[1]);
		}
		if(categroyBuf!=null){
			categroyBuf = categroyBuf.replaceAll("\\s+", "");
			categroyBuf = categroyBuf.replaceAll("&gt;", "");
		}
		return categroyBuf;
	}
	public static void main(String[] args){
		SCOL test =new SCOL();
		test.getSCOLNews();
	}
}
