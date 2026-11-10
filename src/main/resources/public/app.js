let allocationChartInstance = null;
let historyChartInstance = null;
let cachedMarketPrices = {};

async function fetchPortfolio() {
    try {
        const response = await fetch('/api/portfolio');
        const data = await response.json();
        cachedMarketPrices = data.allMarketPrices || {};
        renderView(data);
    } catch (err) {
        console.error("Loading error:", err);
    }
}

function renderView(data) {
    document.getElementById('totalValue').textContent = formatCurrency(data.totalPortfolioValue);
    document.getElementById('cashBalance').textContent = formatCurrency(data.cashBalance);
    document.getElementById('annualIncome').textContent = formatCurrency(data.projectedAnnualIncome);

    const pnlEl = document.getElementById('totalPnl');
    const pnlVal = data.totalUnrealizedPnl || 0.0;
    pnlEl.textContent = (pnlVal >= 0 ? '+' : '') + formatCurrency(pnlVal);
    pnlEl.className = 'kpi-value ' + (pnlVal >= 0 ? 'positive' : 'negative');

    updateSymbolSelect(data.assets, cachedMarketPrices);

    const tbody = document.getElementById('assetsTableBody');
    tbody.innerHTML = '';

    const labels = [];
    const values = [];

    data.assets.forEach(asset => {
        labels.push(asset.symbol);
        values.push(asset.currentValue);

        const row = document.createElement('tr');
        const pnlClass = asset.gainLoss >= 0 ? 'positive' : 'negative';
        const pnlPrefix = asset.gainLoss >= 0 ? '+' : '';

        row.innerHTML = `
            <td><strong>${asset.symbol}</strong></td>
            <td>${asset.assetType}</td>
            <td>${asset.quantity.toFixed(4)}</td>
            <td>${formatCurrency(asset.purchasePrice)}</td>
            <td>${formatCurrency(asset.marketPrice)}</td>
            <td>${formatCurrency(asset.currentValue)}</td>
            <td class="${pnlClass}">${pnlPrefix}${formatCurrency(asset.gainLoss)}</td>
            <td class="${pnlClass}">${pnlPrefix}${asset.returnOnInvestment.toFixed(2)} %</td>
            <td>${asset.allocation.toFixed(1)} %</td>
        `;
        tbody.appendChild(row);
    });

    renderAllocationChart(labels, values);
    renderHistoryChart(data.portfolioHistory || [data.totalPortfolioValue]);
}

function updateSymbolSelect(assets, allPrices) {
    const select = document.getElementById('symbolSelect');
    const currentVal = select.value;
    select.innerHTML = '';

    const symbols = Object.keys(allPrices).sort();
    symbols.forEach(sym => {
        const opt = document.createElement('option');
        opt.value = sym;
        const price = allPrices[sym] || 0.0;
        opt.textContent = `${sym} (${formatCurrency(price)})`;
        select.appendChild(opt);
    });

    if (currentVal && symbols.includes(currentVal)) {
        select.value = currentVal;
    } else if (symbols.length > 0) {
        select.value = symbols[0];
    }
    syncPriceWithSelectedSymbol();
}

function syncPriceWithSelectedSymbol() {
    const symbol = document.getElementById('symbolSelect').value;
    const priceInput = document.getElementById('price');
    if (symbol && cachedMarketPrices[symbol]) {
        priceInput.value = formatCurrency(cachedMarketPrices[symbol]);
    }
}

document.getElementById('symbolSelect').addEventListener('change', syncPriceWithSelectedSymbol);

function renderAllocationChart(labels, values) {
    const ctx = document.getElementById('allocationChart').getContext('2d');
    if (allocationChartInstance) {
        allocationChartInstance.destroy();
    }

    allocationChartInstance = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: labels,
            datasets: [{
                data: values,
                backgroundColor: [
                    '#f5a97f', '#8aadf4', '#a6da95', '#eed49f', '#c6a0f6', '#f0c6c6',
                    '#8bd5ca', '#ed8796', '#91d7e3', '#b7bdf8'
                ],
                borderWidth: 0
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: { color: '#cad3f5', boxWidth: 12, padding: 12 }
                }
            }
        }
    });
}

function renderHistoryChart(history) {
    const ctx = document.getElementById('historyChart').getContext('2d');
    if (historyChartInstance) {
        historyChartInstance.destroy();
    }

    const labels = history.map((_, i) => (i === 0 ? 'Start' : `Day ${i}`));
    const isProfitable = history.length > 1 && history[history.length - 1] >= history[0];
    const lineColor = isProfitable ? '#a6da95' : '#ed8796';

    historyChartInstance = new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: 'Total portfolio value (€)',
                data: history,
                borderColor: lineColor,
                backgroundColor: isProfitable ? 'rgba(166, 218, 149, 0.15)' : 'rgba(237, 135, 150, 0.15)',
                borderWidth: 2,
                fill: true,
                tension: 0.25,
                pointRadius: 4,
                pointBackgroundColor: lineColor
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                x: {
                    grid: { color: '#363a4f' },
                    ticks: { color: '#8087a2' }
                },
                y: {
                    grid: { color: '#363a4f' },
                    ticks: {
                        color: '#8087a2',
                        callback: val => val.toLocaleString('en-US') + ' €'
                    }
                }
            },
            plugins: {
                legend: {
                    display: false
                }
            }
        }
    });
}

function formatCurrency(val) {
    return (val || 0.0).toLocaleString('en-US', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    }) + ' €';
}

document.getElementById('simulateBtn').addEventListener('click', async () => {
    await fetch('/api/market/simulate', { method: 'POST' });
    fetchPortfolio();
});

document.getElementById('resetBtn').addEventListener('click', async () => {
    if (confirm('Reset the portfolio to its initial state?')) {
        await fetch('/api/portfolio/reset', { method: 'POST' });
        fetchPortfolio();
    }
});

document.getElementById('orderForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const symbol = document.getElementById('symbolSelect').value;
    const type = document.getElementById('type').value;
    const quantity = parseFloat(document.getElementById('quantity').value);

    const statusEl = document.getElementById('orderStatus');

    try {
        const response = await fetch('/api/orders', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ symbol, type, quantity })
        });

        const text = await response.text();
        if (response.ok) {
            statusEl.textContent = text;
            statusEl.className = 'positive';
            fetchPortfolio();
            document.getElementById('quantity').value = '';
        } else {
            statusEl.textContent = text;
            statusEl.className = 'negative';
        }
    } catch (err) {
        statusEl.textContent = 'Network error.';
        statusEl.className = 'negative';
    }
});

fetchPortfolio();
