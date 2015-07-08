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
import java.util.Vector;
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
//四川
public class SCGOV implements GOV{


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
	
	public void getSCGOVNews(){
		System.out.println("SC start...");
		DBName = "GOV";
		DBTable = "SCGOV";
		CRUT crut =  new CRUT(DBName,DBTable);
		
		String[] newsTitleLabel = new String[]{"title",""};     //新闻标题标签 t
		String[] newsContentLabel = new String[]{"id" ,"cmsArticleContent"};  //新闻内容标签 "id","endText"
		String[] newsTimeLabel = new String[]{"",""};   //新闻时间 暂时找不到。。
		String[] newsSourceLabel =new String[]{"id","articleattribute","四川省人民政府网站"}; //（3个参数）新闻来源 同新闻时间"class","ep-time-soure cDGray" 再加上一个"网易新闻-国内新闻"
		String[] newsCategroyLabel = new String[]{"id","articleattribute"} ; //
		
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
		//新闻聚焦首页link 
		themeLinks.offer("http://www.sc.gov.cn/10462/10464/10797/index.shtml");
		//文件发布首页 link
		themeLinks.offer("http://www.sc.gov.cn/10462/10883/11066/2015/1/3/10322456.shtml");
		// 内容link 正则http://www.sc.gov.cn/10462/10883/11066/2015/1/3/10322456.shtml
		newsContentLinksReg = "http://www.sc.gov.cn/[0-9]{5}/[0-9]{5}/[0-9]{5}/"+year+"/"+month+"/"+date+"/"+"[0-9]{8}.shtml";
	
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
		System.out.println("SC over...");
		
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
			if(bufException != null)
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
						buf = buf.replaceAll("&nbsp;", "\n");
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
						buf = buf.replaceAll("&nbsp;", "\n");
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
		
		if(titleBuf!=null && titleBuf.contains(" - 四川省人民政府网站")){
			titleBuf = titleBuf.substring(0, titleBuf.indexOf(" - 四川省人民政府网站")) ;
			
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
		if(titleBuf!= null && titleBuf.contains(" - 四川省人民政府网站")){
			titleBuf = titleBuf.substring(0, titleBuf.indexOf(" - 四川省人民政府网站")+12) ;	
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
			contentBuf = contentBuf.replaceAll("&#160;", "");
			contentBuf = contentBuf.replaceFirst("\\s+", "");
		}
		return contentBuf;
	}

	@Override
	public String getNewsImages(String html, String[] label) {
		if(html== null)
			return null;
		String bufHtml = html;        //辅助
		String imageNameTime  = "";
		//获取图片时间，为命名服务
		imageNameTime = getNewsTime(html,label);
		if(imageNameTime == null || imageNameTime.length() < 8)
			return null;
		//处理存放条图片的文件夹
    	File f = new File("SCGOV");
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
		String imageReg = "http://imgs.sc.gov.cn/[0-9]{5}/[0-9]{5}/[0-9]{5}/"+year+"/"+month+"/"+date+"/"+year+month+date+"[0-9]{5}_[0-9]{7}.jpg";
		Pattern newsImage = Pattern.compile(imageReg);
		Matcher imageMatcher = newsImage.matcher(bufHtml);
		//处理图片
		Vector<String> visitedImageUrl = new Vector<String>();
		int i = 1 ;      //本条新闻图片的个数
		while(imageMatcher.find()){
			
			String bufUrl = imageMatcher.group();
			if(!visitedImageUrl.contains(bufUrl)){
//				System.out.println(bufUrl);
				File fileBuf;
		//			imageMatcher.group();
				String imageNameSuffix = bufUrl.substring(bufUrl.lastIndexOf("."), bufUrl.length());  //图片后缀名
				try{
					URL uri = new URL(bufUrl);  
				
					InputStream in = uri.openStream();
					FileOutputStream fo;
					if(imageNumber < 10){
						fileBuf = new File("SCGOV",imageNameTime+photohour+photominute+photosecond+"000"+imageNumber+"000"+i+imageNameSuffix);
						fo = new FileOutputStream(fileBuf); 
						imageLocation.offer(fileBuf.getPath());
					}else if(imageNumber < 100){
						fileBuf = new File("SCGOV",imageNameTime+photohour+photominute+photosecond+"00"+imageNumber+"000"+i+imageNameSuffix);
						fo = new FileOutputStream(fileBuf);
						imageLocation.offer(fileBuf.getPath());
		        
					}else if(imageNumber < 1000){
						fileBuf = new File("SCGOV",imageNameTime+photohour+photominute+photosecond+"0"+imageNumber+"000"+i+imageNameSuffix);
							fo = new FileOutputStream(fileBuf);
							imageLocation.offer(fileBuf.getPath());
		  
						}else{
							fileBuf = new File("SCGOV",imageNameTime+photohour+photominute+photosecond+imageNumber+"000"+i+imageNameSuffix);
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
			visitedImageUrl.add(bufUrl);
        }  
		//如果该条新闻没有图片则图片的编号不再增加
		if(!imageLocation.isEmpty())
			imageNumber ++;
		return imageLocation.toString();
	}

	@Override
	public String getNewsTime(String html, String[] label) {
		String timeBuf = "";
		if(month < 10)
			timeBuf = timeBuf+year+"0"+month;
		else
			timeBuf = timeBuf+year+month ;
		
		if(date < 10)
			timeBuf += "0"+date;
		else
			timeBuf += date;
				
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
			sourceBuf = sourceBuf.replaceAll("\\s+", "");
			if(sourceBuf.contains("【字体"))
				sourceBuf = sourceBuf.substring(0, sourceBuf.indexOf("【字体"));
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
		if(categroyBuf!=null){
			categroyBuf = categroyBuf.replaceAll("\\s+", "");
			categroyBuf = categroyBuf.substring(0, categroyBuf.indexOf("【字体"));
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
		if(categroyBuf!=null){
			categroyBuf = categroyBuf.replaceAll("\\s+", "");
			categroyBuf = categroyBuf.substring(0, categroyBuf.indexOf("【字体"));
		}
		return categroyBuf;
	}

	public static void main(String[] args){
		
		SCGOV test = new SCGOV();
		test.getSCGOVNews();
	}

}
