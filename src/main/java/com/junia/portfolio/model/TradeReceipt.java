package com.junia.portfolio.model;

public class TradeReceipt {
    private final Order order;
    private final double fee;
    private final double tax;
    private final double netAmount;

    public TradeReceipt(Order order, double fee, double tax, double netAmount) {
        this.order = order;
        this.fee = fee;
        this.tax = tax;
        this.netAmount = netAmount;
    }

    public Order getOrder() {
        return order;
    }

    public double getFee() {
        return fee;
    }

    public double getTax() {
        return tax;
    }

    public double getNetAmount() {
        return netAmount;
    }
}
