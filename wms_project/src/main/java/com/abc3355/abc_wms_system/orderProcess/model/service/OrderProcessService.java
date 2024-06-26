package com.abc3355.abc_wms_system.orderProcess.model.service;

import com.abc3355.abc_wms_system.orderProcess.model.dao.OrderProcessMapper;
import com.abc3355.abc_wms_system.orderProcess.model.dto.*;
import com.mysql.cj.x.protobuf.MysqlxCrud;
import org.apache.ibatis.session.SqlSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.abc3355.abc_wms_system.common.MyBatisTemplate.getSqlSession;

public class OrderProcessService {
    public List<OrderListResDTO> searchAllOrders() {
        SqlSession sqlSession = getSqlSession();
        OrderProcessMapper mapper = sqlSession.getMapper(OrderProcessMapper.class);
        List<OrderListResDTO> orderList = mapper.selectAllOrders();
        sqlSession.close();
        return orderList;
    }

    public List<OrderListResDTO> searchOrdersByName(String name) {
        SqlSession sqlSession = getSqlSession();
        OrderProcessMapper mapper = sqlSession.getMapper(OrderProcessMapper.class);
        List<OrderListResDTO> orderList = mapper.selectOrdersByName(name);
        sqlSession.close();
        return orderList;
    }

    public List<OrderListResDTO> searchOrdersByStatus(String status) {
        SqlSession sqlSession = getSqlSession();
        OrderProcessMapper mapper = sqlSession.getMapper(OrderProcessMapper.class);
        List<OrderListResDTO> orderList = mapper.selectOrdersByStatus(status);
        sqlSession.close();
        return orderList;
    }

    public List<OrderListResDTO> searchMyOrders(String userId) {
        SqlSession sqlSession = getSqlSession();
        OrderProcessMapper mapper = sqlSession.getMapper(OrderProcessMapper.class);
        List<OrderListResDTO> orderList = mapper.selectMyOrders(userId);
        sqlSession.close();
        return orderList;
    }

    public List<OrderListResDTO> searchMyOrdersByStatus(String userId, int status) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("status", status);
        SqlSession sqlSession = getSqlSession();
        OrderProcessMapper mapper = sqlSession.getMapper(OrderProcessMapper.class);
        List<OrderListResDTO> orderList = mapper.selectMyOrdersByStatus(map);
        sqlSession.close();
        return orderList;
    }

    public boolean cancelOrder(OrderUpdateReqDTO orderUpdateReqDTO) {
        SqlSession sqlSession = getSqlSession();
        OrderProcessMapper mapper = sqlSession.getMapper(OrderProcessMapper.class);
        int result1 = mapper.updateOrderStatus(orderUpdateReqDTO.getOrderNo(), 4);
        int result2 = mapper.updateOrderString(orderUpdateReqDTO.getOrderNo(), orderUpdateReqDTO.getOrderDetail());
        if(result1 > 0) {
            sqlSession.commit();
        } else {
            sqlSession.rollback();
        }
        sqlSession.close();
        return result1 > 0;
    }

    public boolean processOrderShipment(OrderUpdateReqDTO orderUpdateReqDTO) {
        SqlSession sqlSession = getSqlSession();
        OrderProcessMapper mapper = sqlSession.getMapper(OrderProcessMapper.class);
        List<OrderDetailResDTO> orderDetails = mapper.selectOrderDetails(orderUpdateReqDTO.getOrderNo());
        int updateInventoryResult = 0;

        if(orderDetails.isEmpty()) return false;

        for(OrderDetailResDTO i : orderDetails) {
            int productNo = i.getProductNo();
            int odAmount = i.getOdAmount();

            Integer stockAmount = mapper.getStockAmount(productNo);
            if(stockAmount == null || stockAmount < odAmount) {
                return false;
            }
        }

        for(OrderDetailResDTO i : orderDetails) {
            Map<String, Integer> map = new HashMap<>();
            map.put("productNo", i.getProductNo());
            map.put("orderNo", i.getOrderNo());
            map.put("odAmount", i.getOdAmount());

            updateInventoryResult = updateInventoryResult + mapper.updateHQInventoryAmount(map);
        }
        int updateOrderResult = mapper.updateOrderStatus(orderUpdateReqDTO.getOrderNo(), 2);

        if(updateOrderResult > 0 && updateInventoryResult == orderDetails.size()) {
            sqlSession.commit();
        } else
            sqlSession.rollback();

        sqlSession.close();
        return (updateOrderResult > 0 && updateInventoryResult == orderDetails.size());
    }

    public boolean confirmOrder(OrderUpdateReqDTO orderUpdateReqDto) {
        SqlSession sqlSession = getSqlSession();
        OrderProcessMapper mapper = sqlSession.getMapper(OrderProcessMapper.class);
        List<OrderDetailResDTO> orderDetails = mapper.selectOrderDetails(orderUpdateReqDto.getOrderNo());
        int updateInventoryResult = 0, updateOrderResult;


        for(OrderDetailResDTO i : orderDetails) {
            Map<String, Integer> map = new HashMap<>();
            map.put("productNo", i.getProductNo());
            map.put("orderNo", i.getOrderNo());
            map.put("odAmount", i.getOdAmount());

            int checkInventory = mapper.selectInventoryData(map);

            if(checkInventory != 0) {updateInventoryResult = updateInventoryResult + mapper.updateBranchesInventoryAmount(map);}
            else {updateInventoryResult = updateInventoryResult + mapper.insertBranchesInventoryAmount(map);}
        }

        updateOrderResult = mapper.updateOrderStatus(orderUpdateReqDto.getOrderNo(), 3);

        if(updateOrderResult > 0 && updateInventoryResult == orderDetails.size()) sqlSession.commit();
        else sqlSession.rollback();

        sqlSession.close();
        return (updateOrderResult > 0 && updateInventoryResult == orderDetails.size());
    }

    // ---------------------------------------------------------------------------------------- //

    public List<GetOrderDetailDTO> getOrderDetail(OrderUpdateReqDTO orderUpdateReqDto) {
        SqlSession sqlSession = getSqlSession();
        OrderProcessMapper mapper = sqlSession.getMapper(OrderProcessMapper.class);
        List<GetOrderDetailDTO> orderDetails = mapper.getOrderDetails(orderUpdateReqDto.getOrderNo());
        sqlSession.close();
        return orderDetails;
    }

    public boolean deleteOrderDetail(GetOrderDetailDTO orderDetail) {
        SqlSession sqlSession = getSqlSession();
        OrderProcessMapper mapper = sqlSession.getMapper(OrderProcessMapper.class);

        Map<String, Object> map = new HashMap<>();

        map.put("odNo", orderDetail.getOdNo());
        map.put("odAmount", orderDetail.getOdAmount());
        map.put("orderNo", orderDetail.getOrderNo());
        map.put("productName", orderDetail.getProductName());

        int updateResult = mapper.updateOrderPriceMinus(map);
        int deleteResult = mapper.deleteOrderDetail(map);

        if(deleteResult > 0 && updateResult > 0) sqlSession.commit();
        else sqlSession.rollback();

        return deleteResult > 0 && updateResult > 0;
    }

    public boolean insertOrderDetail(GetOrderDetailDTO orderDetail) {
        SqlSession sqlSession = getSqlSession();
        OrderProcessMapper mapper = sqlSession.getMapper(OrderProcessMapper.class);
        int insertResult = 0;

        Map<String, Integer> map = new HashMap<>();

        map.put("odAmount", orderDetail.getOdAmount());
        map.put("orderNo", orderDetail.getOrderNo());
        map.put("productNo", orderDetail.getProductNo());


        if(mapper.checkOrderDetail(map) > 0) {
            insertResult = mapper.updateOrderDetail(map);
        } else {
            insertResult = mapper.insertOrderDetail(map);
        }

        int updateResult = mapper.updateOrderPricePlus(map);

        if(insertResult > 0 && updateResult > 0) {
            sqlSession.commit();
        } else {
            sqlSession.rollback();
        }
        return insertResult > 0 && updateResult > 0;
    }
}

