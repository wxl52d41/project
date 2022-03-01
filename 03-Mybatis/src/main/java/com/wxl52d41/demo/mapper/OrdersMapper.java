package com.wxl52d41.demo.mapper;

import com.wxl52d41.demo.entity.Orders;

import java.util.List;

public interface OrdersMapper {

    /**
     * # 查询id=?的订单以及对应的用户
     * select * from orders o inner join user u
     * on o.uid = u.id
     * where o.id = ?;
     * <p>
     * 1. 参数类型: Integer id
     * 2. 返回值类型  Orders
     */
    Orders findOrderByIdWithUser(Integer id);  //xml返回用resultMap

    /**
     * 直接用orders作为返回对象，user会映射不出来导致结果不符合预期
     * 正确使用：将user中的属性复制到orders中即可
     */
    Orders findOrderByIdWithUser2(Integer id);   //xml返回用resultType,



    /**
       # 查询id=?的订单以及对应的用户
         select * from orders o inner join user u
         on o.uid = u.id
         where o.id = ?;
        1. 参数类型: Integer id
        2. 返回值类型  Orders
       # 修改成嵌套查询
           1. 先查订单id=1 的订单信息  (uid=41)
               select * from orders where id = ? // 订单id
           2. 查uid=41的用户信息
               select * from user where id = ?  // 用户id
           3. 两个结果嵌套一起: 映射到 orders
   * */
    Orders findOrderByIdWithUser3(Integer id);


    /**
     *  select * from orders where uid = ?;
     *
     *  参数类型: Integer id
     *  返回值类型: List<Orders>
     * */
    List<Orders> findOrdersByUid(Integer uid);
}