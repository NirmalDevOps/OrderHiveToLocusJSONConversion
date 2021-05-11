package com.htc.connector.serviceimpl;

import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.htc.connector.locusmodel.Amount;
import com.htc.connector.locusmodel.Body;
import com.htc.connector.locusmodel.CancelOrder;
import com.htc.connector.locusmodel.CancelOrderStatusMessage;
import com.htc.connector.locusmodel.ContactPoint;
import com.htc.connector.locusmodel.DropAmount;
import com.htc.connector.locusmodel.Filters;
import com.htc.connector.locusmodel.LineItems;
import com.htc.connector.locusmodel.LocationAddress;
import com.htc.connector.locusmodel.Locus;
import com.htc.connector.locusmodel.OrderSelectRequest;
import com.htc.connector.locusmodel.Parts;
import com.htc.connector.locusmodel.PatchBody;
import com.htc.connector.locusmodel.PickupSlot;
import com.htc.connector.locusmodel.Price;
import com.htc.connector.locusmodel.Skills;
import com.htc.connector.locusmodel.Slot;
import com.htc.connector.locusmodel.Volume;
import com.htc.connector.locusmodel.Weight;
import com.htc.connector.orderhivemodel.CustomFieldsListing;
import com.htc.connector.orderhivemodel.OrderHive;
import com.htc.connector.orderhivemodel.OrderItems;
import com.htc.connector.service.OrderConverter;
import com.htc.connector.util.Constant;

/**
 * @author Nirmal
 * @version 1.0
 * @since 30-03-2021
 */

public class OrderConverterImpl implements OrderConverter {

	Locus locus = null;
	LocationAddress pickupLocationAddress = null;
	LocationAddress dropLocationAddress = null;
	Volume volume = null;
	LineItems lineItems = null;
	Slot dropSlot = null;
	Amount amount = null;
	DropAmount dropAmount = null;
	List<LineItems> listOflineItems = null;
	List<Slot> dropSlots = null;
	static final Logger LOGGER = LoggerFactory.getLogger(OrderConverterImpl.class);

	// This method is used to convert the Orderhive JSON request to Locus request.
	@Override
	public String createOrderHiveToLosus(OrderHive orderhive) {

		// Creating the ObjectMapper object
		ObjectMapper mapper = new ObjectMapper();
		// Converting the Object to JSONString
		String createOrderJSONString = null;
		locus = new Locus();
		locus.setClientId(Constant.CLIENT_ID);
		locus.setId(orderhive.getData().getChannel_order_id());
		locus.setStatus(Constant.STATUS);
		locus.setVersion(Constant.VERSION);

		Map<String, String> customProperty = null;
		List<CustomFieldsListing> listOfCustomFieldsListing = orderhive.getData().getCustom_fields_listing();

		// Find the size of CustomFieldsListing if greater then zero then set to custom
		// property
		if (listOfCustomFieldsListing.size() > 0) {
			customProperty = new HashMap<>();
			for (CustomFieldsListing customFieldsListing : listOfCustomFieldsListing) {
				customProperty.put(customFieldsListing.getName(), customFieldsListing.getValue());
			}
			locus.setCustomProperties(customProperty);
		}

		// Find the name from custom field listing and set it to itemid.
		if (listOfCustomFieldsListing.size() > 0) {
			for (CustomFieldsListing customFieldsListing : listOfCustomFieldsListing) {
				if (customFieldsListing.getName().equalsIgnoreCase("Locus Team")) {
					locus.setTeamId(customFieldsListing.getValue());
					break;
				}

			}
		}

		locus.setTaksType(orderhive.getData().getOrder_type());
		locus.setStatus(orderhive.getData().getOrder_status());

		// Iterate the OrderItems and set it to lineItem
		List<OrderItems> order_items = orderhive.getData().getOrder_items();
		if (order_items.size() > 0) {
			listOflineItems = new ArrayList<>();
			for (OrderItems orderItems : order_items) {
				setLineItems(orderItems);
				listOflineItems.add(lineItems);
			}

		}
		locus.setLineItems(listOflineItems);

		// Set Skills
		List<Skills> skills = setSkills();
		locus.setSkills(skills);

		// Setting PickupLocationAddress to locus from shipping address of OrderHive
		LocationAddress pickupLocatonAddress = setPickupLocationAddress(orderhive);
		locus.setPickupLocationAddress(pickupLocatonAddress);

		// Setting dropLocationAddress to locus from billing address of OrderHive
		LocationAddress dropLocatonAddress = setDropLocationAddress(orderhive);
		locus.setDropLocationAddress(dropLocatonAddress);

		// Setting dropContactPoint to locus from billing address of OrderHive
		ContactPoint dropContactPoint = setDropContactPoint(orderhive);
		locus.setDropContactPoint(dropContactPoint);

		// Setting PickupContactPoint to locus from shipping address of OrderHive
		ContactPoint pickupContactPoint = setPickupContactPoint(orderhive);
		locus.setPickupContactPoint(pickupContactPoint);

		// set drop amount
		DropAmount dropAmount = setDropAmount(orderhive);
		locus.setDropAmount(dropAmount);

		// find the current date of system and set it to drop date
		String dropDate = findSystemDate();
		locus.setDropDate(dropDate);

		// set the drop slot
		Slot dropSlotDateTime = setDropSlot();
		locus.setDropSlot(dropSlotDateTime);

		// set the drop slots list
		List<Slot> dropSlots = setListOfDropSlots(dropSlotDateTime);
		locus.setDropSlots(dropSlots);

		locus.setOrderedOn(orderhive.getData().getSync_created());
		locus.setHomebaseId(orderhive.getData().getWarehouse_id());
		locus.setPickupVisitName(orderhive.getData().getWarehouse_id());
		locus.setPickupLocationId(orderhive.getData().getWarehouse_id());
		locus.setOrderStatus(Constant.ORDER_STATUS);
		locus.setEffectiveStatus(Constant.EFFECTIVE_STATUS);

		try {
			createOrderJSONString = mapper.writeValueAsString(locus);
			LOGGER.info(
					"CreateOrderJSONString is converted successfully to locus create order : " + createOrderJSONString);
		} catch (JsonProcessingException e) { // TODO Auto-generated catch block
			LOGGER.error("Error: " + e);
		}
		return createOrderJSONString;
	}

	@Override
	public String editOrderHiveToLosus(OrderHive orderhive) {
		// Creating the ObjectMapper object
		ObjectMapper mapper = new ObjectMapper();
		// Converting the Object to JSONString
		String editOrderJSONString = null;
		Body body = new Body();
		PatchBody patchBody = new PatchBody();
		List<LineItems> listOfLineItems = new ArrayList<>();

		// Iterating the OrderItems and setting it to LineItems to Locus
		for (OrderItems orderItems : orderhive.getData().getOrder_items()) {
			LineItems lineItems = new LineItems();
			List<Parts> listOfParts = new ArrayList<Parts>();
			Price price = setPrice(orderItems);
			setLineIems(orderItems, lineItems, listOfParts, price);
			listOfLineItems.add(lineItems);

		}
		Map<String, String> customProperty = null;
		List<CustomFieldsListing> listOfCustomFieldsListing = orderhive.getData().getCustom_fields_listing();

		// Find the size of CustomFieldsListing if greater then zero then set to custom
		// property
		if (listOfCustomFieldsListing.size() > 0) {
			customProperty = new HashMap<>();
			for (CustomFieldsListing customFieldsListing : listOfCustomFieldsListing) {
				customProperty.put(customFieldsListing.getName(), customFieldsListing.getValue());
			}
			patchBody.setCustomProperties(customProperty);
		}

		patchBody.setLineItems(listOfLineItems);

		// Set Skills
		List<Skills> listOfSkills = new ArrayList<>();
		patchBody.setSkills(listOfSkills);

		patchBody.setPickupLocationId(orderhive.getData().getWarehouse_id());
		patchBody.setPickupVisitName(orderhive.getData().getWarehouse_id());

		List<PickupSlot> pickupSlots = new ArrayList<>();
		patchBody.setPickupSlots(pickupSlots);

		patchBody.setDropVisitName(orderhive.getData().getShipping_address().getName());

		ContactPoint dropContactPoint = setDropContactPoint(orderhive);
		patchBody.setDropContactPoint(dropContactPoint);

		ContactPoint dropPickupContactPoint = setPickupContactPoint(orderhive);
		patchBody.setPickupContactPoint(dropPickupContactPoint);

		// Setting PickupLocationAddress to locus from shipping address of OrderHive
		LocationAddress pickupLocatonAddress = setPickupLocationAddress(orderhive);
		patchBody.setPickupLocationAddress(pickupLocatonAddress);

		// Setting dropLocationAddress to locus from billing address of OrderHive
		LocationAddress dropLocatonAddress = setDropLocationAddress(orderhive);
		patchBody.setDropLocationAddress(dropLocatonAddress);

		String dropDate = findSystemDate();
		patchBody.setDropDate(dropDate);

		Slot dropSlot = setDropSlot();
		patchBody.setDropSlot(dropSlot);

		List<Slot> dropSlots = setListOfDropSlots(dropSlot);
		patchBody.setDropSlots(dropSlots);

		DropAmount dropAmount = setDropAmount(orderhive);
		patchBody.setDropAmount(dropAmount);

		String orderedOn = findSystemDate();
		patchBody.setOrderedOn(orderedOn);
		patchBody.setCreatedOn(findSystemDate());

		// Checking volume or weight from JSON Structure and set the data
		List<OrderItems> order_items = orderhive.getData().getOrder_items();
		if (order_items.size() > 0) {
			for (OrderItems orderItems : order_items) {
				if (orderItems.getVolume() != null) {
					volume.setValue(orderItems.getVolume().toString());
					volume.setUnit(Constant.VOLUME_UNIT);
					patchBody.setVolume(volume);
					break;
				} else {
					Weight weight = new Weight();
					weight.setValue((orderItems.getWeight().toString()));
					weight.setUnit(Constant.WEIGHT_UNIT);
					patchBody.setWeight(weight);
					break;
				}
			}
		}
		body.setPatchBody(patchBody);
		try {
			editOrderJSONString = mapper.writeValueAsString(body);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Converted response for locus by orderhive" + editOrderJSONString);
		return editOrderJSONString;

	}

	@Override
	public String cancelOrder(OrderHive orderhive) {

		CancelOrder cancelOrder = null;
		CancelOrderStatusMessage cancelOrderStatusMessage = null;
		// Creating the ObjectMapper object
		ObjectMapper mapper = new ObjectMapper();
		// Converting the Object to JSONString
		String cancelOrderJSONString = null;

		if (!orderhive.getData().getChannel_order_id().isEmpty()
				&& (orderhive.getData().getOrder_status().equalsIgnoreCase("cancel")
						|| orderhive.getData().getOrder_status().equalsIgnoreCase("canceled"))) {
			cancelOrder = new CancelOrder();
			cancelOrder.setOrderStatus(orderhive.getData().getOrder_status().toUpperCase().concat("LED"));

			OrderSelectRequest orderSelectRequest = new OrderSelectRequest();

			List<Filters> listOfFilters = new ArrayList<Filters>();
			Filters filters = setFilters(orderhive);
			listOfFilters.add(filters);
			orderSelectRequest.setFilters(listOfFilters);
			cancelOrder.setOrderSelectRequest(orderSelectRequest);
			try {
				cancelOrderJSONString = mapper.writeValueAsString(cancelOrder);

			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
			return cancelOrderJSONString;
		} else {
			cancelOrderStatusMessage = new CancelOrderStatusMessage();
			cancelOrderStatusMessage.setMessage("Order Id should not be empty and status should have cancel");
			try {
				cancelOrderJSONString = mapper.writeValueAsString(cancelOrderStatusMessage);

			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
			return cancelOrderJSONString;
		}

	}

	/**
	 * @param orderhive
	 * @return contactPoint
	 */
	private ContactPoint setPickupContactPoint(OrderHive orderhive) {
		ContactPoint contactPoint = new ContactPoint();
		contactPoint.setName(orderhive.getData().getShipping_address().getName());
		contactPoint.setNumber(orderhive.getData().getShipping_address().getContact_number());
		return contactPoint;
	}

	/**
	 * @param orderhive
	 * @return contactPoint
	 */
	private ContactPoint setDropContactPoint(OrderHive orderhive) {
		ContactPoint contactPoint = new ContactPoint();
		contactPoint.setName(orderhive.getData().getBilling_address().getName());
		contactPoint.setNumber(orderhive.getData().getBilling_address().getContact_number());
		return contactPoint;
	}

	/**
	 * @return skills
	 */
	private List<Skills> setSkills() {
		List<Skills> skills = new ArrayList<>();
		// Skills skill = new Skills();
		// skill.setSkills("Marketing");
		// skills.add(skill);
		return skills;
	}

	/**
	 * This method used in cancelOrder
	 * 
	 * @param orderhive
	 * @return filters
	 */
	private Filters setFilters(OrderHive orderhive) {
		Filters filters = new Filters();
		filters.setName("id");
		filters.setOperation("EQUALS");
		String[] values = new String[1];
		values[0] = orderhive.getData().getChannel_order_id();
		filters.setValues(values);
		return filters;
	}

	/**
	 * @param orderItems
	 * @param lineItems
	 * @param listOfParts
	 * @param price
	 */
	private void setLineIems(OrderItems orderItems, LineItems lineItems, List<Parts> listOfParts, Price price) {
		lineItems.setPrice(price);
		lineItems.setId(orderItems.getSku());
		lineItems.setNote(orderItems.getNote());
		lineItems.setLineItemId(orderItems.getItem_id().toString());
		lineItems.setName(orderItems.getName());
		lineItems.setQuantity(Double.parseDouble(orderItems.getQuantity_ordered().toString()));
		lineItems.setQuantityUnit(Constant.QUANTITY_UNIT);
		lineItems.setParts(listOfParts);
	}

	/**
	 * @param orderItems
	 * @return price
	 */
	private Price setPrice(OrderItems orderItems) {
		Price price = new Price();
		price.setAmount((double) orderItems.getPrice());
		price.setCurrency(Constant.CURRENCY_SYMBOL);
		price.setSymbol(Constant.CURRENCY_SYMBOL);
		return price;
	}

	/**
	 * @param orderItems
	 */
	private void setLineItems(OrderItems orderItems) {
		lineItems = new LineItems();
		lineItems.setId(orderItems.getSku());
		lineItems.setNote(orderItems.getNote());
		lineItems.setId(orderItems.getItem_id().toString());
		lineItems.setName(orderItems.getName());
		lineItems.setLineItemId(orderItems.getId().toString());
		lineItems.setQuantity((double) orderItems.getQuantity_ordered());
		lineItems.setQuantityUnit(Constant.QUANTITY_UNIT);
	}

	/**
	 * @param dropAmount
	 * @return dropAmount
	 */
	private DropAmount setDropAmount(OrderHive orderhive) {
		dropAmount = new DropAmount();
		setAmount(orderhive);
		//// NONE/COLLECT/GIVE
		dropAmount.setExchangeType(Constant.EXCHANGE_TYPE);
		return dropAmount;
	}

	/**
	 * @param orderhive
	 */
	private void setAmount(OrderHive orderhive) {
		amount = new Amount();
		amount.setAmount(orderhive.getData().getTotal());
		amount.setCurrency(Constant.CURRENCY_SYMBOL);
		amount.setSymbol(Constant.CURRENCY_SYMBOL);
		dropAmount.setAmount(amount);
	}

	/*
	 * @param 	: This method receive the dropSlotDateTime.
	 * @return	: dropSlots
	 */
	private List<Slot> setListOfDropSlots(Slot dropSlotDateTime) {
		dropSlot = new Slot();
		dropSlots = new ArrayList<Slot>();
		dropSlots.add(dropSlotDateTime);
		dropSlots.add(dropSlotDateTime);
		return dropSlots;
	}
	
	/*
	 * Find out DateTime based on UTC 
	 * @return	: dropSlot
	 */
	private Slot setDropSlot() {
		dropSlot = new Slot();
		dropSlot.setStart(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
		dropSlot.setEnd(ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
		return dropSlot;
	}


	/*
	 * Setting Pickup location address from shipping to orderhive
	 * @param 	: This method receives the orderhive obj as args.
	 * @return	: pickupLocationAddress
	 */
	private LocationAddress setPickupLocationAddress(OrderHive orderhive) {
		pickupLocationAddress = new LocationAddress();
		pickupLocationAddress.setPlaceName(orderhive.getData().getShipping_address().getCompany());
		pickupLocationAddress.setLocalityName(orderhive.getData().getShipping_address().getAddress2());
		pickupLocationAddress.setFormattedAddress(orderhive.getData().getShipping_address().getAddress1());
		pickupLocationAddress.setPincode(orderhive.getData().getShipping_address().getZipcode());
		pickupLocationAddress.setCity(orderhive.getData().getShipping_address().getCity());
		pickupLocationAddress.setState(orderhive.getData().getShipping_address().getState());
		pickupLocationAddress.setCountryCode(orderhive.getData().getShipping_address().getCountry_code());
		return pickupLocationAddress;
	}

	/**
	 * Setting Drop Location Address from shipping to orderhive
	 *  @param	: This method receives the orderhive obj as args.
	 * @return 	: dropLocationAddress
	 */
	private LocationAddress setDropLocationAddress(OrderHive orderhive) {
		dropLocationAddress = new LocationAddress();
		dropLocationAddress.setPlaceName(orderhive.getData().getBilling_address().getCompany());
		dropLocationAddress.setLocalityName(orderhive.getData().getBilling_address().getAddress2());
		dropLocationAddress.setFormattedAddress(orderhive.getData().getBilling_address().getAddress1());
		dropLocationAddress.setPincode(orderhive.getData().getBilling_address().getZipcode());
		dropLocationAddress.setCity(orderhive.getData().getBilling_address().getCity());
		dropLocationAddress.setState(orderhive.getData().getBilling_address().getState());
		dropLocationAddress.setCountryCode(orderhive.getData().getBilling_address().getCountry_code());
		return dropLocationAddress;
	}

	/**
	 * This method is used to find the current date of system in the dd/MM/yyyy format
	 * @return : date
	 */
	private String findSystemDate() {
		Date date = new Date();
		String formatedDate = new SimpleDateFormat(Constant.DATE_FORMAT).format(date);
		return formatedDate;
	}

}
