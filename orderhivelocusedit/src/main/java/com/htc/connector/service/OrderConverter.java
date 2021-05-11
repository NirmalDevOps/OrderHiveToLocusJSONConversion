package com.htc.connector.service;

import com.htc.connector.orderhivemodel.OrderHive;

/**
 * @author Nirmal
 * @version 1.0
 * @since 30-03-2021
 */

public interface OrderConverter {
	public String createOrderHiveToLosus(OrderHive orderHiveData);

	public String editOrderHiveToLosus(OrderHive orderHiveData);

	// public String cancelOrderHiveToLosus(OrderhiveCancelOrder orderhive);
	public String cancelOrder(OrderHive orderhive);

}
