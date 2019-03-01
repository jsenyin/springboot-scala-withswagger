package com.bob.java.webapi.service;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.nqtown.member.service.MemberService;
import com.nqtown.member.service.SupplierService;
import com.nqtown.model.Sku;
import com.nqtown.model.statistics.BuriedPointSkuRecord;
import com.nqtown.order.service.*;
import com.nqtown.service.*;
import com.nqtown.si.service.DataFrameService;
import com.nqtown.util.FinalResult;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author jsen.yin [jsen.yin@gmail.com]
 * 2019-03-01
 * @Description: <p></p>
 */
@Service
public class DataService {

    protected Logger logger = LogManager.getLogger(getClass());

    @Autowired
    private ScmOMServiceWrapper scmOMServiceWrapper;

    @Autowired
    private OrderCommonServiceWrapper orderCommonServiceWrapper;

    @Autowired
    private MemberService memberService;

    @Autowired
    private SupplierService supplierService;

    @Resource(name = "orderService")
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

    @Autowired
    private DataFrameService dataFrameService;


    public FinalResult<List<Sku>> getSkuList() {

        Wrapper<Sku> skuWrapper = new EntityWrapper<>();
        /*Wrapper<BuriedPointSkuRecord> buriedPointSkuRecordWrapper = new EntityWrapper<>();
        Wrapper<OrderAfterSales> orderAfterSalesWrapper = new EntityWrapper<>();
        Wrapper<Order> orderWrapper = new EntityWrapper<>();
        Wrapper<OrderSku> orderSkuWrapper = new EntityWrapper<>();
        Wrapper<SupplierSku> supplierSkuWrapper = new EntityWrapper<>();
        Wrapper<Inventory> inventoryWrapper = new EntityWrapper<>();
        Wrapper<Category> categoryWrapper = new EntityWrapper<>();
        Wrapper<Brand> brandWrapper = new EntityWrapper<>();*/

        List<Sku> skus = dataFrameService.querySkuList(skuWrapper);

        FinalResult result = FinalResult.success();
        result.setData(skus);
        return result;
    }

    public FinalResult<List<BuriedPointSkuRecord>> getPointSku() {
        Wrapper<BuriedPointSkuRecord> wrapper = new EntityWrapper<>();
        List<BuriedPointSkuRecord> buriedPointSkuRecords = dataFrameService.queryBuriedPointSkuRecordList(wrapper);

        /*SELECT sku_id, SUM (click_num) cnt
        from buried_point_sku_record point
        GROUP BY sku_id
        HAVING cnt >=100*/
        FinalResult result = FinalResult.success();
        result.setData(buriedPointSkuRecords);
        return result;
    }

}
