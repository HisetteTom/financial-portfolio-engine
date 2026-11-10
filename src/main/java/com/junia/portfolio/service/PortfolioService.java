package com.junia.portfolio.service;

import com.junia.portfolio.exception.AssetNotFoundException;
import com.junia.portfolio.exception.InsufficientFundsException;
import com.junia.portfolio.exception.InvalidTransactionException;
import com.junia.portfolio.model.Asset;
import com.junia.portfolio.model.Order;
import com.junia.portfolio.model.OrderType;
import com.junia.portfolio.model.Portfolio;
import com.junia.portfolio.model.TradeReceipt;
import com.junia.portfolio.utils.FinancialFormatter;

import java.util.Locale;
import java.util.Optional;

public class PortfolioService {
    private final Portfolio portfolio;
    private final MarketDataService marketDataService;

    public PortfolioService(Portfolio portfolio, MarketDataService marketDataService) {
        this.portfolio = portfolio;
        this.marketDataService = marketDataService;
    }

    public Optional<Asset> findAssetBySymbol(String symbol) {
        return portfolio.findAsset(symbol);
    }

    public TradeReceipt placeMarketOrder(OrderType type, String symbol, double quantity)
            throws InsufficientFundsException, AssetNotFoundException, InvalidTransactionException {
        if (symbol == null || symbol.isBlank()) {
            throw new InvalidTransactionException("Order symbol is missing.");
        }
        // price comes from the market so the user can't buy at a made up price
        double marketPrice = marketDataService.findPrice(symbol)
                .orElseThrow(() -> new AssetNotFoundException("No market price available for " + symbol + "."));
        return executeOrder(new Order(symbol, type, quantity, marketPrice));
    }

    public TradeReceipt executeOrder(Order order)
            throws InsufficientFundsException, AssetNotFoundException, InvalidTransactionException {
        validateOrder(order);

        if (order.getType() == OrderType.BUY) {
            return executeBuy(order);
        }
        return executeSell(order);
    }

    private void validateOrder(Order order) throws InvalidTransactionException {
        if (order.getType() == null) {
            throw new InvalidTransactionException("Order type is missing.");
        }
        if (order.getSymbol().isBlank()) {
            throw new InvalidTransactionException("Order symbol is missing.");
        }
        if (!(order.getQuantity() > 0) || !(order.getPrice() > 0)) {
            throw new InvalidTransactionException("Order quantity and price must be strictly positive.");
        }
    }

    private TradeReceipt executeBuy(Order order) throws InsufficientFundsException, AssetNotFoundException, InvalidTransactionException {
        Optional<Asset> ownedAsset = portfolio.findAsset(order.getSymbol());
        Asset reference = ownedAsset
                .or(() -> marketDataService.findListing(order.getSymbol()))
                .orElseThrow(() -> new AssetNotFoundException("Unknown symbol on the market: " + order.getSymbol()));

        checkOrderSize(reference, order);

        double fee = reference.calculateTransactionFee(order.getQuantity(), order.getPrice());
        double totalCost = order.getTotalValue() + fee;
        if (portfolio.getCashBalance() < totalCost) {
            throw new InsufficientFundsException(String.format(Locale.ROOT,
                    "Insufficient funds: %.2f needed but only %.2f available.", totalCost, portfolio.getCashBalance()));
        }

        portfolio.withdrawCash(totalCost);
        if (ownedAsset.isPresent()) {
            ownedAsset.get().addUnits(order.getQuantity(), order.getPrice());
        } else {
            portfolio.addAsset(reference.createPosition(order.getQuantity(), order.getPrice()));
        }
        return new TradeReceipt(order, fee, 0.0, totalCost);
    }

    private TradeReceipt executeSell(Order order) throws AssetNotFoundException, InvalidTransactionException {
        Asset asset = portfolio.findAsset(order.getSymbol())
                .orElseThrow(() -> new AssetNotFoundException("Asset not found in portfolio: " + order.getSymbol()));

        checkOrderSize(asset, order);

        if (!asset.holdsAtLeast(order.getQuantity())) {
            throw new InvalidTransactionException("Cannot sell more quantity than currently owned.");
        }

        double fee = asset.calculateTransactionFee(order.getQuantity(), order.getPrice());
        double tax = asset.calculateSellingTax(order.getPrice(), order.getQuantity());
        double netProceeds = order.getTotalValue() - fee - tax;
        if (netProceeds <= 0) {
            throw new InvalidTransactionException("Sale amount does not cover the transaction fees.");
        }

        asset.removeUnits(order.getQuantity());
        portfolio.depositCash(netProceeds);
        if (asset.isSoldOut()) {
            portfolio.removeAsset(asset);
        }
        return new TradeReceipt(order, fee, tax, netProceeds);
    }

    private void checkOrderSize(Asset asset, Order order) throws InvalidTransactionException {
        if (!asset.isOrderSizeValid(order.getQuantity())) {
            throw new InvalidTransactionException("Invalid quantity " + FinancialFormatter.formatQuantity(order.getQuantity())
                    + " for " + asset.getAssetType().name().toLowerCase() + " " + asset.getSymbol() + ".");
        }
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }
}
