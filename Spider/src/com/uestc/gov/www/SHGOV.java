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
//上海
public class SHGOV implements GOV{

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
	
	public void getSHGOVNews(){
		System.out.println("SH start...");
		DBName = "GOV";
		DBTable = "SHGOV";
		CRUT crut =  new CRUT(DBName,DBTable);
		
		String[] newsTitleLabel = new String[]{"id","ivs_title"};     //新闻标题标签 t
		String[] newsContentLabel = new String[]{"id" ,"maincontent"};  //新闻内容标签 "id","endText"
		String[] newsTimeLabel = new String[]{"id","fbdate"};   //新闻时间"class","ep-time-soure cDGray"  
		String[] newsSourceLabel =new String[]{"class","search_lt","上海人民政府门户网站"}; //（3个参数）新闻来源 同新闻时间"class","ep-time-soure cDGray" 再加上一个"网易新闻-国内新闻"
		String[] newsCategroyLabel = new String[]{"class","search_lt"} ; //
		
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
		
		ENCODE = "gb2312";
		//首页links
		Queue<String> themeLinks = new LinkedList<String>();
		//要闻首页link 
		themeLinks.offer("http://www.shanghai.gov.cn/shanghai/node2314/node2315/node4411/index.html");
		themeLinks.offer("http://www.shanghai.gov.cn/shanghai/node2314/node2315/node18454/index.html");
		themeLinks.offer("http://www.shanghai.gov.cn/shanghai/node2314/node2315/node17239/index.html");
		themeLinks.offer("http://www.shanghai.gov.cn/shanghai/node2314/node2315/node4411/index.html");
		// 内容link 正则http://www.shanghai.gov.cn/shanghai/node2314/node2315/node4411/u21ai967929.html
		newsContentLinksReg = "(http://www.shanghai.gov.cn/shanghai/node[0-9]{4}/node[0-9]{4}/node[0-9]{4,5}/u21ai[0-9]{6}.html)|(http://www.shanghai.gov.cn/shanghai/node[0-9]{4}/node[0-9]{4}/node[0-9]{4,5}/node[0-9]{4,5}/u21ai[0-9]{6}.html)";
	
		//内容links
		Queue<String> contentLinks = new LinkedList<String>();
		contentLinks = getContentLinks(themeLinks,newsContentLinksReg);
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
		System.out.println("SH over...");
		
	}
	
	@Override
	public Queue<String> getThemeLinks(String themeLink, String themeLinkReg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Queue<String> getContentLinks(Queue<String> themeLink,String ContentLinkReg) {
		Queue<String> contentlinks = new LinkedList<String>(); // 临时征用
		Exception bufException = null ;
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
		IOException bufException = null ;
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
		if(contentBuf!=null){
			contentBuf = contentBuf.replaceAll("&ldquo;", "“");
			contentBuf = contentBuf.replaceAll("&rdquo;", "”");
			contentBuf = contentBuf.replaceAll("&lsquo;", "”");
			contentBuf = contentBuf.replaceAll("&rsquo;", "”");
			contentBuf = contentBuf.replaceFirst("\\s+", "");
		}
		return contentBuf;
	}

	@Override
	public String getNewsImages(String html, String[] label) {
	//该网站一张图片都木有 真是好啊。、、、啊啊啊啊啊。、、
		return null;
//		String bufHtml = html;        //辅助
//		String imageNameTime  = "";
//		//获取图片时间，为命名服务
//		imageNameTime = getNewsTime(html,label);
//		//处理存放条图片的文件夹
//    	File f = new File("SHGOV");
//    	if(!f.exists()){
//    		f.mkdir();
//    	}
//    	//保存图片文件的位置信息
//    	Queue<String> imageLocation = new LinkedList<String>();
//    	//图片正则表达式
//		String imageReg = "../../site1/"+imageNameTime+"/(.*?).jpg";
//		Pattern newsImage = Pattern.compile(imageReg);
//		Matcher imageMatcher = newsImage.matcher(bufHtml);
//		//处理图片
//		int i = 1 ;      //本条新闻图片的个数
//		while(imageMatcher.find()){
//			String bufUrl = imageMatcher.group();
//			bufUrl = bufUrl.replaceAll("../../", "http://www.gov.cn/xinwen/");
//			System.out.println(bufUrl);
//			File fileBuf;
////			imageMatcher.group();
//			String imageNameSuffix = bufUrl.substring(bufUrl.lastIndexOf("."), bufUrl.length());  //图片后缀名
//			try{
//				URL uri = new URL(bufUrl);  
//			
//				InputStream in = uri.openStream();
//				FileOutputStream fo;
//				if(imageNumber < 9){
//					fileBuf = new File(".\\CHINAGOV",imageNameTime+"000"+imageNumber+"000"+i+imageNameSuffix);
//					fo = new FileOutputStream(fileBuf); 
//					imageLocation.offer(fileBuf.getAbsolutePath());
//				}else if(imageNumber < 99){
//					fileBuf = new File(".\\CHINAGOV",imageNameTime+"00"+imageNumber+"000"+i+imageNameSuffix);
//					fo = new FileOutputStream(fileBuf);
//					imageLocation.offer(fileBuf.getAbsolutePath());
//            
//				}else if(imageNumber < 999){
//					fileBuf = new File(".\\CHINAGOV",imageNameTime+"0"+imageNumber+"000"+i+imageNameSuffix);
//					fo = new FileOutputStream(fileBuf);
//					imageLocation.offer(fileBuf.getAbsolutePath());
//  
//				}else{
//					fileBuf = new File(".\\CHINAGOV",imageNameTime+imageNumber+"000"+i+imageNameSuffix);
//					fo = new FileOutputStream(fileBuf);
//					imageLocation.offer(fileBuf.getAbsolutePath());
//				}
//            
//				byte[] buf = new byte[1024];  
//				int length = 0;  
////           	 System.out.println("开始下载:" + url);  
//				while ((length = in.read(buf, 0, buf.length)) != -1) {  
//					fo.write(buf, 0, length);  
//				}  
//				in.close();  
//				fo.close();  
////          	  System.out.println(imageName + "下载完成"); 
//			}catch(Exception e){
//				System.out.println("亲，图片下载失败！！");
//				System.out.println("请检查网络是否正常！");
//			}
//			i ++;
//			
//        }  
//		//如果该条新闻没有图片则图片的编号不再增加
//		if(!imageLocation.isEmpty())
//			imageNumber ++;
//		return imageLocation.toString();
	}

	@Override
	public String getNewsTime(String html, String[] label) {
		String timeBuf ="";
		String yearBuf = null;
		String monthBuf = null;
		String dateBuf = null;
		if(label[1].equals("")){
			timeBuf = HandleHtml(html , label[0]);
		}else{
			timeBuf = HandleHtml(html , label[0],label[1]);
		}
		if(timeBuf!=null && timeBuf.contains("年")&&timeBuf.contains("月")&&timeBuf.contains("日")){
			if(timeBuf.contains("(")&&timeBuf.contains("年"))
				yearBuf = timeBuf.substring(timeBuf.indexOf("( ")+2, timeBuf.indexOf("年"));
			if(timeBuf.contains("年")&&timeBuf.contains("月"))
				monthBuf = timeBuf.substring(timeBuf.indexOf("年")+1, timeBuf.indexOf("月"));
			int i = Integer.parseInt(monthBuf);
			if( i < 10){
				monthBuf = "0" + monthBuf;
			}
			if(timeBuf.contains("月")&&timeBuf.contains("日"))
				dateBuf = timeBuf.substring(timeBuf.indexOf("月")+1, timeBuf.indexOf("日"));
			int j = Integer.parseInt(dateBuf);
			if(j < 10){
				dateBuf = "0" + dateBuf;
			}
			
			timeBuf = yearBuf + monthBuf + dateBuf ;
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
		if(sourceBuf!=null){
			sourceBuf = sourceBuf.replaceAll("您所在的位置 :", "");
			sourceBuf = sourceBuf.replaceAll("&gt;&gt;", "");
			sourceBuf = sourceBuf.replaceAll("\\s+", "-") ;
		}
		if(sourceBuf!=null)
			return label[2]+"-"+sourceBuf;
		else {
			return label[2];
		}
	}

	@Override
	public String getNewsCategroy(String html, String[] label) {
		String categroyBuf ="";
		if(label[1].equals("")){
			categroyBuf = HandleHtml(html , label[0]);
		}else{
			categroyBuf = HandleHtml(html , label[0],label[1]);
		}
		if(categroyBuf!=null){
			categroyBuf = categroyBuf.replaceAll("&gt;", "");
			categroyBuf = categroyBuf.replaceAll("您所在的位置 :", "");
//		    categroyBuf = categroyBuf.replaceAll("\\s+", "");
			categroyBuf = categroyBuf.replaceAll("首页","");
		}
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
		if(categroyBuf!=null)
			categroyBuf = categroyBuf.replaceAll("&gt;", "");
//		categroyBuf = categroyBuf.replaceAll("\\s+", "");
		return categroyBuf;
	}

	public static void main(String[] args){
		
		SHGOV test = new SHGOV();
		test.getSHGOVNews();
	}

}
