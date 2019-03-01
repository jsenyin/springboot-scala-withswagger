
package com.bob.java.webapi.controller.order;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.bob.java.webapi.utils.BeanUtilEx;
import com.bob.java.webapi.utils.DataGroupTools;
import com.bob.java.webapi.utils.Flector;
import com.nqtown.basic.client.logger.TraceUtil;
import com.nqtown.common.mertics.JProfile;
import com.nqtown.enums.BaseEnum;
import com.nqtown.member.model.Supplier;
import com.nqtown.member.service.MemberService;
import com.nqtown.member.service.SupplierService;
import com.nqtown.member.vo.MemberClass;
import com.nqtown.model.ExportOrderVO;
import com.nqtown.model.Inventory;
import com.nqtown.model.SupplierSku;
import com.nqtown.model.SupplierSpec;
import com.nqtown.order.dto.*;
import com.nqtown.order.query.OrderQuery;
import com.nqtown.order.request.OrderRequest;
import com.nqtown.order.service.*;
import com.nqtown.service.*;
import com.nqtown.util.EasyResult;
import com.nqtown.util.FinalResult;
import com.nqtown.util.PageUtil;
import com.nqtown.util.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * @author cjiang
 * @ClassName: SCMOrderManagedController
 * @Description: SCMweb: 订单管理
 * @date 2018年1月15日 下午8:19:25
 */
@RestController
public class SCMOrderManagedController {

    protected Logger logger = LogManager.getLogger(getClass());

    @Autowired
    private ScmOMServiceWrapper scmOMServiceWrapper;

    @Autowired
    private OrderCommonServiceWrapper orderCommonServiceWrapper;

    @Autowired
    private MemberService memberService;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private SkuService skuService;

    @Autowired
    private SpecService specService;

    @Autowired
    protected SupplierSkuService supplierSkuService;

    @Autowired
    protected SupplierSpecService supplierSpecService;

    @Autowired
    private SOrderManagedServiceWrapper sOrderManagedServiceWrapper;

    @Autowired
    protected ExternalOrderService externalOrderService;



    /**
     * @param @param  orderQueryVO
     * @param @return 参数
     * @return EasyResult<List                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               <                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               SCMOrderSimpleVO>>    返回类型
     * @throws
     * @Title: querySCMOrderSimpleByPage
     * @Description: 分页模糊或高级查询订单列表
     */
    @RequestMapping(value = "querySCMOrderSimpleByPage")
    public EasyResult<List<SCMOrderSimpleVO>> querySCMOrderSimpleByPage(SCMOrderQueryVO orderQueryVO) {
        if ((null == orderQueryVO.getPage()) || (orderQueryVO.getPage() < 1)) {
            orderQueryVO.setPage(1);
        }

        if ((null == orderQueryVO.getRows()) || (orderQueryVO.getRows() < 1)) {
            orderQueryVO.setRows(10);
        }

        try {
            JProfile.enter(this.getClass().getName() + "#querySCMOrderSimpleByPage");

            if (null == orderQueryVO.getHistory()) {
                orderQueryVO.setHistory(false);
            }

            if (null == orderQueryVO.getOrderType()) {
                orderQueryVO.setOrderType(1);
            }
            checkedSupplierId(orderQueryVO);

            EasyResult<List<SCMOrderSimpleVO>> result = new EasyResult<>();
            result = scmOMServiceWrapper.querySCMOrderSimpleByPage(orderQueryVO);

            if ((true == orderQueryVO.getHistory())
                    && (null != result)
                    && (CollectionUtils.isNotEmpty(result.getRows()))) {
                result.getRows().stream().forEach(scmOrderSimpleVO -> scmOrderSimpleVO.setHistory(true));
            }

            return result;
        } finally {
            JProfile.release();
        }
    }


    /**
     * @param @param scmOrderDetailVO    参数
     * @return void    返回类型
     * @throws
     * @Title: fillMemberInfos
     * @Description: 填充会员相关信息
     */
    private void fillMemberInfos(SCMOrderDetailVO scmOrderDetailVO) {
        MemberClass memberClass = memberService.queryMemberByOrderMemberId(scmOrderDetailVO.getUserId());
        if (null == memberClass) {
            return;
        }
        scmOrderDetailVO.setScmodMemberVO(new SCMODMemberVO());

        scmOrderDetailVO.getScmodMemberVO().setMemberId(memberClass.getId());
        scmOrderDetailVO.getScmodMemberVO().setName(memberClass.getNote());
        scmOrderDetailVO.getScmodMemberVO().setTelephone(memberClass.getTelephone());
        scmOrderDetailVO.getScmodMemberVO().setMemberTag(memberClass.getMemberTag());
        scmOrderDetailVO.getScmodMemberVO().setMemberLevel(memberClass.getMemberLevel());
        scmOrderDetailVO.getScmodMemberVO().setCreateTime(memberClass.getCreateTime().toString());
        if (null != memberClass.getLastLoginTime()) {
            scmOrderDetailVO.getScmodMemberVO().setLastLoginTime(memberClass.getLastLoginTime().toString());
        }
        if (null != memberClass.getMonthLoginNum()) {
            scmOrderDetailVO.getScmodMemberVO().setMonthLoginNum(memberClass.getMonthLoginNum().toString());
        }
        scmOrderDetailVO.getScmodMemberVO().setLastNowContactTime(memberClass.getLastNowContactTime());
        scmOrderDetailVO.getScmodMemberVO().setLeadingOfficial(memberClass.getLeadingOfficial());
        scmOrderDetailVO.getScmodMemberVO().setHistoryTurnVolume(memberClass.getAllTradeFee());
        scmOrderDetailVO.getScmodMemberVO().setMonthTurnVolume(memberClass.getThisMonthFee());
        scmOrderDetailVO.getScmodMemberVO().setHistoryTurnVolume(memberClass.getAllTradeFee());
        scmOrderDetailVO.getScmodMemberVO().setPrecedingMonthTurnVolumn(memberClass.getThisMonthFee());
    }


    /**
     * @param @param  ids
     * @param @return 参数
     * @return EasyResult<String>    返回类型
     * @throws
     * @Title: batchConfirmReceiverItem
     * @Description: 批量确认收货 - 订单完成
     */
    @RequestMapping("batchConfirmReceiverItem")
    public EasyResult<String> batchConfirmReceiverItem(OrderIdsParasWrapper wrapper) {
        if ((null == wrapper) || (CollectionUtils.isEmpty(wrapper.getOrderIdsParasList()))) {
            return null;
        }
        try {
            JProfile.enter(this.getClass().getName() + "#batchConfirmReceiverItem");

        } finally {
            JProfile.release();
        }
        OperatorInfoVO operatorInfoVO = new OperatorInfoVO();
        operatorInfoVO.setOperatorType(OperatorTypeEnum.SCM);

        wrapper.setOperatorInfoVO(operatorInfoVO);

        return orderCommonServiceWrapper.confirmReceiverItem(wrapper);
    }

    /**
     * @param @param  id:异常订单表id
     * @param @return 参数
     * @return EasyResult<SCMOAInfosVO>    返回类型
     * @throws
     * @Title: querySCMOAInfosVOById
     * @Description: 通过id查询异常详情
     */
    @RequestMapping("querySCMOAInfosVOById")
    public EasyResult<SCMOAInfosVO> querySCMOAInfosVOById(Long id) {
        if (null == id) {
            return null;
        }
        try {
            JProfile.enter(this.getClass().getName() + "#querySCMOAInfosVOById");

            return scmOMServiceWrapper.querySCMOAInfosVOById(id);
        } finally {
            JProfile.release();
        }
    }

    /**
     * @param @param  orderNum 主订单号
     * @param @return 参数
     * @return EasyResult<ChangedOrderReceiverInfosVO>    返回类型
     * @throws
     * @Title: queryOrderReceiverInfos
     * @Description: 通过主订单号查询收货人信息
     */
    @RequestMapping("queryOrderReceiverInfos")
    public EasyResult<ChangedOrderReceiverInfosVO> queryOrderReceiverInfos(Long orderNum) {
        if (null == orderNum) {
            return null;
        }
        try {
            JProfile.enter(this.getClass().getName() + "#queryOrderReceiverInfos");

            return orderCommonServiceWrapper.queryOrderReceiverInfos(orderNum);
        } finally {
            JProfile.release();
        }
    }

    /**
     * @param @param  changedOrderReceiverInfosVO
     * @param @return 参数
     * @return EasyResult<Boolean>    返回类型
     * @throws
     * @Title: changedOrderReceiverInfos
     * @Description: 可修复异常：更换收货人信息
     */
    @RequestMapping("changedOrderReceiverInfos")
    public EasyResult<Boolean> changedOrderReceiverInfos(ChangedOrderReceiverInfosVO changedOrderReceiverInfosVO) {
        if ((null == changedOrderReceiverInfosVO.getOrderNum()) || (null == changedOrderReceiverInfosVO.getOlId())) {
            return null;
        }
        try {
            JProfile.enter(this.getClass().getName() + "#changedOrderReceiverInfos");

            changedOrderReceiverInfosVO.setOperatorType(OperatorTypeEnum.SCM);

            return orderCommonServiceWrapper.changedOrderReceiverInfos(changedOrderReceiverInfosVO);
        } finally {
            JProfile.release();
        }
    }

    /**
     * @param @param  id： 交易订单id
     * @param @return 参数
     * @return EasyResult<String>    返回类型 :转换失败订单号，逗号分隔
     * @throws
     * @Title: batchOrderAbnormalToOAS
     * @Description: 异常订单转为售后
     */
    @RequestMapping("batchOrderAbnormalToOAS")
    public EasyResult<String> batchOrderAbnormalToOAS(OrderIdsParasWrapper wrapper) {
        if ((null == wrapper) || (CollectionUtils.isEmpty(wrapper.getOrderIdsParasList()))) {
            return null;
        }
        try {
            JProfile.enter(this.getClass().getName() + "#batchOrderAbnormalToOAS");

            OperatorInfoVO operatorInfoVO = new OperatorInfoVO();
            operatorInfoVO.setOperatorType(OperatorTypeEnum.SCM);

            wrapper.setOperatorInfoVO(operatorInfoVO);

            return orderCommonServiceWrapper.batchOrderAbnormalToOAS(wrapper);
        } finally {
            JProfile.release();
        }
    }

    /**
     * 异常订单退款处理
     * 神售下的订单(保税订单)异常处理
     *
     * @param refundVO
     * @return
     */
    @RequestMapping("abnormalOrderRefund")
    public EasyResult<Boolean> abnormalOrderRefund(AbnormalOrderRefundVO refundVO) {
        if (refundVO == null
                || refundVO.getOrderId() == null
                || refundVO.getOaId() == null
                || refundVO.getAgree() == null
                || (refundVO.getAgree() != 1 && refundVO.getAgree() != 0)) {
            return null;
        }
        try {
            JProfile.enter(this.getClass().getName() + "#abnormalOrderRefund");

            refundVO.setOperatorType(OperatorTypeEnum.SCM);

            return scmOMServiceWrapper.abnormalOrderRefund(refundVO);
        } finally {
            JProfile.release();
        }
    }


    /**
     * @param @param  applyNum
     * @param @return 参数
     * @return EasyResult<ReviewOASInfosVO>    返回类型
     * @throws
     * @Title: queryOASInfosByApplyNum
     * @Description: 通过申请编号查询售后详情
     */
    @RequestMapping("queryOASInfosByApplyNum")
    public EasyResult<ReviewOASInfosVO> queryOASInfosByApplyNum(String applyNum) {
        if (StringUtils.isBlank(applyNum)) {
            return null;
        }
        try {
            JProfile.enter(this.getClass().getName() + "#queryOASInfosByApplyNum");

            return scmOMServiceWrapper.queryOASInfosByApplyNum(applyNum);
        } finally {
            JProfile.release();
        }
    }


    private void checkedSupplierId(SCMOrderQueryVO orderQueryVO) {
        if (null != orderQueryVO.getSupplierId()) {
            //supplierId:对传入的供应商id进行校验，不存在值为null
            List<Supplier> listSupplier = queryAllSuppliers();
            if (CollectionUtils.isNotEmpty(listSupplier)) {
                Optional<Supplier> optSupplier = listSupplier.stream()
                        .filter(s -> s.getId().equals(orderQueryVO.getSupplierId()))
                        .findFirst();
                if (!optSupplier.isPresent()) {
                    orderQueryVO.setSupplierId(null);
                }
            }
        }
    }


    /**
     * @param
     * @return java.util.List<com.nqtown.order.dto.SCMOrderQueryVO>
     * @description: 对账单批量导出，一次最多导出10个对账单信息
     * @author cjiang
     * @date 2018/12/3 9:08
     */
    private List<SCMOrderQueryVO> buildBatchExportStatementParas(HttpServletRequest request) {
        List<SCMOrderQueryVO> scmOrderQueryVOs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            try {
                String statementId = request.getParameter(String.format("scmOrderQueryVOs[%s].statementId", i));
                String statementNum = request.getParameter(String.format("scmOrderQueryVOs[%s].statementNum", i));
                String statementVersion = request.getParameter(String.format("scmOrderQueryVOs[%s].statementVersion", i));
                String statementSupplierName = request.getParameter(String.format("scmOrderQueryVOs[%s].statementSupplierName", i));
                if ((StringUtils.isNotBlank(statementId))
                        && (StringUtils.isNotBlank(statementNum))
                        && (StringUtils.isNotBlank(statementVersion))) {
                    try {
                        SCMOrderQueryVO scmOrderQueryVO = new SCMOrderQueryVO();
                        scmOrderQueryVO.setStatementId(Long.parseLong(statementId));
                        scmOrderQueryVO.setStatementNum(statementNum);
                        scmOrderQueryVO.setStatementVersion(Integer.parseInt(statementVersion));
                        scmOrderQueryVO.setStatementSupplierName(statementSupplierName);
                        scmOrderQueryVOs.add(scmOrderQueryVO);
                    } catch (Exception e) {

                    }
                } else {
                    break;
                }
            } catch (Exception e) {

            }
        }

        return scmOrderQueryVOs;
    }

    /**
     * @throws
     * @Title: buildStatementOrderOrBack
     * @Description: 对账单中的正常订单或退单
     * @param: @param orderQueryVO
     * @param: @return
     * @return: ExportOrderVO
     */
    private ExportOrderVO buildStatementOrderOrBack(SCMOrderQueryVO scmOrderQueryVO) {
        //分批获取订单简单信息
        List<SCMOrderSimpleVO> scmOrderSimpleVOList = querySCMStatementOrderSimpleByBatch(scmOrderQueryVO);
        if (CollectionUtils.isEmpty(scmOrderSimpleVOList)) {
            return null;
        }

        //使用线程池获取订单导出信息
        try {
            ExecutorService executorService = Executors.newFixedThreadPool(10);
            CountDownLatch countDownLatch = new CountDownLatch(scmOrderSimpleVOList.size());
            List<Future<SCMOrderExportVO>> listTask = new ArrayList<>();
            for (SCMOrderSimpleVO scmOrderSimpleVO : scmOrderSimpleVOList) {
                listTask.add(executorService.submit(() -> {
                    try {
                        return queryOrderExportVOByOrder(scmOrderSimpleVO);
                    } finally {
                        countDownLatch.countDown();
                    }
                }));
            }
            executorService.shutdown();

            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
            }
            //获取线程执行结果，生成导出map信息
            ExportOrderVO exportOrderVO = buildExportOrderVO(listTask.parallelStream()
                    .map(task -> {
                        try {
                            return task.get();
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(detail -> null != detail)
                    .collect(Collectors.toList()), scmOrderQueryVO.getStatementSupplierName());
            //对账单导出
            exportOrderVO.setColumnNames(new String[]{
                    "下单时间", "订单号", "外部订单号", "会员名", "负责人", "供应商", "收货人",
                    "商品名称", "sku属性", "条形码", "品牌", "类目", "南圈货号", "总数量", "总价",
                    "实付金额", "优惠劵金额", "优惠标签金额", "供货价", "税费", "运费", "前台毛利额", "前台毛利率"
            });
            exportOrderVO.setName(scmOrderQueryVO.getStatementNum() + ((0 == scmOrderQueryVO.getType()) ? "(接单)" : "(退单)"));

            return exportOrderVO;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @param
     * @return java.util.List<com.nqtown.order.dto.SCMOrderSimpleVO>
     * @description: 分批查询出对账单中正常订单或异常简单信息
     * @author cjiang
     * @date 2018/12/3 10:18
     */
    private List<SCMOrderSimpleVO> querySCMStatementOrderSimpleByBatch(SCMOrderQueryVO scmOrderQueryVO) {
        //先根据查询条件，分批查询出符合的订单简单信息，避免一次查询数据量太大，导致 dubbo 超时
        List<SCMOrderSimpleVO> scmOrderSimpleVOList = new ArrayList<>();
        try {
            scmOrderQueryVO.setPage(1);
            scmOrderQueryVO.setRows(250);
            EasyResult<List<SCMOrderSimpleVO>> easyResult = scmOMServiceWrapper.querySCMOrderSimpleByStatement(scmOrderQueryVO);
            if ((null == easyResult) || (CollectionUtils.isEmpty(easyResult.getRows()))) {
                return scmOrderSimpleVOList;
            } else {
                scmOrderSimpleVOList.addAll(easyResult.getRows());
            }
            Integer count = easyResult.getTotal();
            Integer page = 1;
            //对账单不受导出条数限制
            while (scmOrderSimpleVOList.size() < count) {
                page++;
                scmOrderQueryVO.setPage(page);
                easyResult = scmOMServiceWrapper.querySCMOrderSimpleByStatement(scmOrderQueryVO);
                if ((null == easyResult) || (CollectionUtils.isEmpty(easyResult.getRows()))) {
                    break;
                } else {
                    scmOrderSimpleVOList.addAll(easyResult.getRows());
                }
            }
        } catch (Exception e) {
            scmOrderSimpleVOList = new ArrayList<>();
        }

        return scmOrderSimpleVOList;
    }

    /**
     * @param
     * @return java.util.List<com.nqtown.order.dto.SCMOASSimpleVO>
     * @description: 分批查询出对账单中售后订单全部信息
     * @author cjiang
     * @date 2018/12/3 10:19
     */
    private List<SCMOASSimpleVO> querySCMStatementOASSimpleByBatch(SCMOrderQueryVO scmOrderQueryVO) {
        //先根据查询条件，分批查询出符合的订单简单信息，避免一次查询数据量太大，导致 dubbo 超时
        List<SCMOASSimpleVO> scmoasSimpleVOList = new ArrayList<>();
        try {
            scmOrderQueryVO.setPage(1);
            scmOrderQueryVO.setRows(250);
            EasyResult<List<SCMOASSimpleVO>> easyResult = scmOMServiceWrapper.querySCMOASListByStatement(scmOrderQueryVO);
            if ((null == easyResult) || (CollectionUtils.isEmpty(easyResult.getRows()))) {
                return scmoasSimpleVOList;
            } else {
                scmoasSimpleVOList.addAll(easyResult.getRows());
            }
            Integer count = easyResult.getTotal();
            Integer page = 1;
            //对账单不受导出条数限制
            while (scmoasSimpleVOList.size() < count) {
                page++;
                scmOrderQueryVO.setPage(page);
                easyResult = scmOMServiceWrapper.querySCMOASListByStatement(scmOrderQueryVO);
                if ((null == easyResult) || (CollectionUtils.isEmpty(easyResult.getRows()))) {
                    break;
                } else {
                    scmoasSimpleVOList.addAll(easyResult.getRows());
                }
            }
        } catch (Exception e) {
            scmoasSimpleVOList = new ArrayList<>();
        }

        return scmoasSimpleVOList;
    }

    /**
     * @throws
     * @Title: buildStatementOAS
     * @Description: 对账单中的售后订单
     * @param: @param orderQueryVO
     * @param: @return
     * @return: ExportOrderVO
     */
    private ExportOrderVO buildStatementOAS(SCMOrderQueryVO scmOrderQueryVO) {
        try {
            List<SCMOASSimpleVO> scmoasSimpleVOList = querySCMStatementOASSimpleByBatch(scmOrderQueryVO);
            if (CollectionUtils.isEmpty(scmoasSimpleVOList)) {
                return null;
            }

            ExportOrderVO exportOrderVO = buildExportOASVO(scmoasSimpleVOList);
            exportOrderVO.setName(scmOrderQueryVO.getStatementNum() + "(售后)");

            return exportOrderVO;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @param @param  scmOrderSimpleVO
     * @param @return 参数
     * @return SCMOrderExportVO    返回类型
     * @throws
     * @Title: queryOrderExportVOByOrder
     * @Description: 订单导出：查询订单导出相关信息
     */
    private SCMOrderExportVO queryOrderExportVOByOrder(SCMOrderSimpleVO scmOrderSimpleVO) {
        SCMOrderExportVO scmOrderExportVO = null;
        try {
            scmOrderExportVO = scmOMServiceWrapper.queryOrderExportVOByOrder(scmOrderSimpleVO);
        } catch (Exception e) {
            scmOrderExportVO = null;
        }

        return scmOrderExportVO;
    }

    /**
     * @param @param  orderListVOs
     * @param @return 参数
     * @return ExportOrderVO    返回类型
     * @throws
     * @Title: buildExportOrderVO
     * @Description: 生成对应的导出订单map：以售后模板为准，对账单只是其中一部分信息。
     */
    private ExportOrderVO buildExportOrderVO(List<SCMOrderExportVO> orderListVOs, String supplierName) {
        ExportOrderVO exportOrderVO = new ExportOrderVO();
        exportOrderVO.setColumnNames(new String[]{
                "订单号", "下单时间", "支付时间", "订单状态", "外部订单号", "会员名", "负责人",
                "供应商",
                "商品名称", "sku属性", "条形码", "品牌", "类目", "南圈货号", "规格", "购买量", "总数量",
                "售价", "总价", "实付金额", "优惠劵金额", "优惠标签金额", "供货价", "税费", "运费", "佣金", "代购价",
                "前台毛利额", "前台毛利率",
                "发货状态", "发货时间", "物流公司", "物流单号", "收货人", "联系电话", "身份证", "国家", "省", "市", "区", "详细地址", "备注"
        });
        exportOrderVO.setLines(new ArrayList<>());

        List<String> orderNumList = new ArrayList<>();
        for (SCMOrderExportVO scmOrderExportVO : orderListVOs) {
            if (CollectionUtils.isEmpty(scmOrderExportVO.getScmOrderItemExportVOs())) {
                continue;
            }
            if (orderNumList.contains(scmOrderExportVO.getOrderNumText())) {
                continue;
            } else {
                orderNumList.add(scmOrderExportVO.getOrderNumText());
            }

            for (SCMOrderItemExportVO scmOrderItemExportVO : scmOrderExportVO.getScmOrderItemExportVOs()) {
                Map<String, String> map = new HashMap<>();
                map.put("订单号", scmOrderExportVO.getOrderNumText());
                map.put("下单时间", scmOrderExportVO.getCreatedTime());
                map.put("支付时间", scmOrderExportVO.getPayTime());
                map.put("订单状态", scmOrderExportVO.getOrderStatusText());
                map.put("外部订单号", scmOrderExportVO.getExternalOrderNum());
                map.put("会员名", scmOrderExportVO.getMemberName());
                map.put("负责人", scmOrderExportVO.getLeadingOfficial());
                if (StringUtils.isNotBlank(supplierName)) {
                    map.put("供应商", supplierName);
                } else {
                    map.put("供应商", scmOrderItemExportVO.getSupplierName());
                }
                map.put("商品名称", scmOrderItemExportVO.getItemName());
                map.put("sku属性", scmOrderItemExportVO.getSkuPropText());
                map.put("条形码", scmOrderItemExportVO.getBarcode());
                map.put("品牌", scmOrderItemExportVO.getBrand());
                map.put("类目", scmOrderItemExportVO.getTopCat());
                map.put("南圈货号", scmOrderItemExportVO.getItemNumber());
                map.put("规格", scmOrderItemExportVO.getSpecification());
                map.put("购买量", scmOrderItemExportVO.getAmount());
                map.put("总数量", scmOrderItemExportVO.getTotalAmount());
                map.put("售价", scmOrderItemExportVO.getSellPriceText());
                map.put("总价", scmOrderItemExportVO.getTotalPriceText());
                map.put("实付金额", scmOrderItemExportVO.getActualPayPriceText());
                map.put("优惠劵金额", scmOrderItemExportVO.getCouponPriceText());
                map.put("优惠标签金额", scmOrderItemExportVO.getDiscountText());
                map.put("供货价", scmOrderItemExportVO.getExcludingCostText());
                map.put("税费", scmOrderItemExportVO.getTaxText());
                map.put("运费", scmOrderItemExportVO.getPostFeeText());
                map.put("代购价", scmOrderItemExportVO.getPurchasingPriceText());
                map.put("佣金", scmOrderItemExportVO.getCommissionText());
                map.put("前台毛利额", scmOrderItemExportVO.getGrossProfitPricePre());
                map.put("前台毛利率", scmOrderItemExportVO.getGrossProfitRatePre());
                map.put("发货状态", scmOrderExportVO.getSendStatusText());
                map.put("发货时间", scmOrderExportVO.getSendTime());
                map.put("物流公司", scmOrderExportVO.getLogisticsCompany());
                map.put("物流单号", scmOrderExportVO.getLogisticsNum());
                map.put("收货人", scmOrderExportVO.getReceiverName());
                map.put("联系电话", scmOrderExportVO.getMobilePhone());
                map.put("身份证", scmOrderExportVO.getIdCard());
                map.put("国家", scmOrderExportVO.getCountry());
                map.put("省", scmOrderExportVO.getProvince());
                map.put("市", scmOrderExportVO.getCity());
                map.put("区", scmOrderExportVO.getDistrict());
                map.put("详细地址", scmOrderExportVO.getAddress());
                map.put("备注", scmOrderExportVO.getOrderRemark());

                exportOrderVO.getLines().add(map);
            }
        }

        return exportOrderVO;
    }

    /**
     * @param @param  orderListVOs
     * @param @return 参数
     * @return ExportOrderVO    返回类型
     * @throws
     * @Title: buildExportOASVO
     * @Description: 生成对应的导出售后订单map
     */
    private ExportOrderVO buildExportOASVO(List<SCMOASSimpleVO> orderListVOs) {
        ExportOrderVO exportOrderVO = new ExportOrderVO();
        exportOrderVO.setColumnNames(new String[]{
                "订单号", "申请编号", "供应商", "商品名称", "供应商退款金额", "退运费金额"
        });
        exportOrderVO.setLines(new ArrayList<>());

        for (SCMOASSimpleVO scmoasSimpleVO : orderListVOs) {
            Map<String, String> map = new HashMap<>();
            map.put("订单号", scmoasSimpleVO.getOrderNumText());
            map.put("申请编号", scmoasSimpleVO.getApplyNumText());
            map.put("供应商", scmoasSimpleVO.getSupplierName());
            map.put("商品名称", scmoasSimpleVO.getItemName());
            map.put("供应商退款金额", scmoasSimpleVO.getSupplierPriceText());
            map.put("退运费金额", scmoasSimpleVO.getWdPostFeeText());

            exportOrderVO.getLines().add(map);
        }

        return exportOrderVO;
    }

    /**
     * @param @param  scmORQueryVO
     * @param @return 参数
     * @return EasyResult<List                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               <                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               SCMOrderRefundListVO>>    返回类型
     * @throws
     * @Title: queryOrderRefundListByPage
     * @Description: SCM 退款管理查询列表
     */
    @RequestMapping("queryOrderRefundListByPage")
    public EasyResult<List<SCMOrderRefundListVO>> queryOrderRefundListByPage(SCMOrderRefundQueryVO scmORQueryVO) {
        if ((null == scmORQueryVO.getPage()) || (scmORQueryVO.getPage() < 1)) {
            scmORQueryVO.setPage(1);
        }

        if ((null == scmORQueryVO.getRows()) || (scmORQueryVO.getRows() < 1)) {
            scmORQueryVO.setRows(10);
        }
        try {
            JProfile.enter(this.getClass().getName() + "#queryOrderRefundListByPage");

            return scmOMServiceWrapper.queryOrderRefundListByPage(scmORQueryVO);
        } finally {
            JProfile.release();
        }
    }

    /**
     * @param @param  queryStatementListVO
     * @param @return 参数
     * @return EasyResult<List                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               <                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               StatementListVO>>    返回类型
     * @throws
     * @Title: queryStatementListByPage
     * @Description: 查询对账单列表
     */
    @RequestMapping("queryStatementListByPage")
    public EasyResult<List<StatementListVO>> queryStatementListByPage(QueryStatementListVO queryStatementListVO) {
        if ((null == queryStatementListVO.getPage()) || (queryStatementListVO.getPage() < 1)) {
            queryStatementListVO.setPage(1);
        }

        if ((null == queryStatementListVO.getRows()) || (queryStatementListVO.getRows() < 1)) {
            queryStatementListVO.setRows(10);
        }
        try {
            JProfile.enter(this.getClass().getName() + "#queryStatementListByPage");

            return orderCommonServiceWrapper.queryStatementListByPage(queryStatementListVO);
        } finally {
            JProfile.release();
        }
    }

    /**
     * @param @param  scmConfirmStatementVO
     * @param @return 参数
     * @return EasyResult<Boolean>    返回类型
     * @throws
     * @Title: scmStatementHandle
     * @Description: SCM 对账单处理：小二修改金额；小二确认对账单；财务确认对账单；
     */
    @RequestMapping("scmStatementHandle")
    public EasyResult<Boolean> scmStatementHandle(SCMStatementHandleVO scmStatementHandleVO) {
        if ((null == scmStatementHandleVO)
                || (CollectionUtils.isEmpty(scmStatementHandleVO.getIds()))
                || (null == scmStatementHandleVO.getType())) {
            return null;
        }
        try {
            JProfile.enter(this.getClass().getName() + "#confirmStatement");

            return scmOMServiceWrapper.scmStatementHandle(scmStatementHandleVO);
        } finally {
            JProfile.release();
        }
    }

    /**
     * @param @param  orderQueryVO
     * @param @return 参数
     * @return EasyResult<List                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               <                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               SCMOrderSimpleVO>>    返回类型
     * @throws
     * @Title: querySCMOrderSimpleByStatement
     * @Description: 通过对账单号查询订单列表或退单列表
     */
    @RequestMapping(value = "querySCMOrderSimpleByStatement")
    public EasyResult<List<SCMOrderSimpleVO>> querySCMOrderSimpleByStatement(SCMOrderQueryVO orderQueryVO) {
        if ((null == orderQueryVO)
                || (StringUtils.isBlank(orderQueryVO.getStatementNum()))
                || (null == orderQueryVO.getStatementVersion())
                || (null == orderQueryVO.getStatementId())
                || (null == orderQueryVO.getType())) {
            return null;
        }

        if ((null == orderQueryVO.getPage()) || (orderQueryVO.getPage() < 1)) {
            orderQueryVO.setPage(1);
        }

        if ((null == orderQueryVO.getRows()) || (orderQueryVO.getRows() < 1)) {
            orderQueryVO.setRows(10);
        }
        try {
            JProfile.enter(this.getClass().getName() + "#querySCMOrderSimpleByStatement");

            return scmOMServiceWrapper.querySCMOrderSimpleByStatement(orderQueryVO);
        } finally {
            JProfile.release();
        }
    }

    /**
     * @throws
     * @Title: querySOASListByStatement
     * @Description: 通过对账单号查询售后单表
     * @param: @param querySOrderVO
     * @param: @return
     * @return: EasyResult<List                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               <                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               SOASListVO>>
     */
    @RequestMapping("querySCMOASListByStatement")
    public EasyResult<List<SCMOASSimpleVO>> querySCMOASListByStatement(SCMOrderQueryVO orderQueryVO) {
        if ((null == orderQueryVO)
                || (StringUtils.isBlank(orderQueryVO.getStatementNum()))
                || (null == orderQueryVO.getStatementVersion())
                || (null == orderQueryVO.getStatementId())
                || (null == orderQueryVO.getType())) {
            return null;
        }

        if ((null == orderQueryVO.getPage()) || (orderQueryVO.getPage() < 1)) {
            orderQueryVO.setPage(1);
        }

        if ((null == orderQueryVO.getRows()) || (orderQueryVO.getRows() < 1)) {
            orderQueryVO.setRows(10);
        }
        try {
            JProfile.enter(this.getClass().getName() + "#querySCMOASListByStatement");

            return scmOMServiceWrapper.querySCMOASListByStatement(orderQueryVO);
        } finally {
            JProfile.release();
        }
    }

    /**
     * @param @return 参数
     * @return EasyResult<Map                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               <                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               Integer                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               ,                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               String>>    返回类型
     * @throws
     * @Title: queryAllSuppliers
     * @Description: 获取所有供应商id-name
     */
    @RequestMapping("queryAllSuppliers")
    public List<Supplier> queryAllSuppliers() {
        return supplierService.querySupplierList();
    }


    /**
     * @param @param  orderDTOList
     * @param @param  userId
     * @param @return 参数
     * @return Boolean    返回类型
     * @throws
     * @Title: changeOrderSupplierWriter
     * @Description: 更换供应商：写入数据库
     */
    private Boolean changeOrderSupplierWriter(List<OrderDTO> orderDTOList, Long userId) {
        CreatingOrderGroupDTO creatingOrderGroupDTO = new CreatingOrderGroupDTO();
        orderDTOList.stream().forEach(orderDTO -> {
            creatingOrderGroupDTO.getOrderDTOList().add(orderDTO);
        });
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setOrderGroupDTO(creatingOrderGroupDTO);
        orderRequest.setUserId(userId);
        orderRequest.setTraceId(TraceUtil.getTraceId());
        orderRequest.setOrderGroupDTO(creatingOrderGroupDTO);
        FinalResult<CreatingOrderResultDTO> finalResult = orderService.batchChangeOrderSupplier(orderRequest);
        if (!finalResult.getStatus() || finalResult.getData() == null) {
            return false;
        }

        return true;
    }

    private List<OrderDTO> queryOrderDTOs(Long orderId) {
        List<Long> orderList = new ArrayList<>();
        orderList.add(orderId);

        // 查询订单状态：待接单
        OrderQuery query = new OrderQuery();
        query.setOrderIdList(orderList);
        query.setOrderStatus(OrderStatusEnum.WAIT_FOR_ACCEPT.getStatus());
        query.setQueryOrderSku(true);
        FinalResult<List<OrderDTO>> orderResult = orderService.queryOrder(query);
        if (!orderResult.getStatus()) {
            return null;
        }
        if (orderList.size() != orderResult.getData().size()) {
            return null;
        }

        //2018-10-29,cjiang,锁库订单不能调仓
        List<OrderDTO> orderDTOList = orderResult.getData().stream()
                .filter(orderDTO -> (!(OrderSourceEnum.LOCK_SKU.getCode().equals(orderDTO.getOrderSource())
                        || OrderSourceEnum.LOCK_IMPORT.getCode().equals(orderDTO.getOrderSource()))))
                .collect(Collectors.toList());

        return orderDTOList;
    }

    /**
     * @param @param  changeSupplierVO
     * @param @return 参数
     * @return List<OrderDTO>    返回类型
     * @throws
     * @Title: checkOrderSupplier
     * @Description: 更换供应商：先检查
     */
    private List<OrderDTO> checkOrderSupplier(ChangeSupplierVO changeSupplierVO) {
        Long newSupplierId = changeSupplierVO.getSupplierId();
        Long newSkuId = changeSupplierVO.getSkuId();

        List<OrderDTO> orderDTOList = queryOrderDTOs(changeSupplierVO.getId());
        if (CollectionUtils.isEmpty(orderDTOList)) {
            return null;
        }
        for (OrderDTO orderDTO : orderDTOList) {
            //交易订单表中需要冗余保存供应商id，此处将新id保存起来。
            orderDTO.setSupplierId(newSupplierId);
            for (OrderSkuDTO orderSkuDTO : orderDTO.getOrderSkuDTOList()) {
                if (supplierService.selectById(newSupplierId) == null) {
                    return null;
                }

                /**
                 * 判断新供应商 商品是否上架
                 */
                SupplierSku supplierSku = querySupplierSku(newSkuId, newSupplierId);
                if (supplierSku == null) {
                    return null;
                }
                /**
                 * 判断库存是否 足够
                 */
                int totalQuantity = orderSkuDTO.getSpecNum().intValue() * orderSkuDTO.getAmount().intValue();
                Integer quantityResult = getSkuQuantity(supplierSku.getSkuId(), supplierSku.getId());
                if (quantityResult.intValue() <= 0) {
                    return null;
                }
                if (totalQuantity > quantityResult.intValue()) {
                    return null;
                }

                /**
                 * 判断规格数量
                 */
                SupplierSpec supplierSpec = querySuplierSpec(supplierSku.getId(), null, orderSkuDTO.getSpecNum());
                if (supplierSpec == null) {
                    return null;
                }
                /**
                 * 更换供应商仓库与原订单供应商Id 一样，报错
                 */
                if (orderSkuDTO.getSupplierId().equals(supplierSku.getSupplierId()) &&
                        orderSkuDTO.getSupplierSkuId().equals(supplierSku.getId())) {
                    return null;
                }
                supplierSpec.setSupplierSku(supplierSku);
                orderSkuDTO.setNewSupplierSpec(supplierSpec);
            }
        }

        return orderDTOList;
    }

    /**
     * @param @param  skuId
     * @param @param  supplierId
     * @param @return 参数
     * @return SupplierSku    返回类型
     * @throws
     * @Title: querySupplierSku
     * @Description: 查询供应商所有商品
     */
    private SupplierSku querySupplierSku(Long skuId, Long supplierId) {
        Wrapper<SupplierSku> wrapper = new EntityWrapper<>();
        wrapper.eq("sku_id", skuId);
        wrapper.eq("supplier_id", supplierId);
        // 0 上架
        wrapper.eq("sku_status", 0);
        //所有供应商的 商品
        List<SupplierSku> supplierSkuList = supplierSkuService.selectList(wrapper);
        if (supplierSkuList != null && !supplierSkuList.isEmpty()) {
            return supplierSkuList.get(0);
        }
        return null;
    }

    /**
     * @param @param  supplierSkuId
     * @param @param  specId
     * @param @param  specNum
     * @param @return 参数
     * @return SupplierSpec    返回类型
     * @throws
     * @Title: querySuplierSpec
     * @Description: 查询供应商商品规格
     */
    private SupplierSpec querySuplierSpec(Long supplierSkuId, Long specId, Integer specNum) {
        Wrapper<SupplierSpec> wrapper = new EntityWrapper<>();
        wrapper.eq("supplier_sku_id", supplierSkuId);
        if (null != specId) {
            wrapper.eq("spec_id", specId);
        }
        wrapper.eq("num", specNum);
        List<SupplierSpec> supplierSpecList = supplierSpecService.selectList(wrapper);
        if (supplierSpecList != null && !supplierSpecList.isEmpty()) {
            return supplierSpecList.get(0);
        }
        return null;
    }

    /**
     * @param @param  skuId
     * @param @param  supplierSkuId
     * @param @return 参数
     * @return Integer    返回类型
     * @throws
     * @Title: getSkuQuantity
     * @Description: 查询供应商可售库存
     */
    private Integer getSkuQuantity(Long skuId, Long supplierSkuId) {
        Wrapper<Inventory> inventoryWrapper = new EntityWrapper<>();
        inventoryWrapper.eq("sku_id", skuId);
        inventoryWrapper.eq("supplier_sku_id", supplierSkuId);
        //查所有供应商 库存
        List<Inventory> inventoryList = inventoryService.selectList(inventoryWrapper);
        if (inventoryList == null || inventoryList.isEmpty()) {
            return 0;
        }

        return inventoryList.get(0).getAvailableQuantity();
    }

    /**
     * @param @param  wrapper
     * @param @return 参数
     * @return String    返回类型
     * @throws
     * @Title: cancelOrderSCM
     * @Description: 待接单：SCM端小二取消订单
     */
    @RequestMapping("cancelOrderSCM")
    public String cancelOrderSCM(OrderIdsParasWrapper wrapper) {
        if ((null == wrapper) || (CollectionUtils.isEmpty(wrapper.getOrderIdsParasList()))) {
            return "请求参数无效";
        }
        try {
            JProfile.enter(this.getClass().getName() + "#cancelOrderSCM");

            OperatorInfoVO operatorInfoVO = new OperatorInfoVO();
            operatorInfoVO.setOperatorType(OperatorTypeEnum.SCM);
            wrapper.setOperatorInfoVO(operatorInfoVO);

            return scmOMServiceWrapper.cancelOrderSCM(wrapper);
        } finally {
            JProfile.release();
        }
    }

    //2.0，SCM 添加发货相关接口

    /**
     * @param @return
     * @return EasyResult<List                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               <                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               String>>
     * @throws
     * @Title: queryAllLogisticsCompanyNames
     * @Description: 查询所有快递公司名称
     */
    @RequestMapping("queryAllLogisticsCompanyNames")
    public List<LogisticsInfoVO> queryAllLogisticsCompanyNames() {
        EasyResult<List<LogisticsInfoVO>> result = sOrderManagedServiceWrapper.queryAllLogisticsCompanyNames();
        return result.getRows(); //前端框架不同，要求修改
    }

    /**
     * @param @param  supplierOrderUpdateVO
     * @param @return
     * @return EasyResult<Boolean>
     * @throws
     * @Title: sendClickByOrderNum
     * @Description: 点击发货或修改运单号
     */
    @RequestMapping(value = "sendClickByOrderNum")
    public EasyResult<Boolean> sendClickByOrderNum(SOrderUpdateVO sOrderUpdateVO) {
        if (StringUtils.isBlank(sOrderUpdateVO.getOrderNum())
                || CollectionUtils.isEmpty(sOrderUpdateVO.getLogisticsVOs())) {
            return null;
        }
        try {
            JProfile.enter(this.getClass().getName() + "#sendClickByOrderNum");

            List<LogisticsVO> logisticsVOs = sOrderUpdateVO.getLogisticsVOs().stream()
                    .filter(l -> ((StringUtils.isNotBlank(l.getLogisticsCode()))
                            && (StringUtils.isNotBlank(l.getLogisticsCompany()))
                            && (StringUtils.isNotBlank(l.getLogisticsNum()))))
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(logisticsVOs)) {
                return null;
            }
            sOrderUpdateVO.setLogisticsVOs(logisticsVOs);

            sOrderUpdateVO.setOperatorTypeEnum(OperatorTypeEnum.SCM);

            return sOrderManagedServiceWrapper.sendClickByOrderNum(sOrderUpdateVO);
        } finally {
            JProfile.release();
        }
    }

    /**
     * @param @param  queryLogisticsInfoVO
     * @param @return 参数
     * @return EasyResult<List                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               <                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               LogisticsInfoVO>>    返回类型
     * @throws
     * @Title: queryOrderLogisticsInfoByEdit
     * @Description: 修改运单号时，获取物流单信息，不需要查询物流详情
     */
    @RequestMapping("queryOrderLogisticsInfoByEdit")
    public EasyResult<List<LogisticsInfoVO>> queryOrderLogisticsInfoByEdit(QueryLogisticsInfoVO queryLogisticsInfoVO) {
        EasyResult<List<LogisticsInfoVO>> result = new EasyResult<>();
        if ((null == queryLogisticsInfoVO) || (StringUtils.isEmpty(queryLogisticsInfoVO.getOrderNum()))) {
            return result;
        }
        try {
            JProfile.enter(this.getClass().getName() + "#queryOrderLogisticsInfoByEdit");

            return sOrderManagedServiceWrapper.queryOrderLogisticsInfoByEdit(queryLogisticsInfoVO);
        } finally {
            JProfile.release();
        }
    }


    /***************************销售统计*************************************/

    /**
     * 销售查询
     *
     * @param pageUtil
     * @param condition
     * @return
     */
    @RequestMapping(value = "querySCMOrderSkuCount", method = RequestMethod.POST)
    @ResponseBody
    public EasyResult<?> querySCMOrderSkuCount(@ModelAttribute PageUtil pageUtil, @ModelAttribute $OrderSkuCondition condition) {

        JProfile.enter("into");
        //当前页
        if (pageUtil.getPage() == null || pageUtil.getPage() < 1) {
            return new EasyResult<>(BaseEnum.REQUEST_PARAM_IS_NULL.getCode(),
                    BaseEnum.REQUEST_PARAM_IS_NULL.getReason());
        }
        //每页显示多少条
        if (pageUtil.getRows() == null || pageUtil.getRows() < 1) {
            return new EasyResult<>(BaseEnum.REQUEST_PARAM_IS_NULL.getCode(),
                    BaseEnum.REQUEST_PARAM_IS_NULL.getReason());
        }

        if (null == condition || StringUtils.isEmpty(condition)) {
            return new EasyResult<>(BaseEnum.REQUEST_PARAM_IS_NULL.getCode(),
                    BaseEnum.REQUEST_PARAM_IS_NULL.getReason());
        }

        if (null == condition.getStartDate() || null == condition.getEndDate()) {
            Date now = new Date();
            condition.setEndDate(now);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(now);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            condition.setStartDate(calendar.getTime());
        } else {
            int days = (int) ((condition.getEndDate().getTime() - condition.getStartDate().getTime()) / (1000 * 3600 * 24));
            if (days > 30) {
                return new EasyResult<>(-1, "查询的时间段不能超过一个月");
            }
        }


        EasyResult<OrderSkuPage<OrderInfo, $OrderSkuDTO>> easyResult = new EasyResult<>();
        List list = new ArrayList();
        try {
            easyResult = (EasyResult<OrderSkuPage<OrderInfo, $OrderSkuDTO>>) queryOederSkuInfo(condition);
            if (null == easyResult.getTotal() || null == easyResult.getRows()) {
                return new EasyResult<>(0, list);
            }

            List<$OrderSkuDTO> datas = easyResult.getRows().getList();
            if (org.apache.commons.lang3.StringUtils.isNotBlank(condition.getSort())) {
                String[] orders = new String[]{
                        "totalPrice",
                        "actualPayPrice",
                        "amount",
                        "cost",
                        "couponPrice",
                        "discount",
                        "grossProfitPriceBack",
                        "grossProfitPricePre",
                        "grossProfitRateBack",
                        "grossProfitRatePre",
                };
                if (Arrays.asList(orders).contains(condition.getSort())) {
                    if ("amount".equals(condition.getSort())) {
                        if ("ASC".equals(condition.getOrder().toUpperCase())) {
                            datas = datas.stream().sorted(Comparator.comparing($OrderSkuDTO::getAmount)).collect(Collectors.toList());
                        } else if ("DESC".equals(condition.getOrder().toUpperCase())) {
                            datas = datas.stream().sorted(Comparator.comparing($OrderSkuDTO::getAmount).reversed()).collect(Collectors.toList());
                        }
                    } else {
                        Flector<$OrderSkuDTO> tFlector = new Flector<>($OrderSkuDTO.class);
                        if ("ASC".equals(condition.getOrder().toUpperCase())) {
                            datas.sort(new Comparator<$OrderSkuDTO>() {
                                @Override
                                public int compare($OrderSkuDTO o1, $OrderSkuDTO o2) {
                                    return ((Double) tFlector.getter(o1, condition.getSort())).compareTo(tFlector.getter(o2, condition.getSort()));
                                }
                            });
                        } else if ("DESC".equals(condition.getOrder().toUpperCase())) {
                            datas.sort(new Comparator<$OrderSkuDTO>() {
                                @Override
                                public int compare($OrderSkuDTO o1, $OrderSkuDTO o2) {
                                    return ((Double) tFlector.getter(o2, condition.getSort())).compareTo(tFlector.getter(o1, condition.getSort()));
                                }
                            });
                        }
                    }
                }
            }

            pageUtil.setTotal(datas.size());
            pageUtil.setAllPage(pageUtil.getTotal());
            if (pageUtil.getTotalPage() < pageUtil.getPage()) {
                pageUtil.setPage(pageUtil.getTotalPage());
            }
            if (pageUtil.getTotal() > pageUtil.getRows() * pageUtil.getPage()) {
                list = datas.subList(pageUtil.getRows() * (pageUtil.getPage() - 1), pageUtil.getRows() * pageUtil.getPage());
            } else {
                list = datas.subList(pageUtil.getRows() * (pageUtil.getPage() - 1), pageUtil.getTotal());
            }
            pageUtil.setItemStart(pageUtil.getPage() * (pageUtil.getRows() - 1) + 1);
            OrderSkuPage<OrderInfo, $OrderSkuDTO> rows = easyResult.getRows();
            rows.setList(list);
            easyResult.setTotal(datas.size());
            return easyResult;
        } catch (Exception e) {
            e.printStackTrace();
            return new EasyResult<>(BaseEnum.SYSTEM_ERROR.getCode(), BaseEnum.SYSTEM_ERROR.getReason());
        } finally {
            JProfile.release();
        }
    }


    private EasyResult<?> queryOederSkuInfo($OrderSkuCondition orderSku) {

        EasyResult<OrderSkuPage<OrderInfo, $OrderSkuDTO>> result = new EasyResult<>();
        if (org.apache.commons.lang.StringUtils.isNotEmpty(orderSku.getSkuName())) {
            if (org.apache.commons.lang.StringUtils.indexOfIgnoreCase(orderSku.getSkuName(), "NQ", 0) == 0) {
                orderSku.setItemNumber(orderSku.getSkuName());
                orderSku.setSkuName(null);
            }
        }
        EasyResult<List<$OrderSkuDTO>> easyResult = (EasyResult<List<$OrderSkuDTO>>) scmOMServiceWrapper.query$OrderSkuList(orderSku);

        if (0 == easyResult.getTotal()) {
            return result;
        }
        List<$OrderSkuDTO> data;
        if (null != (data = easyResult.getRows()) && data.size() > 0) {

            Flector<$OrderSkuDTO> flector = new Flector($OrderSkuDTO.class);
            //计算价格
            computeOrderSkuTotalPrice(result, data, flector, 1);
            //处理map
            String arg = "itemNumber,skuName,categoryName,brandName,tradeType,skuProp";
            ArrayList datas = dealOrderSku2Sum(data, arg, "companyName");
            OrderSkuPage<OrderInfo, $OrderSkuDTO> rows = result.getRows();
            rows.setList(datas);
            result.setTotal(datas.size());
        } else {
            OrderSkuPage<OrderInfo, $OrderSkuDTO> rows = result.getRows();
            result.setTotal(0);
            rows.setList(null);
        }
        return result;
    }

    private <T> void computeOrderSkuTotalPrice(EasyResult<OrderSkuPage<OrderInfo, T>> result, List<T> data, Flector<T> flector, int type) {

        JProfile.enter("computeOrderSkuTotalPrice");
        OrderSkuPage<OrderInfo, T> OrderInfo$OrderSkuDTOOrderSkuPage = new OrderSkuPage<>();
        if (CollectionUtils.isNotEmpty(data)) {
            double totalPrice = data.stream().mapToDouble(T -> {
                return flector.getter(T, "totalPrice");
            }).sum();
            double totalSellPrice = data.stream().mapToDouble(T -> {
                return flector.getter(T, "sellPrice");
            }).sum();
            int totalAmount = data.stream().mapToInt(T -> {
                return flector.getter(T, "amount");
            }).sum();
            double totalCost = data.stream().mapToDouble(T -> {
                return flector.getter(T, "cost");
            }).sum();
            double totalCouponPrice = data.stream().mapToDouble(T -> {
                return flector.getter(T, "couponPrice");
            }).sum();
            double totalActualPayPrice = data.stream().mapToDouble(T -> {
                return flector.getter(T, "actualPayPrice");
            }).sum();

            double totalDiscount = data.stream().mapToDouble(T -> {
                return flector.getter(T, "discount");
            }).sum();

            double totalGrossProfitPricePre = data.stream().mapToDouble(T -> {
                return flector.getter(T, "grossProfitPricePre");
            }).sum();
            double totalGrossProfitPriceBack = data.stream().mapToDouble(T -> {
                return flector.getter(T, "grossProfitPriceBack");
            }).sum();

            double totalNotaxGMV = 0.0;
            try {
                totalNotaxGMV = data.stream().mapToDouble(T -> {
                    return flector.getter(T, "notaxGMV");
                }).sum();
            } catch (Exception e) {
                totalNotaxGMV = 0.0;
            }


            double totalCouponPriceRate = 0.0;
            double totalBdGMV = 0.0;
            double totalCommercialGMV = 00;

            if (type == 0) {
                totalBdGMV = data.stream().mapToDouble(T -> {
                    return flector.getter(T, "bdGMV");
                }).sum();
                totalCommercialGMV = data.stream().mapToDouble(T -> {
                    return flector.getter(T, "commercialGMV");
                }).sum();

                if (0.0 == totalBdGMV) {
                    totalCouponPriceRate = 0.0;
                } else {
                    totalCouponPriceRate = new BigDecimal(totalCouponPrice).multiply(new BigDecimal(100.0)).divide(new BigDecimal(totalBdGMV), 2, BigDecimal.ROUND_HALF_UP).doubleValue();
                }
            }

            //非平均值算法
            double averageGrossProfitRatePre = new BigDecimal(totalGrossProfitPricePre).multiply(new BigDecimal(100.0)).divide(new BigDecimal(totalPrice), 2, BigDecimal.ROUND_HALF_UP).doubleValue();
            double averageGrossProfitRateBack = new BigDecimal(totalGrossProfitPriceBack).multiply(new BigDecimal(100.0)).divide(new BigDecimal(totalPrice), 2, BigDecimal.ROUND_HALF_UP).doubleValue();

            OrderInfo orderInfo = OrderInfo.builder()
                    .totalPrice(totalPrice)
                    .totalSellPrice(totalSellPrice)
                    .totalAmount(totalAmount)
                    .totalCost(totalCost)
                    .totalCouponPrice(totalCouponPrice)
                    .totalDiscount(totalDiscount)
                    .totalActualPayPrice(totalActualPayPrice)
                    .totalGrossProfitPricePre(totalGrossProfitPricePre)
                    .totalGrossProfitPriceBack(totalGrossProfitPriceBack)
                    .averageGrossProfitRatePre(averageGrossProfitRatePre)
                    .averageGrossProfitRateBack(averageGrossProfitRateBack)
                    .totalBdGMV(totalBdGMV)
                    .totalCommercialGMV(totalCommercialGMV)
                    .totalCouponPriceRate(totalCouponPriceRate)
                    .notaxGMV(totalNotaxGMV)
                    .build();
            OrderInfo$OrderSkuDTOOrderSkuPage.setData(orderInfo);
        }
        result.setRows(OrderInfo$OrderSkuDTOOrderSkuPage);
        JProfile.release();
    }

    private ArrayList dealOrderSku2Sum(List<$OrderSkuDTO> al, String... keyName) {

        JProfile.enter("dealOrderSkuInfo");
        LinkedHashMap $OrderSkuDTOMap = null;
        ArrayList $list = null;
        try {
            $OrderSkuDTOMap = DataGroupTools.groupClassifyList(al, keyName);

            Set<Map.Entry<String, HashMap<String, Object>>> $outSet = $OrderSkuDTOMap.entrySet();
            //\t\t一级
            $list = new ArrayList();
            for (Map.Entry<String, HashMap<String, Object>> $keyEntry : $outSet) {
                if ($keyEntry.getValue() == null) {
                    continue;
                }
                Map $map = com.bob.java.webapi.utils.JsonUtils.str2Map($keyEntry.getKey());
                $OrderSkuDTO $orderSku = new $OrderSkuDTO();
                BeanUtilEx.copyProperties($orderSku, $map);
                Set<Map.Entry<String, Object>> $$outSet = $keyEntry.getValue().entrySet();
                //\t\t二级
                List<$OrderSkuDTO> $$list = new ArrayList();
                for (Map.Entry<String, Object> $$keyEntry : $$outSet) {
                    if (null == $$keyEntry.getValue()) {
                        continue;
                    }
                    Map $$map = com.bob.java.webapi.utils.JsonUtils.str2Map($$keyEntry.getKey());
                    $OrderSkuDTO $$orderSku = new $OrderSkuDTO();
                    BeanUtilEx.copyProperties($$orderSku, $$map);
                    List<$OrderSkuDTO> $$$list = (List<$OrderSkuDTO>) $$keyEntry.getValue();

                    double totalPrice = BigDecimal.valueOf($$$list.stream().mapToDouble($OrderSkuDTO::getTotalPrice).sum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    double totalSellPrice = BigDecimal.valueOf($$$list.stream().mapToDouble($OrderSkuDTO::getSellPrice).sum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    int totalAmount = $$$list.stream().mapToInt($OrderSkuDTO::getAmount).sum();
                    double totalCost = BigDecimal.valueOf($$$list.stream().mapToDouble($OrderSkuDTO::getCost).sum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    double totalCouponPrice = BigDecimal.valueOf($$$list.stream().mapToDouble($OrderSkuDTO::getCouponPrice).sum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    double totalActualPayPrice = BigDecimal.valueOf($$$list.stream().mapToDouble($OrderSkuDTO::getActualPayPrice).sum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    double totalDiscount = BigDecimal.valueOf($$$list.stream().mapToDouble($OrderSkuDTO::getDiscount).sum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();

                    double totalGrossProfitPricePre = BigDecimal.valueOf($$$list.stream().mapToDouble($OrderSkuDTO::getGrossProfitPricePre).sum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    double totalGrossProfitPriceBack = BigDecimal.valueOf($$$list.stream().mapToDouble($OrderSkuDTO::getGrossProfitPriceBack).sum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();

                    //非平均值算法
                    double averageGrossProfitRatePre = new BigDecimal(totalGrossProfitPricePre).multiply(new BigDecimal(100.0)).divide(new BigDecimal(totalPrice), 2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    double averageGrossProfitRateBack = new BigDecimal(totalGrossProfitPriceBack).multiply(new BigDecimal(100.0)).divide(new BigDecimal(totalPrice), 2, BigDecimal.ROUND_HALF_UP).doubleValue();

                    $$orderSku.setTotalPrice(totalPrice);
                    $$orderSku.setSellPrice(totalSellPrice);
                    $$orderSku.setAmount(totalAmount);
                    $$orderSku.setCost(totalCost);
                    $$orderSku.setCouponPrice(totalCouponPrice);
                    $$orderSku.setDiscount(totalDiscount);
                    $$orderSku.setActualPayPrice(totalActualPayPrice);
                    $$orderSku.setGrossProfitPricePre(totalGrossProfitPricePre);
                    $$orderSku.setGrossProfitRatePre(averageGrossProfitRatePre);
                    $$orderSku.setGrossProfitPriceBack(totalGrossProfitPriceBack);
                    $$orderSku.setGrossProfitRateBack(averageGrossProfitRateBack);

                    $$list.add($$orderSku);
                }
                $orderSku.setChildren($$list);

                double totalPrice = BigDecimal.valueOf($$list.stream().mapToDouble($OrderSkuDTO::getTotalPrice).sum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                double totalSellPrice = BigDecimal.valueOf($$list.stream().mapToDouble($OrderSkuDTO::getSellPrice).sum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                int totalAmount = $$list.stream().mapToInt($OrderSkuDTO::getAmount).sum();
                double totalCost = BigDecimal.valueOf($$list.stream().mapToDouble($OrderSkuDTO::getCost).sum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                double totalCouponPrice = BigDecimal.valueOf($$list.stream().mapToDouble($OrderSkuDTO::getCouponPrice).sum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                double totalActualPayPrice = BigDecimal.valueOf($$list.stream().mapToDouble($OrderSkuDTO::getActualPayPrice).sum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                double totalDiscount = BigDecimal.valueOf($$list.stream().mapToDouble($OrderSkuDTO::getDiscount).sum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();

                double totalGrossProfitPricePre = BigDecimal.valueOf($$list.stream().mapToDouble($OrderSkuDTO::getGrossProfitPricePre).sum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                double totalGrossProfitPriceBack = BigDecimal.valueOf($$list.stream().mapToDouble($OrderSkuDTO::getGrossProfitPriceBack).sum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();

                //非平均值算法
                double averageGrossProfitRatePre = new BigDecimal(totalGrossProfitPricePre).multiply(new BigDecimal(100.0)).divide(new BigDecimal(totalPrice), 2, BigDecimal.ROUND_HALF_UP).doubleValue();
                double averageGrossProfitRateBack = new BigDecimal(totalGrossProfitPriceBack).multiply(new BigDecimal(100.0)).divide(new BigDecimal(totalPrice), 2, BigDecimal.ROUND_HALF_UP).doubleValue();

                $orderSku.setTotalPrice(totalPrice);
                $orderSku.setSellPrice(totalSellPrice);
                $orderSku.setAmount(totalAmount);
                $orderSku.setCost(totalCost);
                $orderSku.setCouponPrice(totalCouponPrice);
                $orderSku.setDiscount(totalDiscount);
                $orderSku.setActualPayPrice(totalActualPayPrice);
                $orderSku.setGrossProfitPricePre(totalGrossProfitPricePre);
                $orderSku.setGrossProfitRatePre(averageGrossProfitRatePre);
                $orderSku.setGrossProfitPriceBack(totalGrossProfitPriceBack);
                $orderSku.setGrossProfitRateBack(averageGrossProfitRateBack);

                $list.add($orderSku);
            }
        } catch (Exception e) {
            logger.error(e);
            return null;
        } finally {
            JProfile.release();
        }

        return $list;
    }


    /*********************************** GMV 统计 ******************************************/

    /**
     * GMV统计
     *
     * @param pageUtil
     * @param condition
     * @param type      (0：查询非明细，1：查询明细)
     * @return
     */
    @RequestMapping(value = "querySCMOrderSkuGmv", method = RequestMethod.POST)
    @ResponseBody
    public EasyResult<?> querySCMOrderSkuGmv(@ModelAttribute PageUtil pageUtil, @ModelAttribute OrderSkuGmvCondition condition, Integer type) {

        JProfile.enter("into");
        //当前页
        if (pageUtil.getPage() == null || pageUtil.getPage() < 1) {
            return new EasyResult<>(BaseEnum.REQUEST_PARAM_IS_NULL.getCode(),
                    BaseEnum.REQUEST_PARAM_IS_NULL.getReason());
        }
        //每页显示多少条
        if (pageUtil.getRows() == null || pageUtil.getRows() < 1) {
            return new EasyResult<>(BaseEnum.REQUEST_PARAM_IS_NULL.getCode(),
                    BaseEnum.REQUEST_PARAM_IS_NULL.getReason());
        }

        if (null == condition || StringUtils.isEmpty(condition)) {
            return new EasyResult<>(BaseEnum.REQUEST_PARAM_IS_NULL.getCode(),
                    BaseEnum.REQUEST_PARAM_IS_NULL.getReason());
        }

        if (null == condition.getBeginTime() || null == condition.getEndTime()) {
            Date now = new Date();
            condition.setEndTime(now);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(now);
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            condition.setBeginTime(calendar.getTime());
        } else {
            int days = (int) ((condition.getBeginTime().getTime() - condition.getEndTime().getTime()) / (1000 * 3600 * 24));
            if (days > 90) {
                return null;
            }
        }


        EasyResult<OrderSkuPage<OrderInfo, OrderSkuGmvDto>> easyResult = new EasyResult<>();
        List list = new ArrayList();
        List<OrderSkuGmvDto> datas = null;
        try {
            List<OrderSkuGmvDto> gmvDtos = scmOMServiceWrapper.queryOrderSku$GMV(condition, type);
            if (CollectionUtils.isEmpty(gmvDtos)) {
                return new EasyResult<>(0, list);
            }
            Flector<OrderSkuGmvDto> flector = new Flector(OrderSkuGmvDto.class);
            if (1 == (type % 2)) {
                computeOrderSkuTotalPrice(easyResult, gmvDtos, flector, (type % 2));
                datas = dealGmvSum4Detail(gmvDtos, "topCatId,topCatName,tradeType,tradeName,leadingOfficial,leadingOfficialName");
            } else {
                datas = dealGmvSum(gmvDtos, "createTime", "groupId,groupName");
                computeOrderSkuTotalPrice(easyResult, datas, flector, (type % 2));
            }

            pageUtil.setTotal(datas.size());
            pageUtil.setAllPage(pageUtil.getTotal());
            if (pageUtil.getTotalPage() < pageUtil.getPage()) {
                pageUtil.setPage(pageUtil.getTotalPage());
            }
            if (pageUtil.getTotal() > pageUtil.getRows() * pageUtil.getPage()) {
                list = datas.subList(pageUtil.getRows() * (pageUtil.getPage() - 1), pageUtil.getRows() * pageUtil.getPage());
            } else {
                list = datas.subList(pageUtil.getRows() * (pageUtil.getPage() - 1), pageUtil.getTotal());
            }
            pageUtil.setItemStart(pageUtil.getPage() * (pageUtil.getRows() - 1) + 1);

            easyResult.getRows().setList(list);
            easyResult.setTotal(datas.size());
            return easyResult;
        } catch (Exception e) {
            e.printStackTrace();
            return new EasyResult<>(BaseEnum.SYSTEM_ERROR.getCode(), BaseEnum.SYSTEM_ERROR.getReason());
        } finally {
            JProfile.release();
        }
    }

    private List dealGmvSum4Detail(List<OrderSkuGmvDto> al, String... keyName) {

        JProfile.enter("dealGmvSum4Detail");
        LinkedHashMap $OrderSkuDTOMap = null;
        ArrayList $list = null;
        try {
            $OrderSkuDTOMap = DataGroupTools.groupClassifyList(al, keyName);

            Set<Map.Entry<String, HashMap<String, Object>>> $outSet = $OrderSkuDTOMap.entrySet();
            //\t\t一级
            $list = new ArrayList();
            for (Map.Entry<String, HashMap<String, Object>> $$keyEntry : $outSet) {
                if ($$keyEntry.getValue() == null) {
                    continue;
                }
                Map $map = com.bob.java.webapi.utils.JsonUtils.str2Map($$keyEntry.getKey());
                OrderSkuGmvDto orderSku$Gmv = new OrderSkuGmvDto();
                BeanUtilEx.copyProperties(orderSku$Gmv, $map);
                //\t\t二级
                Map $$map = com.bob.java.webapi.utils.JsonUtils.str2Map($$keyEntry.getKey());
                OrderSkuGmvDto $$orderSku = new OrderSkuGmvDto();
                BeanUtilEx.copyProperties($$orderSku, $$map);
                List<OrderSkuGmvDto> $$$list = (List<OrderSkuGmvDto>) $$keyEntry.getValue();

                try {
                    double totalPrice = BigDecimal.valueOf($$$list.stream().mapToDouble(OrderSkuGmvDto::getTotalPrice).sum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    double totalSellPrice = BigDecimal.valueOf($$$list.stream().mapToDouble(OrderSkuGmvDto::getSellPrice).sum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    int totalAmount = $$$list.stream().mapToInt(OrderSkuGmvDto::getAmount).sum();
                    double totalCost = BigDecimal.valueOf($$$list.stream().mapToDouble(OrderSkuGmvDto::getCost).sum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    double totalCouponPrice = BigDecimal.valueOf($$$list.stream().mapToDouble(OrderSkuGmvDto::getCouponPrice).sum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    double totalActualPayPrice = BigDecimal.valueOf($$$list.stream().mapToDouble(OrderSkuGmvDto::getActualPayPrice).sum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    double totalDiscount = BigDecimal.valueOf($$$list.stream().mapToDouble(OrderSkuGmvDto::getDiscount).sum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    double totalNotaxGMV = BigDecimal.valueOf($$$list.stream().mapToDouble(OrderSkuGmvDto::getNotaxGMV).sum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();

                    double totalGrossProfitPricePre = BigDecimal.valueOf($$$list.stream().mapToDouble(OrderSkuGmvDto::getGrossProfitPricePre).sum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    double totalGrossProfitPriceBack = BigDecimal.valueOf($$$list.stream().mapToDouble(OrderSkuGmvDto::getGrossProfitPriceBack).sum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    //非平均值算法
                    double averageGrossProfitRatePre = new BigDecimal(totalGrossProfitPricePre).multiply(new BigDecimal(100.0)).divide(new BigDecimal(totalNotaxGMV), 2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    double averageGrossProfitRateBack = new BigDecimal(totalGrossProfitPriceBack).multiply(new BigDecimal(100.0)).divide(new BigDecimal(totalNotaxGMV), 2, BigDecimal.ROUND_HALF_UP).doubleValue();

                    $$orderSku.setTotalPrice(totalPrice);
                    $$orderSku.setSellPrice(totalSellPrice);
                    $$orderSku.setAmount(totalAmount);
                    $$orderSku.setCost(totalCost);
                    $$orderSku.setCouponPrice(totalCouponPrice);
                    $$orderSku.setDiscount(totalDiscount);
                    $$orderSku.setNotaxGMV(totalNotaxGMV);
                    $$orderSku.setActualPayPrice(totalActualPayPrice);
                    $$orderSku.setGrossProfitPricePre(totalGrossProfitPricePre);
                    $$orderSku.setGrossProfitRatePre(averageGrossProfitRatePre);
                    $$orderSku.setGrossProfitPriceBack(totalGrossProfitPriceBack);
                    $$orderSku.setGrossProfitRateBack(averageGrossProfitRateBack);

                    $list.add($$orderSku);
                } catch (Exception e) {
                    return new ArrayList();
                }
            }
        } catch (Exception e) {
            logger.error(e);
            /*return java.util.Arrays.asList(new Object[0]);*/
            return new ArrayList();
        } finally {
            JProfile.release();
        }

        return $list;
    }


    private List dealGmvSum(List<OrderSkuGmvDto> al, String... keyName) {

        JProfile.enter("dealGmvSum");
        LinkedHashMap $OrderSkuDTOMap = null;
        List<OrderSkuGmvDto> $list = null;
        try {
            $OrderSkuDTOMap = DataGroupTools.groupClassifyList(al, keyName);
            al.clear();
            al = null;
            Set<Map.Entry<String, HashMap<String, Object>>> $outSet = $OrderSkuDTOMap.entrySet();
            //\t\t一级
            $list = new ArrayList();
            for (Map.Entry<String, HashMap<String, Object>> $keyEntry : $outSet) {
                if ($keyEntry.getValue() == null) {
                    continue;
                }
                Map $map = com.bob.java.webapi.utils.JsonUtils.str2Map($keyEntry.getKey());
                OrderSkuGmvDto $orderSku = new OrderSkuGmvDto();
                BeanUtilEx.copyProperties($orderSku, $map);
                Set<Map.Entry<String, Object>> $$outSet = $keyEntry.getValue().entrySet();
                //\t\t二级
                List<OrderSkuGmvDto> $$list = new ArrayList();
                BigDecimal bdGMV = new BigDecimal(0.0);
                BigDecimal commercialGMV = new BigDecimal(0.0);
                for (Map.Entry<String, Object> $$keyEntry : $$outSet) {
                    if (null == $$keyEntry.getValue()) {
                        continue;
                    }
                    Map $$map = com.bob.java.webapi.utils.JsonUtils.str2Map($$keyEntry.getKey());
                    OrderSkuGmvDto $$orderSku = new OrderSkuGmvDto();
                    BeanUtilEx.copyProperties($$orderSku, $$map);
                    List<OrderSkuGmvDto> $$$list = (List<OrderSkuGmvDto>) $$keyEntry.getValue();

                    double totalPrice = BigDecimal.valueOf($$$list.stream().mapToDouble(OrderSkuGmvDto::getTotalPrice).sum()).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
                    double totalNotaxGMV = BigDecimal.valueOf($$$list.stream().mapToDouble(OrderSkuGmvDto::getNotaxGMV).sum()).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
                    double totalSellPrice = BigDecimal.valueOf($$$list.stream().mapToDouble(OrderSkuGmvDto::getSellPrice).sum()).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
                    int totalAmount = $$$list.stream().mapToInt(OrderSkuGmvDto::getAmount).sum();
                    double totalCost = BigDecimal.valueOf($$$list.stream().mapToDouble(OrderSkuGmvDto::getCost).sum()).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
                    double totalCouponPrice = BigDecimal.valueOf($$$list.stream().mapToDouble(OrderSkuGmvDto::getCouponPrice).sum()).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
                    double totalActualPayPrice = BigDecimal.valueOf($$$list.stream().mapToDouble(OrderSkuGmvDto::getActualPayPrice).sum()).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
                    double totalDiscount = BigDecimal.valueOf($$$list.stream().mapToDouble(OrderSkuGmvDto::getDiscount).sum()).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
                    double totalGrossProfitPricePre = BigDecimal.valueOf($$$list.stream().mapToDouble(OrderSkuGmvDto::getGrossProfitPricePre).sum()).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
                    double totalGrossProfitPriceBack = BigDecimal.valueOf($$$list.stream().mapToDouble(OrderSkuGmvDto::getGrossProfitPriceBack).sum()).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
                    //非平均值算法
                    double averageGrossProfitRatePre = new BigDecimal(totalGrossProfitPricePre).multiply(new BigDecimal(100.0)).divide(new BigDecimal(totalNotaxGMV), 4, BigDecimal.ROUND_HALF_UP).doubleValue();
                    double averageGrossProfitRateBack = new BigDecimal(totalGrossProfitPriceBack).multiply(new BigDecimal(100.0)).divide(new BigDecimal(totalNotaxGMV), 4, BigDecimal.ROUND_HALF_UP).doubleValue();

                    $$orderSku.setTotalPrice(totalPrice);
                    $$orderSku.setNotaxGMV(totalNotaxGMV);
                    $$orderSku.setSellPrice(totalSellPrice);
                    $$orderSku.setAmount(totalAmount);
                    $$orderSku.setCost(totalCost);
                    $$orderSku.setCouponPrice(totalCouponPrice);
                    $$orderSku.setDiscount(totalDiscount);
                    /*$$orderSku.setChargePrice(totalChargePrice);*/
                    $$orderSku.setActualPayPrice(totalActualPayPrice);
                    $$orderSku.setGrossProfitPricePre(totalGrossProfitPricePre);
                    $$orderSku.setGrossProfitRatePre(averageGrossProfitRatePre);
                    $$orderSku.setGrossProfitPriceBack(totalGrossProfitPriceBack);
                    $$orderSku.setGrossProfitRateBack(averageGrossProfitRateBack);
                    int i = Math.toIntExact($$orderSku.getGroupId());
                    switch (i) {
                        case 1:
                            bdGMV = bdGMV.add(new BigDecimal(totalActualPayPrice));
                            break;
                        case 2:
                            commercialGMV = commercialGMV.add(new BigDecimal(totalActualPayPrice));
                            break;
                        default:
                            break;
                    }
                    $$list.add($$orderSku);
                }

                double totalPrice = BigDecimal.valueOf($$list.stream().mapToDouble(OrderSkuGmvDto::getTotalPrice).sum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                double totalNotaxGMV = BigDecimal.valueOf($$list.stream().mapToDouble(OrderSkuGmvDto::getNotaxGMV).sum()).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
                double totalSellPrice = BigDecimal.valueOf($$list.stream().mapToDouble(OrderSkuGmvDto::getSellPrice).sum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                int totalAmount = $$list.stream().mapToInt(OrderSkuGmvDto::getAmount).sum();
                double totalCost = BigDecimal.valueOf($$list.stream().mapToDouble(OrderSkuGmvDto::getCost).sum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                double totalCouponPrice = BigDecimal.valueOf($$list.stream().mapToDouble(OrderSkuGmvDto::getCouponPrice).sum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                double totalActualPayPrice = BigDecimal.valueOf($$list.stream().mapToDouble(OrderSkuGmvDto::getActualPayPrice).sum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                double totalDiscount = BigDecimal.valueOf($$list.stream().mapToDouble(OrderSkuGmvDto::getDiscount).sum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                double totalGrossProfitPricePre = BigDecimal.valueOf($$list.stream().mapToDouble(OrderSkuGmvDto::getGrossProfitPricePre).sum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                double totalGrossProfitPriceBack = BigDecimal.valueOf($$list.stream().mapToDouble(OrderSkuGmvDto::getGrossProfitPriceBack).sum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                //非平均值算法
                double averageGrossProfitRatePre = new BigDecimal(totalGrossProfitPricePre).multiply(new BigDecimal(100.0)).divide(new BigDecimal(totalNotaxGMV), 2, BigDecimal.ROUND_HALF_UP).doubleValue();
                double averageGrossProfitRateBack = new BigDecimal(totalGrossProfitPriceBack).multiply(new BigDecimal(100.0)).divide(new BigDecimal(totalNotaxGMV), 2, BigDecimal.ROUND_HALF_UP).doubleValue();

                $orderSku.setTotalPrice(totalPrice);
                $orderSku.setNotaxGMV(totalNotaxGMV);
                $orderSku.setSellPrice(totalSellPrice);
                $orderSku.setAmount(totalAmount);
                $orderSku.setCost(totalCost);
                $orderSku.setCouponPrice(totalCouponPrice);
                $orderSku.setDiscount(totalDiscount);
                $orderSku.setActualPayPrice(totalActualPayPrice);
                $orderSku.setGrossProfitPricePre(totalGrossProfitPricePre);
                $orderSku.setGrossProfitRatePre(averageGrossProfitRatePre);
                $orderSku.setGrossProfitPriceBack(totalGrossProfitPriceBack);
                $orderSku.setGrossProfitRateBack(averageGrossProfitRateBack);
                $orderSku.setBdGMV(bdGMV.doubleValue());
                $orderSku.setCommercialGMV(commercialGMV.doubleValue());
                if (0 == $orderSku.getBdGMV()) {
                    $orderSku.setCouponPriceRate(0.0);
                } else {
                    $orderSku.setCouponPriceRate(new BigDecimal(totalCouponPrice).multiply(new BigDecimal(100.0)).divide(bdGMV, 2, BigDecimal.ROUND_HALF_UP).doubleValue());
                }
                $list.add($orderSku);
            }
        } catch (Exception e) {
            logger.error(e);
            return new ArrayList();
        } finally {
            JProfile.release();
        }
        return $list;
    }


    @RequestMapping(value = "externalOrderSimpleByPage", method = RequestMethod.GET)
    public EasyResult<List<ExternalOrderVo>> externalOrderSimpleByPage(ExternalOrderQueryVO orderQueryVO) {
        if ((null == orderQueryVO.getPage()) || (orderQueryVO.getPage() < 1)) {
            orderQueryVO.setPage(1);
        }

        if ((null == orderQueryVO.getRows()) || (orderQueryVO.getRows() < 1)) {
            orderQueryVO.setRows(10);
        }

        try {

            EasyResult<List<ExternalOrderVo>> result = externalOrderService.queryExternalOrderByPage(orderQueryVO);

            /*checkPrivacySCMOrderSimpleVO(result);*/

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return new EasyResult(0, null);
        } finally {
            JProfile.release();
        }
    }

    /**
     * @param
     * @return com.nqtown.util.FinalResult
     * @description: 移动订单相关数据到历史记录中
     * @author cjiang
     * @date 2018/11/20 15:35
     */
    @RequestMapping("moveOrderInfosToHistory")
    public FinalResult moveOrderInfosToHistory(MoveOrderInfosVO moveOrderInfosVO) {
        if ((null == moveOrderInfosVO)
                || (null == moveOrderInfosVO.getStartTime())
                || (null == moveOrderInfosVO.getEndTime())) {
            FinalResult result = new FinalResult();
            result.setReason("参数无效");
            return result;
        } else {
            Calendar cal1 = Calendar.getInstance();
            cal1.setTime(moveOrderInfosVO.getStartTime());
            cal1.set(Calendar.HOUR_OF_DAY, 0);
            cal1.set(Calendar.MINUTE, 0);
            cal1.set(Calendar.SECOND, 0);
            moveOrderInfosVO.setStartTime(cal1.getTime());

            cal1.setTime(moveOrderInfosVO.getEndTime());
            cal1.set(Calendar.HOUR_OF_DAY, 23);
            cal1.set(Calendar.MINUTE, 59);
            cal1.set(Calendar.SECOND, 59);
            moveOrderInfosVO.setEndTime(cal1.getTime());
            if ((((new Date()).getTime() - moveOrderInfosVO.getEndTime().getTime()) / (1000 * 3600 * 24)) <= 90) {
                FinalResult result = new FinalResult();
                result.setReason("只能移动三个月之前的数据");
                return result;
            }
            if (((moveOrderInfosVO.getEndTime().getTime() - moveOrderInfosVO.getStartTime().getTime()) / (1000 * 3600 * 24)) >= 32) {
                FinalResult result = new FinalResult();
                result.setReason("日期间隔太长，最好不超过一个月");
                return result;
            }
            return scmOMServiceWrapper.moveOrderInfosToHistory(moveOrderInfosVO);
        }
    }

    /**
     * @param
     * @return com.nqtown.util.FinalResult
     * @description: 停止某个正在迁移的任务
     * @author cjiang
     * @date 2018/11/20 18:00
     */
    @RequestMapping("cancelMoveOrderInfosToHistory")
    public FinalResult cancelMoveOrderInfosToHistory(MoveOrderInfosVO moveOrderInfosVO) {
        if ((null == moveOrderInfosVO)
                || (null == moveOrderInfosVO.getId())) {
            FinalResult result = new FinalResult();
            result.setReason("参数无效");
            return result;
        } else {
            return scmOMServiceWrapper.cancelMoveOrderInfosToHistory(moveOrderInfosVO);
        }
    }

    /**
     * @param
     * @return com.nqtown.util.EasyResult<java.util.List       <       com.nqtown.order.dto.OrderMoveLogVO>>
     * @description:迁移记录查询
     * @author cjiang
     * @date 2018/11/22 13:23
     */
    @RequestMapping("queryOrderMoveLogByPage")
    public EasyResult<List<OrderMoveLogVO>> queryOrderMoveLogByPage(Integer page, Integer rows) {
        if ((null == page) || (page < 1)) {
            page = 1;
        }
        if ((null == rows) || (rows < 1)) {
            rows = 10;
        }

        return scmOMServiceWrapper.queryOrderMoveLogByPage(page, rows);
    }

    /**
     * scm后台确认收货
     *
     * @return com.nqtown.util.FinalResult<?>
     * @Date 2019-02-19 11:15
     * @Author xcl
     **/
    @RequestMapping("scmConfirmReceiverItem")
    public FinalResult<?> scmConfirmReceiverItem(Long id, Long orderNum) {
        try {

            if ((null == id) || (null == orderNum)) {
                return FinalResult.of(false, BaseEnum.REQUEST_PARAM_IS_NULL.getCode(), BaseEnum.REQUEST_PARAM_IS_NULL.getReason());
            }

            OrderIdsParas orderIdsParas = new OrderIdsParas();
            orderIdsParas.setId(id);
            orderIdsParas.setOrderNum(orderNum);
            List<OrderIdsParas> orderIdsParasList = new ArrayList<>();
            orderIdsParasList.add(orderIdsParas);

            OperatorInfoVO operatorInfoVO = new OperatorInfoVO();
            operatorInfoVO.setOperatorType(OperatorTypeEnum.SCM);

            OrderIdsParasWrapper wrapper = new OrderIdsParasWrapper();
            wrapper.setOrderIdsParasList(orderIdsParasList);
            wrapper.setOperatorInfoVO(operatorInfoVO);

            EasyResult<String> result = orderCommonServiceWrapper.confirmReceiverItem(wrapper);
            if (result == null || result.getRows() == null) {
                return FinalResult.of(false, BaseEnum.SYSTEM_ERROR.getCode(), BaseEnum.SYSTEM_ERROR.getReason());
            }
            return FinalResult.of(true, BaseEnum.SUCCESS.getCode(), BaseEnum.SUCCESS.getReason());
        } catch (Exception e) {
            logger.error("SCMOrderManagedController scmConfirmReceiverItem error! msg : " + e.getMessage());
            return FinalResult.of(false, BaseEnum.SYSTEM_ERROR.getCode(), BaseEnum.SYSTEM_ERROR.getReason());
        }
    }

}

