package com.junia.portfolio.model;

public class Order {
    private String symbol;
    private OrderType type;
    private double quantity;
    private double price;

    public Order(String symbol, OrderType type, double quantity, double price) {
        this.symbol = symbol;
        this.type = type;
        this.quantity = quantity;
        this.price = price;
    }

    public String getSymbol() {
        return symbol;
    }

    public OrderType getType() {
        return type;
    }

    public double getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }

    public double getTotalValue() {
        return quantity * price;
    }
}
