package com.uestc.timeTask.www;

import java.util.TimerTask;

import com.uestc.NETEASE.www.NETEASEFocus;
import com.uestc.NETEASE.www.NETEASEGuoJi;
import com.uestc.NETEASE.www.NETEASEGuoNei;
import com.uestc.NETEASE.www.NETEASESheHui;
import com.uestc.NETEASE.www.NETEASEView;
import com.uestc.NETEASE.www.NETEASEWar;

public class TimeTask extends TimerTask{

	@Override
	public void run() {
		// TODO Auto-generated method stub
		NETEASEGuoNei test = new NETEASEGuoNei();
		test.getNETEASEGuoNeiNews();
		NETEASEGuoJi test1 = new NETEASEGuoJi();
		test1.getNETEASEGuoJiNews();
		NETEASESheHui test2 = new NETEASESheHui();
		test2.getNETEASESheHuiNews();
		NETEASEView test3 = new NETEASEView();
		test3.getNETEASEViewNews();
		NETEASEWar test4 = new NETEASEWar();
		test4.getNETEASEWarNews();
		NETEASEFocus test5 = new NETEASEFocus();
		test5.getNETEASEFocusNews();
	}

}
