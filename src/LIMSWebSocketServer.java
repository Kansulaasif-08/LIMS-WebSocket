import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.InetSocketAddress;
import java.util.*;

public class LIMSWebSocketServer extends WebSocketServer {
    private Gson gson = new Gson();
    private Map<String, Object> samples = new HashMap<>();
    private Map<String, Object> equipment = new HashMap<>();
    private int sampleCounter = 1;
    private int equipmentCounter = 1;
    
    public LIMSWebSocketServer() {
        super(new InetSocketAddress(8887));
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║  LIMS WebSocket Server Starting...  ║");
        System.out.println("╚══════════════════════════════════════╝");
        initializeDemoData();
    }
    
    private void initializeDemoData() {
        // Create 3 demo samples
        createDemoSample("Blood Test - Patient A", "BLOOD", "PAT-001");
        createDemoSample("Urine Analysis - Patient B", "URINE", "PAT-002");
        createDemoSample("Tissue Biopsy - Patient C", "TISSUE", "PAT-003");
        
        // Create 3 demo equipment
        createDemoEquipment("PCR Machine", "X-200", "BioTech Corp");
        createDemoEquipment("Microscope", "Ultra-5000", "OptiScope");
        createDemoEquipment("Centrifuge", "SpinMax Pro", "LabEquip Inc");
        
        System.out.println("✓ Demo data initialized!");
    }
    
    private void createDemoSample(String name, String type, String patientId) {
        String sampleId = "SMP-" + String.format("%04d", sampleCounter++);
        Map<String, Object> sample = new HashMap<>();
        sample.put("sampleId", sampleId);
        sample.put("name", name);
        sample.put("type", type);
        sample.put("patientId", patientId);
        sample.put("status", "REGISTERED");
        sample.put("barcode", "BAR-" + System.currentTimeMillis());
        sample.put("collectionDate", new Date().toString());
        sample.put("collectedBy", "Demo User");
        samples.put(sampleId, sample);
    }
    
    private void createDemoEquipment(String name, String model, String manufacturer) {
        String equipmentId = "EQP-" + String.format("%03d", equipmentCounter++);
        Map<String, Object> eq = new HashMap<>();
        eq.put("equipmentId", equipmentId);
        eq.put("name", name);
        eq.put("model", model);
        eq.put("manufacturer", manufacturer);
        eq.put("serialNumber", "SN-" + System.currentTimeMillis());
        eq.put("status", "AVAILABLE");
        equipment.put(equipmentId, eq);
    }
    
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("✓ New connection from: " + conn.getRemoteSocketAddress().getAddress().getHostAddress());
        JsonObject response = new JsonObject();
        response.addProperty("type", "connection");
        response.addProperty("status", "connected");
        response.addProperty("message", "Welcome to LIMS!");
        conn.send(gson.toJson(response));
    }
    
    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            System.out.println("→ Received message");
            
            JsonObject request = JsonParser.parseString(message).getAsJsonObject();
            String action = request.get("action").getAsString();
            JsonObject data = request.has("data") ? request.getAsJsonObject("data") : new JsonObject();
            
            switch(action) {
                case "login":
                    handleLogin(conn, data);
                    break;
                case "getSamples":
                    sendSamples(conn);
                    break;
                case "createSample":
                    createSample(conn, data);
                    break;
                case "updateStatus":
                    updateSampleStatus(conn, data);
                    break;
                case "getEquipment":
                    sendEquipment(conn);
                    break;
                case "createEquipment":
                    createEquipment(conn, data);
                    break;
                case "getDashboard":
                    sendDashboard(conn);
                    break;
                case "ping":
                    sendPong(conn);
                    break;
                default:
                    System.out.println("⚠ Unknown action: " + action);
            }
        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleLogin(WebSocket conn, JsonObject data) {
        String username = data.get("username").getAsString();
        String password = data.get("password").getAsString();
        
        System.out.println("→ Login attempt: " + username);
        
        JsonObject response = new JsonObject();
        response.addProperty("type", "loginResponse");
        
        if ("admin".equals(username) && "admin123".equals(password)) {
            response.addProperty("status", "success");
            JsonObject user = new JsonObject();
            user.addProperty("userId", "USR-001");
            user.addProperty("username", username);
            user.addProperty("firstName", "Admin");
            user.addProperty("lastName", "User");
            user.addProperty("email", "admin@lims.com");
            user.addProperty("role", "ADMIN");
            response.add("user", user);
            System.out.println("✓ Login successful: " + username);
        } else {
            response.addProperty("status", "error");
            response.addProperty("message", "Invalid username or password");
            System.out.println("✗ Login failed: " + username);
        }
        
        conn.send(gson.toJson(response));
    }
    
    private void createSample(WebSocket conn, JsonObject data) {
        String sampleId = "SMP-" + String.format("%04d", sampleCounter++);
        
        Map<String, Object> sample = new HashMap<>();
        sample.put("sampleId", sampleId);
        sample.put("name", data.get("name").getAsString());
        sample.put("type", data.get("type").getAsString());
        sample.put("patientId", data.get("patientId").getAsString());
        sample.put("description", data.has("description") ? data.get("description").getAsString() : "");
        sample.put("status", "REGISTERED");
        sample.put("barcode", "BAR-" + System.currentTimeMillis());
        sample.put("collectionDate", new Date().toString());
        sample.put("collectedBy", "Current User");
        
        samples.put(sampleId, sample);
        
        System.out.println("✓ Sample created: " + sampleId + " - " + sample.get("name"));
        
        JsonObject response = new JsonObject();
        response.addProperty("type", "createSampleResponse");
        response.addProperty("status", "success");
        response.add("sample", gson.toJsonTree(sample));
        conn.send(gson.toJson(response));
        
        broadcastSamples();
    }
    
    private void updateSampleStatus(WebSocket conn, JsonObject data) {
        String sampleId = data.get("sampleId").getAsString();
        String newStatus = data.get("status").getAsString();
        
        Map<String, Object> sample = (Map<String, Object>) samples.get(sampleId);
        if (sample != null) {
            sample.put("status", newStatus);
            samples.put(sampleId, sample);
            
            System.out.println("✓ Sample status updated: " + sampleId + " → " + newStatus);
            
            JsonObject response = new JsonObject();
            response.addProperty("type", "updateStatusResponse");
            response.addProperty("status", "success");
            conn.send(gson.toJson(response));
            
            broadcastSamples();
        }
    }
    
    private void createEquipment(WebSocket conn, JsonObject data) {
        String equipmentId = "EQP-" + String.format("%03d", equipmentCounter++);
        
        Map<String, Object> eq = new HashMap<>();
        eq.put("equipmentId", equipmentId);
        eq.put("name", data.get("name").getAsString());
        eq.put("model", data.get("model").getAsString());
        eq.put("manufacturer", data.get("manufacturer").getAsString());
        eq.put("serialNumber", data.has("serialNumber") ? data.get("serialNumber").getAsString() : "SN-AUTO");
        eq.put("status", "AVAILABLE");
        
        equipment.put(equipmentId, eq);
        
        System.out.println("✓ Equipment created: " + equipmentId + " - " + eq.get("name"));
        
        JsonObject response = new JsonObject();
        response.addProperty("type", "createEquipmentResponse");
        response.addProperty("status", "success");
        response.add("equipment", gson.toJsonTree(eq));
        conn.send(gson.toJson(response));
        
        broadcastEquipment();
    }
    
    private void sendSamples(WebSocket conn) {
        JsonObject response = new JsonObject();
        response.addProperty("type", "samplesResponse");
        response.add("samples", gson.toJsonTree(samples.values()));
        conn.send(gson.toJson(response));
        System.out.println("→ Sent " + samples.size() + " samples");
    }
    
    private void sendEquipment(WebSocket conn) {
        JsonObject response = new JsonObject();
        response.addProperty("type", "equipmentResponse");
        response.add("equipment", gson.toJsonTree(equipment.values()));
        conn.send(gson.toJson(response));
        System.out.println("→ Sent " + equipment.size() + " equipment");
    }
    
    private void sendDashboard(WebSocket conn) {
        long registered = samples.values().stream()
            .filter(s -> "REGISTERED".equals(((Map)s).get("status")))
            .count();
        long completed = samples.values().stream()
            .filter(s -> "COMPLETED".equals(((Map)s).get("status")))
            .count();
        long available = equipment.values().stream()
            .filter(e -> "AVAILABLE".equals(((Map)e).get("status")))
            .count();
        
        JsonObject dashboard = new JsonObject();
        dashboard.addProperty("totalSamples", samples.size());
        dashboard.addProperty("pendingSamples", registered);
        dashboard.addProperty("completedSamples", completed);
        dashboard.addProperty("totalEquipment", equipment.size());
        dashboard.addProperty("availableEquipment", available);
        dashboard.add("recentSamples", gson.toJsonTree(
            samples.values().stream().limit(5).toArray()
        ));
        
        JsonObject response = new JsonObject();
        response.addProperty("type", "dashboardResponse");
        response.add("dashboard", dashboard);
        conn.send(gson.toJson(response));
        System.out.println("→ Sent dashboard data");
    }
    
    private void sendPong(WebSocket conn) {
        JsonObject response = new JsonObject();
        response.addProperty("type", "pong");
        response.addProperty("timestamp", System.currentTimeMillis());
        conn.send(gson.toJson(response));
    }
    
    private void broadcastSamples() {
        JsonObject update = new JsonObject();
        update.addProperty("type", "samplesUpdate");
        update.add("samples", gson.toJsonTree(samples.values()));
        broadcast(gson.toJson(update));
        System.out.println("⇄ Broadcasted samples update");
    }
    
    private void broadcastEquipment() {
        JsonObject update = new JsonObject();
        update.addProperty("type", "equipmentUpdate");
        update.add("equipment", gson.toJsonTree(equipment.values()));
        broadcast(gson.toJson(update));
        System.out.println("⇄ Broadcasted equipment update");
    }
    
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("✗ Connection closed: " + conn.getRemoteSocketAddress());
    }
    
    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("✗ WebSocket error: " + ex.getMessage());
    }
    
    @Override
    public void onStart() {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║   SERVER STARTED SUCCESSFULLY! ✓     ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.println("║  WebSocket: ws://localhost:8887      ║");
        System.out.println("║  HTTP Server: http://localhost:8080  ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.println("║  Open browser: http://localhost:8080 ║");
        System.out.println("║  Username: admin                     ║");
        System.out.println("║  Password: admin123                  ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.println();
    }
    
    public static void main(String[] args) {
        try {
            LIMSWebSocketServer server = new LIMSWebSocketServer();
            server.start();
            
            SimpleHTTPServer httpServer = new SimpleHTTPServer();
            httpServer.start();
            
            System.out.println("Press Ctrl+C to stop the server");
            
        } catch (Exception e) {
            System.err.println("✗ Failed to start: " + e.getMessage());
            e.printStackTrace();
        }
    }
}