let ws = null;
let currentUser = null;
let currentSampleId = null;

// Connect to WebSocket
function connect() {
    console.log('Connecting to WebSocket...');
    ws = new WebSocket('ws://localhost:8887');
    
    ws.onopen = () => {
        console.log('✓ Connected to server');
        updateConnectionStatus(true);
        showNotification('Connected to server', 'success');
    };
    
    ws.onmessage = (event) => {
        const data = JSON.parse(event.data);
        console.log('Received:', data.type);
        handleMessage(data);
    };
    
    ws.onerror = (error) => {
        console.error('WebSocket error:', error);
        updateConnectionStatus(false);
    };
    
    ws.onclose = () => {
        console.log('Disconnected from server');
        updateConnectionStatus(false);
        
        if (currentUser) {
            setTimeout(connect, 3000);
        }
    };
}

function send(action, data = {}) {
    if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ action, data }));
    } else {
        showNotification('Not connected to server', 'error');
    }
}

function handleMessage(message) {
    switch(message.type) {
        case 'connection':
            console.log(message.message);
            break;
        case 'loginResponse':
            handleLoginResponse(message);
            break;
        case 'dashboardResponse':
            updateDashboard(message.dashboard);
            break;
        case 'samplesResponse':
        case 'samplesUpdate':
            updateSamplesList(message.samples);
            break;
        case 'equipmentResponse':
        case 'equipmentUpdate':
            updateEquipmentList(message.equipment);
            break;
        case 'createSampleResponse':
            if (message.status === 'success') {
                showNotification('Sample created successfully', 'success');
                hideSampleForm();
            }
            break;
        case 'createEquipmentResponse':
            if (message.status === 'success') {
                showNotification('Equipment registered successfully', 'success');
                hideEquipmentForm();
            }
            break;
        case 'error':
            showNotification(message.message, 'error');
            break;
    }
}

function login() {
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    
    if (!username || !password) {
        document.getElementById('loginError').textContent = 'Please enter credentials';
        return;
    }
    
    send('login', { username, password });
}

function handleLoginResponse(message) {
    if (message.status === 'success') {
        currentUser = message.user;
        document.getElementById('loginPage').classList.remove('active');
        document.getElementById('mainPage').classList.add('active');
        document.getElementById('currentUser').textContent = 
            `${currentUser.firstName} ${currentUser.lastName}`;
        
        loadInitialData();
    } else {
        document.getElementById('loginError').textContent = 
            message.message || 'Login failed';
    }
}

function logout() {
    currentUser = null;
    document.getElementById('mainPage').classList.remove('active');
    document.getElementById('loginPage').classList.add('active');
    document.getElementById('password').value = '';
    document.getElementById('loginError').textContent = '';
}

function loadInitialData() {
    send('getDashboard');
    send('getSamples');
    send('getEquipment');
    updateServerTime();
    setInterval(updateServerTime, 1000);
}

function showTab(tab) {
    document.querySelectorAll('nav button').forEach(btn => {
        btn.classList.remove('active');
    });
    event.target.classList.add('active');
    
    document.querySelectorAll('.tab').forEach(t => {
        t.classList.remove('active');
    });
    document.getElementById(tab + 'Tab').classList.add('active');
    
    if (tab === 'samples') send('getSamples');
    if (tab === 'equipment') send('getEquipment');
    if (tab === 'dashboard') send('getDashboard');
}

function updateDashboard(data) {
    document.getElementById('totalSamples').textContent = data.totalSamples || 0;
    document.getElementById('pendingSamples').textContent = data.pendingSamples || 0;
    document.getElementById('completedSamples').textContent = data.completedSamples || 0;
    document.getElementById('totalEquipment').textContent = data.totalEquipment || 0;
}

// Sample functions
function showSampleForm() {
    document.getElementById('sampleForm').style.display = 'block';
}

function hideSampleForm() {
    document.getElementById('sampleForm').style.display = 'none';
    document.getElementById('sampleName').value = '';
    document.getElementById('sampleType').value = '';
    document.getElementById('patientId').value = '';
    document.getElementById('sampleDescription').value = '';
}

function createSample() {
    const name = document.getElementById('sampleName').value;
    const type = document.getElementById('sampleType').value;
    const patientId = document.getElementById('patientId').value;
    const description = document.getElementById('sampleDescription').value;
    
    if (!name || !type || !patientId) {
        showNotification('Please fill all required fields', 'error');
        return;
    }
    
    send('createSample', { name, type, patientId, description });
}

function updateSamplesList(samples) {
    const container = document.getElementById('samplesList');
    
    if (!samples || samples.length === 0) {
        container.innerHTML = '<p style="padding:20px;">No samples found</p>';
        return;
    }
    
    let html = '<table><thead><tr>';
    html += '<th>Sample ID</th><th>Barcode</th><th>Name</th>';
    html += '<th>Type</th><th>Patient</th><th>Status</th>';
    html += '</tr></thead><tbody>';
    
    samples.forEach(sample => {
        html += `<tr>
            <td>${sample.sampleId}</td>
            <td>${sample.barcode}</td>
            <td>${sample.name}</td>
            <td>${sample.type}</td>
            <td>${sample.patientId}</td>
            <td><span class="status-badge ${sample.status.toLowerCase()}">${sample.status}</span></td>
        </tr>`;
    });
    
    html += '</tbody></table>';
    container.innerHTML = html;
}

function filterSamples() {
    const search = document.getElementById('sampleSearch').value.toLowerCase();
    const rows = document.querySelectorAll('#samplesList tbody tr');
    
    rows.forEach(row => {
        const text = row.textContent.toLowerCase();
        row.style.display = text.includes(search) ? '' : 'none';
    });
}

// Equipment functions
function showEquipmentForm() {
    document.getElementById('equipmentForm').style.display = 'block';
}

function hideEquipmentForm() {
    document.getElementById('equipmentForm').style.display = 'none';
    document.getElementById('equipmentName').value = '';
    document.getElementById('equipmentModel').value = '';
    document.getElementById('equipmentManufacturer').value = '';
}

function createEquipment() {
    const name = document.getElementById('equipmentName').value;
    const model = document.getElementById('equipmentModel').value;
    const manufacturer = document.getElementById('equipmentManufacturer').value;
    
    if (!name || !model || !manufacturer) {
        showNotification('Please fill all fields', 'error');
        return;
    }
    
    send('createEquipment', { name, model, manufacturer });
}

function updateEquipmentList(equipment) {
    const container = document.getElementById('equipmentList');
    
    if (!equipment || equipment.length === 0) {
        container.innerHTML = '<p style="padding:20px;">No equipment found</p>';
        return;
    }
    
    let html = '<table><thead><tr>';
    html += '<th>ID</th><th>Name</th><th>Model</th><th>Manufacturer</th><th>Status</th>';
    html += '</tr></thead><tbody>';
    
    equipment.forEach(eq => {
        html += `<tr>
            <td>${eq.equipmentId}</td>
            <td>${eq.name}</td>
            <td>${eq.model}</td>
            <td>${eq.manufacturer}</td>
            <td><span class="status-badge ${eq.status.toLowerCase()}">${eq.status}</span></td>
        </tr>`;
    });
    
    html += '</tbody></table>';
    container.innerHTML = html;
}

// Utility functions
function updateConnectionStatus(connected) {
    const status = document.getElementById('connectionStatus');
    if (connected) {
        status.textContent = '● Connected';
        status.className = 'status connected';
    } else {
        status.textContent = '● Disconnected';
        status.className = 'status disconnected';
    }
}

function updateServerTime() {
    const now = new Date();
    document.getElementById('serverTime').textContent = 
        now.toLocaleTimeString();
}

function showNotification(message, type) {
    console.log(`${type.toUpperCase()}: ${message}`);
    document.getElementById('statusMessage').textContent = message;
    setTimeout(() => {
        document.getElementById('statusMessage').textContent = 'Ready';
    }, 3000);
}

// Initialize
window.onload = function() {
    connect();
    
    document.getElementById('password').addEventListener('keypress', function(e) {
        if (e.key === 'Enter') login();
    });
};

// Keep connection alive
setInterval(() => {
    if (ws && ws.readyState === WebSocket.OPEN) {
        send('ping');
    }
}, 30000);