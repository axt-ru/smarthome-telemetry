const MAX_POINTS = 600;

let chart = null;
let source = null;
let selected = null;
let devices = [];

const statusEl = document.getElementById("status");
const titleEl = document.getElementById("title");
const currentEl = document.getElementById("current");
const periodEl = document.getElementById("period");

periodEl.onchange = () => { if (selected !== null) select(selected); };

async function loadDevices() {
    const response = await fetch("/api/devices");
    devices = await response.json();

    const list = document.getElementById("devices");
    list.innerHTML = "";
    devices.forEach(device => {
        const item = document.createElement("li");
        item.dataset.deviceId = device.deviceId;
        item.innerHTML = `${metricLabel(device.metric)}<span class="room">${device.room}</span>`;
        item.onclick = () => select(device);
        list.appendChild(item);
    });

    if (devices.length > 0 && selected === null) {
        select(devices[0]);
    }
}

function metricLabel(metric) {
    switch (metric) {
        case "TEMPERATURE": return "Температура";
        case "HUMIDITY": return "Влажность";
        case "CO2": return "CO₂";
        case "POWER": return "Потребление";
        default: return metric;
    }
}

async function select(device) {
    selected = device;

    document.querySelectorAll("#devices li").forEach(item =>
        item.classList.toggle("active", item.dataset.deviceId === device.deviceId));

    titleEl.innerHTML = `${device.room}. ${metricLabel(device.metric)}, ${device.unit}<span id="source">${sourceLabel(Number(periodEl.value))}</span>`;
    currentEl.textContent = "—";

    const minutes = Number(periodEl.value);
    const response = await fetch(`/api/measurements/${encodeURIComponent(device.deviceId)}?minutes=${minutes}`);
    const history = await response.json();

    drawChart(history.map(point => ({x: new Date(point.at).getTime(), y: point.value})));
    subscribe(device.deviceId);
}

function sourceLabel(minutes) {
    if (minutes <= 120) return "сырые показания";
    if (minutes <= 2880) return "минутные агрегаты";
    return "часовые агрегаты";
}

function drawChart(points) {
    if (chart !== null) {
        chart.destroy();
    }
    chart = new Chart(document.getElementById("chart"), {
        type: "line",
        data: {
            datasets: [{
                data: points,
                borderColor: "#2563eb",
                backgroundColor: "rgba(37,99,235,.08)",
                borderWidth: 2,
                pointRadius: 0,
                fill: true,
                tension: .25
            }]
        },
        options: {
            animation: false,
            responsive: true,
            plugins: {legend: {display: false}},
            scales: {
                x: {
                    type: "linear",
                    ticks: {
                        maxTicksLimit: 8,
                        callback: value => new Date(value).toLocaleTimeString("ru-RU")
                    }
                },
                y: {beginAtZero: false}
            }
        }
    });
}

function subscribe(deviceId) {
    if (source !== null) {
        source.close();
    }
    source = new EventSource(`/api/stream?deviceId=${encodeURIComponent(deviceId)}`);

    source.onopen = () => setStatus(true);
    source.onerror = () => setStatus(false);
    source.onmessage = event => {
        const measurement = JSON.parse(event.data);
        appendPoint(new Date(measurement.measuredAt), measurement.value);
    };
}

function appendPoint(at, value) {
    setStatus(true);
    currentEl.textContent = `${value} ${selected.unit}`;

    if (chart === null) {
        return;
    }
    const points = chart.data.datasets[0].data;
    points.push({x: at.getTime(), y: value});
    if (points.length > MAX_POINTS) {
        points.shift();
    }
    chart.update("none");
}

function setStatus(online) {
    statusEl.className = online ? "online" : "offline";
    statusEl.textContent = online ? "поток идёт" : "нет данных";
}

loadDevices();
setInterval(loadDevices, 30000);

const conditionLabels = {GT: "больше", LT: "меньше", OUT_OF_RANGE: "вне диапазона", NO_DATA: "молчит"};

async function loadRules() {
    const response = await fetch("/api/rules");
    const rules = await response.json();

    const table = document.getElementById("rules");
    table.innerHTML = "";
    rules.forEach(rule => {
        const row = table.insertRow();
        row.insertCell().textContent = rule.name;
        row.insertCell().textContent = rule.room || "все комнаты";
        row.insertCell().textContent = metricLabel(rule.metric);
        row.insertCell().textContent = describeCondition(rule);
        const actions = row.insertCell();
        const remove = document.createElement("button");
        remove.textContent = "×";
        remove.title = "удалить правило";
        remove.onclick = async () => {
            await fetch(`/api/rules/${rule.id}`, {method: "DELETE"});
            loadRules();
        };
        actions.appendChild(remove);
    });
}

function describeCondition(rule) {
    const label = conditionLabels[rule.conditionType] || rule.conditionType;
    if (rule.conditionType === "OUT_OF_RANGE") {
        return `${label} ${rule.thresholdLow}…${rule.thresholdHigh}`;
    }
    if (rule.conditionType === "NO_DATA") {
        return label;
    }
    return `${label} ${rule.conditionType === "GT" ? rule.thresholdHigh : rule.thresholdLow}`;
}

async function loadAlerts() {
    const response = await fetch("/api/alerts?limit=20");
    const alerts = await response.json();

    const list = document.getElementById("alerts");
    list.innerHTML = "";
    if (alerts.length === 0) {
        const item = document.createElement("li");
        item.className = "empty";
        item.textContent = "пока тихо";
        list.appendChild(item);
        return;
    }
    alerts.forEach(alert => {
        const item = document.createElement("li");
        const time = new Date(alert.firedAt).toLocaleTimeString("ru-RU");
        item.innerHTML = `<span class="time">${time}</span>${alert.message}`;
        list.appendChild(item);
    });
}

document.getElementById("rule-form").onsubmit = async event => {
    event.preventDefault();
    const form = event.target;
    const body = {
        name: form.name.value,
        room: form.room.value,
        metric: form.metric.value,
        conditionType: form.conditionType.value,
        thresholdLow: form.thresholdLow.value === "" ? null : Number(form.thresholdLow.value),
        thresholdHigh: form.thresholdHigh.value === "" ? null : Number(form.thresholdHigh.value),
        consecutiveCount: Number(form.consecutiveCount.value),
        cooldownSeconds: 120,
        channel: "LOG"
    };
    await fetch("/api/rules", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify(body)
    });
    form.reset();
    loadRules();
};

loadRules();
loadAlerts();
setInterval(loadAlerts, 5000);
