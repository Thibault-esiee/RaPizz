package fr.esiee.rapizz;

import java.sql.Timestamp;

public class Order {
    public int id;
    public Timestamp orderTime;
    public Timestamp deliveryTime;
    public double basePrice;
    public boolean isPaid;
    public String pizza;
    public String deliverer;
    public String vehicle;
    public String customer;
    public int delayMinutes;
    public double totalPrice;

    public Order(int id, Timestamp orderTime, Timestamp deliveryTime, double basePrice, boolean isPaid,
                 String pizza, String deliverer, String vehicle, String customer, int delayMinutes, double totalPrice) {
        this.id = id;
        this.orderTime = orderTime;
        this.deliveryTime = deliveryTime;
        this.basePrice = basePrice;
        this.isPaid = isPaid;
        this.pizza = pizza;
        this.deliverer = deliverer;
        this.vehicle = vehicle;
        this.customer = customer;
        this.delayMinutes = delayMinutes;
        this.totalPrice = totalPrice;
    }

    public int getId() { return id; }
    public Timestamp getOrderTime() { return orderTime; }
    public Timestamp getDeliveryTime() { return deliveryTime; }
    public double getBasePrice() { return basePrice; }
    public boolean isPaid() { return isPaid; }
    public String getPizza() { return pizza; }
    public String getDeliverer() { return deliverer; }
    public String getVehicle() { return vehicle; }
    public String getCustomer() { return customer; }
    public int getDelayMinutes() { return delayMinutes; }
    public double getTotalPrice() { return totalPrice; }
}