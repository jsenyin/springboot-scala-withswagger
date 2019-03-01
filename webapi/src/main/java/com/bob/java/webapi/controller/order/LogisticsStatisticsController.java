package com.bob.java.webapi.controller.order;

import com.nqtown.order.dto.DeliverSituationVo;
import com.nqtown.order.dto.StatisticsOrderQueryVo;
import com.nqtown.order.service.LogisticsStatisticsService;
import com.nqtown.util.PageInfo;
import com.nqtown.util.StringUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;


/**
 * @Description 订单物流统计相关接口
 * @author lwq
 * @date 2018-6-12 14:22:21
 */
@RestController
public class LogisticsStatisticsController {
	private static Logger logger = LoggerFactory.getLogger(LogisticsStatisticsController.class);
	
	@Autowired
	private LogisticsStatisticsService logisticsStatisticsService;
	
	@RequestMapping(value="queryDeliverStatistics")
	public DeliverSituationVo queryDeliverStatistics(){
		try{
//			JProfile.enter(this.getClass().getName() + "#queryDeliverStatistics");
			
			return logisticsStatisticsService.queryDeliverStatistics();
		}finally{
//			JProfile.release();
		}
	}
	
	/**
	 * 分页查询供应商发货物流统计信息
	 * @param page
	 * @param rows
	 * @param sort 排序字段
	 * @param order 升序/降序
	 * @param keyWord 查询关键词(供应商名称)
	 * @return
	 */
	@RequestMapping(value="querySupplierDeliverStatistics")
	public PageInfo querySupplierDeliverStatistics(Integer page,Integer rows,String sort,String order,String keyWord){
		PageInfo tempPage = new PageInfo();
		if(page==null || rows==null || sort==null || order==null){
			tempPage.setRows(new ArrayList());
			return tempPage;
		}
		PageInfo pageInfo = new PageInfo(page,rows,sort,order);
		Map<String, Object> condition = new HashMap<>();
		if(StringUtils.isNotBlank(keyWord)){
			keyWord = StringEscapeUtils.unescapeHtml(keyWord);
			condition.put("keyWord", keyWord);
		}
		pageInfo.setCondition(condition);
		try{
//			JProfile.enter(this.getClass().getName() + "#querySupplierDeliverStatistics");
			return logisticsStatisticsService.querySupplierDeliverStatisticsByPage(pageInfo);
		}finally{
//			JProfile.release();
		}
	}
	
	/**
	 * 分页查询各供应商某段时间内的发货物流统计信息
	 * @param queryVo
	 * @return
	 */
	@RequestMapping(value="querySupplierDeliverStatisticsInPeriodTime")
	public PageInfo querySupplierDeliverStatisticsInPeriodTime(StatisticsOrderQueryVo queryVo){
		PageInfo tempPage = new PageInfo();
		if(queryVo == null || queryVo.getPage() == null || queryVo.getRows() == null 
				|| queryVo.getSort() == null || queryVo.getOrder() == null 
				|| queryVo.getBeginTime() == null || queryVo.getEndTime() == null){
			tempPage.setRows(new ArrayList());
			return tempPage;
		}
		if(queryVo.getEndTime().compareTo(queryVo.getBeginTime()) < 0){
			tempPage.setRows(new ArrayList());
			return tempPage;
		}

		//每次查询时间间隔最多62天
		/*int minusDay = getMinusTimeDay(queryVo.getBeginTime(),queryVo.getEndTime());
		if(minusDay > 62){
			tempPage.setRows(new ArrayList());
			return tempPage;
		}*/
		Date twoMonthAgo = getDateTwoMonthAgo(queryVo.getEndTime());
		if(queryVo.getBeginTime().compareTo(twoMonthAgo) < 0){
			tempPage.setRows(new ArrayList());
			return tempPage;
		}

		PageInfo pageInfo = new PageInfo(queryVo.getPage(),queryVo.getRows(),queryVo.getSort(),queryVo.getOrder());
		Map<String, Object> condition = new HashMap<>();
		condition.put("beginTime", queryVo.getBeginTime());
		condition.put("endTime", queryVo.getEndTime());
		if(StringUtils.isNotBlank(queryVo.getKeyWord())){
			condition.put("keyWord", StringEscapeUtils.unescapeHtml(queryVo.getKeyWord()));
		}
		pageInfo.setCondition(condition);
		try{
//			JProfile.enter(this.getClass().getName() + "#querySupplierDeliverStatisticsInPeriodTime");
			return logisticsStatisticsService.querySupplierDeliverStatisticsByPageInPeriodTime(pageInfo);
		}catch (Exception e){
			logger.error(this.getClass().getName()+"#querySupplierDeliverStatisticsInPeriodTime exception:",e);
			tempPage.setRows(new ArrayList());
			return tempPage;
//			JProfile.release();
		}
	}
	
	
	/**
	 * 查询各物流统计数据对应的订单列表
	 * @param page
	 * @param rows
	 * @param supplierId 供应商id
	 * @param logisticsType 查询的物流类型 (1-未接单，2-24h未接单,3-未发货,4-48h未发货，5-72h未发货，6-24h无物流，7-48h无物流更新,8-付款72h无物流)
	 * @param queryType	查询类型(1-总体查询，2-根据供应商查询)
	 * @return
	 */
	@RequestMapping(value="queryOrderListOfLogisticsStatistics")
	public PageInfo queryOrderListOfLogisticsStatistics(Integer page,Integer rows,Long supplierId,Integer logisticsType,Integer queryType){
		PageInfo tempPage = new PageInfo();
		if(page==null || rows==null || logisticsType==null || queryType==null){
			tempPage.setRows(new ArrayList());
			return tempPage;
		}
		if(logisticsType > 8 || logisticsType < 1){
			tempPage.setRows(new ArrayList());
			return tempPage;
		}
		if(queryType == 2 && supplierId == null){
			tempPage.setRows(new ArrayList());
			return tempPage;
		}
		PageInfo pageInfo = new PageInfo(page,rows);
		return logisticsStatisticsService.queryOrderListOfLogisticsStatistics(pageInfo,supplierId,logisticsType,queryType);
	}
	
	/**
	 * 分页查询某段时间内各物流统计数据对应的订单列表
	 * @param queryVo
	 * param logisticsType 查询的物流类型 (1-未接单，2-24h未接单,3-未发货,4-48h未发货，5-72h未发货，6-24h无物流，7-48h无物流更新,8-付款72h无物流)
	 * @return
	 */
	@RequestMapping(value="queryOrderListOfLogisticsStatisticsInperiodTime")
	public PageInfo queryOrderListOfLogisticsStatisticsInPeriodTime(StatisticsOrderQueryVo queryVo){
		PageInfo tempPage = new PageInfo();
		if(queryVo == null || queryVo.getPage() == null || queryVo.getRows() == null || queryVo.getLogisticsType() == null || queryVo.getSupplierId() == null){
			tempPage.setRows(new ArrayList());
			return tempPage;
		}
		if(queryVo.getBeginTime() == null || queryVo.getEndTime() == null){
			tempPage.setRows(new ArrayList());
			return tempPage;
		}

		if(queryVo.getEndTime().compareTo(queryVo.getBeginTime()) < 0){
			tempPage.setRows(new ArrayList());
			return tempPage;
		}

		//每次查询时间间隔最多62天
		/*int minusDay = getMinusTimeDay(queryVo.getBeginTime(),queryVo.getEndTime());
		if(minusDay > 62){
			tempPage.setRows(new ArrayList());
			return tempPage;
		}*/
		Date twoMonthAgo = getDateTwoMonthAgo(queryVo.getEndTime());
		if(queryVo.getBeginTime().compareTo(twoMonthAgo) < 0){
			tempPage.setRows(new ArrayList());
			return tempPage;
		}

		if(queryVo.getLogisticsType() > 8 || queryVo.getLogisticsType() < 1){
			tempPage.setRows(new ArrayList());
			return tempPage;
		}
		PageInfo pageInfo = new PageInfo(queryVo.getPage(),queryVo.getRows());
		return logisticsStatisticsService.queryOrderListOfLogisticsStatisticsInPeriodTime(pageInfo,queryVo);
	}
	

	/**
	 * 获取两个月前的日期
	 * @param date
	 * @return
	 */
	private Date getDateTwoMonthAgo(Date date){
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c = getSqlDate(c);
		c.add(Calendar.MONTH,-2);
		return c.getTime();
	}

	/**
	 * 获取时分秒清零的Calendar
	 * @param calendar
	 * @return
	 */
	private Calendar getSqlDate(Calendar calendar){
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar;
	}
	
}
