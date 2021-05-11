package com.htc.connector.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.htc.connector.orderhivemodel.OrderHive;
import com.htc.connector.serviceimpl.OrderConverterImpl;

@RestController
@RequestMapping("/orders/salesorder")
public class OrderController {

	OrderConverterImpl orderConverterImpl = new OrderConverterImpl();

	@PostMapping("/createOrder")
	public String createOrder(@RequestBody OrderHive order) {
		System.out.println("Data Create Order request :" + order);
		return orderConverterImpl.createOrderHiveToLosus(order);
	}

	@PostMapping("/editOrder")
	public String editOrder(@RequestBody OrderHive order) {
		System.out.println("Data Edit Order request :" + order);
		return orderConverterImpl.editOrderHiveToLosus(order);
	}

	@PostMapping("/cancelOrder")
	public String cancelOrder(@RequestBody OrderHive order) {
		System.out.println("Data cancelOrder request :" + order);
		return orderConverterImpl.cancelOrder(order);
	}

}
